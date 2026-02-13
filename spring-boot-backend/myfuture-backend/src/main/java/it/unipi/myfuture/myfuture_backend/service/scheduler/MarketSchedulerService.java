package it.unipi.myfuture.myfuture_backend.service.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.myfuture.myfuture_backend.dao.mongo.asset.AssetDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.asset_price.AssetPriceAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.asset_price.AssetPriceDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.news.NewsAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.AssetRedisDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.NewsRedisDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetGrowthDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.DailyVolumeDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.MostTradedAssetDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorNewsGroupDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.model.Asset;
import it.unipi.myfuture.myfuture_backend.model.AssetPrice;
import it.unipi.myfuture.myfuture_backend.model.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for scheduled tasks related to market data and analytics.
 * It synchronizes high-frequency data in Redis with persistent storage in MongoDB and refreshes complex aggregations
 * used by the API.
 */
@Service
public class MarketSchedulerService  implements CommandLineRunner {

    @Autowired
    private AssetDao assetDao;
    @Autowired
    private AssetPriceDao assetPriceDao;

    @Autowired
    private AssetPriceAggregationDao assetPriceAggregationDao;
    @Autowired
    private TransactionAggregationDao transactionAggregationDao;
    @Autowired
    private NewsAggregationDao newsAggregationDao;

    @Autowired
    private AssetRedisDao assetRedisDao;
    @Autowired
    private NewsRedisDao newsRedisDao;

    @Autowired
    private ObjectMapper objectMapper;

    //------------------------------------------ start: scheduled methods ----------------------------------------------
    /**
     * START OF DAY TASKS -> Execution: Every day at the start of the market (NYSE or NASDAQ).
     * The market open at 9:30 from monday to friday.
     * Operation to do:
     * -
     * - takes list of active assets (filtered by type) from MongoDB and puts them in Redis for quick access.
     * - calculate asset:most_traded (perform aggregation on MongoDB and save results on Redis)
     * - calculate asset:most_growth (perform aggregation on MongoDB and save results on Redis)
     * - calculate asset:worst_decline (perform aggregation on MongoDB and save results to Redis)
     */
    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "America/New_York")
    public void executeDailyTasks() {
        try {
            System.out.println("[SCHEDULER] Starting Daily Tasks...");

            // -- assets list --
            refreshAssetLists();

            // -- most traded -- most growth -- worst decline --
            refreshAllAssetStatistics();

            System.out.println("[SCHEDULER] Start daily Tasks Completed successfully.");
        } catch (Exception e) {
            System.err.println("[SCHEDULER ERROR] Failed to complete start daily tasks: " + e.getMessage());
        }
    }

    /**
     * HOURLY MARKET UPDATE -> Execution: Every hour during standard market hours (e.g., Mon-Fri, 9:30 AM - 16:00 PM).
     * While the market is OPEN, we need to update the rankings (like most traded) frequently to reflect the current
     * day's activity without overloading MongoDB on every single user request.
     * Operation to do:
     * - calculate asset:most_growth (perform aggregation on MongoDB and save results on Redis)
     * - calculate asset:worst_decline (perform aggregation on MongoDB and save results to Redis)
     */
    @Scheduled(cron = "0 0 10-16 * * MON-FRI", zone = "America/New_York")
    public void executeHourlyTasks() {
        try {
            System.out.println("[SCHEDULER] Starting hourly tasks...");

            // -- most growth --
            assetRedisDao.clearTopGrowth();                     // clear Redis cache
            // calculate the top 10 assets with the best growth last day.
            List<AssetGrowthDTO> topGrowthAsset = assetPriceAggregationDao.findAssetPerformance(TimeWindow.DAY, false);
            convertAndSaveMostGrowth(topGrowthAsset);           // save into Redis

            // -- worst decline --
            assetRedisDao.clearWorstDecline();                  // clear Redis cache
            // calculate the top 10 assets with the best decline last day.
            List<AssetGrowthDTO> topWorstAsset = assetPriceAggregationDao.findAssetPerformance(TimeWindow.DAY, true);
            convertAndSaveWorstDecline(topWorstAsset);           // save into Redis

            System.out.println("[SCHEDULER] Hourly Tasks Completed successfully.");
        } catch (Exception e) {
            System.err.println("[SCHEDULER ERROR] Failed to complete start hourly tasks: " + e.getMessage());
        }
    }

    /**
     * END OF DAY PERSISTENCE (Data Archiving) -> Execution: Every day at 16:05 PM.
     * We collect intraday prices (more or less every minute) during the day. Before the day ends, we must aggregate
     * these points into a single OHLC (Open-High-Low-Close) record in MongoDB and clear Redis to free up space for
     * the next day.
     */
    @Scheduled(cron = "0 5 16 * * MON-FRI", zone = "America/New_York")
    public void executeEndOfDayTasks() {
        System.out.println("[SCHEDULER] Starting End-of-Day Persistence...");

        // get daily volume and put into a map for easier retrieval
        List<DailyVolumeDTO> dailyVolumes = transactionAggregationDao.getDailyVolumeBySymbol();
        Map<String, Double> volumeMap = dailyVolumes.stream().collect(Collectors.toMap(DailyVolumeDTO::getSymbol, DailyVolumeDTO::getVolume));

        // define the types of assets to process
        AssetType[] types = {AssetType.share, AssetType.etf, AssetType.crypto};

        // get the list of assets by asset type
        for (AssetType type : types) {
            // get the list of symbols for this type from Redis Hash
            Map<Object, Object> assetMap = assetRedisDao.getAssetListByType(type);

            // scan all assets in the list
            for (Object symbolObj : assetMap.keySet()) {
                String symbol = (String) symbolObj;             // get symbol
                // get volume from map (default to 0.0 if no trades happened)
                Double totalVolume = volumeMap.getOrDefault(symbol, 0.0);
                try {
                    processAndPersistAssetIntraday(symbol, totalVolume);     // search intraday for the symbol, process and save into MongoDB
                } catch (Exception e) {
                    System.err.println("[SCHEDULER ERROR] Failed to persist " + symbol + ": " + e.getMessage());
                }
            }
        }

        System.out.println("[SCHEDULER] End-of-Day Persistence Completed.");
    }
    //------------------------------------------ start: scheduled methods ----------------------------------------------

    //--------------------------------------------- start: run methods -------------------------------------------------

     /**
      * This method runs automatically as soon as the Spring application context is fully loaded.
      * Perfect for testing and initializing Redis with MongoDB data.
     */
    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- [STARTUP] INITIALIZING REDIS CACHE ---");

        try {
            this.initializeSystemState();       // perform all initialization of the system

            System.out.println("--- [STARTUP] REDIS CACHE INITIALIZED SUCCESSFULLY ---");
        } catch (Exception e) {
            System.err.println("--- [STARTUP ERROR] Cache initialization failed: " + e.getMessage());
        }
    }

    /**
     * Internal method to encapsulate the initialization logic so it can be called both by the scheduler and at startup.
     */
    private void initializeSystemState() throws JsonProcessingException {

        this.refreshAssetLists();           // clean and update assets list by asset types

        this.refreshAllAssetStatistics();   // clean and update most traded, most growth, worst decline

        this.refreshNewsCache();            // load news
    }
    //---------------------------------------------- end: run methods --------------------------------------------------

    //------------------------------------------ start: utilities methods ----------------------------------------------

    /**
     * Method to delete all news saved in Redis and update with the last news for each category (sector).
     */
    private void refreshNewsCache() {
        System.out.println("[SCHEDULER] Starting News Cache Refresh...");

        newsRedisDao.clearAllNewsData();            // clear all old saved news

        // retrieve the last news grouped by sector (max 10 news for sector)
        List<SectorNewsGroupDTO> groupedNews = newsAggregationDao.findLatestNewsBySector(5*365);

        // scan all sector
        for (SectorNewsGroupDTO group : groupedNews) {
            System.out.println("[SCHEDULER] Processing sector: " + group.getSector());

            // scan all news for the current sector
            for (News n : group.getNewsList()) {
                Map<String, String> map = new HashMap<>();      // create hash map for news information (title, summary, sector, timestamp)
                map.put("title", n.getTitle());                 // set the title of news
                map.put("summary", n.getSummary());             // set the summary news
                map.put("sector", n.getSector());               // save the category(sector) news
                map.put("timestamp", String.valueOf(n.getDate().toEpochMilli()));   // save publication date

                newsRedisDao.saveNews(n.getId(), map);          // save using limit logic (implemented in DAO)
            }
        }
    }

    /**
     * Method to refresh (delete and update) the value of the asset's aggregation to show to the users.
     *
     * @throws JsonProcessingException
     */
    private void refreshAllAssetStatistics() throws JsonProcessingException
    {
        assetRedisDao.clearGlobalDailyData();           // clear all stats
        // -- most traded --
        // calculate the top 10 trading assets in this day
        List<MostTradedAssetDTO> topAssets = transactionAggregationDao.findMostTradedAssets(TimeWindow.DAY);
        if (!topAssets.isEmpty())
            convertAndSaveTopAssets(topAssets);

        // -- most growth --
        // calculate the top 10 assets with the best growth last day.
        List<AssetGrowthDTO> topGrowthAsset = assetPriceAggregationDao.findAssetPerformance(TimeWindow.DAY, false);
        convertAndSaveMostGrowth(topGrowthAsset);                       // save into Redis

        // -- worst decline --
        // calculate the top 10 assets with the best decline last day.
        List<AssetGrowthDTO> topWorstAsset = assetPriceAggregationDao.findAssetPerformance(TimeWindow.DAY, true);
        convertAndSaveWorstDecline(topWorstAsset);                      // save into Redis
    }

    /**
     * Method to clear the asset lists by asset types and update them.
     */
    private void refreshAssetLists()
    {
        // get the asset list and put in Redis. the key is asset:{type}:list (Hash) -> Map (Field: symbol, Value: name)
        List<Asset> etfAssets = assetDao.findByType(AssetType.etf);
        List<Asset> shareAssets = assetDao.findByType(AssetType.share);
        List<Asset> cryptoAssets = assetDao.findByType(AssetType.crypto);

        // delete all list
        assetRedisDao.deleteAssetListByType(AssetType.etf);
        assetRedisDao.deleteAssetListByType(AssetType.share);
        assetRedisDao.deleteAssetListByType(AssetType.crypto);

        // convert Lists to Maps and save to Redis (requires a Map<Field, Value>)
        if (!etfAssets.isEmpty())
            assetRedisDao.saveAssetListByType(AssetType.etf, convertToMap(etfAssets));

        if (!shareAssets.isEmpty())
            assetRedisDao.saveAssetListByType(AssetType.share, convertToMap(shareAssets));

        if (!cryptoAssets.isEmpty())
            assetRedisDao.saveAssetListByType(AssetType.crypto, convertToMap(cryptoAssets));
    }

    /**
     * Aggregates intraday points from Redis into a single OHLC candle and saves to MongoDB.
     */
    private void processAndPersistAssetIntraday(String symbol, double dailyVolume) {
        // fetch all intraday prices from Redis ZSet (Score: Timestamp, Member: "Timestamp:Price")
        Set<Object> intradayData = assetRedisDao.getAllIntradayPrices(symbol);

        // control check
        if (intradayData == null || intradayData.isEmpty())
            return;                     // skip there aren't data for today

        List<Double> prices = new ArrayList<>();                    // list of the prices
        // scan all obtained values
        for (Object entry : intradayData) {
            String[] parts = entry.toString().split(":");    // extract price from "timestamp:price" string
            if (parts.length == 2) {
                prices.add(Double.parseDouble(parts[1]));           // get the price
            }
        }

        // calculate values for asset_prices entity
        if (!prices.isEmpty()) {
            double open = prices.get(0);                    // get open price of the day
            double close = prices.get(prices.size() - 1);   // get close price of the day
            double high = Collections.max(prices);          // get max
            double low = Collections.min(prices);           // get min

            // create asset_prices entity
            AssetPrice historicalPrice = new AssetPrice(LocalDate.now().atStartOfDay(ZoneId.of("America/New_York")).toInstant(),
                    symbol, open, high, low, close, (long)dailyVolume);

            assetPriceDao.save(historicalPrice);        // Save to MongoDB collection 'asset_prices'
            assetRedisDao.clearIntradayData(symbol);    // if save was successful, delete from Redis

            System.out.println("[SCHEDULER] Persisted and cleared intraday data for: " + symbol);
        }
    }

    /**
     * Converts a List of Asset objects into a Map for Redis HASH storage.
     *
     * @param assets List of assets from MongoDB
     * @return Map where Key = Symbol, Value = Name
     */
    private Map<String, String> convertToMap(List<Asset> assets) {
        Map<String, String> map = new HashMap<>();
        for (Asset a : assets) {
            map.put(a.getSymbol(), a.getLongName());   // put the symbol and the long name for the asset
        }
        return map;
    }

    /**
     * Convert a list of most traded assets in json to put into Redis.
     * The key on redis is:
     * asset:most_traded (Hash)   -> Stats of the most traded assets from the previous day (Field: symbol, Value: json).
     *
     * MostTradedAssetDTO
     * - String symbol;
     * - Long transactionCount;      // count of the operations
     * - Double totalQuantity;       // sum of traded quantity
     * - Double totalVolume;         // sum of (quantity * price)
     *
     * @param topAssets list of the top assets to put in Redis
     */
    private void convertAndSaveTopAssets(List<MostTradedAssetDTO> topAssets) throws JsonProcessingException {
        // prepare a Map to store in Redis Hash (Field: Symbol, Value: JSON String)
        Map<String, String> rankingMap = new HashMap<>();
        // create the value to put in Redis
        for (MostTradedAssetDTO dto : topAssets) {
            rankingMap.put(dto.getSymbol(), objectMapper.writeValueAsString(dto));
        }

        assetRedisDao.updateAllMostTraded(rankingMap);      // update into Redis
    }

    /**
     * Take the list of metadata related to the best growth asset of the day and put into Redis.
     * The key on redis is:
     * asset:top_growth (ZSet)      -> Ranking of assets with highest % growth. Score: % change.
     *
     * AssetGrowthDTO
     * - String symbol;              // symbol of the asset
     * - Double percentageChange;    // growth or loss in percentage
     * - TimeWindow window;          // time window considered
     *
     * @param growthAssets list of the best growth asset to put in Redis
     */
    private void convertAndSaveMostGrowth(List<AssetGrowthDTO> growthAssets) throws JsonProcessingException {
        // put the value on Redis (Field: Symbol, Value: percentage)
        for (AssetGrowthDTO dto : growthAssets) {
            assetRedisDao.updateTopGrowth(dto.getSymbol(), dto.getPercentageChange());
        }
    }

    /**
     * Take the list of metadata related to the worst decline asset of the day and put into Redis.
     * The key on redis is:
     * asset:worst_decline (ZSet)   -> Ranking of assets with highest % decline. Score: % change (negative).
     *
     * AssetGrowthDTO
     * - String symbol;              // symbol of the asset
     * - Double percentageChange;    // growth or loss in percentage
     * - TimeWindow window;          // time window considered
     *
     * @param declineAssets list of the best decline asset to put in Redis
     */
    private void convertAndSaveWorstDecline(List<AssetGrowthDTO> declineAssets) throws JsonProcessingException {
        // put the value on Redis (Field: Symbol, Value: percentage)
        for (AssetGrowthDTO dto : declineAssets) {
            assetRedisDao.updateWorstDecline(dto.getSymbol(), dto.getPercentageChange());
        }
    }
    //------------------------------------------- end: utilities methods -----------------------------------------------
}
/*
the cron fields represent:
    - sec -> ex 0 is at 0 sec
    - min -> ex 1 is at first minute
    - hours -> ex 0 is the midnight (00)
    - day (of month) -> ex * is all day of the month
    - month -> ex * is all months of the year
    - day (of week) -> ex * is all day of the week
 */
package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.asset_price.AssetPriceAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.asset_price.AssetPriceDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.AssetRedisDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetGrowthDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetStableTrendDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.DailyVolumeDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.mapper.AssetPriceMapper;
import it.unipi.myfuture.myfuture_backend.model.AssetPrice;
import it.unipi.myfuture.myfuture_backend.service.AssetPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * AssetPrice Service implementation.
 * Handles business logic related to historical asset prices.
 */
@Service
public class AssetPriceServiceImpl implements AssetPriceService {

    @Autowired
    private AssetPriceDao assetPriceDao;

    @Autowired
    private AssetPriceAggregationDao assetPriceAggregationDao;
    @Autowired
    private TransactionAggregationDao transactionAggregationDao;

    @Autowired
    private AssetRedisDao assetRedisDao;

    //------------------------------------ start: method for aggregation API -------------------------------------------
    /**
     * Insert or update an asset price entry. Used by admin.
     *
     * @param request asset price data
     * @return saved asset price
     */
    @Override
    public AssetPriceResponseDTO savePrice(AssetPriceRequestDTO request) {

        AssetPrice price = AssetPriceMapper.toEntity(request);  // convert
        AssetPrice savedPrice = assetPriceDao.save(price);      // save

        return AssetPriceMapper.toResponseDTO(savedPrice);
    }

    /**
     * Retrieve asset price history within a date range.
     *
     * @param symbol asset symbol
     * @param from start date
     * @param to end date
     * @return list of asset prices
     */
    @Override
    public List<AssetPriceResponseDTO> getPricesBySymbolAndDateRange(String symbol, Instant from, Instant to) {

        // retrieve all assetPrices and convert in AssetPriceResponseDTO and put in a list
        return assetPriceDao.findBySymbolAndDateRange(symbol, from, to)
                .stream()
                .map(AssetPriceMapper::toResponseDTO)
                .toList();
    }

    /**
     * Retrieve latest available asset price.
     *
     * @param symbol asset symbol
     * @return latest asset price
     */
    @Override
    public AssetPriceResponseDTO getLatestPrice(String symbol) {

        AssetPrice price = assetPriceDao.findLatestBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("No price available"));

        return AssetPriceMapper.toResponseDTO(price);
    }

    /**
     * Delete all asset prices associated with a symbol. Used only for extraordinary administrative operations.
     *
     * @param symbol asset symbol
     */
    @Override
    public void deletePrices(String symbol) {
        assetPriceDao.deleteBySymbol(symbol);   // real delete, not a soft delete
    }
    //------------------------------------- end: method for aggregation API --------------------------------------------

    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * View the top 10 assets with the best growth decline last day/week/month.
     *
     * @param window    time window considered
     * @return list of AssetGrowthDTO containing the result
     */
    @Override
    public List<AssetGrowthDTO> getGrowthAnalytics(TimeWindow window) {
        return assetPriceAggregationDao.findAssetPerformance(window, false);
    }

    /**
     * View the top 10 assets with the best worst decline last day/week/month.
     *
     * @param window    time window considered
     * @return list of AssetGrowthDTO containing the result
     */
    @Override
    public List<AssetGrowthDTO> getWorstAnalytics(TimeWindow window) {
        return assetPriceAggregationDao.findAssetPerformance(window, true);
    }

    /**
     * See the 10 assets that have consistently raisen over the past week and their average daily growth rate.
     *
     * @return list of AssetStableTrendDTO containing the result
     */
    @Override
    public List<AssetStableTrendDTO> getPositiveStableTrendAnalytics() {
        return assetPriceAggregationDao.findConsistentTrendAssets(true);
    }

    /**
     * See the 10 assets that have consistently fell over the past week and their average daily descent rate.
     *
     * @return list of AssetStableTrendDTO containing the result
     */
    @Override
    public List<AssetStableTrendDTO> getNegativeStableTrendAnalytics() {
        return assetPriceAggregationDao.findConsistentTrendAssets(false);
    }

    //-------------------------------------- end: method for aggregation API -------------------------------------------

    //-------------------------------- start: method for save from Redis to MongoDB ------------------------------------

    /**
     * Methd to consolidate intraday Redis data into a MongoDB OHLC record.
     * Take all the value of the current_price on Redis and data volume from MongoDB to aggregate and calculate the
     * daily price data for an asset and put into MongoDB.
     *
     * @param symbol identifier of the asset
     */
    public void consolidateIntradayData(String symbol) {
        // get volume (logic extracted from transactionAggregationDao)
        Double totalVolume = transactionAggregationDao.getDailyVolumeBySymbol()
                .stream()
                .filter(v -> v.getSymbol().equals(symbol))
                .findFirst()
                .map(DailyVolumeDTO::getVolume)
                .orElse(0.0);

        // fetch all intraday prices from Redis ZSet (Score: Timestamp, Member: "Timestamp:Price")
        Set<Object> intradayData = assetRedisDao.getAllIntradayPrices(symbol);

        // control check
        if (intradayData == null || intradayData.isEmpty())
            return;                     // skip there aren't data for today


        List<Double> prices = new ArrayList<>();                    // list of the prices
        // scan all obtained values
        for (Object entry : intradayData) {
            String[] parts = entry.toString().split(":");
            if (parts.length == 2) prices.add(Double.parseDouble(parts[1]));
        }

        // calculate values for asset_prices entity
        if (!prices.isEmpty()) {
            double open = prices.get(0);                    // get open price of the day
            double close = prices.get(prices.size() - 1);   // get close price of the day
            double high = Collections.max(prices);          // get max
            double low = Collections.min(prices);           // get min

            // create asset_prices entity
            AssetPrice historicalPrice = new AssetPrice(LocalDate.now().atStartOfDay(ZoneId.of("America/New_York")).toInstant(),
                    symbol, open, high, low, close, totalVolume.longValue());

            assetPriceDao.save(historicalPrice);        // save aggregated data into MongoDB
            assetRedisDao.clearIntradayData(symbol);    // clean up Redis after saving
        }
    }

    //--------------------------------- end: method for save from Redis to MongoDB -------------------------------------
}
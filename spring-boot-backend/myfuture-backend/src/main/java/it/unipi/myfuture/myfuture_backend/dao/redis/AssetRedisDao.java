package it.unipi.myfuture.myfuture_backend.dao.redis;

import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;

/**
 * Data Access Object for Asset-related data in Redis.
 * KEYSPACE DESIGN:
 * - asset:{type}:list (Hash)               -> Map of all assets of a certain type (Field: symbol, Value: name).
 * - asset:{symbol}:current_price (String)  -> The most recent real-time market price.
 * - asset:{symbol}:intraday_prices (ZSet)  -> Price history of the day. Score: Timestamp, Member: "Timestamp:Price". SEE NOTE 0
 * - asset:most_traded (Hash)               -> Stats of the most traded assets from the previous day (Field: symbol, Value: json).
 * - asset:top_growth (ZSet)                -> Ranking of assets with highest % growth. Score: % change.
 * - asset:worst_decline (ZSet)             -> Ranking of assets with highest % decline. Score: % change (negative).
 *
 * Explanation:
 *  Rankings (most traded, growth, decline) are pre-calculated by an Admin/Batch process at least at the start of each
 *  trading day to provide instant insights without heavy MongoDB aggregations.
 */
@Repository
public class AssetRedisDao {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //-------------------------------------------- start: key formatter ------------------------------------------------

    /**
     * get the corresponding redis key for the list of assets belonging to a type
     *
     * @param type asset type looking for
     * @return redis key
     */
    private String getAssetListByTypeKey(String type) { return "asset:" + type + ":list"; }

    /**
     * get the corresponding redis key for the current price of an asset
     *
     * @param symbol asset identifier
     * @return redis key
     */
    private String getCurrentPriceKey(String symbol) { return "asset:" + symbol + ":current_price"; }

    /**
     * get the corresponding redis key for the intraday price of an asset
     *
     * @param symbol asset identifier
     * @return redis key
     */
    private String getIntradayPriceKey(String symbol) { return "asset:" + symbol + ":intraday_prices"; }

    /**
     * get the corresponding redis key for the most_traded assets
     *
     * @return redis key
     */
    private String getMostTradedKey() { return "asset:most_traded"; }

    /**
     * get the corresponding redis key for the top_growth assets
     *
     * @return redis key
     */
    private String getTopGrowthKey() { return "asset:top_growth"; }

    /**
     * get the corresponding redis key for the worst_decline assets
     *
     * @return redis key
     */
    private String getWorstDeclineKey() { return "asset:worst_decline"; }

    //--------------------------------------------- end: key formatter -------------------------------------------------

    // -------- start: asset list --------

    /**
     * Caches the list of assets for a specific category.
     *
     * @param type category identifier
     * @param symbolToNameMap map of symbol to full name
     */
    public void saveAssetListByType(AssetType type, Map<String, String> symbolToNameMap) {
        redisTemplate.opsForHash().putAll(getAssetListByTypeKey(type.toString()), symbolToNameMap);
    }

    /**
     * Retrieves the full list of assets (symbol and name) for a specific category.
     *
     * @param type category identifier (share, etf, crypto)
     * @return A Map where Key is the symbol and Value is the asset name.
     */
    public Map<Object, Object> getAssetListByType(AssetType type) {
        return redisTemplate.opsForHash().entries(getAssetListByTypeKey(type.toString()));  // get entire list
    }

    /**
     * Retrieves the full name of a specific asset from the cached list.
     *
     * @param type category identifier
     * @param symbol the asset symbol to look for
     * @return The asset name or null if not found
     */
    public String getAssetNameFromList(AssetType type, String symbol) {
        Object name = redisTemplate.opsForHash().get(getAssetListByTypeKey(type.toString()), symbol);   // get symbol
        return name != null ? name.toString() : null;
    }

    /**
     * Delete the list of assets for a specific category.
     *
     * @param type category identifier
     */
    public void deleteAssetListByType(AssetType type) {
        redisTemplate.delete(getAssetListByTypeKey(type.toString()));
    }

    // -------- end: asset list --------

    // -------- start: current price --------
    /**
     * Updates the current real-time price of an asset.
     *
     * @param symbol asset identifier
     * @param price current market price
     */
    public void updateCurrentPrice(String symbol, double price) {
        redisTemplate.opsForValue().set(getCurrentPriceKey(symbol), price);
    }

    /**
     * Retrieves the current price.
     *
     * @param symbol asset identifier
     * @return current price or null if not available
     */
    public Double getCurrentPrice(String symbol) {
        Object price = redisTemplate.opsForValue().get(getCurrentPriceKey(symbol));
        return price != null ? Double.valueOf(price.toString()) : null;
    }
    // -------- end: current price --------

    // -------- start: intraday price --------
    /**
     * Adds a price sample to the intraday ZSet.
     *
     * @param symbol asset identifier
     * @param price price value to store
     */
    public void addIntradayPrice(String symbol, double price) {
        long timestamp = System.currentTimeMillis();        // take current timestamp
        String member = timestamp + ":" + price;            // format the value
        redisTemplate.opsForZSet().add(getIntradayPriceKey(symbol), member, timestamp);             // add to ZSet
    }

    /**
     * Retrieves intraday prices for a time window.
     *
     * @param symbol asset identifier
     * @param minutes range in minutes
     * @return set of timestamped price samples
     */
    public Set<Object> getRecentPrices(String symbol, long minutes) {
        long now = System.currentTimeMillis();          // take the current timestamp
        long start = now - (minutes * 60 * 1000);       // calculate the stat time to retrieve the asst_prices
        return redisTemplate.opsForZSet().rangeByScore(getIntradayPriceKey(symbol), start, now);    // add to ZSet
    }

    /**
     * Retrieves ALL intraday prices collected for the current day.
     * Essential for the End-of-Day consolidation task to calculate OHLC.
     *
     * @param symbol asset identifier
     * @return set of all timestamped price samples "timestamp:price"
     */
    public Set<Object> getAllIntradayPrices(String symbol) {
        // ZRANGE key 0 -1: gets all elements from the first (0) to the last (-1)
        return redisTemplate.opsForZSet().range(getIntradayPriceKey(symbol), 0, -1);
    }

    /**
     * Clears all intraday price history for an asset.
     *
     * @param symbol asset identifier
     */
    public void clearIntradayData(String symbol) {
        redisTemplate.delete(getIntradayPriceKey(symbol));
    }
    // -------- end: intraday price --------

    // -------- start: most growth --------
    /**
     * Updates the ranking for highest growth assets.
     *
     * @param symbol asset identifier
     * @param percentageChange growth percentage
     */
    public void updateTopGrowth(String symbol, double percentageChange) {
        redisTemplate.opsForZSet().add(getTopGrowthKey(), symbol, percentageChange);
    }

    /**
     * Resets top growth.
     */
    public void clearTopGrowth() {
        redisTemplate.delete(getTopGrowthKey());        // reset top-growth-assets
    }

    // -------- end: most growth --------

    // -------- start: worst decline --------
    /**
     * Updates the ranking for worst decline assets.
     *
     * @param symbol asset identifier
     * @param percentageChange decline percentage (negative)
     */
    public void updateWorstDecline(String symbol, double percentageChange) {
        redisTemplate.opsForZSet().add(getWorstDeclineKey(), symbol, percentageChange);
    }

    /**
     * Resets worst decline.
     */
    public void clearWorstDecline() {
        redisTemplate.delete(getWorstDeclineKey());     // reset worst-decline assets
    }

    // -------- end: worst decline --------

    // -------- start: most traded --------
    /**
     * Stores pre-calculated stats for most traded assets.
     *
     * @param symbol asset identifier
     * @param statsJson JSON string with aggregated statistics
     */
    public void updateMostTradedStats(String symbol, String statsJson) {
        redisTemplate.opsForHash().put(getMostTradedKey(), symbol, statsJson);
    }

    /**
     * Stores all pre-calculated stats for most traded assets.
     *
     * @param allStats list of hash of the most asset
     */
    public void updateAllMostTraded(Map<String, String> allStats) {
        redisTemplate.opsForHash().putAll(getMostTradedKey(), allStats);
    }

    /**
     * Resets most traded.
     */
    public void clearMostTraded() {
        redisTemplate.delete(getMostTradedKey());       // reset most-traded assets
    }
    // -------- end: most traded --------

    /**
     * Resets daily rankings and stats.
     */
    public void clearGlobalDailyData() {
        redisTemplate.delete(getTopGrowthKey());        // reset top-growth-assets
        redisTemplate.delete(getWorstDeclineKey());     // reset worst-decline assets
        redisTemplate.delete(getMostTradedKey());       // reset most-traded assets
    }
}
/*
NOTE 0:
    In Redis Sorted Sets (ZSet), members must be unique. If we used only the price as a member, identical prices at
    different times would not create new entries; Redis would simply update the score (timestamp) of the existing member.
    To ensure we store every price point even when the price remains constant, we store a combination of 'timestamp:price'
    as the member. This guarantees uniqueness for every data point while keeping them sorted by the ZSet score (timestamp).
 */
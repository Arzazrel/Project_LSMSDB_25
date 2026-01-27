package it.unipi.myfuture.myfuture_backend.dao.redis;

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
 * - asset:{symbol}:intraday_prices (ZSet)  -> Price history of the day. Score: Timestamp, Member: "Timestamp:Price".
 * - asset:most_traded (Hash)               -> Stats of the most traded assets from the previous day.
 * - asset:top_growth (ZSet)                -> Ranking of assets with highest % growth. Score: % change.
 * - asset:worst_decline (ZSet)             -> Ranking of assets with highest % decline. Score: % change (negative).
 *
 * Explanation:
 *  * Rankings (most traded, growth, decline) are pre-calculated by an Admin/Batch process
 *  * at the start of each trading day to provide instant insights without heavy MongoDB aggregations.
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
     * Updates the ranking for highest growth assets.
     *
     * @param symbol asset identifier
     * @param percentageChange growth percentage
     */
    public void updateTopGrowth(String symbol, double percentageChange) {
        redisTemplate.opsForZSet().add(getTopGrowthKey(), symbol, percentageChange);
    }

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
     * Stores pre-calculated stats for most traded assets.
     *
     * @param symbol asset identifier
     * @param statsJson JSON string with aggregated statistics
     */
    public void updateMostTradedStats(String symbol, String statsJson) {
        redisTemplate.opsForHash().put(getMostTradedKey(), symbol, statsJson);
    }

    /**
     * Caches the list of assets for a specific category.
     *
     * @param type category identifier
     * @param symbolToNameMap map of symbol to full name
     */
    public void saveAssetListByType(String type, Map<String, String> symbolToNameMap) {
        redisTemplate.opsForHash().putAll(getAssetListByTypeKey(type), symbolToNameMap);
    }

    /**
     * Delete the list of assets for a specific category.
     *
     * @param type category identifier
     * @param symbolToNameMap map of symbol to full name
     */
    public void deleteAssetListByType(String type, Map<String, String> symbolToNameMap) {
        redisTemplate.delete(getAssetListByTypeKey(type));
    }

    /**
     * Clears all intraday price history for an asset.
     *
     * @param symbol asset identifier
     */
    public void clearIntradayData(String symbol) {
        redisTemplate.delete(getIntradayPriceKey(symbol));
    }

    /**
     * Resets daily rankings and stats.
     */
    public void clearGlobalDailyData() {
        redisTemplate.delete(getTopGrowthKey());        // reset top-growth-assets
        redisTemplate.delete(getWorstDeclineKey());     // reset worst-decline assets
        redisTemplate.delete(getMostTradedKey());       // reset most-traded assets
    }
}
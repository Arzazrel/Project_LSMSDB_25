package it.unipi.myfuture.myfuture_backend.dao.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Data Access Object for User-related entities in Redis.
 * KEYSPACE DESIGN:
 * - user:{id}:cash (String)            -> Available cash(balance).
 * - user:{id}:cash:blocked (String)    -> Cash/balance reserved for pending orders.
 * - user:{id}:portfolio (Set)          -> List of owned asset symbols (e.g., {"AAPL", "BTC"}).
 * - user:{id}:port:{symbol} (Hash)     -> Details: "quantity", "bep", "blockedQty".
 * All keys have a default TTL of 30 minutes, refreshed on every interaction.
 */
@Repository
public class UserRedisDao {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final long TTL_MINUTES = 30;             // ttl time for key on redis

    //-------------------------------------------- start: key formatter ------------------------------------------------

    /**
     * get the corresponding redis key for the cash by userID
     *
     * @param userId user identifier
     * @return redis key
     */
    private String getCashKey(String userId) { return "user:" + userId + ":cash"; }

    /**
     * get the corresponding redis key for the blocked cash by userID
     *
     * @param userId user identifier
     * @return redis key
     */
    private String getBlockedCashKey(String userId) { return "user:" + userId + ":cash:blocked"; }

    /**
     * get the corresponding redis key for the portfolio symbols by userID
     *
     * @param userId user identifier
     * @return redis key
     */
    private String getPortfolioSetKey(String userId) { return "user:" + userId + ":portfolio"; }

    /**
     * get the corresponding redis key for an assets of the user by userID.
     *
     * @param userId user identifier
     * @param symbol asset identifier
     * @return redis key
     */
    private String getAssetHashKey(String userId, String symbol) { return "user:" + userId + ":port:" + symbol; }
    //--------------------------------------------- end: key formatter -------------------------------------------------

    //-------------------------------------------- start: utils methods ------------------------------------------------

    /**
     * Refreshes the expiration time for all user-related keys.
     * Used when accessing a user value to keep active user information in memory.
     *
     * @param userId user identifier
     */
    public void refreshUserTTL(String userId) {
        String cashKey = getCashKey(userId);                // get key for cash
        String blockedCashKey = getBlockedCashKey(userId);  // get key for blocked cash
        String portfolioKey = getPortfolioSetKey(userId);   // get key for assets list

        redisTemplate.expire(cashKey, TTL_MINUTES, TimeUnit.MINUTES);           // update ttl for cash
        redisTemplate.expire(blockedCashKey, TTL_MINUTES, TimeUnit.MINUTES);    // update ttl for blocked cash
        redisTemplate.expire(portfolioKey, TTL_MINUTES, TimeUnit.MINUTES);      // update ttl for assets list

        // Refresh TTL for each specific asset hash
        Set<Object> symbols = redisTemplate.opsForSet().members(portfolioKey);
        for (Object sym : symbols) {
            redisTemplate.expire(getAssetHashKey(userId, sym.toString()), TTL_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * Deletes all user-related data from Redis (e.g., on logout).
     *
     * @param userId user identifier
     */
    public void clearUserCache(String userId) {
        // take all the assets symbols and delete all portfolio assets information
        Set<Object> symbols = redisTemplate.opsForSet().members(getPortfolioSetKey(userId));
        for (Object sym : symbols) {
            redisTemplate.delete(getAssetHashKey(userId, sym.toString()));
        }

        redisTemplate.delete(getCashKey(userId));               // delete cash
        redisTemplate.delete(getBlockedCashKey(userId));        // delete blocked cash
        redisTemplate.delete(getPortfolioSetKey(userId));       // delete assets list
    }

    //--------------------------------------------- end: utils methods -------------------------------------------------

    //----------------------------------------------- start: methods ---------------------------------------------------
    /**
     * Saves or updates the user's cash balance.
     *
     * @param userId user identifier
     * @param amount quantity to set
     */
    public void saveCash(String userId, double amount) {
        redisTemplate.opsForValue().set(getCashKey(userId), amount, TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Retrieves the available cash balance.
     *
     * @param userId user identifier
     */
    public Double getCash(String userId) {
        Object val = redisTemplate.opsForValue().get(getCashKey(userId));
        return val != null ? Double.valueOf(val.toString()) : null;
    }

    /**
     * Saves or updates the blocked cash balance.
     *
     * @param userId user identifier
     * @param amount quantity to set
     */
    public void saveBlockedCash(String userId, double amount) {
        redisTemplate.opsForValue().set(getBlockedCashKey(userId), amount, TTL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Updates an asset in the user's portfolio. Uses a Set for the list of symbols and a Hash for the specific details.
     *
     * @param userId user identifier
     * @param symbol asset identifier
     * @param qty quantity of the asset for the user
     * @param bep break event point (average buy price) of the asset for the user
     * @param blockedQty blocked quantity of the asset for the user
     */
    public void updatePortfolioAsset(String userId, String symbol, int qty, double bep, int blockedQty) {

        redisTemplate.opsForSet().add(getPortfolioSetKey(userId), symbol);  // add to the set of owned assets

        // update asset details in Hash
        String hashKey = getAssetHashKey(userId, symbol);
        Map<String, String> details = Map.of(
                "quantity", String.valueOf(qty),
                "bep", String.valueOf(bep),
                "blockedQty", String.valueOf(blockedQty)
        );
        redisTemplate.opsForHash().putAll(hashKey, details);

        // ensure TTL is set for the new entries
        redisTemplate.expire(getPortfolioSetKey(userId), TTL_MINUTES, TimeUnit.MINUTES);    // for owned assets
        redisTemplate.expire(hashKey, TTL_MINUTES, TimeUnit.MINUTES);                       // portfolio specific asset
    }

    /**
     * Removes an asset from the portfolio (when quantity becomes 0).
     */
    public void removeAssetFromPortfolio(String userId, String symbol) {
        redisTemplate.opsForSet().remove(getPortfolioSetKey(userId), symbol);   // remove from owned assets
        redisTemplate.delete(getAssetHashKey(userId, symbol));                  // remove from portfolio
    }

    //------------------------------------------------ end: methods ----------------------------------------------------
}

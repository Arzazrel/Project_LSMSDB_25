package it.unipi.myfuture.myfuture_backend.dao.redis;

import it.unipi.myfuture.myfuture_backend.model.User;
import it.unipi.myfuture.myfuture_backend.model.WalletItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
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
     * Populates all Redis keys for a user using a User entity.
     * This is used during login or when a cache miss occurs.
     *
     * @param user The User entity containing all data from MongoDB.
     */
    public void saveFullUserToCache(User user) {
        String id = user.getUserId().toString();

        // save cash and blocked cash
        redisTemplate.opsForValue().set(getCashKey(id), user.getCash(), TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(getBlockedCashKey(id), user.getBlockedCash(), TTL_MINUTES, TimeUnit.MINUTES);

        // save portfolio set - combine the three lists (Shares, ETFs, Cryptocurrencies) to populate Redis.
        List<WalletItem> allItems = new ArrayList<>();
        if (user.getShareWallet() != null) allItems.addAll(user.getShareWallet());
        if (user.getEtfWallet() != null) allItems.addAll(user.getEtfWallet());
        if (user.getCryptoWallet() != null) allItems.addAll(user.getCryptoWallet());

        redisTemplate.delete(getPortfolioSetKey(id));   // clear the old symbol set before repopulating it

        // scroll each portfolio wallet
        for (WalletItem item : allItems) {

            redisTemplate.opsForSet().add(getPortfolioSetKey(id), item.getSymbol());    // add to list of symbol

            // create the hash with the owned assets details - SEE NOTE 0
            Map<String, String> details = new HashMap<>();
            details.put("quantity", String.valueOf(item.getQuantity()));            // add quantity
            details.put("blockedQty", String.valueOf(item.getBlockedQuantity()));   // add blocked quantity
            details.put("bep", String.valueOf(item.getBep()));                      // add bep

            String assetKey = getAssetHashKey(id, item.getSymbol());                // create the key
            redisTemplate.opsForHash().putAll(assetKey, details);                   // put the key and the value
            redisTemplate.expire(assetKey, TTL_MINUTES, TimeUnit.MINUTES);          // set TTL
        }

        redisTemplate.expire(getPortfolioSetKey(id), TTL_MINUTES, TimeUnit.MINUTES);    // set TTl for symbol list
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
     * Saves or updates the cash of the user.
     *
     * @param userId user identifier
     * @param amount quantity to set
     */
    public void saveCash(String userId, double amount) {
        redisTemplate.opsForValue().set(getCashKey(userId), amount, TTL_MINUTES, TimeUnit.MINUTES);

        refreshUserTTL(userId);             // every time update, synchronize the TTL of everything else.
    }

    /**
     * Retrieves the cash of the user.
     *
     * @param userId user identifier
     */
    public Double getCash(String userId) {
        Double cash = (Double)redisTemplate.opsForValue().get(getCashKey(userId));

        if (cash != null)                   // check if exist the key in Redis
            refreshUserTTL(userId);         // every time get, synchronize the TTL of everything else.

        return cash;                        // return the retrieved value or null
    }

    /**
     * Saves or updates the blocked cash of the user.
     *
     * @param userId user identifier
     * @param amount quantity to set
     */
    public void saveBlockedCash(String userId, double amount) {
        redisTemplate.opsForValue().set(getBlockedCashKey(userId), amount, TTL_MINUTES, TimeUnit.MINUTES);

        refreshUserTTL(userId);             // every time update, synchronize the TTL of everything else.
    }

    /**
     * Retrieves the blocked cash of the user.
     *
     * @param userId user identifier
     */
    public Double getBlockedCash(String userId) {
        Double blockedCash = (Double)redisTemplate.opsForValue().get(getBlockedCashKey(userId));

        if (blockedCash != null)                   // check if exist the key in Redis
            refreshUserTTL(userId);         // every time get, synchronize the TTL of everything else.

        return blockedCash;                        // return the retrieved value or null
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

        refreshUserTTL(userId);             // every time update, synchronize the TTL of everything else.
    }

    /**
     * Removes an asset from the portfolio (when quantity becomes 0).
     */
    public void removeAssetFromPortfolio(String userId, String symbol) {
        redisTemplate.opsForSet().remove(getPortfolioSetKey(userId), symbol);   // remove from owned assets
        redisTemplate.delete(getAssetHashKey(userId, symbol));                  // remove from portfolio

        refreshUserTTL(userId);             // every time update, synchronize the TTL of everything else.
    }

    /**
     * Retrieves specific asset details (quantity, bep, blockedQty) from the user's portfolio in Redis.
     *
     * @param userId user identifier
     * @param symbol asset symbol
     * @return a Map containing the asset details, or null if the asset is not in cache.
     */
    public WalletItem getAssetDetails(String userId, String symbol) {
        String assetKey = getAssetHashKey(userId, symbol);                          // get key
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(assetKey); // retrieve all entities from hash
        // assets are empty
        if (entries.isEmpty()) {
            return null;
        }

        // convert the values from String (as saved in saveFullUserToCache) to Double for calculation
        WalletItem details = new WalletItem();
        details.setQuantity(Double.parseDouble((String) entries.get("quantity")));          // get asset quantity
        details.setBep(Double.parseDouble((String) entries.get("bep")));                    // get asset bep
        details.setBlockedQuantity(Double.parseDouble((String) entries.get("blockedQty"))); // get blocked quantity

        refreshUserTTL(userId);                         // refresh TTL cache

        return details;
    }

    //------------------------------------------------ end: methods ----------------------------------------------------
}
/**
 * NOTE 0
 *  All numerical values (cash, quantity, bep) are converted to Strings before being stored in Redis Hash/Strings.
 *  Motivation:
 *  - Readability: Ensures values are human-readable via Redis CLI (e.g., "150.5" instead of JDK binary format).
 *  - Serialization Consistency: Avoids complex configuration of RedisSerializers and prevents "ClassCastException" or
 *                              "ClassNotFound" issues during cross-platform data retrieval.
 *  - Precision: Maintaining values as strings preserves decimal precision during the transfer between MongoDB, Java
 *              (Double), and Redis.
 *  - Interoperability: Standardizes the data format for the Redis Hash structure, which natively treats field values
 *                      as strings/binary.
 *
 */

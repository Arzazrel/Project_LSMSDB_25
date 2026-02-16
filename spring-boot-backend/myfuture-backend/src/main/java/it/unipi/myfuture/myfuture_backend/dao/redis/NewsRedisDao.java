package it.unipi.myfuture.myfuture_backend.dao.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data Access Object for News in Redis.
 * KEYSPACE DESIGN:
 * - news:latest (ZSet)                 -> Global index of news IDs by timestamp.
 * - news:latest:sector:{sector} (ZSet) -> Sector-specific index of news IDs.
 * - news:{newsId} (Hash)               -> title, summary, sector, timestamp.
 */
@Repository
public class NewsRedisDao {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // -------------------------------------------- start: key formatter ------------------------------------------------
    /**
     * get the corresponding redis key for the news list
     *
     * @return redis key
     */
    private String getGlobalNewsKey() { return "news:latest"; }

    /**
     * get the corresponding redis key for the news list belonging to a sector
     *
     * @param sector sector of the news
     * @return redis key
     */
    private String getSectorNewsKey(String sector) { return "news:latest:sector:" + sector; }

    /**
     * get the corresponding redis key for the news information
     *
     * @param newsId news identifier
     * @return redis key
     */
    private String getNewsHashKey(String newsId) { return "news:" + newsId; }
    // --------------------------------------------- end: key formatter -------------------------------------------------

    /**
     * Stores the news metadata and updates both global and sector-specific indexes.
     *
     * @param newsId database identifier
     * @param newsMap map containing title, summary, sector, and timestamp
     */
    public void saveNews(String newsId, Map<String, String> newsMap) {
        long timestamp = Long.parseLong(newsMap.get("timestamp"));      // get timestamp
        String sector = newsMap.get("sector");                          // get sector

        redisTemplate.opsForHash().putAll(getNewsHashKey(newsId), newsMap);             // store the metadata hash

        redisTemplate.opsForZSet().add(getGlobalNewsKey(), newsId, timestamp);          // index in Global ZSet
        redisTemplate.opsForZSet().add(getSectorNewsKey(sector), newsId, timestamp);    // index in Sector ZSet

        // keeps only the last 10 news items (the most recent ones)
        cleanupOldestNews(getGlobalNewsKey());
        cleanupOldestNews(getSectorNewsKey(sector));
    }

    /**
     * Retrieves a list of news details for a specific sector. Based on the sector and counter passed as parameters,
     * it retrieves the IDs of the latest count news belonging to that sector.
     * The IDs will be used to obtain information using the news:{newsId} hash.
     *
     * @param sector the sector to filter by
     * @param count number of items to retrieve
     * @return list of maps containing news details
     */
    public List<Map<Object, Object>> getLatestNewsBySector(String sector, int count) {
        // take the latest count ids from the specific ZSet
        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(getSectorNewsKey(sector), 0, count - 1);  // 0 is the index for the last element
        return fetchNewsDetails(ids);           // retrieve the information about the news from the ids and hash
    }

    /**
     * Retrieves the latest global news details regardless of the sector.
     * Uses the global ZSet index to find the most recent news IDs.
     *
     * @param count number of items to retrieve (usually 10)
     * @return list of maps containing news details
     */
    public List<Map<Object, Object>> getLatestNews(int count) {
        // take the latest count ids from the global ZSet
        Set<Object> ids = redisTemplate.opsForZSet().reverseRange(getGlobalNewsKey(), 0, count - 1);
        return fetchNewsDetails(ids);
    }

    /**
     * Internal helper to fetch multiple hashes from a set of IDs.
     * Transform a list of ids in a list of object (news data).
     *
     * @param ids list of news ids
     * @return a list of object representing the news information
     */
    private List<Map<Object, Object>> fetchNewsDetails(Set<Object> ids) {
        List<Map<Object, Object>> newsList = new ArrayList<>();             // create the new list of hash elements

        // take a list of news id
        if (ids != null) {
            // iterate each ids
            for (Object id : ids) {
                // take all the information for the current id and put them into Map
                Map<Object, Object> details = redisTemplate.opsForHash().entries(getNewsHashKey(id.toString()));

                // control check. If it is empty, it means that the news item has been deleted but its ID has accidentally remained.
                if (!details.isEmpty()) {
                    details.put("id", id);          // add ID to the map to correlate data to the ID
                    newsList.add(details);          // add the map object to the list to return
                }
            }
        }
        return newsList;
    }

    /**
     * Deletes a news item from all Redis structures.
     */
    public void deleteNews(String newsId) {
        String hashKey = getNewsHashKey(newsId);                                    // get hash key for the news
        // retrieve the sector to know which ZSet to clean
        String sector = (String) redisTemplate.opsForHash().get(hashKey, "sector");
        redisTemplate.opsForZSet().remove(getGlobalNewsKey(), newsId);              // remove from global index

        // control check
        if (sector != null)
            redisTemplate.opsForZSet().remove(getSectorNewsKey(sector), newsId);    // remove from index sector

        redisTemplate.delete(hashKey);                                              // remove from hash data
    }

    //------------------------------------------ start: utilities methods ----------------------------------------------
    /**
     * Helper to identify IDs that are outside the top 10 and delete their Hashes.
     * If news:latest (ZSet) you can't delete news information if this news is in the list of the last news for its sector.
     */
    private void cleanupOldestNews(String zsetKey) {
        Long size = redisTemplate.opsForZSet().size(zsetKey);       // get num of element in ZSet
        // control check of ZSet
        if (size != null && size > 10) {
            // get the IDs from index 0 up to the one that makes the 11th element (everything before the last 10)
            Set<Object> idsToRemove = redisTemplate.opsForZSet().range(zsetKey, 0, size - 11);

            // control check
            if (idsToRemove != null && !idsToRemove.isEmpty()) {
                boolean isGlobalStack = zsetKey.equals(getGlobalNewsKey()); // check if delete from general last news

                // scan all elements of the list and remove from news:{newsId} (Hash)
                for (Object id : idsToRemove) {
                    String newsId = id.toString();      // convert news id

                    // case of Global cleanup, check sector before deleting data
                    if (isGlobalStack) {
                        // get the sector of the news
                        String sector = (String) redisTemplate.opsForHash().get(getNewsHashKey(newsId), "sector");
                        // check if the news is still relevant for its sector
                        Double sectorScore = (sector != null) ? redisTemplate.opsForZSet().score(getSectorNewsKey(sector), newsId) : null;
                        // delete hash only if it's not in the sector list
                        if (sectorScore == null) {
                            redisTemplate.delete(getNewsHashKey(newsId));
                        }
                    }
                    else            // case of Sector cleanup no extra hash deletion logic here
                        redisTemplate.delete(getNewsHashKey(newsId));

                    redisTemplate.opsForZSet().remove(zsetKey, newsId); // remove the ID from the current ZSet index
                }
            }
        }
    }

    /**
     * Fully clears all news-related keys. Useful during startup or daily full refreshes.
     */
    public void clearAllNewsData() {
        Set<String> keys = redisTemplate.keys("news:*");    // get all keys starting with news
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);                             // delete all keys
        }
    }
    //------------------------------------------- end: utilities methods -----------------------------------------------
}
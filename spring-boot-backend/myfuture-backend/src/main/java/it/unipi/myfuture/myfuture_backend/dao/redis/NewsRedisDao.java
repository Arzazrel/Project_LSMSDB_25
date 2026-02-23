package it.unipi.myfuture.myfuture_backend.dao.redis;

import it.unipi.myfuture.myfuture_backend.model.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

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

        // save metadata to Hash using Raw Connection to avoid quotes in values
        redisTemplate.execute((RedisConnection connection) -> {
            byte[] hashKey = redisTemplate.getStringSerializer().serialize(getNewsHashKey(newsId));

            Map<byte[], byte[]> rawMap = new HashMap<>();
            newsMap.forEach((k, v) -> {
                if (v != null) {
                    rawMap.put(
                            redisTemplate.getStringSerializer().serialize(k),
                            redisTemplate.getStringSerializer().serialize(v)
                    );
                }
            });

            connection.hMSet(hashKey, rawMap);
            return null;
        });

        // save to ZSets using direct connection to avoid Object serialisation
        redisTemplate.execute((RedisConnection connection) -> {
            byte[] rawId = redisTemplate.getStringSerializer().serialize(newsId);
            byte[] globalKey = redisTemplate.getStringSerializer().serialize(getGlobalNewsKey());

            connection.zAdd(globalKey, (double) timestamp, rawId);                      // add to global key

            // add to sector kay, only if the sector is valid
            if (sector != null && !sector.isEmpty()) {
                byte[] sectorKey = redisTemplate.getStringSerializer().serialize(getSectorNewsKey(sector));
                connection.zAdd(sectorKey, (double) timestamp, rawId);
            }
            return null;
        });

        //cleanupOldestNews(getGlobalNewsKey());          // clean the global key (check if there are more than 10 object)
        //if (sector != null)
        //    cleanupOldestNews(getSectorNewsKey(sector));// clean the sector key (check if there are more than 10 object)
    }

    /**
     * Updates news data in Redis.
     * If the sector has changed, it removes the news ID from the old sector's ZSet before updating the new one.
     *
     * @param newsId database identifier
     * @param newsMap map containing new news data
     * @param oldSector the sector before the update (null if not changed or unknown)
     */
    public void updateNews(String newsId, Map<String, String> newsMap, String oldSector) {
        String newSector = newsMap.get("sector");

        // if the sector has changed, remove the news ID from the old sector ZSet
        if (oldSector != null && !oldSector.equals(newSector)) {
            redisTemplate.execute((RedisConnection connection) -> {
                byte[] rawId = redisTemplate.getStringSerializer().serialize(newsId);
                byte[] oldSectorKey = redisTemplate.getStringSerializer().serialize(getSectorNewsKey(oldSector));
                connection.zRem(oldSectorKey, rawId);                       // remove from the old sector index
                return null;
            });
        }

        // Now save the updated data (this handles Hash and new ZSet entries)
        saveNews(newsId, newsMap);
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
        // take the latest ids from the specific ZSet, execute to fetch IDs as raw bytes and avoid Jackson's JSON parsing error
        Set<Object> ids = redisTemplate.execute((RedisConnection connection) -> {
            byte[] rawKey = redisTemplate.getStringSerializer().serialize(getSectorNewsKey(sector));
            // Fetch last 'count' IDs as raw bytes (from index 0 to count-1 in reverse order)
            Set<byte[]> rawIds = connection.zRevRange(rawKey, 0, count - 1);
            // control check
            if (rawIds == null)
                return new HashSet<>();
            // Convert to Set<Object> to match fetchNewsDetails signature
            return new HashSet<>(rawIds);
        });

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
        // take the latest ids from the specific ZSet, execute to fetch IDs as raw bytes and avoid Jackson's JSON parsing error
        Set<Object> ids = redisTemplate.execute((RedisConnection connection) -> {
            byte[] rawKey = redisTemplate.getStringSerializer().serialize(getGlobalNewsKey());
            // fetch the latest count IDs as raw bytes
            Set<byte[]> rawIds = connection.zRevRange(rawKey, 0, count - 1);
            // control check
            if (rawIds == null)
                return new HashSet<>();
            return new HashSet<>(rawIds);
        });

        return fetchNewsDetails(ids);
    }

    /**
     * Synchronizes the global index with a batch of news.
     *
     * @param newsList List of News entities to be synchronized
     */
    public void syncGlobalNewsBulk(List<News> newsList) {
        // check if the list is null or empty to avoid unnecessary processing
        if (newsList == null || newsList.isEmpty()) return;

        redisTemplate.execute((RedisConnection connection) -> {
            // serialize the key for the global news sorted set
            byte[] globalKey = redisTemplate.getStringSerializer().serialize(getGlobalNewsKey());

            for (int i = 0; i < newsList.size(); i++) {
                News n = newsList.get(i);
                String newsId = n.getId();

                // serialize the news id to be used as value in the sorted set
                byte[] rawId = redisTemplate.getStringSerializer().serialize(newsId);

                // create the specific hash key for this news item like news:id
                byte[] hashKey = redisTemplate.getStringSerializer().serialize(getNewsHashKey(newsId));

                // calculate the base timestamp from the news date
                long baseTimestamp = n.getDate().toEpochMilli();

                // add a small offset based on the loop index to ensure a unique score
                // this prevents redis from reordering news with the same identical date
                // by forcing a deterministic order that matches the mongodb result list
                long uniqueScore = baseTimestamp + (newsList.size() - i);

                // initialize the map to store all news fields as byte arrays
                Map<byte[], byte[]> rawMap = new HashMap<>();

                // add title to the map if it is not null to prevent serialization errors
                if (n.getTitle() != null) {
                    rawMap.put(
                            redisTemplate.getStringSerializer().serialize("title"),
                            redisTemplate.getStringSerializer().serialize(n.getTitle())
                    );
                }

                // add summary to the map only if present since mongodb often has null summaries
                if (n.getSummary() != null) {
                    rawMap.put(
                            redisTemplate.getStringSerializer().serialize("summary"),
                            redisTemplate.getStringSerializer().serialize(n.getSummary())
                    );
                }

                // add sector information to allow the api to display categorical data
                if (n.getSector() != null) {
                    rawMap.put(
                            redisTemplate.getStringSerializer().serialize("sector"),
                            redisTemplate.getStringSerializer().serialize(n.getSector())
                    );
                }

                // store the unique timestamp string in the hash for easy dto conversion
                rawMap.put(
                        redisTemplate.getStringSerializer().serialize("timestamp"),
                        redisTemplate.getStringSerializer().serialize(String.valueOf(uniqueScore))
                );

                // write the entire hash map to redis using hmset for atomic field updates
                if (!rawMap.isEmpty()) {
                    connection.hMSet(hashKey, rawMap);
                }

                // insert or update the news id in the global sorted set with the unique score
                // this ensures the global index always points to existing and updated hash data
                connection.zAdd(globalKey, (double) uniqueScore, rawId);
            }
            return null;
        });
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
                try {
                    String cleanId = null;
                    if (id instanceof byte[]) {
                        // direct conversion from byte to UTF-8 string
                        cleanId = new String((byte[]) id, java.nio.charset.StandardCharsets.UTF_8);
                    } else {
                        // security fallback, remove any JSON residues
                        cleanId = id.toString().replace("\"", "");
                    }
                    // control check
                    if (cleanId != null) {
                        final String finalId = cleanId;     // create an effectively final variable for the lambda
                        // take all the information for the current id and put them into Map
                        Map<Object, Object> details = redisTemplate.execute((RedisConnection connection) -> {
                            byte[] rawKey = redisTemplate.getStringSerializer().serialize(getNewsHashKey(finalId));
                            Map<byte[], byte[]> rawMap = connection.hGetAll(rawKey);

                            Map<Object, Object> result = new HashMap<>();
                            if (rawMap != null && !rawMap.isEmpty()) {
                                rawMap.forEach((k, v) -> {
                                    String field = (String) redisTemplate.getStringSerializer().deserialize(k);
                                    String value = (String) redisTemplate.getStringSerializer().deserialize(v);
                                    result.put(field, value);
                                });
                            }
                            return result;
                        });
                        // control check. If it is empty, it means that the news item has been deleted but its ID has accidentally remained.
                        if (!details.isEmpty()) {
                            details.put("id", cleanId);     // add ID to the map to correlate data to the ID
                            newsList.add(details);          // add the map object to the list to return
                        }
                        else
                            System.out.println("[DEBUG] Hash missing for ID: " + cleanId + " | Expected Key: news:" + cleanId);
                    }
                } catch (Exception e) {
                    System.err.println("[REDIS ERROR] Failed to process ID: " + id + " Error: " + e.getMessage());
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
    public void cleanupOldestNews(String zsetKey) {
        // Use execute to interact directly with the connection and avoid JSON serialization issues
        redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
            byte[] rawZsetKey = redisTemplate.getStringSerializer().serialize(zsetKey);
            Long size = connection.zCard(rawZsetKey); // get num of element in ZSet

            // control check of ZSet
            if (size != null && size > 10) {
                // get the IDs from index 0 up to the one that makes the 11th element (everything before the last 10)
                // fetch them as byte arrays to avoid the "Unexpected character" error
                Set<byte[]> idsToRemove = connection.zRange(rawZsetKey, 0, size - 11);

                // control check
                if (idsToRemove != null && !idsToRemove.isEmpty()) {
                    boolean isGlobalStack = zsetKey.equals(getGlobalNewsKey()); // check if delete from general last news

                    // scan all elements of the list and remove from news:{newsId} (Hash)
                    for (byte[] rawId : idsToRemove) {
                        // convert the raw byte ID to a clean String
                        String newsId = new String(rawId, java.nio.charset.StandardCharsets.UTF_8);
                        byte[] rawHashKey = redisTemplate.getStringSerializer().serialize(getNewsHashKey(newsId));

                        boolean shouldDeleteHash = false;                               // safety check
                        // case of Global cleanup, check sector before deleting data
                        if (isGlobalStack) {
                            // We are cleaning the Global index.
                            // Delete Hash ONLY IF it's not in its Sector index.
                            byte[] sectorField = redisTemplate.getStringSerializer().serialize("sector");
                            byte[] rawSector = connection.hGet(rawHashKey, sectorField);

                            if (rawSector != null) {
                                String sector = new String(rawSector, java.nio.charset.StandardCharsets.UTF_8);
                                byte[] rawSectorKey = redisTemplate.getStringSerializer().serialize(getSectorNewsKey(sector));
                                // Check if it still exists in the sector ZSet
                                if (connection.zScore(rawSectorKey, rawId) == null) {
                                    shouldDeleteHash = true;
                                }
                            } else {
                                // No sector found, safe to delete
                                shouldDeleteHash = true;
                            }
                        } else {
                            // We are cleaning a Sector index.
                            // Delete Hash ONLY IF it's not in the Global index.
                            byte[] globalKey = redisTemplate.getStringSerializer().serialize(getGlobalNewsKey());
                            if (connection.zScore(globalKey, rawId) == null) {
                                shouldDeleteHash = true;
                            }
                        }

                        // 3. Final execution of deletion
                        if (shouldDeleteHash) {
                            connection.del(rawHashKey);
                            // Optional: System.out.println("[REDIS CLEANUP] Evicted data for: " + newsId);
                        }
                        // remove the ID from the current ZSet using the raw connection
                        connection.zRem(rawZsetKey, rawId);
                    }
                }
            }
            return null;
        });
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
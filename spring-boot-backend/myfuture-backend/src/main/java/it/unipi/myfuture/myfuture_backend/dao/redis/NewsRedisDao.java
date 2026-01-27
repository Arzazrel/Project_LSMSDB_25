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
}
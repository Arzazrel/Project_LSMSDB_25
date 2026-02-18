package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.news.NewsAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.news.NewsDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.NewsRedisDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorNewsCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TopMentionedAssetDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.NewsMapper;
import it.unipi.myfuture.myfuture_backend.model.News;
import it.unipi.myfuture.myfuture_backend.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NewsService implementation.
 */
@Service
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsDao newsDao;

    @Autowired
    private NewsAggregationDao newsAggregationDao;

    @Autowired
    private NewsRedisDao newsRedisDao;

    //----------------------------------------- start: method for CRUD API ---------------------------------------------
    /**
     * Create a news entry. After saving to MongoDB, it updates the Redis cache. Used by admin.
     *
     * @param requestDTO data of the news to be created or updated
     * @return saved news as response DTO
     */
    @Override
    public NewsResponseDTO saveNews(NewsRequestDTO requestDTO) {

        News savedNews = newsDao.save(NewsMapper.toEntity(requestDTO));             // save to MongoDB
        newsRedisDao.saveNews(savedNews.getId(), NewsMapper.toRedisMap(savedNews)); // update Redis

        return NewsMapper.toResponseDTO(savedNews);                                 // return the response DTO
    }

    /**
     * Update a news entry. After saving to MongoDB, it updates the Redis cache. Used by admin.
     *
     * @param id news identifier
     * @param requestDTO data of the news to be updated
     * @return
     */
    @Override
    public NewsResponseDTO updateNews(String id, NewsRequestDTO requestDTO) {
        // check if the news exist
        News existingNews = newsDao.findById(id)
                .orElseThrow(() -> new BusinessException("Cannot update: News not found with id " + id));

        String oldSector = existingNews.getSector();            // get the sector of the news before the update

        // map new data keeping the original ID
        News newsToUpdate = NewsMapper.toEntity(requestDTO);
        newsToUpdate.setId(id);                                 // set id

        News updatedNews = newsDao.save(newsToUpdate);          // save on MongoDB

        newsRedisDao.updateNews(updatedNews.getId(), NewsMapper.toRedisMap(updatedNews), oldSector);    // Redis update

        return NewsMapper.toResponseDTO(updatedNews);
    }

    /**
     * Retrieve an active news by its ID. Used by users and customers.
     *
     * @param id MongoDB identifier
     * @return news DTO
     * @throws RuntimeException if news is not found or soft-deleted
     */
    @Override
    public NewsResponseDTO getActiveNewsById(String id) {

        return NewsMapper.toResponseDTO(
                newsDao.findById(id).orElseThrow(() -> new BusinessException("News not found or deleted"))
        );
    }

    /**
     * Retrieve all active (non-deleted) news. Used by users and customers.
     *
     * @return list of active news DTOs
     */
    @Override
    public List<NewsResponseDTO> getAllActiveNews() {

        return newsDao.findAllActive()
                .stream()
                .map(NewsMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all news, including soft-deleted ones. Used by admin.
     *
     * @return list of all news DTOs
     */
    @Override
    public List<NewsResponseDTO> getAllNews() {

        return newsDao.findAll()
                .stream()
                .map(NewsMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve active news filtered by sector. Used by users and customers.
     *
     * @param sector sector name
     * @return list of news DTOs
     */
    @Override
    public List<NewsResponseDTO> getActiveNewsBySector(String sector) {

        return newsDao.findBySectorActive(sector)
                .stream()
                .map(NewsMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Soft delete a news entry. Used by admin.
     *
     * @param id MongoDB identifier
     */
    @Override
    public void deleteNews(String id) {
        // control check
        if (newsDao.existsById(id)) {
            throw new BusinessException("Cannot delete: News not found with id " + id);
        }
        newsRedisDao.deleteNews(id);    // delete from redis
        newsDao.softDelete(id);         // soft delete the retrieved news
    }

    //------------------------------------------ end: method for CRUD API ----------------------------------------------

    //--------------------------------------- start: paginated news methods --------------------------------------------
    /**
     * Retrieves a paginated list of active news using offset and limit. (user)
     * If offset is 0 and limit is <= 10, it attempts to fetch data from Redis cache.
     *
     * @param offset the number of news to skip
     * @param limit the maximum number of news to return
     * @return list of active news DTOs
     */
    @Override
    public List<NewsResponseDTO> getLimitActiveNews(int offset, int limit){
        // Cache Layer: Try Redis for the initial feed
        if (offset == 0 && limit <= 10) {
            List<Map<Object, Object>> rawNews = newsRedisDao.getLatestNews(limit);
            if (!rawNews.isEmpty()) {
                return rawNews.stream()
                        .map(NewsMapper::fromRedisMap)
                        .toList();
            }
        }
        // DB layer: Fallback to MongoDB for deeper pages or cache miss
        return newsDao.findLatestActive(offset, limit).stream()
                .map(NewsMapper::toResponseDTO)
                .toList();
    }

    /**
     * Retrieves a paginated list of all news (including soft-deleted) using offset and limit. (admin only)
     * Primarily used for administrative purposes.
     *
     * @param offset the number of news to skip
     * @param limit the maximum number of news to return
     * @return list of all news DTOs
     */
    @Override
    public List<NewsResponseDTO> getLimitNews(int offset, int limit){
        return newsDao.findLatest(offset, limit).stream()
                .map(NewsMapper::toResponseDTO)
                .toList();
    }

    /**
     * Retrieves a paginated list of active news filtered by sector using offset and limit. (customer)
     * Uses Redis cache for the first 10 items if available.
     *
     * @param sector the market sector to filter by
     * @param offset the number of news to skip
     * @param limit the maximum number of news to return
     * @return list of active news DTOs for the specified sector
     */
    @Override
    public List<NewsResponseDTO> getLimitActiveNewsBySector(String sector, int offset, int limit){
        if (offset == 0 && limit <= 10) {
            List<Map<Object, Object>> rawNews = newsRedisDao.getLatestNewsBySector(sector,limit);
            if (!rawNews.isEmpty()) {
                return rawNews.stream()
                        .map(NewsMapper::fromRedisMap)
                        .toList();
            }
        }
        return newsDao.findLatestBySectorActive(sector, offset, limit).stream()
                .map(NewsMapper::toResponseDTO)
                .toList();
    }

    /**
     * Retrieves a paginated list of news for a specific sector, including deleted ones. (admin only)
     *
     * @param sector the market sector to filter by
     * @param offset the number of news to skip
     * @param limit the maximum number of news to return
     * @return list of all news DTOs for the specified sector
     */
    @Override
    public List<NewsResponseDTO> getLimitNewsBySector(String sector, int offset, int limit){
        return newsDao.findLatestBySector(sector, offset, limit).stream()
                .map(NewsMapper::toResponseDTO)
                .toList();
    }
    //---------------------------------------- end: paginated news methods ---------------------------------------------

    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * Retrieves the count of news articles per sector for a specific time window.
     */
    @Override
    public List<SectorNewsCountDTO> getNewsCountBySector(TimeWindow window) {
        // Business logic: Analyze news distribution across market sectors
        return newsAggregationDao.countNewsBySector(window);
    }

    /**
     * Retrieves the top 5 companies most mentioned in recent news articles.
     */
    @Override
    public List<TopMentionedAssetDTO> getTopMentionedCompanies(TimeWindow window) {
        // Business logic: Identify trending companies based on media coverage
        return newsAggregationDao.findTopMentionedAssets(window);
    }

    //------------------------------------- end: method for aggregation API --------------------------------------------
}
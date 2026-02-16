package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorNewsCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TopMentionedAssetDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsRequestDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;

import java.util.List;

/**
 * Service interface for News domain. Defines the business operations related to financial news.
 * (Controllers interact ONLY with this interface layer)
 */
public interface NewsService {

    //----------------------------------------- start: method for CRUD API ---------------------------------------------
    /**
     * Create or update a news entry. Used by admin.
     *
     * @param requestDTO data of the news to be created or updated
     * @return saved news as response DTO
     */
    NewsResponseDTO saveNews(NewsRequestDTO requestDTO);

    /**
     * Retrieve an active news by its ID. Used by users and customers.
     *
     * @param id MongoDB identifier
     * @return news DTO
     * @throws RuntimeException if news is not found or soft-deleted
     */
    NewsResponseDTO getActiveNewsById(String id);

    /**
     * Retrieve all active (non-deleted) news. Used by users and customers.
     *
     * @return list of active news DTOs
     */
    List<NewsResponseDTO> getAllActiveNews();

    /**
     * Retrieve all news, including soft-deleted ones. Used by admin.
     *
     * @return list of all news DTOs
     */
    List<NewsResponseDTO> getAllNews();

    /**
     * Retrieve active news filtered by sector. Used by users and customers.
     *
     * @param sector sector name
     * @return list of news DTOs
     */
    List<NewsResponseDTO> getActiveNewsBySector(String sector);

    /**
     * Soft delete a news entry. Used by admin.
     *
     * @param id MongoDB identifier
     */
    void deleteNews(String id);

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
    List<NewsResponseDTO> getLimitActiveNews(int offset, int limit);

    /**
     * Retrieves a paginated list of all news (including soft-deleted) using offset and limit. (admin only)
     * Primarily used for administrative purposes.
     *
     * @param offset the number of news to skip
     * @param limit the maximum number of news to return
     * @return list of all news DTOs
     */
    List<NewsResponseDTO> getLimitNews(int offset, int limit);

    /**
     * Retrieves a paginated list of active news filtered by sector using offset and limit. (customer)
     * Uses Redis cache for the first 10 items if available.
     *
     * @param sector the market sector to filter by
     * @param offset the number of news to skip
     * @param limit the maximum number of news to return
     * @return list of active news DTOs for the specified sector
     */
    List<NewsResponseDTO> getLimitActiveNewsBySector(String sector, int offset, int limit);

    /**
     * Retrieves a paginated list of news for a specific sector, including deleted ones. (admin only)
     *
     * @param sector the market sector to filter by
     * @param offset the number of news to skip
     * @param limit the maximum number of news to return
     * @return list of all news DTOs for the specified sector
     */
    List<NewsResponseDTO> getLimitNewsBySector(String sector, int offset, int limit);
    //---------------------------------------- end: paginated news methods ---------------------------------------------

    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * Retrieves the count of news articles per sector for a specific time window.
     */
    List<SectorNewsCountDTO> getNewsCountBySector(TimeWindow window);

    /**
     * Retrieves the top 5 companies most mentioned in recent news articles.
     */
    List<TopMentionedAssetDTO> getTopMentionedCompanies(TimeWindow window);
    //------------------------------------- end: method for aggregation API --------------------------------------------
}

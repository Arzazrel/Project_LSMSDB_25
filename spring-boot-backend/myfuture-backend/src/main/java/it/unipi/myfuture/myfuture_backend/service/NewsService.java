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

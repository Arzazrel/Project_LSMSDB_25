package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.news.NewsAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.news.NewsDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorNewsCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TopMentionedAssetDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.NewsMapper;
import it.unipi.myfuture.myfuture_backend.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

    //----------------------------------------- start: method for CRUD API ---------------------------------------------
    /**
     * Create or update a news entry. Used by admin.
     *
     * @param requestDTO data of the news to be created or updated
     * @return saved news as response DTO
     */
    @Override
    public NewsResponseDTO saveNews(NewsRequestDTO requestDTO) {

        return NewsMapper.toResponseDTO(
                newsDao.save(NewsMapper.toEntity(requestDTO))
        );
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
        newsDao.softDelete(id);         // soft delete the retrieved news
    }

    //------------------------------------------ end: method for CRUD API ----------------------------------------------

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
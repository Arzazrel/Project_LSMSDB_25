package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.NewsDao;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
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

    // ----------------------------------------------- news API --------------------------------------------------

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
                newsDao.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("News not found or deleted"))
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
        newsDao.softDelete(id);
    }
}
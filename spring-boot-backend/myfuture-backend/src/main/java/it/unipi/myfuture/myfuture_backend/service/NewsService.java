package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dao.mongo.NewsDao;
import it.unipi.myfuture.myfuture_backend.model.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for News entity.
 *
 * Manages news browsing, filtering and administrative operations.
 */
@Service
public class NewsService {

    @Autowired
    private NewsDao newsDao;

    /**
     * Insert or update news.
     *
     * @param news news to save
     * @return saved news
     */
    public News saveNews(News news) {
        return newsDao.save(news);
    }

    /**
     * Retrieve active news by ID.
     *
     * @param id MongoDB ID
     * @return Optional containing the news if found
     */
    public Optional<News> getNewsById(String id) {
        return newsDao.findById(id);
    }

    /**
     * Retrieve all active news.
     *
     * @return list of news
     */
    public List<News> getAllNews() {
        return newsDao.findAllActive();
    }

    /**
     * Retrieve news by sector.
     *
     * @param sector sector name
     * @return list of news
     */
    public List<News> getNewsBySector(String sector) {
        return newsDao.findBySector(sector);
    }

    /**
     * Retrieve all news including soft-deleted ones (admin).
     *
     * @return list of all news
     */
    public List<News> getAllNewsAdmin() {
        return newsDao.findAllAdmin();
    }

    /**
     * Soft delete a news document.
     *
     * @param id MongoDB ID
     */
    public void deleteNews(String id) {
        newsDao.softDelete(id);
    }
}

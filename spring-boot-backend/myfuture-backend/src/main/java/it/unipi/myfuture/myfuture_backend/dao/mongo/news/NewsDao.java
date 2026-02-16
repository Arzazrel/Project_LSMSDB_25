package it.unipi.myfuture.myfuture_backend.dao.mongo.news;

import it.unipi.myfuture.myfuture_backend.model.Asset;
import it.unipi.myfuture.myfuture_backend.model.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for News collection. Manage persistence and queries for financial news.
 *
 * Collection: news
 */
@Repository
public class NewsDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Insert or update news.
     *
     * @param news the news to be entered(saved)
     * @return the inserted object
     */
    public News save(News news) {
        return mongoTemplate.save(news);
    }

    /**
     * Retrieves an active news document by its MongoDB ID.
     * This method is for registered users and unregistered users.
     * Soft-deleted news (deleted = true) are excluded from the result.
     *
     * @param id MongoDB _id of the news
     * @return Optional containing the News if found and not deleted, otherwise empty
     */
    public Optional<News> findByIdActive(String id) {
        Query query = new Query(
                Criteria.where("_id").is(id)
                        .and("deleted").ne(true)
        );
        return Optional.ofNullable(mongoTemplate.findOne(query, News.class));
    }

    /**
     * Retrieves a news document by its MongoDB ID (also soft-deleted entries).
     * This method is only for administrators, who are allowed to access also soft-deleted news.
     *
     * @param id MongoDB _id of the news
     * @return Optional containing the News if found, otherwise empty
     */
    public Optional<News> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, News.class));
    }

    /**
     * Retrieves all active (non-deleted) news documents.
     * Used to populate the news list for users and unregistered users. Soft-deleted news are excluded.
     *
     * @return list of active News documents
     */
    public List<News> findAllActive() {
        Query query = new Query(Criteria.where("deleted").ne(true));
        return mongoTemplate.find(query, News.class);
    }

    /**
     * Retrieves all news documents without any filtering (both active and soft-deleted news). Admin only.
     *
     * @return list of all News documents
     */
    public List<News> findAll() {
        return mongoTemplate.findAll(News.class);
    }

    /**
     * Retrieves all active (non-deleted) news belonging to a specific sector.
     * This method supports filtering news by sector for users and unregistered visitors (soft-deleted news are excluded).
     *
     * @param sector sector name used to filter news
     * @return list of active News documents matching the given sector
     */
    public List<News> findBySectorActive(String sector) {
        Query query = new Query(
                Criteria.where("sector").is(sector)
                        .and("deleted").ne(true)
        );
        return mongoTemplate.find(query, News.class);
    }

    /**
     * Retrieves all active news belonging to a specific sector. This method supports filtering news by sector for admin.
     *
     * @param sector sector name used to filter news
     * @return list of active News documents matching the given sector
     */
    public List<News> findBySector(String sector) {
        Query query = new Query(
                Criteria.where("sector").is(sector)
        );
        return mongoTemplate.find(query, News.class);
    }


    /**
     * Performs a soft delete on a news document. Instead of physically removing the document from the database,
     * the news is marked as deleted and the deletion timestamp is stored. This operation is only for admin.
     *
     * @param id MongoDB _id of the news to be soft-deleted
     */
    public void softDelete(String id) {
        News news = mongoTemplate.findById(id, News.class);     // get the news
        if (news != null)                                       // control check
        {
            news.setDeleted(true);
            news.setDeletedAt(Instant.now());
            mongoTemplate.save(news);                           // save changed data for the news
        }
    }

    /**
     * Undo Soft delete a news by symbol.
     * Restores a previously soft-deleted news by resetting the deleted flag and removing the deletion timestamp.
     *
     * @param id the id that identify the news
     */
    public void undoSoftDelete(String id) {
        Query query = new Query(
                Criteria.where("_id").is(id)
                        .and("deleted").is(true)
        );

        Update update = new Update()
                .set("deleted", false)
                .unset("deletedAt");

        mongoTemplate.updateFirst(query, update, News.class);
    }

    /**
     * Check if a news identified by id exist or not
     *
     * @param id the id that identify the news
     * @return true if the news exist or false if th news doesn't exist
     */
    public boolean existsById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        return mongoTemplate.exists(query, News.class);
    }

    /**
     * Retrieves the most recent news articles that have not been soft-deleted. (user)
     *
     * @param offset the number of documents to skip
     * @param limit the maximum number of documents to return
     * @return a list of active News entities
     */
    public List<News> findLatestActive(int offset, int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("deleted").is(false));
        query.with(Sort.by(Sort.Direction.DESC, "date"));
        query.skip(offset);                                                 // get news start from offset
        query.limit(limit);                                                 // get news until limit
        return mongoTemplate.find(query, News.class);
    }

    /**
     * Retrieves the most recent news articles, including those that are soft-deleted. (admin only)
     *
     * @param offset the number of documents to skip
     * @param limit the maximum number of documents to return
     * @return a list of all News entities
     */
    public List<News> findLatest(int offset, int limit) {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "date"));
        query.skip(offset);                                                 // get news start from offset
        query.limit(limit);                                                 // get news until limit
        return mongoTemplate.find(query, News.class);
    }

    /**
     * Retrieves the most recent active news articles filtered by market sector. (customer)
     *
     * @param sector the market sector to filter by
     * @param offset number of documents to skip
     * @param limit maximum number of documents to return
     */
    public List<News> findLatestBySectorActive(String sector, int offset, int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("deleted").is(false).and("sector").is(sector));
        query.with(Sort.by(Sort.Direction.DESC, "date"));
        query.skip(offset);                                                 // get news start from offset
        query.limit(limit);                                                 // get news until limit
        return mongoTemplate.find(query, News.class);
    }

    /**
     * Retrieves the most recent news articles for a sector, including deleted ones. (admin only)
     *
     * @param sector the market sector to filter by
     * @param offset number of documents to skip
     * @param limit maximum number of documents to return
     */
    public List<News> findLatestBySector(String sector, int offset, int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("sector").is(sector));
        query.with(Sort.by(Sort.Direction.DESC, "date"));
        query.skip(offset);                                                 // get news start from offset
        query.limit(limit);                                                 // get news until limit
        return mongoTemplate.find(query, News.class);
    }
}
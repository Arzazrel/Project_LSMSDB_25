package it.unipi.myfuture.myfuture_backend.dao.mongo.user;

import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import it.unipi.myfuture.myfuture_backend.model.News;
import it.unipi.myfuture.myfuture_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User collection. Manage persistence and queries for user.
 *
 * Collection: users
 */
@Repository
public class UserDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Save or update a user.
     *
     * @param user the user to be entered(saved)
     * @return the inserted object
     */
    public User save(User user) {
        return mongoTemplate.save(user);
    }

    /**
     * Find a user by application-level userId (user must not have been deleted).
     *
     * @param userId ID for the user
     * @return Optional containing the user if found, otherwise empty
     */
    public Optional<User> findByUserId(Long userId) {
        Query query = new Query(
                Criteria.where("user_id").is(userId)
                        .and("deleted").ne(true)
                        .and("suspended").ne(true)
        );
        return Optional.ofNullable(mongoTemplate.findOne(query, User.class));
    }

    /**
     * Find a user by email (used for login). The user must not have been deleted.
     *
     * @param email email related to the user account (used for login)
     * @return Optional containing the user if found, otherwise empty
     */
    public Optional<User> findByEmail(String email) {
        Query query = new Query(
                Criteria.where("email").is(email)
                        .and("deleted").ne(true)
                        .and("suspended").ne(true)
        );
        return Optional.ofNullable(mongoTemplate.findOne(query, User.class));
    }

    /**
     * Retrieve all active users (admin usage).
     *
     * @return list of the users
     */
    public List<User> findAllActive() {
        Query query = new Query(
                Criteria.where("deleted").ne(true)
                        .and("suspended").ne(true)
        );
        return mongoTemplate.find(query, User.class);
    }

    /**
     * Soft delete a user.
     *
     * @param userId id of the user
     */
    public void softDelete(Long userId) {
        Query query = new Query(Criteria.where("user_id").is(userId));
        User user = mongoTemplate.findOne(query, User.class);

        if (user != null) {
            user.setDeleted(true);
            user.setDeletedAt(java.time.Instant.now());
            mongoTemplate.save(user);
        }
    }

    /**
     * Undo Soft delete a user by symbol.
     * Restores a previously soft-deleted user by resetting the deleted flag and removing the deletion timestamp.
     *
     * @param userId the id that identify the news
     */
    public void undoSoftDelete(Long userId) {
        Query query = new Query(
                Criteria.where("user_id").is(userId)
                        .and("deleted").is(true)
        );

        Update update = new Update()
                .set("deleted", false)
                .unset("deletedAt");

        mongoTemplate.updateFirst(query, update, User.class);
    }

    /**
     * Suspend a user account.
     *
     * @param userId id of the user
     * @param reason contain the data and the reason of the suspension
     */
    public void suspendUser(Long userId, SuspendReason reason) {
        Query query = new Query(Criteria.where("user_id").is(userId));
        User user = mongoTemplate.findOne(query, User.class);

        if (user != null) {
            user.setSuspended(true);
            user.getSuspensionInfo().setSuspendedAt(java.time.Instant.now());
            user.getSuspensionInfo().setSuspendReason(reason);
            mongoTemplate.save(user);
        }
    }

    /**
     * Remove suspension from a user. Used by admin operations.
     */
    public void undoSuspendUser(Long userId) {

        Query query = new Query(
                Criteria.where("user_id").is(userId)
                        .and("suspended").is(true)
        );

        Update update = new Update()
                .set("suspended", false)
                .unset("suspensionInfo");

        mongoTemplate.updateFirst(query, update, User.class);
    }

}
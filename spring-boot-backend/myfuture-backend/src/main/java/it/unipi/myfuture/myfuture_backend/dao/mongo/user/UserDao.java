package it.unipi.myfuture.myfuture_backend.dao.mongo.user;

import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import it.unipi.myfuture.myfuture_backend.model.SuspensionInfo;
import it.unipi.myfuture.myfuture_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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
        user.setUpdateAt(Instant.now());
        return mongoTemplate.save(user);
    }

    /**
     * Find a user by application-level userId (admin).
     *
     * @param userId ID for the user
     * @return Optional containing the user if found, otherwise empty
     */
    public Optional<User> findByUserIdActive(Long userId) {
        Query query = new Query(
                Criteria.where("user_id").is(userId)
                        .and("deleted").ne(true)
                        .and("suspended").ne(true)
        );
        return Optional.ofNullable(mongoTemplate.findOne(query, User.class));
    }

    /**
     * Find any user by application-level userId (admin).
     * Used for admin operations or system processes that need to handle suspended/deleted accounts.
     */
    public Optional<User> findByUserId(Long userId) {
        Query query = new Query(Criteria.where("user_id").is(userId));
        return Optional.ofNullable(mongoTemplate.findOne(query, User.class));
    }

    /**
     * Find a user by email (used for login). The user must not have been deleted.
     *
     * @param email email related to the user account (used for login)
     * @return Optional containing the user if found, otherwise empty
     */
    public Optional<User> findByEmailActive(String email) {
        Query query = new Query(
                Criteria.where("email").is(email)
                        .and("deleted").ne(true)
                        .and("suspended").ne(true)
        );
        return Optional.ofNullable(mongoTemplate.findOne(query, User.class));
    }

    /**
     * Find any user by email.
     *
     * @param email email related to the user account (used for login)
     * @return Optional containing the user if found, otherwise empty
     */
    public Optional<User> findByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return Optional.ofNullable(mongoTemplate.findOne(query, User.class));
    }

    /**
     * Retrieve all users in the system, including those who are suspended or soft-deleted. (admin only)
     *
     * @return list of all users in the collection
     */
    public List<User> findAllUsers() {
        return mongoTemplate.findAll(User.class);
    }

    /**
     * Retrieve only users that have been soft-deleted. (admin only, for management)
     *
     * @return list of soft-deleted users
     */
    public List<User> findDeletedUsers() {
        Query query = new Query(Criteria.where("deleted").is(true));
        return mongoTemplate.find(query, User.class);
    }

    /**
     * Retrieve only users that are currently suspended. (admin only, monitoring and management)
     *
     * @return list of suspended users
     */
    public List<User> findSuspendedUsers() {
        Query query = new Query(Criteria.where("suspended").is(true));
        return mongoTemplate.find(query, User.class);
    }

    /**
     * Soft delete a user. (admin only)
     *
     * @param userId id of the user
     */
    public void softDelete(Long userId) {
        Query query = new Query(Criteria.where("user_id").is(userId));
        User user = mongoTemplate.findOne(query, User.class);

        if (user != null) {
            user.setDeleted(true);
            user.setDeletedAt(java.time.Instant.now());
            user.setUpdateAt(java.time.Instant.now());
            mongoTemplate.save(user);
        }
    }

    /**
     * Undo Soft delete a user by id. (admin only)
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
                .unset("deletedAt")
                .set("updateAt", java.time.Instant.now());

        mongoTemplate.updateFirst(query, update, User.class);
    }

    /**
     * Suspend a user account. (admin only)
     *
     * @param userId id of the user
     * @param reason contain the data and the reason of the suspension
     */
    public void suspendUser(Long userId, SuspendReason reason) {
        Query query = new Query(Criteria.where("user_id").is(userId));
        User user = mongoTemplate.findOne(query, User.class);

        if (user != null) {
            user.setSuspended(true);
            // control check for null SuspensionInfo, case of user never suspended before
            if (user.getSuspensionInfo() == null)
                user.setSuspensionInfo(new SuspensionInfo());

            user.getSuspensionInfo().setSuspendedAt(Instant.now());
            user.getSuspensionInfo().setSuspendReason(reason);
            user.setUpdateAt(Instant.now());
            mongoTemplate.save(user);
        }
    }

    /**
     * Remove suspension from a user. Used by admin operations. (admin only)
     *
     * @param userId id of the user
     */
    public void undoSuspendUser(Long userId) {

        Query query = new Query(
                Criteria.where("user_id").is(userId)
                        .and("suspended").is(true)
        );

        Update update = new Update()
                .set("suspended", false)
                .unset("suspensionInfo")
                .set("updateAt", java.time.Instant.now());

        mongoTemplate.updateFirst(query, update, User.class);
    }

}
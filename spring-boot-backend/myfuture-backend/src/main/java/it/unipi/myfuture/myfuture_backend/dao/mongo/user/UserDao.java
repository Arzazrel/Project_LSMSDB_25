package it.unipi.myfuture.myfuture_backend.dao.mongo.user;

import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.model.SuspensionInfo;
import it.unipi.myfuture.myfuture_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
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
        user.setUpdatedAt(Instant.now());
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
            user.setUpdatedAt(java.time.Instant.now());
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
                .set("updatedAt", Instant.now());

        mongoTemplate.updateFirst(query, update, User.class);
    }

    /**
     * Suspend a user account. (admin only)
     *
     * @param userId id of the user
     * @param reason contain the data and the reason of the suspension
     */
    public void suspendUser(Long userId, SuspendReason reason, Instant timestamp) {

        // Retrieve user
        User user = findByUserId(userId).orElseThrow(() -> new BusinessException("User not found"));

        if (user != null) {
            // check if the user is suspended
            if (Boolean.TRUE.equals(user.getSuspended())) {
                throw new BusinessException("User is already suspended");
            }

            // Create suspension info
            SuspensionInfo suspensionInfo = new SuspensionInfo();
            suspensionInfo.setSuspendReason(reason);
            suspensionInfo.setSuspendedAt(timestamp != null ? timestamp : Instant.now());

            // Update user suspension state
            user.setSuspended(true);
            user.setSuspensionInfo(suspensionInfo);
            user.setUpdatedAt(Instant.now());
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
                .set("updatedAt", Instant.now());

        mongoTemplate.updateFirst(query, update, User.class);
    }

    //--------------------------------------- start: methods for analytics ---------------------------------------------
    /**
     * counts the total number of users in the collection.
     *
     * @return total count
     */
    public long countAll() {
        return mongoTemplate.count(new Query(), User.class);
    }

    //---------------------------------------- end: methods for analytics ----------------------------------------------

    //-------------------------------------- start: methods for atomic update ------------------------------------------
    //------------------------------------------ start: methods for cash -----------------------------------------------

    /**
     * Updates user cash atomically for a purchase.
     * Checks if (cash - blockedCash) >= amount before subtracting.
     *
     * @param userId the user identifier
     * @param amount the positive amount to subtract from cash
     * @return true if the update was successful, false if insufficient funds
     */
    public User subtractCashAtomic(Long userId, double amount) {
        Query query = new Query(Criteria.where("user_id").is(userId));      // get user

        // Purchases can only be made if the user is active and not suspended.
        // When a user is suspended or deleted, they cannot make purchases or withdrawals, and therefore cannot perform
        // any transactions that take money from the account.
        query.addCriteria(Criteria.where("deleted").ne(true));
        query.addCriteria(Criteria.where("suspended").ne(true));

        // The critical condition: sufficient cash must be available. MongoDB allows us to use the $expr operator to 7
        // compare two fields in the same document. Constraint to purchase -> (cash - blockedCash) >= amount.
        query.addCriteria(new Criteria("$expr").is(
                new org.bson.Document("$gte", java.util.Arrays.asList(
                        new org.bson.Document("$subtract", java.util.Arrays.asList("$cash", "$blockedCash")),
                        amount
                ))
        ));

        Update update = new Update()
                .inc("cash", -amount)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Updates blocked cash atomically (e.g., when placing a Limit Order).
     * Condition: (cash - blockedCash) >= amount
     *
     * @param userId the user identifier
     * @param amount the positive amount to subtract from cash
     */
    public User addBlockedCashAtomic(Long userId, double amount) {
        Query query = new Query(Criteria.where("user_id").is(userId));      // get user

        // control check: ensure available cash (cash - blockedCash) is enough to block
        query.addCriteria(new Criteria("$expr").is(
                new org.bson.Document("$gte", java.util.Arrays.asList(
                        new org.bson.Document("$subtract", java.util.Arrays.asList("$cash", "$blockedCash")),
                        amount
                ))
        ));

        Update update = new Update()
                .inc("blockedCash", amount)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Simple atomic increment for cash (e.g., after a sale or refund).
     * No specific condition other than user existence.
     *
     * @param userId the user identifier
     * @param amount the positive amount to subtract from cash
     */
    public User addCashAtomic(Long userId, double amount) {
        Query query = new Query(Criteria.where("user_id").is(userId));      // get user
        Update update = new Update()
                .inc("cash", amount)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    //------------------------------------------- end: methods for cash ------------------------------------------------
    //--------------------------------------- end: methods for atomic update -------------------------------------------
}
package it.unipi.myfuture.myfuture_backend.dao.mongo.transaction;

import com.mongodb.client.result.UpdateResult;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.model.AssetPrice;
import it.unipi.myfuture.myfuture_backend.model.Transaction;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
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
 * DAO for user transactions. Handles financial operations such as purchases, sales, deposits and withdrawals.
 *
 * Collection: transactions
 */
@Repository
public class TransactionDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Save or update a transaction (with update of the time).
     *
     * @param transaction the transaction to be entered(saved)
     * @return the inserted object
     */
    public Transaction save(Transaction transaction) {
        transaction.setUpdatedAt(Instant.now());
        return mongoTemplate.save(transaction);
    }

    /**
     * Save or update a transaction (without update of the time).
     *
     * @param transaction the transaction to be entered(saved)
     * @return the inserted object
     */
    public Transaction saveWithoutTime(Transaction transaction) {
        return mongoTemplate.save(transaction);
    }


    /**
     * Find a transaction by tits ID.
     *
     * @param transactionId id of the transaction
     * @return Optional containing the transaction if found, otherwise empty
     */
    public Optional<Transaction> findByTransactionId(Long transactionId) {
        Query query = new Query(Criteria.where("transaction_id").is(transactionId));
        return Optional.ofNullable(mongoTemplate.findOne(query, Transaction.class));
    }

    /**
     * Get the list of a transaction for a user.
     *
     * @param userId id of the user related the transactions
     * @return list of the transaction belonging to a user
     */
    public List<Transaction> findByUserId(Long userId) {
        Query query = new Query(
                Criteria.where("user_id").is(userId)
        );

        return mongoTemplate.find(query, Transaction.class);
    }

    /**
     * Search transactions applying optional filters. This method builds a dynamic MongoDB query based on the parameters
     * provided. Each parameter is considered optional:
     * - if a parameter is null, the corresponding filter is not applied
     * - if a parameter is not null, it is added as a filtering condition
     * This method is used by both customer and admin services.
     *
     * @param status transaction status to filter by (optional)
     * @param type transaction type to filter by (optional)
     * @param userId user identifier to filter by (optional)
     * @param from start date of the time range (optional, requires {@code to})
     * @param to end date of the time range (optional, requires {@code from})
     * @return list of transactions matching the provided filters
     */
    public List<Transaction> search(TransactionStatus status, TransactionType type, Long userId, Instant from, Instant to) {

        Criteria criteria = new Criteria();
        // set the parameters for the query
        if (status != null)
            criteria.and("status").is(status);

        if (type != null)
            criteria.and("type").is(type);

        if (userId != null)
            criteria.and("user_id").is(userId);

        if (from != null && to != null)
            criteria.and("date").gte(from).lte(to);

        Query query = new Query(criteria);                      // create query
        return mongoTemplate.find(query, Transaction.class);    // make the query
    }

    /**
     * Update the status of a transaction. This method is intended for internal system operations only
     * (e.g. batch jobs that process pending transactions at market opening).
     * It updates:
     * - transaction status
     * - updatedAt timestamp
     *
     * @param transactionId application-level transaction identifier
     * @param status new transaction status
     */
    public void updateTransactionStatus(Long transactionId, TransactionStatus status) {

        Query query = new Query(Criteria.where("transaction_id").is(transactionId));    // get transaction

        Update update = new Update()
                .set("status", status)
                .set("updatedAt", Instant.now());                                           // make the query

        UpdateResult result = mongoTemplate.updateFirst(query, update, Transaction.class);  // update transaction

        if (result.getMatchedCount() == 0)                                                  // control check
            throw new BusinessException("Transaction not found: " + transactionId);
    }

    /**
     * Permanently delete a transaction. Admin only, extraordinary maintenance operation.
     */
    public void deleteById(Long transactionId)
    {
        Query query = new Query(Criteria.where("transaction_id").is(transactionId));
        mongoTemplate.remove(query, Transaction.class);
    }

}
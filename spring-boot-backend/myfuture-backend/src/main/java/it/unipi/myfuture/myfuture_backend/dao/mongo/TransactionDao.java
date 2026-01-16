package it.unipi.myfuture.myfuture_backend.dao.mongo;

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
     * Save or update a transaction.
     *
     * @param transaction the transaction to be entered(saved)
     * @return the inserted object
     */
    public Transaction save(Transaction transaction) {
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
                        .and("deleted").ne(true)
        );
        return mongoTemplate.find(query, Transaction.class);
    }

    /**
     * Get the list of a transaction related to the user passed as parameter between the dates passed as parameters.
     *
     * @param userId id of the user related the transactions
     * @param from start date
     * @param to end date
     * @return list of transactions
     */
    public List<Transaction> findByUserIdAndDateRange(Long userId, Instant from, Instant to){
        Query query = new Query(
                Criteria.where("user_id").is(userId)
                        .and("date").gte(from).lte(to)
        );
        return mongoTemplate.find(query, Transaction.class);
    }


    /**
     *  Get the list of a transaction belonging to the status passed as parameter.
     *
     * @param status
     * @return list of the transaction belonging to the status passed as parameter
     */
    public List<Transaction> findByStatus(TransactionStatus status) {
        Query query = new Query(
                Criteria.where("status").is(status)
                        .and("deleted").ne(true)
        );
        return mongoTemplate.find(query, Transaction.class);
    }

    /**
     * Get the list of a transaction belonging to the status passed as parameter between the date passed as parameters.
     *
     * @param type the type of transactions requested
     * @param from start date
     * @param to end date
     * @return list of transactions
     */
    public List<Transaction> findByTypeAndDateRange(TransactionType type, Instant from, Instant to) {

        Query query = new Query(
                Criteria.where("type").is(type)
                        .and("date").gte(from).lte(to)
                        .and("deleted").ne(true)
        );
        return mongoTemplate.find(query, Transaction.class);
    }

    /**
     * Permanently delete a transaction.
     * Admin only – extraordinary maintenance operation.
     */
    public void deleteById(String transactionId)
    {
        Query query = new Query(Criteria.where("transaction_id").is(transactionId));
        mongoTemplate.remove(query, Transaction.class);
    }

}
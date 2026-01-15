package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dao.mongo.TransactionDao;
import it.unipi.myfuture.myfuture_backend.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Transaction entity.
 *
 * Handles transaction lifecycle and transaction history queries.
 */
@Service
public class TransactionService {

    @Autowired
    private TransactionDao transactionDao;

    /**
     * Create a new transaction.
     *
     * @param transaction transaction to create
     * @return saved transaction
     */
    public Transaction createTransaction(Transaction transaction) {
        return transactionDao.save(transaction);
    }

    /**
     * Retrieve a transaction by its transaction ID.
     *
     * @param transactionId transaction ID
     * @return Optional containing the transaction
     */
    public Optional<Transaction> getTransactionById(Long transactionId) {
        return transactionDao.findByTransactionId(transactionId);
    }

    /**
     * Retrieve all transactions of a user.
     *
     * @param userId application user ID
     * @return list of transactions
     */
    public List<Transaction> getTransactionsByUser(Long userId) {
        return transactionDao.findByUserId(userId);
    }

    /**
     * Retrieve user transactions in a specific date range.
     *
     * @param userId user ID
     * @param from start date
     * @param to end date
     * @return list of transactions
     */
    public List<Transaction> getTransactionsByUserAndDateRange(
            Long userId, Instant from, Instant to) {
        return transactionDao.findByUserIdAndDateRange(userId, from, to);
    }

    /**
     * Update a transaction.
     *
     * @param transaction updated transaction
     * @return saved transaction
     */
    public Transaction updateTransaction(Transaction transaction) {
        return transactionDao.save(transaction);
    }
}
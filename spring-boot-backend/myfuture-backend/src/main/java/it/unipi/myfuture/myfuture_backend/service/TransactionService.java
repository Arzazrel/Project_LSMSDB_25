package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for Transaction entity. (Controllers interact ONLY with this interface layer)
 */
public interface TransactionService {

    /**
     * Create a new transaction.
     *
     * @param request transaction data coming from client
     * @param userId id of the authenticated user (NOT coming from client)
     * @return created transaction
     */
    TransactionResponseDTO createTransaction(TransactionRequestDTO request, Long userId);

    /**
     * Retrieve a transaction by its id. Used by both customers and admin.
     *
     * @param id transaction id
     * @return transaction data
     */
    TransactionResponseDTO getTransactionById(Long id);

    /**
     * Retrieve all transactions of a specific user.
     *
     * @param userId id of the user
     * @return list of transactions
     */
    List<TransactionResponseDTO> getTransactionsByUser(Long userId);

    /**
     * Retrieve transactions using optional filters. Used by both customers and admin.
     *
     * @param status optional transaction status
     * @param type optional transaction type
     * @param userId optional user id (admin only)
     * @param from optional start date
     * @param to optional end date
     * @return list of transactions matching filters
     */
    List<TransactionResponseDTO> searchTransactions(TransactionStatus status, TransactionType type, Long userId, Instant from, Instant to);

    /**
     * Update the status of a transaction. (used form the system)
     *
     * @param id transaction id
     * @param status new status
     */
    void updateTransactionStatus(Long id, TransactionStatus status);

    /**
     * Update a transaction. (non-routine operation)
     *
     * @param transactionId
     * @param request
     */
    TransactionResponseDTO updateTransaction(Long transactionId, TransactionRequestDTO request);

    /**
     * Permanently delete a transaction.
     *
     * @param id transaction id
     */
    void deleteTransaction(Long id);

}

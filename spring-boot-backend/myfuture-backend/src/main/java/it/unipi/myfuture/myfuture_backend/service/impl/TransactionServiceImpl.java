package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.TransactionDao;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.mapper.AssetMapper;
import it.unipi.myfuture.myfuture_backend.mapper.TransactionMapper;
import it.unipi.myfuture.myfuture_backend.model.Asset;
import it.unipi.myfuture.myfuture_backend.model.Transaction;
import it.unipi.myfuture.myfuture_backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction service implementation.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionDao transactionDao;

    // ----------------------------------------------- start: transaction API -------------------------------------------

    /**
     * Create a new transaction.
     *
     * @param request transaction data coming from client
     * @param userId id of the authenticated user (NOT coming from client)
     * @return created transaction
     */
    @Override
    public TransactionResponseDTO createTransaction(TransactionRequestDTO request, Long userId) {

        Transaction transaction = TransactionMapper.toEntity(request, userId);
        Transaction saved = transactionDao.save(transaction);

        return TransactionMapper.toResponseDTO(saved);
    }

    /**
     * Retrieve a transaction by its id. Used by both customers and admin.
     *
     * @param id transaction id
     * @return transaction data
     */
    @Override
    public TransactionResponseDTO getTransactionById(Long id) {

        Transaction transaction = transactionDao.findByTransactionId(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        return TransactionMapper.toResponseDTO(transaction);
    }

    /**
     * Retrieve all transactions of a specific user.
     *
     * @param userId id of the user
     * @return list of transactions
     */
    @Override
    public List<TransactionResponseDTO> getTransactionsByUser(Long userId) {

        return transactionDao.findByUserId(userId)
                .stream()
                .map(TransactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve transactions using optional filters. Used by both customers and admin.
     * The service decides which DAO logic to apply based on provided parameters.
     *
     * @param status optional transaction status
     * @param type optional transaction type
     * @param userId optional user id (admin only)
     * @param from optional start date
     * @param to optional end date
     * @return list of transactions matching filters
     */
    @Override
    public List<TransactionResponseDTO> searchTransactions(
            TransactionStatus status,
            TransactionType type,
            Long userId,
            Instant from,
            Instant to) {

        return transactionDao.search(status, type, userId, from, to)
                .stream()
                .map(TransactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update the status of a transaction. (used form the system)
     *
     * @param id transaction id
     * @param status new status
     */
    @Override
    public void updateTransactionStatus(Long id, TransactionStatus status) {
        transactionDao.updateTransactionStatus(id, status);
    }

    /**
     * Update a transaction. (non-routine operation)
     *
     * @param transactionId
     * @param request
     */
    @Override
    public void updateTransaction(Long transactionId, TransactionRequestDTO request) {
        Transaction transaction = transactionDao.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        TransactionMapper.updateEntityFromDTO(transaction, request);

        Transaction savedTransaction = transactionDao.save(transaction);
        TransactionMapper.toResponseDTO(savedTransaction);
    }

    /**
     * Permanently delete a transaction.
     *
     * @param id transaction id
     */
    @Override
    public void deleteTransaction(Long id) {
        transactionDao.deleteById(id);
    }

    // ----------------------------------------------- end: transaction API ---------------------------------------------
}
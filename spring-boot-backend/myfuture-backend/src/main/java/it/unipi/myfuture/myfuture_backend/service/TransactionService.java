package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.transaction.*;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for Transaction entity.
 */
public interface TransactionService {

    // ----------------------------------------------- start: transaction API -------------------------------------------

    TransactionResponseDTO createTransaction(TransactionRequestDTO request);

    TransactionResponseDTO getTransactionById(String id);

    TransactionResponseDTO getTransactionByIdAdmin(String id);

    List<TransactionResponseDTO> getTransactionsByUser(Long userId);

    List<TransactionResponseDTO> getTransactionsAdmin(
            String status, String type, Long userId, Instant from, Instant to);

    void updateTransactionStatus(String id, String status);

    void softDeleteTransaction(String id);

    // ----------------------------------------------- end: transaction API ---------------------------------------------
}
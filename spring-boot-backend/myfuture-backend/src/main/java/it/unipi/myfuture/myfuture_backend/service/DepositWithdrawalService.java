package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;

/**
 * Deposit/Withdrawal Service interface.
 * Manages deposit and withdrawal transactions by managing the consistency of MongoDB and Redis.
 */
public interface DepositWithdrawalService {

    /**
     * Processes a financial transaction for depositing or withdrawing funds.
     * This method validates the transaction type, ensures the user is active and authorized,
     * and executes the cash movement using atomic operations to prevent race conditions.
     * It ensures consistency between the persistent storage (MongoDB) and the cache (Redis).
     *
     * @param email   The email of the user, retrieved from the security context, used for initial validation.
     * @param userId  The unique identifier of the user performing the operation.
     * @param request The DTO containing transaction details, including the amount and type (deposit or withdrawal).
     * @return A TransactionResponseDTO containing the status of the operation (SUCCESS or FAILED)
     * and the updated transaction details.
     */
    TransactionResponseDTO processDepositWithdrawal(String email, Long userId, TransactionRequestDTO request);
}

package it.unipi.myfuture.myfuture_backend.mapper;

import it.unipi.myfuture.myfuture_backend.dto.transaction.*;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.model.RecentTransaction;
import it.unipi.myfuture.myfuture_backend.model.Transaction;

import java.time.Instant;

/**
 * Transaction Mapper handles conversion between Transaction entity and Transaction DTOs.
 * Used inside service layer to keep business logic clean.
 */
public class TransactionMapper {

    //----------------------------------------- start: create mapping (request) ----------------------------------------

    /**
     * Convert TransactionRequestDTO to Transaction entity.
     * Used when creating a new transaction.
     *
     * @param transactionRequest transaction request DTO
     * @param userId id of the user related to the transaction
     * @return transaction entity
     */
    public static Transaction toEntity(TransactionRequestDTO transactionRequest, Long userId) {
        Transaction tx = new Transaction();
        Instant now = Instant.now();

        tx.setUserId(userId);
        tx.setTransactionType(transactionRequest.getTransactionType());
        tx.setCurrency(transactionRequest.getCurrency());
        tx.setTotalPrice(transactionRequest.getTotalPrice());
        tx.setPaymentMethod(transactionRequest.getPaymentMethod());

        tx.setSymbol(transactionRequest.getSymbol());
        tx.setAssetType(transactionRequest.getAssetType());
        tx.setPricePerUnit(transactionRequest.getPricePerUnit());
        tx.setQuantity(transactionRequest.getQuantity());

        tx.setDate(now);
        tx.setUpdatedAt(now);

        return tx;
    }

    //------------------------------------------ end: create mapping (request) -----------------------------------------

    //------------------------------------------ start: update mapping (request) ---------------------------------------
    /**
     * Update an existing Transaction entity using data from TransactionRequestDTO.
     * This method is intended for ADMIN operations only.
     * It performs a partial update:
     * - Only fields present in the DTO are overwritten
     * - Immutable fields (transactionId, userId, date) are NOT modified
     * Business rules (status transitions, validation, permissions) must be enforced at service layer.
     *
     * @param tx existing transaction entity
     * @param dto DTO containing updated values
     */
    public static void updateEntityFromDTO(Transaction tx, TransactionRequestDTO dto) {

        // Update mutable business fields
        tx.setTransactionType(dto.getTransactionType());
        tx.setCurrency(dto.getCurrency());
        tx.setTotalPrice(dto.getTotalPrice());
        tx.setPaymentMethod(dto.getPaymentMethod());

        // Update trading-related fields (purchase / sell)
        tx.setSymbol(dto.getSymbol());
        tx.setAssetType(dto.getAssetType());
        tx.setPricePerUnit(dto.getPricePerUnit());
        tx.setQuantity(dto.getQuantity());

        // Always update last modification timestamp
        tx.setUpdatedAt(Instant.now());
    }

    //----------------------------------------- end: update mapping (request) ------------------------------------------

    //---------------------------------------------- start: response mapping -------------------------------------------

    /**
     * Convert Transaction entity to TransactionResponseDTO.
     *
     * @param transaction transaction entity
     * @return transaction response DTO
     */
    public static TransactionResponseDTO toResponseDTO(Transaction transaction) {
        TransactionResponseDTO dto = new TransactionResponseDTO();

        dto.setTransactionId(transaction.getTransactionId());
        dto.setUserId(transaction.getUserId());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setDate(transaction.getDate());
        dto.setCurrency(transaction.getCurrency());
        dto.setTotalPrice(transaction.getTotalPrice());
        dto.setPaymentMethod(transaction.getPaymentMethod());
        dto.setStatus(transaction.getStatus());
        dto.setFailureReason(transaction.getFailureReason());

        dto.setSymbol(transaction.getSymbol());
        dto.setAssetType(transaction.getAssetType());
        dto.setPricePerUnit(transaction.getPricePerUnit());
        dto.setQuantity(transaction.getQuantity());

        dto.setUpdatedAt(transaction.getUpdatedAt());

        return dto;
    }
    //---------------------------------------------- end: response mapping ---------------------------------------------

    //--------------------------------------------- start: sub entity mapping ------------------------------------------

    /**
     * Convert Transaction entity to RecentTransaction entity (to add in user's last transaction embedding).
     *
     * @param transaction transaction entity
     * @return transaction response DTO
     */
    public static RecentTransaction toRecentTransaction(Transaction transaction)
    {
        RecentTransaction recentTransaction = new RecentTransaction();

        recentTransaction.setTransactionId(transaction.getTransactionId());
        TransactionType transactionType = transaction.getTransactionType();
        recentTransaction.setType(transactionType);

        // check transaction type to choose the right value for symbol
        if (transaction.getTransactionType() == TransactionType.purchase || transaction.getTransactionType() == TransactionType.sell)
        {
            recentTransaction.setSymbol(transaction.getSymbol());   // purchase / sell
            recentTransaction.setQuantity(transaction.getQuantity());
        }
        else
        {
            recentTransaction.setSymbol("");                        // deposit / withdrawal
            recentTransaction.setQuantity(0.0);                     // default value
        }

        recentTransaction.setTotalPrice(transaction.getTotalPrice());
        recentTransaction.setStatus(transaction.getStatus());
        recentTransaction.setDate(transaction.getUpdatedAt());

        return recentTransaction;
    }

    //---------------------------------------------- end: sub entity mapping -------------------------------------------
}
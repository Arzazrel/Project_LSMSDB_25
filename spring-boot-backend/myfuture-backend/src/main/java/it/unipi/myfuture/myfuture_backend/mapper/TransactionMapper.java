package it.unipi.myfuture.myfuture_backend.mapper;

import it.unipi.myfuture.myfuture_backend.dto.transaction.*;
import it.unipi.myfuture.myfuture_backend.model.Transaction;

import java.time.Instant;

/**
 * Transaction Mapper handles conversion between Transaction entity and Transaction DTOs.
 * Used inside service layer to keep business logic clean.
 */
public class TransactionMapper {

    // -------------------------------------- request → entity --------------------------------------

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

        tx.setUserId(userId);
        tx.setType(transactionRequest.getType());
        tx.setCurrency(transactionRequest.getCurrency());
        tx.setTotalPrice(transactionRequest.getTotalPrice());
        tx.setPaymentMethod(transactionRequest.getPaymentMethod());

        tx.setSymbol(transactionRequest.getSymbol());
        tx.setAssetType(transactionRequest.getAssetType());
        tx.setPricePerUnit(transactionRequest.getPricePerUnit());
        tx.setQuantity(transactionRequest.getQuantity());

        tx.setDate(Instant.now());

        return tx;
    }

    // -------------------------------------- entity → response --------------------------------------

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
        dto.setType(transaction.getType());
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
}
package it.unipi.myfuture.myfuture_backend.dto;

import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import lombok.Data;

import java.time.Instant;

/**
 * DTO used to expose transaction details via REST API.
 */
@Data
public class TransactionResponseDTO {

    private Long transactionId;

    private TransactionType type;
    private TransactionStatus status;

    private String symbol;
    private String assetType;

    private double quantity;
    private double pricePerUnit;
    private double totalPrice;

    private Instant date;
}
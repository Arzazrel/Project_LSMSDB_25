package it.unipi.myfuture.myfuture_backend.dto;

import it.unipi.myfuture.myfuture_backend.enums.PaymentMethod;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import lombok.Data;

/**
 * DTO used to receive transaction requests from users.
 */
@Data
public class TransactionRequestDTO {

    private TransactionType type;

    private String symbol;
    private String assetType;

    private double quantity;
    private double pricePerUnit;

    private PaymentMethod paymentMethod;
}
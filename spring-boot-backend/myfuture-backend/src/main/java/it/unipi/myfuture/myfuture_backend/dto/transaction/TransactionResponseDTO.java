package it.unipi.myfuture.myfuture_backend.dto.transaction;

import it.unipi.myfuture.myfuture_backend.enums.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO used to expose transaction details via REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private Long transactionId;         // application-level ID (from counters)
    private Long userId;
    private TransactionType type;       // purchase, sell, deposit, withdrawal
    private Instant date;

    private UserCurrency currency;      // default: USD (for now is supported USD only)

    /**
     * Total amount of the transaction.
     * - buy/sell → total price
     * - deposit/withdrawal → transferred amount
     */
    private double totalPrice;

    private PaymentMethod paymentMethod;

    private TransactionStatus status;
    private FailureReason failureReason;    // Only if status == FAILED

    // fields only for purchase / sell
    private String symbol;
    private AssetType assetType;
    private double pricePerUnit;
    private double quantity;

    private Instant updatedAt;
}
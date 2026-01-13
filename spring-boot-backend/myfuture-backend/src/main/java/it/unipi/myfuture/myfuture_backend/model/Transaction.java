package it.unipi.myfuture.myfuture_backend.model;

import it.unipi.myfuture.myfuture_backend.enums.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents a financial transaction performed by a user.
 *
 * Includes trading operations (buy/sell) and account operations
 * (deposit/withdrawal).
 *
 * Collection: transactions
 */
@Data
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;                  // MongoDB _id

    private Long transactionId;         // application-level ID (from counters)
    private Long userId;

    private TransactionType type;       // purchase, sell, deposit, withdrawal
    private Instant date;

    private UserCurrency currency;      // USD

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


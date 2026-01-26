package it.unipi.myfuture.myfuture_backend.model;

import it.unipi.myfuture.myfuture_backend.enums.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Represents a financial transaction performed by a user.
 * Includes trading operations (buy/sell) and account operations s(deposit/withdrawal).
 *
 * Collection: transactions
 */
@Data
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;                          // MongoDB _id
    @Field("transaction_id")
    private Long transactionId;                 // application-level ID (from counters)
    @Field("user_id")
    private Long userId;                        // application-level ID (from counters)
    private TransactionType transactionType;    // purchase, sell, deposit, withdrawal
    private Instant date;

    private UserCurrency currency;              // default: USD (for now is supported USD only)

    /**
     * Total amount of the transaction.
     * - purchase/sell → total price
     * - deposit/withdrawal → transferred amount
     */
    private double totalPrice;

    private PaymentMethod paymentMethod;        // 'paypal', 'creditcard', 'storecredit'

    private TransactionStatus status;
    private FailureReason failureReason;        // Only if status == FAILED

    // fields only for purchase / sell
    private String symbol;
    private AssetType assetType;
    private double pricePerUnit;
    private double quantity;
    private Instant updatedAt;
}


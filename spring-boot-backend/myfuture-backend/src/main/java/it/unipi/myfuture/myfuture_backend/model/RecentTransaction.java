package it.unipi.myfuture.myfuture_backend.model;

import lombok.Data;
import java.time.Instant;

/**
 * Represents a lightweight summary of a transaction.
 *
 * Stored as an embedded document inside the user's recentTransactions array.
 */
@Data
public class RecentTransaction {

    private String transactionId;
    private String type;       // buy / sell / deposit / withdrawal
    private String symbol;
    private Double quantity;
    private Double totalPrice;
    private String status;
    private Instant date;
}

package it.unipi.myfuture.myfuture_backend.model;

import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import lombok.Data;

import java.time.Instant;

/**
 * Represents a lightweight summary of a transaction.
 *
 * Stored as an embedded document inside the user's recentTransactions array.
 */
@Data
public class RecentTransaction {

    private Long transactionId;         // counter id
    private TransactionType type;       // purchase / sell / deposit / withdrawal
    private String symbol;              // set only for purchase / sell transactions
    private Double quantity;            // set only for purchase / sell transactions
    private Double totalPrice;          // total price for purchase / sell and amount of cash for deposit / withdrawal
    private TransactionStatus status;
    private Instant date;               // updateAt not date -> SEE NOTE 0
}

/**
 * NOTE 0
 *  The updateAt date is used rather than the execution date (date) because in the case of deposit, withdrawal, purchase,
 *  or sell transactions made during market hours, these dates coincide.
 *  However, in the case of purchase and sell transactions made after market hours, the two dates will only coincide as
 *  long as they remain in pending status. When the transaction is processed (it will change to EXECUTED or FAILED),
 *  the updateAt date will be updated.
 *  And since it will be the most up-to-date date indicating the last modification/iteration of the transaction,
 *  I have chosen it as the most representative and to be displayed.
 */
package it.unipi.myfuture.myfuture_backend.dto.transaction;

import it.unipi.myfuture.myfuture_backend.enums.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO used to receive transaction requests from users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDTO {

    private TransactionType transactionType;       // purchase, sell, deposit, withdrawal

    private UserCurrency currency;      // default: USD (for now is supported USD only)

    /**
     * Total amount of the transaction.
     * - buy/sell → total price
     * - deposit/withdrawal → transferred amount
     */
    private double totalPrice;

    private PaymentMethod paymentMethod;

    // fields only for purchase / sell
    private String symbol;
    private AssetType assetType;
    private double pricePerUnit;
    private double quantity;
}
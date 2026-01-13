package it.unipi.myfuture.myfuture_backend.model;

import lombok.Data;

/**
 * Represents a single asset entry inside a user's wallet.
 *
 * Used for shares, ETFs, and cryptocurrencies wallets.
 * This is an embedded document inside the User collection.
 */
@Data
public class WalletItem {

    private String symbol;
    private double quantity;
    private double blockedQuantity;
    private double bep;                 // break-even price (weighted mean)
}
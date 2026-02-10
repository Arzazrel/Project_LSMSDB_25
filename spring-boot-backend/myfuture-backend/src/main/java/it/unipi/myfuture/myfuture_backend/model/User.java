package it.unipi.myfuture.myfuture_backend.model;

import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.enums.UserCurrency;
import it.unipi.myfuture.myfuture_backend.enums.UserRole;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Represents a registered user or administrator of the platform. This entity stores personal information,
 * account status, wallet data, and recent transactions for a user.
 *
 * Collection: users
 * Used by: authentication, account management, trading operations
 */
@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;                  // MongoDB _id
    @Field("user_id")
    private Long userId;                // application-level ID (from counters)
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private UserRole role;              // user or admin
    private LocalDate birthDate;
    private String phone;

    private String address;
    private String city;
    private String province;
    private String cap;

    private Instant registrationDate;

    private Boolean suspended;
    private SuspensionInfo suspensionInfo;

    private double cash;
    private double blockedCash;
    private UserCurrency currency;

    // document embedded
    private List<WalletItem> shareWallet;
    private List<WalletItem> etfWallet;
    private List<WalletItem> cryptoWallet;

    private List<RecentTransaction> recentTransactions;

    // soft delete
    private Boolean deleted;
    private Instant deletedAt;
    // manage field
    private Instant updatedAt;

    //---------------------------------------- start: complex get/set methods ------------------------------------------

    /**
     * Getter that find the correct wallet list by asset Type.
     *
     * @param type asset type to determine which portfolio list is required
     * @return
     */
    public List<WalletItem> getWalletByType(AssetType type) {
        return switch (type) {
            case share -> this.shareWallet;
            case etf -> this.etfWallet;
            case crypto -> this.cryptoWallet;
            default -> throw new BusinessException("Unsupported asset type");
        };
    }

    /**
     * Setter that find the correct wallet list by asset Type.
     *
     * @param type asset type to determine which portfolio list is required
     * @param wallet wallet list to put
     */
    public void setWalletByType(AssetType type, List<WalletItem> wallet) {
        switch (type) {
            case share -> this.shareWallet = wallet;
            case etf -> this.etfWallet = wallet;
            case crypto -> this.cryptoWallet = wallet;
            default -> throw new BusinessException("Unsupported asset type");
        };
    }

    /**
     * Method to extract a wallet item for an asset of the user or null.
     *
     * @param symbol asset identifier
     * @param assetType asset type
     * @return the WalletItem for the searched asset or null
     */
    public WalletItem getWalletItemBySymbol(String symbol, AssetType assetType) {

        List<WalletItem> targetWallet = getWalletByType(assetType);     // chose the right wallet

        // check if the searched asset is in the user's portfolio
        int index = findAssetIndexInWallet(targetWallet,symbol);

        if (index != -1) {      // the asset is in the portfolio
            return targetWallet.get(index);     // take the wallet item for the asset
        }
        else
            return null;                        // walletList null or there isn't item for the asset in the walletList
    }
    //----------------------------------------- end: complex get/set methods -------------------------------------------

    //-------------------------------------- start: recentTransactions methods -----------------------------------------
    /**
     * Adds a transaction to the recent transactions list.
     * Maintains only the last 'maxLastTransaction'(10) transactions, acting as a fixed-size LIFO buffer.
     *
     * @param transaction the transaction to add (light class of the transaction class, only most relevant information).
     */
    public void addLatestTransaction(RecentTransaction transaction) {
        int maxLastTransaction = 10;                // the maximum amount of last transaction to save in the user

        if (this.recentTransactions == null)        // check empty list
            this.recentTransactions = new java.util.ArrayList<>();              // create the list


        // adds in first position (index 0) to have the most recent at the top
        this.recentTransactions.add(0, transaction);

        // if we exceed 10 transactions, we remove the oldest one (the last one on the list).
        if (this.recentTransactions.size() > maxLastTransaction) {
            this.recentTransactions.remove(this.recentTransactions.size() - 1);
        }
    }

    /**
     * Updates an existing transaction in the recent list or adds it if not present. If the transaction is found (by ID),
     * it is removed and re-inserted at the top to maintain chronological order (LIFO).
     *
     * @param transaction the transaction to update or add.
     */
    public void upsertRecentTransaction(RecentTransaction transaction) {
        int maxLastTransaction = 10;                // the maximum amount of last transaction to save in the user

        if (this.recentTransactions == null)        // check empty list
            this.recentTransactions = new java.util.ArrayList<>();      // create the list

        // Removes the transaction if it already exists (comparison by ID). Use removeIf, it's efficient
        this.recentTransactions.removeIf(t ->
                t.getTransactionId().equals(transaction.getTransactionId())
        );

        // adds the transaction (new or updated) in the first position
        this.recentTransactions.add(0, transaction);

        // if we exceed 10 transactions, we remove the oldest one (the last one on the list).
        if (this.recentTransactions.size() > maxLastTransaction) {
            this.recentTransactions.remove(this.recentTransactions.size() - 1);
        }
    }
    //--------------------------------------- end: recentTransactions methods ------------------------------------------

    //-------------------------------------------- start: wallet methods -----------------------------------------------

    public double calculateNewBep(String symbol, AssetType assetType, double qty, double currentPrice)
    {
        // chose the right wallet and check if is null
        if (getWalletByType(assetType) == null) {
            setWalletByType(assetType, new java.util.ArrayList<>());    // is null, initialize
        }
        List<WalletItem> targetWallet = getWalletByType(assetType);     // get the correct wallet

        // check if the searched asset is in the user's portfolio
        int index = findAssetIndexInWallet(targetWallet,symbol);

        if (index != -1) {      // the asset is in the portfolio

            WalletItem item = targetWallet.get(index);  // take the wallet item for the asset
            // update BEP and quantity
            double oldTotalCost = item.getQuantity() * item.getBep();
            double newTotalCost = qty * currentPrice;

            return (oldTotalCost + newTotalCost) / item.getQuantity();
        }
        else {                  // the asset isn't in the portfolio
            return currentPrice;
        }
    }

    /**
     * Helper to update user portfolio and calculate Weighted Average Price (BEP).
     *
     * @param symbol the asset symbol.
     * @param qty quantity being purchased.
     * @param currentPrice current price per asset unit of the current purchase.
     */
    public void updatePortfolioForPurchase(String symbol, AssetType assetType, double qty, double currentPrice) {

        // chose the right wallet and check if is null
        if (getWalletByType(assetType) == null) {
            setWalletByType(assetType, new java.util.ArrayList<>());    // is null, initialize
        }
        List<WalletItem> targetWallet = getWalletByType(assetType);     // get the correct wallet

        // check if the searched asset is in the user's portfolio
        int index = findAssetIndexInWallet(targetWallet,symbol);

        if (index != -1) {      // the asset is in the portfolio

            WalletItem item = targetWallet.get(index);  // take the wallet item for the asset
            // update BEP and quantity
            double oldTotalCost = item.getQuantity() * item.getBep();
            double newTotalCost = qty * currentPrice;

            item.setQuantity(item.getQuantity() + qty);                         // update quantity
            item.setBep((oldTotalCost + newTotalCost) / item.getQuantity());    // update BEP
        }
        else {                  // the asset isn't in the portfolio
            // create new walletItem and add asset in corresponding wallet
            WalletItem newItem = new WalletItem();
            newItem.setSymbol(symbol);
            newItem.setQuantity(qty);
            newItem.setBep(currentPrice);           // set BEP = price per unit = current price
            newItem.setBlockedQuantity(0);          // set blocked quantity to 0

            targetWallet.add(newItem);              // add new document embedded
        }
    }

    /**
     * Helper to update user portfolio after a sell transaction.
     *
     * @param symbol the asset symbol.
     * @param qty quantity being purchased.
     * @param limitOrder if true -> indicate that is a transaction done when market is closed
     *                   if false -> indicate that is a transaction done when market is open
     * @param useBlockedQuantity if true -> indicate to use blockedQuantity instead quantity (execution of pending sell)
     *                           if false -> indicate to use quantity (execution of sell on market open)
     */
    public void updatePortfolioForSell(String symbol, AssetType assetType, double qty, boolean limitOrder, boolean useBlockedQuantity) {

        List<WalletItem> targetWallet = getWalletByType(assetType);     // chose the right wallet

        // check if the searched asset is in the user's portfolio
        int index = findAssetIndexInWallet(targetWallet,symbol);

        if (index != -1) {      // the asset is in the portfolio
            WalletItem item = targetWallet.get(index);  // tak ethe current item
            if (limitOrder)                 // is a limit order operate on blockedQuantity (sell after)
            {
                item.setBlockedQuantity(item.getBlockedQuantity() + qty);       // update asset's blocked quantity
            }
            else
            {
                double newQty = targetWallet.get(index).getQuantity() - qty;    // calculate new asset's quantity
                if (newQty <= 0)        // sell all asset
                    targetWallet.remove(index);                         // remove document embedded for this asset
                else                    // isn't a limit order operate on quantity (sell now)
                {
                    item.setQuantity(newQty);                           // update asset's quantity

                    if(useBlockedQuantity) // execution of limit order operate both on blockedQuantity and quantity (execution of pending sell)
                        item.setBlockedQuantity(item.getBlockedQuantity() - qty);
                }
            }
        }
        else {                  // the asset isn't in the portfolio
            throw new BusinessException("The user doesn't have assets for this trade");
        }
    }

    /**
     * method for updating user status (blocked cash, blocked quantity) in case of failed transaction
     *
     * @param transaction failed transaction
     */
    public void updateUserWhenTransactionFailed(Transaction transaction)
    {
        if (transaction.getTransactionType() == TransactionType.purchase)   // purchase case
        {
            this.setBlockedCash(this.getBlockedCash() - transaction.getTotalPrice());   // free blocked cash
        }
        else if(transaction.getTransactionType() == TransactionType.sell)   // sell case
        {
            // update only the blocked quantity for the target asset
            WalletItem targetItem = this.getWalletItemBySymbol(transaction.getSymbol(), transaction.getAssetType());
            if (targetItem != null)
            {
                double newqty = targetItem.getBlockedQuantity() - transaction.getQuantity();
                if (newqty > 0)
                    targetItem.setBlockedQuantity(newqty);
                else
                    targetItem.setBlockedQuantity(0);
            }
        }
    }

    //--------------------------------------------- end: wallet methods ------------------------------------------------

    //------------------------------------------ start: utilities methods ----------------------------------------------

    /**
     * Function to search a symbol in the user wallet and return its index if is in the wallet.
     *
     * @param wallet the list of wallet for an asset type of the user
     * @param symbol the symbol of te
     * @return index i -> if the symbol is in the list
     *          -1 -> if the symbol isn't in the list or the list is null
     */
    private int findAssetIndexInWallet(List<WalletItem> wallet, String symbol) {
        if (wallet == null) return -1;  // control check
        // scroll each asset in the wallet
        for (int i = 0; i < wallet.size(); i++) {
            if (wallet.get(i).getSymbol().equals(symbol)) {
                return i;           // there is the searched asset
            }
        }
        return -1;                  // there isn't
    }



    //------------------------------------------ end: utilities methods ----------------------------------------------
}
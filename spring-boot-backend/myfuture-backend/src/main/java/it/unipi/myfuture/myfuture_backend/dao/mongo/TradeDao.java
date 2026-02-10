package it.unipi.myfuture.myfuture_backend.dao.mongo;

import com.mongodb.client.result.UpdateResult;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.model.RecentTransaction;
import it.unipi.myfuture.myfuture_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Data Access Object for trade operation, to ensure atomicity. Manage persistence and queries for user.
 *
 * Collection: users
 */
@Repository
public class TradeDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    //---------------------------------------- start: methods for purchase ---------------------------------------------

    /**
     * Executes an atomic multi-field update for a market purchase when the asset is already present in the wallet.
     * This method ensures data consistency by updating the user's cash balance, the specific wallet item
     * (quantity and BEP), and the recent transactions list in a single, atomic database operation.
     * This is done to prevent race conditions during the calculation phase.
     *
     * @param userId       the unique identifier of the user
     * @param totalCost    the total cash amount to be deducted
     * @param symbol       the ticker symbol of the asset
     * @param type         the type of asset (e.g., STOCK, CRYPTO)
     * @param expectedCash the cash value used during service calculations to ensure data consistency
     * @param qtyToAdd     the quantity purchased to be added to the existing balance
     * @param newBep       the new calculated Break-Even Price
     * @param transaction  the light transaction object for the history list
     * @return the updated user entity if the update was successful, otherwise null
     */
    public User executeMarketPurchaseExistingAtomic(Long userId, double totalCost, String symbol, AssetType type, double expectedCash,
                                                    double qtyToAdd, double newBep, RecentTransaction transaction) {

        // query with check of: user's status, user's cash, and existence of the specific asset in the wallet
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true)
                .and("cash").is(expectedCash)
                .and("wallet").elemMatch(Criteria.where("symbol").is(symbol).and("assetType").is(type)));

        // control check to ensure (cash - blockedCash) is greater than or equal to totalCost
        query.addCriteria(new Criteria("$expr").is(
                new org.bson.Document("$gte", java.util.Arrays.asList(
                        new org.bson.Document("$subtract", java.util.Arrays.asList("$cash", "$blockedCash")),
                        totalCost
                ))
        ));

        // definition of the multi-field atomic update: subtract cash, update specific wallet item and history
        Update update = new Update()
                .inc("cash", -totalCost)                   // decrease the user's available cash
                .inc("wallet.$.quantity", qtyToAdd)        // increment quantity for the matched wallet item
                .set("wallet.$.bep", newBep)                    // update bep for the matched wallet item
                .push("recentTransactions").atPosition(0).slice(10).value(transaction) // add to embedded (max 10)
                .set("updatedAt", Instant.now());               // update updatedAt

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Executes an atomic multi-field update for a market purchase when the asset is NOT yet in the wallet.
     * This method creates a new wallet entry while simultaneously deducting cash and updating history.
     * * It ensures that no duplicate wallet items are created if a concurrent process adds the same asset
     * by checking for the non-existence of the symbol.
     *
     * @param userId       the unique identifier of the user
     * @param totalCost    the total cash amount to be deducted
     * @param symbol       the ticker symbol of the asset
     * @param type         the type of asset (e.g., STOCK, CRYPTO)
     * @param expectedCash the cash value used during service calculations to ensure data consistency
     * @param qtyToAdd     the quantity purchased
     * @param newBep       the initial Break-Even Price (purchase price)
     * @param transaction  the light transaction object for the history list
     * @return the updated user entity if the update was successful, otherwise null
     */
    public User executeMarketPurchaseNewAtomic(Long userId, double totalCost, String symbol, AssetType type, double expectedCash,
                                               double qtyToAdd, double newBep, RecentTransaction transaction) {

        // query with check of: user's status, user's cash, and there isn't the specific asset in the wallet
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true)
                .and("cash").is(expectedCash)
                .and("wallet.symbol").ne(symbol));

        // control check to ensure (cash - blockedCash) is greater than or equal to totalCost
        query.addCriteria(new Criteria("$expr").is(
                new org.bson.Document("$gte", java.util.Arrays.asList(
                        new org.bson.Document("$subtract", java.util.Arrays.asList("$cash", "$blockedCash")),
                        totalCost
                ))
        ));

        // create the BSON document for the new wallet item to be pushed into the embedded array
        org.bson.Document newWalletItem = new org.bson.Document("symbol", symbol)
                .append("assetType", type.toString())
                .append("quantity", qtyToAdd)
                .append("blockedQuantity", 0.0)
                .append("bep", newBep);

        // definition of the multi-field atomic update: subtract cash, update specific wallet item and history
        Update update = new Update()
                .inc("cash", -totalCost)                       // decrease the user's available cash
                .push("wallet", newWalletItem)                      // append the new asset to the wallet array in user
                .push("recentTransactions").atPosition(0).slice(10).value(transaction) // add to embedded (max 10)
                .set("updatedAt", Instant.now());                   // update updatedAt

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Executes an atomic update for creating a purchase limit order when the market is closed.
     * It validates funds availability and moves the required amount from available to blocked cash.
     *
     * @param userId       the unique identifier of the user
     * @param totalCost    the amount to be blocked
     * @param expectedCash the cash value used during service calculations
     * @param transaction  the new pending transaction object
     * @return the updated User object, or null if insufficient funds or status is invalid
     */
    public User executeLimitPurchaseAtomic(Long userId, double totalCost, double expectedCash, RecentTransaction transaction) {

        // query with status check and compare-and-swap on cash
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true)
                .and("cash").is(expectedCash));

        // control check to ensure (cash - blockedCash) is greater than or equal to totalCost
        query.addCriteria(new Criteria("$expr").is(
                new org.bson.Document("$gte", java.util.Arrays.asList(
                        new org.bson.Document("$subtract", java.util.Arrays.asList("$cash", "$blockedCash")),
                        totalCost
                ))
        ));

        // atomic update: increase blocked cash and add the pending transaction
        Update update = new Update()
                .inc("blockedCash", totalCost)
                .push("recentTransactions").atPosition(0).slice(10).value(transaction)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Executes an atomic update for a triggered pending purchase order when the asset is already in the wallet.
     * It subtracts the cost from both total cash and blocked cash, updates the asset's quantity and BEP,
     * and replaces the pending transaction with the executed one in the history.
     *
     * @param userId            the unique identifier of the user
     * @param blockedToRelease  the blocked cash quantity blocked for the transaction at the creation
     * @param totalCost         the total cost of the transaction
     * @param symbol            the ticker symbol of the asset
     * @param type              the asset type
     * @param qtyToAdd          the quantity purchased
     * @param newBep            the updated Break-Even Price
     * @param transaction       the updated executed transaction object
     * @return the updated User object, or null if conditions (like enough blocked cash) were not met
     */
    public User executePendingPurchaseExistingAtomic(Long userId, double blockedToRelease, double totalCost, String symbol,
                                                     AssetType type, double qtyToAdd, double newBep, RecentTransaction transaction) {

        // query with status check and existence of asset, ensuring enough blocked cash is present
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true)
                .and("blockedCash").gte(blockedToRelease)
                .and("wallet").elemMatch(Criteria.where("symbol").is(symbol).and("assetType").is(type)));

        // atomic update: decrease total cash and blocked cash, update wallet item and refresh history
        Update update = new Update()
                .inc("cash", -totalCost)                        // final deduction from total cash
                .inc("blockedCash", -blockedToRelease)                 // release the previously blocked cash
                .inc("wallet.$.quantity", qtyToAdd)             // add quantity to existing asset
                .set("wallet.$.bep", newBep)                         // update bep
                // remove the old version of the transaction (pending) to avoid duplicates
                .pull("recentTransactions", new org.bson.Document("transactionId", transaction.getTransactionId()))
                // push the new version (executed) to the first position
                .push("recentTransactions").atPosition(0).slice(10).value(transaction)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Executes an atomic update for a triggered pending purchase order for a new asset.
     * It subtracts the cost from total and blocked cash, pushes a new entry to the wallet,
     * and replaces the pending transaction in the history.
     *
     * @param userId            the unique identifier of the user
     * @param blockedToRelease  the blocked cash quantity blocked for the transaction at the creation
     * @param totalCost         the total cost of the transaction
     * @param symbol            the ticker symbol of the new asset
     * @param type              the asset type
     * @param qtyToAdd          the quantity purchased
     * @param newBep            the initial BEP (purchase price)
     * @param transaction       the updated executed transaction object
     * @return the updated User object, or null if conditions were not met
     */
    public User executePendingPurchaseNewAtomic(Long userId, double blockedToRelease, double totalCost, String symbol,
                                                AssetType type, double qtyToAdd, double newBep, RecentTransaction transaction) {

        // query with status check, enough blocked cash, and ensuring the asset doesn't exist yet
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true)
                .and("blockedCash").gte(blockedToRelease)
                .and("wallet.symbol").ne(symbol));

        // create the document for the new wallet item
        org.bson.Document newWalletItem = new org.bson.Document("symbol", symbol)
                .append("assetType", type.toString())
                .append("quantity", qtyToAdd)
                .append("blockedQuantity", 0.0)
                .append("bep", newBep);

        // atomic update: cash adjustment, wallet push and history refresh
        Update update = new Update()
                .inc("cash", -totalCost)
                .inc("blockedCash", -blockedToRelease)
                .push("wallet", newWalletItem)
                // remove the old version of the transaction (pending) to avoid duplicates
                .pull("recentTransactions", new org.bson.Document("transactionId", transaction.getTransactionId()))
                // push the new version (executed) to the first position
                .push("recentTransactions").atPosition(0).slice(10).value(transaction)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }
    //------------------------------------------ end: methods for purchase ---------------------------------------------
    //------------------------------------------ start: methods for sell -----------------------------------------------

    /**
     * Executes an atomic multi-field update for a market sell transaction.
     * Updates the user's cash balance (increment), decreases the asset quantity in the wallet, and updates the recent
     * transactions list in a single atomic operation.
     *
     * @param userId       the unique identifier of the user
     * @param totalGain    the total cash amount to be added to the balance
     * @param expectedCash the cash value used during service calculations (cas guard)
     * @param symbol       the ticker symbol of the asset to sell
     * @param type         the asset type
     * @param qtyToSell    the quantity to subtract from the wallet
     * @param transaction  the light transaction object for the history list
     * @return the updated user entity if the update was successful, otherwise null
     */
    public User executeMarketSellAtomic(Long userId, double totalGain, double expectedCash, String symbol,
                                           AssetType type, double qtyToSell, RecentTransaction transaction) {

        // query with check of: user status, cash consistency, and asset existence in wallet
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true)
                .and("cash").is(expectedCash)
                .and("wallet").elemMatch(Criteria.where("symbol").is(symbol).and("assetType").is(type)));

        // control check to ensure available quantity (quantity - blockedQuantity) is enough to sell
        query.addCriteria(new Criteria("$expr").is(
                new org.bson.Document("$gte", java.util.Arrays.asList(
                        new org.bson.Document("$subtract", java.util.Arrays.asList("$wallet.quantity", "$wallet.blockedQuantity")),
                        qtyToSell
                ))
        ));

        // definition of the multi-field atomic update
        Update update = new Update()
                .inc("cash", totalGain)                         // add the sale proceeds to cash
                .inc("wallet.$.quantity", -qtyToSell)           // decrease the asset quantity
                .push("recentTransactions").atPosition(0).slice(10).value(transaction) // add to embedded (max 10)
                .set("updatedAt", Instant.now());                    // update updatedAt

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Executes an atomic multi-field update for a market off sell transaction.
     * Decreases the asset blocked quantity in the wallet, and updates the recent transactions in a single atomic op.
     *
     * @param userId      the unique identifier of the user
     * @param symbol      the ticker symbol of the asset to sell
     * @param type        the asset type
     * @param qtyToBlock  the quantity to add to blocked quantity
     * @param transaction the light transaction object for the history list
     * @return the updated user entity if the update was successful, otherwise null
     */
    public User executeSellLimitAtomic(Long userId, String symbol, AssetType type, double qtyToBlock, RecentTransaction transaction) {

        // query with check of: user status, cash consistency, and asset existence in wallet
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true).and("suspended").ne(true)
                .and("wallet").elemMatch(Criteria.where("symbol").is(symbol).and("assetType").is(type)));

        // check if available quantity is enough: (quantity - blockedQuantity) >= qtyToBlock
        query.addCriteria(new Criteria("$expr").is(
                new org.bson.Document("$gte", java.util.Arrays.asList(
                        new org.bson.Document("$subtract", java.util.Arrays.asList("$wallet.quantity", "$wallet.blockedQuantity")),
                        qtyToBlock
                ))
        ));

        // definition of the multi-field atomic update
        Update update = new Update()
                .inc("wallet.$.blockedQuantity", qtyToBlock)       // add blocked quantity
                .push("recentTransactions").atPosition(0).slice(10).value(transaction) // add to embedded (max 10)
                .set("updatedAt", Instant.now());                       // update updatedAt

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Executes an atomic multi-field update for execution of a pending sell transaction.
     * Decreases the asset blocked quantity and quantity in the wallet, update the user's cash and updates the recent
     * transactions in a single atomic op.
     *
     * @param userId       the unique identifier of the user
     * @param totalGain    the total cash amount to be added to the balance
     * @param symbol       the ticker symbol of the asset to sell
     * @param type         the asset type
     * @param transaction  the light transaction object for the history list
     * @return the updated user entity if the update was successful, otherwise null
     */
    public User executePendingSellAtomic(Long userId, double totalGain, String symbol, AssetType type,
                                                  double qtyToRemove, RecentTransaction transaction) {

        // query with check of: user status, cash consistency, asset existence in wallet, quantity e blocked quantity.
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true)
                .and("wallet").elemMatch(Criteria.where("symbol").is(symbol)
                        .and("assetType").is(type)
                        .and("blockedQuantity").gte(qtyToRemove)
                        .and("quantity").gte(qtyToRemove)));

        // definition of the multi-field atomic update
        Update update = new Update()
                .inc("cash", totalGain)
                .inc("wallet.$.quantity", -qtyToRemove)
                .inc("wallet.$.blockedQuantity", -qtyToRemove)
                // remove the old version of the transaction (pending) to avoid duplicates
                .pull("recentTransactions", new org.bson.Document("transactionId", transaction.getTransactionId()))
                // push the new version (executed) to the first position
                .push("recentTransactions").atPosition(0).slice(10).value(transaction)
                .set("updatedAt", Instant.now());

        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }
    //------------------------------------------- end: methods for sell ------------------------------------------------

    //--------------------------------- start: methods for deposit and Withdrawal --------------------------------------

    /**
     * Executes an atomic update for a cash deposit.
     * Increases the user's cash balance and adds the transaction to the history list.
     *
     * @param userId      the unique identifier of the user
     * @param amount      the amount of cash to deposit
     * @param transaction the light transaction object for history
     * @return the updated User object, or null if the user is deleted
     */
    public User executeDepositAtomic(Long userId, double amount, RecentTransaction transaction) {
        // query to ensure the user exists and is not deleted
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true));

        // atomic update: increment cash and push transaction
        Update update = new Update()
                .inc("cash", amount)
                .push("recentTransactions").atPosition(0).slice(10).value(transaction)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Executes an atomic update for a cash withdrawal.
     * Validates available funds and decreases the user's cash balance.
     *
     * @param userId      the unique identifier of the user
     * @param amount      the amount of cash to withdraw
     * @param transaction the light transaction object for history
     * @return the updated User object, or null if insufficient funds or user is suspended/deleted
     */
    public User executeWithdrawalAtomic(Long userId, double amount, RecentTransaction transaction) {
        // query with status check
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true));

        // control check: (cash - blockedCash) >= amount
        query.addCriteria(new Criteria("$expr").is(
                new org.bson.Document("$gte", java.util.Arrays.asList(
                        new org.bson.Document("$subtract", java.util.Arrays.asList("$cash", "$blockedCash")),
                        amount
                ))
        ));

        // atomic update: decrease cash and push transaction
        Update update = new Update()
                .inc("cash", -amount)
                .push("recentTransactions").atPosition(0).slice(10).value(transaction)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    //---------------------------------- end: methods for deposit and Withdrawal ---------------------------------------

    //------------------------------------------ start: utilities method -----------------------------------------------

    /**
     * Executes an atomic update adding a RecentTransaction into embedded array in the user.
     *
     * @param userId      the unique identifier of the user
     * @param transaction the light transaction object for history
     * @return the updated User object or null
     */
    public User addLatestTransaction(Long userId, RecentTransaction transaction)
    {
        // query with status check
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("deleted").ne(true)
                .and("suspended").ne(true));

        // add recent transaction and set updatedAt
        Update update = new Update()
                .push("recentTransactions").atPosition(0).slice(10).value(transaction)
                .set("updatedAt", Instant.now());

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }

    /**
     * Atomically removes an asset from the user's wallet if its quantity has reached zero.
     * This is used as a cleanup step after a sell transaction to keep the wallet document lean.
     *
     * @param userId the unique identifier of the user
     * @param symbol the ticker symbol of the asset to be removed
     * @param type   the type of the asset (e.g., STOCK, CRYPTO)
     * @return the updated User object if the asset was removed, or null if the criteria were not met
     */
    public User removeAssetIfEmpty(Long userId, String symbol, AssetType type) {
        // query with check of: asset existence in wallet and quantity is exactly 0
        Query query = new Query(Criteria.where("user_id").is(userId)
                .and("wallet").elemMatch(Criteria.where("symbol").is(symbol)
                        .and("assetType").is(type)
                        .and("quantity").is(0.0)));

        Update update = new Update().pull("wallet", new org.bson.Document("symbol", symbol).append("assetType", type.toString()));

        // findAndModify with FindAndModifyOptions().returnNew(true) returns the updated user or null otherwise
        return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), User.class);
    }
    //------------------------------------------- end: utilities method ------------------------------------------------
}

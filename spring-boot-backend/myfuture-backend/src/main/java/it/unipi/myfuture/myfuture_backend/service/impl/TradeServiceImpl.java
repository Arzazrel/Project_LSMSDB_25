package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.CounterDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.TradeDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.user.UserDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.AssetRedisDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.UserRedisDao;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.*;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.TransactionMapper;
import it.unipi.myfuture.myfuture_backend.model.Transaction;
import it.unipi.myfuture.myfuture_backend.model.User;
import it.unipi.myfuture.myfuture_backend.model.WalletItem;
import it.unipi.myfuture.myfuture_backend.service.TradeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class TradeServiceImpl implements TradeService {

    @Autowired
    private CounterDao counterDao;
    @Autowired
    private UserDao userDao;                    // dao operating on MongoDB
    @Autowired
    private TransactionDao transactionDao;      // dao operating on MongoDB
    @Autowired
    private UserRedisDao userRedisDao;          // dao operating on Redis
    @Autowired
    private AssetRedisDao assetRedisDao;        // dao operating on Redis
    @Autowired
    private TradeDao tradeDao;

    //------------------------------------------- start: process methods -----------------------------------------------
    /**
     * Main entry point for processing an asset purchase or sell request.
     * This method checks if the New York Stock Exchange (NYSE) is currently open.
     * - If the market is open, it routes the request to an immediate market order execution.
     * - If the market is closed, it routes the request to a limit order placement (Pending).
     *
     * @param email email fo the user taken by the authentication context
     * @param userId  The unique identifier of the user performing the trade.
     * @param request The DTO containing trade details such as symbol, quantity, and currency.
     * @return A TransactionResponseDTO representing the outcome of the trade (SUCCESS or PENDING).
     * @throws BusinessException if the symbol is invalid, funds are insufficient, or price data is missing.
     */
    @Override
    @Transactional
    public TransactionResponseDTO processTrade(String email, Long userId, TransactionRequestDTO request) {

        // control check for the transaction type
        TransactionType transactionType = request.getTransactionType();
        if (transactionType != TransactionType.purchase && transactionType != TransactionType.sell)
            throw new BusinessException("Invalid transaction type, transaction canceled.");

        // control check for the user, check in MongoDB. Check if user is active or suspended or deleted
        User user = userDao.findByEmail(email).orElse(null);
        if (user == null)       // there isn't an user with this userid
        {
            Transaction transaction = setNewTransaction(userId, request, false);    // create entity
            transaction.markTransactionAsFailed(FailureReason.UNKNOWN_USER);        // update transaction
            transactionDao.save(transaction);                                       // save transaction in MongoDB
            return TransactionMapper.toResponseDTO(transaction);
        }
        else if(user.getDeleted() || user.getSuspended()) // there is the user is deleted or suspended transaction fails and user's fields have to be updated
        {
            FailureReason reason = user.getDeleted() ? FailureReason.USER_DELETED : FailureReason.USER_SUSPENDED;

            return TransactionMapper.toResponseDTO(finalizeFailedTransaction(request, userId, reason));
        }

        // get current price (last) for the wanted asset for the transaction
        Double currentPrice = assetRedisDao.getCurrentPrice(request.getSymbol());
        // control check, this information is only in Redis. It is also validation check for symbol -> SEE NOTE 0
        if (currentPrice == null)
        {
            // return the ResponseDTO of the failed transaction
            return TransactionMapper.toResponseDTO(finalizeFailedTransaction(request, userId, FailureReason.ASSET_DELISTED));
        }

        // check if the market is open
        if (isMarketOpen() || request.getAssetType().equals(AssetType.crypto)) {
            // discriminate by transaction type
            if (transactionType == TransactionType.purchase)    // purchase case
            {
                // control check SEE NOTE 1
                if (currentPrice > request.getPricePerUnit())
                {
                    // return the ResponseDTO of the failed transaction
                    return TransactionMapper.toResponseDTO(finalizeFailedTransaction(request, userId, FailureReason.PRICE_LIMIT_NOT_MET));
                }
            }
            else                                                // sell case
            {
                // control check SEE NOTE 1
                if (currentPrice < request.getPricePerUnit())
                {
                    // return the ResponseDTO of the failed transaction
                    return TransactionMapper.toResponseDTO(finalizeFailedTransaction(request, userId, FailureReason.PRICE_LIMIT_NOT_MET));
                }
            }

            // update current price in case of a change in the current price (quick update)
            request.setPricePerUnit(currentPrice);
            request.setTotalPrice(currentPrice * request.getQuantity());    // calculate new total price

            // discriminate by transaction type
            if (transactionType == TransactionType.purchase)    // purchase case
                return purchaseMarketOrder(user, request);     // execute the purchase transactions
            else                                                // sell case
                return sellMarketOrder(user, request);         // execute the sell transactions

        } else          // the market is closed
        {
            // discriminate by transaction type
            if (transactionType == TransactionType.purchase)    // purchase case
                return purchaseLimitOrder(user, request);     // execute the purchase transactions
            else                                                // sell case
                return sellLimitOrder(user, request);         // execute the sell transactions
        }
    }

    //-------------------------------------------- end: process methods ------------------------------------------------

    //----------------------------------------------- start: methods ---------------------------------------------------

    /**
     * Executes a market buy order for a specific asset.
     *
     * @param user the entity of the user that made the transaction
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return a TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO purchaseMarketOrder(User user, TransactionRequestDTO request) {

        Long userId = user.getUserId();
        double totalCost = request.getTotalPrice();                     // get total cost of transaction
        User updatedUser;

        // check on Redis
        Double cash = userRedisDao.getCash(userId.toString());                  // get cash
        Double blockedCash = userRedisDao.getBlockedCash(userId.toString());    // get blocked cash
        // check key on redis. -> SEE NOTE 2
        if (cash == null || blockedCash == null || cash != user.getCash() || blockedCash != user.getBlockedCash())
        {
            // populate Redis for next time (Self-healing cache) with all user information
            userRedisDao.saveFullUserToCache(user);
            cash = user.getCash();                                      // get cash
            blockedCash = user.getBlockedCash();                        // get blocked cash
        }
        double userAvailableCash = cash - blockedCash;                  // calculate the available cash

        // control check
        if (userAvailableCash < totalCost)
        {
            return TransactionMapper.toResponseDTO(finalizeFailedTransaction(request, userId, FailureReason.INSUFFICIENT_FUNDS));
        }

        long transactionId = counterDao.getNextSequence(CounterType.transaction_id);    // crete new transaction_id
        // set success transaction
        Transaction newtransaction = setSuccessfulTransaction(userId, request, transactionId);
        // check if user has already this asset
        if (user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType()) == null)
            updatedUser = tradeDao.executeMarketPurchaseNewAtomic(userId, totalCost, request.getSymbol(),
                    request.getAssetType(), cash, request.getQuantity(), request.getPricePerUnit(),
                    TransactionMapper.toRecentTransaction(newtransaction));
        else
        {
            // calculate new bep
            double newBep = user.calculateNewBep(request.getSymbol(), request.getAssetType(), request.getQuantity(), request.getPricePerUnit());
            updatedUser = tradeDao.executeMarketPurchaseExistingAtomic(userId, totalCost, request.getSymbol(),
                    request.getAssetType(), cash, request.getQuantity(), newBep,
                    TransactionMapper.toRecentTransaction(newtransaction));
        }

        // check if transaction is successfully done or not
        if (updatedUser != null)
            userRedisDao.updateUserInCacheIfActive(updatedUser);         // update Redis cache
        else
        {
            // transaction failed, set transaction as failed
            newtransaction = setFailedTransaction(userId, request, FailureReason.INSUFFICIENT_FUNDS, transactionId);
            // update only user's recent transaction list in MongoDB (with failed transaction)
            tradeDao.addLatestTransaction(userId, TransactionMapper.toRecentTransaction(newtransaction));
            userRedisDao.clearUserCache(String.valueOf(userId));        // security cache delete for error cases
        }

        transactionDao.saveWithoutTime(newtransaction);     // save transaction in MongoDB, maintain correct updatedAt
        return TransactionMapper.toResponseDTO(newtransaction);         // return response DTO
    }

    /**
     * Define a closed market purchase order for a specific asset.
     *
     * @param user the entity of the user that made the transaction
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return a TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO purchaseLimitOrder(User user, TransactionRequestDTO request) {

        Long userId = user.getUserId();
        double totalCost = request.getTotalPrice();                     // get total cost of transaction
        User updatedUser;

        // check on Redis
        Double cash = userRedisDao.getCash(userId.toString());                  // get cash
        Double blockedCash = userRedisDao.getBlockedCash(userId.toString());    // get blocked cash
        // check key on redis. -> SEE NOTE 2
        if (cash == null || blockedCash == null || cash != user.getCash() || blockedCash != user.getBlockedCash())
        {
            // populate Redis for next time (Self-healing cache) with all user information
            userRedisDao.saveFullUserToCache(user);
            cash = user.getCash();                                      // get cash
            blockedCash = user.getBlockedCash();                        // get blocked cash
        }
        double userAvailableCash = cash - blockedCash;                  // calculate the available cash

        // control check
        if (userAvailableCash < totalCost)
        {
            return TransactionMapper.toResponseDTO(finalizeFailedTransaction(request, userId, FailureReason.INSUFFICIENT_FUNDS));
        }

        long transactionId = counterDao.getNextSequence(CounterType.transaction_id);        // crete new transaction_id
        Transaction newtransaction = setPendingTransaction(userId, request, transactionId); // set pending transaction
        updatedUser = tradeDao.executeLimitPurchaseAtomic(userId, request.getTotalPrice(), cash, TransactionMapper.toRecentTransaction(newtransaction));

        // check if transaction is successfully done or not
        if (updatedUser != null)
            userRedisDao.updateUserInCacheIfActive(updatedUser);         // update Redis cache
        else
        {
            // transaction failed, set transaction as failed
            newtransaction = setFailedTransaction(userId, request, FailureReason.INSUFFICIENT_FUNDS, transactionId);
            // update only user's recent transaction list in MongoDB (with failed transaction)
            tradeDao.addLatestTransaction(userId, TransactionMapper.toRecentTransaction(newtransaction));
            userRedisDao.clearUserCache(String.valueOf(userId));        // security cache delete for error cases
        }

        transactionDao.saveWithoutTime(newtransaction);     // save transaction in MongoDB, maintain correct updatedAt
        return TransactionMapper.toResponseDTO(newtransaction);         // return response DTO
    }

    /**
     * Executes a market sell order for a specific asset.
     *
     * @param user the entity of the user that made the transaction
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return A TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO sellMarketOrder(User user, TransactionRequestDTO request)
    {
        Long userId = user.getUserId();
        User updatedUser;
        // check on Redis, try to retrieve the details of the specific asset from Redis (Hash)
        WalletItem cachedItem = userRedisDao.getAssetDetails(userId.toString(), request.getSymbol());
        // get wallet from user
        WalletItem mongoItem = user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType());
        // control check if the cache is null and if the cache is updated with MongoDB
        if (cachedItem == null || !cachedItem.equals(mongoItem)) {
            // populate Redis for next time (Self-healing cache) with all user information
            userRedisDao.saveFullUserToCache(user);
            // get the item from the user
            cachedItem = mongoItem;
        }

        // get the information of the asset sold in the user
        double availableQuantity = cachedItem.getQuantity() - cachedItem.getBlockedQuantity();  // get available quantity
        double sellQuantity = request.getQuantity();                // get sell quantity

        // control check
        if (availableQuantity < sellQuantity)
            return TransactionMapper.toResponseDTO(finalizeFailedTransaction(request, userId, FailureReason.INSUFFICIENT_ASSET_QUANTITY));

        long transactionId = counterDao.getNextSequence(CounterType.transaction_id);    // crete new transaction_id
        // set success transaction
        Transaction newtransaction = setSuccessfulTransaction(userId, request, transactionId);
        updatedUser = tradeDao.executeMarketSellAtomic(userId, request.getTotalPrice(), user.getCash(), request.getSymbol(),
            request.getAssetType(), request.getQuantity(), TransactionMapper.toRecentTransaction(newtransaction));

        // check if transaction is successfully done or not
        if (updatedUser != null)
        {
            if ((mongoItem.getQuantity() - request.getQuantity()) <= 0) {
                User userAfterRemoval = tradeDao.removeAssetIfEmpty(userId, request.getSymbol(), request.getAssetType());

                if (userAfterRemoval != null)
                    updatedUser = userAfterRemoval;
            }
            userRedisDao.updateUserInCacheIfActive(updatedUser);            // update Redis cache
        }
        else
        {
            // transaction failed, set transaction as failed
            newtransaction = setFailedTransaction(userId, request, FailureReason.INSUFFICIENT_ASSET_QUANTITY, transactionId);
            // update only user's recent transaction list in MongoDB (with failed transaction)
            tradeDao.addLatestTransaction(userId, TransactionMapper.toRecentTransaction(newtransaction));
            userRedisDao.clearUserCache(String.valueOf(userId));        // security cache delete for error cases
        }

        transactionDao.saveWithoutTime(newtransaction);     // save transaction in MongoDB, maintain correct updatedAt
        return TransactionMapper.toResponseDTO(newtransaction);         // return response DTO
    }

    /**
     * Define a market sell order for a specific asset.
     *
     * @param user the entity of the user that made the transaction
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return A TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO sellLimitOrder(User user, TransactionRequestDTO request)
    {
        Long userId = user.getUserId();
        User updatedUser;
        // check on Redis, try to retrieve the details of the specific asset from Redis (Hash)
        WalletItem cachedItem = userRedisDao.getAssetDetails(userId.toString(), request.getSymbol());
        // get wallet from user
        WalletItem mongoItem = user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType());
        // control check if the cache is null and if the cache is updated with MongoDB
        if (cachedItem == null || !cachedItem.equals(mongoItem)) {
            // populate Redis for next time (Self-healing cache) with all user information
            userRedisDao.saveFullUserToCache(user);
            // get the item from the user
            cachedItem = mongoItem;
        }

        // get the information of the asset sold in the user
        double availableQuantity = cachedItem.getQuantity() - cachedItem.getBlockedQuantity();  // get available quantity
        double sellQuantity = request.getQuantity();                // get sell quantity

        // control check
        if (availableQuantity < sellQuantity)
            return TransactionMapper.toResponseDTO(finalizeFailedTransaction(request, userId, FailureReason.INSUFFICIENT_ASSET_QUANTITY));

        long transactionId = counterDao.getNextSequence(CounterType.transaction_id);    // crete new transaction_id
        // set pending transaction
        Transaction newtransaction = setPendingTransaction(userId, request, transactionId);
        updatedUser = tradeDao.executeSellLimitAtomic(userId, request.getSymbol(), request.getAssetType(),
                request.getQuantity(), TransactionMapper.toRecentTransaction(newtransaction));

        // check if transaction is successfully done or not
        if (updatedUser != null)
            userRedisDao.updateUserInCacheIfActive(updatedUser);            // update Redis cache
        else
        {
            // transaction failed, set transaction as failed
            newtransaction = setFailedTransaction(userId, request, FailureReason.INSUFFICIENT_ASSET_QUANTITY, transactionId);
            // update only user's recent transaction list in MongoDB (with failed transaction)
            tradeDao.addLatestTransaction(userId, TransactionMapper.toRecentTransaction(newtransaction));
            userRedisDao.clearUserCache(String.valueOf(userId));        // security cache delete for error cases
        }

        transactionDao.saveWithoutTime(newtransaction);     // save transaction in MongoDB, maintain correct updatedAt
        return TransactionMapper.toResponseDTO(newtransaction);         // return response DTO
    }

    //------------------------------------------------ end: methods ----------------------------------------------------

    //------------------------------------------ start: utilities methods ----------------------------------------------

    /**
     * Method that return if the market is open or not (New York trade exchange)
     *
     * @return  if true the market is open
     *          if false the market is closed
     */
    private boolean isMarketOpen() {
        ZonedDateTime nowNY = ZonedDateTime.now(ZoneId.of("America/New_York")); // take New York zone time
        DayOfWeek day = nowNY.getDayOfWeek();   // take the current day of the week
        LocalTime time = nowNY.toLocalTime();   // take the new york local time

        // market open Monday through Friday
        boolean isBusinessDay = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;       // calculate if the day is correct

        // the market is open from 09:30 to 16:00
        LocalTime openingTime = LocalTime.of(9, 30);
        LocalTime closingTime = LocalTime.of(16, 0);

        boolean isTradingHours = !time.isBefore(openingTime) && !time.isAfter(closingTime); // calculate if time is correct

        return isBusinessDay && isTradingHours;     // return if the market is open or not
    }

    /**
     * Function that generates the transaction entity, executed, complete with all fields ready to be saved in MongoDB.
     *
     * @param userId user identifier
     * @param tempTransaction the request DTO of the transaction
     * @param limitOrder if true -> indicate that is a transaction done when market is closed
     *                   if false -> indicate that is a transaction done when market is open
     * @return transaction entity to save
     */
    private Transaction setNewTransaction(Long userId, TransactionRequestDTO tempTransaction, boolean limitOrder)
    {
        // create a complete transaction from request
        Transaction transaction = TransactionMapper.toEntity(tempTransaction, userId);
        // now add remaining fields
        transaction.setTransactionId(counterDao.getNextSequence(CounterType.transaction_id)); // get and update transactionId
        if (limitOrder)     // market is closed
            transaction.setStatus(TransactionStatus.PENDING);                   // set status transaction
        else
            transaction.setStatus(TransactionStatus.EXECUTED);                  // set status transaction

        return transaction;
    }

    /**
     * method that contains all the steps to update and save a transaction and its user when a transaction fails.
     *
     * @param request failed transaction
     * @param userId identifier of the user related to  the transaction
     * @param reason failure reason
     */
    private Transaction finalizeFailedTransaction(TransactionRequestDTO request, long userId, FailureReason reason) {
        Transaction transaction = setNewTransaction(userId, request, false);    // create entity
        transaction.markTransactionAsFailed(reason);                        // set failed status
        Transaction savedTransaction = transactionDao.save(transaction);    // save transaction into MongoDb

        tradeDao.addLatestTransaction(userId,TransactionMapper.toRecentTransaction(savedTransaction));
        userRedisDao.clearUserCache(String.valueOf(userId));        // security cache delete for error cases

        return savedTransaction;
    }

    /**
     * From transactionRequest create a new transaction with successful status and passed id.
     *
     * @param userId        the user identifier related to transaction
     * @param request       the transaction request details.
     * @param transactionId the identifier of the transaction
     * @return the new Transaction created.
     */
    private Transaction setSuccessfulTransaction(long userId, TransactionRequestDTO request, long transactionId) {

        Transaction transaction = TransactionMapper.toEntity(request, userId);  // create entity, set also updatedAt
        transaction.setTransactionId(transactionId);                            // set new id
        transaction.setStatus(TransactionStatus.EXECUTED);                      // set status

        return transaction;
    }

    /**
     * From transactionRequest create a new transaction with successful status and passed id.
     *
     * @param userId        the user identifier related to transaction
     * @param request       the transaction request details.
     * @param transactionId the identifier of the transaction
     * @return the new Transaction created.
     */
    private Transaction setPendingTransaction(long userId, TransactionRequestDTO request, long transactionId) {

        Transaction transaction = TransactionMapper.toEntity(request, userId);  // create entity, set also updatedAt
        transaction.setTransactionId(transactionId);                            // set new id
        transaction.setStatus(TransactionStatus.PENDING);                       // set status

        return transaction;
    }

    /**
     * From transactionRequest create a new transaction with failed status and passed id.
     *
     * @param userId  the user identifier related to transaction
     * @param request the transaction request details.
     * @param reason  the specific reason why the transaction failed.
     * @return the new Transaction created.
     */
    private Transaction setFailedTransaction(long userId, TransactionRequestDTO request, FailureReason reason, long transactionId) {
        Transaction transaction = TransactionMapper.toEntity(request, userId);  //create entity, set also updatedAt
        transaction.setTransactionId(transactionId);                            // set new id
        transaction.markTransactionAsFailed(reason);                            // set status

        return transaction;
    }

    //------------------------------------------- end: utilities methods -----------------------------------------------
}

/**
 * NOTE 0:
 *  Asset validation occurs implicitly by retrieving the price from Redis. If currentPrice is null, the operation fails
 *  immediately. This approach avoids a redundant query on MongoDB to verify the existence of the symbol.
 *  This control is performed both on the closed and open market because the system is designed to retain the
 *  'currentPrice' in Redis even when the market is closed. Motivation:
 *  - User Experience: Allows users to view their portfolio valuation based on the last closing price (Last Known Value).
 *  - Limit Order Basis: Provides a reference point for users to set purchase/sell limits when placing orders after hours.
 *  - Performance: Avoids continuous fallback to MongoDB for valuation queries during market downtime, keeping the
 *                 system responsive.
 *  The value remains static until the next market opening, ensuring data consistency for all off-market operations.
 *
 * NOTE 1
 *  The current price may have changed from the limit set by the user. It is necessary to check that:
 *      in a purchase transaction, the current price is not higher than the price chosen by the user (the maximum they
 *      are willing to pay);
 *      in a sale transaction, it is not lower than the price chosen by the user (less than the minimum they are
 *      willing to sell for).
 *  If these conditions are not met, the transaction must be canceled.
 *
 * NOTE 2
 *  First two elements check if there aren't the information in Redis.
 *  Last two elements there are the information in Redis, check consistency. Theoretically, if the data is in Redis,
 *  it should be updated. Over check.
 */
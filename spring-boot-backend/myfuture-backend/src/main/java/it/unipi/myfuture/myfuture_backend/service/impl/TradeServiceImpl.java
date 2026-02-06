package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.CounterDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.asset.AssetDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.user.UserDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.AssetRedisDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.UserRedisDao;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.*;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.TransactionMapper;
import it.unipi.myfuture.myfuture_backend.model.RecentTransaction;
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
import java.time.Instant;
import java.util.List;

@Service
public class TradeServiceImpl implements TradeService {

    @Autowired
    private CounterDao counterDao;
    @Autowired
    private UserDao userDao;                    // dao operating on MongoDB
    @Autowired
    private TransactionDao transactionDao;      // dao operating on MongoDB
    @Autowired
    private AssetDao assetDao;                  // dao operating on MongoDB
    @Autowired
    private UserRedisDao userRedisDao;          // dao operating on Redis
    @Autowired
    private AssetRedisDao assetRedisDao;        // dao operating on Redis

    ZonedDateTime nowNY = ZonedDateTime.now(ZoneId.of("America/New_York")); // take New York zone time

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

        // get current price (last) for the wanted asset for the transaction
        Double currentPrice = assetRedisDao.getCurrentPrice(request.getSymbol());
        // control check, this information is only in Redis. It is also validation check for symbol -> SEE NOTE 0
        if (currentPrice == null)
            throw new BusinessException("Market price not available for " + request.getSymbol());

        // check if the market is open
        if (isMarketOpen()) {

            // discriminate by transaction type
            if (transactionType == TransactionType.purchase)    // purchase case
            {
                // control check SEE NOTE 1
                if (currentPrice > request.getPricePerUnit())
                    throw new BusinessException("Market price for " + request.getSymbol() + " higher than the maximum limit chosen by the user. Transaction canceled.");
            }
            else                                                // sell case
            {
                // control check SEE NOTE 1
                if (currentPrice < request.getPricePerUnit())
                    throw new BusinessException("Market price for " + request.getSymbol() + " lower than the maximum limit chosen by the user. Transaction canceled.");
            }

            // update current price in case of a change in the current price (quick update)
            request.setPricePerUnit(currentPrice);
            request.setTotalPrice(currentPrice * request.getQuantity());    // calculate new total price

            // discriminate by transaction type
            if (transactionType == TransactionType.purchase)    // purchase case
                return purchaseMarketOrder(email, userId, request);     // execute the purchase transactions
            else                                                // sell case
                return sellMarketOrder(email, userId, request);         // execute the sell transactions

        } else          // the market is closed
        {
            // discriminate by transaction type
            if (transactionType == TransactionType.purchase)    // purchase case
                return purchaseLimitOrder(email, userId, request);     // execute the purchase transactions
            else                                                // sell case
                return sellLimitOrder(email, userId, request);         // execute the sell transactions
        }
    }

    //-------------------------------------------- end: process methods ------------------------------------------------

    //----------------------------------------------- start: methods ---------------------------------------------------

    /**
     * Executes a market buy order for a specific asset.
     *
     * @param email email fo the user taken by the authentication context
     * @param userId The ID of the user performing the trade.
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return a TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO purchaseMarketOrder(String email, Long userId, TransactionRequestDTO request) {

        double totalCost = request.getTotalPrice();                     // get total cost of transaction
        User user= null;                                                // reference of user entity get from MongoDB
        // check on Redis
        Double cash = userRedisDao.getCash(userId.toString());                  // get cash
        Double blockedCash = userRedisDao.getBlockedCash(userId.toString());    // get blocked cash
        // check key on redis
        if (cash == null || blockedCash == null) {
            // redis does not have the value, it is retrieved from MongoDB
            user = userDao.findByEmailActive(email).orElseThrow(() -> new BusinessException("User not found"));
            // populate Redis for next time (Self-healing cache) with all user information
            userRedisDao.saveFullUserToCache(user);
            cash = user.getCash();                                      // get cash
            blockedCash = user.getBlockedCash();                        // get blocked cash
        }
        double userAvailableCash = cash - blockedCash;                  // calculate the available cash

        // control check
        if (userAvailableCash < totalCost)
            throw new BusinessException("Insufficient funds for this trade");

        // get user by email (unique index, fast retrieve). If user is null, it means that the data was in the
        // cache and we need to load the user now. If it is NOT null, already have it.
        if (user == null)
            user = userDao.findByEmailActive(email).orElseThrow(() -> new BusinessException("User session invalid"));

        // consistency check
        if ((user.getCash() - user.getBlockedCash()) < totalCost)
            throw new BusinessException("Insufficient funds (Consistency check failed)");

        // update portfolio, add quantity and modify BEP
        user.updatePortfolioForPurchase(request.getSymbol(), request.getAssetType(), request.getQuantity(), request.getPricePerUnit());
        user.setCash(user.getCash() - totalCost);                       // update user's cash

        Transaction transaction = setNewTransaction(userId, request, false);    // set transaction
        Transaction savedTransaction = transactionDao.save(transaction);// save transaction in MongoDB

        RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);
        user.addLatestTransaction(recTransaction);                      // update embedded lastTransaction in user
        User savedUser = userDao.save(user);                            // update user in MongoDB

        // update user in Redis (cache)
        try {
            userRedisDao.saveFullUserToCache(savedUser);                // update redis cache

        } catch (Exception e) {
            // if Redis fails -> delete to avoid inconsistent data (Cache Eviction)
            userRedisDao.clearUserCache(userId.toString());
        }

        return TransactionMapper.toResponseDTO(savedTransaction);       // return transactionDTO
    }


    /**
     * Define a closed market purchase order for a specific asset.
     *
     * @param email email fo the user taken by the authentication context
     * @param userId The ID of the user performing the trade.
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return a TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO purchaseLimitOrder(String email, Long userId, TransactionRequestDTO request) {

        double totalCost = request.getTotalPrice();                     // get total cost of transaction
        User user= null;                                                // reference of user entity get from MongoDB
        // check on Redis
        Double cash = userRedisDao.getCash(userId.toString());                  // get cash
        Double blockedCash = userRedisDao.getBlockedCash(userId.toString());    // get blocked cash
        // check key on redis
        if (cash == null || blockedCash == null) {
            // redis does not have the value, it is retrieved from MongoDB
            user = userDao.findByEmailActive(email).orElseThrow(() -> new BusinessException("User not found"));
            // populate Redis for next time (Self-healing cache) with all user information
            userRedisDao.saveFullUserToCache(user);
            cash = user.getCash();                                      // get cash
            blockedCash = user.getBlockedCash();                        // get blocked cash
        }
        double userAvailableCash = cash - blockedCash;                  // calculate the available cash

        // control check
        if (userAvailableCash < totalCost)
            throw new BusinessException("Insufficient funds for this trade");

        // get user by email (unique index, fast retrieve). If user is null, it means that the data was in the
        // cache and we need to load the user now. If it is NOT null, already have it.
        if (user == null)
            user = userDao.findByEmailActive(email).orElseThrow(() -> new BusinessException("User session invalid"));

        // consistency check
        if ((user.getCash() - user.getBlockedCash()) < totalCost)
            throw new BusinessException("Insufficient funds (Consistency check failed)");

        user.setBlockedCash(user.getBlockedCash() + totalCost);         // update user's blocked cash (add total cost)

        Transaction transaction = setNewTransaction(userId, request, true); // set transaction
        Transaction savedTransaction = transactionDao.save(transaction);              // save transaction in MongoDB

        RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);
        user.addLatestTransaction(recTransaction);                      // update embedded lastTransaction in user
        User savedUser = userDao.save(user);                            // update user in MongoDB

        // update user in Redis (cache)
        try {
            userRedisDao.saveFullUserToCache(savedUser);                // update redis cache
        } catch (Exception e) {
            // if Redis fails -> delete to avoid inconsistent data (Cache Eviction)
            userRedisDao.clearUserCache(userId.toString());
        }

        return TransactionMapper.toResponseDTO(savedTransaction);       // return transactionDTO
    }

    /**
     * Executes a market sell order for a specific asset.
     *
     * @param email email fo the user taken by the authentication context
     * @param userId The ID of the user performing the trade.
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return A TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO sellMarketOrder(String email, Long userId, TransactionRequestDTO request)
    {
        User user  = null;                                               // reference of user entity get from MongoDB
        // check on Redis, try to retrieve the details of the specific asset from Redis (Hash)
        WalletItem cachedItem = userRedisDao.getAssetDetails(userId.toString(), request.getSymbol());
        // check key on redis
        if (cachedItem == null) {
            // redis does not have the value, it is retrieved from MongoDB
            user = userDao.findByEmailActive(email).orElseThrow(() -> new BusinessException("User not found"));
            // populate Redis for next time (Self-healing cache) with all user information
            userRedisDao.saveFullUserToCache(user);
            // get the item from the user
            cachedItem = user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType());
        }

        if (cachedItem == null)         // control check
            throw new BusinessException("User doesn't have the asset for this trade");

        // get the information of the asset sold in the user
        double availableQuantity = cachedItem.getQuantity() - cachedItem.getBlockedQuantity();  // get available quantity
        double sellQuantity = request.getQuantity();                // get sell quantity

        // control check
        if (availableQuantity < sellQuantity)
            throw new BusinessException("Insufficient asset's quantity for this trade");

        // get user by email (unique index, fast retrieve). If userHolder[0] is null, it means that the data was in the
        // cache and we need to load the user now. If it is NOT null, already have it.
        if (user == null)
            user = userDao.findByEmailActive(email).orElseThrow(() -> new BusinessException("User session invalid"));

        WalletItem tempWI = user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType());    // get wallet from user
        // consistency check
        if ((tempWI == null) || (availableQuantity != (tempWI.getQuantity() - tempWI.getBlockedQuantity())))
            throw new BusinessException("Insufficient asset's quantity (Consistency check failed)");

        // update portfolio, remove quantity and asset if quantity = 0
        user.updatePortfolioForSell(request.getSymbol(), request.getAssetType(), request.getQuantity(), false, false);
        user.setCash(user.getCash() + request.getTotalPrice());                       // update user's cash

        Transaction transaction = setNewTransaction(userId, request, false);    // set transaction
        Transaction savedTransaction = transactionDao.save(transaction);// save transaction in MongoDB

        RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);
        user.addLatestTransaction(recTransaction);                      // update embedded lastTransaction in user
        User savedUser = userDao.save(user);                            // update user in MongoDB

        // update user in Redis (cache)
        try {
            userRedisDao.saveFullUserToCache(savedUser);                // update redis cache

        } catch (Exception e) {
            // if Redis fails -> delete to avoid inconsistent data (Cache Eviction)
            userRedisDao.clearUserCache(userId.toString());
        }

        return TransactionMapper.toResponseDTO(savedTransaction);       // return transactionDTO
    }

    /**
     * Define a market sell order for a specific asset.
     *
     * @param email email fo the user taken by the authentication context
     * @param userId The ID of the user performing the trade.
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return A TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO sellLimitOrder(String email, Long userId, TransactionRequestDTO request)
    {
        User user = null;                                               // reference of user entity get from MongoDB
        // check on Redis, try to retrieve the details of the specific asset from Redis (Hash)
        WalletItem cachedItem = userRedisDao.getAssetDetails(userId.toString(), request.getSymbol());
        // check key on redis
        if (cachedItem == null) {
            // redis does not have the value, it is retrieved from MongoDB
            user = userDao.findByEmailActive(email).orElseThrow(() -> new BusinessException("User not found"));
            // populate Redis for next time (Self-healing cache) with all user information
            userRedisDao.saveFullUserToCache(user);
            // get the item from the user
            cachedItem = user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType());
        }

        if (cachedItem == null)         // control check
            throw new BusinessException("User doesn't have the asset for this trade");

        // get the information of the asset sold in the user
        double availableQuantity = cachedItem.getQuantity() - cachedItem.getBlockedQuantity();  // get available quantity
        double sellQuantity = request.getQuantity();                // get sell quantity

        // control check
        if (availableQuantity < sellQuantity)
            throw new BusinessException("Insufficient asset's quantity for this trade");

        // get user by email (unique index, fast retrieve). If userHolder[0] is null, it means that the data was in the
        // cache and we need to load the user now. If it is NOT null, already have it.
        if (user == null)
            user = userDao.findByEmailActive(email).orElseThrow(() -> new BusinessException("User session invalid"));

        WalletItem tempWI = user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType());    // get wallet from user
        // consistency check
        if ((tempWI == null) || (availableQuantity != (tempWI.getQuantity() - tempWI.getBlockedQuantity())))
            throw new BusinessException("Insufficient asset's quantity (Consistency check failed)");

        // update portfolio, remove quantity and asset if quantity = 0
        user.updatePortfolioForSell(request.getSymbol(), request.getAssetType(), request.getQuantity(), true, false);

        Transaction transaction = setNewTransaction(userId, request, true);    // set transaction
        Transaction savedTransaction = transactionDao.save(transaction);// save transaction in MongoDB

        RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);
        user.addLatestTransaction(recTransaction);                      // update embedded lastTransaction in user
        User savedUser = userDao.save(user);                            // update user in MongoDB

        // update user in Redis (cache)
        try {
            userRedisDao.saveFullUserToCache(savedUser);                // update redis cache

        } catch (Exception e) {
            // if Redis fails -> delete to avoid inconsistent data (Cache Eviction)
            userRedisDao.clearUserCache(userId.toString());
        }

        return TransactionMapper.toResponseDTO(savedTransaction);       // return transactionDTO
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
        Transaction tx = TransactionMapper.toEntity(tempTransaction, userId);
        // now add remaining fields
        tx.setTransactionId(counterDao.getNextSequence(CounterType.transaction_id));     // get and update transactionId
        if (limitOrder)     // market is closed
            tx.setStatus(TransactionStatus.PENDING);                                    // set status transaction
        else
            tx.setStatus(TransactionStatus.EXECUTED);                                   // set status transaction

        return tx;
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
 *  NOTE 2 - PASS-BY-REFERENCE EMULATION
 *  Java passes arguments by value (copies of references). To allow this method to "return" a User object fetched
 *  during a cache miss back to the caller, we use a single-element array (userHolder). This effectively updates the
 *  caller's reference, preventing a second redundant MongoDB query later in the business logic.
 */
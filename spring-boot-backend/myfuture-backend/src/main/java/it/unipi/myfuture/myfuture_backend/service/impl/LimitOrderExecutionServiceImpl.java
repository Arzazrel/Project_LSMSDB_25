package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.asset.AssetDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.user.UserDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.AssetRedisDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.UserRedisDao;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.FailureReason;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.TransactionMapper;
import it.unipi.myfuture.myfuture_backend.model.RecentTransaction;
import it.unipi.myfuture.myfuture_backend.model.Transaction;
import it.unipi.myfuture.myfuture_backend.model.User;
import it.unipi.myfuture.myfuture_backend.model.WalletItem;
import it.unipi.myfuture.myfuture_backend.service.LimitOrderExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@Service
public class LimitOrderExecutionServiceImpl implements LimitOrderExecutionService {

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

    //------------------------------------------- start: process methods -----------------------------------------------
    /**
     * Method for processing a pending asset purchase or sell request.
     *
     * @param request The DTO containing pending trade details
     * @return a TransactionResponseDTO representing the outcome of the processed transaction.
     * @throws BusinessException if the symbol is invalid, user is deleted, limit price isn't respected
     */
    @Override
    @Transactional
    public TransactionResponseDTO processPendingTrade(Transaction request) {

        User user = null;
        // control check for the transaction type
        TransactionType transactionType = request.getTransactionType();
        if (transactionType != TransactionType.purchase && transactionType != TransactionType.sell)
            throw new BusinessException("Invalid transaction type, transaction canceled.");

        // get current price (last) for the wanted asset for the transaction
        Double currentPrice = assetRedisDao.getCurrentPrice(request.getSymbol());
        // control check, this information is only in Redis. It is also validation check for symbol -> SEE NOTE 0
        if (currentPrice == null)
        {
            // update and save transaction
            Transaction savedTransaction = markTransactionAsFailed(request, FailureReason.ASSET_DELISTED);

            RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);  // convert
            user = userDao.findByUserId(request.getUserId()).orElse(null);  // get also deleted and suspended user
            // manage also case of deleted assets and deleted or suspended user
            if (user != null)
            {
                user.upsertRecentTransaction(recTransaction);               // update recent transaction in user
                // must update quantity value for the asset related to transaction and cash or blocked cash
                updateUserWhenTransactionFailed(request, user);
            }
            return TransactionMapper.toResponseDTO(savedTransaction);       // return the ResponseDTO of the transaction
        }

        // control check discriminate by transaction type, fast check with redis value
        if (transactionType == TransactionType.purchase)    // purchase case
        {
            if (currentPrice > request.getPricePerUnit())   // check if the current price is higher than maximum limit
            {
                // update and save transaction
                Transaction savedTransaction = markTransactionAsFailed(request, FailureReason.PRICE_LIMIT_NOT_MET);

                RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);  // convert
                user = userDao.findByUserId(request.getUserId()).orElse(null);  // get also deleted and suspended user
                if (user != null)
                {
                    user.upsertRecentTransaction(recTransaction);               // update recent transaction in user
                    // must update quantity value for the asset related to transaction and cash or blocked cash
                    updateUserWhenTransactionFailed(request, user);
                }

                return TransactionMapper.toResponseDTO(savedTransaction);       // return the ResponseDTO of the transaction
            }
        }
        else                                                // sell case
        {
            if (currentPrice < request.getPricePerUnit())   // check if the current price is lower than minimum limit
            {
                // update and save transaction
                Transaction savedTransaction = markTransactionAsFailed(request, FailureReason.PRICE_LIMIT_NOT_MET);

                RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);  // convert
                user = userDao.findByUserId(request.getUserId()).orElse(null);  // get also deleted and suspended user
                if (user != null)
                {
                    user.upsertRecentTransaction(recTransaction);               // update recent transaction in user
                    // must update quantity value for the asset related to transaction and cash or blocked cash
                    updateUserWhenTransactionFailed(request, user);
                }

                return TransactionMapper.toResponseDTO(savedTransaction);       // return the ResponseDTO of the transaction
            }
            else
            {
                // update the price per unit and total price with currentPrice
                request.setPricePerUnit(currentPrice);
                request.setTotalPrice(currentPrice * request.getQuantity());
            }
        }

        // control check for the user, check in MongoDB. Check if user is active or suspended or deleted
        user = userDao.findByUserId(request.getUserId()).orElse(null);
        if (user == null)       // there isn't an user with this userid
        {
            // update and save transaction
            Transaction savedTransaction = markTransactionAsFailed(request, FailureReason.UNKNOWN_USER);
            return TransactionMapper.toResponseDTO(savedTransaction);       // return the ResponseDTO of the transaction
        }
        else if(user.getDeleted() || user.getSuspended()) // there is the user is deleted or suspended transaction fails and user's fields have to be updated
        {
            Transaction savedTransaction = null;
            if (user.getDeleted())
                savedTransaction = markTransactionAsFailed(request, FailureReason.USER_DELETED);   // update and save transaction
            else
                savedTransaction = markTransactionAsFailed(request, FailureReason.USER_SUSPENDED);   // update and save transaction

            RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);  // convert
            user.addLatestTransaction(recTransaction);                      // update embedded lastTransaction in user
            // must update quantity value for the asset related to transaction and cash or blocked cash
            updateUserWhenTransactionFailed(request, user);
            return TransactionMapper.toResponseDTO(savedTransaction);       // return the ResponseDTO of the transaction
        }
        // there is the user and is active can execute the transaction

        // discriminate by transaction type
        if (transactionType == TransactionType.purchase)    // purchase case
            return executePurchaseOrder(user, currentPrice, request);   // execute the purchase transactions
        else                                                // sell case
            return executeSellOrder(user, request);                     // execute the sell transactions
    }

    //-------------------------------------------- end: process methods ------------------------------------------------

    //----------------------------------------------- start: methods ---------------------------------------------------

    /**
     * Executes a pending purchase order. The pending order is likely to be executed many hours after the user created
     * it, so that user's data will probably no longer be in the cache. Since the user has already been loaded from
     * MongoDb for verification, the user entity will be used and modified directly, and then the entire content will
     * be updated/inserted into Redis for the user.
     *
     * @param user entity of the user related to transaction
     * @param currentPrice the current price for the asset involved in the transaction
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return a TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO executePurchaseOrder(User user, double currentPrice, Transaction request) {

        double totalCurrentCost = currentPrice * request.getQuantity(); // calculate total current cost of transaction
        double totalPlannedCost = request.getTotalPrice();
        double blockedCash = user.getBlockedCash();

        // control check, error there aren't enough cash for the transaction
        if ((blockedCash < totalCurrentCost) || (totalPlannedCost < totalCurrentCost))
        {
            // update and save transaction
            Transaction savedTransaction = markTransactionAsFailed(request, FailureReason.INSUFFICIENT_FUNDS);

            RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(request);  // convert
            user.upsertRecentTransaction(recTransaction);               // update recent transaction in user
            return TransactionMapper.toResponseDTO(savedTransaction);   // return the ResponseDTO of the transaction
        }

        // update portfolio, add quantity and modify BEP
        user.updatePortfolioForPurchase(request.getSymbol(), request.getAssetType(), request.getQuantity(), currentPrice);
        // calculates the amount of money blocked (the maximum amount the user was willing to spend) that was not used
        // for the purchase and must therefore be made available again as cash in the user's account.
        double savings = totalPlannedCost - totalCurrentCost;
        // update blocked cash, use request total cost because is the prevented total cost
        user.setBlockedCash(user.getBlockedCash() - request.getTotalPrice());
        user.setCash(user.getCash() + savings);

        // update request field
        request.setPricePerUnit(currentPrice);          // update price per unit
        request.setTotalPrice(totalCurrentCost);        // update total cost of the transaction
        request.setStatus(TransactionStatus.EXECUTED);  // set status

        Transaction savedTransaction = transactionDao.save(request);                    // save transaction in MongoDB
        RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction); // create embedded

        user.addLatestTransaction(recTransaction);                      // update embedded lastTransaction in user
        User savedUser = userDao.save(user);                            // update user in MongoDB

        // update user in Redis (cache)
        try {
            userRedisDao.saveFullUserToCache(savedUser);                // update redis cache

        } catch (Exception e) {
            // if Redis fails -> delete to avoid inconsistent data (Cache Eviction)
            userRedisDao.clearUserCache(user.getUserId().toString());
        }

        return TransactionMapper.toResponseDTO(savedTransaction);       // return transactionDTO
    }

    /**
     * Executes a pending sell order. The pending order is likely to be executed many hours after the user created
     * it, so that user's data will probably no longer be in the cache. Since the user has already been loaded from
     * MongoDb for verification, the user entity will be used and modified directly, and then the entire content will
     * be updated/inserted into Redis for the user.
     *
     * @param user entity of the user related to transaction
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     * @return a TransactionResponseDTO containing the result of the operation.
     */
    private TransactionResponseDTO executeSellOrder(User user, Transaction request)
    {
        // get wallet item from user
        WalletItem targetItem = user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType());
        double quantity = request.getQuantity();

        // control checks. When a sell transaction is created, a blocked quantity is added. Now, in order to make a sale,
        // there must be the right quantity, and the blocked quantity must be updated.
        if ((targetItem == null) || (quantity > targetItem.getQuantity()) || (quantity > targetItem.getBlockedQuantity()))
        {
            // update and save transaction
            Transaction savedTransaction = markTransactionAsFailed(request, FailureReason.INSUFFICIENT_ASSET_QUANTITY);

            RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(request);  // convert
            user.upsertRecentTransaction(recTransaction);               // update recent transaction in user
            return TransactionMapper.toResponseDTO(savedTransaction);   // return the ResponseDTO of the transaction
        }

        // update portfolio, remove quantity and blocked quantity and asset if quantity = 0
        user.updatePortfolioForSell(request.getSymbol(), request.getAssetType(), request.getQuantity(), false, true);
        user.setCash(user.getCash() + request.getTotalPrice());                       // update user's cash

        // update request field
        request.setStatus(TransactionStatus.EXECUTED);  // set status

        Transaction savedTransaction = transactionDao.save(request);    // save transaction in MongoDB
        RecentTransaction recTransaction = TransactionMapper.toRecentTransaction(savedTransaction);

        user.addLatestTransaction(recTransaction);                      // update embedded lastTransaction in user
        User savedUser = userDao.save(user);                            // update user in MongoDB

        // update user in Redis (cache)
        try {
            userRedisDao.saveFullUserToCache(savedUser);                // update redis cache

        } catch (Exception e) {
            // if Redis fails -> delete to avoid inconsistent data (Cache Eviction)
            userRedisDao.clearUserCache(user.getUserId().toString());
        }

        return TransactionMapper.toResponseDTO(savedTransaction);       // return transactionDTO
    }

    //------------------------------------------------ end: methods ----------------------------------------------------

    //------------------------------------------ start: utilities methods ----------------------------------------------

    /**
     * update the status of a transaction to FAILED, set the reason, and save to the database
     *
     * @param transaction transaction to update
     * @param reason the reason of the failure
     */
    private Transaction markTransactionAsFailed(Transaction transaction, FailureReason reason) {
        transaction.setStatus(TransactionStatus.FAILED);        // set status
        transaction.setFailureReason(reason);                   // set reason

        return transactionDao.save(transaction);                       // update transaction into MongoDB
    }

    /**
     * method for updating user status (blocked cash, blocked quantity) in case of failed transaction
     *
     * @param transaction failed transaction
     * @param user user to update
     */
    private void updateUserWhenTransactionFailed(Transaction transaction, User user)
    {
        if (transaction.getTransactionType() == TransactionType.purchase)   // purchase case
        {
            user.setBlockedCash(user.getBlockedCash() - transaction.getTotalPrice());   // free blocked cash
            userDao.save(user);                                                         // update user in MongoDB
        }
        else if(transaction.getTransactionType() == TransactionType.sell)   // sell case
        {
            // update only the blocked quantity for the target asset
            WalletItem targetItem = user.getWalletItemBySymbol(transaction.getSymbol(), transaction.getAssetType());
            if (targetItem != null)
            {
                double newqty = targetItem.getBlockedQuantity() - transaction.getQuantity();
                if (newqty > 0)
                    targetItem.setBlockedQuantity(newqty);
                else
                    targetItem.setBlockedQuantity(0);
                userDao.save(user);                                 // update user in MongoDB
            }
        }
        // update user in Redis (cache) if needed
        if (user.getDeleted() || user.getSuspended())
            userRedisDao.clearUserCache(user.getUserId().toString());   // delete user from RedisCache
        else
        {
            try {
                userRedisDao.saveFullUserToCache(user);                // update redis cache
            } catch (Exception e) {
                // if Redis fails -> delete to avoid inconsistent data (Cache Eviction)
                userRedisDao.clearUserCache(transaction.getUserId().toString());
            }
        }
    }

    //------------------------------------------- end: utilities methods -----------------------------------------------
}
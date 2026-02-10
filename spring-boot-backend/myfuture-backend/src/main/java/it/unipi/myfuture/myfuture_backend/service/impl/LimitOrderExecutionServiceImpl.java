package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.TradeDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.user.UserDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.AssetRedisDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.UserRedisDao;
import it.unipi.myfuture.myfuture_backend.enums.FailureReason;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.TransactionMapper;
import it.unipi.myfuture.myfuture_backend.model.Transaction;
import it.unipi.myfuture.myfuture_backend.model.User;
import it.unipi.myfuture.myfuture_backend.model.WalletItem;
import it.unipi.myfuture.myfuture_backend.service.LimitOrderExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LimitOrderExecutionServiceImpl implements LimitOrderExecutionService {

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
     * Method for processing a pending asset purchase or sell request.
     *
     * @param request The DTO containing pending trade details
     * @throws BusinessException if the symbol is invalid, user is deleted, limit price isn't respected
     */
    @Override
    @Transactional
    public void processPendingTrade(Transaction request) {

        // control check for the transaction type
        TransactionType transactionType = request.getTransactionType();
        if (transactionType != TransactionType.purchase && transactionType != TransactionType.sell)
            throw new BusinessException("Invalid transaction type, transaction canceled.");

        // control check for the user, check in MongoDB. Check if user is active or suspended or deleted
        User user = userDao.findByUserId(request.getUserId()).orElse(null);
        if (user == null)       // there isn't an user with this userid
        {
            request.markTransactionAsFailed(FailureReason.UNKNOWN_USER);    // update transaction
            transactionDao.save(request);                                   // save transaction in MongoDB
            return;
        }
        else if(user.getDeleted() || user.getSuspended()) // there is the user is deleted or suspended transaction fails and user's fields have to be updated
        {
            FailureReason reason = user.getDeleted() ? FailureReason.USER_DELETED : FailureReason.USER_SUSPENDED;
            finalizeFailedTransaction(request, user.getUserId(), reason);   // update and save user and transaction
            return;
        }
        // there is the user and is active can execute the transaction

        // get current price (last) for the wanted asset for the transaction
        Double currentPrice = assetRedisDao.getCurrentPrice(request.getSymbol());
        // control check, this information is only in Redis. It is also validation check for symbol -> SEE NOTE 0
        if (currentPrice == null)
        {
            finalizeFailedTransaction(request, user.getUserId(), FailureReason.ASSET_DELISTED); // update and save user and transaction
            return;
        }

        // control check discriminate by transaction type, fast check with redis value
        if (transactionType == TransactionType.purchase)    // purchase case
        {
            if (currentPrice > request.getPricePerUnit())   // check if the current price is higher than maximum limit
            {
                finalizeFailedTransaction(request, user.getUserId(), FailureReason.PRICE_LIMIT_NOT_MET); // update and save user and transaction
                return;
            }
        }
        else                                                // sell case
        {
            if (currentPrice < request.getPricePerUnit())   // check if the current price is lower than minimum limit
            {
                finalizeFailedTransaction(request, user.getUserId(), FailureReason.PRICE_LIMIT_NOT_MET); // update and save user and transaction
                return;
            }
            else
            {
                // update the price per unit and total price with currentPrice
                request.setPricePerUnit(currentPrice);
                request.setTotalPrice(currentPrice * request.getQuantity());
            }
        }

        // discriminate by transaction type
        if (transactionType == TransactionType.purchase)    // purchase case
            executePurchaseOrder(user, currentPrice, request);   // execute the purchase transactions
        else                                                // sell case
            executeSellOrder(user, request);                     // execute the sell transactions
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
     */
    private void executePurchaseOrder(User user, double currentPrice, Transaction request) {

        double totalCurrentCost = currentPrice * request.getQuantity(); // calculate total current cost of transaction
        double totalPlannedCost = request.getTotalPrice();
        double blockedCash = user.getBlockedCash();
        User updatedUser;

        // control check, error there aren't enough cash for the transaction
        if ((blockedCash < totalCurrentCost) || (totalPlannedCost < totalCurrentCost))
        {
            finalizeFailedTransaction(request, user.getUserId(), FailureReason.INSUFFICIENT_FUNDS); // update and save user and transaction
            return;
        }

        // update transaction status and field
        request.setStatus(TransactionStatus.EXECUTED);      // executed status
        request.setPricePerUnit(currentPrice);
        request.setTotalPrice(totalCurrentCost);
        request.setUpdatedAt(Instant.now());                // set UpdatedAt

        // check if user has already this asset
        if (user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType()) == null)
            updatedUser = tradeDao.executePendingPurchaseNewAtomic(user.getUserId(), totalPlannedCost ,request.getTotalPrice(), request.getSymbol(),
                    request.getAssetType(), request.getQuantity(), request.getPricePerUnit(), TransactionMapper.toRecentTransaction(request));
        else
        {
            // calculate new bep
            double newBep = user.calculateNewBep(request.getSymbol(), request.getAssetType(), request.getQuantity(), request.getPricePerUnit());
            updatedUser = tradeDao.executePendingPurchaseExistingAtomic(user.getUserId(), totalPlannedCost, request.getTotalPrice(), request.getSymbol(),
                    request.getAssetType(), request.getQuantity(), newBep, TransactionMapper.toRecentTransaction(request));
        }

        // check if transaction is successfully done or not
        if (updatedUser != null)
        {
            transactionDao.saveWithoutTime(request);    // save transaction in MongoDB, maintain correct updatedAt
            userRedisDao.updateUserInCacheIfActive(updatedUser);// update Redis cache
        }
        else
        {
            // transaction failed, set transaction as failed
            finalizeFailedTransaction(request, user.getUserId(), FailureReason.INSUFFICIENT_FUNDS);
        }
    }

    /**
     * Executes a pending sell order. The pending order is likely to be executed many hours after the user created
     * it, so that user's data will probably no longer be in the cache. Since the user has already been loaded from
     * MongoDb for verification, the user entity will be used and modified directly, and then the entire content will
     * be updated/inserted into Redis for the user.
     *
     * @param user entity of the user related to transaction
     * @param request The DTO containing trade details (symbol, quantity, etc...).
     */
    private void executeSellOrder(User user, Transaction request)
    {
        // get wallet item from user
        WalletItem targetItem = user.getWalletItemBySymbol(request.getSymbol(), request.getAssetType());
        double quantity = request.getQuantity();
        User updatedUser;

        // control checks. When a sell transaction is created, a blocked quantity is added. Now, in order to make a sale,
        // there must be the right quantity, and the blocked quantity must be updated.
        if ((targetItem == null) || (quantity > targetItem.getQuantity()) || (quantity > targetItem.getBlockedQuantity()))
        {
            finalizeFailedTransaction(request, user.getUserId(), FailureReason.INSUFFICIENT_ASSET_QUANTITY); // update and save user and transaction
            return;
        }

        // update transaction status and field
        request.setStatus(TransactionStatus.EXECUTED);      // executed status
        request.setUpdatedAt(Instant.now());                // set UpdatedAt

        // execute sel transaction
        updatedUser = tradeDao.executePendingSellAtomic(user.getUserId(), request.getTotalPrice(), request.getSymbol(),
                    request.getAssetType(), request.getQuantity(), TransactionMapper.toRecentTransaction(request));

        // check if transaction is successfully done or not
        if (updatedUser != null)
        {
            transactionDao.saveWithoutTime(request);    // save transaction in MongoDB, maintain correct updatedAt
            userRedisDao.updateUserInCacheIfActive(updatedUser);// update Redis cache
        }
        else
        {
            // transaction failed, set transaction as failed
            finalizeFailedTransaction(request, user.getUserId(), FailureReason.INSUFFICIENT_ASSET_QUANTITY);
        }
    }

    //------------------------------------------------ end: methods ----------------------------------------------------

    //------------------------------------------ start: utilities methods ----------------------------------------------

    /**
     * method that contains all the steps to update and save a transaction and its user when a transaction fails.
     *
     * @param request failed transaction
     * @param userId identifier of the user related to  the transaction
     * @param reason failure reason
     */
    private Transaction finalizeFailedTransaction(Transaction request, long userId, FailureReason reason) {
        request.markTransactionAsFailed(reason);                        // update failure status transaction
        Transaction savedTransaction = transactionDao.save(request);    // save transaction (update updatedAt)

        // update only user's recent transaction list in MongoDB (with failed transaction)
        tradeDao.addLatestTransaction(userId,TransactionMapper.toRecentTransaction(savedTransaction));
        userRedisDao.clearUserCache(String.valueOf(userId));            // security cache delete for error cases

        return savedTransaction;
    }
    //------------------------------------------- end: utilities methods -----------------------------------------------
}
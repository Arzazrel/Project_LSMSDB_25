package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.CounterDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.TradeDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.user.UserDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.UserRedisDao;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.CounterType;
import it.unipi.myfuture.myfuture_backend.enums.FailureReason;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.TransactionMapper;
import it.unipi.myfuture.myfuture_backend.model.Transaction;
import it.unipi.myfuture.myfuture_backend.model.User;
import it.unipi.myfuture.myfuture_backend.service.DepositWithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class DepositWithdrawalServiceImpl implements DepositWithdrawalService {

    @Autowired
    private UserDao userDao;
    @Autowired
    private TransactionDao transactionDao;
    @Autowired
    private UserRedisDao userRedisDao;
    @Autowired
    private CounterDao counterDao;
    @Autowired
    private TradeDao tradeDao;

    //------------------------------------------- start: process methods -----------------------------------------------
    /**
     * Entry point for processing deposit and withdrawal requests.
     * Validates the user status and routes the request to the specific execution method.
     *
     * @param email   user's email from security context.
     * @param userId  user's unique identifier.
     * @param request DTO containing transaction details (type and amount).
     * @return TransactionResponseDTO with the result of the operation.
     */
    @Override
    @Transactional
    public TransactionResponseDTO processDepositWithdrawal(String email, Long userId, TransactionRequestDTO request) {

        TransactionType type = request.getTransactionType();                // get transaction type
        // validate the transaction type for this specific service
        if (type != TransactionType.deposit && type != TransactionType.withdrawal) {
            throw new BusinessException("Invalid transaction type for Deposit/Withdrawal service.");
        }

        User user = userDao.findByEmail(email).orElse(null);                // get user from MongoDB
        // fetch user from MongoDB and perform status check (Active/Suspended/Deleted)
        if (user == null || user.getDeleted() || user.getSuspended()) {
            // set correct failed motivation
            FailureReason reason = (user == null) ? FailureReason.UNKNOWN_USER :
                    (user.getDeleted() ? FailureReason.USER_DELETED : FailureReason.USER_SUSPENDED);

            long transactionId = counterDao.getNextSequence(CounterType.transaction_id);    // crete new transaction_id
            Transaction newtransaction = setFailedTransaction(userId, request, reason, transactionId);
            Transaction savedTransaction = transactionDao.save(newtransaction);     // save transaction into MongoDb

            // update user's recent transaction list in MongoDB if isn't null
            if (user != null) {
                tradeDao.addLatestTransaction(userId,TransactionMapper.toRecentTransaction(savedTransaction));
                userRedisDao.clearUserCache(String.valueOf(userId));        // security cache delete for error cases
            }

            return TransactionMapper.toResponseDTO(savedTransaction);               // return transaction response DTO
        }

        if (type == TransactionType.deposit) {
            return executeDeposit(user, request);       // execute deposit transaction
        } else {
            return executeWithdrawal(user, request);    // execute withdrawal transaction
        }
    }
    //-------------------------------------------- end: process methods ------------------------------------------------

    //----------------------------------------------- start: methods ---------------------------------------------------

    /**
     * Executes a deposit operation by atomically incrementing the user's cash balance.
     *
     * @param user the validated User entity.
     * @param request the transaction request details.
     * @return TransactionResponseDTO representing the successful deposit.
     */
    private TransactionResponseDTO executeDeposit(User user, TransactionRequestDTO request) {
        double amount = request.getTotalPrice();                // amount is stored in totalPrice field
        long userId = user.getUserId();

        long transactionId = counterDao.getNextSequence(CounterType.transaction_id);    // crete new transaction_id
        // set success transaction
        Transaction newtransaction = setSuccessfulTransaction(userId, request, transactionId);
        // atomic user's cash and embedded array update
        User updatedUser = tradeDao.executeDepositAtomic(user.getUserId(), amount, TransactionMapper.toRecentTransaction(newtransaction));
        // check if transaction is successfully done or not
        if (updatedUser != null)
        {
            userRedisDao.updateUserInCacheIfActive(updatedUser);         // update Redis cache
        }
        else
        {
            // transaction failed, set transaction as failed
            newtransaction = setFailedTransaction(userId, request, FailureReason.CASH_UPDATE_ERROR, transactionId);
            // update only user's recent transaction list in MongoDB (with failed transaction)
            tradeDao.addLatestTransaction(userId, TransactionMapper.toRecentTransaction(newtransaction));
            userRedisDao.clearUserCache(String.valueOf(userId));        // security cache delete for error cases
        }

        transactionDao.saveWithoutTime(newtransaction);     // save transaction in MongoDB, maintain correct updatedAt
        return TransactionMapper.toResponseDTO(newtransaction);         // return response DTO
    }

    /**
     * Executes a withdrawal operation with atomic balance checks to prevent race conditions.
     * Uses a multi-layered check: Redis for speed and an Atomic DAO update for final authorization.
     *
     * @param user    The validated User entity.
     * @param request The transaction request details.
     * @return TransactionResponseDTO representing the outcome of the withdrawal.
     */
    private TransactionResponseDTO executeWithdrawal(User user, TransactionRequestDTO request) {
        double amount = request.getTotalPrice();
        long userId = user.getUserId();

        long transactionId = counterDao.getNextSequence(CounterType.transaction_id);    // crete new transaction_id
        // set success transaction
        Transaction newtransaction = setSuccessfulTransaction(user.getUserId(), request, transactionId);
        // atomic user's cash and embedded array update
        User updatedUser = tradeDao.executeWithdrawalAtomic(userId, amount, TransactionMapper.toRecentTransaction(newtransaction));
        // check if transaction is successfully done or not
        if (updatedUser != null)
        {
            userRedisDao.updateUserInCacheIfActive(updatedUser);         // update Redis cache
        }
        else
        {
            // transaction failed, set transaction as failed
            newtransaction = setFailedTransaction(userId, request, FailureReason.INSUFFICIENT_FUNDS, transactionId);
            // update only user's recent transaction list in MongoDB (with failed transaction)
            tradeDao.addLatestTransaction(user.getUserId(), TransactionMapper.toRecentTransaction(newtransaction));
            userRedisDao.clearUserCache(String.valueOf(userId));        // security cache delete for error cases
        }

        transactionDao.saveWithoutTime(newtransaction);         // save transaction in MongoDB, maintain correct updatedAt
        return TransactionMapper.toResponseDTO(newtransaction); // return response DTO
    }

    //------------------------------------------------ end: methods ----------------------------------------------------

    //------------------------------------------ start: utilities methods ----------------------------------------------

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
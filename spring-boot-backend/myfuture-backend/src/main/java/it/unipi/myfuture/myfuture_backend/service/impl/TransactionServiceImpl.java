package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionAggregationDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.MostTradedAssetDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TotalInvestmentDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TransactionDistributionDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserFinancialFlowDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.enums.TransactionGroupField;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.TransactionMapper;
import it.unipi.myfuture.myfuture_backend.model.Transaction;
import it.unipi.myfuture.myfuture_backend.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction service implementation.
 */
@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionDao transactionDao;

    @Autowired
    private TransactionAggregationDao transactionAggregationDao;

    //----------------------------------------- start: method for CRUD API ---------------------------------------------
    /**
     * Create a new transaction.
     *
     * @param request transaction data coming from client
     * @param userId id of the authenticated user (NOT coming from client)
     * @return created transaction
     */
    @Override
    public TransactionResponseDTO createTransaction(TransactionRequestDTO request, Long userId) {

        Transaction transaction = TransactionMapper.toEntity(request, userId);
        Transaction saved = transactionDao.save(transaction);

        return TransactionMapper.toResponseDTO(saved);
    }

    /**
     * Update the status of a transaction. (used form the system)
     *
     * @param id transaction id
     * @param status new status
     */
    @Override
    public void updateTransactionStatus(Long id, TransactionStatus status) {
        transactionDao.updateTransactionStatus(id, status);
    }

    /**
     * Update a transaction. (non-routine operation)
     *
     * @param transactionId the identifier of the transaction
     * @param request TransactionRequestDTO containing the data for the upload
     */
    @Override
    public TransactionResponseDTO updateTransaction(Long transactionId, TransactionRequestDTO request) {
        // get transaction
        Transaction transaction = transactionDao.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessException("Transaction not found"));

        TransactionMapper.updateEntityFromDTO(transaction, request);        // modify the data of the retrieved transaction
        Transaction savedTransaction = transactionDao.save(transaction);    // update
        return TransactionMapper.toResponseDTO(savedTransaction);           // return the ResponseDTO of the transaction
    }


    /**
     * Retrieve a transaction by its id. Used by both customers and admin.
     *
     * @param id transaction id
     * @return transaction data
     */
    @Override
    public TransactionResponseDTO getTransactionById(Long id) {

        Transaction transaction = transactionDao.findByTransactionId(id)
                .orElseThrow(() -> new BusinessException("Transaction not found"));

        return TransactionMapper.toResponseDTO(transaction);
    }

    /**
     * Retrieve all transactions of a specific user.
     *
     * @param userId id of the user
     * @return list of transactions
     */
    @Override
    public List<TransactionResponseDTO> getTransactionsByUser(Long userId) {

        // retrieve all transaction belonging to a specific user and convert in TransactionResponseDTO and put in a list
        return transactionDao.findByUserId(userId)
                .stream()
                .map(TransactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve transactions using optional filters. Used by both customers and admin.
     * The service decides which DAO logic to apply based on provided parameters.
     *
     * @param status optional transaction status
     * @param type optional transaction type
     * @param userId optional user id (admin only)
     * @param from optional start date
     * @param to optional end date
     * @return list of transactions matching filters
     */
    @Override
    public List<TransactionResponseDTO> searchTransactions(
            TransactionStatus status,
            TransactionType type,
            Long userId,
            Instant from,
            Instant to) {

        // retrieve all transaction belonging to a specific user and convert in TransactionResponseDTO and put in a list
        return transactionDao.search(status, type, userId, from, to)
                .stream()
                .map(TransactionMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Permanently delete a transaction.
     *
     * @param id transaction id
     */
    @Override
    public void deleteTransaction(Long id) {
        transactionDao.deleteById(id);
    }
    //------------------------------------------ end: method for CRUD API ----------------------------------------------
    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * Get the most traded assets based on volume and transaction count.
     *
     * @param window analysis time window
     * @return a list of  MostTradedAssetDTO sorted by descending monetary volume.
     */
    @Override
    public List<MostTradedAssetDTO> getMostTradedAssets(TimeWindow window) {
        return transactionAggregationDao.findMostTradedAssets(window);
    }

    /**
     * Analyze transaction distribution by a specific field (e.g., 'tupe' or 'paymentMethod').
     *
     * @param groupByField The field on which to perform the grouping (e.g., “type” or “paymentMethod”).
     * @param window analysis time window (DAY, WEEK, MONTH, YEAR)
     * @return A list of TransactionDistributionDTO with counts and volumes for each group.
     */
    @Override
    public List<TransactionDistributionDTO> getTransactionDistribution(TransactionGroupField groupByField, TimeWindow window) {
        // Validate grouping field to prevent invalid MongoDB queries
        if (!"category".equals(groupByField.name()) && !"paymentMethod".equals(groupByField.name())) {
            throw new IllegalArgumentException("Invalid grouping field: " + groupByField);
        }
        return transactionAggregationDao.getTransactionDistribution(groupByField, window);
    }

    /**
     * Get the total money invested (BUY operations) globally or for a specific asset.
     *
     * @param symbol (Optional) The symbol of a specific asset. If null, the calculation is global.
     * @param window The time window for calculating the investment.
     * @return A TotalInvestmentDTO containing the amount invested and the number of purchases.
     */
    @Override
    public TotalInvestmentDTO getTotalMoneyInvested(String symbol, TimeWindow window) {
        // Calculate total capital inflow for the platform or a specific asset
        return transactionAggregationDao.getTotalMoneyInvested(symbol, window);
    }

    /**
     * Rank users by their net financial flow (Sales - Purchases).
     *
     * @param window The analysis time window.
     * @param ascending If true, returns users with the worst flow (those who invested the most).
     *                  If false, returns users with the best flow (those who earned the most).
     * @return A list of UserFinancialFlowDTO with details of volumes per user.
     */
    @Override
    public List<UserFinancialFlowDTO> getUserFinancialFlow(TimeWindow window, boolean ascending) {
        // Rank users based on their liquidity impact (net flow)
        return transactionAggregationDao.findUserFinancialFlow(window, ascending);
    }

    //------------------------------------- end: method for aggregation API --------------------------------------------
}
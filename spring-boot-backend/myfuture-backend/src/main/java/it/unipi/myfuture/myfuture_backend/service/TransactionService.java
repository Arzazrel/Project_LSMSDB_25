package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.analytics.MostTradedAssetDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TotalInvestmentDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TransactionDistributionDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserFinancialFlowDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.transaction.TransactionResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.enums.TransactionStatus;
import it.unipi.myfuture.myfuture_backend.enums.TransactionType;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for Transaction entity. (Controllers interact ONLY with this interface layer)
 */
public interface TransactionService {

    //----------------------------------------- start: method for CRUD API ---------------------------------------------
    /**
     * Create a new transaction.
     *
     * @param request transaction data coming from client
     * @param userId id of the authenticated user (NOT coming from client)
     * @return created transaction
     */
    TransactionResponseDTO createTransaction(TransactionRequestDTO request, Long userId);

    /**
     * Retrieve a transaction by its id. Used by both customers and admin.
     *
     * @param id transaction id
     * @return transaction data
     */
    TransactionResponseDTO getTransactionById(Long id);

    /**
     * Retrieve all transactions of a specific user.
     *
     * @param userId id of the user
     * @return list of transactions
     */
    List<TransactionResponseDTO> getTransactionsByUser(Long userId);

    /**
     * Retrieve transactions using optional filters. Used by both customers and admin.
     *
     * @param status optional transaction status
     * @param type optional transaction type
     * @param userId optional user id (admin only)
     * @param from optional start date
     * @param to optional end date
     * @return list of transactions matching filters
     */
    List<TransactionResponseDTO> searchTransactions(TransactionStatus status, TransactionType type, Long userId, Instant from, Instant to);

    /**
     * Update the status of a transaction. (used form the system)
     *
     * @param id transaction id
     * @param status new status
     */
    void updateTransactionStatus(Long id, TransactionStatus status);

    /**
     * Update a transaction. (non-routine operation)
     *
     * @param transactionId transaction id
     * @param request updated transaction
     */
    TransactionResponseDTO updateTransaction(Long transactionId, TransactionRequestDTO request);

    /**
     * Permanently delete a transaction.
     *
     * @param id transaction id
     */
    void deleteTransaction(Long id);

    //------------------------------------------ end: method for CRUD API ----------------------------------------------
    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * Get the most traded assets based on volume and transaction count.
     *
     * @param window analysis time window
     * @return a list of  MostTradedAssetDTO sorted by descending monetary volume.
     */
    List<MostTradedAssetDTO> getMostTradedAssets(TimeWindow window);

    /**
     * Analyze transaction distribution by a specific field (e.g., 'category' or 'paymentMethod').
     *
     * @param groupByField The field on which to perform the grouping (e.g., “category” or “paymentMethod”).
     * @param window analysis time window (DAY, WEEK, MONTH, YEAR)
     * @return A list of TransactionDistributionDTO with counts and volumes for each group.
     */
    List<TransactionDistributionDTO> getTransactionDistribution(String groupByField, TimeWindow window);

    /**
     * Get the total money invested (BUY operations) globally or for a specific asset.
     *
     * @param symbol (Optional) The symbol of a specific asset. If null, the calculation is global.
     * @param window The time window for calculating the investment.
     * @return A TotalInvestmentDTO containing the amount invested and the number of purchases.
     */
    TotalInvestmentDTO getTotalMoneyInvested(String symbol, TimeWindow window);

    /**
     * Rank users by their net financial flow (Sales - Purchases).
     *
     * @param window The analysis time window.
     * @param ascending If true, returns users with the worst flow (those who invested the most).
     *                  If false, returns users with the best flow (those who earned the most).
     * @return A list of UserFinancialFlowDTO with details of volumes per user.
     */
    List<UserFinancialFlowDTO> getUserFinancialFlow(TimeWindow window, boolean ascending);

    //------------------------------------- end: method for aggregation API --------------------------------------------
}

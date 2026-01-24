package it.unipi.myfuture.myfuture_backend.dao.mongo.transaction;

import it.unipi.myfuture.myfuture_backend.dto.analytics.MostTradedAssetDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TotalInvestmentDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TransactionDistributionDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserFinancialFlowDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;

import java.util.List;
/**
 * class that defines aggregations that work on the transaction collection
 */
public interface TransactionAggregationDao {
    /**
     * Calculate the top 10 most traded assets on the platform for day/week/month/year
     *
     * @param window analysis time window (DAY, WEEK, MONTH, YEAR)
     * @return A list of  MostTradedAssetDTO sorted by descending monetary volume.
     */
    List<MostTradedAssetDTO> findMostTradedAssets(TimeWindow window);

    /**
     * Analyze the number of transaction, by categories or by payment type for day/week/month/year.
     * Offering an overview of user payment preferences
     *
     * @param groupByField The field on which to perform the grouping (e.g., “category” or “paymentMethod”).
     * @param window analysis time window (DAY, WEEK, MONTH, YEAR)
     * @return A list of TransactionDistributionDTO with counts and volumes for each group.
     */
    List<TransactionDistributionDTO> getTransactionDistribution(String groupByField, TimeWindow window);

    /**
     * Calculates the total capital invested in the purchase of assets during a specific period.
     * The aggregation filters only ‘BUY’ transactions.
     *
     * @param symbol (Optional) The symbol of a specific asset. If null, the calculation is global.
     * @param window The time window for calculating the investment.
     * @return A TotalInvestmentDTO containing the amount invested and the number of purchases.
     */
    TotalInvestmentDTO getTotalMoneyInvested(String symbol, TimeWindow window);

    /**
     * Rank users based on their net financial flow. The flow is calculated as: (Total Sales) - (Total Purchases).
     * A positive value indicates a user who is liquidating positions (cashing out),
     * A negative value indicates a user who is accumulating assets (investing capital).
     *
     * @param window The analysis time window.
     * @param ascending If true, returns users with the worst flow (those who invested the most).
     *                  If false, returns users with the best flow (those who earned the most).
     * @return A list of UserFinancialFlowDTO with details of volumes per user.
     */
    List<UserFinancialFlowDTO> findUserFinancialFlow(TimeWindow window, boolean ascending);
}

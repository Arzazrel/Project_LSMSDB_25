package it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.transaction.TransactionAggregationDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.MostTradedAssetDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TotalInvestmentDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TransactionDistributionDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserFinancialFlowDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * class that implements aggregations that work on the transactions collection
 */
@Repository
public class TransactionAggregationDaoImpl implements TransactionAggregationDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Calculate the top 10 most traded assets on the platform for day/week/month/year
     *
     * @param window analysis time window (DAY, WEEK, MONTH, YEAR)
     * @return A list of  MostTradedAssetDTO sorted by descending monetary volume.
     */
    @Override
    public List<MostTradedAssetDTO> findMostTradedAssets(TimeWindow window) {
        Instant startDate = DateUtils.calculateStartDate(window);               // calculate the start date

        Aggregation aggregation = Aggregation.newAggregation(
                // filter only transaction more recent from the start date
                Aggregation.match(Criteria.where("timestamp").gte(startDate)),
                // group by symbol
                Aggregation.group("symbol")
                        .count().as("transactionCount")                   // count number of transaction
                        .sum("quantity").as("totalQuantity")    // count the total quantity of asset for all transactions
                        .sum("totalPrice").as("totalVolume"),   // sum the total price (quantity * asset price)
                // Sort by decreasing monetary volume
                Aggregation.sort(Sort.Direction.DESC, "totalVolume"),
                // take top 10
                Aggregation.limit(10),
                // rename the field with the correct name for DTO -> MostTradedAssetDTO has symbol, transactionCount, totalQuantity, totalVolume
                Aggregation.project("transactionCount", "totalQuantity", "totalVolume")
                        .and("_id").as("symbol")
        );

        return mongoTemplate.aggregate(aggregation, "transactions", MostTradedAssetDTO.class).getMappedResults();
    }

    /**
     * Analyze the number of transaction, by categories or by payment type for day/week/month/year.
     * Offering an overview of user payment preferences
     *
     * @param groupByField The field on which to perform the grouping (e.g., “category” or “paymentMethod”).
     * @param window analysis time window (DAY, WEEK, MONTH, YEAR)
     * @return A list of TransactionDistributionDTO with counts and volumes for each group.
     */
    @Override
    public List<TransactionDistributionDTO> getTransactionDistribution(String groupByField, TimeWindow window){
        Instant startDate = DateUtils.calculateStartDate(window);               // calculate the start date

        Aggregation aggregation = Aggregation.newAggregation(
                // filter only transaction more recent from the start date
                Aggregation.match(Criteria.where("timestamp").gte(startDate)),
                // group by parameter (es. "paymentMethod")
                Aggregation.group(groupByField)
                        .count().as("count")
                        .sum("totalPrice").as("totalAmount"),
                // rename the field with the correct name for DTO -> TransactionDistributionDTO has category, count, totalAmount, timeWindow
                Aggregation.project("count", "totalAmount")
                        .and("_id").as("category")
                        .andExpression(window.name()).as("timeWindow")
        );

        return mongoTemplate.aggregate(aggregation, "transactions", TransactionDistributionDTO.class).getMappedResults();
    }

    /**
     * Calculates the total capital invested in the purchase of assets during a specific period.
     * The aggregation filters only ‘BUY’ transactions.
     *
     * @param symbol (Optional) The symbol of a specific asset. If null, the calculation is global.
     * @param window The time window for calculating the investment.
     * @return A TotalInvestmentDTO containing the amount invested and the number of purchases.
     */
    @Override
    public TotalInvestmentDTO getTotalMoneyInvested(String symbol, TimeWindow window) {
        Instant startDate = DateUtils.calculateStartDate(window);       // calculate the start date

        // create dynamic filter: always type = BUY and by data
        Criteria criteria = Criteria.where("type").is("BUY").and("timestamp").gte(startDate);

        // If the symbol is provided, add the filter for the specific asset.
        if (symbol != null && !symbol.isEmpty())
            criteria.and("symbol").is(symbol);

        Aggregation aggregation = Aggregation.newAggregation(
                // match by created filter
                Aggregation.match(criteria),
                // standard group
                Aggregation.group()
                        .sum("totalPrice").as("totalInvested")
                        .count().as("numberOfTransactions"),                // count number of transactions
                // rename the field with the correct name for DTO -> TotalInvestmentDTO has totalInvested, numberOfTransactions, window
                Aggregation.project("totalInvested", "numberOfTransactions")
                        .andExpression(window.name()).as("window")
        );

        // run aggregation
        List<TotalInvestmentDTO> results = mongoTemplate.aggregate(aggregation, "transactions", TotalInvestmentDTO.class).getMappedResults();
        // If there are no transactions in the period, we return an empty DTO or one with zeros.
        return results.isEmpty() ? new TotalInvestmentDTO(0.0, 0L, window) : results.get(0);
    }

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
    @Override
    public List<UserFinancialFlowDTO> findUserFinancialFlow(TimeWindow window, boolean ascending){
        Instant startDate = DateUtils.calculateStartDate(window);       // calculate the start date

        Aggregation aggregation = Aggregation.newAggregation(
                // filter only transaction more recent from the start date
                Aggregation.match(Criteria.where("timestamp").gte(startDate)),
                // conditional flow: if BUY -> totalBought, if SELL -> in totalSold
                Aggregation.project("userId", "totalPrice", "type")
                        .and(ConditionalOperators.when(Criteria.where("type").is("BUY"))
                                .then(Fields.field("totalPrice")).otherwise(0.0)).as("buyAmount")   // sum in buy contributor
                        .and(ConditionalOperators.when(Criteria.where("type").is("SELL"))
                                .then(Fields.field("totalPrice")).otherwise(0.0)).as("sellAmount"), // sum in sell contributor
                // group by userId
                Aggregation.group("userId")
                        .sum("buyAmount").as("totalBought")
                        .sum("sellAmount").as("totalSold"),
                // calculate Net Flow: (Sold - Bought)
                Aggregation.project("totalBought", "totalSold")
                        .and("_id").as("userId")
                        .andExpression("totalSold - totalBought").as("netFlow"),
                // rename the field with the correct name for DTO -> UserFinancialFlowDTO has userId, netFlow, totalBought, totalSold
                Aggregation.project("userId", "netFlow", "totalBought", "totalSold"),
                // order by netFlow
                Aggregation.sort(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "netFlow"),
                // take the top 10
                Aggregation.limit(10)
        );

        return mongoTemplate.aggregate(aggregation, "transactions", UserFinancialFlowDTO.class).getMappedResults();
    }
}
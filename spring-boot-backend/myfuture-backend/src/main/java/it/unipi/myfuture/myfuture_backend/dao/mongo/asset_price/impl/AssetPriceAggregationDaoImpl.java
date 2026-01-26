package it.unipi.myfuture.myfuture_backend.dao.mongo.asset_price.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.asset_price.AssetPriceAggregationDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetGrowthDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetStableTrendDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
/**
 * class that implements aggregations that work on the asset_prices collection
 */
@Repository
public class AssetPriceAggregationDaoImpl implements AssetPriceAggregationDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * View the top 10 assets with the best growth/worst decline last day/week/month.
     *
     * @param window    time window considered
     * @param ascending if true -> worst , if false -> top
     * @return list of AssetGrowthDTO containing the result
     */
    @Override
    public List<AssetGrowthDTO> findAssetPerformance(TimeWindow window, boolean ascending) {

        Instant startDate = DateUtils.calculateStartDate(window);         // calculate the start date

        Aggregation aggregation = Aggregation.newAggregation(
                // filter get only asset_prices more recent than start time
                Aggregation.match(Criteria.where("date").gte(startDate)),
                // sort the timestamp in ascending order
                Aggregation.sort(Sort.Direction.ASC, "date"),
                // group by symbol and take the first price of the window (open) and the last price of the window (close)
                Aggregation.group("symbol")
                        .first("open").as("firstPrice")
                        .last("close").as("lastPrice"),
                // calculate percentage: ((lastPrice - firstPrice) / firstPrice) * 100
                Aggregation.project("firstPrice", "lastPrice")
                        .andExpression("(lastPrice - firstPrice) / firstPrice * 100").as("percentageChange"),
                // sort related the 'ascending' parameter
                Aggregation.sort(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "percentageChange"),
                // take the top/worst
                Aggregation.limit(10),
                // rename the field with the correct name for DTO -> AssetGrowthDTO has symbol, percentageChange, window
                Aggregation.project("percentageChange")
                        .and("_id").as("symbol")
                        .andExpression("'" + window.name() + "'").as("window")
        );

        return mongoTemplate.aggregate(aggregation, "asset_prices", AssetGrowthDTO.class).getMappedResults();
    }

    /**
     * See the 10 assets that have consistently raisen/fell over the past week (i.e., each day closed higher than its
     * opening for a week) and their average daily growth/descent rate.
     *
     * @param positiveTrend if true -> raise , if false -> fell
     * @return list of AssetStableTrendDTO containing the result
     */
    @Override
    public List<AssetStableTrendDTO> findConsistentTrendAssets(boolean positiveTrend) {
        Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);   // calculate the start date
        Object oppositeTrend = positiveTrend ? false : true;                            // get opposite trend

        Aggregation aggregation = Aggregation.newAggregation(
                // filter get only asset_prices more recent than start time
                Aggregation.match(Criteria.where("date").gte(oneWeekAgo)),
                // create a Boolean field: 'true' if positive trend, 'false' if negative trend
                Aggregation.project("symbol")
                        .andExpression("(close - open) / open * 100").as("dailyPriceChange")
                        .and(ConditionalOperators.when(ComparisonOperators.Gt.valueOf("close").greaterThan("open"))
                                .then(true).otherwise(false)).as("isPositive"),
                // group by symbol
                Aggregation.group("symbol")
                        .push("isPositive").as("allDaysTrend")      // group isPositive in an array
                        .avg("dailyPriceChange").as("averageRate")
                        .min("dailyPriceChange").as("minRate")
                        .max("dailyPriceChange").as("maxRate"),
                // filter only the symbol with same trend (positiveTrend) for all the window
                Aggregation.match(Criteria.where("allDaysTrend").nin(oppositeTrend)),
                // sort by averageRate
                Aggregation.sort(positiveTrend ? Sort.Direction.DESC : Sort.Direction.ASC, "averageRate"),
                // take top 10 doc
                Aggregation.limit(10),
                // rename the field with the correct name for DTO -> AssetStableTrendDTO has symbol, averageRate, minRate, maxRate, positiveTrend
                Aggregation.project("averageRate", "minRate", "maxRate")
                        .and("_id").as("symbol")
                        .and(new AggregationExpression() {
                            @Override
                            public org.bson.Document toDocument(AggregationOperationContext context) {
                                return new org.bson.Document("$literal", positiveTrend);
                            }
                        }).as("positiveTrend")
        );

        return mongoTemplate.aggregate(aggregation, "asset_prices", AssetStableTrendDTO.class).getMappedResults();
    }
}

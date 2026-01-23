package it.unipi.myfuture.myfuture_backend.dao.mongo.asset_price;

import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetGrowthDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetStableTrendDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * class that defines aggregations that work on the asset_prices collection
 */
public interface AssetPriceAggregationDao {

    /**
     * View the top 10 assets with the best growth/worst decline last day/week/month.
     *
     * @param window    time window considered
     * @param ascending if true -> growth , if false -> worst
     * @return list of AssetGrowthDTO containing the result
     */
    List<AssetGrowthDTO> findAssetPerformance(TimeWindow window, boolean ascending);

    /**
     * See the 10 assets that have consistently raisen/fell over the past week (i.e., each day closed higher than its
     * opening for a week) and their average daily growth/descent rate.
     *
     * @param positiveTrend if true -> raise , if false -> fell
     * @return list of AssetStableTrendDTO containing the result
     */
    List<AssetStableTrendDTO> findConsistentTrendAssets(boolean positiveTrend);
}
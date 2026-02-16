package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetGrowthDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetStableTrendDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for AssetPrice entity. Defines business operations related to historical asset prices.
 * (Controllers interact ONLY with this interface layer)
 */
public interface AssetPriceService {

    //------------------------------------ start: method for CRUD API -------------------------------------------
    /**
     * Insert or update an asset price entry. Used by admin.
     *
     * @param request asset price data
     * @return saved asset price
     */
    AssetPriceResponseDTO savePrice(AssetPriceRequestDTO request);

    /**
     * Retrieve asset price history within a date range.
     *
     * @param symbol asset symbol
     * @param from start date
     * @param to end date
     * @return list of asset prices
     */
    List<AssetPriceResponseDTO> getPricesBySymbolAndDateRange(String symbol, Instant from, Instant to);

    /**
     * Retrieve latest available asset price.
     *
     * @param symbol asset symbol
     * @return latest asset price
     */
    AssetPriceResponseDTO getLatestPrice(String symbol);

    /**
     * Delete all asset prices associated with a symbol. Used only for extraordinary administrative operations.
     *
     * @param symbol asset symbol
     */
    void deletePrices(String symbol);

    //------------------------------------- end: method for CRUD API --------------------------------------------

    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * View the top 10 assets with the best growth decline last day/week/month.
     *
     * @param window    time window considered
     * @return list of AssetGrowthDTO containing the result
     */
    List<AssetGrowthDTO> getGrowthAnalytics(TimeWindow window);

    /**
     * View the top 10 assets with the best worst decline last day/week/month.
     *
     * @param window    time window considered
     * @return list of AssetGrowthDTO containing the result
     */
    List<AssetGrowthDTO> getWorstAnalytics(TimeWindow window);

    /**
     * See the 10 assets that have consistently raisen over the past week and their average daily growth rate.
     *
     * @return list of AssetStableTrendDTO containing the result
     */
    List<AssetStableTrendDTO> getPositiveStableTrendAnalytics();

    /**
     * See the 10 assets that have consistently fell over the past week and their average daily descent rate.
     *
     * @return list of AssetStableTrendDTO containing the result
     */
    List<AssetStableTrendDTO> getNegativeStableTrendAnalytics();

    //-------------------------------------- end: method for aggregation API -------------------------------------------

    //-------------------------------- start: method for save from Redis to MongoDB ------------------------------------

    /**
     * Methd to consolidate intraday Redis data into a MongoDB OHLC record.
     * Take all the value of the current_price on Redis and data volume from MongoDB to aggregate and calculate the
     * daily price data for an asset and put into MongoDB.
     *
     * @param symbol identifier of the asset
     */
     void consolidateIntradayData(String symbol);

    //--------------------------------- end: method for save from Redis to MongoDB -------------------------------------
}
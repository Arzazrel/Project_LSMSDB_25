package it.unipi.myfuture.myfuture_backend.dao.mongo.asset;

import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetTypeCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorShareCountDTO;
import java.util.List;

/**
 * class that defines aggregations that work on the assets collection
 */
public interface AssetAggregationDao {
    /**
     * Number of assets by type (share / ETF / crypto)
     *
     * @return list of AssetTypeCountDTO containing the result for each asset type
     */
    List<AssetTypeCountDTO> countAssetsByType();

    /**
     * Top 10 sectors by number of listed share
     *
     * @return list of SectorShareCountDTO containing the 10 top sectors and their counts
     */
    List<SectorShareCountDTO> findTop10SectorsByShares();
}
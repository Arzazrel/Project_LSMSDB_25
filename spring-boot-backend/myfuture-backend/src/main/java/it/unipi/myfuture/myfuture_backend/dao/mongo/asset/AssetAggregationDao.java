package it.unipi.myfuture.myfuture_backend.dao.mongo.asset;

import it.unipi.myfuture.myfuture_backend.dto.asset.AssetTypeCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.SectorShareCountDTO;
import java.util.List;

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
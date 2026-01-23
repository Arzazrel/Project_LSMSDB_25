package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.asset.AssetRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetTypeCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.SectorShareCountDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;

import java.util.List;

/**
 * Asset Service interface.
 * Defines all business operations related to asset management. (Controllers interact ONLY with this interface layer)
 */
public interface AssetService {
    //----------------------------------------- start: method for CRUD API ---------------------------------------------
    /**
     * Create a new asset. Used by admin.
     *
     * @param request asset data
     * @return created asset
     */
    AssetResponseDTO createAsset(AssetRequestDTO request);

    /**
     * Update asset metadata. Used by admin.
     *
     * @param symbol asset symbol
     * @param request updated data
     * @return updated asset
     */
    AssetResponseDTO updateAsset(String symbol, AssetRequestDTO request);

    /**
     * Retrieve an active asset by symbol. Used by users and customers.
     *
     * @param symbol asset symbol
     * @return asset data
     */
    AssetResponseDTO getAssetBySymbol(String symbol);

    /**
     * Retrieve all active assets. Used by users and customers.
     *
     * @return list of assets
     */
    List<AssetResponseDTO> getAllAssets();

    /**
     * Retrieve assets filtered by type.
     *
     * @param type asset type
     * @return list of assets
     */
    List<AssetResponseDTO> getAssetsByType(AssetType type);

    /**
     * Soft delete an asset. Used by admin.
     *
     * @param symbol asset symbol
     */
    void deleteAsset(String symbol);

    /**
     * Restore a previously soft-deleted asset. Used by admin.
     *
     * @param symbol asset symbol
     */
    void restoreAsset(String symbol);

    //------------------------------------------ end: method for CRUD API ----------------------------------------------

    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * Calculate number of assets by type (share / ETF / crypto)
     *
     * @return statistics DTO
     */
    List<AssetTypeCountDTO> getAssetTypeDistribution();

    /**
     * Calculate top 10 sectors by number of listed share
     *
     * @return statistics DTO
     */
    List<SectorShareCountDTO> getTopSectorsByShares();
    //------------------------------------- end: method for aggregation API --------------------------------------------
}

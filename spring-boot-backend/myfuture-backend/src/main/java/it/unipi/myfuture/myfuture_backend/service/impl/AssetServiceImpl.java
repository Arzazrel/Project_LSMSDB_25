package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.asset.AssetAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.asset.AssetDao;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetTypeCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorShareCountDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.AssetMapper;
import it.unipi.myfuture.myfuture_backend.model.Asset;
import it.unipi.myfuture.myfuture_backend.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Asset Service implementation.
 */
@Service
public class AssetServiceImpl implements AssetService {

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetAggregationDao assetAggregationDao;

    //----------------------------------------- start: method for CRUD API ---------------------------------------------

    /**
     * Create a new asset. Used by admin.
     *
     * @param request asset data
     * @return created asset
     */
    @Override
    public AssetResponseDTO createAsset(AssetRequestDTO request) {

        Asset asset = AssetMapper.toEntityForCreate(request);   // create new entity from request data
        Asset savedAsset = assetDao.save(asset);                // save new asset entity

        return AssetMapper.toResponseDTO(savedAsset);           // return saved asset
    }

    /**
     * Update asset metadata. Used by admin.
     *
     * @param symbol asset symbol
     * @param request updated data
     * @return updated asset
     */
    @Override
    public AssetResponseDTO updateAsset(String symbol, AssetRequestDTO request) {

        Asset asset = assetDao.findBySymbolActive(symbol).orElseThrow(() -> new BusinessException("Asset not found"));  // get asset
        AssetMapper.updateEntityFromDTO(asset, request);        // update the retrieved asset with request data
        Asset savedAsset = assetDao.save(asset);                // update asset

        return AssetMapper.toResponseDTO(savedAsset);           // return updated asset
    }

    /**
     * Retrieve an active asset by symbol. Used by users and customers.
     *
     * @param symbol asset symbol
     * @return asset data
     */
    @Override
    public AssetResponseDTO getAssetBySymbol(String symbol) {

        Asset asset = assetDao.findBySymbolActive(symbol.trim().toUpperCase()).orElseThrow(() -> new BusinessException("Asset not found"));

        return AssetMapper.toResponseDTO(asset);
    }

    /**
     * Retrieve all active assets. Used by users and customers.
     *
     * @return list of assets
     */
    @Override
    public List<AssetResponseDTO> getAllAssets() {

        // retrieve all active asset and convert in AssetResponseDTO and put in a list
        return assetDao.findAllActive()
                .stream()
                .map(AssetMapper::toResponseDTO)
                .toList();
    }

    /**
     * Retrieve assets filtered by type.
     *
     * @param type asset type
     * @return list of assets
     */
    @Override
    public List<AssetResponseDTO> getAssetsByType(AssetType type) {

        // retrieve all active asset filtered by type and convert in AssetResponseDTO and put in a list
        return assetDao.findByTypeActive(type)
                .stream()
                .map(AssetMapper::toResponseDTO)
                .toList();
    }

    /**
     * Soft delete an asset. Used by admin.
     *
     * @param symbol asset symbol
     */
    @Override
    public void deleteAsset(String symbol) {

        // check: asset must exist and be active
        assetDao.findBySymbolActive(symbol).orElseThrow(() -> new BusinessException("Asset not found"));

        assetDao.softDelete(symbol);            // soft delete the asset
    }

    /**
     * Restore a previously soft-deleted asset. Used by admin.
     *
     * @param symbol asset symbol
     */
    @Override
    public void restoreAsset(String symbol) {
        // check: asset must exist and be deleted
        assetDao.findBySymbol(symbol).orElseThrow(() -> new BusinessException("No price available for symbol: " + symbol));

        assetDao.undoSoftDelete(symbol);        // removes soft delete
    }

    //------------------------------------------ end: method for CRUD API ----------------------------------------------

    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * Calculate number of assets by type (share / ETF / crypto)
     *
     * @return statistics DTO
     */
    @Override
    public List<AssetTypeCountDTO> getAssetTypeDistribution()
    {
        return assetAggregationDao.countAssetsByType();
    }

    /**
     * Calculate top 10 sectors by number of listed share
     *
     * @return statistics DTO
     */
    @Override
    public List<SectorShareCountDTO> getTopSectorsByShares()
    {
        return assetAggregationDao.findTop10SectorsByShares();
    }
    //------------------------------------- end: method for aggregation API --------------------------------------------
}
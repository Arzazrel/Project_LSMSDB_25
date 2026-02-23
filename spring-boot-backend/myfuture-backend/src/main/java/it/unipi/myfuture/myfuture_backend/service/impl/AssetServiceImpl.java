package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.asset.AssetAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.asset.AssetDao;
import it.unipi.myfuture.myfuture_backend.dao.redis.AssetRedisDao;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.AssetTypeCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorShareCountDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.AssetMapper;
import it.unipi.myfuture.myfuture_backend.model.Asset;
import it.unipi.myfuture.myfuture_backend.service.AssetPriceService;
import it.unipi.myfuture.myfuture_backend.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Asset Service implementation.
 */
@Service
public class AssetServiceImpl implements AssetService {

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetAggregationDao assetAggregationDao;

    @Autowired
    private AssetRedisDao assetRedisDao;

    @Autowired
    private AssetPriceService assetPriceService;    // used to save the intraday prices from Redis to MongoDB

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

        // create a small map with information of new asset: symbol -> name
        Map<String, String> singleAssetMap = Collections.singletonMap(
                savedAsset.getSymbol(),
                savedAsset.getLongName()
        );
        assetRedisDao.saveAssetListByType(savedAsset.getType(), singleAssetMap);    // update Redis, added new asset

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

        // create a small map with information of new asset: symbol -> name
        Map<String, String> singleAssetMap = Collections.singletonMap(
                savedAsset.getSymbol(),
                savedAsset.getLongName()
        );
        assetRedisDao.saveAssetListByType(savedAsset.getType(), singleAssetMap);    // update Redis, added new asset

        return AssetMapper.toResponseDTO(savedAsset);           // return updated asset
    }

    /**
     * Retrieves the most recent price for a given asset identified by the symbol. if there isn't current price in redis
     * generate an error.
     *
     * @param symbol The unique identifier (ticker) of the asset.
     * @return The current price of the asset.
     */
    @Override
    public Double getCurrentPrice(String symbol) {
        // attempt to retrieve the real-time price exclusively from the Redis cache
        Double cachedPrice = assetRedisDao.getCurrentPrice(symbol);
        // validation: If the price is not present in Redis, the system treats it as an error
        if (cachedPrice == null) {
            System.err.println("[REDIS MISS] Real-time price not available for symbol: " + symbol);
            // throw business exception as the price must be available in the cache layer
            throw new BusinessException("Asset not found");
        }

        return cachedPrice;         // return the verified price from the cache
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
        // try the retrieve from Redis
        Map<Object, Object> cachedAssets = assetRedisDao.getAssetListByType(type);
        // check if there are assets in Redis
        if (cachedAssets != null && !cachedAssets.isEmpty()) {
            // convert retrieved assets list
            return cachedAssets.entrySet().stream()
                    .map(entry -> {
                        AssetResponseDTO dto = new AssetResponseDTO();
                        dto.setSymbol((String) entry.getKey());
                        dto.setShortName((String) entry.getValue());
                        dto.setType(type);
                        return dto;
                    })
                    .toList();
        }

        // cache miss: Retrieve active assets from MongoDB
        List<Asset> assetsFromDb = assetDao.findByTypeActive(type);
        // map entities to DTOs with null-safe fallback for names
        List<AssetResponseDTO> assetsFromDbDto = assetsFromDb.stream()
                .map(asset -> {
                    AssetResponseDTO dto = AssetMapper.toResponseDTO(asset);
                    // if the name is missing in DB (e.g., BRK.B), use the symbol to prevent downstream NullPointerExceptions
                    if (dto.getShortName() == null) {
                        dto.setShortName(asset.getSymbol());
                    }
                    return dto;
                })
                .toList();

        // update Redis cache
        if (!assetsFromDbDto.isEmpty()) {
            Map<String, String> symbolToNameMap = new java.util.HashMap<>();
            for (AssetResponseDTO dto : assetsFromDbDto) {
                if (dto.getSymbol() != null) {
                    // Ensure we never put a null value into the map
                    String name = (dto.getShortName() != null) ? dto.getShortName() : dto.getSymbol();
                    symbolToNameMap.put(dto.getSymbol(), name);
                }
            }
            assetRedisDao.saveAssetListByType(type, symbolToNameMap);
        }

        return assetsFromDbDto;
    }

    /**
     * Soft delete an asset. Used by admin.
     *
     * @param symbol asset symbol
     */
    @Override
    public void deleteAsset(String symbol) {
        // check: asset must exist and be active
        Asset asset = assetDao.findBySymbolActive(symbol).orElseThrow(() -> new BusinessException("Asset not found"));

        assetDao.softDelete(symbol);                                // soft delete the asset
        assetPriceService.consolidateIntradayData(symbol);          // save today's data before delete it
        assetRedisDao.deleteFullAssetData(symbol, asset.getType()); // clear all Redis data related to this asset
    }

    /**
     * Restore a previously soft-deleted asset. Used by admin.
     *
     * @param symbol asset symbol
     */
    @Override
    public void restoreAsset(String symbol) {
        // check: asset must exist and be deleted
        Asset asset = assetDao.findBySymbol(symbol).orElseThrow(() -> new BusinessException("No price available for symbol: " + symbol));

        assetDao.undoSoftDelete(symbol);        // removes soft delete
        // create a small map with information of new asset: symbol -> name
        Map<String, String> singleAssetMap = Collections.singletonMap(
                asset.getSymbol(),
                asset.getLongName()
        );
        assetRedisDao.saveAssetListByType(asset.getType(), singleAssetMap);     // update Redis, added new asset
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
package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.AssetDao;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
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

    // ------------------------------------------------ start: asset API --------------------------------------------------

    /**
     * Create a new asset. Used by admin.
     *
     * @param request asset data
     * @return created asset
     */
    @Override
    public AssetResponseDTO createAsset(AssetRequestDTO request) {

        Asset asset = AssetMapper.toEntityForCreate(request);

        Asset savedAsset = assetDao.save(asset);

        return AssetMapper.toResponseDTO(savedAsset);
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

        Asset asset = assetDao.findBySymbolActive(symbol)
                .orElseThrow(() -> new BusinessException("Asset not found"));

        AssetMapper.updateEntityFromDTO(asset, request);

        Asset savedAsset = assetDao.save(asset);

        return AssetMapper.toResponseDTO(savedAsset);
    }

    /**
     * Retrieve an active asset by symbol. Used by users and customers.
     *
     * @param symbol asset symbol
     * @return asset data
     */
    @Override
    public AssetResponseDTO getAssetBySymbol(String symbol) {

        Asset asset = assetDao.findBySymbolActive(symbol)
                .orElseThrow(() -> new BusinessException("Asset not found"));

        return AssetMapper.toResponseDTO(asset);
    }

    /**
     * Retrieve all active assets. Used by users and customers.
     *
     * @return list of assets
     */
    @Override
    public List<AssetResponseDTO> getAllAssets() {

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

        assetDao.softDelete(symbol);
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

        assetDao.undoSoftDelete(symbol);
    }

    // ------------------------------------------------ end: asset API --------------------------------------------------
}
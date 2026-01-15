package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dao.mongo.AssetDao;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.model.Asset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Asset entity.
 *
 * Handles asset catalog operations, including creation, update,
 * retrieval and soft deletion. Used by users and administrators.
 */
@Service
public class AssetService {

    @Autowired
    private AssetDao assetDao;

    /**
     * Create or update an asset.
     *
     * @param asset asset metadata
     * @return saved asset
     */
    public Asset saveAsset(Asset asset) {
        return assetDao.save(asset);
    }

    /**
     * Retrieve an active asset by symbol.
     *
     * @param symbol asset symbol
     * @return Optional containing the asset if found and not deleted
     */
    public Optional<Asset> getAssetBySymbol(String symbol) {
        return assetDao.findBySymbol(symbol);
    }

    /**
     * Retrieve all active assets.
     *
     * @return list of active assets
     */
    public List<Asset> getAllAssets() {
        return assetDao.findAllActive();
    }

    /**
     * Retrieve all assets of a specific type (share, ETF, crypto).
     *
     * @param type asset type
     * @return list of assets
     */
    public List<Asset> getAssetsByType(AssetType type) {
        return assetDao.findByType(type);
    }

    /**
     * Soft delete an asset (admin operation).
     *
     * @param symbol asset symbol
     */
    public void deleteAsset(String symbol) {
        assetDao.softDelete(symbol);
    }

    /**
     * Restore a previously soft-deleted asset.
     *
     * @param symbol asset symbol
     */
    public void restoreAsset(String symbol) {
        assetDao.undoSoftDelete(symbol);
    }
}
package it.unipi.myfuture.myfuture_backend.dao.mongo;

import it.unipi.myfuture.myfuture_backend.model.Asset;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Asset collection.
 * Manage persistence and queries for financial assets (shares, ETFs, cryptocurrencies) stored in MongoDB.
 *
 * Collection: assets
 */
@Repository
public class AssetDao {

    @Autowired
    private MongoTemplate mongoTemplate;

     /**
     * Insert or update an Asset.
     *
     * @param asset the asset to be entered(saved)
     * @return the inserted object
     */
    public Asset save(Asset asset) {
        return mongoTemplate.save(asset);
    }

    /**
     * Find an asset by its unique symbol. Used by users, customers and admins.
     */
    public Optional<Asset> findBySymbol(String symbol) {
        Query query = new Query(
                Criteria.where("symbol").is(symbol)
                        .and("deleted").ne(true)
        );
        return Optional.ofNullable(mongoTemplate.findOne(query, Asset.class));
    }

    /**
     * Find an asset by symbol including soft-deleted ones (admin only).
     *
     * @param symbol the symbol that identify the asset
     * @return Optional containing the News if found, otherwise empty
     */
    public Optional<Asset> findBySymbolAdmin(String symbol) {
        Query query = new Query(
                Criteria.where("symbol").is(symbol)
        );
        return Optional.ofNullable(mongoTemplate.findOne(query, Asset.class));
    }

    /**
     * Retrieve all assets (excluding soft-deleted).
     *
     * @return list of assets documents
     */
    public List<Asset> findAllActive() {
        Query query = new Query(
                Criteria.where("deleted").ne(true)
        );
        return mongoTemplate.find(query, Asset.class);
    }

    /**
     * Retrieve all assets of a specific type.
     *
     * @return list of assets documents by type (share, crypto, etf)
     */
    public List<Asset> findByType(AssetType type) {
        Query query = new Query(
                Criteria.where("type").is(type)
                        .and("deleted").ne(true)
        );
        return mongoTemplate.find(query, Asset.class);
    }

    /**
     * Retrieve all assets including soft-deleted ones (admin only).
     *
     * @return list of assets documents
     */
    public List<Asset> findAllAdmin() {
        return mongoTemplate.findAll(Asset.class);
    }

    /**
     * Soft delete an asset by symbol.
     *
     * @param symbol the symbol that identify the asset
     */
    public void softDelete(String symbol) {
        Query query = new Query(Criteria.where("symbol").is(symbol));
        Asset asset = mongoTemplate.findOne(query, Asset.class);

        if (asset != null) {
            asset.setDeleted(true);
            asset.setDeletedAt(Instant.now());
            mongoTemplate.save(asset);
        }
    }

    /**
     * Undo Soft delete an asset by symbol.
     *
     * @param symbol the symbol that identify the asset
     */
    public void undoSoftDelete(String symbol) {
        Query query = new Query(
                Criteria.where("symbol").is(symbol)
                        .and("deleted").is(true)
        );

        Update update = new Update()
                .set("deleted", false)
                .unset("deletedAt");

        mongoTemplate.updateFirst(query, update, Asset.class);
    }
}
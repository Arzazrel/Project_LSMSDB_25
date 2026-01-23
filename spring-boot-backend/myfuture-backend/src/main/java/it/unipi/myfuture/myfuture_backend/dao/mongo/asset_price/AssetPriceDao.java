package it.unipi.myfuture.myfuture_backend.dao.mongo.asset_price;

import it.unipi.myfuture.myfuture_backend.model.AssetPrice;
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
 * DAO for historical asset prices.
 * Used for charts, analytics and time-series queries.
 *
 * Collection: asset_prices
 */
@Repository
public class AssetPriceDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Insert or update asset_price.
     *
     * @param price the asset_price to be entered(saved)
     * @return the inserted object
     */
    public AssetPrice save(AssetPrice price) {
        return mongoTemplate.save(price);
    }

    /**
     * Find all asset prices for an asset within a date range
     *
     * @param symbol ID of the asset related to asset_prices
     * @param from start date
     * @param to end date
     * @return list of asset prices founded
     */
    public List<AssetPrice> findBySymbolAndDateRange(
            String symbol, Instant from, Instant to) {

        Query query = new Query(
                Criteria.where("symbol").is(symbol)
                        .and("date").gte(from).lte(to)
                        .and("deleted").ne(true)
        );
        return mongoTemplate.find(query, AssetPrice.class);
    }

    /**
     * Get the last prices (day summary) for a symbol 8asset).
     *
     * @param symbol ID of the asset related to asset_prices
     * @return Optional containing the asset if found, otherwise empty
     */
    public Optional<AssetPrice> findLatestBySymbol(String symbol) {
        Query query = new Query(
                Criteria.where("symbol").is(symbol)
                        .and("deleted").ne(true)
        );
        query.limit(1);
        query.with(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "date"
                )
        );
        return Optional.ofNullable(mongoTemplate.findOne(query, AssetPrice.class));
    }

    /**
     * Delete all asset prices associated with a symbol.
     * Used only for extraordinary administrative operations.
     */
    public void deleteBySymbol(String symbol) {

        Query query = new Query(Criteria.where("symbol").is(symbol));
        mongoTemplate.remove(query, AssetPrice.class);
    }


}

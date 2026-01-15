package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dao.mongo.AssetPriceDao;
import it.unipi.myfuture.myfuture_backend.model.AssetPrice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for AssetPrice entity.
 *
 * Used to retrieve historical price data for charts,
 * analytics and asset visualization.
 */
@Service
public class AssetPriceService {

    @Autowired
    private AssetPriceDao assetPriceDao;

    /**
     * Insert or update an asset price.
     *
     * @param assetPrice asset price entry
     * @return saved asset price
     */
    public AssetPrice savePrice(AssetPrice assetPrice) {
        return assetPriceDao.save(assetPrice);
    }

    /**
     * Retrieve price history for an asset within a date range.
     *
     * @param symbol asset symbol
     * @param from start date
     * @param to end date
     * @return list of prices
     */
    public List<AssetPrice> getPricesBySymbolAndDateRange(
            String symbol, Instant from, Instant to) {
        return assetPriceDao.findBySymbolAndDateRange(symbol, from, to);
    }

    /**
     * Retrieve the latest price for an asset.
     *
     * @param symbol asset symbol
     * @return latest asset price
     */
    public Optional<AssetPrice> getLatestPrice(String symbol) {
        return assetPriceDao.findLatestBySymbol(symbol);
    }
}
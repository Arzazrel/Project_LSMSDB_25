package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.AssetPriceDao;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;
import it.unipi.myfuture.myfuture_backend.mapper.AssetPriceMapper;
import it.unipi.myfuture.myfuture_backend.model.AssetPrice;
import it.unipi.myfuture.myfuture_backend.service.AssetPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * AssetPrice Service implementation.
 * Handles business logic related to historical asset prices.
 */
@Service
public class AssetPriceServiceImpl implements AssetPriceService {

    @Autowired
    private AssetPriceDao assetPriceDao;

    // ---------------------------------------------- start: asset_price API ----------------------------------------------

    /**
     * Insert or update an asset price entry. Used by admin.
     *
     * @param request asset price data
     * @return saved asset price
     */
    @Override
    public AssetPriceResponseDTO savePrice(AssetPriceRequestDTO request) {

        AssetPrice price = AssetPriceMapper.toEntity(request);  // convert
        AssetPrice savedPrice = assetPriceDao.save(price);      // save

        return AssetPriceMapper.toResponseDTO(savedPrice);
    }

    /**
     * Retrieve asset price history within a date range.
     *
     * @param symbol asset symbol
     * @param from start date
     * @param to end date
     * @return list of asset prices
     */
    @Override
    public List<AssetPriceResponseDTO> getPricesBySymbolAndDateRange(String symbol, Instant from, Instant to) {

        // retrieve all assetPrices and convert in AssetPriceResponseDTO and put in a list
        return assetPriceDao.findBySymbolAndDateRange(symbol, from, to)
                .stream()
                .map(AssetPriceMapper::toResponseDTO)
                .toList();
    }

    /**
     * Retrieve latest available asset price.
     *
     * @param symbol asset symbol
     * @return latest asset price
     */
    @Override
    public AssetPriceResponseDTO getLatestPrice(String symbol) {

        AssetPrice price = assetPriceDao.findLatestBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("No price available"));

        return AssetPriceMapper.toResponseDTO(price);
    }

    /**
     * Delete all asset prices associated with a symbol. Used only for extraordinary administrative operations.
     *
     * @param symbol asset symbol
     */
    @Override
    public void deletePrices(String symbol) {
        assetPriceDao.deleteBySymbol(symbol);   // real delete, not a soft delete
    }

    // ---------------------------------------------- end: asset_price API ------------------------------------------------
}
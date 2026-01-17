package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for AssetPrice entity. Defines business operations related to historical asset prices.
 * (Controllers interact ONLY with this interface layer)
 */
public interface AssetPriceService {

    /**
     * Insert or update an asset price entry. Used by admin.
     *
     * @param request asset price data
     * @return saved asset price
     */
    AssetPriceResponseDTO savePrice(AssetPriceRequestDTO request);

    /**
     * Retrieve asset price history within a date range.
     *
     * @param symbol asset symbol
     * @param from start date
     * @param to end date
     * @return list of asset prices
     */
    List<AssetPriceResponseDTO> getPricesBySymbolAndDateRange(String symbol, Instant from, Instant to);

    /**
     * Retrieve latest available asset price.
     *
     * @param symbol asset symbol
     * @return latest asset price
     */
    AssetPriceResponseDTO getLatestPrice(String symbol);

    /**
     * Delete all asset prices associated with a symbol. Used only for extraordinary administrative operations.
     *
     * @param symbol asset symbol
     */
    void deletePrices(String symbol);

}
package it.unipi.myfuture.myfuture_backend.mapper;

import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.assetPrice.AssetPriceResponseDTO;
import it.unipi.myfuture.myfuture_backend.model.AssetPrice;

/**
 * AssetPrice Mapper handles conversion between AssetPrice entity and AssetPrice DTOs.
 * Used inside service layer to keep business logic clean.
 */
public class AssetPriceMapper {

    // ---------------------------------------------- start: request mapping ---------------------------------------------

    /**
     * Convert AssetPriceRequestDTO to AssetPrice entity.
     *
     * @param AssPriceRequest asset price request DTO
     * @return asset price entity
     */
    public static AssetPrice toEntity(AssetPriceRequestDTO AssPriceRequest) {

        AssetPrice price = new AssetPrice();

        price.setSymbol(AssPriceRequest.getSymbol());
        price.setDate(AssPriceRequest.getTimestamp());

        price.setOpen(AssPriceRequest.getOpen());
        price.setHigh(AssPriceRequest.getHigh());
        price.setLow(AssPriceRequest.getLow());
        price.setClose(AssPriceRequest.getClose());
        price.setVolume(AssPriceRequest.getVolume());

        return price;
    }

    // ---------------------------------------------- end: request mapping -----------------------------------------------


    // ---------------------------------------------- start: response mapping --------------------------------------------

    /**
     * Convert AssetPrice entity to AssetPriceResponseDTO.
     *
     * @param price asset price entity
     * @return asset price response DTO
     */
    public static AssetPriceResponseDTO toResponseDTO(AssetPrice price) {

        AssetPriceResponseDTO response = new AssetPriceResponseDTO();

        response.setSymbol(price.getSymbol());
        response.setTimestamp(price.getDate());

        response.setOpen(price.getOpen());
        response.setHigh(price.getHigh());
        response.setLow(price.getLow());
        response.setClose(price.getClose());
        response.setVolume(price.getVolume());

        return response;
    }

    // ---------------------------------------------- end: response mapping ----------------------------------------------
}
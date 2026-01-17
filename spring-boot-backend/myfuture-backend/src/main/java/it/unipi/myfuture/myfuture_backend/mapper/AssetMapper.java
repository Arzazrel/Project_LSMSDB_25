package it.unipi.myfuture.myfuture_backend.mapper;

import it.unipi.myfuture.myfuture_backend.dto.asset.AssetRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetResponseDTO;
import it.unipi.myfuture.myfuture_backend.model.Asset;

import java.time.Instant;

/**
 * Asset Mapper handles conversion between Asset entity and Asset DTOs.
 * Used inside service layer to keep business logic clean and separated.
 */
public class AssetMapper {

    //----------------------------------------- start: create mapping (request) ----------------------------------------

    /**
     * Build a new Asset entity from AssetRequestDTO. Used ONLY for create operations.
     *
     * @param AssetRequest asset request DTO
     * @return new Asset entity
     */
    public static Asset toEntityForCreate(AssetRequestDTO AssetRequest) {

        Asset asset = new Asset();

        asset.setSymbol(AssetRequest.getSymbol());
        asset.setShortName(AssetRequest.getShortName());
        asset.setLongName(AssetRequest.getLongName());
        asset.setType(AssetRequest.getType());

        // common fields
        asset.setCountry(AssetRequest.getCountry());

        // share only
        asset.setSector(AssetRequest.getSector());
        asset.setIndustry(AssetRequest.getIndustry());

        // ETF only
        asset.setFundFamily(AssetRequest.getFundFamily());
        asset.setAnnualReportExpenseRatio(AssetRequest.getAnnualReportExpenseRatio());
        asset.setTotalAssets(AssetRequest.getTotalAssets());

        // crypto only
        asset.setCurrency(AssetRequest.getCurrency());
        asset.setCirculatingSupply(AssetRequest.getCirculatingSupply());
        asset.setMaxSupply(AssetRequest.getMaxSupply());

        // metadata
        asset.setDeleted(false);                // default value, is active
        asset.setDeletedAt(null);
        asset.setIngestedAt(Instant.now());     // set the current timestamp

        return asset;
    }

    //------------------------------------------ end: create mapping (request) -----------------------------------------

    //------------------------------------------ start: update mapping (request) ---------------------------------------

    /**
     * Update an existing Asset entity using data from AssetRequestDTO. Used ONLY for update operations.
     *
     * @param asset existing asset entity
     * @param AssetRequest asset request DTO
     */
    public static void updateEntityFromDTO(Asset asset, AssetRequestDTO AssetRequest) {

        asset.setShortName(AssetRequest.getShortName());
        asset.setLongName(AssetRequest.getLongName());
        asset.setType(AssetRequest.getType());

        // common fields
        asset.setCountry(AssetRequest.getCountry());

        // share only
        asset.setSector(AssetRequest.getSector());
        asset.setIndustry(AssetRequest.getIndustry());

        // ETF only
        asset.setFundFamily(AssetRequest.getFundFamily());
        asset.setAnnualReportExpenseRatio(AssetRequest.getAnnualReportExpenseRatio());
        asset.setTotalAssets(AssetRequest.getTotalAssets());

        // crypto only
        asset.setCurrency(AssetRequest.getCurrency());
        asset.setCirculatingSupply(AssetRequest.getCirculatingSupply());
        asset.setMaxSupply(AssetRequest.getMaxSupply());
    }

    //----------------------------------------- end: update mapping (request) ------------------------------------------

    //---------------------------------------------- start: response mapping -------------------------------------------

    /**
     * Convert Asset entity to AssetResponseDTO. Used to expose asset data to controllers.
     *
     * @param asset asset entity
     * @return asset response DTO
     */
    public static AssetResponseDTO toResponseDTO(Asset asset) {

        AssetResponseDTO response = new AssetResponseDTO();

        response.setSymbol(asset.getSymbol());
        response.setShortName(asset.getShortName());
        response.setLongName(asset.getLongName());
        response.setType(asset.getType());

        // common fields
        response.setCountry(asset.getCountry());

        // share only
        response.setSector(asset.getSector());
        response.setIndustry(asset.getIndustry());

        // ETF only
        response.setFundFamily(asset.getFundFamily());
        response.setAnnualReportExpenseRatio(asset.getAnnualReportExpenseRatio());
        response.setTotalAssets(asset.getTotalAssets());

        // crypto only
        response.setCurrency(asset.getCurrency());
        response.setCirculatingSupply(asset.getCirculatingSupply());
        response.setMaxSupply(asset.getMaxSupply());

        // soft delete info
        response.setDeleted(asset.isDeleted());
        response.setDeletedAt(asset.getDeletedAt());

        // ingestion timestamp
        response.setIngestedAt(asset.getIngestedAt());

        return response;
    }

    //---------------------------------------------- end: response mapping ---------------------------------------------
}
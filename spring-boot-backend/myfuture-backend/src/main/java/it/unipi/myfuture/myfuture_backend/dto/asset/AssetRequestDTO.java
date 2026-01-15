package it.unipi.myfuture.myfuture_backend.dto.asset;

import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import lombok.Data;

/**
 * DTO used for asset creation and update requests. It represents only the fields that can be provided by administrators.
 */
@Data
public class AssetRequestDTO {

    private String symbol;
    private String shortName;
    private String longName;
    private AssetType type;

    // field for both share and ETF
    private String country;

    // fields share only
    private String sector;
    private String industry;

    // fields ETF only
    private String fundFamily;
    private Double annualReportExpenseRatio;
    private Double totalAssets;

    // fields crypto only
    private String currency;
    private Double circulatingSupply;
    private Double maxSupply;
}
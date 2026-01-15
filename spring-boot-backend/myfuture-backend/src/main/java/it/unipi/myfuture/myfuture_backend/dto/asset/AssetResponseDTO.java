package it.unipi.myfuture.myfuture_backend.dto.asset;

import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import lombok.Data;

import java.time.Instant;

/**
 * DTO used to expose asset information to users.
 */
@Data
public class AssetResponseDTO {

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

    // soft delete
    private boolean deleted;
    private Instant deletedAt;

    // ingested at (for tracking when the asset was added to DB)
    private Instant ingestedAt;
}
package it.unipi.myfuture.myfuture_backend.model;

import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Date;

/**
 * Asset entity representing an asset document in MongoDB.
 * It contains common fields and fields specific to asset types (share, ETF, crypto).
 * Also includes soft delete support.
 *
 * Collection: assets
 */
@Data
@Document(collection = "assets")
public class Asset {

    @Id
    private String id;              // MongoDB _id

    private String symbol;           // unique symbol of the asset

    private String shortName;        // short name
    private String longName;         // full descriptive name
    private AssetType type;          // type: SHARE, ETF, CRYPTO

    // fields for share and ETF
    private String country;

    // fields for share
    private String sector;
    private String industry;

    // fields for ETF
    private String fundFamily;
    private Double annualReportExpenseRatio;
    private Double totalAssets;

    // fields for crypto
    private String currency;
    private Double circulatingSupply;
    private Double maxSupply;

    // soft delete
    private boolean deleted;
    private Instant deletedAt;

    // ingested at (for tracking when the asset was added to DB)
    private Instant ingestedAt;
}
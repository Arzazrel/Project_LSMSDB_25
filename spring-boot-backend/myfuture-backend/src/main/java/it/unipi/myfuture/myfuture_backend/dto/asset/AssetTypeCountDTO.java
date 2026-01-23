package it.unipi.myfuture.myfuture_backend.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for the asset count, for a specific type, aggregation (in asset collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetTypeCountDTO {
    private String type;        // asset type (share, etf, crypto)
    private Long count;         // total quantity of assets for the selected type in DB
}

package it.unipi.myfuture.myfuture_backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for the global average statistics aggregation (in users collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalUserStatsDTO {
    private Double avgDistinctAssets;   // the average quantity of distinct assets
    private Double minAssetHeld;        // the minimum quantity of distinct assets (not the quantity of individual assets) owned by database users
    private Integer maxAssetsHeld;      // the maximum quantity of distinct assets (not the quantity of individual assets) owned by database users
}

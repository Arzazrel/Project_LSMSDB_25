package it.unipi.myfuture.myfuture_backend.dto.analytics;

import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for best/worst assets growth aggregation (in asset_prices collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetGrowthDTO {
    private String symbol;              // symbol of the asset
    private Double percentageChange;    // growth or loss in percentage
    private TimeWindow window;          // time window considered
}

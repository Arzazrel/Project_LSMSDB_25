package it.unipi.myfuture.myfuture_backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for consistently raise/fell assets prices aggregation (in asset_prices collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetStableTrendDTO {
    private String symbol;          // symbol of the asset
    private Double averageRate;     // average raise/fell rate
    private Double minRate;         // min raise/fell rate
    private Double maxRate;         // max raise/fell rate
    boolean positiveTrend;          // if true -> raise , if false -> fell
}
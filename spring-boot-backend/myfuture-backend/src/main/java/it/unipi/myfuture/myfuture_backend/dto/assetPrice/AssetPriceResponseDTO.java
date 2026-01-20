package it.unipi.myfuture.myfuture_backend.dto.assetPrice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO used to expose historical asset prices for charts and analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetPriceResponseDTO {

    private Instant timestamp;      // Price date (UTC, time usually set to 00:00)
    private String symbol;

    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
}
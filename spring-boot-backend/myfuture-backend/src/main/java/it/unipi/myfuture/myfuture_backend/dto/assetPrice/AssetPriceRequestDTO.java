package it.unipi.myfuture.myfuture_backend.dto.assetPrice;

import lombok.Data;

import java.time.Instant;

/**
 * DTO used to insert or update asset price entries.
 *
 * Used only by admin APIs.
 */
@Data
public class AssetPriceRequestDTO {

    private Instant timestamp;          // Price date (UTC, time usually set to 00:00)
    private String symbol;

    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;
}
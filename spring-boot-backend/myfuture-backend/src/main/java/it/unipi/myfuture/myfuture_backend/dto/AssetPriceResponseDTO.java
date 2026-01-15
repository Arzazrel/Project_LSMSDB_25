package it.unipi.myfuture.myfuture_backend.dto;

import lombok.Data;

import java.time.Instant;

/**
 * DTO used to expose historical asset prices for charts and analytics.
 */
@Data
public class AssetPriceResponseDTO {

    private Instant date;

    private String symbol;

    private double open;
    private double high;
    private double low;
    private double close;

    private long volume;
}
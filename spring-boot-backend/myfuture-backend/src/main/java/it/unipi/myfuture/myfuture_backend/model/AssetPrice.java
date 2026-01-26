package it.unipi.myfuture.myfuture_backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Represents the historical price of an asset for a specific date.
 * Used for charts, analytics and price history queries.
 *
 * Collection: asset_prices
 */
@Data
@Document(collection = "asset_prices")
public class AssetPrice {
    @Id
    private String id;              // MongoDB _id
    private Instant date;           // Price date (UTC, time usually set to 00:00)
    private String symbol;

    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    private Instant ingestedAt;
}

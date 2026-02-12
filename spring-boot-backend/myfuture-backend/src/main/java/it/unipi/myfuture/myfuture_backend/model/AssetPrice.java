package it.unipi.myfuture.myfuture_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
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

    public AssetPrice(Instant date, String symbol, double open, double high, double low, double close, long volume) {
        this.date = date;
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.ingestedAt = Instant.now();
    }
}

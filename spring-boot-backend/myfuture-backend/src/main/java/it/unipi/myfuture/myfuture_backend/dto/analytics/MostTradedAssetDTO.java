package it.unipi.myfuture.myfuture_backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for the most traded asset aggregation (in transactions collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MostTradedAssetDTO {
    private String symbol;
    private Long transactionCount;      // count of the operations
    private Double totalQuantity;       // sum of traded quantity
    private Double totalVolume;         // sum of (quantity * price)
}

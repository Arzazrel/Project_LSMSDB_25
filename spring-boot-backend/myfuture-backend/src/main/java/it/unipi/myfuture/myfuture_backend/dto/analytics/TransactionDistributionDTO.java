package it.unipi.myfuture.myfuture_backend.dto.analytics;

import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for the number of transaction by payment type or category aggregation (in transaction collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDistributionDTO {
    String category;            // 'share','etf','crypto' or 'creditcard', 'paypal', 'storecredit'
    Long count;                 // number of transactions
    Double totalAmount;         // total volume for type or category transactions (total price sum)
    TimeWindow timeWindow;      // indicate selected time window
}
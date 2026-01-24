package it.unipi.myfuture.myfuture_backend.dto.analytics;

import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for total amount of money invested in assets aggregation (in transaction collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TotalInvestmentDTO {
    private Double totalInvested;       // sum of the total price for the buy transaction
    private Long numberOfTransactions;  // number of buy transactions
    private TimeWindow window;          // time window
}

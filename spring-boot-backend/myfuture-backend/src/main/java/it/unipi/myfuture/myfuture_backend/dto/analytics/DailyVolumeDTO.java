package it.unipi.myfuture.myfuture_backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used to collect the daily volume of all asset (used to add volume filed when elaborate intraday price on Redis).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyVolumeDTO {
    private String symbol;
    private Double volume;
}

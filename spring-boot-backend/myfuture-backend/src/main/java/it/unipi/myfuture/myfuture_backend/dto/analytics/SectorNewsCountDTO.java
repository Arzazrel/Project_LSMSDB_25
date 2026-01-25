package it.unipi.myfuture.myfuture_backend.dto.analytics;

import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for sector statistics aggregation (in news collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectorNewsCountDTO {
    private String sector;          // sector of the news
    private Long newsCount;         // number of news for the same
    private TimeWindow window;      // temporal window
}

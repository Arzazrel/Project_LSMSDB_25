package it.unipi.myfuture.myfuture_backend.dto.analytics;

import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for top mentioned companies in news aggregation (in news collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopMentionedAssetDTO {
    private String companyName;     // name of the mentioned company
    private Long mentionCount;      // counter for the mentioned
    private TimeWindow window;      // time window
}

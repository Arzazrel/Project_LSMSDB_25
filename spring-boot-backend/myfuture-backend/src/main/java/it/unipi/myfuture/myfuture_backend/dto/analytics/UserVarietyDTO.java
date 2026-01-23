package it.unipi.myfuture.myfuture_backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for the variety wallet aggregation (in users collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVarietyDTO {
    private String userId;
    private String username;
    private Integer totalDistinctAssets;
}

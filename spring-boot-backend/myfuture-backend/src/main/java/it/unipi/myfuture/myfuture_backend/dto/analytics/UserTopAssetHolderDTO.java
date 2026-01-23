package it.unipi.myfuture.myfuture_backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for the top asset holders aggregation (in users collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTopAssetHolderDTO {
    private String userId;
    private String username;
    private Integer quantity;
}

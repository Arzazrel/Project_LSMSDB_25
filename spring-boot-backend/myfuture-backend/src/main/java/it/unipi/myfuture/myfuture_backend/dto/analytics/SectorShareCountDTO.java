package it.unipi.myfuture.myfuture_backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for the asset count, for a specific type, aggregation (in asset collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectorShareCountDTO {
    private String sector;          // sector name (es. "Technology")
    private Long count;             // number of share belonging to the sector
}

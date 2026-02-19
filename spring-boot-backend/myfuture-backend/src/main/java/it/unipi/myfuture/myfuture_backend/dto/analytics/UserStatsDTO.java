package it.unipi.myfuture.myfuture_backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used for users statistics in the analytics (in users collection).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDTO {
    long totalUsers;
    long activeUsers;
    long suspendedUsers;
    long deletedUsers;
}

package it.unipi.myfuture.myfuture_backend.dto.user;

import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Class to contain the reason of the suspension passed for the API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSuspendDTO {

    private SuspendReason reason;
    private Instant suspendedAt;
}
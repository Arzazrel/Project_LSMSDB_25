package it.unipi.myfuture.myfuture_backend.dto.user;

import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import lombok.Data;

import java.time.Instant;

/**
 * Class to contain the reason of the suspension passed for the API.
 */
@Data
public class UserSuspendDTO {

    private SuspendReason reason;
    private Instant suspendedAt;
}
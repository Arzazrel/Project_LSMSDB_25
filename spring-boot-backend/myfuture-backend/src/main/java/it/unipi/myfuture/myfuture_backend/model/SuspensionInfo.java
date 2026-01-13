package it.unipi.myfuture.myfuture_backend.model;

import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import lombok.Data;
import java.time.Instant;

@Data
public class SuspensionInfo {

    private Instant suspendedAt;
    private SuspendReason suspendReason;
}

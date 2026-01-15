package it.unipi.myfuture.myfuture_backend.dto;

import it.unipi.myfuture.myfuture_backend.enums.UserRole;
import lombok.Data;

import java.time.Instant;

/**
 * DTO used to expose user information via REST API. Sensitive fields are excluded.
 */
@Data
public class UserResponseDTO {

    private Long userId;

    private String firstName;
    private String lastName;
    private String email;

    private UserRole role;

    private Instant registrationDate;
    private boolean suspended;
}
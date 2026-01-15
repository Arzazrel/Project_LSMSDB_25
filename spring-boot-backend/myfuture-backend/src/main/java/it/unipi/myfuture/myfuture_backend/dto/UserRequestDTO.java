package it.unipi.myfuture.myfuture_backend.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * DTO used to receive user data from client during registration or profile update.
 * Does NOT contain sensitive or system-managed fields.
 */
@Data
public class UserRequestDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String password;

    private LocalDate birthDate;
    private String phone;

    private String address;
    private String city;
    private String province;
    private String cap;
}
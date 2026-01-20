package it.unipi.myfuture.myfuture_backend.dto.user;

import it.unipi.myfuture.myfuture_backend.enums.UserCurrency;
import it.unipi.myfuture.myfuture_backend.enums.UserRole;
import it.unipi.myfuture.myfuture_backend.model.RecentTransaction;
import it.unipi.myfuture.myfuture_backend.model.SuspensionInfo;
import it.unipi.myfuture.myfuture_backend.model.WalletItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO used to receive user data from client during registration or profile update.
 * Does NOT contain sensitive or system-managed fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
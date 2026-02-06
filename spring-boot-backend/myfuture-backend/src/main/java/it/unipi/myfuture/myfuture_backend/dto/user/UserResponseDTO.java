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
 * DTO used to expose user information via REST API. Sensitive fields are excluded.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Long userId;            // application-level ID (from counters)
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;          // user or admin
    private LocalDate birthDate;
    private String phone;

    private String address;
    private String city;
    private String province;
    private String cap;

    private Instant registrationDate;

    private boolean suspended;
    private SuspensionInfo suspensionInfo; // nullable

    private double cash;
    private double blockedCash;
    private UserCurrency currency;

    private List<WalletItem> shareWallet;
    private List<WalletItem> etfWallet;
    private List<WalletItem> cryptoWallet;

    private List<RecentTransaction> recentTransactions;

    // soft delete
    private Boolean deleted;
    private Instant deletedAt;
    // manage field
    private Instant updateAt;
}
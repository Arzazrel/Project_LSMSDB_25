package it.unipi.myfuture.myfuture_backend.model;

import it.unipi.myfuture.myfuture_backend.enums.UserCurrency;
import it.unipi.myfuture.myfuture_backend.enums.UserRole;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Represents a registered user or administrator of the platform.
 *
 * This entity stores personal information, account status, wallet data,
 * and recent transactions for a user.
 *
 * Collection: users
 * Used by: authentication, account management, trading operations
 */
@Data
@Document(collection = "users")
public class User {

    @Id
    private String id; // MongoDB _id

    private Long userId; // application-level ID (from counters)

    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;

    private UserRole role;

    private LocalDate birthDate;
    private String phone;

    private String address;
    private String city;
    private String province;
    private String cap;

    private Instant registrationDate;

    private boolean suspended;
    private SuspensionInfo suspensionInfo; // nullable

    // soft delete
    private Boolean deleted;
    private Date deletedAt;

    private double cash;
    private double blockedCash;
    private UserCurrency currency;

    private List<WalletItem> shareWallet;
    private List<WalletItem> etfWallet;
    private List<WalletItem> cryptoWallet;

    private List<RecentTransaction> recentTransactions;
}
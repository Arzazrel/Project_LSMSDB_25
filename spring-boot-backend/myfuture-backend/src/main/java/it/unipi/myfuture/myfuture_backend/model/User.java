package it.unipi.myfuture.myfuture_backend.model;

import it.unipi.myfuture.myfuture_backend.enums.UserCurrency;
import it.unipi.myfuture.myfuture_backend.enums.UserRole;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Represents a registered user or administrator of the platform. This entity stores personal information,
 * account status, wallet data, and recent transactions for a user.
 *
 * Collection: users
 * Used by: authentication, account management, trading operations
 */
@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;                  // MongoDB _id
    @Field("user_id")
    private Long userId;                // application-level ID (from counters)
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private UserRole role;              // user or admin
    private LocalDate birthDate;
    private String phone;

    private String address;
    private String city;
    private String province;
    private String cap;

    private Instant registrationDate;

    private Boolean suspended;
    private SuspensionInfo suspensionInfo;

    // soft delete
    private Boolean deleted;
    private Instant deletedAt;

    private double cash;
    private double blockedCash;
    private UserCurrency currency;

    private List<WalletItem> shareWallet;
    private List<WalletItem> etfWallet;
    private List<WalletItem> cryptoWallet;

    private List<RecentTransaction> recentTransactions;

    //----------------------------------------------- start: methods ---------------------------------------------------
    /**
     * Adds a transaction to the recent transactions list.
     * Maintains only the last 'maxLastTransaction'(10) transactions, acting as a fixed-size LIFO buffer.
     *
     * @param transaction the transaction to add (light class of the transaction class, only most relevant information).
     */
    public void addLatestTransaction(RecentTransaction transaction) {
        int maxLastTransaction = 10;                // the maximum amount of last transaction to save in the user

        if (this.recentTransactions == null)        // empty check
            this.recentTransactions = new java.util.ArrayList<>();              // create the list


        // Aggiunge in prima posizione (indice 0) per avere la più recente in alto
        this.recentTransactions.add(0, transaction);

        // Se superiamo le 10 transazioni, rimuoviamo la più vecchia (l'ultima della lista)
        if (this.recentTransactions.size() > maxLastTransaction) {
            this.recentTransactions.remove(this.recentTransactions.size() - 1);
        }
    }
    //------------------------------------------------ end: methods ----------------------------------------------------
}
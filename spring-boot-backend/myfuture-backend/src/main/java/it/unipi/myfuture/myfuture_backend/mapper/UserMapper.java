package it.unipi.myfuture.myfuture_backend.mapper;

import it.unipi.myfuture.myfuture_backend.dto.user.*;
import it.unipi.myfuture.myfuture_backend.enums.UserCurrency;
import it.unipi.myfuture.myfuture_backend.model.RecentTransaction;
import it.unipi.myfuture.myfuture_backend.model.User;
import it.unipi.myfuture.myfuture_backend.model.WalletItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * UserMapper Mapper handles conversion between User entity and User DTOs.
 * Used inside service layer to keep business logic clean.
 */
public class UserMapper {

    // -------------------------------------- request → entity --------------------------------------

    /**
     * Convert UserRequestDTO to User entity. Used for user registration.
     *
     * @param dto user request DTO
     * @return user entity
     */
    public static User toEntity(UserRequestDTO dto) {
        User user = new User();

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(dto.getPassword());
        user.setCash(0.0);
        user.setBlockedCash(0.0);
        user.setRegistrationDate(Instant.now());
        user.setDeleted(false);
        user.setSuspended(false);

        return user;
    }

    /**
     * Update mutable fields of an existing user. Used for account update.
     *
     * @param user existing user entity
     * @param userRequest user request DTO
     */
    public static void updateEntity(User user, UserRequestDTO userRequest) {
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setPhone(userRequest.getPhone());
    }

    // -------------------------------------- entity → response --------------------------------------

    /**
     * Convert User entity to UserResponseDTO.
     *
     * @param user user entity
     * @return user response DTO
     */
    public static UserResponseDTO toResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();

        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());

        dto.setCash(user.getCash());
        dto.setBlockedCash(user.getBlockedCash());
        dto.setCurrency(user.getCurrency());

        dto.setShareWallet(user.getShareWallet());
        dto.setEtfWallet(user.getEtfWallet());
        dto.setCryptoWallet(user.getCryptoWallet());

        dto.setRecentTransactions(user.getRecentTransactions());

        return dto;
    }

    /**
     * Convert User entity to UserCashResponseDTO.
     *
     * @param user user entity
     * @return user subfields response DTO
     */
    public static UserCashResponseDTO toCashtDTO(User user) {
        UserCashResponseDTO dto = new UserCashResponseDTO();
        dto.setCash(user.getCash());
        dto.setBlockedCash(user.getBlockedCash());
        dto.setCurrency(user.getCurrency());
        return dto;
    }

    /**
     * Convert User entity to UserPortfolioResponseDTO.
     *
     * @param user user entity
     * @return user subfields response DTO
     */
    public static UserPortfolioResponseDTO toPortfolioDTO(User user) {
        UserPortfolioResponseDTO dto = new UserPortfolioResponseDTO();
        dto.setShareWallet(user.getShareWallet());
        dto.setEtfWallet(user.getEtfWallet());
        dto.setCryptoWallet(user.getCryptoWallet());
        return dto;
    }

    /**
     * Convert User entity to UserTransactionsResponseDTO.
     *
     * @param user user entity
     * @return user subfields response DTO
     */
    public static UserTransactionsResponseDTO toTransactionsDTO(User user) {
        List<RecentTransaction> list = user.getRecentTransactions() != null ?
                user.getRecentTransactions() : new ArrayList<>();

        return new UserTransactionsResponseDTO(
                list,
                list.size()
        );
    }
}
package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.user.*;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.UserMapper;
import it.unipi.myfuture.myfuture_backend.model.User;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for User entity. Defines business operations related to users:
 * registration, authentication, account management and admin controls.
 * (Controllers interact ONLY with this interface layer)
 */
public interface UserService {

    //----------------------------------------- start: method for CRUD API ---------------------------------------------
    /**
     * Register a new user. Initializes wallet, portfolio and default values.
     *
     * @param request registration data
     * @return created user
     */
    UserResponseDTO registerUser(UserRequestDTO  request);

    /**
     * Authenticate user credentials.
     *
     * @param email email of the user
     * @param psw psw of the user
     * @return authenticated user
     */
    UserResponseDTO login(String email, String psw);

    /**
     * Retrieve user by application-level ID.
     *
     * @param userId user ID
     * @return user data
     */
    UserResponseDTO getUserById(Long userId);

    /**
     * Retrieve user ID related to the email.
     *
     * @param email username
     * @return user ID
     */
    Long getUserIdByEmail(String email);

    /**
     * Retrieve user by email.
     *
     * @param email username of the user
     * @return user data
     */
    UserResponseDTO getUserByEmail(String email);

    /**
     * Retrieve the cash information of the user.
     *
     * @param email username of the user
     * @return user fields for cash
     */
    UserCashResponseDTO getUserCash(String email);

    /**
     * Retrieve the portfolio of the user.
     *
     * @param email username of the user
     * @return user portfolio (wallets for the share,etf,crypto)
     */
    UserPortfolioResponseDTO getUserPortfolio(String email);

    /**
     * Retrieve the last transactions (10 almost) of the user.
     *
     * @param email username of the user
     * @return user last transactions
     */
    UserTransactionsResponseDTO getUserLastTransactions(String email);

    /**
     * Retrieve all active users. Admin only.
     *
     * @return list of users
     */
    List<UserResponseDTO> getAllUsers();

    /**
     * Update user account information. Customer only.
     *
     * @param userId user ID
     * @param request update data
     * @return updated user
     */
    UserResponseDTO updateAccountByUserId(Long userId, UserRequestDTO request);

    /**
     * Update user account information. Customer only.
     *
     * @param email username
     * @param request update data
     * @return updated user
     */
    UserResponseDTO updateAccountByEmail(String email, UserRequestDTO request);

    /**
     * Suspend a user.  Admin only.
     *
     * @param userId user ID
     * @param reason suspension reason
     */
    void suspendUser(Long userId, SuspendReason reason, Instant timestamp);

    /**
     * Remove suspension from a user. Admin only.
     *
     * @param userId user ID
     */
    void unSuspendUser(Long userId);

    /**
     * Soft delete a user. Admin only.
     *
     * @param userId user ID
     */
    void softDeleteUser(Long userId);

    //------------------------------------------ end: method for CRUD API ----------------------------------------------

    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * View the 10 users with the largest portfolios in terms of different assets.
     *
     * @return list of the user
     */
    List<UserVarietyDTO> getTopUsersByPortfolioVariety();

    /**
     * View the 10 users with the largest amount of a given asset in their portfolio.
     *
     * @param symbol symbol of the asset to looking for
     * @param type the type of the asset, used for identify the correct wallet to look in
     * @return list of the user
     */
    List<UserTopAssetHolderDTO> getTopHoldersByAsset(String symbol, AssetType type);

    /**
     * View the average number of distinct, average, maximum amount of assets held by users
     *
     * @return list of the user
     */
    GlobalUserStatsDTO getGlobalPortfolioStats();

    //------------------------------------- end: method for aggregation API --------------------------------------------
}

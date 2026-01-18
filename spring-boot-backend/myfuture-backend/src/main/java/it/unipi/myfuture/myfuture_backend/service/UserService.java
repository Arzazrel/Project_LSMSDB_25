package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dto.user.*;
import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;

import java.time.Instant;
import java.util.List;

/**
 * Service interface for User entity. Defines business operations related to users:
 * registration, authentication, account management and admin controls.
 * (Controllers interact ONLY with this interface layer)
 */
public interface UserService {

    // ----------------------------------------------- start: user API --------------------------------------------------

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
    UserResponseDTO updateAccount(Long userId, UserRequestDTO request);

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

    // ----------------------------------------------- end: user API --------------------------------------------------
}

package it.unipi.myfuture.myfuture_backend.service;

import it.unipi.myfuture.myfuture_backend.dao.mongo.UserDao;
import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import it.unipi.myfuture.myfuture_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for User entity.
 * Handles registration, suspension, account management and user-related validations.
 *
 * Used by: UserController, AdminController
 */
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    /**
     * Register a new user.
     *
     * @param user the user to register
     * @return the created user
     */
    public User registerUser(User user) {
        return userDao.save(user);
    }

    /**
     * Retrieve an active user by application-level userId.
     *
     * @param userId application user ID
     * @return Optional containing the user if found and not deleted
     */
    public Optional<User> getUserByUserId(Long userId) {
        return userDao.findByUserId(userId);
    }

    /**
     * Retrieve a user by email (used during login).
     *
     * @param email user email
     * @return Optional containing the user
     */
    public Optional<User> getUserByEmail(String email) {
        return userDao.findByEmail(email);
    }

    /**
     * Retrieve all active users (admin operation).
     *
     * @return list of active users
     */
    public List<User> getAllActiveUsers() {
        return userDao.findAllActive();
    }

    /**
     * Update user account information.
     *
     * @param user updated user object
     * @return updated user
     */
    public User updateUser(User user) {
        return userDao.save(user);
    }

    /**
     * Suspend a user account.
     *
     * @param userId application user ID
     * @param reason suspension reason
     */
    public void suspendUser(Long userId, SuspendReason reason) {
        userDao.suspendUser(userId, reason);
    }

    /**
     * Soft delete a user account.
     *
     * @param userId application user ID
     */
    public void deleteUser(Long userId) {
        userDao.softDelete(userId);
    }
}

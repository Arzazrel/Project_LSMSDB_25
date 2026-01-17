package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.UserDao;
import it.unipi.myfuture.myfuture_backend.dto.user.*;
import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import it.unipi.myfuture.myfuture_backend.mapper.UserMapper;
import it.unipi.myfuture.myfuture_backend.model.User;
import it.unipi.myfuture.myfuture_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService implementation.
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    // ----------------------------------------------- user API --------------------------------------------------

    /**
     * Register a new user. Initializes wallet, portfolio and default values.
     *
     * @param request registration data
     * @return created user
     */
    @Override
    public UserResponseDTO registerUser(UserRequestDTO request) {

        User user = UserMapper.toEntity(request);
        return UserMapper.toResponseDTO(userDao.save(user));
    }

    /**
     * Authenticate user credentials.
     *
     * @param email email of the user
     * @param psw psw of the user
     * @return authenticated user
     */
    @Override
    public UserResponseDTO login(String email, String psw) {

        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        return UserMapper.toResponseDTO(user);
    }

    /**
     * Retrieve user by application-level ID.
     *
     * @param userId user ID
     * @return user data
     */
    @Override
    public UserResponseDTO getUserById(Long userId) {

        User user = userDao.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return UserMapper.toResponseDTO(user);
    }

    /**
     * Retrieve all active users. Admin only.
     *
     * @return list of users
     */
    @Override
    public List<UserResponseDTO> getAllUsers() {

        return userDao.findAllActive()
                .stream()
                .map(UserMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update user account information. Customer only.
     *
     * @param userId user ID
     * @param request update data
     * @return updated user
     */
    @Override
    public UserResponseDTO updateAccount(Long userId, UserRequestDTO request) {

        User user = userDao.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserMapper.updateEntity(user, request);
        return UserMapper.toResponseDTO(userDao.save(user));
    }

    /**
     * Suspend a user.  Admin only.
     *
     * @param userId user ID
     * @param reason suspension reason
     */
    @Override
    public void suspendUser(Long userId, SuspendReason reason) {
        userDao.suspendUser(userId, reason);
    }

    /**
     * Remove suspension from a user. Admin only.
     *
     * @param userId user ID
     */
    @Override
    public void unsuspendUser(Long userId) {
        userDao.undoSuspendUser(userId);
    }

    /**
     * Soft delete a user. Admin only.
     *
     * @param userId user ID
     */
    @Override
    public void softDeleteUser(Long userId) {
        userDao.softDelete(userId);
    }
}
package it.unipi.myfuture.myfuture_backend.service.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.CounterDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.user.UserAggregationDao;
import it.unipi.myfuture.myfuture_backend.dao.mongo.user.UserDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.GlobalUserStatsDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserTopAssetHolderDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserVarietyDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.*;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import it.unipi.myfuture.myfuture_backend.enums.SuspendReason;
import it.unipi.myfuture.myfuture_backend.enums.UserRole;
import it.unipi.myfuture.myfuture_backend.exception.BusinessException;
import it.unipi.myfuture.myfuture_backend.mapper.UserMapper;
import it.unipi.myfuture.myfuture_backend.model.SuspensionInfo;
import it.unipi.myfuture.myfuture_backend.model.User;
import it.unipi.myfuture.myfuture_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService implementation.
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserAggregationDao userAggregationDao;

    @Autowired
    private CounterDao counterDao;

    @Autowired
    private PasswordEncoder passwordEncoder;    // for user authentication

    //----------------------------------------- start: method for CRUD API ---------------------------------------------
    /**
     * Register a new user. Initializes wallet, portfolio and default values.
     *
     * @param request registration data
     * @return created user
     */
    @Override
    public UserResponseDTO registerUser(UserRequestDTO request) {

        // check if the user already exist
        if (userDao.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("Email already registered");
        }

        User user = UserMapper.toEntity(request);                               // create entity from request
        user.setRole(UserRole.user);                                            // set default role
        user.setUserId(counterDao.getNextSequence("user_id"));         // generate the userId

        String encodedPassword = passwordEncoder.encode(request.getPassword()); // encrypt the psw
        user.setPasswordHash(encodedPassword);                                  // set the encrypted psw in the entity

        return UserMapper.toResponseDTO(userDao.save(user));                    // save and return
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
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        // check if the encrypted psw passed as parameter matches with the encrypted psw saved in the DB
        if (!passwordEncoder.matches(psw, user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password");
        }

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
                .orElseThrow(() -> new BusinessException("User not found"));

        return UserMapper.toResponseDTO(user);
    }

    /**
     * Retrieve user by email.
     *
     * @param email username of the user
     * @return user data
     */
    @Override
    public UserResponseDTO getUserByEmail(String email)
    {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return UserMapper.toResponseDTO(user);
    }

    /**
     * Retrieve user ID related to the email.
     *
     * @param email username
     * @return user ID
     */
    @Override
    public Long getUserIdByEmail(String email)
    {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return user.getUserId();
    }

    /**
     * Retrieve the cash information of the user.
     *
     * @param email username of the user
     * @return user fields for cash
     */
    @Override
    public UserCashResponseDTO getUserCash(String email) {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return UserMapper.toCashtDTO(user);     // convert and return only the request fields
    }

    /**
     * Retrieve the portfolio of the user.
     *
     * @param email username of the user
     * @return user portfolio (wallets for the share,etf,crypto)
     */
    @Override
    public UserPortfolioResponseDTO getUserPortfolio(String email) {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return UserMapper.toPortfolioDTO(user); // convert and return only the request fields
    }

    /**
     * Retrieve the last transactions (10 almost) of the user.
     *
     * @param email username of the user
     * @return user last transactions
     */
    @Override
    public UserTransactionsResponseDTO getUserLastTransactions(String email) {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return UserMapper.toTransactionsDTO(user);  // convert and return only the request fields
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
    public UserResponseDTO updateAccountByUserId(Long userId, UserRequestDTO request) {

        User user = userDao.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        UserMapper.updateEntity(user, request);
        return UserMapper.toResponseDTO(userDao.save(user));
    }

    /**
     * Update user account information. Customer only.
     *
     * @param email username
     * @param request update data
     * @return updated user
     */
    @Override
    public UserResponseDTO updateAccountByEmail(String email, UserRequestDTO request)
    {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

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
    public void suspendUser(Long userId, SuspendReason reason, Instant timestamp) throws BusinessException {

        // Validate suspend reason
        if (reason == null) {
            throw new BusinessException("Suspend reason is required");
        }
        // Retrieve user
        User user = userDao.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        // check if the user is suspended
        if (Boolean.TRUE.equals(user.getSuspended())) {
            throw new BusinessException("User is already suspended");
        }

        // Create suspension info
        SuspensionInfo suspensionInfo = new SuspensionInfo();
        suspensionInfo.setSuspendReason(reason);
        suspensionInfo.setSuspendedAt(
                timestamp != null ? timestamp : Instant.now()
        );

        // Update user suspension state
        user.setSuspended(true);
        user.setSuspensionInfo(suspensionInfo);

        userDao.save(user);                         // save changes
    }

    /**
     * Remove suspension from a user. Admin only.
     *
     * @param userId user ID
     */
    @Override
    public void unSuspendUser(Long userId) {
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

    //------------------------------------------ end: method for CRUD API ----------------------------------------------

    //------------------------------------- start: method for aggregation API ------------------------------------------

    /**
     * View the 10 users with the largest portfolios in terms of different assets.
     *
     * @return list of the user
     */
    @Override
    public List<UserVarietyDTO> getTopUsersByPortfolioVariety()
    {
        return userAggregationDao.findTop10ByPortfolioVariety();
    }

    /**
     * View the 10 users with the largest amount of a given asset in their portfolio.
     *
     * @param symbol symbol of the asset to looking for
     * @param type the type of the asset, used for identify the correct wallet to look in
     * @return list of the user
     */
    @Override
    public List<UserTopAssetHolderDTO> getTopHoldersByAsset(String symbol, AssetType type)
    {
        // control check for input validation
        if (symbol == null || symbol.isEmpty())
            throw new BusinessException("Symbol cannot be empty");

        return userAggregationDao.findTop10HoldersByAsset(symbol, type);
    }

    /**
     * View the average number of distinct, average, maximum amount of assets held by users
     *
     * @return list of the user
     */
    @Override
    public GlobalUserStatsDTO getGlobalPortfolioStats(){
        return userAggregationDao.getGlobalUsageStats();
    }

    //------------------------------------- end: method for aggregation API --------------------------------------------
}
package it.unipi.myfuture.myfuture_backend.dao.mongo.user;

import it.unipi.myfuture.myfuture_backend.dto.analytics.GlobalUserStatsDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserStatsDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserTopAssetHolderDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserVarietyDTO;
import it.unipi.myfuture.myfuture_backend.dto.user.UserResponseDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;

import java.time.Instant;
import java.util.List;

/**
 * class that define aggregations that work on the users collection
 */
public interface UserAggregationDao {

    /**
     * View the 10 users with the largest portfolios in terms of different assets.
     *
     * @return list of the user
     */
    List<UserVarietyDTO> findTop10ByPortfolioVariety();

    /**
     * View the 10 users with the largest amount of a given asset in their portfolio.
     *
     * @param symbol symbol of the asset to looking for
     * @param type the type of the asset, used for identify the correct wallet to look in
     * @return list of the user
     */
    List<UserTopAssetHolderDTO> findTop10HoldersByAsset(String symbol, AssetType type);

    /**
     * View the average, minimum and maximum number of distinct assets held by users
     *
     * @return list of the user
     */
    GlobalUserStatsDTO getGlobalUsageStats();

    /**
     * Counts users using a single aggregation pipeline for better performance.
     */
    UserStatsDTO countUsersByStatusAggregation();

    /**
     * Retrieves a detailed list of users registered within a specific time window.
     *
     * @param startDate The starting point in time to filter registrations
     * @return List of UserResponseDTO containing full public profile information
     */
    List<UserResponseDTO> findRecentRegistrations(Instant startDate);
}
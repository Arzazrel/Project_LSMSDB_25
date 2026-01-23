package it.unipi.myfuture.myfuture_backend.dao.mongo.user.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.user.UserAggregationDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserTopAssetHolderDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.UserVarietyDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.GlobalUserStatsDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * class that implements aggregations that work on the users collection
 */
@Repository
public class UserAggregationDaoImpl implements UserAggregationDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * View the 10 users with the largest portfolios in terms of different assets.
     *
     * @return list of the user
     */
    @Override
    public List<UserVarietyDTO> findTop10ByPortfolioVariety() {
        Aggregation aggregation = Aggregation.newAggregation(
                // get user_id, username, size of wallets as 's', 'e' and 'c'
                Aggregation.project("user_id", "email")
                        .and(ArrayOperators.Size.lengthOfArray(ConditionalOperators.ifNull("shareWallet").then(Collections.emptyList()))).as("s")
                        .and(ArrayOperators.Size.lengthOfArray(ConditionalOperators.ifNull("etfWallet").then(Collections.emptyList()))).as("e")
                        .and(ArrayOperators.Size.lengthOfArray(ConditionalOperators.ifNull("cryptoWallet").then(Collections.emptyList()))).as("c"),
                // calculate the total sum of the assets (sum of s,e and c) as totalDistinctAssets
                Aggregation.project("user_id", "email").andExpression("s + e + c").as("totalDistinctAssets"),
                // sort the result in descending order
                Aggregation.sort(Sort.Direction.DESC, "totalDistinctAssets"),
                // take the top 10 users
                Aggregation.limit(10),
                // rename the field with the correct name for DTO -> UserVarietyDTO has userId, username, totalDistinctAssets
                Aggregation.project().and("user_id").as("userId")
                        .and("email").as("username").andInclude("totalDistinctAssets")
        );
        return mongoTemplate.aggregate(aggregation, "users", UserVarietyDTO.class).getMappedResults();
    }

    /**
     * View the 10 users with the largest amount of a given asset in their portfolio.
     *
     * @param symbol symbol of the asset to looking for
     * @param type the type of the asset, used for identify the correct wallet to look in
     * @return list of the user
     */
    @Override
    public List<UserTopAssetHolderDTO> findTop10HoldersByAsset(String symbol, AssetType type) {
        // get the correct name of the wallet to search in based on the AssetType (share, etf, crypto)
        String walletField = type + "Wallet";

        Aggregation aggregation = Aggregation.newAggregation(
                // divide the array of asset
                Aggregation.unwind(walletField),
                // get only the searched asset
                Aggregation.match(Criteria.where(walletField + ".symbol").is(symbol)),
                // sort the quantity of the asset in descending order
                Aggregation.sort(Sort.Direction.DESC, walletField + ".quantity"),
                // take the top 10 users
                Aggregation.limit(10),
                // rename the field with the correct name for DTO -> UserTopAssetHolderDTO has userId, username, quantity
                Aggregation.project().and("user_id").as("userId")
                        .and("email").as("username")
                        .and(walletField + ".quantity").as("quantity")
        );
        return mongoTemplate.aggregate(aggregation, "users", UserTopAssetHolderDTO.class).getMappedResults();
    }

    /**
     * View the average, minimum and maximum number of distinct assets held by users
     *
     * @return list of the user
     */
    @Override
    public GlobalUserStatsDTO getGlobalUsageStats() {
        Aggregation aggregation = Aggregation.newAggregation(
                // get the sizes of the wallets as sCount, eCount, cCount
                Aggregation.project()
                        .and(ArrayOperators.Size.lengthOfArray(ConditionalOperators.ifNull("shareWallet").then(Collections.emptyList()))).as("sCount")
                        .and(ArrayOperators.Size.lengthOfArray(ConditionalOperators.ifNull("etfWallet").then(Collections.emptyList()))).as("eCount")
                        .and(ArrayOperators.Size.lengthOfArray(ConditionalOperators.ifNull("cryptoWallet").then(Collections.emptyList()))).as("cCount"),
                // calculate the total asset for each user
                Aggregation.project().andExpression("sCount + eCount + cCount").as("totalAsset"),
                // group all user for final statistics (default by _id)
                Aggregation.group()
                        .avg("totalAsset").as("avgDistinctAssets")
                        .min("totalAsset").as("minAssetHeld")
                        .max("totalAsset").as("maxAssetsHeld")
        );
        // no rename needed -> GlobalUserStatsDTO has avgDistinctAssets, minAssetHeld, maxAssetsHeld
        return mongoTemplate.aggregate(aggregation, "users", GlobalUserStatsDTO.class).getUniqueMappedResult();
    }
}

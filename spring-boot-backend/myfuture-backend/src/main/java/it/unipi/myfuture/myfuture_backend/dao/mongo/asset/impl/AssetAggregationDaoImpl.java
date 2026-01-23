package it.unipi.myfuture.myfuture_backend.dao.mongo.asset.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.asset.AssetAggregationDao;
import it.unipi.myfuture.myfuture_backend.dto.asset.AssetTypeCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.asset.SectorShareCountDTO;
import it.unipi.myfuture.myfuture_backend.enums.AssetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AssetAggregationDaoImpl implements AssetAggregationDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Number of assets by type (share / ETF / crypto)
     *
     * @return list of AssetTypeCountDTO containing the result for each asset type
     */
    @Override
    public List<AssetTypeCountDTO> countAssetsByType() {
        Aggregation aggregation = Aggregation.newAggregation(
                // group and count all asset by asset type
                Aggregation.group("type").count().as("count"),
                // rename the field with the correct name for DTO -> AssetTypeCountDTO has type, count
                Aggregation.project("count").and("_id").as("type")
        );

        return mongoTemplate.aggregate(aggregation, "assets", AssetTypeCountDTO.class).getMappedResults();
    }

    /**
     * Top 10 sectors by number of listed share
     *
     * @return list of SectorShareCountDTO containing the 10 top sectors and their counts
     */
    @Override
    public List<SectorShareCountDTO> findTop10SectorsByShares() {
        Aggregation aggregation = Aggregation.newAggregation(
                // filter get only shares (ETFs and cryptocurrencies do not have a sector field)
                Aggregation.match(Criteria.where("type").is(AssetType.share.name())),
                // group and count all share by sector
                Aggregation.group("sector").count().as("count"),
                // sort the quantity of the asset in descending order
                Aggregation.sort(Sort.Direction.DESC, "count"),
                // take the top 10 share sector
                Aggregation.limit(10),
                // rename the field with the correct name for DTO -> SectorShareCountDTO has sector, count
                Aggregation.project("count").and("_id").as("sector")
        );

        return mongoTemplate.aggregate(aggregation, "assets", SectorShareCountDTO.class).getMappedResults();
    }
}
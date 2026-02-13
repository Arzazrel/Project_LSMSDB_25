package it.unipi.myfuture.myfuture_backend.dao.mongo.news.impl;

import it.unipi.myfuture.myfuture_backend.dao.mongo.news.NewsAggregationDao;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorNewsCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorNewsGroupDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TopMentionedAssetDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;
import it.unipi.myfuture.myfuture_backend.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Repository
public class NewsAggregationDaoImpl implements NewsAggregationDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Counts how many news articles were published for each sector in a given window.
     * Useful for identifying which market areas are currently most active.
     */
    @Override
    public List<SectorNewsCountDTO> countNewsBySector(TimeWindow window) {
        Instant startDate = DateUtils.calculateStartDate(window);               // calculate start date

        Aggregation aggregation = Aggregation.newAggregation(
                // filter get only asset_prices more recent than start time
                Aggregation.match(Criteria.where("date").gte(startDate)),
                // group by sector and count the news for each sector
                Aggregation.group("sector").count().as("newsCount"),
                // sort by count news
                Aggregation.sort(Sort.Direction.DESC, "newsCount"),
                // rename the field with the correct name for DTO -> SectorNewsCountDTO has sector, newsCount, window
                Aggregation.project("newsCount")
                        .and("_id").as("sector")
                        .andExpression("'" + window.name() + "'").as("window")
        );

        return mongoTemplate.aggregate(aggregation, "news", SectorNewsCountDTO.class).getMappedResults();
    }

    /**
     * Identifies the top 5 assets (symbols) most frequently mentioned in news articles.
     * Helps track which companies are "trending" in the media.
     */
    @Override
    public List<TopMentionedAssetDTO> findTopMentionedAssets(TimeWindow window) {
        Instant startDate = DateUtils.calculateStartDate(window);

        Aggregation aggregation = Aggregation.newAggregation(
                // filter get only asset_prices more recent than start time
                Aggregation.match(Criteria.where("date").gte(startDate).and("company").exists(true).ne(null)),
                // group by company name and count
                Aggregation.group("company").count().as("mentionCount"),
                // sort by mentionCount
                Aggregation.sort(Sort.Direction.DESC, "mentionCount"),
                // take the top 5
                Aggregation.limit(5),
                // rename the field with the correct name for DTO -> TopMentionedAssetDTO has companyName, mentionCount, window
                Aggregation.project("mentionCount")
                        .and("_id").as("companyName")
                        .andExpression("'" + window.name() + "'").as("window")
        );

        return mongoTemplate.aggregate(aggregation, "news", TopMentionedAssetDTO.class).getMappedResults();
    }

    /**
     * Retrieves the latest news for each sector within the last week.
     * Organizes them by sector to facilitate Redis cache population.
     *
     * @param daysLimit limit on the number of days to search for information
     */
    @Override
    public List<SectorNewsGroupDTO> findLatestNewsBySector(int daysLimit) {
        Instant startDate = Instant.now().minus(daysLimit, ChronoUnit.DAYS);

        Aggregation aggregation = Aggregation.newAggregation(
                // filter by date and ensure that the sector exists
                Aggregation.match(Criteria.where("date").gte(startDate).and("sector").exists(true)),
                // sort by descending date (most recent first)
                Aggregation.sort(Sort.Direction.DESC, "date"),
                // group by sector
                Aggregation.group("sector")
                        .push("$$ROOT").as("newsList"),
                // rename the field with the correct name for DTO -> SectorNewsGroupDTO has sector, newsList
                Aggregation.project()
                        .and("_id").as("sector")
                        .and("newsList").slice(10).as("newsList")
        );

        return mongoTemplate.aggregate(aggregation, "news", SectorNewsGroupDTO.class).getMappedResults();
    }
}

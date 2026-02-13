package it.unipi.myfuture.myfuture_backend.dao.mongo.news;

import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorNewsCountDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.SectorNewsGroupDTO;
import it.unipi.myfuture.myfuture_backend.dto.analytics.TopMentionedAssetDTO;
import it.unipi.myfuture.myfuture_backend.enums.TimeWindow;

import java.util.List;

public interface NewsAggregationDao {
    /**
     * Counts how many news articles were published for each sector in a given window.
     * Useful for identifying which market areas are currently most active.
     */
    List<SectorNewsCountDTO> countNewsBySector(TimeWindow window);

    /**
     * Identifies the top 5 assets (symbols) most frequently mentioned in news articles.
     * Helps track which companies are "trending" in the media.
     */
    List<TopMentionedAssetDTO> findTopMentionedAssets(TimeWindow window);

    /**
     * Retrieves the latest news for each sector within the last week.
     * Organizes them by sector to facilitate Redis cache population.
     *
     * @param daysLimit limit on the number of days to search for information
     */
    List<SectorNewsGroupDTO> findLatestNewsBySector(int daysLimit);
}
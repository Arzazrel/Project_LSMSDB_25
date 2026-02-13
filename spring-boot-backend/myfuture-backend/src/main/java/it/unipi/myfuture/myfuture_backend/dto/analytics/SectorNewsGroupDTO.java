package it.unipi.myfuture.myfuture_backend.dto.analytics;

import it.unipi.myfuture.myfuture_backend.model.News;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO used for populate the redis cache for the news (to collect last ten news for all categories)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectorNewsGroupDTO {
    private String sector;          // sector of the news (also unknown)
    private List<News> newsList;    // list of news belonging the category
}

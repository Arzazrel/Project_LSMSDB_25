package it.unipi.myfuture.myfuture_backend.mapper;

import it.unipi.myfuture.myfuture_backend.dto.news.NewsRequestDTO;
import it.unipi.myfuture.myfuture_backend.dto.news.NewsResponseDTO;
import it.unipi.myfuture.myfuture_backend.model.News;

import java.time.Instant;

/**
 * News Mapper handles conversion between News entity and News DTOs.
 * Used inside service layer to keep business logic clean.
 */
public class NewsMapper {

    // -------------------------------------- request → entity --------------------------------------

    /**
     * Convert NewsRequestDTO to News entity.
     *
     * @param newsRequest news request DTO
     * @return news entity
     */
    public static News toEntity(NewsRequestDTO newsRequest) {
        News news = new News();

        news.setDate(newsRequest.getDate());
        news.setTitle(newsRequest.getTitle());
        news.setSummary(newsRequest.getSummary());
        news.setText(newsRequest.getText());
        news.setSector(newsRequest.getSector());
        news.setIndex(newsRequest.getIndex());
        news.setCompany(newsRequest.getCompany());
        news.setDeleted(false);
        news.setIngestedAt(Instant.now());

        return news;
    }

    // -------------------------------------- entity → response --------------------------------------

    /**
     * Convert News entity to NewsResponseDTO.
     *
     * @param news news entity
     * @return news response DTO
     */
    public static NewsResponseDTO toResponseDTO(News news) {
        NewsResponseDTO dto = new NewsResponseDTO();

        dto.setId(news.getId());
        dto.setDate(news.getDate());
        dto.setTitle(news.getTitle());
        dto.setSummary(news.getSummary());
        dto.setText(news.getText());
        dto.setSector(news.getSector());
        dto.setIndex(news.getIndex());
        dto.setCompany(news.getCompany());
        dto.setIngestedAt(news.getIngestedAt());
        dto.setDeleted(news.isDeleted());
        dto.setDeletedAt(news.getDeletedAt());

        return dto;
    }
}
package it.unipi.myfuture.myfuture_backend.dto.news;

import lombok.Data;

import java.time.Instant;

/**
 * DTO used to receive news data from clients.
 *
 * This object represents the input of REST APIs
 * for creating or updating a news entry.
 */
@Data
public class NewsRequestDTO {

    private Instant date;
    private String title;
    private String summary;
    private String text;
    private String sector;
    private String index;
    private String company;
}
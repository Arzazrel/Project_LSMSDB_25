package it.unipi.myfuture.myfuture_backend.dto;

import lombok.Data;

import java.time.Instant;

/**
 * DTO used to expose financial news to users and visitors.
 */
@Data
public class NewsResponseDTO {

    private String id;

    private Instant date;
    private String title;
    private String summary;
    private String text;

    private String sector;
    private String index;
    private String company;
}
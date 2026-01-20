package it.unipi.myfuture.myfuture_backend.dto.news;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO used to expose financial news to users and visitors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsResponseDTO {

    private String id;              // MongoDB _id
    private Instant date;
    private String title;
    private String summary;
    private String text;
    private String sector;
    private String index;
    private String company;

    private Instant ingestedAt;
    // fields for soft delete
    private boolean deleted;
    private Instant deletedAt;
}
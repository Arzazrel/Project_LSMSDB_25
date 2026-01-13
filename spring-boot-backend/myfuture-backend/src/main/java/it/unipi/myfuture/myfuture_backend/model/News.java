package it.unipi.myfuture.myfuture_backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "news")
public class News {

    @Id
    private String id;              // MongoDB _id
    private Instant date;
    private String title;
    private String summary;
    private String text;
    private String sector;
    private String index;
    private String company;
    private Instant ingestedAt;
    // Soft delete
    private boolean deleted;
    private Instant deletedAt;
}

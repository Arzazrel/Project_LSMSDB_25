package it.unipi.myfuture.myfuture_backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a counter used to generate sequential
 * application-level IDs (e.g. userId, transactionId).
 *
 * Collection: counters
 */
@Data
@Document(collection = "counters")
public class Counter {

    @Id
    private String id;   // e.g. "user_id", "transaction_id"

    private long seq;
}
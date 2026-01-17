package it.unipi.myfuture.myfuture_backend.dto;

import java.time.Instant;

/**
 * Generic response wrapper used for all API responses.
 *
 * @param <T> the type of the response payload
 */
public class ResponseWrapper<T> {

    private String message;     // message
    private T data;             // data of the payload
    private Instant timestamp;  // creation timestamp of the mex

    public ResponseWrapper(String message, T data) {
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
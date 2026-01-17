package it.unipi.myfuture.myfuture_backend.exception;

/**
 * Exception representing a business rule violation.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
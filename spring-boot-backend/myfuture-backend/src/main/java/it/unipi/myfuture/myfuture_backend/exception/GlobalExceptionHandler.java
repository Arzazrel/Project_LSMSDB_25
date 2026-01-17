package it.unipi.myfuture.myfuture_backend.exception;

import it.unipi.myfuture.myfuture_backend.dto.ResponseWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the application.
 * use:
 * - @RestControllerAdvice -> a global exception interceptor for REST controllers
 * - @ExceptionHandler -> specifies which method should handle a specific exception.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles business-level exceptions. These exceptions are thrown when a business rule is violated
     * (e.g. invalid input, user not found, insufficient balance). HTTP Status: 400 - Bad Request
     *
     * @param ex the thrown BusinessException
     * @return standardized error response
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseWrapper<Void>> handleBusinessException(BusinessException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseWrapper<>(ex.getMessage(), null));
    }

    /**
     * Handles all uncaught exceptions. This method is a fallback for unexpected errors such as
     * NullPointerException, database failures, or system errors. HTTP Status: 500 - Internal Server Error
     *
     * @param ex the thrown exception
     * @return standardized error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<Void>> handleGenericException(Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseWrapper<>("Internal server error", null));
    }
}
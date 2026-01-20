package it.unipi.myfuture.myfuture_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    /**
     * Customizes the ObjectMapper to properly handle Java 8 Date/Time API.
     *
     * @return a configured ObjectMapper with JavaTimeModule registered.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register the module to support Instant, LocalDateTime, and other JSR-310 types
        mapper.registerModule(new JavaTimeModule());
        // Optional: Uncomment the line below to ensure dates are written in ISO-8601 format (e.g., "2024-01-20T14:00:00Z") instead of numeric timestamps.
        // mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
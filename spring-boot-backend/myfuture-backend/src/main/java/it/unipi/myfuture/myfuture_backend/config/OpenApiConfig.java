package it.unipi.myfuture.myfuture_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * Configuration class for Swagger/OpenAPI documentation.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Customizes the metadata shown in the Swagger UI.
     *
     * @return OpenAPI object with title, description, and version.
     */
    @Bean
    public OpenAPI myFutureOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyFuture Backend API")
                        .description("REST API documentation for MyFuture platform")
                        .version("1.0.0"));
    }
}
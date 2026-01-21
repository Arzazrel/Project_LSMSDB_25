package it.unipi.myfuture.myfuture_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain to define authorization rules and authentication mechanisms.
     * This method determines which HTTP requests are permitted, which require authentication,
     * and which roles are necessary to access specific endpoints.
     *
     * @param http the {@link HttpSecurity} object to modify.
     * @return the configured {@link SecurityFilterChain}.
     * @throws Exception if an error occurs during the configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())               // Must be disabled for REST API
                .authorizeHttpRequests(auth -> auth
                        // 1. Public: registration, Login and all API that start with /api/users/
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        .requestMatchers("/api/users/**").permitAll()

                        // 2. Admin: everything that begins with /api/admin/
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 3. Customer: everything that begins with /api/customers/
                        .requestMatchers("/api/customers/**").hasRole("USER")

                        // 4. DOCUMENTATION: Swagger must be accessible for testing
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // last security level
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults()); // use Basic Auth for tests (username/password in the header)

        return http.build();
    }

    /**
     * Configures the password hashing algorithm for the application (uses the BCrypt strong hashing function).
     * Automatically handles "salting" to protect against rainbow table attacks.
     * This bean is used by Spring Security to:
     * - Encode the password during user registration.
     * - Match the raw password provided during login with the hashed version stored in the DB.
     *
     * @return an instance of BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
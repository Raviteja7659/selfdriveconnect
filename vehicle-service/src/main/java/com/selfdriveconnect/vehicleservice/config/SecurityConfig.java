package com.selfdriveconnect.vehicleservice.config;

import com.selfdriveconnect.vehicleservice.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Browsing vehicles is public — anyone can search/view listings
                        // without logging in, same as any real rental marketplace.
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles/**").permitAll()
                        // Listing, editing, and removing a car requires a logged-in owner.
                        .requestMatchers(HttpMethod.POST, "/api/v1/vehicles/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/vehicles/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/vehicles/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/vehicles/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Allows the React frontend (running on a different origin/port,
     * e.g. http://localhost:5173) to call this API from the browser.
     * Without this, every request from the frontend would be blocked
     * by the browser's CORS policy before it even reaches the
     * controller — this is a browser-enforced rule, not something
     * Postman/curl/PowerShell ever run into, which is why testing
     * worked fine before without it.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

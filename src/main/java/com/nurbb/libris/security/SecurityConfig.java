package com.nurbb.libris.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth

                        // Auth & Swagger
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Books
                        .requestMatchers(HttpMethod.GET, "/api/books/**").hasAnyRole("LIBRARIAN", "PATRON", "GUEST")
                        .requestMatchers("/api/books/**").hasRole("LIBRARIAN")

                        // Authors
                        .requestMatchers(HttpMethod.GET, "/api/authors/**").hasAnyRole("LIBRARIAN", "PATRON", "GUEST")
                        .requestMatchers("/api/authors/**").hasRole("LIBRARIAN")

                        // Borrows
                        .requestMatchers("/api/borrows/my-borrows/**").hasRole("PATRON")
                        .requestMatchers("/api/borrows/**").hasRole("LIBRARIAN")

                        // Statistics
                        .requestMatchers("/api/statistics/**").hasRole("LIBRARIAN")

                        // Users
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}", "/api/users/{id}/stats").hasAnyRole("LIBRARIAN", "PATRON")
                        .requestMatchers("/api/users/**").hasRole("LIBRARIAN")

                        // Reactive endpoints
                        .requestMatchers(HttpMethod.GET, "/api/reactive/books/search").hasAnyRole("LIBRARIAN", "PATRON", "GUEST")
                        .requestMatchers("/api/reactive/books/availability").permitAll()

                        // Diğer her şey: kimlik doğrulama gerekir
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

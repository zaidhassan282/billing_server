package com.billing.system.config;

import com.billing.system.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration — Phase 2 transitional state.
 *
 * Phase 2 introduces JWT auth *alongside* the existing public endpoints
 * so the running app keeps working while the frontend hasn't grown its
 * login page yet. Once Login/Signup ship in the frontend (P2-6), the
 * permitAll() blanket below flips to authenticated() and the only public
 * endpoints will be /auth/login + /auth/signup.
 *
 * Current rules:
 *   /auth/login, /auth/signup  → permitAll
 *   /auth/me                   → authenticated (validates JWT pipeline)
 *   everything else            → permitAll (legacy compat, transitional)
 *
 * The JwtAuthFilter runs on every request and populates the
 * SecurityContext with an {@link com.billing.system.security.AuthPrincipal}
 * when a valid Bearer token is present.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;

    public SecurityConfig(JwtAuthFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                // Stateless: every request must carry its own JWT; we don't
                // create or join HTTP sessions.
                .sessionManagement((SessionManagementConfigurer<HttpSecurity> sm) ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints.
                        .requestMatchers("/auth/login", "/auth/signup").permitAll()
                        // /auth/me is the JWT-pipeline diagnostic.
                        .requestMatchers("/auth/me").authenticated()
                        // Transitional: every other endpoint still public.
                        // Phase 2's P2-6 flips this to authenticated() once
                        // the frontend has a login page.
                        .anyRequest().permitAll())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength 10 — Spring's default, ~100 ms per hash on a
        // modern CPU. Bump to 12 for production, but mind /auth/login
        // latency.
        return new BCryptPasswordEncoder();
    }
}

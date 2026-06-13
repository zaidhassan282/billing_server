package com.billing.system.config;

import com.billing.system.security.JwtAuthFilter;
import com.billing.system.security.TenantContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
 * Spring Security configuration — Phase 2 authenticated state.
 *
 * After P2-6, the SPA owns a login/signup flow and attaches the JWT to
 * every request. The legacy permitAll() blanket is gone. Only the three
 * public auth endpoints stay open.
 *
 * Filter chain order:
 *   JwtAuthFilter           — parses Bearer token, sets SecurityContext
 *   TenantContextFilter     — copies AuthPrincipal.tenantId into
 *                             TenantContext for Hibernate's @TenantId
 *   UsernamePasswordAuthenticationFilter (default position)
 *   ...
 *
 * Rules:
 *   /auth/login, /auth/signup, /auth/verify  → permitAll
 *   everything else                          → authenticated
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final TenantContextFilter tenantFilter;

    public SecurityConfig(JwtAuthFilter jwtFilter, TenantContextFilter tenantFilter) {
        this.jwtFilter = jwtFilter;
        this.tenantFilter = tenantFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                // Stateless: every request carries its own JWT; no HTTP sessions.
                .sessionManagement((SessionManagementConfigurer<HttpSecurity> sm) ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Open endpoints. /auth/logout is also open since it's
                        // a stateless no-op stub — drop the JWT client-side.
                        .requestMatchers("/auth/login", "/auth/signup",
                                         "/auth/verify", "/auth/logout").permitAll()
                        // Spring's CORS preflight requests fly without an
                        // Authorization header.
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // TenantContextFilter must run AFTER JwtAuthFilter so the
                // SecurityContext is already populated when it copies the
                // tenant id over to TenantContext.
                .addFilterAfter(tenantFilter, JwtAuthFilter.class);

        return http.build();
    }

    /**
     * Prevent Spring Boot from auto-registering TenantContextFilter as a
     * global servlet filter — it must run INSIDE the security chain
     * (after JwtAuthFilter), not before the chain entirely. Without
     * this, the filter would fire too early, before authentication runs.
     */
    @Bean
    public FilterRegistrationBean<TenantContextFilter> disableTenantFilterAutoReg(
            TenantContextFilter f) {
        FilterRegistrationBean<TenantContextFilter> reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }

    /**
     * Same containment trick for the JwtAuthFilter — it's already added
     * via .addFilterBefore() above, so block the auto-registration.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> disableJwtFilterAutoReg(JwtAuthFilter f) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt strength 10 — Spring's default. Bump to 12 for prod,
        // but mind /auth/login latency.
        return new BCryptPasswordEncoder();
    }
}

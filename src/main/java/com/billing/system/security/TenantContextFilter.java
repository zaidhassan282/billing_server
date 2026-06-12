package com.billing.system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Per-request servlet filter that pushes the active tenant id into
 * {@link TenantContext}. Runs after {@link JwtAuthFilter} so the
 * SecurityContext is already populated.
 *
 * Unauthenticated requests (the existing endpoints stay open during the
 * Phase 2 transition) get {@link TenantContext#DEFAULT_TENANT} so legacy
 * behaviour is preserved.
 *
 * Always clears the ThreadLocal in a finally block — otherwise a tomcat
 * thread reused by a different request could leak the previous tenant.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        Long tenantId = TenantContext.DEFAULT_TENANT;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal p && p.tenantId() != null) {
            tenantId = p.tenantId();
        }

        try {
            TenantContext.set(tenantId);
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
        }
    }
}

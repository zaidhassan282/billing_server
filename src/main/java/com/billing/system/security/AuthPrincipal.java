package com.billing.system.security;

/**
 * Lightweight authenticated principal stored in the SecurityContext after
 * a JWT validates. Phase 2's TenantInterceptor pulls tenantId off this to
 * scope every query. Phase 3 reads {@code admin} to short-circuit the
 * per-module permission check.
 */
public record AuthPrincipal(Long userId, String email, Long tenantId, boolean admin) {
}

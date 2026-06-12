package com.billing.system.security;

/**
 * Per-request ThreadLocal holder for the active tenant id. Populated by
 * {@link TenantContextFilter} from the SecurityContext's
 * {@link AuthPrincipal}; cleared at the end of every request.
 *
 * Hibernate's {@link CurrentTenantResolver} reads from this every time it
 * needs to scope a query or assign a value to a {@code @TenantId} column.
 *
 * Default during the Phase 2 transition (no auth yet): tenant id 1
 * (Fine Fusion / single-tenant). Anything that calls {@link #required()}
 * outside a request context blows up loudly so the bug is visible.
 */
public final class TenantContext {

    /** Same default the seeded Fine Fusion tenant has. */
    public static final Long DEFAULT_TENANT = 1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() { }

    public static void set(Long tenantId) {
        CURRENT.set(tenantId);
    }

    /** May return null when outside a request (e.g. boot-time seeders). */
    public static Long get() {
        return CURRENT.get();
    }

    /**
     * The tenant id to use for the current operation, with the safe
     * single-tenant default. Use this for writes — never null.
     */
    public static Long getOrDefault() {
        Long t = CURRENT.get();
        return t == null ? DEFAULT_TENANT : t;
    }

    /** Throws when there's no tenant context. Use sparingly. */
    public static Long required() {
        Long t = CURRENT.get();
        if (t == null) {
            throw new IllegalStateException(
                    "No tenant context — caller must run inside an authenticated request");
        }
        return t;
    }

    public static void clear() {
        CURRENT.remove();
    }
}

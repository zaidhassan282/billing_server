package com.billing.system.security;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Tells Hibernate which tenant the current Session belongs to. Backs
 * Hibernate's {@code @TenantId} annotation: every read is filtered to
 * this id, every write that doesn't already supply tenantId gets it
 * assigned automatically.
 *
 * Reads from {@link TenantContext} — populated per request by
 * {@link TenantContextFilter}.
 *
 * Boot-time persistence (the seeders in SchemaCleanupRunner run outside
 * any request) MUST be allowed to talk to the DB; this resolver returns
 * the default tenant in that case so {@code validateExistingCurrentSessions=false}
 * can stay off.
 */
@Component
public class CurrentTenantResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.getOrDefault();
    }

    /**
     * Letting Hibernate validate the session's tenant id against ours
     * would force us to close the session whenever a request changes
     * tenants. We do one Session per request via OpenEntityManagerInView
     * already; skipping validation is the safe choice.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}

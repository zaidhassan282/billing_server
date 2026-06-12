package com.billing.system.config;

import com.billing.system.security.CurrentTenantResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Plugs our Spring-managed {@link CurrentTenantResolver} into Hibernate.
 *
 * Hibernate normally instantiates the resolver from a class name, which
 * means it can't use Spring beans. This customizer injects the live
 * Spring instance instead, so the resolver can pull from
 * {@code TenantContext} (which is request-scoped).
 */
@Component
public class TenantHibernateCustomizer implements HibernatePropertiesCustomizer {

    private final CurrentTenantResolver resolver;

    public TenantHibernateCustomizer(CurrentTenantResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void customize(Map<String, Object> properties) {
        properties.put("hibernate.tenant_identifier_resolver", resolver);
    }
}

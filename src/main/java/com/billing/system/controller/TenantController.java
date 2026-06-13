package com.billing.system.controller;

import com.billing.system.entity.Tenant;
import com.billing.system.repository.TenantRepository;
import com.billing.system.security.TenantContext;
import com.billing.system.service.AuditService;
import org.springframework.web.bind.annotation.*;

/**
 * Per-tenant branding + business-defaults endpoint.
 *
 * After Phase 2, every authenticated request carries the user's tenant
 * id in the JWT, and TenantContextFilter pushes it into
 * {@link TenantContext}. {@code /tenant/current} resolves which tenant
 * to read against from there.
 *
 * Pre-authenticated requests (legacy / transitional / boot-time admin)
 * fall back to the default tenant (1 — seeded Fine Fusion) so existing
 * deployments keep working.
 */
@RestController
@CrossOrigin
@RequestMapping("/tenant")
public class TenantController {

    private static final String ENTITY = "Tenant";

    private final TenantRepository tenantRepo;
    private final AuditService audit;

    public TenantController(TenantRepository tenantRepo, AuditService audit) {
        this.tenantRepo = tenantRepo;
        this.audit = audit;
    }

    @GetMapping("/current")
    public Tenant getCurrent() {
        Long id = TenantContext.getOrDefault();
        return tenantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Tenant " + id + " not found — restart the backend so the "
                        + "boot-time seeder creates the default row."));
    }

    @PutMapping("/current")
    public Tenant updateCurrent(@RequestBody Tenant patch) {
        Long id = TenantContext.getOrDefault();
        Tenant existing = tenantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant " + id + " not found"));
        Object before = audit.snapshot(existing);

        // Records-only: every settable field gets overwritten. Null on a
        // field means "clear it"; the frontend's Settings form always
        // sends the full object so that's safe.
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getLogoUrl() != null) existing.setLogoUrl(patch.getLogoUrl());
        if (patch.getAddress() != null) existing.setAddress(patch.getAddress());
        if (patch.getPhone() != null) existing.setPhone(patch.getPhone());
        if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
        if (patch.getGstRate() != null) existing.setGstRate(patch.getGstRate());
        if (patch.getCurrency() != null) existing.setCurrency(patch.getCurrency());
        if (patch.getPaymentTermsDefault() != null) existing.setPaymentTermsDefault(patch.getPaymentTermsDefault());
        if (patch.getTermsAndConditions() != null) existing.setTermsAndConditions(patch.getTermsAndConditions());
        if (patch.getAuthorisedSignatoryName() != null) existing.setAuthorisedSignatoryName(patch.getAuthorisedSignatoryName());

        Tenant saved = tenantRepo.save(existing);
        audit.logUpdate(ENTITY, String.valueOf(saved.getId()), saved.getName(),
                before, saved, "Tenant settings updated");
        return saved;
    }
}

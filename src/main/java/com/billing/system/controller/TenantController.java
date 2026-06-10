package com.billing.system.controller;

import com.billing.system.entity.Tenant;
import com.billing.system.repository.TenantRepository;
import com.billing.system.service.AuditService;
import org.springframework.web.bind.annotation.*;

/**
 * Per-tenant branding + business-defaults endpoint. Returns the single
 * tenant row that the seeder created on first boot. PUT is records-only
 * (no side effects on Invoice / Contract / etc. — they read live).
 *
 * Phase 2 of the SaaS roadmap turns this into per-account scoping; for
 * now /tenant/current always means tenant id = 1.
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
        return tenantRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException(
                        "Tenant not seeded — restart the backend so the boot-time "
                        + "seeder creates the default row."));
    }

    @PutMapping("/current")
    public Tenant updateCurrent(@RequestBody Tenant patch) {
        Tenant existing = tenantRepo.findById(1L)
                .orElseThrow(() -> new RuntimeException("Tenant not seeded"));
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

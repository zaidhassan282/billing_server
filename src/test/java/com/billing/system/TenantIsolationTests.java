package com.billing.system;

import com.billing.system.entity.PermanentTable;
import com.billing.system.entity.User;
import com.billing.system.repository.PermanentTableRepository;
import com.billing.system.repository.UserRepository;
import com.billing.system.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-8 — proves the multi-tenant boundary actually holds.
 *
 * Each test sets {@link TenantContext} explicitly, performs CRUD via the
 * normal repositories, then switches tenant and asserts there's no
 * leakage. Hibernate's {@code @TenantId} should silently filter every
 * read; writes should land in the right tenant without explicit
 * tenantId on the entity.
 *
 * Runs against H2 (PostgreSQL mode) — the seeded admin in tenant 1 from
 * SchemaCleanupRunner is already present at test start.
 */
@SpringBootTest
class TenantIsolationTests {

    @Autowired PermanentTableRepository partyRepo;
    @Autowired UserRepository userRepo;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void reads_are_scoped_per_tenant() {
        // Two rows, one per tenant.
        PermanentTable a = newParty("Alpha Co", "TENANT-A");
        PermanentTable b = newParty("Beta Co",  "TENANT-B");

        TenantContext.set(11L);
        partyRepo.save(a);    // tenantId set automatically via @TenantId

        TenantContext.set(22L);
        partyRepo.save(b);

        // Read as tenant 11 — sees only Alpha.
        TenantContext.set(11L);
        List<PermanentTable> seenByA = partyRepo.findAll();
        assertTrue(seenByA.stream().anyMatch(p -> "Alpha Co".equals(p.getNameOfParty())),
                "Tenant 11 should see its own Alpha row");
        assertFalse(seenByA.stream().anyMatch(p -> "Beta Co".equals(p.getNameOfParty())),
                "Tenant 11 must NOT see tenant 22's Beta row");

        // Read as tenant 22 — sees only Beta.
        TenantContext.set(22L);
        List<PermanentTable> seenByB = partyRepo.findAll();
        assertTrue(seenByB.stream().anyMatch(p -> "Beta Co".equals(p.getNameOfParty())),
                "Tenant 22 should see its own Beta row");
        assertFalse(seenByB.stream().anyMatch(p -> "Alpha Co".equals(p.getNameOfParty())),
                "Tenant 22 must NOT see tenant 11's Alpha row");
    }

    @Test
    void findById_across_tenants_returns_empty() {
        TenantContext.set(31L);
        PermanentTable a = partyRepo.save(newParty("Gamma Co", "TENANT-A-G"));
        Long id = a.getId();

        // Same tenant, same id — found.
        assertTrue(partyRepo.findById(id).isPresent(),
                "Owner tenant should resolve its own row");

        // Different tenant, same id — must be empty.
        TenantContext.set(32L);
        assertTrue(partyRepo.findById(id).isEmpty(),
                "Cross-tenant findById must not leak the row");
    }

    @Test
    void writes_inherit_tenant_from_context() {
        TenantContext.set(41L);
        PermanentTable saved = partyRepo.save(newParty("Delta Co", "TENANT-D"));
        // tenantId defaults to 1L on the entity, but the @TenantId
        // mechanism overwrites it on flush from the resolver. Reload
        // and confirm.
        PermanentTable reloaded = partyRepo.findById(saved.getId()).orElseThrow();
        assertEquals(41L, reloaded.getTenantId(),
                "Tenant id on the saved row should match the active context");
    }

    /**
     * The User table is NOT @TenantId-scoped (login needs to find users
     * across tenants by email). Verify that behaviour still holds.
     */
    @Test
    void user_table_is_not_scoped() {
        // Seeded admin lives in tenant 1.
        TenantContext.set(99L);   // some other tenant
        User admin = userRepo.findByEmail("admin@finefusion.local").orElse(null);
        assertNotNull(admin, "User lookup must be tenant-agnostic for login to work");
        assertEquals(1L, admin.getTenantId());
    }

    private PermanentTable newParty(String name, String code) {
        PermanentTable p = new PermanentTable();
        p.setNameOfParty(name);
        p.setPartyCode(code);
        return p;
    }
}

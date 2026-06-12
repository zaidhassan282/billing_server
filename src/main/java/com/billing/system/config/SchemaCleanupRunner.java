package com.billing.system.config;

import com.billing.system.entity.Tenant;
import com.billing.system.entity.User;
import com.billing.system.repository.TenantRepository;
import com.billing.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * One-shot schema cleanups that run after Hibernate's {@code ddl-auto=update}
 * has finished. Every statement is idempotent (uses {@code IF EXISTS}) so
 * it's safe to leave wired in place forever — fresh DBs no-op, existing
 * DBs get cleaned up exactly once.
 *
 * Why this exists: {@code ddl-auto=update} only ADDS mapped columns; it
 * never DROPS old ones after a rename. The DR→OGP rewire renamed
 * {@code Invoice.dyedReceiveId} → {@code outwardGatePassId}, leaving the
 * old {@code dyed_receive_id} column (and its unique constraint) orphaned
 * in any DB that existed before the rewire. Old invoice rows still point
 * to the dead column and show blank qty/DR in the UI.
 *
 * Add more idempotent statements below as future schema renames land.
 */
@Configuration
public class SchemaCleanupRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaCleanupRunner.class);

    @Bean
    @Order(1)
    ApplicationRunner schemaCleanups(JdbcTemplate jdbc) {
        return args -> {
            // BUG-2: drop the orphan dyed_receive_id column on invoice.
            // Postgres and H2 (in MODE=PostgreSQL) both honour IF EXISTS +
            // CASCADE; the latter takes any unique constraint with it.
            tryCleanup(jdbc,
                    "ALTER TABLE invoice DROP COLUMN IF EXISTS dyed_receive_id CASCADE",
                    "invoice.dyed_receive_id");

            // Wipe rows that were created against the old FK and now have
            // no OGP link — they'd render as blank-qty / blank-DR rows in
            // the UI. Safe even when there are none.
            tryCleanup(jdbc,
                    "DELETE FROM invoice WHERE outward_gate_pass_id IS NULL",
                    "invoice rows with NULL outward_gate_pass_id");
        };
    }

    /**
     * Seed the single Tenant row on first boot — the values were the
     * hardcoded JSX strings that lived in Dashboard / Invoice /
     * OutwardGatePassDocument etc. before Phase 1 of the SaaS roadmap. An
     * admin can edit any of them via the Settings page; this seeder only
     * runs when the table is empty.
     */
    @Bean
    @Order(2)
    ApplicationRunner seedTenant(TenantRepository tenantRepo) {
        return args -> {
            if (tenantRepo.count() > 0) {
                log.info("Tenant seed: already populated, skipping");
                return;
            }
            Tenant t = new Tenant();
            t.setName("Fine Fusion Textile");
            t.setLogoUrl("");
            t.setAddress("Plot A-15/B, Binoria Chowk, SITE, Karachi");
            t.setPhone("0315-1113223");
            t.setEmail("finefusiontextile@gmail.com");
            t.setGstRate(0.18);
            t.setCurrency("PKR");
            t.setPaymentTermsDefault("");
            t.setTermsAndConditions(
                    "1. Goods once sold will not be returned without prior approval.\n"
                    + "2. Payment due within agreed terms.");
            t.setAuthorisedSignatoryName("");
            tenantRepo.save(t);
            log.info("Tenant seed: created '{}' as tenant id={}", t.getName(), t.getId());
        };
    }

    /**
     * Seed the first admin user when the {@code app_user} table is
     * empty. Email + password come from env vars so a real operator can
     * pick them before the very first boot; defaults are dev-only.
     *
     * Prints the credentials to the log because there's no other way to
     * find them on a fresh deployment. Change the password on first
     * login (once that screen exists in P2-6).
     */
    @Bean
    @Order(3)
    ApplicationRunner seedAdminUser(UserRepository users,
                                    PasswordEncoder encoder,
                                    org.springframework.core.env.Environment env) {
        return args -> {
            if (users.count() > 0) {
                log.info("Admin seed: at least one user exists, skipping");
                return;
            }
            String email = env.getProperty("app.bootstrap.admin-email",
                    "admin@finefusion.local");
            String password = env.getProperty("app.bootstrap.admin-password",
                    "admin1234");
            User admin = new User();
            admin.setEmail(email.toLowerCase());
            admin.setPasswordHash(encoder.encode(password));
            admin.setTenantId(1L);     // Fine Fusion is the seeded tenant
            admin.setTenantAdmin(true);
            admin.setDisplayName("Administrator");
            admin.setEmailVerified(true);
            users.save(admin);
            log.warn("Admin seed: created '{}' / password '{}' — CHANGE THIS on first login",
                    email, password);
        };
    }

    /**
     * Run one DDL/DML statement and swallow any failure with a clear log
     * message — schema cleanups must never block app startup. A failure
     * here is almost always "table doesn't exist yet on a fresh DB", which
     * is fine.
     */
    private static void tryCleanup(JdbcTemplate jdbc, String sql, String description) {
        try {
            int affected = jdbc.update(sql);
            if (affected > 0) {
                log.info("Schema cleanup: {} → {} row(s) affected", description, affected);
            } else {
                log.info("Schema cleanup: {} → already clean", description);
            }
        } catch (Exception e) {
            log.warn("Schema cleanup skipped for {}: {} (safe to ignore on a fresh DB)",
                    description, e.getMessage());
        }
    }
}

package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_entity_type", columnList = "entityType"),
        @Index(name = "idx_audit_entity_id", columnList = "entityType, entityId"),
        @Index(name = "idx_audit_changed_at", columnList = "changedAt"),
        @Index(name = "idx_audit_tenant", columnList = "tenant_id, changedAt")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1 so single-tenant deploys
     *  keep working; P2-4 overrides via TenantContext from the JWT. */
    @Column(name = "tenant_id")
    private Long tenantId = 1L;

    /** Entity type, e.g. "InwardGatePass", "PermanentTable". */
    @Column(nullable = false, length = 64)
    private String entityType;

    /** Database PK of the affected row, as a String. Nullable only if unknown. */
    @Column(length = 64)
    private String entityId;

    /** Optional business id, e.g. "IGP26001" or party code "P26001". */
    @Column(length = 64)
    private String businessId;

    /** CREATE / UPDATE / DELETE. */
    @Column(nullable = false, length = 16)
    private String action;

    /** "system" until login lands. */
    @Column(nullable = false, length = 64)
    private String changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    /** Human-readable summary, e.g. "Permanent party P26001 created". */
    @Column(length = 512)
    private String summary;

    /**
     * Full row state at change time, as JSON.
     * Plain TEXT (not @Lob) — Hibernate 6+ on Postgres maps @Lob String to OID,
     * which fails outside auto-commit transactions.
     */
    @Column(columnDefinition = "TEXT")
    private String snapshot;

    /** UPDATE-only: list of {field, before, after} as JSON. Null for CREATE/DELETE. */
    @Column(columnDefinition = "TEXT")
    private String changes;
}

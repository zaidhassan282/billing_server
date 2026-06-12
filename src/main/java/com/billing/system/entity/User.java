package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-tenant user. Phase 2 of the SaaS roadmap.
 *
 * Email is unique GLOBALLY (one address can't belong to two tenants;
 * keeps signup logic simple and matches the public-signup flow we picked).
 * Password is stored only as a BCrypt hash via PasswordEncoder.
 *
 * tenantId links the user to a {@link Tenant}; Phase 3 layers per-module
 * permissions on top via a UserPermission table. Until then,
 * {@code isTenantAdmin = true} is the only ACL we have.
 */
@Getter
@Setter
@Entity
@Table(name = "app_user",
       indexes = {
           @Index(name = "ix_app_user_email", columnList = "email", unique = true),
           @Index(name = "ix_app_user_tenant", columnList = "tenant_id"),
       })
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    /** BCrypt hash. Plain passwords never enter this column. */
    @Column(nullable = false, length = 80)
    private String passwordHash;

    @Column(name = "tenant_id")
    private Long tenantId;

    /** Tenant admin = implicit all-yes on permissions. Phase 3 refines. */
    @Column(nullable = false)
    private boolean tenantAdmin = false;

    @Column(length = 128)
    private String displayName;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

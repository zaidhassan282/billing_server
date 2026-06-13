package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory", indexes = {
        @Index(name = "idx_inv_lookup",
               columnList = "tenant_id, contractNo, quality, color, stage", unique = true)
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1 so single-tenant deploys
     *  keep working; P2-4 overrides via TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    /** Contract this stock belongs to. Stock is scoped per contract. */
    @Column(nullable = false, length = 64)
    private String contractNo;

    /** Origin doc id (e.g. inwardId or dyedReceive id) — informational. */
    private String refId;

    /** GREIGH or DYED. */
    @Column(nullable = false, length = 16)
    private String stage;

    @Column(nullable = false)
    private String quality;

    @Column(nullable = false)
    private String color;

    private Double availableKg;
    private Double availableMeters;
    private Integer availableRolls;
}

package com.billing.system.entity;

import com.billing.system.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "fabric_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1 so single-tenant deploys
     *  keep working; P2-4 overrides via TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId = 1L;

    @Column(unique = true, length = 32)
    private String orderId; // ORD26001

    @Column(nullable = false, length = 64)
    private String contractNo;

    private String partyCode;
    private String nameOfParty;

    private String quality;
    private String color;

    private Double quantityKg;
    private Integer quantityRolls;
    private Double quantityMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    /** Set when the order is fulfilled by an Outward gate pass. */
    private String linkedOutwardId;

    @Column(length = 1024)
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

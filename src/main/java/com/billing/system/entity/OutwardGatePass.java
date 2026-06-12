package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
public class OutwardGatePass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1 so single-tenant deploys
     *  keep working; P2-4 overrides via TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId = 1L;

    @Column(unique = true)
    private String outwardId; // OGP-xxxxxx

    private LocalDate dated;

    // Reference to source inward (optional)
    private String inwardId;

    /**
     * Foreign key to the Dyed Receive this OGP draws from. Required for
     * every NEW gate pass — Outward is now the "deliver the dyed fabric
     * to the customer" step that sits between DR and Invoice. Legacy
     * pre-rewire rows may still have this null.
     */
    private Long dyedReceiveId;

    /** Denormalised DR business id (e.g. "DR-ABC123") for display and joins. */
    @Column(length = 32)
    private String drId;

    private String contractNo;
    private String customerCode;
    private String customerName;

    // Lot tracking
    private String customerLotNo;
    private String factoryLotNo;

    // ISSUE / RETURN
    private String type;

    // Logistics
    private String address;
    private String vehicleNo;
    private String driverName;
    private String referenceNo;

    private String fabricType;

    // Security / gate metadata
    private String gateTime;
    private String securityGuardName;
    private String checkedBy;

    @OneToMany(mappedBy = "outward", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutwardItem> items;
}

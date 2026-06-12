package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
public class InwardGatePass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1 so single-tenant deploys
     *  keep working; P2-4 overrides via TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId = 1L;

    @Column(unique = true)
    private String inwardId; // IGP-xxxxxx

    private LocalDate dated;

    // Contract & party
    private String contractNo;
    private String partyCode;
    private String nameOfParty;
    private String supplierName;

    // Lot tracking
    private String customerLotNo;
    private String factoryLotNo;

    // Logistics
    private String address;
    private String vehicleNo;
    private String driverName;
    private String referenceNo;

    // Fabric / classification
    private String fabricType;
    private Boolean isDyedFabric;
    private Boolean isGreigeFabric;

    // Security / gate metadata
    private String gateTime;
    private String securityGuardName;
    private String checkedBy;

    // --- New fields (per latest spec) ---
    /** Dye house / dyeing party associated with this batch. */
    private String dyeing;
    /** Process challan number printed on the document. */
    private String pChallanNo;
    /** Promised delivery date back to the customer. */
    private LocalDate deliveryDate;

    @OneToMany(mappedBy = "inward", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InwardItem> items;
}

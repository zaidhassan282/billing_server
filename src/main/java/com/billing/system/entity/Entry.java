package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "daily_entry")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1; P2-4 overrides via
     *  TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    private Integer serialNo;

    private LocalDate dated;

    private String partyCode;
    private String nameOfParty;

    private String gstInvoiceYesNo;
    private String commercialBillNo;
    private String gstInvoiceNo;

    private String description;
    private String quality;
    private String colour;

    private Double GreighInRoll;
    private Double GreighInKg;

    private Double dyedFabricARoll;
    private Double dyedFabricOutAKg;
    private Double dyedFabricBRoll;
    private Double dyedFabricOutBKg;
    private Double dyedFabricOutCP;

    private Double quantityRollBilled;
    private Double quantityKgBilled;

    private Double rate;
    private Double amount;
    private Double gstAmount;
    private Double totalAmount;
}

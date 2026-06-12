package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "contract_table")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1 so single-tenant deploys
     *  keep working; P2-4 overrides via TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId = 1L;

    private LocalDate dated;

    private String contractNo;

    private String partyCode;

    private String nameOfParty;

    private String gstInvoice;

    // Frontend sends "Yes"/"No" for the GST flag in ContractTable.js
    private String gstInvoiceYesNo;

    private String hsCode;

    private String quality;

    private String color;

    private Double weight;

    private Double rateA;

    private Double rateB;

    // Either field name is accepted from older clients
    private Double shrinkage;
    private Double shrinkageAllowed;

    private String deliveryTime;

    private String paymentTerm;

    private String remarks;
}

package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Invoice — billing record against an Outward Gate Pass (the delivery
 * step). Strict 1:1 with OGP (the {@code outwardGatePassId} column is
 * unique). The rate and totals are NOT stored here: qty is read from the
 * OGP's delivered kg, rate from {@code Contract.rateA}, every time the
 * invoice is read, so a corrected contract rate flows straight through to
 * the invoice.
 *
 * Flow:  DR  →  OGP  →  Invoice
 *
 * Editable fields (records-only): date, GST flag, payment terms, remarks.
 *
 * Snapshot-at-create fields (immutable after save): contractNo, partyCode,
 * nameOfParty.
 */
@Getter
@Setter
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1 so single-tenant deploys
     *  keep working; P2-4 overrides via TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    /** Business id, format INV26001. */
    @Column(unique = true, length = 32)
    private String invoiceNo;

    private LocalDate dated;

    /** Strict 1:1 with an Outward Gate Pass — unique constraint blocks a second invoice for the same OGP. */
    @Column(unique = true)
    private Long outwardGatePassId;

    /** Snapshot identity fields — set from the linked OGP / Contract at create time. */
    private String contractNo;
    private String partyCode;
    private String nameOfParty;

    /** "Yes" / "No". Defaults from the contract; editable on the invoice. */
    @Column(length = 8)
    private String gstInvoiceYesNo;

    @Column(length = 256)
    private String paymentTerms;

    @Column(length = 1024)
    private String remarks;
}

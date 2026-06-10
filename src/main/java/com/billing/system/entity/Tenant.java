package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-customer branding + business defaults. The system runs as a single
 * tenant today (Sincere Billing System sells per-install), so there is
 * exactly one Tenant row with id = 1. Phase 2 of the SaaS roadmap turns
 * this into the multi-tenant root.
 *
 * Everything that used to be hardcoded in JSX or in service constants
 * (company name, address, phone, GST rate, T&C paragraph, etc.) lives
 * here. Pages and invoices read these fields at request time so an admin
 * change in /settings is reflected on the next paint.
 */
@Getter
@Setter
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Display name on the dashboard h1 and every printable doc header. */
    @Column(length = 128)
    private String name;

    /**
     * Direct URL of a hosted logo image. Phase 4 of the roadmap upgrades
     * this to a multipart file upload + disk/S3 storage; for now an
     * external URL keeps the form simple.
     */
    @Column(length = 512)
    private String logoUrl;

    @Column(length = 512)
    private String address;

    @Column(length = 64)
    private String phone;

    @Column(length = 128)
    private String email;

    /**
     * GST rate as a fraction (0.18 = 18%). Was a hardcoded
     * {@code InvoiceService.GST_RATE = 0.18}; now per-tenant so a customer
     * in a different jurisdiction can run their own number.
     */
    private Double gstRate;

    /** ISO-ish currency code shown on invoices ("PKR", "USD", …). */
    @Column(length = 8)
    private String currency;

    /** Pre-fills the Payment Terms field on a new Contract / Invoice. */
    @Column(length = 256)
    private String paymentTermsDefault;

    /** Free-form paragraph printed at the bottom of every invoice. */
    @Column(length = 2048)
    private String termsAndConditions;

    /** Name printed under "Authorized Signature" on every signed doc. */
    @Column(length = 128)
    private String authorisedSignatoryName;
}

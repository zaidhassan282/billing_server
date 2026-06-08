package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Invoice — billing record against a Dyed Receive. Strict 1:1 with DR (the
 * {@code dyedReceiveId} column is unique). The rate, qty and totals are NOT
 * stored here: they live-derive from the linked DR and the Contract's
 * {@code rateA} every time the invoice is read, so a corrected contract rate
 * flows straight through to the invoice.
 *
 * Editable fields (records-only, like our other entities): date, GST flag,
 * payment terms, remarks.
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

    /** Business id, format INV26001. */
    @Column(unique = true, length = 32)
    private String invoiceNo;

    private LocalDate dated;

    /** Strict 1:1 with a Dyed Receive — unique constraint blocks a second invoice for the same DR. */
    @Column(unique = true)
    private Long dyedReceiveId;

    /** Snapshot identity fields — set from the linked DR / Contract at create time. */
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

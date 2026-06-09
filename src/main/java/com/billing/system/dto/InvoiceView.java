package com.billing.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * View returned by the invoice read endpoints — the stored
 * {@link com.billing.system.entity.Invoice} fields PLUS the live-derived
 * bits: qty from the linked Outward GP's delivered kg, rate from the
 * Contract, amount/GST/total computed each read.
 *
 * The chain is Invoice → OGP → DR → Contract; everything downstream of
 * Invoice is denormalised here for the UI.
 */
@Getter
@Setter
public class InvoiceView {

    // ----- stored on Invoice -----
    private Long id;
    private String invoiceNo;
    private LocalDate dated;
    private Long outwardGatePassId;
    private String contractNo;
    private String partyCode;
    private String nameOfParty;
    private String gstInvoiceYesNo;
    private String paymentTerms;
    private String remarks;

    // ----- derived from the linked OGP -----
    private String outwardId;   // OutwardGatePass.outwardId (e.g. OGP26001)
    private Double qtyKg;       // sum of OGP item kg (what was actually delivered)

    // ----- derived from the OGP's DR -----
    private Long dyedReceiveId;
    private String drId;        // DyedReceive.newId
    private String issueId;
    private String quality;
    private String color;

    // ----- derived from the Contract -----
    private Double rate;        // Contract.rateA

    // ----- computed -----
    private Double amount;
    private Double gstAmount;
    private Double totalAmount;
}

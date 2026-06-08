package com.billing.system.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * View returned by the invoice read endpoints — the stored {@link com.billing.system.entity.Invoice}
 * fields PLUS the live-derived bits (rate from Contract, qty from the linked
 * Dyed Receive, amount/GST/total computed each read).
 */
@Getter
@Setter
public class InvoiceView {

    // ----- stored on Invoice -----
    private Long id;
    private String invoiceNo;
    private LocalDate dated;
    private Long dyedReceiveId;
    private String contractNo;
    private String partyCode;
    private String nameOfParty;
    private String gstInvoiceYesNo;
    private String paymentTerms;
    private String remarks;

    // ----- derived from the linked DR -----
    private String drId;        // DyedReceive.newId
    private String issueId;
    private String quality;
    private String color;
    private Double qtyKg;       // DR net (received - cut - shrinkage%)

    // ----- derived from the Contract -----
    private Double rate;        // Contract.rateA

    // ----- computed -----
    private Double amount;
    private Double gstAmount;
    private Double totalAmount;
}

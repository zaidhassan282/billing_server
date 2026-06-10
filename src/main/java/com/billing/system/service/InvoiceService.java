package com.billing.system.service;

import com.billing.system.dto.InvoiceView;
import com.billing.system.entity.Contract;
import com.billing.system.entity.DyedReceive;
import com.billing.system.entity.Invoice;
import com.billing.system.entity.OutwardGatePass;
import com.billing.system.entity.OutwardItem;
import com.billing.system.entity.Tenant;
import com.billing.system.repository.ContractRepository;
import com.billing.system.repository.DyedReceiveRepository;
import com.billing.system.repository.InvoiceRepository;
import com.billing.system.repository.OutwardGatePassRepository;
import com.billing.system.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Invoice service — Invoice now hangs off an Outward Gate Pass (the
 * delivery step). Identity is snapshot from the linked OGP / DR /
 * Contract; qty + rate are live-derived each read.
 */
@Service
public class InvoiceService {

    /** Fallback if the tenant row has no GST rate set. */
    private static final double GST_RATE_FALLBACK = 0.18;

    private final InvoiceRepository invoiceRepo;
    private final OutwardGatePassRepository outwardRepo;
    private final DyedReceiveRepository dyedRepo;
    private final ContractRepository contractRepo;
    private final TenantRepository tenantRepo;
    private final AuditService audit;

    public InvoiceService(InvoiceRepository invoiceRepo,
                          OutwardGatePassRepository outwardRepo,
                          DyedReceiveRepository dyedRepo,
                          ContractRepository contractRepo,
                          TenantRepository tenantRepo,
                          AuditService audit) {
        this.invoiceRepo = invoiceRepo;
        this.outwardRepo = outwardRepo;
        this.dyedRepo = dyedRepo;
        this.contractRepo = contractRepo;
        this.tenantRepo = tenantRepo;
        this.audit = audit;
    }

    /**
     * Create a new invoice against an Outward Gate Pass. Pre-fills the
     * snapshot identity fields (contract, party, GST default, payment
     * terms default) from the OGP and its Contract. Rejects if the OGP is
     * already invoiced (1:1 invariant).
     */
    @Transactional
    public Invoice save(Invoice invoice) {
        if (invoice.getOutwardGatePassId() == null) {
            throw new RuntimeException("Outward Gate Pass reference is required");
        }
        if (invoiceRepo.findByOutwardGatePassId(invoice.getOutwardGatePassId()).isPresent()) {
            throw new RuntimeException("An invoice already exists for this Outward Gate Pass");
        }
        OutwardGatePass ogp = outwardRepo.findById(invoice.getOutwardGatePassId())
                .orElseThrow(() -> new RuntimeException(
                        "Outward Gate Pass not found: " + invoice.getOutwardGatePassId()));

        invoice.setContractNo(ogp.getContractNo());
        invoice.setPartyCode(ogp.getCustomerCode());
        invoice.setNameOfParty(ogp.getCustomerName());

        Contract contract = (ogp.getContractNo() != null && !ogp.getContractNo().isEmpty())
                ? contractRepo.findByContractNo(ogp.getContractNo())
                : null;
        if (contract != null) {
            if (isBlank(invoice.getPartyCode()))   invoice.setPartyCode(contract.getPartyCode());
            if (isBlank(invoice.getNameOfParty())) invoice.setNameOfParty(contract.getNameOfParty());
            if (isBlank(invoice.getGstInvoiceYesNo())) {
                invoice.setGstInvoiceYesNo(contract.getGstInvoiceYesNo());
            }
            if (isBlank(invoice.getPaymentTerms())) {
                invoice.setPaymentTerms(contract.getPaymentTerm());
            }
        }
        if (isBlank(invoice.getGstInvoiceYesNo())) invoice.setGstInvoiceYesNo("No");
        if (invoice.getDated() == null) invoice.setDated(LocalDate.now());
        invoice.setInvoiceNo(generateInvoiceNo());

        Invoice saved = invoiceRepo.save(invoice);
        audit.logCreate("Invoice", String.valueOf(saved.getId()), saved.getInvoiceNo(),
                saved, "Invoice " + saved.getInvoiceNo()
                        + " for OGP " + ogp.getOutwardId());
        return saved;
    }

    /**
     * Records-only update — only the "anyone-can-make-a-mistake" fields:
     * date, GST flag, payment terms, remarks. Identity fields (OGP link,
     * contract, party, invoiceNo) are NOT changeable; correct via delete +
     * regenerate if they're wrong.
     */
    @Transactional
    public Invoice update(Long id, Invoice patch) {
        Invoice existing = invoiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
        Object before = audit.snapshot(existing);

        if (patch.getDated() != null) existing.setDated(patch.getDated());
        if (patch.getGstInvoiceYesNo() != null && !patch.getGstInvoiceYesNo().isEmpty()) {
            existing.setGstInvoiceYesNo(patch.getGstInvoiceYesNo());
        }
        if (patch.getPaymentTerms() != null) existing.setPaymentTerms(patch.getPaymentTerms());
        if (patch.getRemarks() != null) existing.setRemarks(patch.getRemarks());

        Invoice saved = invoiceRepo.save(existing);
        audit.logUpdate("Invoice", String.valueOf(saved.getId()), saved.getInvoiceNo(),
                before, saved, "Invoice " + saved.getInvoiceNo() + " updated");
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Invoice existing = invoiceRepo.findById(id).orElse(null);
        if (existing == null) return;
        Object before = audit.snapshot(existing);
        invoiceRepo.deleteById(id);
        audit.logDelete("Invoice", String.valueOf(id), existing.getInvoiceNo(), before,
                "Invoice " + existing.getInvoiceNo() + " deleted");
    }

    public List<InvoiceView> getAll() {
        return invoiceRepo.findAll().stream().map(this::toView).collect(Collectors.toList());
    }

    public InvoiceView getById(Long id) {
        Invoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
        return toView(inv);
    }

    /**
     * Build the InvoiceView (stored + derived) for an Invoice. Walks
     * Invoice → OGP → DR → Contract, surfacing display fields and computing
     * amounts from the OGP's delivered kg × Contract.rateA.
     */
    public InvoiceView toView(Invoice inv) {
        InvoiceView v = new InvoiceView();
        v.setId(inv.getId());
        v.setInvoiceNo(inv.getInvoiceNo());
        v.setDated(inv.getDated());
        v.setOutwardGatePassId(inv.getOutwardGatePassId());
        v.setContractNo(inv.getContractNo());
        v.setPartyCode(inv.getPartyCode());
        v.setNameOfParty(inv.getNameOfParty());
        v.setGstInvoiceYesNo(inv.getGstInvoiceYesNo());
        v.setPaymentTerms(inv.getPaymentTerms());
        v.setRemarks(inv.getRemarks());

        OutwardGatePass ogp = inv.getOutwardGatePassId() != null
                ? outwardRepo.findById(inv.getOutwardGatePassId()).orElse(null)
                : null;
        if (ogp != null) {
            v.setOutwardId(ogp.getOutwardId());
            v.setQtyKg(sumItemKg(ogp.getItems()));

            // OGP carries dyedReceiveId after the rewire — chain into DR for
            // quality / color / issue / DR business id.
            if (ogp.getDyedReceiveId() != null) {
                v.setDyedReceiveId(ogp.getDyedReceiveId());
                DyedReceive dr = dyedRepo.findById(ogp.getDyedReceiveId()).orElse(null);
                if (dr != null) {
                    v.setDrId(dr.getNewId());
                    v.setIssueId(dr.getIssueId());
                    v.setQuality(dr.getQuality());
                    v.setColor(dr.getColor());
                }
            }
        }

        Contract c = (inv.getContractNo() != null && !inv.getContractNo().isEmpty())
                ? contractRepo.findByContractNo(inv.getContractNo())
                : null;
        double rate = (c != null && c.getRateA() != null) ? c.getRateA() : 0.0;
        v.setRate(rate);

        double qtyKg = v.getQtyKg() == null ? 0.0 : v.getQtyKg();
        double amount = qtyKg * rate;
        v.setAmount(amount);

        double gst = "Yes".equalsIgnoreCase(inv.getGstInvoiceYesNo()) ? amount * currentGstRate() : 0.0;
        v.setGstAmount(gst);
        v.setTotalAmount(amount + gst);
        return v;
    }

    private String generateInvoiceNo() {
        String yy = String.valueOf(LocalDate.now().getYear()).substring(2);
        String prefix = "INV" + yy;
        int max = invoiceRepo.findAll().stream()
                .map(Invoice::getInvoiceNo)
                .filter(s -> s != null && s.startsWith(prefix))
                .map(s -> {
                    try { return Integer.parseInt(s.substring(prefix.length())); }
                    catch (Exception e) { return 0; }
                })
                .max(Integer::compareTo)
                .orElse(0);
        return prefix + String.format("%03d", max + 1);
    }

    private static double sumItemKg(List<OutwardItem> items) {
        if (items == null) return 0.0;
        double sum = 0.0;
        for (OutwardItem it : items) sum += it.getKg() == null ? 0.0 : it.getKg();
        return sum;
    }

    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }

    /**
     * GST rate as a fraction (0.18 = 18%) read live from the Tenant row,
     * so an admin change in /settings flows straight into every invoice
     * read. Falls back to the historical 18% if the tenant row hasn't
     * been seeded yet (only happens before SchemaCleanupRunner runs).
     */
    private double currentGstRate() {
        Tenant t = tenantRepo.findById(1L).orElse(null);
        if (t == null || t.getGstRate() == null) return GST_RATE_FALLBACK;
        return t.getGstRate();
    }
}

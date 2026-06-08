package com.billing.system.service;

import com.billing.system.dto.InvoiceView;
import com.billing.system.entity.Contract;
import com.billing.system.entity.DyedReceive;
import com.billing.system.entity.Invoice;
import com.billing.system.repository.ContractRepository;
import com.billing.system.repository.DyedReceiveRepository;
import com.billing.system.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private static final double GST_RATE = 0.18;

    private final InvoiceRepository invoiceRepo;
    private final DyedReceiveRepository dyedRepo;
    private final ContractRepository contractRepo;
    private final AuditService audit;

    public InvoiceService(InvoiceRepository invoiceRepo,
                          DyedReceiveRepository dyedRepo,
                          ContractRepository contractRepo,
                          AuditService audit) {
        this.invoiceRepo = invoiceRepo;
        this.dyedRepo = dyedRepo;
        this.contractRepo = contractRepo;
        this.audit = audit;
    }

    /**
     * Create a new invoice against a Dyed Receive. Pre-fills the snapshot
     * identity fields (contractNo, party, GST default, payment terms default)
     * from the linked DR and its Contract. Rejects if the DR is already
     * invoiced (1:1 invariant).
     */
    @Transactional
    public Invoice save(Invoice invoice) {
        if (invoice.getDyedReceiveId() == null) {
            throw new RuntimeException("Dyed Receive reference is required");
        }
        if (invoiceRepo.findByDyedReceiveId(invoice.getDyedReceiveId()).isPresent()) {
            throw new RuntimeException("An invoice already exists for this Dyed Receive");
        }
        DyedReceive dr = dyedRepo.findById(invoice.getDyedReceiveId())
                .orElseThrow(() -> new RuntimeException(
                        "Dyed Receive not found: " + invoice.getDyedReceiveId()));

        invoice.setContractNo(dr.getContractNo());

        Contract contract = (dr.getContractNo() != null && !dr.getContractNo().isEmpty())
                ? contractRepo.findByContractNo(dr.getContractNo())
                : null;
        if (contract != null) {
            invoice.setPartyCode(contract.getPartyCode());
            invoice.setNameOfParty(contract.getNameOfParty());
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
                saved, "Invoice " + saved.getInvoiceNo() + " for DR id " + saved.getDyedReceiveId());
        return saved;
    }

    /**
     * Records-only update — only the "anyone-can-make-a-mistake" fields:
     * date, GST flag, payment terms, remarks. Identity fields (DR link,
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
     * Build the InvoiceView (stored + derived) for an Invoice. Rate is read
     * live from Contract.rateA; qty is the DR's net (received − cut − shrinkage%);
     * amount/GST/total fall out from those two numbers.
     */
    public InvoiceView toView(Invoice inv) {
        InvoiceView v = new InvoiceView();
        v.setId(inv.getId());
        v.setInvoiceNo(inv.getInvoiceNo());
        v.setDated(inv.getDated());
        v.setDyedReceiveId(inv.getDyedReceiveId());
        v.setContractNo(inv.getContractNo());
        v.setPartyCode(inv.getPartyCode());
        v.setNameOfParty(inv.getNameOfParty());
        v.setGstInvoiceYesNo(inv.getGstInvoiceYesNo());
        v.setPaymentTerms(inv.getPaymentTerms());
        v.setRemarks(inv.getRemarks());

        DyedReceive dr = inv.getDyedReceiveId() != null
                ? dyedRepo.findById(inv.getDyedReceiveId()).orElse(null)
                : null;
        if (dr != null) {
            v.setDrId(dr.getNewId());
            v.setIssueId(dr.getIssueId());
            v.setQuality(dr.getQuality());
            v.setColor(dr.getColor());
            double qty = nz(dr.getQuantityKg());
            double cut = nz(dr.getCutPiecesKg());
            double shrinkPct = nz(dr.getShrinkage());
            double shrinkKg = qty * shrinkPct / 100.0;
            v.setQtyKg(Math.max(0.0, qty - cut - shrinkKg));
        }

        Contract c = (inv.getContractNo() != null && !inv.getContractNo().isEmpty())
                ? contractRepo.findByContractNo(inv.getContractNo())
                : null;
        double rate = (c != null && c.getRateA() != null) ? c.getRateA() : 0.0;
        v.setRate(rate);

        double qtyKg = v.getQtyKg() == null ? 0.0 : v.getQtyKg();
        double amount = qtyKg * rate;
        v.setAmount(amount);

        double gst = "Yes".equalsIgnoreCase(inv.getGstInvoiceYesNo()) ? amount * GST_RATE : 0.0;
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

    private static double nz(Double d) { return d == null ? 0.0 : d; }
    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }
}

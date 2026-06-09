package com.billing.system.controller;

import com.billing.system.dto.InvoiceView;
import com.billing.system.entity.Invoice;
import com.billing.system.service.InvoiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invoices")
@CrossOrigin
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<InvoiceView> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public InvoiceView getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Create — the body just needs {@code outwardGatePassId}; everything
     * else is auto-filled from the linked OGP / DR / Contract. Returns the
     * saved invoice as a view.
     */
    @PostMapping
    public InvoiceView save(@RequestBody Invoice invoice) {
        Invoice saved = service.save(invoice);
        return service.toView(saved);
    }

    /** Records-only edit of the four "fix-mistake" fields. Returns the view. */
    @PutMapping("/{id}")
    public InvoiceView update(@PathVariable Long id, @RequestBody Invoice patch) {
        Invoice saved = service.update(id, patch);
        return service.toView(saved);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

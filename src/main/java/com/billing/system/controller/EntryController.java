package com.billing.system.controller;

import com.billing.system.entity.Entry;
import com.billing.system.repository.EntryRepository;
import com.billing.system.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/entries")
@CrossOrigin
public class EntryController {

    private static final String ENTITY = "Entry";

    private final EntryRepository entryRepo;
    private final AuditService audit;

    public EntryController(EntryRepository entryRepo, AuditService audit) {
        this.entryRepo = entryRepo;
        this.audit = audit;
    }

    @GetMapping
    public List<Entry> getAll() {
        return entryRepo.findAll();
    }

    @GetMapping("/{id}")
    public Entry getOne(@PathVariable Long id) {
        return entryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + id));
    }

    @PostMapping
    public Entry save(@RequestBody Entry entry) {
        applyTotals(entry);
        boolean isUpdate = entry.getId() != null;
        Object before = isUpdate
                ? audit.snapshot(entryRepo.findById(entry.getId()).orElse(null))
                : null;

        Entry saved = entryRepo.save(entry);
        String id = String.valueOf(saved.getId());

        if (isUpdate && before != null) {
            audit.logUpdate(ENTITY, id, null, before, saved,
                    "Entry " + id + " updated");
        } else {
            audit.logCreate(ENTITY, id, null, saved,
                    "Entry " + id + " created");
        }
        return saved;
    }

    @PostMapping("/bulk")
    public List<Entry> saveAll(@RequestBody List<Entry> entries) {
        List<Entry> results = new ArrayList<>();
        for (Entry e : entries) {
            results.add(save(e));
        }
        return results;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Entry existing = entryRepo.findById(id).orElse(null);
        Object before = audit.snapshot(existing);
        entryRepo.deleteById(id);
        if (existing != null) {
            audit.logDelete(ENTITY, String.valueOf(id), null, before,
                    "Entry " + id + " deleted");
        }
    }

    private void applyTotals(Entry entry) {
        double qty = entry.getQuantityKgBilled() == null ? 0.0 : entry.getQuantityKgBilled();
        double rate = entry.getRate() == null ? 0.0 : entry.getRate();
        double amount = qty * rate;
        double gst = "Yes".equalsIgnoreCase(entry.getGstInvoiceYesNo()) ? amount * 0.18 : 0.0;
        entry.setAmount(amount);
        entry.setGstAmount(gst);
        entry.setTotalAmount(amount + gst);
    }
}

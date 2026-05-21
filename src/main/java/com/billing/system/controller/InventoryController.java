package com.billing.system.controller;

import com.billing.system.entity.Inventory;
import com.billing.system.repository.InventoryRepository;
import com.billing.system.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@CrossOrigin
public class InventoryController {

    private final InventoryRepository inventoryRepo;
    private final AuditService audit;

    public InventoryController(InventoryRepository inventoryRepo, AuditService audit) {
        this.inventoryRepo = inventoryRepo;
        this.audit = audit;
    }

    @GetMapping
    public List<Inventory> getAll() {
        return inventoryRepo.findAll();
    }

    @GetMapping("/Greigh")
    public List<Inventory> getGreigh() {
        return inventoryRepo.findByStage("GREIGH");
    }

    @GetMapping("/dyed")
    public List<Inventory> getDyed() {
        return inventoryRepo.findByStage("DYED");
    }

    /**
     * Manual stock correction. Only the quantity fields can be adjusted; the
     * identity fields (contract / quality / color / stage) stay fixed so the
     * per-contract inventory key is never disturbed. Every edit is audit-logged.
     */
    @PutMapping("/{id}")
    public Inventory update(@PathVariable Long id, @RequestBody Inventory patch) {
        Inventory existing = inventoryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory row not found: " + id));
        Object before = audit.snapshot(existing);

        if (patch.getAvailableKg() != null)     existing.setAvailableKg(patch.getAvailableKg());
        if (patch.getAvailableRolls() != null)  existing.setAvailableRolls(patch.getAvailableRolls());
        if (patch.getAvailableMeters() != null) existing.setAvailableMeters(patch.getAvailableMeters());

        Inventory saved = inventoryRepo.save(existing);
        audit.logUpdate("Inventory", String.valueOf(saved.getId()), saved.getContractNo(),
                before, saved,
                "Inventory adjusted — " + saved.getContractNo() + " / " + saved.getQuality()
                        + " / " + saved.getColor() + " (" + saved.getStage() + ")");
        return saved;
    }

    /** Remove an inventory row outright. Audit-logged. */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Inventory existing = inventoryRepo.findById(id).orElse(null);
        if (existing == null) return;
        Object before = audit.snapshot(existing);
        inventoryRepo.deleteById(id);
        audit.logDelete("Inventory", String.valueOf(id), existing.getContractNo(), before,
                "Inventory row deleted — " + existing.getContractNo() + " / "
                        + existing.getQuality() + " / " + existing.getColor()
                        + " (" + existing.getStage() + ")");
    }
}

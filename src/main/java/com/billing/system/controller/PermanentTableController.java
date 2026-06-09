package com.billing.system.controller;

import com.billing.system.entity.PermanentTable;
import com.billing.system.repository.PermanentTableRepository;
import com.billing.system.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/permanent-table")
public class PermanentTableController {

    private static final String ENTITY = "PermanentTable";

    private final PermanentTableRepository repo;
    private final AuditService audit;

    public PermanentTableController(PermanentTableRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @GetMapping
    public List<PermanentTable> getAll() {
        return repo.findAll();
    }

    @PostMapping
    public PermanentTable save(@RequestBody PermanentTable data) {

        // Normalize blanks to null so the DB unique constraint allows multiple rows without an NTN
        if (data.getNtn() != null && data.getNtn().isBlank()) data.setNtn(null);
        if (data.getNameOfParty() != null && data.getNameOfParty().isBlank()) data.setNameOfParty(null);

        // Auto-generate Party ID (P{yy}{seq}) when missing — covers imports
        // and any client that forgets to send one. Matches the AddParty form's
        // generator so the sequence stays continuous.
        if (data.getId() == null
                && (data.getPartyCode() == null || data.getPartyCode().isBlank())) {
            data.setPartyCode(generatePartyCode());
        }

        if (data.getNtn() != null) {
            repo.findByNtn(data.getNtn()).ifPresent(existing -> {
                if (!existing.getId().equals(data.getId())) {
                    throw new RuntimeException("NTN already exists");
                }
            });
        }

        if (data.getNameOfParty() != null) {
            repo.findByNameOfParty(data.getNameOfParty()).ifPresent(existing -> {
                if (!existing.getId().equals(data.getId())) {
                    throw new RuntimeException("Party Name already exists");
                }
            });
        }

        boolean isUpdate = data.getId() != null;
        Object before = isUpdate
                ? audit.snapshot(repo.findById(data.getId()).orElse(null))
                : null;

        PermanentTable saved = repo.save(data);
        String id = String.valueOf(saved.getId());
        String code = saved.getPartyCode();

        if (isUpdate && before != null) {
            audit.logUpdate(ENTITY, id, code, before, saved,
                    "Permanent party " + code + " updated");
        } else {
            audit.logCreate(ENTITY, id, code, saved,
                    "Permanent party " + code + " created");
        }
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        PermanentTable existing = repo.findById(id).orElse(null);
        Object before = audit.snapshot(existing);
        repo.deleteById(id);
        if (existing != null) {
            audit.logDelete(ENTITY, String.valueOf(id), existing.getPartyCode(), before,
                    "Permanent party " + existing.getPartyCode() + " deleted");
        }
    }

    @GetMapping("/search")
    public List<PermanentTable> search(@RequestParam String query) {
        List<PermanentTable> byName = repo.findByNameOfPartyContainingIgnoreCase(query);
        List<PermanentTable> byCode = repo.findByPartyCodeContainingIgnoreCase(query);
        byName.addAll(byCode);
        return byName;
    }

    /** Next Party id in P{yy}{seq} format (e.g. {@code P26001}). */
    private String generatePartyCode() {
        String yy = String.valueOf(LocalDate.now().getYear()).substring(2);
        String prefix = "P" + yy;
        int max = repo.findAll().stream()
                .map(PermanentTable::getPartyCode)
                .filter(s -> s != null && s.startsWith(prefix))
                .map(s -> {
                    try { return Integer.parseInt(s.substring(prefix.length())); }
                    catch (Exception e) { return 0; }
                })
                .max(Integer::compareTo)
                .orElse(0);
        return prefix + String.format("%03d", max + 1);
    }
}

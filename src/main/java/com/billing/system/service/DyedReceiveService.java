package com.billing.system.service;

import com.billing.system.entity.DyedReceive;
import com.billing.system.entity.IssueToDyeing;
import com.billing.system.repository.DyedReceiveRepository;
import com.billing.system.repository.IssueToDyeingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Dyed Receive is its own per-Issue stock pool — the completed-work returns
 * from Issue to Dyeing. It is deliberately decoupled from the per-contract
 * {@code Inventory} table: that table's DYED stage is reserved for fabric
 * arriving already dyed via an Inward Gate Pass (type=DYED).
 *
 * Lifecycle of one receipt:
 *   - Created against an Issue to Dyeing (FK).
 *   - {@code availableKg} initialised to {@code netKg = quantityKg - cut - shrinkage}.
 *   - Decremented later when an Outward GP draws from this receipt
 *     (Phase 2 — Outward source-pool picker).
 *
 * Edit and Delete are records-only: identity fields (issue link, contract,
 * quality, color) are not editable; quantities can be corrected.
 */
@Service
public class DyedReceiveService {

    private final DyedReceiveRepository dyedRepo;
    private final IssueToDyeingRepository issueRepo;
    private final AuditService audit;

    public DyedReceiveService(DyedReceiveRepository dyedRepo,
                              IssueToDyeingRepository issueRepo,
                              AuditService audit) {
        this.dyedRepo = dyedRepo;
        this.issueRepo = issueRepo;
        this.audit = audit;
    }

    @Transactional
    public DyedReceive save(DyedReceive receive) {

        if (receive.getIssueToDyeingId() == null) {
            throw new RuntimeException("Issue to Dyeing reference is required");
        }
        IssueToDyeing issue = issueRepo.findById(receive.getIssueToDyeingId())
                .orElseThrow(() -> new RuntimeException(
                        "Issue to Dyeing not found: " + receive.getIssueToDyeingId()));

        // Inherit identity fields from the Issue if the client didn't supply them.
        if (isBlank(receive.getContractNo()))   receive.setContractNo(issue.getContractNo());
        if (isBlank(receive.getQuality()))      receive.setQuality(issue.getQuality());
        if (isBlank(receive.getColor()))        receive.setColor(issue.getColor());
        if (isBlank(receive.getInwardId()))     receive.setInwardId(issue.getInwardId());
        receive.setIssueId(issue.getIssueId());

        if (receive.getColor() == null || receive.getColor().isEmpty()) receive.setColor("NA");
        if (receive.getQuantityKg() == null || receive.getQuantityKg() <= 0) {
            throw new RuntimeException("Quantity (kg) must be greater than 0");
        }

        // Over-receive guard: total of all DRs for this Issue can't exceed the issued kg.
        assertWithinIssueCapacity(issue, null, receive.getQuantityKg());

        // Sequence-based id (DR{yy}{seq}) so DRs read cleanly next to the
        // IGP / OGP / ITD / INV families rather than as one-off UUIDs.
        receive.setNewId(generateDrId());
        if (receive.getDated() == null) receive.setDated(LocalDate.now());

        // Shrinkage is a percentage of the received quantity (e.g. 5 = 5%).
        double cut = receive.getCutPiecesKg() == null ? 0.0 : receive.getCutPiecesKg();
        double shrinkPercent = receive.getShrinkage() == null ? 0.0 : receive.getShrinkage();
        double shrinkKg = receive.getQuantityKg() * shrinkPercent / 100.0;
        double netKg = Math.max(0.0, receive.getQuantityKg() - cut - shrinkKg);
        receive.setAvailableKg(netKg);

        DyedReceive saved = dyedRepo.save(receive);
        audit.logCreate("DyedReceive", String.valueOf(saved.getId()), saved.getNewId(),
                saved, "Dyed receipt " + saved.getNewId() + " for issue " + issue.getIssueId());
        return saved;
    }

    /**
     * Records-only update. Identity fields (issue link, contract, quality, color)
     * are NOT changeable. Quantities can be corrected; availableKg is recomputed
     * from the new net. (Note: this v1 recompute ignores any prior Outward draws
     * — once the source-pool Outward path lands in Phase 2 we'll need to
     * subtract delivered qty too.)
     */
    @Transactional
    public DyedReceive update(Long id, DyedReceive patch) {
        DyedReceive existing = dyedRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Dyed Receive not found: " + id));
        Object before = audit.snapshot(existing);

        if (patch.getDated() != null) existing.setDated(patch.getDated());
        if (patch.getCustomerLotNo() != null) existing.setCustomerLotNo(patch.getCustomerLotNo());
        if (patch.getFactoryLotNo() != null) existing.setFactoryLotNo(patch.getFactoryLotNo());
        if (patch.getQuantityKg() != null) existing.setQuantityKg(patch.getQuantityKg());
        if (patch.getCutPiecesKg() != null) existing.setCutPiecesKg(patch.getCutPiecesKg());
        if (patch.getShrinkage() != null) existing.setShrinkage(patch.getShrinkage());

        // Over-receive guard for edit — sum-of-OTHER-DRs plus this updated qty
        // must still fit within the linked Issue's issued kg.
        if (existing.getIssueToDyeingId() != null) {
            IssueToDyeing issue = issueRepo.findById(existing.getIssueToDyeingId()).orElse(null);
            if (issue != null) {
                assertWithinIssueCapacity(issue, existing.getId(),
                        existing.getQuantityKg() == null ? 0.0 : existing.getQuantityKg());
            }
        }

        double cut = existing.getCutPiecesKg() == null ? 0.0 : existing.getCutPiecesKg();
        double shrinkPercent = existing.getShrinkage() == null ? 0.0 : existing.getShrinkage();
        double q = existing.getQuantityKg() == null ? 0.0 : existing.getQuantityKg();
        double shrinkKg = q * shrinkPercent / 100.0;
        existing.setAvailableKg(Math.max(0.0, q - cut - shrinkKg));

        DyedReceive saved = dyedRepo.save(existing);
        audit.logUpdate("DyedReceive", String.valueOf(saved.getId()), saved.getNewId(),
                before, saved, "Dyed Receive " + saved.getNewId() + " updated");
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        DyedReceive existing = dyedRepo.findById(id).orElse(null);
        if (existing == null) return;
        Object before = audit.snapshot(existing);
        dyedRepo.deleteById(id);
        audit.logDelete("DyedReceive", String.valueOf(id), existing.getNewId(), before,
                "Dyed Receive " + existing.getNewId() + " deleted");
    }

    public List<DyedReceive> getAll() {
        return dyedRepo.findAll();
    }

    public List<DyedReceive> getByIssue(Long issueToDyeingId) {
        return dyedRepo.findByIssueToDyeingId(issueToDyeingId);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Next DR business id in DR{yy}{seq} format (e.g. {@code DR26001}).
     * Old DR-XXXXXX UUID rows are ignored by the prefix filter and stay
     * untouched.
     */
    private String generateDrId() {
        String yy = String.valueOf(LocalDate.now().getYear()).substring(2);
        String prefix = "DR" + yy;
        int max = dyedRepo.findAll().stream()
                .map(DyedReceive::getNewId)
                .filter(s -> s != null && s.startsWith(prefix))
                .map(s -> {
                    try { return Integer.parseInt(s.substring(prefix.length())); }
                    catch (Exception e) { return 0; }
                })
                .max(Integer::compareTo)
                .orElse(0);
        return prefix + String.format("%03d", max + 1);
    }

    /**
     * Reject a save/update that would push total received kg above the source
     * Issue's issued kg. Partial returns are fine — many DRs per Issue, just
     * never more than what was sent out (with a tiny float tolerance).
     *
     * @param excludeDrId id of the DR being updated (so its own qty isn't
     *                    counted twice); pass {@code null} on create.
     */
    private void assertWithinIssueCapacity(IssueToDyeing issue, Long excludeDrId, double thisDrQtyKg) {
        double issuedKg = issue.getQuantityKg() == null ? 0.0 : issue.getQuantityKg();
        double otherReceived = dyedRepo.findByIssueToDyeingId(issue.getId()).stream()
                .filter(dr -> excludeDrId == null || !dr.getId().equals(excludeDrId))
                .map(dr -> dr.getQuantityKg() == null ? 0.0 : dr.getQuantityKg())
                .reduce(0.0, Double::sum);
        if (otherReceived + thisDrQtyKg > issuedKg + 0.001) {
            double remaining = Math.max(0.0, issuedKg - otherReceived);
            throw new RuntimeException(
                    "Over-receive against issue " + issue.getIssueId() + ": issued "
                    + issuedKg + " kg, already received " + otherReceived + " kg — "
                    + "only " + remaining + " kg remaining to receive."
            );
        }
    }
}

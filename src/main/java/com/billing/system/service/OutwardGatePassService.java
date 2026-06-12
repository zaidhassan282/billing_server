package com.billing.system.service;

import com.billing.system.entity.DyedReceive;
import com.billing.system.entity.OutwardGatePass;
import com.billing.system.entity.OutwardItem;
import com.billing.system.repository.DyedReceiveRepository;
import com.billing.system.repository.OutwardGatePassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Outward Gate Pass is the "deliver the dyed fabric to the customer" step
 * that sits between Dyed Receive and Invoice. Every new OGP must draw from
 * a DyedReceive. The dyed pool lives in {@code DyedReceive.availableKg} —
 * NOT in the per-contract {@code Inventory} table (that pool is reserved
 * for fabric that arrives already dyed via an Inward Gate Pass of
 * type=DYED).
 *
 * Cardinality:
 *   - DR → OGP: 1:N (partial deliveries allowed — total OGP kg ≤ DR.availableKg)
 *   - OGP → Invoice: 1:1 (enforced on the Invoice side)
 *
 * Records-only updates EXCEPT for the DR pool — create deducts, delete
 * refunds, edit re-balances. Audit-logged.
 */
@Service
public class OutwardGatePassService {

    private static final String TYPE_DELIVERY = "DELIVERY";
    private static final String TYPE_RETURN = "RETURN";

    private final OutwardGatePassRepository outwardRepo;
    private final DyedReceiveRepository dyedRepo;
    private final AuditService audit;

    public OutwardGatePassService(OutwardGatePassRepository outwardRepo,
                                  DyedReceiveRepository dyedRepo,
                                  AuditService audit) {
        this.outwardRepo = outwardRepo;
        this.dyedRepo = dyedRepo;
        this.audit = audit;
    }

    @Transactional
    public OutwardGatePass save(OutwardGatePass outward) {

        if (outward.getDyedReceiveId() == null) {
            throw new RuntimeException(
                    "Dyed Receive reference is required — Outward Gate Pass must be "
                    + "generated from a Dyed Receive");
        }
        DyedReceive dr = dyedRepo.findById(outward.getDyedReceiveId())
                .orElseThrow(() -> new RuntimeException(
                        "Dyed Receive not found: " + outward.getDyedReceiveId()));

        if (outward.getItems() == null || outward.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }

        // Inherit identity fields from the DR — OGP is its delivery slip.
        outward.setDrId(dr.getNewId());
        if (isBlank(outward.getContractNo()))    outward.setContractNo(dr.getContractNo());
        if (isBlank(outward.getInwardId()))      outward.setInwardId(dr.getInwardId());
        if (isBlank(outward.getCustomerName()))  outward.setCustomerName(dr.getNameOfParty());
        if (isBlank(outward.getCustomerCode()))  outward.setCustomerCode(dr.getPartyCode());
        if (isBlank(outward.getCustomerLotNo())) outward.setCustomerLotNo(dr.getCustomerLotNo());
        if (isBlank(outward.getFactoryLotNo()))  outward.setFactoryLotNo(dr.getFactoryLotNo());
        if (isBlank(outward.getFabricType()))    outward.setFabricType("Dyed");

        if (isBlank(outward.getOutwardId())) outward.setOutwardId(generateOutwardId());
        if (outward.getDated() == null)      outward.setDated(LocalDate.now());
        if (isBlank(outward.getType()))      outward.setType(TYPE_DELIVERY);

        double totalKg = 0.0;
        for (OutwardItem item : outward.getItems()) {
            item.setOutward(outward);
            if (isBlank(item.getQuality())) item.setQuality(dr.getQuality());
            if (isBlank(item.getColor()))   item.setColor(dr.getColor() == null ? "NA" : dr.getColor());
            if (item.getKg() == null) item.setKg(0.0);
            if (item.getMeters() == null) item.setMeters(0.0);
            if (item.getRoll() == null) item.setRoll(0);
            if (item.getKg() <= 0 && item.getRoll() <= 0 && item.getMeters() <= 0) {
                throw new RuntimeException(
                        "Item '" + item.getQuality()
                        + "' needs at least one of weight (kg), rolls, or meters greater than 0");
            }
            totalKg += item.getKg();
        }

        // Over-draw guard before mutating DR.availableKg.
        assertWithinDrCapacity(dr, null, totalKg);

        double prevAvailable = nz(dr.getAvailableKg());
        dr.setAvailableKg(Math.max(0.0, prevAvailable - totalKg));
        dyedRepo.save(dr);

        OutwardGatePass saved = outwardRepo.save(outward);
        audit.logCreate("OutwardGatePass", String.valueOf(saved.getId()), saved.getOutwardId(),
                saved, "Outward gate pass " + saved.getOutwardId()
                        + " for DR " + dr.getNewId() + " (" + totalKg + " kg)");
        return saved;
    }

    public OutwardGatePass issue(OutwardGatePass outward) {
        outward.setType(TYPE_DELIVERY);
        return save(outward);
    }

    public OutwardGatePass returnFromCustomer(OutwardGatePass outward) {
        outward.setType(TYPE_RETURN);
        return save(outward);
    }

    /**
     * Records-only update with one exception: changing item kg re-balances
     * the linked DR pool (refund old, re-deduct new) so availableKg stays
     * truthful. Identity fields (DR link, contract, party) are not changed.
     */
    @Transactional
    public OutwardGatePass update(Long id, OutwardGatePass patch) {
        OutwardGatePass existing = outwardRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Outward gate pass not found: " + id));
        Object before = audit.snapshot(existing);

        if (patch.getItems() == null || patch.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }

        if (patch.getDated() != null) existing.setDated(patch.getDated());
        existing.setCustomerCode(patch.getCustomerCode());
        existing.setCustomerName(patch.getCustomerName());
        existing.setCustomerLotNo(patch.getCustomerLotNo());
        existing.setFactoryLotNo(patch.getFactoryLotNo());
        if (patch.getType() != null && !patch.getType().isEmpty()) {
            existing.setType(patch.getType());
        }
        existing.setAddress(patch.getAddress());
        existing.setVehicleNo(patch.getVehicleNo());
        existing.setDriverName(patch.getDriverName());
        existing.setReferenceNo(patch.getReferenceNo());
        if (patch.getFabricType() != null) existing.setFabricType(patch.getFabricType());
        existing.setGateTime(patch.getGateTime());
        existing.setSecurityGuardName(patch.getSecurityGuardName());
        existing.setCheckedBy(patch.getCheckedBy());

        double oldTotalKg = sumItemKg(existing.getItems());

        if (existing.getItems() == null) {
            existing.setItems(new ArrayList<>());
        } else {
            existing.getItems().clear();
        }
        double newTotalKg = 0.0;
        for (OutwardItem it : patch.getItems()) {
            it.setId(null);
            it.setOutward(existing);
            if (isBlank(it.getQuality())) it.setQuality("NA");
            if (isBlank(it.getColor()))   it.setColor("NA");
            if (it.getKg() == null) it.setKg(0.0);
            if (it.getMeters() == null) it.setMeters(0.0);
            if (it.getRoll() == null) it.setRoll(0);
            existing.getItems().add(it);
            newTotalKg += it.getKg();
        }

        // Re-balance the DR pool (refund old, re-deduct new).
        if (existing.getDyedReceiveId() != null) {
            DyedReceive dr = dyedRepo.findById(existing.getDyedReceiveId()).orElse(null);
            if (dr != null) {
                double refunded = nz(dr.getAvailableKg()) + oldTotalKg;
                dr.setAvailableKg(refunded);
                assertWithinDrCapacity(dr, existing.getId(), newTotalKg);
                dr.setAvailableKg(Math.max(0.0, refunded - newTotalKg));
                dyedRepo.save(dr);
            }
        }

        OutwardGatePass saved = outwardRepo.save(existing);
        audit.logUpdate("OutwardGatePass", String.valueOf(saved.getId()), saved.getOutwardId(),
                before, saved, "Outward gate pass " + saved.getOutwardId() + " updated");
        return saved;
    }

    /**
     * Delete the OGP record AND refund its kg to the linked DR pool —
     * otherwise the pool would keep counting a delivery that no longer
     * exists. (Different from Inward/ISD where delete is purely records-only.)
     */
    @Transactional
    public void delete(Long id) {
        OutwardGatePass existing = outwardRepo.findById(id).orElse(null);
        if (existing == null) return;
        Object before = audit.snapshot(existing);

        double refundKg = sumItemKg(existing.getItems());
        if (existing.getDyedReceiveId() != null && refundKg > 0) {
            DyedReceive dr = dyedRepo.findById(existing.getDyedReceiveId()).orElse(null);
            if (dr != null) {
                dr.setAvailableKg(nz(dr.getAvailableKg()) + refundKg);
                dyedRepo.save(dr);
            }
        }
        outwardRepo.deleteById(id);
        audit.logDelete("OutwardGatePass", String.valueOf(id), existing.getOutwardId(), before,
                "Outward gate pass " + existing.getOutwardId() + " deleted (refunded "
                + refundKg + " kg to DR)");
    }

    public List<OutwardGatePass> getAll() {
        return outwardRepo.findAll();
    }

    public OutwardGatePass getByOutwardId(String outwardId) {
        return outwardRepo.findByOutwardId(outwardId)
                .orElseThrow(() -> new RuntimeException("Outward not found: " + outwardId));
    }

    public List<OutwardGatePass> getByDyedReceive(Long dyedReceiveId) {
        return outwardRepo.findByDyedReceiveId(dyedReceiveId);
    }

    /**
     * Reject a save/update that would push total delivered kg past the
     * source DR's availableKg. Mirrors {@code DyedReceiveService}'s
     * over-receive guard.
     *
     * @param excludeOgpId id of the OGP being updated (so its own draw
     *                     isn't double-counted); pass {@code null} on create.
     */
    private void assertWithinDrCapacity(DyedReceive dr, Long excludeOgpId, double thisOgpKg) {
        // NOTE: availableKg here has ALREADY been refunded for excludeOgp on the
        // update path, so we only sum OGPs we're NOT replacing.
        double available = nz(dr.getAvailableKg());
        if (thisOgpKg > available + 0.001) {
            throw new RuntimeException(
                    "Over-draw from DR " + dr.getNewId() + ": only " + available
                    + " kg remaining, requested " + thisOgpKg + " kg.");
        }
    }

    private String generateOutwardId() {
        String yy = String.valueOf(LocalDate.now().getYear()).substring(2);
        String prefix = "OGP" + yy;
        // PERF-3 + P2-5 — see InwardService.generateInwardId for the rationale.
        int max = outwardRepo.findFirstByOutwardIdStartingWithOrderByOutwardIdDesc(prefix)
                .map(OutwardGatePass::getOutwardId)
                .map(s -> {
                    try { return Integer.parseInt(s.substring(prefix.length())); }
                    catch (Exception e) { return 0; }
                })
                .orElse(0);
        return prefix + String.format("%03d", max + 1);
    }

    private static double sumItemKg(List<OutwardItem> items) {
        if (items == null) return 0.0;
        double sum = 0.0;
        for (OutwardItem it : items) sum += it.getKg() == null ? 0.0 : it.getKg();
        return sum;
    }

    private static double nz(Double d) { return d == null ? 0.0 : d; }
    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }
}

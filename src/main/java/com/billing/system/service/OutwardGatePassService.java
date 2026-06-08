package com.billing.system.service;

import com.billing.system.entity.Inventory;
import com.billing.system.entity.OutwardGatePass;
import com.billing.system.entity.OutwardItem;
import com.billing.system.enums.FabricStage;
import com.billing.system.repository.InventoryRepository;
import com.billing.system.repository.OutwardGatePassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OutwardGatePassService {

    private static final String TYPE_ISSUE = "ISSUE";
    private static final String TYPE_DELIVERY = "DELIVERY";
    private static final String TYPE_RETURN = "RETURN";

    private final OutwardGatePassRepository outwardRepo;
    private final InventoryRepository inventoryRepo;
    private final AuditService audit;

    public OutwardGatePassService(OutwardGatePassRepository outwardRepo,
                                  InventoryRepository inventoryRepo,
                                  AuditService audit) {
        this.outwardRepo = outwardRepo;
        this.inventoryRepo = inventoryRepo;
        this.audit = audit;
    }

    @Transactional
    public OutwardGatePass save(OutwardGatePass outward) {

        if (outward.getOutwardId() == null || outward.getOutwardId().isEmpty()) {
            outward.setOutwardId("OGP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (outward.getDated() == null) {
            outward.setDated(LocalDate.now());
        }
        if (outward.getType() == null || outward.getType().isEmpty()) {
            outward.setType(TYPE_DELIVERY);
        }
        if (outward.getContractNo() == null || outward.getContractNo().isEmpty()) {
            throw new RuntimeException("Contract No is required (stock is scoped per contract)");
        }
        if (outward.getItems() == null || outward.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }
        if (outward.getCustomerName() == null || outward.getCustomerName().isEmpty()) {
            throw new RuntimeException("Customer name is required");
        }

        String contractNo = outward.getContractNo();
        String stage = "DYED".equalsIgnoreCase(outward.getFabricType())
                ? FabricStage.DYED.name()
                : FabricStage.GREIGH.name();

        for (OutwardItem item : outward.getItems()) {
            item.setOutward(outward);

            if (item.getQuality() == null || item.getQuality().isEmpty()) {
                throw new RuntimeException("Quality is required");
            }
            if (item.getKg() == null) item.setKg(0.0);
            if (item.getMeters() == null) item.setMeters(0.0);
            if (item.getRoll() == null) item.setRoll(0);
            if (item.getKg() <= 0 && item.getRoll() <= 0 && item.getMeters() <= 0) {
                throw new RuntimeException(
                        "Item '" + item.getQuality()
                        + "' needs at least one of weight (kg), rolls, or meters greater than 0");
            }
            if (item.getColor() == null || item.getColor().isEmpty()) {
                item.setColor("NA");
            }

            // Both DELIVERY and RETURN deduct (per business rule:
            // "When Greigh/dyed is used or returned it decrements inventory").
            deductFromInventory(contractNo, stage, item);
        }

        OutwardGatePass saved = outwardRepo.save(outward);
        audit.logCreate("OutwardGatePass", String.valueOf(saved.getId()), saved.getOutwardId(),
                saved, "Outward gate pass " + saved.getOutwardId()
                        + " (" + saved.getType() + ") for contract " + contractNo);
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

    private void deductFromInventory(String contractNo, String stage, OutwardItem item) {
        Inventory inv = inventoryRepo
                .findByContractNoAndQualityAndColorAndStage(contractNo, item.getQuality(), item.getColor(), stage)
                .orElseThrow(() -> new RuntimeException(
                        "No " + stage + " stock for contract " + contractNo
                                + " — " + item.getQuality() + " / " + item.getColor()));

        double availKg = inv.getAvailableKg() == null ? 0.0 : inv.getAvailableKg();
        double availM  = inv.getAvailableMeters() == null ? 0.0 : inv.getAvailableMeters();
        int    availR  = inv.getAvailableRolls()  == null ? 0   : inv.getAvailableRolls();

        double needKg = item.getKg() == null ? 0.0 : item.getKg();
        double needM  = item.getMeters() == null ? 0.0 : item.getMeters();
        int    needR  = item.getRoll() == null ? 0 : item.getRoll();

        if (needKg > 0 && availKg < needKg) {
            throw new RuntimeException("Not enough kg for " + item.getQuality()
                    + " (avail " + availKg + ", need " + needKg + ")");
        }
        if (needR > 0 && availR < needR) {
            throw new RuntimeException("Not enough rolls for " + item.getQuality()
                    + " (avail " + availR + ", need " + needR + ")");
        }
        if (needM > 0 && availM < needM) {
            throw new RuntimeException("Not enough meters for " + item.getQuality()
                    + " (avail " + availM + ", need " + needM + ")");
        }

        inv.setAvailableKg(Math.max(0.0, availKg - needKg));
        inv.setAvailableRolls(Math.max(0, availR - needR));
        inv.setAvailableMeters(Math.max(0.0, availM - needM));
        inventoryRepo.save(inv);
    }

    public List<OutwardGatePass> getAll() {
        return outwardRepo.findAll();
    }

    public OutwardGatePass getByOutwardId(String outwardId) {
        return outwardRepo.findByOutwardId(outwardId)
                .orElseThrow(() -> new RuntimeException("Outward not found: " + outwardId));
    }

    /**
     * Records-only update of an existing gate pass.
     * NOTE: inventory is intentionally NOT re-deducted — only the original
     * create deducts stock — so editing a gate pass can never double-decrement
     * inventory. Correct stock on the Inventory page if an edit changes
     * quantities.
     */
    @Transactional
    public OutwardGatePass update(Long id, OutwardGatePass patch) {
        OutwardGatePass existing = outwardRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Outward gate pass not found: " + id));
        Object before = audit.snapshot(existing);

        if (patch.getContractNo() == null || patch.getContractNo().isEmpty()) {
            throw new RuntimeException("Contract No is required");
        }
        if (patch.getCustomerName() == null || patch.getCustomerName().isEmpty()) {
            throw new RuntimeException("Customer name is required");
        }
        if (patch.getItems() == null || patch.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }

        if (patch.getDated() != null) existing.setDated(patch.getDated());
        existing.setInwardId(patch.getInwardId());
        existing.setContractNo(patch.getContractNo());
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
        existing.setFabricType(patch.getFabricType());
        existing.setGateTime(patch.getGateTime());
        existing.setSecurityGuardName(patch.getSecurityGuardName());
        existing.setCheckedBy(patch.getCheckedBy());

        // Replace item rows (orphanRemoval deletes the old ones)
        if (existing.getItems() == null) {
            existing.setItems(new ArrayList<>());
        } else {
            existing.getItems().clear();
        }
        for (OutwardItem it : patch.getItems()) {
            it.setId(null);
            it.setOutward(existing);
            if (it.getQuality() == null || it.getQuality().isEmpty()) {
                throw new RuntimeException("Quality is required");
            }
            if (it.getKg() == null) it.setKg(0.0);
            if (it.getMeters() == null) it.setMeters(0.0);
            if (it.getRoll() == null) it.setRoll(0);
            if (it.getColor() == null || it.getColor().isEmpty()) it.setColor("NA");
            existing.getItems().add(it);
        }

        OutwardGatePass saved = outwardRepo.save(existing);
        audit.logUpdate("OutwardGatePass", String.valueOf(saved.getId()), saved.getOutwardId(),
                before, saved, "Outward gate pass " + saved.getOutwardId() + " updated");
        return saved;
    }

    /** Records-only delete — inventory is not restored. Audit-logged. */
    @Transactional
    public void delete(Long id) {
        OutwardGatePass existing = outwardRepo.findById(id).orElse(null);
        if (existing == null) return;
        Object before = audit.snapshot(existing);
        outwardRepo.deleteById(id);
        audit.logDelete("OutwardGatePass", String.valueOf(id), existing.getOutwardId(), before,
                "Outward gate pass " + existing.getOutwardId() + " deleted");
    }
}

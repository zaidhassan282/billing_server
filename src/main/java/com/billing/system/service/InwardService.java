package com.billing.system.service;

import com.billing.system.entity.*;
import com.billing.system.enums.FabricStage;
import com.billing.system.enums.MovementType;
import com.billing.system.repository.FabricMovementRepository;
import com.billing.system.repository.InventoryRepository;
import com.billing.system.repository.InwardGatePassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InwardService {

    private final InwardGatePassRepository inwardRepo;
    private final InventoryRepository inventoryRepo;
    private final FabricMovementRepository movementRepo;
    private final AuditService audit;

    public InwardService(InwardGatePassRepository inwardRepo,
                         InventoryRepository inventoryRepo,
                         FabricMovementRepository movementRepo,
                         AuditService audit) {
        this.inwardRepo = inwardRepo;
        this.inventoryRepo = inventoryRepo;
        this.movementRepo = movementRepo;
        this.audit = audit;
    }

    @Transactional
    public InwardGatePass save(InwardGatePass inward) {

        if (inward.getInwardId() == null || inward.getInwardId().isEmpty()) {
            inward.setInwardId("IGP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }

        if (inward.getDated() == null) {
            inward.setDated(LocalDate.now());
        }

        if (inward.getContractNo() == null || inward.getContractNo().isEmpty()) {
            throw new RuntimeException("Contract No is required (stock is scoped per contract)");
        }

        if (inward.getItems() == null || inward.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }

        String contractNo = inward.getContractNo();
        String inwardType = inward.getFabricType();
        boolean isDyed = (inward.getIsDyedFabric() != null && inward.getIsDyedFabric())
                || (inwardType != null && inwardType.toUpperCase().contains("DYED"));
        String stage = isDyed ? FabricStage.DYED.name() : FabricStage.GREIGH.name();

        for (InwardItem item : inward.getItems()) {
            item.setInward(inward);

            // Quality is optional — default blank to "NA" so the per-contract
            // inventory key (contract/quality/color/stage) still has a value.
            if (item.getQuality() == null || item.getQuality().isEmpty()) {
                item.setQuality("NA");
            }
            // Authoritative weight for inventory: prefer totalWeight, fall back to kg,
            // then to fabricWeight + ribWeight if the user filled those instead.
            double effectiveKg = pickWeight(item);
            // Mirror the chosen weight back into `kg` so downstream code & old reports keep working.
            item.setKg(effectiveKg);
            if (item.getTotalWeight() == null || item.getTotalWeight() <= 0) {
                item.setTotalWeight(effectiveKg);
            }
            if (item.getMeters() == null) item.setMeters(0.0);
            if (item.getRoll() == null) item.setRoll(0);

            if (effectiveKg <= 0 && item.getRoll() <= 0 && item.getMeters() <= 0) {
                throw new RuntimeException(
                        "Item '" + item.getQuality()
                        + "' needs at least one of weight (kg), rolls, or meters greater than 0");
            }
            if (item.getColor() == null || item.getColor().isEmpty()) {
                item.setColor("NA");
            }

            mergeIntoInventory(contractNo, stage, inward.getInwardId(), item);
            recordMovement(inward.getInwardId(), item, stage);
        }

        InwardGatePass saved = inwardRepo.save(inward);
        audit.logCreate("InwardGatePass", String.valueOf(saved.getId()), saved.getInwardId(),
                saved, "Inward gate pass " + saved.getInwardId() + " created");
        return saved;
    }

    private void mergeIntoInventory(String contractNo, String stage, String refId, InwardItem item) {
        Inventory inv = inventoryRepo
                .findByContractNoAndQualityAndColorAndStage(contractNo, item.getQuality(), item.getColor(), stage)
                .orElse(null);

        if (inv == null) {
            inv = new Inventory();
            inv.setContractNo(contractNo);
            inv.setRefId(refId);
            inv.setStage(stage);
            inv.setQuality(item.getQuality());
            inv.setColor(item.getColor());
            inv.setAvailableKg(item.getKg());
            inv.setAvailableMeters(item.getMeters());
            inv.setAvailableRolls(item.getRoll());
        } else {
            inv.setAvailableKg(safeAdd(inv.getAvailableKg(), item.getKg()));
            inv.setAvailableMeters(safeAdd(inv.getAvailableMeters(), item.getMeters()));
            inv.setAvailableRolls(safeAddInt(inv.getAvailableRolls(), item.getRoll()));
        }
        inventoryRepo.save(inv);
    }

    private void recordMovement(String refId, InwardItem item, String stage) {
        FabricMovement m = new FabricMovement();
        m.setRefId(refId);
        m.setQuality(item.getQuality());
        m.setQuantityKg(item.getKg());
        m.setType(MovementType.INWARD);
        m.setFromStage(null);
        m.setToStage(FabricStage.valueOf(stage));
        m.setDated(LocalDate.now());
        movementRepo.save(m);
    }

    private static double safeAdd(Double a, Double b) {
        return (a == null ? 0.0 : a) + (b == null ? 0.0 : b);
    }

    /**
     * Resolve the kg value that should flow into inventory.
     * Order of precedence: totalWeight → kg → fabricWeight + ribWeight.
     */
    private static double pickWeight(InwardItem item) {
        if (item.getTotalWeight() != null && item.getTotalWeight() > 0) {
            return item.getTotalWeight();
        }
        if (item.getKg() != null && item.getKg() > 0) {
            return item.getKg();
        }
        double fw = item.getFabricWeight() == null ? 0.0 : item.getFabricWeight();
        double rw = item.getRibWeight() == null ? 0.0 : item.getRibWeight();
        return fw + rw;
    }

    private static int safeAddInt(Integer a, Integer b) {
        return (a == null ? 0 : a) + (b == null ? 0 : b);
    }

    public List<InwardGatePass> getAll() {
        return inwardRepo.findAll();
    }

    public InwardGatePass getByInwardId(String inwardId) {
        return inwardRepo.findByInwardId(inwardId)
                .orElseThrow(() -> new RuntimeException("Inward not found: " + inwardId));
    }

    /**
     * Records-only update of an existing gate pass.
     * NOTE: inventory and the fabric-movement ledger are intentionally NOT
     * re-applied — only the original create merges stock — so editing a gate
     * pass can never double-count inventory. Correct stock on the Inventory
     * page if an edit changes quantities.
     */
    @Transactional
    public InwardGatePass update(Long id, InwardGatePass patch) {
        InwardGatePass existing = inwardRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Inward gate pass not found: " + id));
        Object before = audit.snapshot(existing);

        if (patch.getContractNo() == null || patch.getContractNo().isEmpty()) {
            throw new RuntimeException("Contract No is required");
        }
        if (patch.getItems() == null || patch.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }

        if (patch.getDated() != null) existing.setDated(patch.getDated());
        existing.setContractNo(patch.getContractNo());
        existing.setPartyCode(patch.getPartyCode());
        existing.setNameOfParty(patch.getNameOfParty());
        existing.setSupplierName(patch.getSupplierName());
        existing.setCustomerLotNo(patch.getCustomerLotNo());
        existing.setFactoryLotNo(patch.getFactoryLotNo());
        existing.setAddress(patch.getAddress());
        existing.setVehicleNo(patch.getVehicleNo());
        existing.setDriverName(patch.getDriverName());
        existing.setReferenceNo(patch.getReferenceNo());
        existing.setFabricType(patch.getFabricType());
        existing.setIsDyedFabric(patch.getIsDyedFabric());
        existing.setIsGreigeFabric(patch.getIsGreigeFabric());
        existing.setGateTime(patch.getGateTime());
        existing.setSecurityGuardName(patch.getSecurityGuardName());
        existing.setCheckedBy(patch.getCheckedBy());
        existing.setDyeing(patch.getDyeing());
        existing.setPChallanNo(patch.getPChallanNo());
        existing.setDeliveryDate(patch.getDeliveryDate());

        // Replace the item rows (orphanRemoval deletes the old ones).
        if (existing.getItems() == null) {
            existing.setItems(new ArrayList<>());
        } else {
            existing.getItems().clear();
        }
        for (InwardItem it : patch.getItems()) {
            it.setId(null);
            it.setInward(existing);
            if (it.getQuality() == null || it.getQuality().isEmpty()) {
                it.setQuality("NA");
            }
            double effectiveKg = pickWeight(it);
            it.setKg(effectiveKg);
            if (it.getTotalWeight() == null || it.getTotalWeight() <= 0) {
                it.setTotalWeight(effectiveKg);
            }
            if (it.getMeters() == null) it.setMeters(0.0);
            if (it.getRoll() == null) it.setRoll(0);
            if (it.getColor() == null || it.getColor().isEmpty()) it.setColor("NA");
            existing.getItems().add(it);
        }

        InwardGatePass saved = inwardRepo.save(existing);
        audit.logUpdate("InwardGatePass", String.valueOf(saved.getId()), saved.getInwardId(),
                before, saved, "Inward gate pass " + saved.getInwardId() + " updated");
        return saved;
    }

    /** Records-only delete — inventory is not reversed. Audit-logged. */
    @Transactional
    public void delete(Long id) {
        InwardGatePass existing = inwardRepo.findById(id).orElse(null);
        if (existing == null) return;
        Object before = audit.snapshot(existing);
        inwardRepo.deleteById(id);
        audit.logDelete("InwardGatePass", String.valueOf(id), existing.getInwardId(), before,
                "Inward gate pass " + existing.getInwardId() + " deleted");
    }
}

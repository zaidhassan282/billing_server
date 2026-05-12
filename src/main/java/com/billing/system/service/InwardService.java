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
}

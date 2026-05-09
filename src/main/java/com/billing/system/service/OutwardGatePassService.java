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
            if (item.getKg() == null || item.getKg() <= 0) {
                throw new RuntimeException("Weight (kg) must be greater than 0");
            }
            if (item.getColor() == null || item.getColor().isEmpty()) {
                item.setColor("NA");
            }
            if (item.getMeters() == null) item.setMeters(0.0);
            if (item.getRoll() == null) item.setRoll(0);

            // Both DELIVERY and RETURN deduct (per business rule:
            // "When grey/dyed is used or returned it decrements inventory").
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

        if (availKg < item.getKg()) {
            throw new RuntimeException("Not enough kg for " + item.getQuality()
                    + " (avail " + availKg + ", need " + item.getKg() + ")");
        }
        if (item.getRoll() != null && item.getRoll() > 0 && availR < item.getRoll()) {
            throw new RuntimeException("Not enough rolls for " + item.getQuality()
                    + " (avail " + availR + ", need " + item.getRoll() + ")");
        }
        if (item.getMeters() != null && item.getMeters() > 0 && availM < item.getMeters()) {
            throw new RuntimeException("Not enough meters for " + item.getQuality()
                    + " (avail " + availM + ", need " + item.getMeters() + ")");
        }

        inv.setAvailableKg(availKg - item.getKg());
        inv.setAvailableRolls(availR - (item.getRoll() == null ? 0 : item.getRoll()));
        inv.setAvailableMeters(Math.max(0.0, availM - (item.getMeters() == null ? 0.0 : item.getMeters())));
        inventoryRepo.save(inv);
    }

    public List<OutwardGatePass> getAll() {
        return outwardRepo.findAll();
    }

    public OutwardGatePass getByOutwardId(String outwardId) {
        return outwardRepo.findByOutwardId(outwardId)
                .orElseThrow(() -> new RuntimeException("Outward not found: " + outwardId));
    }
}

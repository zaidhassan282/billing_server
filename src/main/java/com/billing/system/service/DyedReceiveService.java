package com.billing.system.service;

import com.billing.system.entity.DyedReceive;
import com.billing.system.entity.FabricMovement;
import com.billing.system.entity.Inventory;
import com.billing.system.enums.FabricStage;
import com.billing.system.enums.MovementType;
import com.billing.system.repository.DyedReceiveRepository;
import com.billing.system.repository.FabricMovementRepository;
import com.billing.system.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class DyedReceiveService {

    private final DyedReceiveRepository dyedRepo;
    private final InventoryRepository inventoryRepo;
    private final FabricMovementRepository movementRepo;
    private final AuditService audit;

    public DyedReceiveService(DyedReceiveRepository dyedRepo,
                              InventoryRepository inventoryRepo,
                              FabricMovementRepository movementRepo,
                              AuditService audit) {
        this.dyedRepo = dyedRepo;
        this.inventoryRepo = inventoryRepo;
        this.movementRepo = movementRepo;
        this.audit = audit;
    }

    @Transactional
    public DyedReceive save(DyedReceive receive) {

        if (receive.getQuality() == null || receive.getQuality().isEmpty()) {
            throw new RuntimeException("Quality is required");
        }
        if (receive.getQuantityKg() == null || receive.getQuantityKg() <= 0) {
            throw new RuntimeException("Quantity (kg) must be greater than 0");
        }
        if (receive.getContractNo() == null || receive.getContractNo().isEmpty()) {
            throw new RuntimeException("Contract No is required");
        }
        if (receive.getColor() == null || receive.getColor().isEmpty()) {
            receive.setColor("NA");
        }

        receive.setNewId("DR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        if (receive.getDated() == null) receive.setDated(LocalDate.now());

        double cut = receive.getCutPiecesKg() == null ? 0.0 : receive.getCutPiecesKg();
        double shrink = receive.getShrinkage() == null ? 0.0 : receive.getShrinkage();
        double netKg = Math.max(0.0, receive.getQuantityKg() - cut - shrink);

        Inventory inv = inventoryRepo
                .findByContractNoAndQualityAndColorAndStage(
                        receive.getContractNo(), receive.getQuality(), receive.getColor(),
                        FabricStage.DYED.name())
                .orElse(null);

        if (inv == null) {
            inv = new Inventory();
            inv.setContractNo(receive.getContractNo());
            inv.setRefId(receive.getNewId());
            inv.setStage(FabricStage.DYED.name());
            inv.setQuality(receive.getQuality());
            inv.setColor(receive.getColor());
            inv.setAvailableKg(netKg);
            inv.setAvailableMeters(0.0);
            inv.setAvailableRolls(0);
        } else {
            inv.setAvailableKg((inv.getAvailableKg() == null ? 0.0 : inv.getAvailableKg()) + netKg);
        }
        inventoryRepo.save(inv);

        FabricMovement m = new FabricMovement();
        m.setRefId(receive.getNewId());
        m.setQuality(receive.getQuality());
        m.setQuantityKg(netKg);
        m.setType(MovementType.RECEIVED_DYED);
        m.setFromStage(FabricStage.DYEING);
        m.setToStage(FabricStage.DYED);
        m.setDated(LocalDate.now());
        movementRepo.save(m);

        DyedReceive saved = dyedRepo.save(receive);
        audit.logCreate("DyedReceive", String.valueOf(saved.getId()), saved.getNewId(),
                saved, "Dyed receipt " + saved.getNewId() + " for " + saved.getQuality());
        return saved;
    }
}

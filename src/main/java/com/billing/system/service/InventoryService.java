package com.billing.system.service;

import com.billing.system.entity.Inventory;
import com.billing.system.enums.FabricStage;
import com.billing.system.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepo;

    public InventoryService(InventoryRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
    }

    @Transactional
    public void issueStock(String contractNo, String quality, String color, Double qtyKg) {
        require(contractNo, "contractNo");
        require(quality, "quality");
        if (qtyKg == null || qtyKg <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }
        String c = (color == null || color.isEmpty()) ? "NA" : color;

        Inventory inv = inventoryRepo
                .findByContractNoAndQualityAndColorAndStage(contractNo, quality, c, FabricStage.GREIGH.name())
                .orElseThrow(() -> new RuntimeException(
                        "No greige stock for contract " + contractNo + " — " + quality + " / " + c));

        if (inv.getAvailableKg() == null || inv.getAvailableKg() < qtyKg) {
            throw new RuntimeException("Not enough stock. Available: " + inv.getAvailableKg());
        }
        inv.setAvailableKg(inv.getAvailableKg() - qtyKg);
        inventoryRepo.save(inv);
    }

    private static void require(String s, String name) {
        if (s == null || s.isEmpty()) throw new RuntimeException(name + " is required");
    }
}

package com.billing.system.controller;

import com.billing.system.entity.Inventory;
import com.billing.system.repository.InventoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@CrossOrigin
public class InventoryController {

    private final InventoryRepository inventoryRepo;

    public InventoryController(InventoryRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
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
}

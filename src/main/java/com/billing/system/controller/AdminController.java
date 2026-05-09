package com.billing.system.controller;

import com.billing.system.repository.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-shot administrative endpoints.
 * /admin/snapshot — dumps the current DB state of every business entity.
 * Curl this once to archive existing data as text before audit logs start filling up.
 */
@RestController
@RequestMapping("/admin")
@CrossOrigin
public class AdminController {

    private final PermanentTableRepository permanentRepo;
    private final ContractRepository contractRepo;
    private final InwardGatePassRepository inwardRepo;
    private final OutwardGatePassRepository outwardRepo;
    private final InventoryRepository inventoryRepo;
    private final IssueToDyeingRepository issueRepo;
    private final DyedReceiveRepository dyedRepo;
    private final EntryRepository entryRepo;
    private final FabricMovementRepository movementRepo;
    private final OrderRepository orderRepo;

    public AdminController(PermanentTableRepository permanentRepo,
                           ContractRepository contractRepo,
                           InwardGatePassRepository inwardRepo,
                           OutwardGatePassRepository outwardRepo,
                           InventoryRepository inventoryRepo,
                           IssueToDyeingRepository issueRepo,
                           DyedReceiveRepository dyedRepo,
                           EntryRepository entryRepo,
                           FabricMovementRepository movementRepo,
                           OrderRepository orderRepo) {
        this.permanentRepo = permanentRepo;
        this.contractRepo = contractRepo;
        this.inwardRepo = inwardRepo;
        this.outwardRepo = outwardRepo;
        this.inventoryRepo = inventoryRepo;
        this.issueRepo = issueRepo;
        this.dyedRepo = dyedRepo;
        this.entryRepo = entryRepo;
        this.movementRepo = movementRepo;
        this.orderRepo = orderRepo;
    }

    @GetMapping("/snapshot")
    public Map<String, Object> snapshot() {
        Map<String, Object> dump = new LinkedHashMap<>();
        dump.put("snapshotAt", LocalDateTime.now().toString());
        dump.put("permanentTable", permanentRepo.findAll());
        dump.put("contracts", contractRepo.findAll());
        dump.put("inwards", inwardRepo.findAll());
        dump.put("outwards", outwardRepo.findAll());
        dump.put("inventory", inventoryRepo.findAll());
        dump.put("issuesToDyeing", issueRepo.findAll());
        dump.put("dyedReceives", dyedRepo.findAll());
        dump.put("entries", entryRepo.findAll());
        dump.put("fabricMovements", movementRepo.findAll());
        dump.put("orders", orderRepo.findAll());
        return dump;
    }
}

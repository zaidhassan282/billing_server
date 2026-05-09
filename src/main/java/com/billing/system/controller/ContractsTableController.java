package com.billing.system.controller;

import com.billing.system.entity.Contract;
import com.billing.system.repository.ContractRepository;
import com.billing.system.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint used by the ContractTable.js page (POST /contracts-table per row).
 * Stores Contract rows, same backing table as /contracts.
 */
@RestController
@CrossOrigin
@RequestMapping("/contracts-table")
public class ContractsTableController {

    private static final String ENTITY = "Contract";

    private final ContractRepository contractRepo;
    private final AuditService audit;

    public ContractsTableController(ContractRepository contractRepo, AuditService audit) {
        this.contractRepo = contractRepo;
        this.audit = audit;
    }

    @GetMapping
    public List<Contract> getAll() {
        return contractRepo.findAll();
    }

    @PostMapping
    public Contract save(@RequestBody Contract contract) {
        boolean isUpdate = contract.getId() != null;
        Object before = isUpdate
                ? audit.snapshot(contractRepo.findById(contract.getId()).orElse(null))
                : null;

        Contract saved = contractRepo.save(contract);
        String id = String.valueOf(saved.getId());
        String biz = saved.getContractNo();

        if (isUpdate && before != null) {
            audit.logUpdate(ENTITY, id, biz, before, saved,
                    "Contract " + biz + " updated (table)");
        } else {
            audit.logCreate(ENTITY, id, biz, saved,
                    "Contract " + biz + " created (table)");
        }
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Contract existing = contractRepo.findById(id).orElse(null);
        Object before = audit.snapshot(existing);
        contractRepo.deleteById(id);
        if (existing != null) {
            audit.logDelete(ENTITY, String.valueOf(id), existing.getContractNo(), before,
                    "Contract " + existing.getContractNo() + " deleted (table)");
        }
    }
}

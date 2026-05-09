package com.billing.system.controller;

import com.billing.system.entity.Contract;
import com.billing.system.entity.PermanentTable;
import com.billing.system.repository.ContractRepository;
import com.billing.system.repository.PermanentTableRepository;
import com.billing.system.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/contracts")
public class ContractController {

    private static final String ENTITY = "Contract";

    private final ContractRepository contractRepo;
    private final PermanentTableRepository permanentRepo;
    private final AuditService audit;

    public ContractController(ContractRepository contractRepo,
                              PermanentTableRepository permanentRepo,
                              AuditService audit) {
        this.contractRepo = contractRepo;
        this.permanentRepo = permanentRepo;
        this.audit = audit;
    }

    @GetMapping
    public List<Contract> getAll() {
        return contractRepo.findAll();
    }

    @GetMapping("/all")
    public List<Contract> getAllAlias() {
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
                    "Contract " + biz + " updated");
        } else {
            audit.logCreate(ENTITY, id, biz, saved,
                    "Contract " + biz + " created");
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
                    "Contract " + existing.getContractNo() + " deleted");
        }
    }

    @GetMapping("/autofill/name")
    public PermanentTable autofillByName(@RequestParam String name) {
        return permanentRepo.findByNameOfParty(name)
                .orElseThrow(() -> new RuntimeException("Party not found"));
    }

    @GetMapping("/autofill/code")
    public PermanentTable autofillByCode(@RequestParam String code) {
        return permanentRepo.findByPartyCodeContainingIgnoreCase(code)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Party not found"));
    }
}

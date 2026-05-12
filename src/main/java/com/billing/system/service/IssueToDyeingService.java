package com.billing.system.service;

import com.billing.system.dto.IssueRequest;
import com.billing.system.entity.Contract;
import com.billing.system.entity.FabricMovement;
import com.billing.system.entity.Inventory;
import com.billing.system.entity.IssueToDyeing;
import com.billing.system.entity.Order;
import com.billing.system.enums.FabricStage;
import com.billing.system.enums.MovementType;
import com.billing.system.enums.OrderStatus;
import com.billing.system.repository.ContractRepository;
import com.billing.system.repository.FabricMovementRepository;
import com.billing.system.repository.InventoryRepository;
import com.billing.system.repository.IssueToDyeingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class IssueToDyeingService {

    private final InventoryRepository inventoryRepo;
    private final IssueToDyeingRepository issueRepo;
    private final FabricMovementRepository movementRepo;
    private final ContractRepository contractRepo;
    private final OrderService orderService;
    private final AuditService audit;

    public IssueToDyeingService(InventoryRepository inventoryRepo,
                                IssueToDyeingRepository issueRepo,
                                FabricMovementRepository movementRepo,
                                ContractRepository contractRepo,
                                OrderService orderService,
                                AuditService audit) {
        this.inventoryRepo = inventoryRepo;
        this.issueRepo = issueRepo;
        this.movementRepo = movementRepo;
        this.contractRepo = contractRepo;
        this.orderService = orderService;
        this.audit = audit;
    }

    @Transactional
    public IssueToDyeing issue(IssueRequest req) {

        String contractNo = req.getContractNo();
        if (contractNo == null || contractNo.isEmpty()) {
            throw new RuntimeException("Contract No is required");
        }
        if (req.getQuality() == null || req.getQuality().isEmpty()) {
            throw new RuntimeException("Quality is required");
        }

        double qtyKg = req.getQtyKg() == null ? 0.0 : req.getQtyKg();
        int qtyRolls = req.getQtyRolls() == null ? 0 : req.getQtyRolls();
        double qtyMeters = req.getQtyMeters() == null ? 0.0 : req.getQtyMeters();

        if (qtyKg <= 0 && qtyRolls <= 0 && qtyMeters <= 0) {
            throw new RuntimeException("At least one of weight, rolls, or meters must be greater than 0");
        }

        String color = (req.getColor() == null || req.getColor().isEmpty()) ? "NA" : req.getColor();

        Inventory inv = inventoryRepo
                .findByContractNoAndQualityAndColorAndStage(contractNo, req.getQuality(), color, FabricStage.GREIGH.name())
                .orElseThrow(() -> new RuntimeException(
                        "No greige stock for contract " + contractNo
                                + " — " + req.getQuality() + " / " + color));

        double availKg = inv.getAvailableKg() == null ? 0.0 : inv.getAvailableKg();
        double availM  = inv.getAvailableMeters() == null ? 0.0 : inv.getAvailableMeters();
        int    availR  = inv.getAvailableRolls()  == null ? 0   : inv.getAvailableRolls();

        if (qtyKg > 0 && availKg < qtyKg) {
            throw new RuntimeException("Not enough kg (avail " + availKg + ", need " + qtyKg + ")");
        }
        if (qtyRolls > 0 && availR < qtyRolls) {
            throw new RuntimeException("Not enough rolls (avail " + availR + ", need " + qtyRolls + ")");
        }
        if (qtyMeters > 0 && availM < qtyMeters) {
            throw new RuntimeException("Not enough meters (avail " + availM + ", need " + qtyMeters + ")");
        }

        inv.setAvailableKg(Math.max(0.0, availKg - qtyKg));
        inv.setAvailableRolls(Math.max(0, availR - qtyRolls));
        inv.setAvailableMeters(Math.max(0.0, availM - qtyMeters));
        inventoryRepo.save(inv);

        IssueToDyeing record = new IssueToDyeing();
        record.setIssueId(generateIssueId());
        record.setContractNo(contractNo);
        record.setInwardId(req.resolveInwardRef());
        record.setQuality(req.getQuality());
        record.setColor(color);
        record.setQuantityKg(qtyKg);
        record.setQuantityRolls(qtyRolls);
        record.setQuantityMeters(qtyMeters);
        record.setDate(LocalDate.now());
        record.setRemarks(req.getRemarks());

        IssueToDyeing saved = issueRepo.save(record);
        audit.logCreate("IssueToDyeing", String.valueOf(saved.getId()), saved.getIssueId(),
                saved, "Issue " + saved.getIssueId() + " of " + qtyKg + " kg "
                        + req.getQuality() + " for contract " + contractNo);

        FabricMovement m = new FabricMovement();
        m.setRefId(saved.getIssueId());
        m.setQuality(req.getQuality());
        m.setQuantityKg(qtyKg);
        m.setType(MovementType.ISSUE_TO_DYEING);
        m.setFromStage(FabricStage.GREIGH);
        m.setToStage(FabricStage.DYEING);
        m.setDated(LocalDate.now());
        movementRepo.save(m);

        // Auto-create an Order in IN_PROGRESS state for tracking the dyed-output workflow.
        try {
            Contract contract = contractRepo.findByContractNo(contractNo);
            Order order = new Order();
            order.setContractNo(contractNo);
            if (contract != null) {
                order.setPartyCode(contract.getPartyCode());
                order.setNameOfParty(contract.getNameOfParty());
            }
            order.setQuality(req.getQuality());
            order.setColor(color);
            order.setQuantityKg(qtyKg);
            order.setQuantityRolls(qtyRolls);
            order.setQuantityMeters(qtyMeters);
            order.setStatus(OrderStatus.IN_PROGRESS);
            order.setRemarks("Auto-created from Issue " + saved.getIssueId()
                    + (req.getRemarks() != null && !req.getRemarks().isEmpty()
                        ? " · " + req.getRemarks() : ""));
            orderService.create(order);
        } catch (Exception e) {
            // Order creation failure should not block the issue itself.
            audit.logCreate("Order", null, null, null,
                    "Order auto-create failed for issue " + saved.getIssueId() + ": " + e.getMessage());
        }

        return saved;
    }

    private String generateIssueId() {
        String yy = String.valueOf(LocalDate.now().getYear()).substring(2);
        String prefix = "ITD" + yy;
        int max = issueRepo.findAll().stream()
                .map(IssueToDyeing::getIssueId)
                .filter(id -> id != null && id.startsWith(prefix))
                .map(id -> {
                    try { return Integer.parseInt(id.substring(prefix.length())); }
                    catch (Exception e) { return 0; }
                })
                .max(Integer::compareTo)
                .orElse(0);
        return prefix + String.format("%03d", max + 1);
    }

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "date");

    public List<IssueToDyeing> getAll() {
        return issueRepo.findAll(NEWEST_FIRST);
    }

    public List<IssueToDyeing> getByContract(String contractNo) {
        return issueRepo.findByContractNo(contractNo, NEWEST_FIRST);
    }
}

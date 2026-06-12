package com.billing.system.service;

import com.billing.system.entity.Order;
import com.billing.system.enums.OrderStatus;
import com.billing.system.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final String ENTITY = "Order";

    private final OrderRepository repo;
    private final AuditService audit;

    public OrderService(OrderRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    @Transactional
    public Order create(Order order) {
        if (order.getContractNo() == null || order.getContractNo().isEmpty()) {
            throw new RuntimeException("Contract No is required");
        }
        if (order.getQuality() == null || order.getQuality().isEmpty()) {
            throw new RuntimeException("Quality is required");
        }
        if (order.getStatus() == null) order.setStatus(OrderStatus.IN_PROGRESS);
        if (order.getColor() == null || order.getColor().isEmpty()) order.setColor("NA");
        if (order.getCreatedAt() == null) order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setOrderId(generateOrderId());

        Order saved = repo.save(order);
        audit.logCreate(ENTITY, String.valueOf(saved.getId()), saved.getOrderId(),
                saved, "Order " + saved.getOrderId() + " created");
        return saved;
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus, String remarks) {
        Order existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        Object before = audit.snapshot(existing);

        existing.setStatus(newStatus);
        if (remarks != null) existing.setRemarks(remarks);
        existing.setUpdatedAt(LocalDateTime.now());
        Order saved = repo.save(existing);

        audit.logUpdate(ENTITY, String.valueOf(saved.getId()), saved.getOrderId(),
                before, saved,
                "Order " + saved.getOrderId() + " status → " + newStatus);
        return saved;
    }

    @Transactional
    public Order markDelivered(Long id, String outwardId) {
        Order existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        Object before = audit.snapshot(existing);

        existing.setStatus(OrderStatus.DELIVERED);
        existing.setLinkedOutwardId(outwardId);
        existing.setUpdatedAt(LocalDateTime.now());
        Order saved = repo.save(existing);

        audit.logUpdate(ENTITY, String.valueOf(saved.getId()), saved.getOrderId(),
                before, saved,
                "Order " + saved.getOrderId() + " delivered via " + outwardId);
        return saved;
    }

    @Transactional
    public Order update(Long id, Order patch) {
        Order existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        Object before = audit.snapshot(existing);

        if (patch.getQuality() != null) existing.setQuality(patch.getQuality());
        if (patch.getColor() != null) existing.setColor(patch.getColor());
        if (patch.getQuantityKg() != null) existing.setQuantityKg(patch.getQuantityKg());
        if (patch.getQuantityRolls() != null) existing.setQuantityRolls(patch.getQuantityRolls());
        if (patch.getQuantityMeters() != null) existing.setQuantityMeters(patch.getQuantityMeters());
        if (patch.getRemarks() != null) existing.setRemarks(patch.getRemarks());
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        Order saved = repo.save(existing);

        audit.logUpdate(ENTITY, String.valueOf(saved.getId()), saved.getOrderId(),
                before, saved, "Order " + saved.getOrderId() + " updated");
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Order existing = repo.findById(id).orElse(null);
        if (existing == null) return;
        Object before = audit.snapshot(existing);
        repo.deleteById(id);
        audit.logDelete(ENTITY, String.valueOf(id), existing.getOrderId(), before,
                "Order " + existing.getOrderId() + " deleted");
    }

    private String generateOrderId() {
        String yy = String.valueOf(LocalDateTime.now().getYear()).substring(2);
        String prefix = "ORD" + yy;
        // PERF-3 + P2-5 — see InwardService.generateInwardId for the rationale.
        int max = repo.findFirstByOrderIdStartingWithOrderByOrderIdDesc(prefix)
                .map(Order::getOrderId)
                .map(o -> {
                    try { return Integer.parseInt(o.substring(prefix.length())); }
                    catch (Exception e) { return 0; }
                })
                .orElse(0);
        return prefix + String.format("%03d", max + 1);
    }

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    public List<Order> getAll() { return repo.findAll(NEWEST_FIRST); }
    public List<Order> byStatus(OrderStatus status) { return repo.findByStatus(status, NEWEST_FIRST); }
    public List<Order> byContract(String contractNo) { return repo.findByContractNo(contractNo, NEWEST_FIRST); }
}

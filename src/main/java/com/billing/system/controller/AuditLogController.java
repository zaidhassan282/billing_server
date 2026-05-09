package com.billing.system.controller;

import com.billing.system.entity.AuditLog;
import com.billing.system.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit")
@CrossOrigin
public class AuditLogController {

    private static final int MAX_PAGE_SIZE = 200;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "changedAt");

    private final AuditLogRepository repo;

    public AuditLogController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Page<AuditLog> list(@RequestParam(required = false) String entityType,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(0, page), safeSize, NEWEST_FIRST);
        if (entityType != null && !entityType.isEmpty()) {
            return repo.findByEntityType(entityType, pageable);
        }
        return repo.findAll(pageable);
    }

    @GetMapping("/{id}")
    public AuditLog getOne(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("AuditLog not found: " + id));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public List<AuditLog> historyOf(@PathVariable String entityType, @PathVariable String entityId) {
        return repo.findByEntityTypeAndEntityId(entityType, entityId);
    }
}

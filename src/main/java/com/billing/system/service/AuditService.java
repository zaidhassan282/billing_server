package com.billing.system.service;

import com.billing.system.entity.AuditLog;
import com.billing.system.repository.AuditLogRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String DEFAULT_USER = "system";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final AuditLogRepository repo;
    private final ObjectMapper mapper;

    public AuditService(AuditLogRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    /**
     * Freeze an entity's current state into a Map so callers can capture "before"
     * before calling repo.save(...). Returns null if entity is null.
     */
    public Map<String, Object> snapshot(Object entity) {
        if (entity == null) return null;
        try {
            return mapper.convertValue(entity, MAP_TYPE);
        } catch (Exception e) {
            log.warn("AuditService.snapshot failed: {}", e.getMessage());
            return null;
        }
    }

    public void logCreate(String entityType, String entityId, String businessId,
                          Object state, String summary) {
        try {
            AuditLog row = base(entityType, entityId, businessId, "CREATE", summary);
            row.setSnapshot(toJson(state));
            repo.save(row);
        } catch (Exception e) {
            log.warn("AuditService.logCreate failed for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    public void logUpdate(String entityType, String entityId, String businessId,
                          Object before, Object after, String summary) {
        try {
            AuditLog row = base(entityType, entityId, businessId, "UPDATE", summary);
            row.setSnapshot(toJson(after));
            row.setChanges(toJson(diff(before, after)));
            repo.save(row);
        } catch (Exception e) {
            log.warn("AuditService.logUpdate failed for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    public void logDelete(String entityType, String entityId, String businessId,
                          Object state, String summary) {
        try {
            AuditLog row = base(entityType, entityId, businessId, "DELETE", summary);
            row.setSnapshot(toJson(state));
            repo.save(row);
        } catch (Exception e) {
            log.warn("AuditService.logDelete failed for {} {}: {}", entityType, entityId, e.getMessage());
        }
    }

    private AuditLog base(String entityType, String entityId, String businessId,
                          String action, String summary) {
        AuditLog row = new AuditLog();
        row.setEntityType(entityType);
        row.setEntityId(entityId);
        row.setBusinessId(businessId);
        row.setAction(action);
        row.setChangedBy(DEFAULT_USER);
        row.setChangedAt(LocalDateTime.now());
        row.setSummary(truncate(summary, 512));
        return row;
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("AuditService.toJson failed: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value == null) return Collections.emptyMap();
        try {
            return mapper.convertValue(value, MAP_TYPE);
        } catch (Exception e) {
            log.warn("AuditService.asMap failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<Map<String, Object>> diff(Object before, Object after) {
        Map<String, Object> b = asMap(before);
        Map<String, Object> a = asMap(after);

        Set<String> keys = new TreeSet<>();
        keys.addAll(b.keySet());
        keys.addAll(a.keySet());

        List<Map<String, Object>> changes = new ArrayList<>();
        for (String key : keys) {
            Object oldV = b.get(key);
            Object newV = a.get(key);
            if (!Objects.equals(oldV, newV)) {
                Map<String, Object> change = new LinkedHashMap<>();
                change.put("field", key);
                change.put("before", oldV);
                change.put("after", newV);
                changes.add(change);
            }
        }
        return changes;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}

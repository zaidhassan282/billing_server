package com.billing.system.entity;

import com.billing.system.enums.FabricStage;
import com.billing.system.enums.MovementType;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class FabricMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Defaults to 1; P2-4 overrides via
     *  TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    private String refId;
    private String quality;
    private Double quantityKg;

    @Enumerated(EnumType.STRING)
    private MovementType type;

    @Enumerated(EnumType.STRING)
    private FabricStage fromStage;

    @Enumerated(EnumType.STRING)
    private FabricStage toStage;

    private LocalDate dated;

    // ✅ GETTERS & SETTERS

    public Long getId() {
        return id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public Double getQuantityKg() {
        return quantityKg;
    }

    public void setQuantityKg(Double quantityKg) {
        this.quantityKg = quantityKg;
    }

    public MovementType getType() {
        return type;
    }

    public void setType(MovementType type) {
        this.type = type;
    }

    public FabricStage getFromStage() {
        return fromStage;
    }

    public void setFromStage(FabricStage fromStage) {
        this.fromStage = fromStage;
    }

    public FabricStage getToStage() {
        return toStage;
    }

    public void setToStage(FabricStage toStage) {
        this.toStage = toStage;
    }

    public LocalDate getDated() {
        return dated;
    }

    public void setDated(LocalDate dated) {
        this.dated = dated;
    }
}
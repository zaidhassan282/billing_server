package com.billing.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class InwardItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-tenant scope (Phase 2). Denormalised from the parent gate
     *  pass for cheap query filtering. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId;

    // --- Existing item fields (still saved, can be filled or left blank) ---
    private String quality;
    private String color;
    private String design;
    private String article;

    /** Existing rolls count. UI now labels this as "Total Roll". */
    private Integer roll;
    /** Existing kg field — falls back here if totalWeight is not provided. */
    private Double kg;
    private Double meters;
    private String remarks;

    // --- New fields (per latest spec) ---
    /** Per-row lot number, e.g. customer's lot or batch id. */
    private String lotNo;
    /** Plain fabric weight (kg). */
    private Double fabricWeight;
    /** Rib weight (kg) — usually a smaller portion of the roll. */
    private Double ribWeight;
    /** T.D. W.t — total dispatched / dyed weight (kg). */
    private Double tdWeight;
    /** Authoritative weight (kg) that drives inventory. */
    private Double totalWeight;
    /** Process applied (e.g. mercerising, singeing, brushing). */
    private String process;
    /** Process Lot # — internal lot id after processing. */
    private String pLotNo;
    /** Size or GSM specification. */
    private String sizeGsm;

    /**
     * Fabric type for this item (free text — e.g. "Cotton 60×60", "Polyester").
     * Distinct from the header-level fabricType (Inward Type) on InwardGatePass.
     */
    private String fabricType;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "inward_id")
    private InwardGatePass inward;
}

package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
public class DyedReceive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dated;

    // Source inward gate-pass reference (optional)
    private String inwardId;

    private String contractNo;

    private String partyCode;

    private String nameOfParty;

    private String customerLotNo;

    private String factoryLotNo;

    private String quality;

    private String color;

    private Double quantityKg;

    private Double cutPiecesKg;

    private Double shrinkage;

    /** Foreign key to the source Issue to Dyeing — Dyed Receive is the completed-work pool for that issue. */
    private Long issueToDyeingId;

    /** Denormalised business id of the linked Issue (e.g. "ITD26001") for display & easy joins. */
    @Column(length = 32)
    private String issueId;

    /** Net kg available for delivery from this receipt (= quantityKg − cut − shrinkage, minus what's been delivered). */
    private Double availableKg;

    @Column(unique = true)
    private String newId;
}

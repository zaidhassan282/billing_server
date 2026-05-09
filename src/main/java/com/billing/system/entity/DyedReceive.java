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

    @Column(unique = true)
    private String newId;
}

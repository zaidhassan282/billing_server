package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory", indexes = {
        @Index(name = "idx_inv_lookup",
               columnList = "contractNo, quality, color, stage", unique = true)
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Contract this stock belongs to. Stock is scoped per contract. */
    @Column(nullable = false, length = 64)
    private String contractNo;

    /** Origin doc id (e.g. inwardId or dyedReceive id) — informational. */
    private String refId;

    /** GREIGH or DYED. */
    @Column(nullable = false, length = 16)
    private String stage;

    @Column(nullable = false)
    private String quality;

    @Column(nullable = false)
    private String color;

    private Double availableKg;
    private Double availableMeters;
    private Integer availableRolls;
}

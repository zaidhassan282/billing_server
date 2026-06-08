package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
public class IssueToDyeing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 32)
    private String issueId; // ITD26001

    private String contractNo;

    private String inwardId;

    private String quality;

    private String color;

    private Double quantityKg;

    private Integer quantityRolls;

    private Double quantityMeters;

    private LocalDate date;

    /** Stock the issue was drawn from: GREIGH (first-time dyeing) or DYED (re-dyeing). */
    @Column(length = 16)
    private String sourceStage;

    @Column(length = 512)
    private String remarks;
}

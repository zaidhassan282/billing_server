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

    private String quality;
    private String color;
    private String design;
    private String article;

    private Integer roll;
    private Double kg;
    private Double meters;
    private String remarks;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "inward_id")
    private InwardGatePass inward;
}

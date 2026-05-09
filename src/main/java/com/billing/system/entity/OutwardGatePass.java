package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Entity
public class OutwardGatePass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String outwardId; // OGP-xxxxxx

    private LocalDate dated;

    // Reference to source inward (optional)
    private String inwardId;

    private String contractNo;
    private String customerCode;
    private String customerName;

    // Lot tracking
    private String customerLotNo;
    private String factoryLotNo;

    // ISSUE / RETURN
    private String type;

    // Logistics
    private String address;
    private String vehicleNo;
    private String driverName;
    private String referenceNo;

    private String fabricType;

    // Security / gate metadata
    private String gateTime;
    private String securityGuardName;
    private String checkedBy;

    @OneToMany(mappedBy = "outward", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutwardItem> items;
}

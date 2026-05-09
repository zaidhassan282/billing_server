package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permanent_table",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "name_of_party"),
                @UniqueConstraint(columnNames = "ntn")
        })
public class PermanentTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partyCode;

    @Column(name = "name_of_party")
    private String nameOfParty;

    private String invoiceAddress;

    @Column(name = "ntn")
    private String ntn;

    private String gstInvoice;

    private String deliveryAddress1;
    private String deliveryAddress2;
    private String deliveryAddress3;
}

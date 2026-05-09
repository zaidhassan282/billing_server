package com.billing.system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
@Entity
@Table(name = "permanent_party",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "name_of_party"),
                @UniqueConstraint(columnNames = "ntn")
        })
public class PermanentParty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partyCode;
    private String nameOfParty;
    private String invoiceAddress;
    private String ntn;
    private String gstInvoice;
    private String deliveryAddress1;
    private String deliveryAddress2;
    private String deliveryAddress3;

    // getters + setters
}
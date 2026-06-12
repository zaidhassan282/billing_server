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

    /** Per-tenant scope (Phase 2). Defaults to 1 so single-tenant deploys
     *  keep working; P2-4 overrides via TenantContext from the JWT. */
    @org.hibernate.annotations.TenantId
    @Column(name = "tenant_id")
    private Long tenantId = 1L;

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

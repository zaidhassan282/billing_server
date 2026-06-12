package com.billing.system.repository;

import com.billing.system.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOutwardGatePassId(Long outwardGatePassId);

    /** Single-row lookup for the next-id generator (replaces findAll().stream()). */
    Optional<Invoice> findFirstByInvoiceNoStartingWithOrderByInvoiceNoDesc(String prefix);
}

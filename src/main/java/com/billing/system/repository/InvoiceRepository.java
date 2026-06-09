package com.billing.system.repository;

import com.billing.system.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByOutwardGatePassId(Long outwardGatePassId);
}

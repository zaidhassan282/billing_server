package com.billing.system.repository;

import com.billing.system.entity.InwardGatePass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InwardGatePassRepository extends JpaRepository<InwardGatePass, Long> {

    Optional<InwardGatePass> findByInwardId(String inwardId);

    /** Single-row lookup for the next-id generator (replaces findAll().stream()). */
    Optional<InwardGatePass> findFirstByInwardIdStartingWithOrderByInwardIdDesc(String prefix);
}

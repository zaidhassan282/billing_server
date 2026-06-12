package com.billing.system.repository;

import com.billing.system.entity.DyedReceive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DyedReceiveRepository extends JpaRepository<DyedReceive, Long> {

    List<DyedReceive> findByIssueToDyeingId(Long issueToDyeingId);

    /** Single-row lookup for the next-id generator (replaces findAll().stream()). */
    Optional<DyedReceive> findFirstByNewIdStartingWithOrderByNewIdDesc(String prefix);
}
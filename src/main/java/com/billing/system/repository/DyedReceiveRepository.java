package com.billing.system.repository;

import com.billing.system.entity.DyedReceive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DyedReceiveRepository extends JpaRepository<DyedReceive, Long> {

    List<DyedReceive> findByIssueToDyeingId(Long issueToDyeingId);
}
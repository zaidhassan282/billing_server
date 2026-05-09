package com.billing.system.repository;

import com.billing.system.entity.OutwardGatePass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutwardGatePassRepository extends JpaRepository<OutwardGatePass, Long> {

    Optional<OutwardGatePass> findByOutwardId(String outwardId);
}

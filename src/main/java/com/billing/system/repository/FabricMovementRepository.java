package com.billing.system.repository;

import com.billing.system.entity.FabricMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FabricMovementRepository extends JpaRepository<FabricMovement, Long> {
}
package com.billing.system.repository;

import com.billing.system.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByContractNoAndQualityAndColorAndStage(
            String contractNo, String quality, String color, String stage);

    List<Inventory> findByContractNoAndStage(String contractNo, String stage);

    List<Inventory> findByContractNo(String contractNo);

    List<Inventory> findByStage(String stage);
}

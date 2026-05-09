package com.billing.system.repository;

import com.billing.system.entity.IssueToDyeing;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueToDyeingRepository extends JpaRepository<IssueToDyeing, Long> {
    List<IssueToDyeing> findByContractNo(String contractNo, Sort sort);
}

package com.billing.system.repository;

import com.billing.system.entity.IssueToDyeing;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueToDyeingRepository extends JpaRepository<IssueToDyeing, Long> {
    List<IssueToDyeing> findByContractNo(String contractNo, Sort sort);

    /** Single-row lookup for the next-id generator (replaces findAll().stream()). */
    Optional<IssueToDyeing> findFirstByIssueIdStartingWithOrderByIssueIdDesc(String prefix);
}

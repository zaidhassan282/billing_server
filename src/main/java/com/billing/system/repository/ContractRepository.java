package com.billing.system.repository;

import com.billing.system.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Contract findByContractNo(String contractNo);

    /** Single-row lookup for the next-id generator (replaces findAll().stream()). */
    Optional<Contract> findFirstByContractNoStartingWithOrderByContractNoDesc(String prefix);
}

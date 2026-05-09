package com.billing.system.repository;

import com.billing.system.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Contract findByContractNo(String contractNo);

}
package com.billing.system.repository;

import com.billing.system.entity.PermanentTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermanentTableRepository extends JpaRepository<PermanentTable, Long> {

    Optional<PermanentTable> findByNameOfParty(String nameOfParty);

    Optional<PermanentTable> findByNtn(String ntn);

    List<PermanentTable> findByNameOfPartyContainingIgnoreCase(String name);

    List<PermanentTable> findByPartyCodeContainingIgnoreCase(String code);
}

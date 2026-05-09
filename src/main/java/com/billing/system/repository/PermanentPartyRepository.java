package com.billing.system.repository;

import com.billing.system.entity.PermanentParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermanentPartyRepository extends JpaRepository<PermanentParty, Long> {

    List<PermanentParty> findByNameOfPartyContainingIgnoreCaseOrPartyCodeContainingIgnoreCase(
            String name,
            String code

    );
}
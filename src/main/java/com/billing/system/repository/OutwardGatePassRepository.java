package com.billing.system.repository;

import com.billing.system.entity.OutwardGatePass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutwardGatePassRepository extends JpaRepository<OutwardGatePass, Long> {

    Optional<OutwardGatePass> findByOutwardId(String outwardId);

    /** All OGPs that draw from the given Dyed Receive — used for the capacity check. */
    List<OutwardGatePass> findByDyedReceiveId(Long dyedReceiveId);
}

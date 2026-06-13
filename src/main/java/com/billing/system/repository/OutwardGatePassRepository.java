package com.billing.system.repository;

import com.billing.system.entity.OutwardGatePass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OutwardGatePassRepository extends JpaRepository<OutwardGatePass, Long> {

    Optional<OutwardGatePass> findByOutwardId(String outwardId);

    /** All OGPs that draw from the given Dyed Receive — used for the capacity check. */
    List<OutwardGatePass> findByDyedReceiveId(Long dyedReceiveId);

    /** Single-row lookup for the next-id generator (replaces findAll().stream()). */
    Optional<OutwardGatePass> findFirstByOutwardIdStartingWithOrderByOutwardIdDesc(String prefix);

    /**
     * Batch fetch with items already loaded — used by InvoiceService.getAll
     * to break the N+1 (PERF-2). One query for the OGPs + their items
     * instead of one per invoice.
     */
    @Query("SELECT DISTINCT o FROM OutwardGatePass o LEFT JOIN FETCH o.items WHERE o.id IN :ids")
    List<OutwardGatePass> findAllWithItemsByIdIn(@Param("ids") Collection<Long> ids);
}

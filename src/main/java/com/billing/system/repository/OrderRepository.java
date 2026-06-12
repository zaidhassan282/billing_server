package com.billing.system.repository;

import com.billing.system.entity.Order;
import com.billing.system.enums.OrderStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderId(String orderId);
    List<Order> findByStatus(OrderStatus status, Sort sort);
    List<Order> findByContractNo(String contractNo, Sort sort);

    /** Single-row lookup for the next-id generator (replaces findAll().stream()). */
    Optional<Order> findFirstByOrderIdStartingWithOrderByOrderIdDesc(String prefix);
}

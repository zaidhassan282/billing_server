package com.billing.system.controller;

import com.billing.system.entity.Order;
import com.billing.system.enums.OrderStatus;
import com.billing.system.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@CrossOrigin
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<Order> all(@RequestParam(required = false) String status,
                           @RequestParam(required = false) String contractNo) {
        if (status != null && !status.isEmpty()) {
            return service.byStatus(OrderStatus.valueOf(status));
        }
        if (contractNo != null && !contractNo.isEmpty()) {
            return service.byContract(contractNo);
        }
        return service.getAll();
    }

    @PostMapping
    public Order create(@RequestBody Order order) {
        return service.create(order);
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable Long id, @RequestBody Order patch) {
        return service.update(id, patch);
    }

    @PostMapping("/{id}/status")
    public Order changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        OrderStatus status = OrderStatus.valueOf(body.get("status"));
        return service.updateStatus(id, status, body.get("remarks"));
    }

    @PostMapping("/{id}/deliver")
    public Order markDelivered(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.markDelivered(id, body.get("outwardId"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

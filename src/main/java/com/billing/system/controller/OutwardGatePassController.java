package com.billing.system.controller;

import com.billing.system.entity.OutwardGatePass;
import com.billing.system.service.OutwardGatePassService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/outward")
@CrossOrigin
public class OutwardGatePassController {

    private final OutwardGatePassService outwardService;

    public OutwardGatePassController(OutwardGatePassService outwardService) {
        this.outwardService = outwardService;
    }

    @PostMapping
    public OutwardGatePass save(@RequestBody OutwardGatePass outward) {
        return outwardService.save(outward);
    }

    @PostMapping("/save")
    public OutwardGatePass saveAlias(@RequestBody OutwardGatePass outward) {
        return outwardService.save(outward);
    }

    @PostMapping("/issue")
    public OutwardGatePass issue(@RequestBody OutwardGatePass outward) {
        return outwardService.issue(outward);
    }

    @PostMapping("/return")
    public OutwardGatePass returnFromCustomer(@RequestBody OutwardGatePass outward) {
        return outwardService.returnFromCustomer(outward);
    }

    @GetMapping
    public List<OutwardGatePass> getAll() {
        return outwardService.getAll();
    }

    @GetMapping("/{outwardId}")
    public OutwardGatePass getOne(@PathVariable String outwardId) {
        return outwardService.getByOutwardId(outwardId);
    }
}

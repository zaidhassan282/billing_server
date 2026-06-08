package com.billing.system.controller;

import com.billing.system.entity.InwardGatePass;
import com.billing.system.service.InwardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inward")
@CrossOrigin
public class InwardController {

    private final InwardService inwardService;

    public InwardController(InwardService inwardService) {
        this.inwardService = inwardService;
    }

    @PostMapping
    public InwardGatePass save(@RequestBody InwardGatePass inward) {
        return inwardService.save(inward);
    }

    // Alias used by GatePass.js
    @PostMapping("/save")
    public InwardGatePass saveAlias(@RequestBody InwardGatePass inward) {
        return inwardService.save(inward);
    }

    @GetMapping
    public List<InwardGatePass> getAll() {
        return inwardService.getAll();
    }

    @GetMapping("/{inwardId}")
    public InwardGatePass getOne(@PathVariable String inwardId) {
        return inwardService.getByInwardId(inwardId);
    }

    @PutMapping("/{id}")
    public InwardGatePass update(@PathVariable Long id, @RequestBody InwardGatePass inward) {
        return inwardService.update(id, inward);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        inwardService.delete(id);
    }
}

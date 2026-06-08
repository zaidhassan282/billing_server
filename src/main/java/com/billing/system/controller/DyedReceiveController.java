package com.billing.system.controller;

import com.billing.system.entity.DyedReceive;
import com.billing.system.service.DyedReceiveService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dyed")
@CrossOrigin
public class DyedReceiveController {

    private final DyedReceiveService service;

    public DyedReceiveController(DyedReceiveService service) {
        this.service = service;
    }

    @PostMapping("/receive")
    public DyedReceive receive(@RequestBody DyedReceive receive) {
        return service.save(receive);
    }

    @GetMapping
    public List<DyedReceive> getAll() {
        return service.getAll();
    }

    @GetMapping("/by-issue/{issueId}")
    public List<DyedReceive> getByIssue(@PathVariable Long issueId) {
        return service.getByIssue(issueId);
    }

    @PutMapping("/{id}")
    public DyedReceive update(@PathVariable Long id, @RequestBody DyedReceive patch) {
        return service.update(id, patch);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

package com.billing.system.controller;

import com.billing.system.dto.IssueRequest;
import com.billing.system.entity.IssueToDyeing;
import com.billing.system.service.IssueToDyeingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dyeing")
@CrossOrigin
public class DyeingController {

    private final IssueToDyeingService service;

    public DyeingController(IssueToDyeingService service) {
        this.service = service;
    }

    @PostMapping("/issue")
    public IssueToDyeing issue(@RequestBody IssueRequest req) {
        return service.issue(req);
    }

    @GetMapping("/issues")
    public List<IssueToDyeing> getAll() {
        return service.getAll();
    }

    @GetMapping("/issues/by-contract/{contractNo}")
    public List<IssueToDyeing> getByContract(@PathVariable String contractNo) {
        return service.getByContract(contractNo);
    }

    @PutMapping("/issue/{id}")
    public IssueToDyeing update(@PathVariable Long id, @RequestBody IssueRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/issue/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

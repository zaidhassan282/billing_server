package com.billing.system.controller;

import com.billing.system.entity.DyedReceive;
import com.billing.system.repository.DyedReceiveRepository;
import com.billing.system.service.DyedReceiveService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dyed")
@CrossOrigin
public class DyedReceiveController {

    private final DyedReceiveService service;
    private final DyedReceiveRepository repo;

    public DyedReceiveController(DyedReceiveService service, DyedReceiveRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @PostMapping("/receive")
    public DyedReceive receive(@RequestBody DyedReceive receive) {
        return service.save(receive);
    }

    @GetMapping
    public List<DyedReceive> getAll() {
        return repo.findAll();
    }
}

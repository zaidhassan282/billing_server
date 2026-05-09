package com.billing.system.controller;

import com.billing.system.entity.PermanentParty;
import com.billing.system.repository.PermanentPartyRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permanent")
@CrossOrigin
public class PermanentPartyController {

    private final PermanentPartyRepository repository;

    public PermanentPartyController(PermanentPartyRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/search")
    public List<PermanentParty> search(@RequestParam String query) {
        return repository.findByNameOfPartyContainingIgnoreCaseOrPartyCodeContainingIgnoreCase(query, query);
    }
}
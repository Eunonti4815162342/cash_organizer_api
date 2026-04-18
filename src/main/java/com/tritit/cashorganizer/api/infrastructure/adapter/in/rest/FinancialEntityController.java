package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.FinancialEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
public class FinancialEntityController {

    private final com.tritit.cashorganizer.api.application.FinancialEntityService service;

    @GetMapping
    public List<FinancialEntity> getAllEntities() {
        return service.getAllEntities();
    }

    @PostMapping
    public FinancialEntity createEntity(@RequestBody FinancialEntity entity) {
        return service.createEntity(entity);
    }

    @DeleteMapping("/{id}")
    public void deleteEntity(@PathVariable Long id) {
        service.deleteEntity(id);
    }
}

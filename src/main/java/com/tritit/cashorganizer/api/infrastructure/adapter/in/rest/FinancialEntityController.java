package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.FinancialEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entities")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FinancialEntityController {

    private final FinancialEntityRepository repository;

    @GetMapping
    public List<FinancialEntity> getAllEntities() {
        return repository.findAll();
    }

    @PostMapping
    public FinancialEntity createEntity(@RequestBody FinancialEntity entity) {
        return repository.save(entity);
    }

    @PutMapping("/{id}")
    public FinancialEntity updateEntity(@PathVariable Long id, @RequestBody FinancialEntity entity) {
        entity.setId(id);
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    public void deleteEntity(@PathVariable Long id) {
        repository.deleteById(id);
    }
}

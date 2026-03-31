package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.FinancialEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialEntityService {

    private final FinancialEntityRepository repository;

    public List<FinancialEntity> getAllEntities() {
        return repository.findAll();
    }

    @Transactional
    public FinancialEntity createEntity(FinancialEntity entity) {
        // Rule: Unique name per type
        boolean exists = repository.findAll().stream()
                .anyMatch(e -> e.getName().equalsIgnoreCase(entity.getName()) && e.getType() == entity.getType());
        
        if (exists) {
            throw new RuntimeException("An entity with this name and type already exists.");
        }
        return repository.save(entity);
    }

    @Transactional
    public void deleteEntity(Long id) {
        FinancialEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found"));

        // Rule: Cannot delete if it has accounts
        if (entity.getAccounts() != null && !entity.getAccounts().isEmpty()) {
            throw new RuntimeException("Cannot delete entity: It has associated accounts. Delete or reassign accounts first.");
        }

        repository.delete(entity);
    }
}

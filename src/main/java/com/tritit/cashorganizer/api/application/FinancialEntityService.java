package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.FinancialEntityPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialEntityService {

    private final FinancialEntityPersistencePort financialEntityPersistencePort;
    private final AccountPersistencePort accountPersistencePort;
    private final UserContextPort userContextPort;

    @Transactional(readOnly = true)
    public List<FinancialEntity> getAllEntities() {
        User user = userContextPort.getCurrentUser();
        return financialEntityPersistencePort.findAllByUser(user);
    }

    @Transactional
    public FinancialEntity createEntity(FinancialEntity entity) {
        User user = userContextPort.getCurrentUser();
        entity.setUser(user);
        return financialEntityPersistencePort.save(entity);
    }

    @Transactional
    public void deleteEntity(Long id) {
        User user = userContextPort.getCurrentUser();
        financialEntityPersistencePort.findById(id)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        
        // Desvincular de todas las cuentas que usen esta entidad
        accountPersistencePort.findAllByUser(user).stream()
                .filter(a -> a.getEntity() != null && a.getEntity().getId().equals(id))
                .forEach(a -> {
                    a.setEntity(null);
                    accountPersistencePort.save(a);
                });

        financialEntityPersistencePort.delete(id);
    }
}

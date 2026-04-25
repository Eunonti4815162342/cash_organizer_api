package com.tritit.cashorganizer.api.domain.port.out;

import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface FinancialEntityPersistencePort {
    List<FinancialEntity> findAllByUser(User user);
    Optional<FinancialEntity> findById(Long id);
    FinancialEntity save(FinancialEntity entity);
    void delete(Long id);
}

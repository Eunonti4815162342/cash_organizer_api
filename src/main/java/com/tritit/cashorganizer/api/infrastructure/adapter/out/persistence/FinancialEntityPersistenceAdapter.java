package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.FinancialEntityPersistencePort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.FinancialEntityEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FinancialEntityPersistenceAdapter implements FinancialEntityPersistencePort {

    private final FinancialEntityRepository financialEntityRepository;
    private final PersistenceMapper mapper;

    @Override
    public List<FinancialEntity> findAllByUser(User user) {
        UserEntity userEntity = mapper.toEntity(user);
        return financialEntityRepository.findAllByUser(userEntity).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<FinancialEntity> findById(Long id) {
        return financialEntityRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public FinancialEntity save(FinancialEntity entity) {
        FinancialEntityEntity persistenceEntity = mapper.toEntity(entity);
        return mapper.toDomain(financialEntityRepository.save(persistenceEntity));
    }

    @Override
    public void delete(Long id) {
        financialEntityRepository.deleteById(id);
    }
}

package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.FinancialEntityRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.FinancialEntityEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialEntityService {

    private final FinancialEntityRepository financialEntityRepository;
    private final UserContextPort userContextPort;
    private final PersistenceMapper mapper;

    @Transactional(readOnly = true)
    public List<FinancialEntity> getAllEntities() {
        UserEntity user = mapper.toEntity(userContextPort.getCurrentUser());
        return financialEntityRepository.findAllByUser(user).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public FinancialEntity createEntity(FinancialEntity entity) {
        UserEntity user = mapper.toEntity(userContextPort.getCurrentUser());
        FinancialEntityEntity persistenceEntity = mapper.toEntity(entity);
        persistenceEntity.setUser(user);
        return mapper.toDomain(financialEntityRepository.save(persistenceEntity));
    }

    @Transactional
    public void deleteEntity(Long id) {
        UserEntity user = mapper.toEntity(userContextPort.getCurrentUser());
        FinancialEntityEntity entity = financialEntityRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        financialEntityRepository.delete(entity);
    }
}

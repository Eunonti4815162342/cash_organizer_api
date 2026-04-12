package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.FinancialEntityRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.FinancialEntityEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialEntityService {

    private final FinancialEntityRepository financialEntityRepository;
    private final UserRepository userRepository;
    private final PersistenceMapper mapper;

    private UserEntity getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Transactional(readOnly = true)
    public List<FinancialEntity> getAllEntities() {
        UserEntity user = getCurrentUserEntity();
        return financialEntityRepository.findAllByUser(user).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public FinancialEntity createEntity(FinancialEntity entity) {
        UserEntity user = getCurrentUserEntity();
        FinancialEntityEntity persistenceEntity = mapper.toEntity(entity);
        persistenceEntity.setUser(user);
        return mapper.toDomain(financialEntityRepository.save(persistenceEntity));
    }

    @Transactional
    public void deleteEntity(Long id) {
        UserEntity user = getCurrentUserEntity();
        FinancialEntityEntity entity = financialEntityRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Entity not found"));
        financialEntityRepository.delete(entity);
    }
}

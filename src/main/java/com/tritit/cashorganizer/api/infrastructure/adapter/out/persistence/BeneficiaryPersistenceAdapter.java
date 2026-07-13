package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.Beneficiary;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.BeneficiaryPersistencePort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BeneficiaryPersistenceAdapter implements BeneficiaryPersistencePort {

    private final BeneficiaryRepository beneficiaryRepository;
    private final PersistenceMapper mapper;

    @Override
    public List<Beneficiary> findAllByUser(User user) {
        UserEntity userEntity = mapper.toEntity(user);
        return beneficiaryRepository.findAllByUser(userEntity).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Beneficiary> findById(Long id) {
        return beneficiaryRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdAndUser(Long id, User user) {
        UserEntity userEntity = mapper.toEntity(user);
        return beneficiaryRepository.existsByIdAndUser(id, userEntity);
    }

    @Override
    public Beneficiary save(Beneficiary beneficiary) {
        var entity = mapper.toEntity(beneficiary);
        return mapper.toDomain(beneficiaryRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        beneficiaryRepository.deleteById(id);
    }
}

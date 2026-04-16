package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.UserPersistencePort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository userRepository;
    private final PersistenceMapper mapper;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(mapper::toDomain);
    }
}

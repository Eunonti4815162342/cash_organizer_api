package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountPersistencePort {

    private final AccountRepository accountRepository;
    private final PersistenceMapper mapper;

    @Override
    public List<AccountItem> findAllByUser(User user) {
        UserEntity userEntity = mapper.toEntity(user);
        return accountRepository.findAllByUser(userEntity).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AccountItem> findById(Long id) {
        return accountRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public AccountItem save(AccountItem account) {
        var entity = mapper.toEntity(account);
        return mapper.toDomain(accountRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        accountRepository.deleteById(id);
    }
}

package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    List<AccountEntity> findAllByUser(UserEntity user);
    List<AccountEntity> findAllByUserId(UUID userId);
}

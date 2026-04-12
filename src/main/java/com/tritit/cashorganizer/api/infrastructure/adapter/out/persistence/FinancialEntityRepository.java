package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.FinancialEntityEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FinancialEntityRepository extends JpaRepository<FinancialEntityEntity, Long> {
    List<FinancialEntityEntity> findAllByUser(UserEntity user);
}

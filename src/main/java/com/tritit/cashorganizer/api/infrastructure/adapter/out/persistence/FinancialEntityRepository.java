package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialEntityRepository extends JpaRepository<FinancialEntity, Long> {
}

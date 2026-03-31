package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionItem, Long> {
    
    @Query("SELECT t FROM TransactionItem t WHERE t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<TransactionItem> findAllByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query("SELECT t FROM TransactionItem t WHERE (t.account.id = :accountId OR t.toAccount.id = :accountId) AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<TransactionItem> findAllByAccountAndDateRange(@Param("accountId") Long accountId, @Param("startDate") String startDate, @Param("endDate") String endDate);
}

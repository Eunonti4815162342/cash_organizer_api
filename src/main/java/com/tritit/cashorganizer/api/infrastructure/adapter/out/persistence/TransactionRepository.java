package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionItem, Long> {
    
    List<TransactionItem> findAllByUser(User user);

    @Query("SELECT t FROM TransactionItem t WHERE t.user = :user AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<TransactionItem> findAllByUserAndDateRange(@Param("user") User user, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Query("SELECT t FROM TransactionItem t WHERE t.user = :user AND (t.account.id = :accountId OR t.toAccount.id = :accountId) AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<TransactionItem> findAllByUserAndAccountAndDateRange(@Param("user") User user, @Param("accountId") Long accountId, @Param("startDate") String startDate, @Param("endDate") String endDate);
}

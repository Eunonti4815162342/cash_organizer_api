package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.TransactionItemEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionItemEntity, Long> {
    
    Page<TransactionItemEntity> findAllByUser(UserEntity user, Pageable pageable);

    @Query("SELECT t FROM TransactionItemEntity t WHERE t.user = :user AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    Page<TransactionItemEntity> findAllByUserAndDateRange(@Param("user") UserEntity user, @Param("startDate") String startDate, @Param("endDate") String endDate, Pageable pageable);

    @Query("SELECT t FROM TransactionItemEntity t WHERE t.user = :user AND (t.account.id IN :accountIds OR t.toAccount.id IN :accountIds) AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    Page<TransactionItemEntity> findAllByUserAndAccountAndDateRange(@Param("user") UserEntity user, @Param("accountIds") java.util.List<Long> accountIds, @Param("startDate") String startDate, @Param("endDate") String endDate, Pageable pageable);

    @Query("SELECT t FROM TransactionItemEntity t WHERE t.user = :user AND t.category.id = :categoryId ORDER BY t.date DESC")
    Page<TransactionItemEntity> findAllByUserAndCategory(@Param("user") UserEntity user, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM TransactionItemEntity t WHERE t.user = :user AND t.category.id = :categoryId")
    long countByUserAndCategory(@Param("user") UserEntity user, @Param("categoryId") Long categoryId);

    @Query("SELECT t.category.id, t.type FROM TransactionItemEntity t WHERE t.user.id = :userId AND t.beneficiary.id = :beneficiaryId GROUP BY t.category.id, t.type ORDER BY COUNT(t) DESC")
    java.util.List<Object[]> findMostFrequentCategoryAndType(@Param("userId") java.util.UUID userId, @Param("beneficiaryId") Long beneficiaryId, Pageable pageable);

    @Modifying
    @Query("UPDATE TransactionItemEntity t SET t.subcategory = null WHERE t.user = :user AND t.subcategory.id = :subcategoryId")
    int unlinkSubcategoryFromTransactions(@Param("user") UserEntity user, @Param("subcategoryId") Long subcategoryId);
}

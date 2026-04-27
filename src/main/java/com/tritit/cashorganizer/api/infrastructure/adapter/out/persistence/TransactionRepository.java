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
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionItemEntity, Long> {

    @Query("SELECT t FROM TransactionItemEntity t WHERE t.user = :user")
    Page<TransactionItemEntity> findAllByUser(@Param("user") UserEntity user, Pageable pageable);

    @Query("SELECT t FROM TransactionItemEntity t WHERE t.user = :user AND t.date BETWEEN :startDate AND :endDate")
    Page<TransactionItemEntity> findAllByUserAndDateRange(@Param("user") UserEntity user, @Param("startDate") String startDate, @Param("endDate") String endDate, Pageable pageable);

    @Query("SELECT t FROM TransactionItemEntity t WHERE t.user = :user AND t.account.id IN :accountIds AND t.date BETWEEN :startDate AND :endDate")
    Page<TransactionItemEntity> findAllByUserAndAccountAndDateRange(@Param("user") UserEntity user, @Param("accountIds") List<Long> accountIds, @Param("startDate") String startDate, @Param("endDate") String endDate, Pageable pageable);

    @Query("SELECT t FROM TransactionItemEntity t WHERE t.user = :user AND t.category.id = :categoryId")
    Page<TransactionItemEntity> findAllByUserAndCategory(@Param("user") UserEntity user, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM TransactionItemEntity t WHERE t.user = :user AND t.category.id = :categoryId")
    long countByUserAndCategory(@Param("user") UserEntity user, @Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE TransactionItemEntity t SET t.subcategory = null WHERE t.user = :user AND t.subcategory.id = :subcategoryId")
    int unlinkSubcategoryFromTransactions(@Param("user") UserEntity user, @Param("subcategoryId") Long subcategoryId);

    @Query("SELECT t.category.id, t.subcategory.id, t.type FROM TransactionItemEntity t WHERE t.user.id = :userId AND t.beneficiary.id = :beneficiaryId GROUP BY t.category.id, t.subcategory.id, t.type ORDER BY COUNT(t) DESC")
    List<Object[]> findMostFrequentCategoryAndType(@Param("userId") UUID userId, @Param("beneficiaryId") Long beneficiaryId, Pageable pageable);

    // CONSULTA DE AUDITORÍA CORREGIDA: LEFT JOIN para no perder datos
    @Query("SELECT t FROM TransactionItemEntity t " +
           "LEFT JOIN FETCH t.account a " +
           "LEFT JOIN FETCH a.financialEntity e " +
           "LEFT JOIN FETCH t.category c " +
           "LEFT JOIN FETCH t.subcategory s " +
           "LEFT JOIN FETCH t.beneficiary b " +
           "WHERE t.user = :user AND t.date BETWEEN :startDate AND :endDate")
    List<TransactionItemEntity> findAllForReport(@Param("user") UserEntity user, @Param("startDate") String startDate, @Param("endDate") String endDate);
}

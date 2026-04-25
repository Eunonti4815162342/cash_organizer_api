package com.tritit.cashorganizer.api.domain.port.out;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.TransactionSuggestion;
import com.tritit.cashorganizer.api.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface TransactionPersistencePort {
    TransactionItem save(TransactionItem transaction);
    void delete(TransactionItem transaction);
    void deleteById(Long id);
    Page<TransactionItem> findAllByUser(User user, Pageable pageable);
    Optional<TransactionItem> findById(Long id);
    Page<TransactionItem> findAllByUserAndDateRange(User user, String startDate, String endDate, Pageable pageable);
    Page<TransactionItem> findAllByUserAndAccountAndDateRange(User user, List<Long> accountIds, String startDate, String endDate, Pageable pageable);
    Page<TransactionItem> findAllByUserAndCategory(User user, Long categoryId, Pageable pageable);
    long countByUserAndCategory(User user, Long categoryId);
    void unlinkSubcategoryFromTransactions(User user, Long subcategoryId);
    java.util.Optional<com.tritit.cashorganizer.api.domain.model.TransactionSuggestion> findMostFrequentCategoryAndType(java.util.UUID userId, Long beneficiaryId);
}

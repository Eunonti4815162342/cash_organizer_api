package com.tritit.cashorganizer.api.domain.port.in;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.TransactionSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionUseCase {
    Page<TransactionItem> getAllTransactions(Pageable pageable);
    Page<TransactionItem> getTransactionsByDateRange(String startDate, String endDate, Pageable pageable);
    Page<TransactionItem> getTransactionsByAccountAndDateRange(java.util.List<Long> accountIds, String startDate, String endDate, Pageable pageable);
    TransactionItem createTransaction(TransactionItem transaction);
    TransactionItem updateTransaction(Long id, TransactionItem transactionDetails);
    void deleteTransaction(Long id);
    TransactionSuggestion getSuggestionForBeneficiary(Long beneficiaryId);
}

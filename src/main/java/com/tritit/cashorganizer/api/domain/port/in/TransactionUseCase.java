package com.tritit.cashorganizer.api.domain.port.in;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransactionUseCase {
    Page<TransactionItem> getAllTransactions(Pageable pageable);
    Page<TransactionItem> getTransactionsByDateRange(String startDate, String endDate, Pageable pageable);
    Page<TransactionItem> getTransactionsByAccountAndDateRange(List<Long> accountIds, String startDate, String endDate, Pageable pageable);
    TransactionItem createTransaction(TransactionItem transaction);
    TransactionItem updateTransaction(Long id, TransactionItem transactionDetails);
    void deleteTransaction(Long id);
}

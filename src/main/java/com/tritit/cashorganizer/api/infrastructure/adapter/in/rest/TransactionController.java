package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.application.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @GetMapping
    public Page<TransactionItem> getAllTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        
        if (startDate != null && endDate != null) {
            if (accountId != null) {
                return service.getTransactionsByAccountAndDateRange(accountId, startDate, endDate, pageable);
            }
            return service.getTransactionsByDateRange(startDate, endDate, pageable);
        }
        return service.getAllTransactions(pageable);
    }

    @PostMapping
    public TransactionItem createTransaction(@RequestBody TransactionItem transactionItem) {
        return service.createTransaction(transactionItem);
    }

    @PutMapping("/{id}")
    public TransactionItem updateTransaction(@PathVariable Long id, @RequestBody TransactionItem transactionItem) {
        return service.updateTransaction(id, transactionItem);
    }

    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable Long id) {
        service.deleteTransaction(id);
    }
}

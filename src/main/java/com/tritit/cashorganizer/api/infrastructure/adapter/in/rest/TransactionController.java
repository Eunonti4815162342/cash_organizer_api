package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.application.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @GetMapping
    public Page<TransactionItem> getAllTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> accountIds,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        // Sanitize dates: recorta formatos ISO largos (2025-09-01T00:00:00 -> 2025-09-01)
        String sDate = (startDate != null && startDate.contains("T")) ? startDate.split("T")[0] : startDate;
        String eDate = (endDate != null && endDate.contains("T")) ? endDate.split("T")[0] : endDate;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        // Unificar accountId en la lista si viene informado
        List<Long> finalAccountIds = accountIds;
        if (accountId != null) {
            finalAccountIds = (accountIds == null) ? new java.util.ArrayList<>() : new java.util.ArrayList<>(accountIds);
            if (!finalAccountIds.contains(accountId)) {
                finalAccountIds.add(accountId);
            }
        }
        
        if (sDate != null && eDate != null) {
            if (finalAccountIds != null && !finalAccountIds.isEmpty()) {
                return service.getTransactionsByAccountAndDateRange(finalAccountIds, sDate, eDate, pageable);
            }
            return service.getTransactionsByDateRange(sDate, eDate, pageable);
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

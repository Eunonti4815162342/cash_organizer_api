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
        
        try {
            // Sanitize dates: robust truncation to YYYY-MM-DD
            String sDate = startDate;
            if (sDate != null) {
                if (sDate.contains("T")) sDate = sDate.split("T")[0];
                if (sDate.contains(" ")) sDate = sDate.split(" ")[0];
                if (sDate.length() > 10) sDate = sDate.substring(0, 10);
            }

            String eDate = endDate;
            if (eDate != null) {
                if (eDate.contains("T")) eDate = eDate.split("T")[0];
                if (eDate.contains(" ")) eDate = eDate.split(" ")[0];
                if (eDate.length() > 10) eDate = eDate.substring(0, 10);
            }
            
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
        } catch (Exception e) {
            e.printStackTrace(); // Esto saldrá en la consola del servidor
            throw e; // Volvemos a lanzar para que siga siendo un error pero ahora sepamos qué es
        }
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

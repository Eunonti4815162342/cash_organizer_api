package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.application.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @GetMapping
    public List<TransactionItem> getAllTransactions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long accountId) {
        
        return service.getTransactions(startDate, endDate, accountId);
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

package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository repository;

    @GetMapping
    public List<TransactionItem> getAllTransactions() {
        return repository.findAll();
    }

    @PostMapping
    public TransactionItem createTransaction(@RequestBody TransactionItem transactionItem) {
        return repository.save(transactionItem);
    }
}
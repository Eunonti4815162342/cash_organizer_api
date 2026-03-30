package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AccountController {

    private final AccountRepository repository;

    @GetMapping
    public List<AccountItem> getAllAccounts() {
        return repository.findAll();
    }

    @PostMapping
    public AccountItem createAccount(@RequestBody AccountItem accountItem) {
        return repository.save(accountItem);
    }
}
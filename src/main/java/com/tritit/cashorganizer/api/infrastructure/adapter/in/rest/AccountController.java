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
    private final com.tritit.cashorganizer.api.application.AccountService service;

    @GetMapping
    public List<AccountItem> getAllAccounts() {
        return service.getAllActiveAccounts();
    }

    @PostMapping
    public AccountItem createAccount(@RequestBody AccountItem accountItem) {
        return repository.save(accountItem);
    }

    @PutMapping("/{id}")
    public AccountItem updateAccount(@PathVariable Long id, @RequestBody AccountItem accountItem) {
        return service.updateAccount(id, accountItem);
    }

    @DeleteMapping("/{id}")
    public void closeAccount(@PathVariable Long id) {
        service.closeAccount(id);
    }

    @DeleteMapping("/{id}/permanent")
    public void deleteAccountPermanently(@PathVariable Long id) {
        service.permanentlyDeleteAccount(id);
    }
}
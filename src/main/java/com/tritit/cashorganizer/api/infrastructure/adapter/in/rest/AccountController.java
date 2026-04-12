package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.AccountService;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @GetMapping
    public List<AccountItem> getAccounts() {
        return service.getAllActiveAccounts();
    }

    @PostMapping
    public AccountItem createAccount(@RequestBody AccountItem accountItem) {
        return service.createAccount(accountItem);
    }

    @PutMapping("/{id}")
    public AccountItem updateAccount(@PathVariable Long id, @RequestBody AccountItem accountItem) {
        return service.updateAccount(id, accountItem);
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean permanent) {
        if (permanent) {
            service.permanentlyDeleteAccount(id);
        } else {
            service.closeAccount(id);
        }
    }
}

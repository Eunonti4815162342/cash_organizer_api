package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<AccountItem> getAllActiveAccounts() {
        return accountRepository.findAll().stream()
                .filter(a -> a.getActive() == null || a.getActive())
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountItem updateAccount(Long id, AccountItem accountDetails) {
        AccountItem account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Rule: Unique name
        boolean nameExists = accountRepository.findAll().stream()
                .anyMatch(a -> !a.getId().equals(id) && a.getName().equalsIgnoreCase(accountDetails.getName()));
        
        if (nameExists) {
            throw new RuntimeException("An account with this name already exists.");
        }
        
        account.setName(accountDetails.getName());
        account.setDescription(accountDetails.getDescription());
        account.setAccountType(accountDetails.getAccountType());
        account.setNotes(accountDetails.getNotes());
        account.setFlags(accountDetails.getFlags());
        account.setEntity(accountDetails.getEntity());
        
        // Rule: Balance can only be updated if there are NO transactions
        boolean hasTransactions = transactionRepository.findAll().stream()
                .anyMatch(t -> (t.getAccount() != null && t.getAccount().getId().equals(id)) || 
                             (t.getToAccount() != null && t.getToAccount().getId().equals(id)));

        if (accountDetails.getAmount() != null) {
            account.getAmount().setCurrency(accountDetails.getAmount().getCurrency());
            if (!hasTransactions) {
                account.getAmount().setValue(accountDetails.getAmount().getValue());
            }
        }
        
        return accountRepository.save(account);
    }

    @Transactional
    public void closeAccount(Long id) {
        AccountItem account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getActive()) return;

        // 1. Mark as inactive
        account.setActive(false);
        accountRepository.save(account);

        // 2. Register closing transaction
        TransactionItem closeTx = new TransactionItem();
        closeTx.setAccount(account);
        closeTx.setType(TransactionItem.TransactionType.ACCOUNT_CLOSE);
        closeTx.setDate(LocalDateTime.now().toString());
        closeTx.setDescription("Account Closed: " + account.getName());
        closeTx.setAmount(new Amount(account.getAmount().getValue(), account.getAmount().getCurrency(), false));
        closeTx.setStatusFlags(0);
        closeTx.setIsScheduled(false);
        closeTx.setIsHeader(false);
        
        transactionRepository.save(closeTx);
    }

    @Transactional
    public void permanentlyDeleteAccount(Long id) {
        AccountItem account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // 1. Delete all transactions linked to this account (including transfers where it is source or destination)
        transactionRepository.findAll().stream()
                .filter(t -> (t.getAccount() != null && t.getAccount().getId().equals(id)) || 
                             (t.getToAccount() != null && t.getToAccount().getId().equals(id)))
                .forEach(t -> transactionRepository.delete(t));

        // 2. Physically remove the account
        accountRepository.delete(account);
    }
}

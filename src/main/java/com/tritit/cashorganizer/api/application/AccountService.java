package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public List<AccountItem> getAllActiveAccounts() {
        User user = getCurrentUser();
        return accountRepository.findAllByUser(user).stream()
                .filter(a -> a.getActive() == null || a.getActive())
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountItem updateAccount(Long id, AccountItem accountDetails) {
        User user = getCurrentUser();
        
        AccountItem account = accountRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Account not found or access denied"));

        // Rule: Unique name (within user's accounts)
        boolean nameExists = accountRepository.findAllByUser(user).stream()
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
        boolean hasTransactions = transactionRepository.findAllByUser(user).stream()
                .anyMatch(t -> (t.getAccount() != null && t.getAccount().getId().equals(id)) || 
                             (t.getToAccount() != null && t.getToAccount().getId().equals(id)));

        if (accountDetails.getAmount() != null) {
            if (account.getAmount() == null) account.setAmount(new Amount());
            account.getAmount().setCurrency(accountDetails.getAmount().getCurrency());
            if (!hasTransactions) {
                account.getAmount().setValue(accountDetails.getAmount().getValue());
            }
        }
        
        return accountRepository.save(account);
    }

    @Transactional
    public void closeAccount(Long id) {
        User user = getCurrentUser();
        
        AccountItem account = accountRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Account not found or access denied"));

        if (!account.getActive()) return;

        // 1. Mark as inactive
        account.setActive(false);
        accountRepository.save(account);

        // 2. Register closing transaction
        TransactionItem closeTx = new TransactionItem();
        closeTx.setUser(user);
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
        User user = getCurrentUser();
        
        AccountItem account = accountRepository.findById(id)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Account not found or access denied"));

        // 1. Delete all transactions linked to this account
        transactionRepository.findAllByUser(user).stream()
                .filter(t -> (t.getAccount() != null && t.getAccount().getId().equals(id)) || 
                             (t.getToAccount() != null && t.getToAccount().getId().equals(id)))
                .forEach(t -> transactionRepository.delete(t));

        // 2. Physically remove the account
        accountRepository.delete(account);
    }
}

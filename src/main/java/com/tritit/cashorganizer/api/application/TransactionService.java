package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Transactional(readOnly = true)
    public List<TransactionItem> getTransactions(String startDate, String endDate, Long accountId) {
        User user = getCurrentUser();
        
        if (startDate != null && endDate != null) {
            if (accountId != null) {
                return transactionRepository.findAllByUserAndAccountAndDateRange(user, accountId, startDate, endDate);
            }
            return transactionRepository.findAllByUserAndDateRange(user, startDate, endDate);
        }
        return transactionRepository.findAllByUser(user);
    }

    @Transactional
    public TransactionItem createTransaction(TransactionItem transaction) {
        User user = getCurrentUser();
        transaction.setUser(user);
        
        applyTransactionImpact(transaction, false, user);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public TransactionItem updateTransaction(Long id, TransactionItem newTransaction) {
        User user = getCurrentUser();
        
        TransactionItem oldTransaction = transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        // 1. Revert old impact
        applyTransactionImpact(oldTransaction, true, user);

        // 2. Apply new impact
        newTransaction.setUser(user);
        applyTransactionImpact(newTransaction, false, user);

        newTransaction.setId(id);
        return transactionRepository.save(newTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        User user = getCurrentUser();
        
        TransactionItem transaction = transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));
        
        // REVERSAL LOGIC
        if (transaction.getType() == TransactionItem.TransactionType.ACCOUNT_CLOSE) {
            AccountItem account = transaction.getAccount();
            account.setActive(true);
            accountRepository.save(account);
        } else {
            applyTransactionImpact(transaction, true, user);
        }
        
        transactionRepository.delete(transaction);
    }

    private void applyTransactionImpact(TransactionItem transaction, boolean revert, User user) {
        if (transaction.getAccount() == null) throw new RuntimeException("Source account is mandatory");

        AccountItem fromAccount = accountRepository.findById(transaction.getAccount().getId())
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Source account not found or access denied"));

        long val = transaction.getAmount().getValue();
        if (revert) val = -val;

        TransactionItem.TransactionType type = transaction.getType();
        if (type == null) type = TransactionItem.TransactionType.EXPENSE;

        switch (type) {
            case INCOME:
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() + val);
                break;
            case EXPENSE:
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() - val);
                break;
            case TRANSFER:
                if (transaction.getToAccount() == null) throw new RuntimeException("Destination account is mandatory for transfers");
                
                AccountItem toAccount = accountRepository.findById(transaction.getToAccount().getId())
                        .filter(a -> a.getUser().getId().equals(user.getId()))
                        .orElseThrow(() -> new RuntimeException("Destination account not found or access denied"));
                
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() - val);
                toAccount.getAmount().setValue(toAccount.getAmount().getValue() + val);
                accountRepository.save(toAccount);
                break;
        }
        accountRepository.save(fromAccount);
    }
}

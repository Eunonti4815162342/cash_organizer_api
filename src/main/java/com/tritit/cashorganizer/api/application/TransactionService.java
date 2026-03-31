package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionItem createTransaction(TransactionItem transaction) {
        applyTransactionImpact(transaction, false);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public TransactionItem updateTransaction(Long id, TransactionItem newTransaction) {
        TransactionItem oldTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        // 1. Revert old impact
        applyTransactionImpact(oldTransaction, true);

        // 2. Apply new impact
        applyTransactionImpact(newTransaction, false);

        newTransaction.setId(id);
        return transactionRepository.save(newTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        TransactionItem transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        // REVERSAL LOGIC
        if (transaction.getType() == TransactionItem.TransactionType.ACCOUNT_CLOSE) {
            AccountItem account = transaction.getAccount();
            account.setActive(true); // Reabrir la cuenta
            accountRepository.save(account);
        } else {
            // Revert impact for normal transactions
            applyTransactionImpact(transaction, true);
        }
        
        transactionRepository.deleteById(id);
    }

    private void applyTransactionImpact(TransactionItem transaction, boolean revert) {
        AccountItem fromAccount = accountRepository.findById(transaction.getAccount().getId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        long val = transaction.getAmount().getValue();
        if (revert) val = -val; // Reverse the operation

        TransactionItem.TransactionType type = transaction.getType();
        if (type == null) type = TransactionItem.TransactionType.EXPENSE; // Default for legacy data

        switch (type) {
            case INCOME:
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() + val);
                break;
            case EXPENSE:
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() - val);
                break;
            case TRANSFER:
                AccountItem toAccount = accountRepository.findById(transaction.getToAccount().getId())
                        .orElseThrow(() -> new RuntimeException("Destination account not found"));
                
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() - val);
                toAccount.getAmount().setValue(toAccount.getAmount().getValue() + val);
                accountRepository.save(toAccount);
                break;
        }
        accountRepository.save(fromAccount);
    }
}

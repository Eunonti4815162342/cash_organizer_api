package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.in.TransactionUseCase;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService implements TransactionUseCase {

    private final TransactionPersistencePort transactionPersistencePort;
    private final AccountPersistencePort accountPersistencePort;
    private final UserPersistencePort userPersistencePort;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userPersistencePort.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> getAllTransactions(Pageable pageable) {
        User user = getCurrentUser();
        return transactionPersistencePort.findAllByUser(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> getTransactionsByDateRange(String startDate, String endDate, Pageable pageable) {
        User user = getCurrentUser();
        return transactionPersistencePort.findAllByUserAndDateRange(user, startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> getTransactionsByAccountAndDateRange(Long accountId, String startDate, String endDate, Pageable pageable) {
        User user = getCurrentUser();
        return transactionPersistencePort.findAllByUserAndAccountAndDateRange(user, accountId, startDate, endDate, pageable);
    }

    @Override
    @Transactional
    public TransactionItem createTransaction(TransactionItem transaction) {
        User user = getCurrentUser();
        transaction.setUser(user);
        
        applyTransactionImpact(transaction, false, user);
        return transactionPersistencePort.save(transaction);
    }

    @Override
    @Transactional
    public TransactionItem updateTransaction(Long id, TransactionItem newTransaction) {
        User user = getCurrentUser();
        
        TransactionItem oldTransaction = transactionPersistencePort.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        // Revertimos impacto de la anterior
        applyTransactionImpact(oldTransaction, true, user);

        // Aplicamos impacto de la nueva
        newTransaction.setUser(user);
        newTransaction.setId(id);
        applyTransactionImpact(newTransaction, false, user);

        return transactionPersistencePort.save(newTransaction);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        User user = getCurrentUser();
        
        TransactionItem transaction = transactionPersistencePort.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));
        
        if (transaction.getType() == TransactionItem.TransactionType.ACCOUNT_CLOSE) {
            AccountItem account = transaction.getAccount();
            if (account != null) {
                account.setActive(true);
                accountPersistencePort.save(account);
            }
        } else {
            applyTransactionImpact(transaction, true, user);
        }
        
        transactionPersistencePort.deleteById(id);
    }

    private void applyTransactionImpact(TransactionItem transaction, boolean revert, User user) {
        if (transaction.getAccount() == null) throw new RuntimeException("Source account is mandatory");

        AccountItem fromAccount = accountPersistencePort.findById(transaction.getAccount().getId())
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
                
                AccountItem toAccount = accountPersistencePort.findById(transaction.getToAccount().getId())
                        .filter(a -> a.getUser().getId().equals(user.getId()))
                        .orElseThrow(() -> new RuntimeException("Destination account not found or access denied"));
                
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() - val);
                toAccount.getAmount().setValue(toAccount.getAmount().getValue() + val);
                accountPersistencePort.save(toAccount);
                break;
            default:
                break;
        }
        accountPersistencePort.save(fromAccount);
    }
}

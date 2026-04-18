package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.application.impact.TransactionImpactResolver;
import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.in.TransactionUseCase;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService implements TransactionUseCase {

    private final TransactionPersistencePort transactionPersistencePort;
    private final UserContextPort userContextPort;
    private final TransactionImpactResolver impactResolver;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> getAllTransactions(Pageable pageable) {
        User user = userContextPort.getCurrentUser();
        return transactionPersistencePort.findAllByUser(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> getTransactionsByDateRange(String startDate, String endDate, Pageable pageable) {
        User user = userContextPort.getCurrentUser();
        return transactionPersistencePort.findAllByUserAndDateRange(user, startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> getTransactionsByAccountAndDateRange(Long accountId, String startDate, String endDate, Pageable pageable) {
        User user = userContextPort.getCurrentUser();
        return transactionPersistencePort.findAllByUserAndAccountAndDateRange(user, accountId, startDate, endDate, pageable);
    }

    @Override
    @Transactional
    public TransactionItem createTransaction(TransactionItem transaction) {
        User user = userContextPort.getCurrentUser();
        transaction.setUser(user);
        validateSourceAccount(transaction);

        impactResolver.forType(transaction.getType()).apply(transaction, user);
        return transactionPersistencePort.save(transaction);
    }

    @Override
    @Transactional
    public TransactionItem updateTransaction(Long id, TransactionItem newTransaction) {
        User user = userContextPort.getCurrentUser();

        TransactionItem oldTransaction = transactionPersistencePort.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        impactResolver.forType(oldTransaction.getType()).revert(oldTransaction, user);

        newTransaction.setUser(user);
        newTransaction.setId(id);
        validateSourceAccount(newTransaction);
        impactResolver.forType(newTransaction.getType()).apply(newTransaction, user);

        return transactionPersistencePort.save(newTransaction);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        User user = userContextPort.getCurrentUser();

        TransactionItem transaction = transactionPersistencePort.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        impactResolver.forType(transaction.getType()).revert(transaction, user);
        transactionPersistencePort.deleteById(id);
    }

    private void validateSourceAccount(TransactionItem transaction) {
        if (transaction.getAccount() == null) {
            throw new InvalidTransactionException("Source account is mandatory");
        }
    }
}

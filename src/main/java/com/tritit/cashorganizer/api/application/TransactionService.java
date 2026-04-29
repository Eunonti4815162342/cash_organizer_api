package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.application.impact.TransactionImpactResolver;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.TransactionSuggestion;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.in.TransactionUseCase;
import com.tritit.cashorganizer.api.domain.port.out.BeneficiaryPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService implements TransactionUseCase {

    private final TransactionPersistencePort transactionPersistencePort;
    private final UserContextPort userContextPort;
    private final BeneficiaryPersistencePort beneficiaryPersistencePort;
    private final TransactionImpactResolver impactResolver;

    @Override
    @Transactional(readOnly = true)
    public TransactionSuggestion getSuggestionForBeneficiary(Long beneficiaryId) {
        User user = userContextPort.getCurrentUser();
        
        // Validar que el beneficiario existe y pertenece al usuario
        beneficiaryPersistencePort.findById(beneficiaryId)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found or unauthorized"));

        return transactionPersistencePort.findMostFrequentCategoryAndType(user.getId(), beneficiaryId)
                .orElse(new TransactionSuggestion());
    }

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
    public Page<TransactionItem> getTransactionsByAccountAndDateRange(List<Long> accountIds, String startDate, String endDate, Pageable pageable) {
        User user = userContextPort.getCurrentUser();
        return transactionPersistencePort.findAllByUserAndAccountAndDateRange(user, accountIds, startDate, endDate, pageable);
    }

    @Override
    @Transactional
    public TransactionItem createTransaction(TransactionItem transaction) {
        User user = userContextPort.getCurrentUser();
        transaction.setUser(user);
        transaction.validate();

        // Handle on-the-fly beneficiary creation
        if (transaction.getBeneficiary() != null && transaction.getBeneficiary().getId() == null) {
            transaction.getBeneficiary().setUser(user);
            transaction.setBeneficiary(beneficiaryPersistencePort.save(transaction.getBeneficiary()));
        }

        impactResolver.forType(transaction.getType()).apply(transaction, user);
        return transactionPersistencePort.save(transaction);
    }

    @Override
    @Transactional
    public TransactionItem updateTransaction(Long id, TransactionItem newTransaction) {
        User user = userContextPort.getCurrentUser();

        TransactionItem oldTransaction = transactionPersistencePort.findById(id)
                .filter(t -> t.belongsTo(user))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        impactResolver.forType(oldTransaction.getType()).revert(oldTransaction, user);

        newTransaction.setUser(user);
        newTransaction.setId(id);
        newTransaction.validate();

        // Handle on-the-fly beneficiary creation
        if (newTransaction.getBeneficiary() != null && newTransaction.getBeneficiary().getId() == null) {
            newTransaction.getBeneficiary().setUser(user);
            newTransaction.setBeneficiary(beneficiaryPersistencePort.save(newTransaction.getBeneficiary()));
        }

        impactResolver.forType(newTransaction.getType()).apply(newTransaction, user);

        return transactionPersistencePort.save(newTransaction);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        User user = userContextPort.getCurrentUser();

        TransactionItem transaction = transactionPersistencePort.findById(id)
                .filter(t -> t.belongsTo(user))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        impactResolver.forType(transaction.getType()).revert(transaction, user);
        transactionPersistencePort.deleteById(id);
    }
}

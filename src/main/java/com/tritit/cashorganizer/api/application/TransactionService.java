package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.TransactionItemEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final PersistenceMapper mapper;

    private UserEntity getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Transactional(readOnly = true)
    public Page<TransactionItem> getTransactions(String startDate, String endDate, Long accountId, Pageable pageable) {
        UserEntity user = getCurrentUserEntity();
        
        Page<TransactionItemEntity> entities;
        if (startDate != null && endDate != null) {
            if (accountId != null) {
                entities = transactionRepository.findAllByUserAndAccountAndDateRange(user, accountId, startDate, endDate, pageable);
            } else {
                entities = transactionRepository.findAllByUserAndDateRange(user, startDate, endDate, pageable);
            }
        } else {
            entities = transactionRepository.findAllByUser(user, pageable);
        }
        return entities.map(mapper::toDomain);
    }

    @Transactional
    public TransactionItem createTransaction(TransactionItem transaction) {
        UserEntity user = getCurrentUserEntity();
        TransactionItemEntity entity = mapper.toEntity(transaction);
        entity.setUser(user);
        
        applyTransactionImpact(entity, false, user);
        return mapper.toDomain(transactionRepository.save(entity));
    }

    @Transactional
    public TransactionItem updateTransaction(Long id, TransactionItem newTransaction) {
        UserEntity user = getCurrentUserEntity();
        
        TransactionItemEntity oldEntity = transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));

        applyTransactionImpact(oldEntity, true, user);

        TransactionItemEntity newEntity = mapper.toEntity(newTransaction);
        newEntity.setUser(user);
        newEntity.setId(id);
        applyTransactionImpact(newEntity, false, user);

        return mapper.toDomain(transactionRepository.save(newEntity));
    }

    @Transactional
    public void deleteTransaction(Long id) {
        UserEntity user = getCurrentUserEntity();
        
        TransactionItemEntity entity = transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Transaction not found or access denied"));
        
        if (entity.getType() == TransactionItem.TransactionType.ACCOUNT_CLOSE) {
            AccountEntity account = entity.getAccount();
            account.setActive(true);
            accountRepository.save(account);
        } else {
            applyTransactionImpact(entity, true, user);
        }
        
        transactionRepository.delete(entity);
    }

    private void applyTransactionImpact(TransactionItemEntity entity, boolean revert, UserEntity user) {
        if (entity.getAccount() == null) throw new RuntimeException("Source account is mandatory");

        AccountEntity fromAccount = accountRepository.findById(entity.getAccount().getId())
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Source account not found or access denied"));

        long val = entity.getAmount().getValue();
        if (revert) val = -val;

        TransactionItem.TransactionType type = entity.getType();
        if (type == null) type = TransactionItem.TransactionType.EXPENSE;

        switch (type) {
            case INCOME:
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() + val);
                break;
            case EXPENSE:
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() - val);
                break;
            case TRANSFER:
                if (entity.getToAccount() == null) throw new RuntimeException("Destination account is mandatory for transfers");
                
                AccountEntity toAccount = accountRepository.findById(entity.getToAccount().getId())
                        .filter(a -> a.getUser().getId().equals(user.getId()))
                        .orElseThrow(() -> new RuntimeException("Destination account not found or access denied"));
                
                fromAccount.getAmount().setValue(fromAccount.getAmount().getValue() - val);
                toAccount.getAmount().setValue(toAccount.getAmount().getValue() + val);
                accountRepository.save(toAccount);
                break;
            default:
                break;
        }
        accountRepository.save(fromAccount);
    }
}

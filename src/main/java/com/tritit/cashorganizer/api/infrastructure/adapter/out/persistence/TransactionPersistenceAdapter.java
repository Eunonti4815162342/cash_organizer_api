package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem.TransactionType;
import com.tritit.cashorganizer.api.domain.model.TransactionSuggestion;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.TransactionItemEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionPersistencePort {

    private final TransactionRepository transactionRepository;
    private final PersistenceMapper mapper;

    @Override
    public TransactionItem save(TransactionItem transaction) {
        TransactionItemEntity entity = mapper.toEntity(transaction);
        TransactionItemEntity savedEntity = transactionRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void delete(TransactionItem transaction) {
        transactionRepository.delete(mapper.toEntity(transaction));
    }

    @Override
    public void deleteById(Long id) {
        transactionRepository.deleteById(id);
    }

    @Override
    public Page<TransactionItem> findAllByUser(User user, Pageable pageable) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.findAllByUser(userEntity, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<TransactionItem> findById(Long id) {
        return transactionRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<TransactionItem> findAllByUserAndDateRange(User user, String startDate, String endDate, Pageable pageable) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.findAllByUserAndDateRange(userEntity, startDate, endDate, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<TransactionItem> findAllByUserAndAccountAndDateRange(User user, List<Long> accountIds, String startDate, String endDate, Pageable pageable) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.findAllByUserAndAccountAndDateRange(userEntity, accountIds, startDate, endDate, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<TransactionItem> findAllByUserAndCategory(User user, Long categoryId, Pageable pageable) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.findAllByUserAndCategory(userEntity, categoryId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public long countByUserAndCategory(User user, Long categoryId) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.countByUserAndCategory(userEntity, categoryId);
    }

    @Override
    public void unlinkSubcategoryFromTransactions(User user, Long subcategoryId) {
        UserEntity userEntity = mapper.toEntity(user);
        transactionRepository.unlinkSubcategoryFromTransactions(userEntity, subcategoryId);
    }

    @Override
    public Optional<TransactionSuggestion> findMostFrequentCategoryAndType(UUID userId, Long beneficiaryId) {
        List<Object[]> results = transactionRepository.findMostFrequentCategoryAndType(userId, beneficiaryId, PageRequest.of(0, 1));
        if (results.isEmpty()) {
            return Optional.empty();
        }
        Object[] firstResult = results.get(0);
        return Optional.of(TransactionSuggestion.builder()
                .categoryId((Long) firstResult[0])
                .transactionType((TransactionType) firstResult[1])
                .build());
    }
}

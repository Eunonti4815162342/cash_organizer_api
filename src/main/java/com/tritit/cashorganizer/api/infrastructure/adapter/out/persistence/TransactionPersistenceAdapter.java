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
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionPersistencePort {

    private final TransactionRepository transactionRepository;
    private final PersistenceMapper mapper;

    @Override
    @Transactional
    public TransactionItem save(TransactionItem transaction) {
        TransactionItemEntity entity = mapper.toEntity(transaction);
        TransactionItemEntity savedEntity = transactionRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public void delete(TransactionItem transaction) {
        transactionRepository.delete(mapper.toEntity(transaction));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        transactionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> findAllByUser(User user, Pageable pageable) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.findAllByUser(userEntity, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionItem> findById(Long id) {
        return transactionRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> findAllByUserAndDateRange(User user, String startDate, String endDate, Pageable pageable) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.findAllByUserAndDateRange(userEntity, startDate, endDate, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> findAllByUserAndAccountAndDateRange(User user, List<Long> accountIds, String startDate, String endDate, Pageable pageable) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.findAllByUserAndAccountAndDateRange(userEntity, accountIds, startDate, endDate, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional
    public void unlinkSubcategoryFromTransactions(User user, Long subcategoryId) {
        UserEntity userEntity = mapper.toEntity(user);
        transactionRepository.unlinkSubcategoryFromTransactions(userEntity, subcategoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TransactionSuggestion> findMostFrequentCategoryAndType(UUID userId, Long beneficiaryId) {
        List<Object[]> results = transactionRepository.findMostFrequentCategoryAndType(userId, beneficiaryId, PageRequest.of(0, 1));
        if (results.isEmpty()) {
            return Optional.empty();
        }
        Object[] firstResult = results.get(0);
        return Optional.of(TransactionSuggestion.builder()
                .categoryId((Long) firstResult[0])
                .subcategoryId((Long) firstResult[1])
                .transactionType((TransactionType) firstResult[2])
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionItem> findAllForReport(User user, String startDate, String endDate) {
        UserEntity userEntity = mapper.toEntity(user);
        return transactionRepository.findAllForReport(userEntity, startDate, endDate).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

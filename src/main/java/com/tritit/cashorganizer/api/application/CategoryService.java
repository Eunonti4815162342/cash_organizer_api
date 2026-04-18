package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.in.CategoryUseCase;
import com.tritit.cashorganizer.api.domain.port.out.CategoryPersistencePort;
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
public class CategoryService implements CategoryUseCase {

    private final CategoryPersistencePort categoryPersistencePort;
    private final TransactionPersistencePort transactionPersistencePort;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategories() {
        User user = userContextPort.getCurrentUser();
        return categoryPersistencePort.findAllByUser(user);
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        User user = userContextPort.getCurrentUser();
        category.setUser(user);
        return categoryPersistencePort.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionItem> getTransactionsByCategory(Long categoryId, Pageable pageable) {
        User user = userContextPort.getCurrentUser();
        return transactionPersistencePort.findAllByUserAndCategory(user, categoryId, pageable);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        User user = userContextPort.getCurrentUser();
        categoryPersistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (transactionPersistencePort.countByUserAndCategory(user, id) > 0) {
            throw new InvalidTransactionException("Cannot delete category with linked transactions. Please reassign them first.");
        }

        categoryPersistencePort.delete(id);
    }

    @Override
    @Transactional
    public void deleteSubcategory(Long subcategoryId) {
        User user = userContextPort.getCurrentUser();

        List<TransactionItem> transactions = transactionPersistencePort.findAllByUser(user, Pageable.unpaged()).getContent();

        List<TransactionItem> linkedTransactions = transactions.stream()
                .filter(t -> t.getSubcategory() != null && t.getSubcategory().getId().equals(subcategoryId))
                .toList();

        for (TransactionItem tx : linkedTransactions) {
            transactionPersistencePort.save(tx);
        }

        categoryPersistencePort.deleteSubcategoryById(subcategoryId);
    }
}


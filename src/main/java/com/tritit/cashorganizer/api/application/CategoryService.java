package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.in.CategoryUseCase;
import com.tritit.cashorganizer.api.domain.port.out.CategoryPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.FinancialEntityPersistencePort;
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
    private final FinancialEntityPersistencePort financialEntityPersistencePort;
    private final UserContextPort userContextPort;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getCategories(Long financialEntityId) {
        User user = userContextPort.getCurrentUser();
        
        if (financialEntityId != null) {
            var financialEntity = financialEntityPersistencePort.findById(financialEntityId)
                    .filter(e -> e.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Financial Entity not found or does not belong to user"));
            return categoryPersistencePort.findAllByUserAndFinancialEntity(user, financialEntity);
        }
        
        return categoryPersistencePort.findAllByUser(user);
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        User user = userContextPort.getCurrentUser();
        category.setUser(user);

        if (category.getFinancialEntity() != null && category.getFinancialEntity().getId() != null) {
            var entity = financialEntityPersistencePort.findById(category.getFinancialEntity().getId())
                    .filter(e -> e.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Financial Entity not found or does not belong to user"));
            category.setFinancialEntity(entity);
        }

        return categoryPersistencePort.save(category);
    }

    @Override
    @Transactional
    public Subcategory createSubcategory(Long categoryId, Subcategory subcategory) {
        Category parent = categoryPersistencePort.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));
        subcategory.setCategory(parent);
        return categoryPersistencePort.saveSubcategory(subcategory);
    }

    @Override
    @Transactional
    public Subcategory updateSubcategory(Long id, Subcategory subcategoryDetails) {
        Subcategory existing = categoryPersistencePort.findSubcategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found"));
        existing.setName(subcategoryDetails.getName());
        return categoryPersistencePort.saveSubcategory(existing);
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
        transactionPersistencePort.unlinkSubcategoryFromTransactions(user, subcategoryId);
        categoryPersistencePort.deleteSubcategoryById(subcategoryId);
    }
}


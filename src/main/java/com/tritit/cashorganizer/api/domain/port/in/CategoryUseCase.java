package com.tritit.cashorganizer.api.domain.port.in;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CategoryUseCase {
    List<Category> getCategories(Long financialEntityId);
    Category createCategory(Category category);
    Subcategory createSubcategory(Long categoryId, Subcategory subcategory);
    Subcategory updateSubcategory(Long id, Subcategory subcategory);
    Page<TransactionItem> getTransactionsByCategory(Long categoryId, Pageable pageable);
    void deleteCategory(Long id);
    void deleteSubcategory(Long subcategoryId);
}

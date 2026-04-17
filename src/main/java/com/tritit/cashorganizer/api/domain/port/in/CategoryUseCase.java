package com.tritit.cashorganizer.api.domain.port.in;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import java.util.List;

public interface CategoryUseCase {
    List<Category> getCategories();
    Category createCategory(Category category);
    List<TransactionItem> getTransactionsByCategory(Long categoryId);
    void deleteCategory(Long id);
    void deleteSubcategory(Long subcategoryId);
}

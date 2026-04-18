package com.tritit.cashorganizer.api.domain.port.in;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CategoryUseCase {
    List<Category> getCategories();
    Category createCategory(Category category);
    Page<TransactionItem> getTransactionsByCategory(Long categoryId, Pageable pageable);
    void deleteCategory(Long id);
    void deleteSubcategory(Long subcategoryId);
}

package com.tritit.cashorganizer.api.domain.port.out;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import com.tritit.cashorganizer.api.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface CategoryPersistencePort {
    List<Category> findAllByUser(User user);
    List<Category> findAllByUserAndFinancialEntity(User user, FinancialEntity entity);
    Optional<Category> findById(Long id);
    Category save(Category category);
    void delete(Long id);
    
    Subcategory saveSubcategory(Subcategory subcategory);
    Optional<Subcategory> findSubcategoryById(Long id);
    void deleteSubcategoryById(Long subcategoryId);
}

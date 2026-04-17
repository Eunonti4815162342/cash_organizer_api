package com.tritit.cashorganizer.api.domain.port.out;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface CategoryPersistencePort {
    List<Category> findAllByUser(User user);
    Optional<Category> findById(Long id);
    Category save(Category category);
    void delete(Long id);
    void deleteSubcategoryById(Long subcategoryId);
}

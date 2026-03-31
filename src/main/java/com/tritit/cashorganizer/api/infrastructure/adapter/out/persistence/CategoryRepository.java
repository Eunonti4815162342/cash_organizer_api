package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    @Query("SELECT c FROM Category c ORDER BY c.type DESC, c.name ASC")
    List<Category> findAllSorted();

    List<Category> findByType(Category.CategoryType type);
}

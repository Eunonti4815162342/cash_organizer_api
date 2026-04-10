package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    java.util.List<Category> findAllByUser(com.tritit.cashorganizer.api.domain.model.User user);

    @Query("SELECT c FROM Category c WHERE c.user = :user ORDER BY c.type DESC, c.name ASC")
    java.util.List<Category> findAllByUserSorted(@org.springframework.data.repository.query.Param("user") com.tritit.cashorganizer.api.domain.model.User user);

    java.util.List<Category> findByUserAndType(com.tritit.cashorganizer.api.domain.model.User user, com.tritit.cashorganizer.api.domain.model.Category.CategoryType type);
}

package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.CategoryRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository userRepository;

    private com.tritit.cashorganizer.api.domain.model.User getCurrentUser() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Transactional
    public java.util.List<com.tritit.cashorganizer.api.domain.model.Category> getAllCategories() {
        com.tritit.cashorganizer.api.domain.model.User user = getCurrentUser();
        return categoryRepository.findAllByUser(user);
    }

    @Transactional
    public Category createCategory(Category category) {
        com.tritit.cashorganizer.api.domain.model.User user = getCurrentUser();
        category.setUser(user);
        
        boolean exists = categoryRepository.findAllByUser(user).stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(category.getName()) && c.getType() == category.getType());
        
        if (exists) {
            throw new RuntimeException("A category with this name already exists for this type.");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, Category details) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Check uniqueness if name or type changed
        boolean nameChanged = !category.getName().equalsIgnoreCase(details.getName());
        boolean typeChanged = category.getType() != details.getType();

        if (nameChanged || typeChanged) {
            boolean exists = categoryRepository.findAll().stream()
                    .anyMatch(c -> !c.getId().equals(id) && 
                                   c.getName().equalsIgnoreCase(details.getName()) && 
                                   c.getType() == details.getType());
            if (exists) throw new RuntimeException("A category with this name and type already exists.");
        }

        category.setName(details.getName());
        category.setType(details.getType()); // Update type
        category.setIconName(details.getIconName());
        return categoryRepository.save(category);
    }

    @Transactional
    public Subcategory updateSubcategory(Long id, Subcategory details) {
        Subcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subcategory not found"));

        // Check uniqueness within parent
        if (!subcategory.getName().equalsIgnoreCase(details.getName())) {
            boolean exists = subcategory.getCategory().getSubcategories().stream()
                    .anyMatch(s -> s.getName().equalsIgnoreCase(details.getName()));
            if (exists) throw new RuntimeException("A subcategory with this name already exists.");
        }

        subcategory.setName(details.getName());
        subcategory.setIconName(details.getIconName());
        return subcategoryRepository.save(subcategory);
    }

    @Transactional
    public Subcategory createSubcategory(Long categoryId, Subcategory subcategory) {
        Category parent = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Parent category not found"));

        boolean exists = parent.getSubcategories().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(subcategory.getName()));

        if (exists) {
            throw new RuntimeException("A subcategory with this name already exists in this category.");
        }

        subcategory.setCategory(parent);
        return subcategoryRepository.save(subcategory);
    }
}

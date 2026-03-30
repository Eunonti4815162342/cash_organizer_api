package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.CategoryRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.SubcategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository repository;
    private final SubcategoryRepository subcategoryRepository;

    @GetMapping
    public List<Category> getAllCategories() {
        return repository.findAll();
    }

    @GetMapping("/type/{type}")
    public List<Category> getCategoriesByType(@PathVariable Category.CategoryType type) {
        return repository.findByType(type);
    }

    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        return repository.save(category);
    }

    @PostMapping("/{categoryId}/subcategories")
    public Subcategory createSubcategory(@PathVariable Long categoryId, @RequestBody Subcategory subcategory) {
        return repository.findById(categoryId).map(category -> {
            subcategory.setCategory(category);
            return subcategoryRepository.save(subcategory);
        }).orElseThrow(() -> new RuntimeException("Category not found"));
    }
}

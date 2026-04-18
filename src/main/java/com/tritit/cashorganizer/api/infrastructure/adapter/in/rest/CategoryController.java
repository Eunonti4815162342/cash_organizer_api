package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.CategoryService;
import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    public List<Category> getCategories() {
        return service.getCategories();
    }

    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        return service.createCategory(category);
    }

    @DeleteMapping("/subcategories/{id}")
    public void deleteSubcategory(@PathVariable Long id) {
        service.deleteSubcategory(id);
    }

    @GetMapping("/{id}/transactions")
    public List<com.tritit.cashorganizer.api.domain.model.TransactionItem> getTransactionsByCategory(@PathVariable Long id) {
        return service.getTransactionsByCategory(id);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
    }
}

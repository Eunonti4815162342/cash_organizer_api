package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.CategoryService;
import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Page<TransactionItem> getTransactionsByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        return service.getTransactionsByCategory(id, pageable);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
    }
}

package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.CategoryService;
import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
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

    // Nota: Las rutas de subcategorías se pueden añadir aquí si el servicio las soporta
}

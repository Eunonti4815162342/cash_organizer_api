package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.CategoryRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.SubcategoryRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.SubcategoryEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.TransactionItemEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.CategoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PersistenceMapper mapper;

    private UserEntity getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @Transactional(readOnly = true)
    public List<Category> getCategories() {
        UserEntity user = getCurrentUserEntity();
        return categoryRepository.findAllByUser(user).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public Category createCategory(Category category) {
        UserEntity user = getCurrentUserEntity();
        CategoryEntity entity = mapper.toEntity(category);
        entity.setUser(user);
        return mapper.toDomain(categoryRepository.save(entity));
    }

    public List<TransactionItem> getTransactionsByCategory(Long categoryId) {
        UserEntity user = getCurrentUserEntity();
        
        // Obtener todas las transacciones del usuario
        List<TransactionItemEntity> allTransactions = transactionRepository.findAllByUser(user, Pageable.unpaged()).getContent();
        
        // Filtrar las que pertenecen a esta categoría o a cualquiera de sus subcategorías
        return allTransactions.stream()
                .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCategory(Long id) {
        UserEntity user = getCurrentUserEntity();
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if (!category.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // REGLA: No se puede borrar si tiene transacciones
        List<TransactionItem> linkedTransactions = getTransactionsByCategory(id);
        if (!linkedTransactions.isEmpty()) {
            throw new RuntimeException("Cannot delete category with linked transactions. Please reassign them first.");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public void deleteSubcategory(Long subcategoryId) {
        UserEntity user = getCurrentUserEntity();
        
        SubcategoryEntity subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new RuntimeException("Subcategory not found"));

        // Verificar que la subcategoría pertenece a una categoría del usuario
        if (!subcategory.getCategory().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this subcategory");
        }

        // 1. Buscar transacciones asociadas a esta subcategoría
        // Usamos Pageable.unpaged() porque queremos procesar todas para la migración
        List<TransactionItemEntity> transactions = transactionRepository.findAllByUser(user, Pageable.unpaged()).getContent();
        
        List<TransactionItemEntity> linkedTransactions = transactions.stream()
                .filter(t -> t.getSubcategory() != null && t.getSubcategory().getId().equals(subcategoryId))
                .collect(Collectors.toList());

        // 2. Reasignar a la categoría padre y poner subcategoría a null
        for (TransactionItemEntity tx : linkedTransactions) {
            tx.setCategory(subcategory.getCategory());
            tx.setSubcategory(null);
            transactionRepository.save(tx);
        }

        // 3. Eliminar físicamente la subcategoría
        subcategoryRepository.delete(subcategory);
    }
}

package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.CategoryRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.CategoryEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
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
}

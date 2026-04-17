package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence;

import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.CategoryPersistencePort;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryPersistencePort {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final PersistenceMapper mapper;

    @Override
    public List<Category> findAllByUser(User user) {
        UserEntity userEntity = mapper.toEntity(user);
        return categoryRepository.findAllByUser(userEntity).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Category save(Category category) {
        var entity = mapper.toEntity(category);
        return mapper.toDomain(categoryRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public void deleteSubcategoryById(Long subcategoryId) {
        subcategoryRepository.deleteById(subcategoryId);
    }
}

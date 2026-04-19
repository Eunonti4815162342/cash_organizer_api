package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.CategoryPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
class CategoryServiceTest {

    @Mock CategoryPersistencePort categoryPersistencePort;
    @Mock TransactionPersistencePort transactionPersistencePort;
    @Mock UserContextPort userContextPort;

    @InjectMocks
    CategoryService service;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(UUID.randomUUID()).email("user@test.com").build();
        when(userContextPort.getCurrentUser()).thenReturn(currentUser);
    }

    @Nested
    @DisplayName("getCategories()")
    class GetCategories {

        @Test
        void returnsAllCategoriesForUser() {
            Category cat1 = Category.builder().id(1L).name("Alimentación").build();
            Category cat2 = Category.builder().id(2L).name("Transporte").build();
            when(categoryPersistencePort.findAllByUser(currentUser)).thenReturn(List.of(cat1, cat2));

            List<Category> result = service.getCategories();

            assertThat(result).containsExactly(cat1, cat2);
        }

        @Test
        void returnsEmptyWhenNone() {
            when(categoryPersistencePort.findAllByUser(currentUser)).thenReturn(List.of());
            assertThat(service.getCategories()).isEmpty();
        }
    }

    @Nested
    @DisplayName("createCategory()")
    class CreateCategory {

        @Test
        void setsUserAndSaves() {
            Category input = Category.builder().name("Ocio").build();
            Category saved = Category.builder().id(5L).name("Ocio").user(currentUser).build();
            when(categoryPersistencePort.save(any())).thenReturn(saved);

            Category result = service.createCategory(input);

            assertThat(input.getUser()).isEqualTo(currentUser);
            assertThat(result.getId()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("createSubcategory()")
    class CreateSubcategory {

        @Test
        void linksToParentAndSaves() {
            Category parent = Category.builder().id(1L).name("Alimentación").build();
            Subcategory sub = Subcategory.builder().name("Supermercado").build();
            when(categoryPersistencePort.findById(1L)).thenReturn(Optional.of(parent));
            when(categoryPersistencePort.saveSubcategory(any())).thenAnswer(inv -> inv.getArgument(0));

            Subcategory result = service.createSubcategory(1L, sub);

            assertThat(result.getCategory()).isEqualTo(parent);
        }

        @Test
        void throwsWhenParentNotFound() {
            when(categoryPersistencePort.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createSubcategory(99L, Subcategory.builder().name("sub").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateSubcategory()")
    class UpdateSubcategory {

        @Test
        void updatesNameAndSaves() {
            Subcategory existing = Subcategory.builder().id(1L).name("OldName").build();
            when(categoryPersistencePort.findSubcategoryById(1L)).thenReturn(Optional.of(existing));
            when(categoryPersistencePort.saveSubcategory(any())).thenAnswer(inv -> inv.getArgument(0));

            Subcategory updated = service.updateSubcategory(1L, Subcategory.builder().name("NewName").build());

            assertThat(updated.getName()).isEqualTo("NewName");
        }

        @Test
        void throwsWhenSubcategoryNotFound() {
            when(categoryPersistencePort.findSubcategoryById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateSubcategory(99L, Subcategory.builder().name("x").build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteCategory()")
    class DeleteCategory {

        @Test
        void deletesWhenNoTransactionsLinked() {
            when(categoryPersistencePort.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).build()));
            when(transactionPersistencePort.countByUserAndCategory(currentUser, 1L)).thenReturn(0L);

            service.deleteCategory(1L);

            verify(categoryPersistencePort).delete(1L);
        }

        @Test
        void throwsWhenTransactionsLinked() {
            when(categoryPersistencePort.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).build()));
            when(transactionPersistencePort.countByUserAndCategory(currentUser, 1L)).thenReturn(3L);

            assertThatThrownBy(() -> service.deleteCategory(1L))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("reassign");

            verify(categoryPersistencePort, never()).delete(any());
        }

        @Test
        void throwsWhenCategoryNotFound() {
            when(categoryPersistencePort.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteCategory(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteSubcategory()")
    class DeleteSubcategory {

        @Test
        void unlinksFromTransactionsAndDeletes() {
            service.deleteSubcategory(5L);

            verify(transactionPersistencePort).unlinkSubcategoryFromTransactions(currentUser, 5L);
            verify(categoryPersistencePort).deleteSubcategoryById(5L);
        }
    }
}

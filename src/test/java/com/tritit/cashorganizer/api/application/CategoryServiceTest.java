package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.Category;
import com.tritit.cashorganizer.api.domain.model.FinancialEntity;
import com.tritit.cashorganizer.api.domain.model.Subcategory;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.CategoryPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.FinancialEntityPersistencePort;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
    @Mock FinancialEntityPersistencePort financialEntityPersistencePort;
    @Mock UserContextPort userContextPort;

    @InjectMocks
    CategoryService service;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(UUID.randomUUID()).email("user@test.com").build();
    }

    @Nested
    @DisplayName("getCategories()")
    class GetCategories {

        @Test
        void returnsAllCategoriesForUser() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            Category cat1 = Category.builder().id(1L).name("Alimentación").build();
            Category cat2 = Category.builder().id(2L).name("Transporte").build();
            when(categoryPersistencePort.findAllByUser(currentUser)).thenReturn(List.of(cat1, cat2));

            List<Category> result = service.getCategories(null);

            assertThat(result).containsExactly(cat1, cat2);
        }

        @Test
        @DisplayName("getCategories: should filter by financial entity when provided")
        void returnsFilteredCategoriesForUser() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            FinancialEntity entity = FinancialEntity.builder().id(10L).user(currentUser).build();
            Category cat1 = Category.builder().id(1L).name("Empresa 1").financialEntity(entity).build();
            
            when(financialEntityPersistencePort.findById(10L)).thenReturn(Optional.of(entity));
            when(categoryPersistencePort.findAllByUserAndFinancialEntity(currentUser, entity)).thenReturn(List.of(cat1));

            List<Category> result = service.getCategories(10L);

            assertThat(result).containsExactly(cat1);
            verify(categoryPersistencePort).findAllByUserAndFinancialEntity(currentUser, entity);
        }

        @Test
        @DisplayName("getCategories: should throw exception when entity belongs to another user")
        void throwsWhenEntityIsUnauthorized() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            FinancialEntity entity = FinancialEntity.builder().id(10L).user(otherUser).build();
            
            when(financialEntityPersistencePort.findById(10L)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.getCategories(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not belong to user");
        }

        @Test
        void returnsEmptyWhenNone() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            when(categoryPersistencePort.findAllByUser(currentUser)).thenReturn(List.of());
            assertThat(service.getCategories(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("createCategory()")
    class CreateCategory {

        @Test
        void setsUserAndSaves() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            Category input = Category.builder().name("Ocio").build();
            Category saved = Category.builder().id(5L).name("Ocio").user(currentUser).build();
            when(categoryPersistencePort.save(any())).thenReturn(saved);

            Category result = service.createCategory(input);

            assertThat(input.getUser()).isEqualTo(currentUser);
            assertThat(result.getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("createCategory: should link to financial entity when provided and valid")
        void createCategoryWithFinancialEntity() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            FinancialEntity entity = FinancialEntity.builder().id(10L).user(currentUser).build();
            Category input = Category.builder().name("Empresa").financialEntity(entity).build();
            
            when(financialEntityPersistencePort.findById(10L)).thenReturn(Optional.of(entity));
            when(categoryPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Category result = service.createCategory(input);

            assertThat(result.getFinancialEntity()).isEqualTo(entity);
            verify(financialEntityPersistencePort).findById(10L);
        }

        @Test
        @DisplayName("createCategory: should throw exception when entity belongs to another user")
        void createCategoryWithUnauthorizedEntity() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            FinancialEntity entity = FinancialEntity.builder().id(10L).user(otherUser).build();
            Category input = Category.builder().name("Empresa").financialEntity(entity).build();
            
            when(financialEntityPersistencePort.findById(10L)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.createCategory(input))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("not belong to user");
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
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            when(categoryPersistencePort.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).build()));
            when(transactionPersistencePort.countByUserAndCategory(currentUser, 1L)).thenReturn(0L);

            service.deleteCategory(1L);

            verify(categoryPersistencePort).delete(1L);
        }

        @Test
        void throwsWhenTransactionsLinked() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            when(categoryPersistencePort.findById(1L)).thenReturn(Optional.of(Category.builder().id(1L).build()));
            when(transactionPersistencePort.countByUserAndCategory(currentUser, 1L)).thenReturn(3L);

            assertThatThrownBy(() -> service.deleteCategory(1L))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("reassign");

            verify(categoryPersistencePort, never()).delete(any());
        }

        @Test
        void throwsWhenCategoryNotFound() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            when(categoryPersistencePort.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteCategory(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTransactionsByCategory()")
    class GetTransactionsByCategory {

        @Test
        void returnsPagedTransactions() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            Pageable pageable = mock(Pageable.class);
            Page<TransactionItem> page = new PageImpl<>(List.of());
            when(transactionPersistencePort.findAllByUserAndCategory(currentUser, 1L, pageable)).thenReturn(page);

            Page<TransactionItem> result = service.getTransactionsByCategory(1L, pageable);

            assertThat(result).isEqualTo(page);
        }
    }
}

package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.application.impact.TransactionImpact;
import com.tritit.cashorganizer.api.application.impact.TransactionImpactResolver;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.*;
import com.tritit.cashorganizer.api.domain.port.out.BeneficiaryPersistencePort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @Mock TransactionPersistencePort transactionPersistencePort;
    @Mock UserContextPort userContextPort;
    @Mock BeneficiaryPersistencePort beneficiaryPersistencePort;
    @Mock TransactionImpactResolver impactResolver;

    @InjectMocks
    TransactionService sut;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(UUID.randomUUID()).email("veiga@test.com").build();
    }

    @Nested
    @DisplayName("getSuggestionForBeneficiary()")
    class GetSuggestion {

        @Test
        @DisplayName("should return suggestion from history")
        void returnsSuggestion() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            TransactionSuggestion suggestion = TransactionSuggestion.builder()
                    .categoryId(1L)
                    .transactionType(TransactionItem.TransactionType.EXPENSE)
                    .build();

            when(beneficiaryPersistencePort.existsByIdAndUser(5L, currentUser)).thenReturn(true);
            when(transactionPersistencePort.findMostFrequentCategoryAndType(currentUser.getId(), 5L))
                    .thenReturn(Optional.of(suggestion));

            TransactionSuggestion result = sut.getSuggestionForBeneficiary(5L);

            assertThat(result).isEqualTo(suggestion);
        }

        @Test
        @DisplayName("should return empty suggestion when no history exists")
        void returnsEmptyWhenNoHistory() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            when(beneficiaryPersistencePort.existsByIdAndUser(5L, currentUser)).thenReturn(true);
            when(transactionPersistencePort.findMostFrequentCategoryAndType(currentUser.getId(), 5L))
                    .thenReturn(Optional.empty());

            TransactionSuggestion result = sut.getSuggestionForBeneficiary(5L);

            assertThat(result.getCategoryId()).isNull();
            assertThat(result.getTransactionType()).isNull();
        }

        @Test
        @DisplayName("should throw exception when beneficiary not found or unauthorized")
        void throwsWhenUnauthorized() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            when(beneficiaryPersistencePort.existsByIdAndUser(5L, currentUser)).thenReturn(false);

            assertThatThrownBy(() -> sut.getSuggestionForBeneficiary(5L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllTransactions()")
    class GetAll {
        @Test
        void callsPersistence() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            Pageable pageable = mock(Pageable.class);
            Page<TransactionItem> page = new PageImpl<>(List.of());
            when(transactionPersistencePort.findAllByUser(currentUser, pageable)).thenReturn(page);

            assertThat(sut.getAllTransactions(pageable)).isEqualTo(page);
        }
    }

    @Nested
    @DisplayName("createTransaction()")
    class Create {
        @Test
        void validatesAndSaves() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.INCOME)
                    .account(AccountItem.builder().id(1L).build())
                    .amount(new Amount(100L, "EUR", false))
                    .build();
            
            TransactionImpact impactMock = mock(TransactionImpact.class);
            when(impactResolver.forType(TransactionItem.TransactionType.INCOME)).thenReturn(impactMock);
            when(transactionPersistencePort.save(tx)).thenReturn(tx);

            TransactionItem result = sut.createTransaction(tx);

            assertThat(result.getUser()).isEqualTo(currentUser);
            verify(impactMock).apply(tx, currentUser);
            verify(transactionPersistencePort).save(tx);
        }
    }

    @Nested
    @DisplayName("deleteTransaction()")
    class Delete {
        @Test
        void revertsImpactAndDelete() {
            when(userContextPort.getCurrentUser()).thenReturn(currentUser);
            TransactionItem tx = TransactionItem.builder()
                    .id(10L)
                    .user(currentUser)
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .build();
            
            TransactionImpact impactMock = mock(TransactionImpact.class);
            when(impactResolver.forType(TransactionItem.TransactionType.EXPENSE)).thenReturn(impactMock);
            when(transactionPersistencePort.findById(10L)).thenReturn(Optional.of(tx));

            sut.deleteTransaction(10L);

            verify(impactMock).revert(tx, currentUser);
            verify(transactionPersistencePort).deleteById(10L);
        }
    }
}

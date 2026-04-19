package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.application.impact.TransactionImpactResolver;
import com.tritit.cashorganizer.api.application.impact.TransactionImpact;
import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
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
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @Mock TransactionPersistencePort transactionPersistencePort;
    @Mock UserContextPort userContextPort;
    @Mock TransactionImpactResolver impactResolver;
    @Mock TransactionImpact impact;

    @InjectMocks
    TransactionService service;

    private User currentUser;
    private AccountItem account;
    private Amount amount;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(UUID.randomUUID()).email("user@test.com").build();
        account = AccountItem.builder().id(1L).name("Cuenta").build();
        amount = new Amount(5000L, "EUR", false);
        when(userContextPort.getCurrentUser()).thenReturn(currentUser);
        when(impactResolver.forType(any())).thenReturn(impact);
    }

    @Nested
    @DisplayName("getAllTransactions()")
    class GetAll {

        @Test
        void delegatesToPort() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<TransactionItem> expected = new PageImpl<>(List.of());
            when(transactionPersistencePort.findAllByUser(currentUser, pageable)).thenReturn(expected);

            Page<TransactionItem> result = service.getAllTransactions(pageable);

            assertThat(result).isSameAs(expected);
            verify(transactionPersistencePort).findAllByUser(currentUser, pageable);
        }
    }

    @Nested
    @DisplayName("getTransactionsByDateRange()")
    class GetByDateRange {

        @Test
        void passesDateRangeToPort() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<TransactionItem> page = new PageImpl<>(List.of());
            when(transactionPersistencePort.findAllByUserAndDateRange(currentUser, "2024-01-01", "2024-01-31", pageable))
                    .thenReturn(page);

            Page<TransactionItem> result = service.getTransactionsByDateRange("2024-01-01", "2024-01-31", pageable);
            assertThat(result).isSameAs(page);
        }
    }

    @Nested
    @DisplayName("getTransactionsByAccountAndDateRange()")
    class GetByAccountAndDateRange {

        @Test
        void passesAccountIdsAndDatesToPort() {
            List<Long> ids = List.of(1L, 2L);
            Pageable pageable = PageRequest.of(0, 20);
            Page<TransactionItem> page = new PageImpl<>(List.of());
            when(transactionPersistencePort.findAllByUserAndAccountAndDateRange(currentUser, ids, "2024-01-01", "2024-01-31", pageable))
                    .thenReturn(page);

            service.getTransactionsByAccountAndDateRange(ids, "2024-01-01", "2024-01-31", pageable);
            verify(transactionPersistencePort).findAllByUserAndAccountAndDateRange(currentUser, ids, "2024-01-01", "2024-01-31", pageable);
        }
    }

    @Nested
    @DisplayName("createTransaction()")
    class Create {

        @Test
        void setsUserValidatesAppliesImpactAndSaves() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account)
                    .amount(amount)
                    .build();
            when(transactionPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTransaction(tx);

            assertThat(tx.getUser()).isEqualTo(currentUser);
            verify(impact).apply(tx, currentUser);
            verify(transactionPersistencePort).save(tx);
        }

        @Test
        void throwsWhenAccountIsNull() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .amount(amount)
                    .build();
            assertThatThrownBy(() -> service.createTransaction(tx))
                    .isInstanceOf(InvalidTransactionException.class);
            verify(transactionPersistencePort, never()).save(any());
        }

        @Test
        void throwsWhenAmountIsZero() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account)
                    .amount(new Amount(0L, "EUR", false))
                    .build();
            assertThatThrownBy(() -> service.createTransaction(tx))
                    .isInstanceOf(InvalidTransactionException.class);
        }

        @Test
        void resolveImpactForCorrectType() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.INCOME)
                    .account(account).amount(amount).build();
            when(transactionPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createTransaction(tx);

            verify(impactResolver).forType(TransactionItem.TransactionType.INCOME);
        }
    }

    @Nested
    @DisplayName("updateTransaction()")
    class Update {

        @Test
        void revertsOldImpactAndAppliesNew() {
            TransactionItem old = TransactionItem.builder()
                    .id(1L).user(currentUser)
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account).amount(amount).build();
            TransactionItem newTx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account).amount(new Amount(3000L, "EUR", false)).build();

            when(transactionPersistencePort.findById(1L)).thenReturn(Optional.of(old));
            when(transactionPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateTransaction(1L, newTx);

            verify(impact).revert(old, currentUser);
            verify(impact).apply(newTx, currentUser);
        }

        @Test
        void throwsWhenTransactionNotFound() {
            when(transactionPersistencePort.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateTransaction(99L, TransactionItem.builder().account(account).amount(amount).build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsWhenTransactionBelongsToDifferentUser() {
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            TransactionItem other = TransactionItem.builder().id(1L).user(otherUser).build();
            when(transactionPersistencePort.findById(1L)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.updateTransaction(1L,
                    TransactionItem.builder().account(account).amount(amount).type(TransactionItem.TransactionType.EXPENSE).build()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void preservesIdFromPath() {
            TransactionItem old = TransactionItem.builder()
                    .id(7L).user(currentUser)
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account).amount(amount).build();
            when(transactionPersistencePort.findById(7L)).thenReturn(Optional.of(old));
            when(transactionPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TransactionItem updated = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account).amount(amount).build();
            service.updateTransaction(7L, updated);

            assertThat(updated.getId()).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("deleteTransaction()")
    class Delete {

        @Test
        void revertsImpactAndDeletes() {
            TransactionItem tx = TransactionItem.builder()
                    .id(1L).user(currentUser)
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account).amount(amount).build();
            when(transactionPersistencePort.findById(1L)).thenReturn(Optional.of(tx));

            service.deleteTransaction(1L);

            verify(impact).revert(tx, currentUser);
            verify(transactionPersistencePort).deleteById(1L);
        }

        @Test
        void throwsWhenNotFound() {
            when(transactionPersistencePort.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.deleteTransaction(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void throwsWhenNotOwner() {
            User other = User.builder().id(UUID.randomUUID()).build();
            TransactionItem tx = TransactionItem.builder().id(1L).user(other).build();
            when(transactionPersistencePort.findById(1L)).thenReturn(Optional.of(tx));

            assertThatThrownBy(() -> service.deleteTransaction(1L))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(transactionPersistencePort, never()).deleteById(any());
        }
    }
}

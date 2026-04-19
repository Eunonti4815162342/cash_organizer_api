package com.tritit.cashorganizer.api.application.impact;

import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import com.tritit.cashorganizer.api.domain.exception.ResourceNotFoundException;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Impact strategies — balance effects")
class ImpactStrategyTest {

    @Mock AccountPersistencePort accountPort;

    private User user;
    private AccountItem fromAccount;
    private AccountItem toAccount;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("user@test.com").build();
        fromAccount = AccountItem.builder()
                .id(1L).user(user).amount(new Amount(10000L, "EUR", false)).build();
        toAccount = AccountItem.builder()
                .id(2L).user(user).amount(new Amount(5000L, "EUR", false)).build();
    }

    private TransactionItem expenseOf(long value) {
        return TransactionItem.builder()
                .type(TransactionItem.TransactionType.EXPENSE)
                .account(fromAccount)
                .amount(new Amount(value, "EUR", false))
                .build();
    }

    private TransactionItem incomeOf(long value) {
        return TransactionItem.builder()
                .type(TransactionItem.TransactionType.INCOME)
                .account(fromAccount)
                .amount(new Amount(value, "EUR", false))
                .build();
    }

    private TransactionItem transferOf(long value) {
        return TransactionItem.builder()
                .type(TransactionItem.TransactionType.TRANSFER)
                .account(fromAccount)
                .toAccount(toAccount)
                .amount(new Amount(value, "EUR", false))
                .build();
    }

    @Nested
    @DisplayName("ExpenseImpact")
    class ExpenseImpactTests {

        private ExpenseImpact impact;

        @BeforeEach
        void setUp() {
            impact = new ExpenseImpact(accountPort);
            when(accountPort.findById(1L)).thenReturn(Optional.of(fromAccount));
        }

        @Test
        void apply_withdrawsFromSourceAccount() {
            TransactionItem tx = expenseOf(3000L);
            impact.apply(tx, user);

            ArgumentCaptor<AccountItem> captor = ArgumentCaptor.forClass(AccountItem.class);
            verify(accountPort).save(captor.capture());
            assertThat(captor.getValue().getAmount().getValue()).isEqualTo(7000L);
        }

        @Test
        void revert_depositsBackToSourceAccount() {
            TransactionItem tx = expenseOf(3000L);
            impact.revert(tx, user);

            ArgumentCaptor<AccountItem> captor = ArgumentCaptor.forClass(AccountItem.class);
            verify(accountPort).save(captor.capture());
            assertThat(captor.getValue().getAmount().getValue()).isEqualTo(13000L);
        }

        @Test
        void apply_throwsWhenAccountNotOwned() {
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            when(accountPort.findById(1L)).thenReturn(Optional.of(fromAccount));
            // fromAccount belongs to `user`, not `otherUser`
            assertThatThrownBy(() -> impact.apply(expenseOf(100L), otherUser))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void supportedType_isExpense() {
            assertThat(impact.supportedType()).isEqualTo(TransactionItem.TransactionType.EXPENSE);
        }
    }

    @Nested
    @DisplayName("IncomeImpact")
    class IncomeImpactTests {

        private IncomeImpact impact;

        @BeforeEach
        void setUp() {
            impact = new IncomeImpact(accountPort);
            when(accountPort.findById(1L)).thenReturn(Optional.of(fromAccount));
        }

        @Test
        void apply_depositsToSourceAccount() {
            impact.apply(incomeOf(2000L), user);

            ArgumentCaptor<AccountItem> captor = ArgumentCaptor.forClass(AccountItem.class);
            verify(accountPort).save(captor.capture());
            assertThat(captor.getValue().getAmount().getValue()).isEqualTo(12000L);
        }

        @Test
        void revert_withdrawsFromSourceAccount() {
            impact.revert(incomeOf(2000L), user);

            ArgumentCaptor<AccountItem> captor = ArgumentCaptor.forClass(AccountItem.class);
            verify(accountPort).save(captor.capture());
            assertThat(captor.getValue().getAmount().getValue()).isEqualTo(8000L);
        }

        @Test
        void supportedType_isIncome() {
            assertThat(impact.supportedType()).isEqualTo(TransactionItem.TransactionType.INCOME);
        }

        @Test
        void apply_throwsWhenAccountNotOwned() {
            User otherUser = User.builder().id(UUID.randomUUID()).build();
            assertThatThrownBy(() -> impact.apply(incomeOf(100L), otherUser))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("TransferImpact")
    class TransferImpactTests {

        private TransferImpact impact;

        @BeforeEach
        void setUp() {
            impact = new TransferImpact(accountPort);
            when(accountPort.findById(1L)).thenReturn(Optional.of(fromAccount));
            when(accountPort.findById(2L)).thenReturn(Optional.of(toAccount));
        }

        @Test
        void apply_withdrawsFromSourceAndDepositsToDestination() {
            impact.apply(transferOf(4000L), user);

            assertThat(fromAccount.getAmount().getValue()).isEqualTo(6000L);
            assertThat(toAccount.getAmount().getValue()).isEqualTo(9000L);
            verify(accountPort).save(fromAccount);
            verify(accountPort).save(toAccount);
        }

        @Test
        void revert_depositsBackToSourceAndWithdrawsFromDestination() {
            impact.revert(transferOf(4000L), user);

            assertThat(fromAccount.getAmount().getValue()).isEqualTo(14000L);
            assertThat(toAccount.getAmount().getValue()).isEqualTo(1000L);
        }

        @Test
        void apply_throwsWhenToAccountIsNull() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.TRANSFER)
                    .account(fromAccount)
                    .toAccount(null)
                    .amount(new Amount(1000L, "EUR", false))
                    .build();
            assertThatThrownBy(() -> impact.apply(tx, user))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Destination");
        }

        @Test
        void revert_skipsWhenToAccountIsNull() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.TRANSFER)
                    .account(fromAccount)
                    .toAccount(null)
                    .amount(new Amount(1000L, "EUR", false))
                    .build();
            assertThatCode(() -> impact.revert(tx, user)).doesNotThrowAnyException();
        }

        @Test
        void supportedType_isTransfer() {
            assertThat(impact.supportedType()).isEqualTo(TransactionItem.TransactionType.TRANSFER);
        }
    }
}

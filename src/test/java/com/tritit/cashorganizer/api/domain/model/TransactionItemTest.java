package com.tritit.cashorganizer.api.domain.model;

import com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TransactionItem — domain model")
class TransactionItemTest {

    private AccountItem account;
    private Amount amount;

    @BeforeEach
    void setUp() {
        account = AccountItem.builder().id(1L).name("Cuenta test").build();
        amount = new Amount(1000L, "EUR", false);
    }

    @Nested
    @DisplayName("validate() — EXPENSE")
    class ValidateExpense {

        @Test
        void validExpensePasses() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account)
                    .amount(amount)
                    .build();
            assertThatCode(tx::validate).doesNotThrowAnyException();
        }

        @Test
        void expenseWithoutAccount_throws() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .amount(amount)
                    .build();
            assertThatThrownBy(tx::validate)
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("account");
        }

        @Test
        void expenseWithNullAmount_throws() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account)
                    .amount(null)
                    .build();
            assertThatThrownBy(tx::validate)
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Amount");
        }

        @Test
        void expenseWithZeroAmount_throws() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.EXPENSE)
                    .account(account)
                    .amount(new Amount(0L, "EUR", false))
                    .build();
            assertThatThrownBy(tx::validate)
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("non-zero");
        }
    }

    @Nested
    @DisplayName("validate() — INCOME")
    class ValidateIncome {

        @Test
        void validIncomePasses() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.INCOME)
                    .account(account)
                    .amount(amount)
                    .build();
            assertThatCode(tx::validate).doesNotThrowAnyException();
        }

        @Test
        void incomeWithoutAccount_throws() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.INCOME)
                    .amount(amount)
                    .build();
            assertThatThrownBy(tx::validate)
                    .isInstanceOf(InvalidTransactionException.class);
        }
    }

    @Nested
    @DisplayName("validate() — TRANSFER")
    class ValidateTransfer {

        @Test
        void validTransferWithToAccount_passes() {
            AccountItem toAccount = AccountItem.builder().id(2L).name("Destino").build();
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.TRANSFER)
                    .account(account)
                    .toAccount(toAccount)
                    .amount(amount)
                    .build();
            assertThatCode(tx::validate).doesNotThrowAnyException();
        }

        @Test
        void transferWithoutToAccount_throws() {
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.TRANSFER)
                    .account(account)
                    .amount(amount)
                    // toAccount = null
                    .build();
            assertThatThrownBy(tx::validate)
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Destination account");
        }

        @Test
        void transferWithoutSourceAccount_throws() {
            AccountItem toAccount = AccountItem.builder().id(2L).name("Destino").build();
            TransactionItem tx = TransactionItem.builder()
                    .type(TransactionItem.TransactionType.TRANSFER)
                    .toAccount(toAccount)
                    .amount(amount)
                    .build();
            assertThatThrownBy(tx::validate)
                    .isInstanceOf(InvalidTransactionException.class);
        }
    }

    @Nested
    @DisplayName("belongsTo()")
    class BelongsTo {

        @Test
        void returnsTrueForOwner() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).email("owner@test.com").build();
            TransactionItem tx = TransactionItem.builder().user(user).build();
            assertThat(tx.belongsTo(user)).isTrue();
        }

        @Test
        void returnsFalseForOtherUser() {
            User owner = User.builder().id(UUID.randomUUID()).build();
            User other = User.builder().id(UUID.randomUUID()).build();
            TransactionItem tx = TransactionItem.builder().user(owner).build();
            assertThat(tx.belongsTo(other)).isFalse();
        }

        @Test
        void returnsFalseWhenNoUser() {
            TransactionItem tx = TransactionItem.builder().build();
            User user = User.builder().id(UUID.randomUUID()).build();
            assertThat(tx.belongsTo(user)).isFalse();
        }
    }
}

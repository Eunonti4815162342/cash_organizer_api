package com.tritit.cashorganizer.api.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AccountItem — domain model")
class AccountItemTest {

    private AccountItem account;
    private Amount initialAmount;

    @BeforeEach
    void setUp() {
        initialAmount = new Amount(10000L, "EUR", false);
        account = AccountItem.builder()
                .id(1L)
                .name("Cuenta corriente")
                .amount(initialAmount)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("withdraw()")
    class Withdraw {

        @Test
        void decreasesBalance() {
            account.withdraw(3000L);
            assertThat(account.getAmount().getValue()).isEqualTo(7000L);
        }

        @Test
        void withdrawEntireBalance() {
            account.withdraw(10000L);
            assertThat(account.getAmount().getValue()).isEqualTo(0L);
        }

        @Test
        void withdrawMoreThanBalance_goesNegative() {
            account.withdraw(15000L);
            assertThat(account.getAmount().getValue()).isEqualTo(-5000L);
        }

        @Test
        void withdrawZero_noChange() {
            account.withdraw(0L);
            assertThat(account.getAmount().getValue()).isEqualTo(10000L);
        }

        @Test
        void doesNothingWhenAmountIsNull() {
            account.setAmount(null);
            assertThatCode(() -> account.withdraw(500L)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("deposit()")
    class Deposit {

        @Test
        void increasesBalance() {
            account.deposit(5000L);
            assertThat(account.getAmount().getValue()).isEqualTo(15000L);
        }

        @Test
        void depositOnZeroBalance() {
            account.setAmount(new Amount(0L, "EUR", false));
            account.deposit(1000L);
            assertThat(account.getAmount().getValue()).isEqualTo(1000L);
        }

        @Test
        void depositZero_noChange() {
            account.deposit(0L);
            assertThat(account.getAmount().getValue()).isEqualTo(10000L);
        }

        @Test
        void doesNothingWhenAmountIsNull() {
            account.setAmount(null);
            assertThatCode(() -> account.deposit(500L)).doesNotThrowAnyException();
        }

        @Test
        void sequentialWithdrawAndDeposit() {
            account.withdraw(3000L);
            account.deposit(1000L);
            assertThat(account.getAmount().getValue()).isEqualTo(8000L);
        }
    }

    @Nested
    @DisplayName("belongsTo()")
    class BelongsTo {

        @Test
        void returnsTrueForOwner() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).email("owner@test.com").build();
            account.setUser(user);
            assertThat(account.belongsTo(user)).isTrue();
        }

        @Test
        void returnsFalseForDifferentUser() {
            User owner = User.builder().id(UUID.randomUUID()).email("owner@test.com").build();
            User other = User.builder().id(UUID.randomUUID()).email("other@test.com").build();
            account.setUser(owner);
            assertThat(account.belongsTo(other)).isFalse();
        }

        @Test
        void returnsFalseWhenUserIsNull() {
            account.setUser(null);
            User user = User.builder().id(UUID.randomUUID()).email("user@test.com").build();
            assertThat(account.belongsTo(user)).isFalse();
        }
    }
}

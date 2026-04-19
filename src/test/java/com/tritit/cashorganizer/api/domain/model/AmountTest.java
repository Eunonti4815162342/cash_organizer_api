package com.tritit.cashorganizer.api.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Amount — value object")
class AmountTest {

    @Nested
    @DisplayName("add()")
    class Add {

        @Test
        void addsPositiveDelta() {
            Amount amount = new Amount(1000L, "EUR", false);
            Amount result = amount.add(500L);
            assertThat(result.getValue()).isEqualTo(1500L);
        }

        @Test
        void preservesCurrency() {
            Amount amount = new Amount(1000L, "USD", false);
            assertThat(amount.add(100L).getCurrency()).isEqualTo("USD");
        }

        @Test
        void addsZero() {
            Amount amount = new Amount(1000L, "EUR", false);
            assertThat(amount.add(0L).getValue()).isEqualTo(1000L);
        }

        @Test
        void addingNegativeDeltaDecreasesValue() {
            Amount amount = new Amount(1000L, "EUR", false);
            assertThat(amount.add(-300L).getValue()).isEqualTo(700L);
        }

        @Test
        void isImmutable_originalUnchanged() {
            Amount original = new Amount(1000L, "EUR", false);
            original.add(500L);
            assertThat(original.getValue()).isEqualTo(1000L);
        }
    }

    @Nested
    @DisplayName("subtract()")
    class Subtract {

        @Test
        void subtractsPositiveDelta() {
            Amount amount = new Amount(1000L, "EUR", false);
            Amount result = amount.subtract(400L);
            assertThat(result.getValue()).isEqualTo(600L);
        }

        @Test
        void canGoNegative() {
            Amount amount = new Amount(100L, "EUR", false);
            Amount result = amount.subtract(500L);
            assertThat(result.getValue()).isEqualTo(-400L);
        }

        @Test
        void subtractZero() {
            Amount amount = new Amount(1000L, "EUR", false);
            assertThat(amount.subtract(0L).getValue()).isEqualTo(1000L);
        }

        @Test
        void preservesCurrency() {
            Amount amount = new Amount(1000L, "GBP", false);
            assertThat(amount.subtract(100L).getCurrency()).isEqualTo("GBP");
        }

        @Test
        void isImmutable_originalUnchanged() {
            Amount original = new Amount(1000L, "EUR", false);
            original.subtract(500L);
            assertThat(original.getValue()).isEqualTo(1000L);
        }
    }

    @Nested
    @DisplayName("negate()")
    class Negate {

        @Test
        void negatesPositiveValue() {
            Amount amount = new Amount(500L, "EUR", false);
            assertThat(amount.negate().getValue()).isEqualTo(-500L);
        }

        @Test
        void negatesNegativeValue() {
            Amount amount = new Amount(-300L, "EUR", false);
            assertThat(amount.negate().getValue()).isEqualTo(300L);
        }

        @Test
        void negatesZero() {
            Amount amount = new Amount(0L, "EUR", false);
            assertThat(amount.negate().getValue()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        void equalWhenSameValueAndCurrency() {
            Amount a = new Amount(1000L, "EUR", false);
            Amount b = new Amount(1000L, "EUR", false);
            assertThat(a).isEqualTo(b);
        }

        @Test
        void notEqualWhenDifferentValue() {
            Amount a = new Amount(1000L, "EUR", false);
            Amount b = new Amount(2000L, "EUR", false);
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        void notEqualWhenDifferentCurrency() {
            Amount a = new Amount(1000L, "EUR", false);
            Amount b = new Amount(1000L, "USD", false);
            assertThat(a).isNotEqualTo(b);
        }
    }
}

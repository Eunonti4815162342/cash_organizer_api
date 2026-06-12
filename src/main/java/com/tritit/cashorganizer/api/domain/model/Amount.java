package com.tritit.cashorganizer.api.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class Amount {
    private final Long value;
    private final String currency;
    // sin la anotación, el getter de Lombok serializa como "negative" mientras
    // el @JsonCreator espera "isNegative" — asimetría que rompe el round-trip
    @JsonProperty("isNegative")
    private final boolean isNegative;

    @JsonCreator
    public Amount(
            @JsonProperty("value") Long value,
            @JsonProperty("currency") String currency,
            @JsonProperty("isNegative") boolean isNegative) {
        this.value = value;
        this.currency = currency;
        this.isNegative = isNegative;
    }

    public Amount add(long delta) {
        return new Amount(this.value + delta, this.currency, this.isNegative);
    }

    public Amount subtract(long delta) {
        return new Amount(this.value - delta, this.currency, this.isNegative);
    }

    public Amount negate() {
        return new Amount(-this.value, this.currency, this.isNegative);
    }
}

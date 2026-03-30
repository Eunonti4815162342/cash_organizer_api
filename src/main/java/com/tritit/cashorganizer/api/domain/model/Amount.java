package com.tritit.cashorganizer.api.domain.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Amount {
    private long value;
    private String currency;
    private boolean isNegative;
}
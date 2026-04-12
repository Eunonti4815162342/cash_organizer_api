package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmountEmbeddable {
    private Long value;
    private String currency;
    private boolean isNegative;
}

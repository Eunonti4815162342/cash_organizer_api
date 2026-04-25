package com.tritit.cashorganizer.api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {
    private Long id;
    private User user;
    private String name;
    private String description;
    private FinancialEntity financialEntity;
}

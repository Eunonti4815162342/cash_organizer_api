package com.tritit.cashorganizer.api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountItem {
    private Long id;
    private User user;
    private String name;
    private String description;
    private String accountType;
    private Integer flags;
    private String notes;
    private Integer accountOrder;
    private Boolean active;
    private Amount amount;
    private FinancialEntity entity;
}

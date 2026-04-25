package com.tritit.cashorganizer.api.domain.model;

import com.tritit.cashorganizer.api.domain.model.TransactionItem.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSuggestion {
    private Long categoryId;
    private Long subcategoryId;
    private TransactionType transactionType;
}

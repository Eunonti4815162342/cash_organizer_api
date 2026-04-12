package com.tritit.cashorganizer.api.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionItem {
    private Long id;
    private User user;
    private String date;
    private String description;
    private Amount amount;
    private AccountItem account;
    private Category category;
    private Subcategory subcategory;
    private AccountItem toAccount;
    private TransactionType type;
    private String notes;
    private Integer statusFlags;
    private Boolean isScheduled;
    private Boolean isHeader;
    private List<String> tags;

    public enum TransactionType {
        EXPENSE, INCOME, TRANSFER, ACCOUNT_CLOSE
    }
}

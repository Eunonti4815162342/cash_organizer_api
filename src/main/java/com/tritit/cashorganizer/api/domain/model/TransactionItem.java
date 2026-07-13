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
    private Beneficiary beneficiary;
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

    public void validate() {
        // Normaliza a fecha pura YYYY-MM-DD: el campo se compara como string
        // (BETWEEN) tanto aquí como en SQLite local, y una hora residual
        // rompe esa comparación lexicográfica para transacciones del día en curso.
        if (this.date != null && this.date.length() > 10) {
            this.date = this.date.substring(0, 10);
        }
        if (this.account == null) {
            throw new com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException("Source account is mandatory");
        }
        if (this.amount == null || this.amount.getValue() == 0) {
            throw new com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException("Amount must be non-zero");
        }
        if (this.type == TransactionType.TRANSFER && this.toAccount == null) {
            throw new com.tritit.cashorganizer.api.domain.exception.InvalidTransactionException("Destination account is mandatory for transfers");
        }
    }

    public boolean belongsTo(User user) {
        return this.user != null && this.user.getId().equals(user.getId());
    }
}

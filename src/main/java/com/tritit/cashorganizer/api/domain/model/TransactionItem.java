package com.tritit.cashorganizer.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "transactions", schema = "cash_organizer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    private String date;
    private String description;

    @Embedded
    private Amount amount;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountItem account;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private AccountItem toAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType type;

    private String notes;
    private Integer statusFlags;

    private Boolean isScheduled;
    private Boolean isHeader;

    public enum TransactionType {
        EXPENSE, INCOME, TRANSFER, ACCOUNT_CLOSE
    }

    @ElementCollection
    @CollectionTable(name = "transaction_tags", schema = "cash_organizer", joinColumns = @JoinColumn(name = "transaction_id"))
    @Column(name = "tag")
    private List<String> tags;
}
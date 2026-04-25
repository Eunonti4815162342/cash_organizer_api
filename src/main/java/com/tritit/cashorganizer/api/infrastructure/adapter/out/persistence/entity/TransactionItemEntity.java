package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity;

import com.tritit.cashorganizer.api.domain.model.TransactionItem.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions", schema = "cash_organizer")
public class TransactionItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String date;
    private String description;

    @Embedded
    private AmountEmbeddable amount;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne
    @JoinColumn(name = "subcategory_id")
    private SubcategoryEntity subcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id")
    private BeneficiaryEntity beneficiary;

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private AccountEntity toAccount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private String notes;
    private Integer statusFlags;
    private Boolean isScheduled;
    private Boolean isHeader;

    @ElementCollection
    @CollectionTable(name = "transaction_tags", schema = "cash_organizer", joinColumns = @JoinColumn(name = "transaction_id"))
    @Column(name = "tag")
    private List<String> tags;
}

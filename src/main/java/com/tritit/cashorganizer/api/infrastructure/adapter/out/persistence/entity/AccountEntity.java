package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts", schema = "cash_organizer")
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String name;
    private String description;
    private String accountType;
    private Integer flags;
    private String notes;
    private Integer accountOrder;
    private Boolean active;

    @Embedded
    private AmountEmbeddable amount;

    @ManyToOne
    @JoinColumn(name = "entity_id")
    private FinancialEntityEntity financialEntity;
}

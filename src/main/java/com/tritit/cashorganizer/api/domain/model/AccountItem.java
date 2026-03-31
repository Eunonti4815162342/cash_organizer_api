package com.tritit.cashorganizer.api.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts", schema = "cash_organizer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Embedded
    private Amount amount;

    @ManyToOne
    @JoinColumn(name = "entity_id")
    @JsonIgnoreProperties("accounts")
    private FinancialEntity entity;

    private String description;
    private String accountType;
    private Integer flags;
    private String notes;
    private Integer accountOrder;

    @Column(nullable = false)
    private Boolean active = true;
}
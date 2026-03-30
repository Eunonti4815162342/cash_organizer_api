package com.tritit.cashorganizer.api.domain.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    private String description;
    private String accountType;
    private int flags;
    private String notes;
    private Integer accountOrder;
}
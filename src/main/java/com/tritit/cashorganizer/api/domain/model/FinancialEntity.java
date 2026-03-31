package com.tritit.cashorganizer.api.domain.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "financial_entities", schema = "cash_organizer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String taxId; // CIF/NIF
    private String description;

    @Enumerated(EnumType.STRING)
    private EntityType type;

    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<AccountItem> accounts;

    public enum EntityType {
        PHYSICAL, LEGAL
    }
}

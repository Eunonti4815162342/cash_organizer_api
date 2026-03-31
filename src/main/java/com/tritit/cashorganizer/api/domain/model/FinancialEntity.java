package com.tritit.cashorganizer.api.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties("entity")
    private List<AccountItem> accounts;

    // Static factory method for Jackson to use when it sees an object with "id"
    @com.fasterxml.jackson.annotation.JsonCreator
    public static FinancialEntity create(@JsonProperty("id") Long id) {
        FinancialEntity entity = new FinancialEntity();
        entity.setId(id);
        return entity;
    }

    public enum EntityType {
        PHYSICAL, LEGAL
    }
}

package com.tritit.cashorganizer.api.domain.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "categories", schema = "cash_organizer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String iconName;
    
    @Enumerated(EnumType.STRING)
    private CategoryType type; // EXPENSE o INCOME

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Subcategory> subcategories;

    public enum CategoryType {
        EXPENSE, INCOME
    }
}

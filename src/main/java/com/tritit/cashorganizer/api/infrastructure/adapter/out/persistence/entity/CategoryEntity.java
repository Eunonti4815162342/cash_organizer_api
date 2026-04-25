package com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity;

import com.tritit.cashorganizer.api.domain.model.Category.CategoryType;
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
@Table(name = "categories", schema = "cash_organizer")
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private String name;
    private String iconName;
    
    @Enumerated(EnumType.STRING)
    private CategoryType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_entity_id")
    private FinancialEntityEntity financialEntity;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    @OrderBy("name ASC")
    private List<SubcategoryEntity> subcategories;
}

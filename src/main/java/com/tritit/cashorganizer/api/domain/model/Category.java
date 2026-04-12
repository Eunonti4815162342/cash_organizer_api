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
public class Category {
    private Long id;
    private User user;
    private String name;
    private String iconName;
    private CategoryType type;
    private List<Subcategory> subcategories;

    public enum CategoryType {
        EXPENSE, INCOME
    }
}

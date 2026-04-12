package com.tritit.cashorganizer.api.domain.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subcategory {
    private Long id;
    private String name;
    private String iconName;
    private Category category;
}

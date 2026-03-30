package com.tritit.cashorganizer.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "transactions", schema = "cash_organizer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String date;
    private String description;

    @Embedded
    private Amount amount;

    private String notes;
    private Integer statusFlags;
    
    private Boolean isScheduled;
    private Boolean isHeader;

    @ElementCollection
    @CollectionTable(name = "transaction_tags", schema = "cash_organizer", joinColumns = @JoinColumn(name = "transaction_id"))
    @Column(name = "tag")
    private List<String> tags;
}
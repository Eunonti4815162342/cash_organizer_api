package com.tritit.cashorganizer.api.domain.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DetailedReport {
    private String title;
    private String period;
    private int totalTransactions; // NUEVO
    private Map<String, Long> categorySummary;
    private Map<String, Map<String, List<TransactionItem>>> segregatedData;
}

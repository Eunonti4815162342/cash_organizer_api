package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.DetailedReport;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportDataService {

    private final TransactionPersistencePort transactionPersistencePort;
    private final UserContextPort userContextPort;

    public DetailedReport getSegregatedReport(String startDate, String endDate, List<Long> accountIds, List<Long> categoryIds, List<Long> beneficiaryIds) {
        User user = userContextPort.getCurrentUser();
        
        var transactions = transactionPersistencePort.findAllForReport(user, startDate, endDate);

        List<TransactionItem> filtered = transactions.stream()
                .filter(t -> (accountIds == null || accountIds.isEmpty() || (t.getAccount() != null && accountIds.contains(t.getAccount().getId()))))
                .filter(t -> (beneficiaryIds == null || beneficiaryIds.isEmpty() || (t.getBeneficiary() != null && beneficiaryIds.contains(t.getBeneficiary().getId()))))
                .filter(t -> {
                    if (categoryIds == null || categoryIds.isEmpty()) return true;
                    boolean catMatch = t.getCategory() != null && categoryIds.contains(t.getCategory().getId());
                    boolean subMatch = t.getSubcategory() != null && t.getSubcategory().getCategory() != null 
                                       && categoryIds.contains(t.getSubcategory().getCategory().getId());
                    return catMatch || subMatch;
                })
                .sorted(Comparator.comparing(TransactionItem::getDate).reversed())
                .collect(Collectors.toList());

        // Limpiar fechas para el periodo
        String pStart = (startDate != null) ? startDate.split("T")[0] : "?";
        String pEnd = (endDate != null) ? endDate.split("T")[0] : "?";

        Map<String, Map<String, List<TransactionItem>>> segregated = new TreeMap<>();
        for (TransactionItem tx : filtered) {
            String entityName = (tx.getAccount() != null && tx.getAccount().getEntity() != null) 
                    ? tx.getAccount().getEntity().getName() : "PERSONAL / OTROS";
            String accountName = (tx.getAccount() != null) ? tx.getAccount().getName() : "CUENTA DESCONOCIDA";

            segregated.computeIfAbsent(entityName, k -> new TreeMap<>())
                      .computeIfAbsent(accountName, k -> new ArrayList<>())
                      .add(tx);
        }

        return DetailedReport.builder()
                .period(pStart + " - " + pEnd)
                .totalTransactions(filtered.size())
                .totalExpenses(filtered.stream()
                        .filter(t -> t.getAmount().isNegative())
                        .mapToLong(t -> Math.abs(t.getAmount().getValue()))
                        .sum())
                .totalIncomes(filtered.stream()
                        .filter(t -> !t.getAmount().isNegative())
                        .mapToLong(t -> t.getAmount().getValue())
                        .sum())
                .categorySummary(calculateCategoryStats(filtered))
                .segregatedData(segregated)
                .build();
    }

    private Map<String, Long> calculateCategoryStats(List<TransactionItem> txs) {
        return txs.stream().collect(Collectors.groupingBy(
            t -> t.getCategory() != null ? t.getCategory().getName() : "Otros",
            Collectors.summingLong(t -> Math.abs(t.getAmount().getValue()))
        ));
    }

    // Métodos antiguos delegados
    public Map<String, Long> getCategoryGroupedData(String startDate, String endDate, List<Long> accountIds, boolean groupBySubcategory) {
        return getSegregatedReport(startDate, endDate, accountIds, null, null).getCategorySummary();
    }
    public Map<String, Long> getEntityGroupedData(String startDate, String endDate, List<Long> accountIds) {
        return new HashMap<>(); 
    }
    public Map<String, Long> getBeneficiaryGroupedData(String startDate, String endDate, List<Long> accountIds) {
        return new HashMap<>();
    }
}

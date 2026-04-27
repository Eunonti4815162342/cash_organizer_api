package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.DetailedReport;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
        
        // USAMOS LA CONSULTA DE AUDITORÍA (JOIN FETCH)
        var allTransactions = transactionPersistencePort.findAllForReport(user, startDate, endDate);

        System.out.println("[AUDITORÍA SQL] Filas cargadas con éxito: " + allTransactions.size());

        // 2. FILTRADO IDÉNTICO AL FRONT (Criterio de Inclusión Total)
        List<TransactionItem> filtered = allTransactions.stream()
                .filter(t -> {
                    // Filtro de Cuenta: Si pides 34,35, solo pasan esas
                    if (accountIds == null || accountIds.isEmpty()) return true;
                    return t.getAccount() != null && accountIds.contains(t.getAccount().getId());
                })
                .filter(t -> {
                    // Filtro de Beneficiario
                    if (beneficiaryIds == null || beneficiaryIds.isEmpty()) return true;
                    return t.getBeneficiary() != null && beneficiaryIds.contains(t.getBeneficiary().getId());
                })
                .filter(t -> {
                    // Filtro de Categoría: ESTE ES EL PUNTO CRÍTICO
                    if (categoryIds == null || categoryIds.isEmpty()) return true;
                    
                    // Caso A: La transacción tiene la categoría padre directamente
                    if (t.getCategory() != null && categoryIds.contains(t.getCategory().getId())) return true;
                    
                    // Caso B: La transacción tiene una subcategoría que pertenece a una de esas categorías padre
                    if (t.getSubcategory() != null && t.getSubcategory().getCategory() != null 
                        && categoryIds.contains(t.getSubcategory().getCategory().getId())) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        System.out.println("[DB-AUDIT] Transacciones tras filtrado: " + filtered.size());

        // 3. AGRUPACIÓN GARANTIZADA POR EMPRESA
        // Usamos TreeMap para asegurar orden alfabético: Empresa A, Empresa B...
        Map<String, Map<String, List<TransactionItem>>> segregated = new TreeMap<>();
        
        for (TransactionItem tx : filtered) {
            String entityName = "SIN EMPRESA / OTROS";
            if (tx.getAccount() != null && tx.getAccount().getEntity() != null) {
                entityName = tx.getAccount().getEntity().getName();
            }
            
            String accountName = (tx.getAccount() != null) ? tx.getAccount().getName() : "CUENTA DESCONOCIDA";

            segregated.computeIfAbsent(entityName, k -> new TreeMap<>())
                      .computeIfAbsent(accountName, k -> new ArrayList<>())
                      .add(tx);
        }

        return DetailedReport.builder()
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

    // Compatibilidad para gráficos
    public Map<String, Long> getCategoryGroupedData(String startDate, String endDate, List<Long> accountIds, boolean groupBySubcategory) {
        return getSegregatedReport(startDate, endDate, accountIds, null, null).getCategorySummary();
    }

    public Map<String, Long> getEntityGroupedData(String startDate, String endDate, List<Long> accountIds) {
        DetailedReport report = getSegregatedReport(startDate, endDate, accountIds, null, null);
        Map<String, Long> stats = new HashMap<>();
        report.getSegregatedData().forEach((entity, accounts) -> {
            long total = accounts.values().stream().flatMap(List::stream).mapToLong(t -> Math.abs(t.getAmount().getValue())).sum();
            stats.put(entity, total);
        });
        return stats;
    }

    public Map<String, Long> getBeneficiaryGroupedData(String startDate, String endDate, List<Long> accountIds) {
        DetailedReport report = getSegregatedReport(startDate, endDate, accountIds, null, null);
        return new HashMap<>(); // No implementado para este caso
    }
}

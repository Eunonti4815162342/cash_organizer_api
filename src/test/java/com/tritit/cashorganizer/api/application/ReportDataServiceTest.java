package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.DetailedReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReportDataServiceTest {

    @Autowired
    private ReportDataService reportDataService;

    @Test
    @WithMockUser(username="alvaroveigavazquez@gmail.com")
    void testSegregation() {
        DetailedReport report = reportDataService.getSegregatedReport(
            "2026-04-01T00:00:00.000", 
            "2026-04-28T23:59:59.999", 
            List.of(34L, 35L), 
            List.of(14L),
            null
        );

        System.out.println("================= RESULTADO SIMULACIÓN ==================");
        System.out.println("Empresas encontradas: " + report.getSegregatedData().keySet());
        report.getSegregatedData().forEach((entity, accounts) -> {
            System.out.println("Empresa: " + entity + " | Cuentas: " + accounts.keySet());
            accounts.forEach((acc, txs) -> {
                System.out.println("  -> Cuenta: " + acc + " | Transacciones: " + txs.size());
                txs.forEach(t -> {
                    String catName = (t.getCategory() != null) ? t.getCategory().getName() : "N/A";
                    String subCatName = (t.getSubcategory() != null) ? t.getSubcategory().getName() : "N/A";
                    System.out.println("      - ID: " + t.getId() + " | Cat: " + catName + " | SubCat: " + subCatName);
                });
            });
        });
        System.out.println("=========================================================");
        
        assertTrue(report.getSegregatedData().size() >= 1, "Debe haber al menos una empresa");
    }
}

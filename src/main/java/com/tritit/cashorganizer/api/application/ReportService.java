package com.tritit.cashorganizer.api.application;

import com.tritit.cashorganizer.api.domain.model.DetailedReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportDataService reportDataService;
    private final PdfReportGenerator pdfReportGenerator;

    public Map<String, Long> getCategoryGroupedData(String startDate, String endDate, List<Long> accountIds, List<Long> categoryIds, List<Long> beneficiaryIds, boolean groupBySubcategory) {
        return reportDataService.getCategoryGroupedData(startDate, endDate, accountIds, categoryIds, beneficiaryIds, groupBySubcategory);
    }

    public Map<String, Long> getEntityGroupedData(String startDate, String endDate, List<Long> accountIds) {
        return reportDataService.getEntityGroupedData(startDate, endDate, accountIds);
    }

    public Map<String, Long> getBeneficiaryGroupedData(String startDate, String endDate, List<Long> accountIds) {
        return reportDataService.getBeneficiaryGroupedData(startDate, endDate, accountIds);
    }

    public byte[] generatePdfReport(String title, String chartType, String reportType, String startDate, String endDate, 
                                   List<Long> accountIds, List<Long> categoryIds, List<Long> beneficiaryIds, String lang) {
        DetailedReport report = reportDataService.getSegregatedReport(startDate, endDate, accountIds, categoryIds, beneficiaryIds);
        return pdfReportGenerator.generatePdfReport(report, title, lang);
    }
}

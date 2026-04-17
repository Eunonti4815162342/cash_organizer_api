package com.tritit.cashorganizer.api.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportDataService reportDataService;
    private final PdfReportGenerator pdfReportGenerator;

    public Map<String, Long> getCategoryGroupedData(String startDate, String endDate, List<Long> accountIds, boolean groupBySubcategory) {
        return reportDataService.getCategoryGroupedData(startDate, endDate, accountIds, groupBySubcategory);
    }

    public byte[] generatePdfReport(String title, String chartType, String startDate, String endDate, List<Long> accountIds, List<Long> categoryIds, String lang) {
        return pdfReportGenerator.generatePdfReport(title, chartType, startDate, endDate, accountIds, categoryIds, lang);
    }
}

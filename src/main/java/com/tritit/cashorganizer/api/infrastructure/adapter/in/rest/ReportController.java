package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.tritit.cashorganizer.api.application.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    private String sanitizeDate(String date) {
        if (date == null) return null;
        String d = date.contains("T") ? date.split("T")[0] : date;
        d = d.contains(" ") ? d.split(" ")[0] : d;
        return d.length() > 10 ? d.substring(0, 10) : d;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam String title,
            @RequestParam String chartType,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> accountIds,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<Long> beneficiaryIds, // NUEVO
            @RequestParam(defaultValue = "en") String lang) {

        byte[] pdfBytes = reportService.generatePdfReport(title, chartType, reportType, startDate, endDate, accountIds, categoryIds, beneficiaryIds, lang);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/category-stats")
    public ResponseEntity<Map<String, Long>> getCategoryStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> accountIds,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<Long> beneficiaryIds,
            @RequestParam(defaultValue = "false") boolean groupBySubcategory) {
        String sDate = sanitizeDate(startDate);
        String eDate = sanitizeDate(endDate);
        return ResponseEntity.ok(reportService.getCategoryGroupedData(sDate, eDate, accountIds, categoryIds, beneficiaryIds, groupBySubcategory));
    }
}

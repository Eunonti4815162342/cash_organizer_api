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

    @GetMapping("/category-stats")
    public ResponseEntity<Map<String, Long>> getCategoryStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> accountIds,
            @RequestParam(defaultValue = "false") boolean groupBySubcategory) {
        return ResponseEntity.ok(reportService.getCategoryGroupedData(startDate, endDate, accountIds, groupBySubcategory));
    }

    @GetMapping("/entity-stats")
    public ResponseEntity<Map<String, Long>> getEntityStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> accountIds) {
        return ResponseEntity.ok(reportService.getEntityGroupedData(startDate, endDate, accountIds));
    }

    @GetMapping("/beneficiary-stats")
    public ResponseEntity<Map<String, Long>> getBeneficiaryStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> accountIds) {
        return ResponseEntity.ok(reportService.getBeneficiaryGroupedData(startDate, endDate, accountIds));
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdfReport(
            @RequestParam String title, 
            @RequestParam(defaultValue = "PIE") String chartType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> accountIds,
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(defaultValue = "en") String lang) {
        
        byte[] pdfBytes = reportService.generatePdfReport(title, chartType, startDate, endDate, accountIds, categoryIds, lang);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

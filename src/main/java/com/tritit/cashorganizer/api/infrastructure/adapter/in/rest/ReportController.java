package com.tritit.cashorganizer.api.infrastructure.adapter.in.rest;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReportController {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping("/pdf")
    public void generatePdfReport(
            @RequestParam String title, 
            @RequestParam(defaultValue = "PIE") String chartType,
            HttpServletResponse response) throws IOException {
        
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=report.pdf");

        List<AccountItem> accounts = accountRepository.findAll();
        List<TransactionItem> transactions = transactionRepository.findAll();

        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        // Fonts
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(0, 159, 251));
        Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10);

        // Header
        Paragraph pTitle = new Paragraph("Cash Organizer - Financial Report", fontTitle);
        pTitle.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(pTitle);

        Paragraph pDesc = new Paragraph(title, fontSubtitle);
        pDesc.setAlignment(Paragraph.ALIGN_CENTER);
        pDesc.setSpacingAfter(20);
        document.add(pDesc);

        // Visual Chart Section (as an Image object to avoid overlapping)
        document.add(new Paragraph("Visual Summary (" + chartType + ")", fontSubtitle));
        document.add(new Paragraph(" ")); 

        Image chartImage;
        try {
            if (chartType.equalsIgnoreCase("PIE")) {
                chartImage = createPieChartImage(writer, accounts);
            } else {
                chartImage = createBarChartImage(writer, accounts);
            }
            if (chartImage != null) {
                chartImage.setAlignment(Image.ALIGN_CENTER);
                document.add(chartImage);
            }
        } catch (Exception e) {
            document.add(new Paragraph("Error generating chart: " + e.getMessage()));
        }
        
        document.add(new Paragraph(" ")); 

        // Accounts Table
        document.add(new Paragraph("Account Details", fontSubtitle));
        PdfPTable accTable = new PdfPTable(3);
        accTable.setWidthPercentage(100);
        accTable.setSpacingBefore(10);
        accTable.setSpacingAfter(20);

        writeTableHeader(accTable, fontHeader, new String[]{"Account", "Type", "Balance"});
        for (AccountItem acc : accounts) {
            accTable.addCell(new Phrase(acc.getName(), fontBody));
            accTable.addCell(new Phrase(acc.getAccountType(), fontBody));
            double bal = acc.getAmount().getValue() / 100.0;
            accTable.addCell(new Phrase("EUR " + String.format("%.2f", bal), fontBody));
        }
        document.add(accTable);

        // Transactions Table
        document.add(new Paragraph("Recent Transactions", fontSubtitle));
        PdfPTable txTable = new PdfPTable(4);
        txTable.setWidthPercentage(100);
        txTable.setSpacingBefore(10);

        writeTableHeader(txTable, fontHeader, new String[]{"Date", "Category", "Description", "Amount"});
        for (TransactionItem tx : transactions) {
            txTable.addCell(new Phrase(tx.getDate().split("T")[0], fontBody));
            txTable.addCell(new Phrase(tx.getCategory() != null ? tx.getCategory().getName() : "General", fontBody));
            txTable.addCell(new Phrase(tx.getDescription(), fontBody));
            double val = tx.getAmount().getValue() / 100.0;
            String sign = tx.getAmount().isNegative() ? "-" : "";
            txTable.addCell(new Phrase(sign + "EUR " + String.format("%.2f", val), fontBody));
        }
        document.add(txTable);

        document.close();
    }

    private Image createBarChartImage(PdfWriter writer, List<AccountItem> accounts) throws BadElementException {
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate template = cb.createTemplate(400, 200);
        
        float x = 50;
        float y = 30; // Base line
        float width = 30;
        float maxHeight = 130;

        if (accounts.isEmpty()) return Image.getInstance(template);

        double maxVal = accounts.stream()
                .mapToDouble(a -> Math.abs(a.getAmount().getValue()))
                .max().orElse(100.0);

        for (AccountItem acc : accounts) {
            double val = acc.getAmount().getValue();
            float normalizedHeight = (float) ((Math.abs(val) / maxVal) * maxHeight);
            
            template.setColorFill(val < 0 ? Color.RED : new Color(0, 159, 251));
            template.rectangle(x, y, width, normalizedHeight);
            template.fill();
            
            // Label below the bar
            template.beginText();
            template.setFontAndSize(FontFactory.getFont(FontFactory.HELVETICA).getBaseFont(), 8);
            template.setColorFill(Color.DARK_GRAY);
            template.showTextAligned(PdfContentByte.ALIGN_CENTER, acc.getName(), x + width/2, y - 15, 0);
            template.endText();

            x += 50;
        }
        return Image.getInstance(template);
    }

    private Image createPieChartImage(PdfWriter writer, List<AccountItem> accounts) throws BadElementException {
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate template = cb.createTemplate(400, 200);
        
        float centerX = 150;
        float centerY = 100;
        float radius = 80;

        if (accounts.isEmpty()) return Image.getInstance(template);

        double total = accounts.stream().mapToDouble(a -> Math.abs(a.getAmount().getValue())).sum();
        float startAngle = 0;

        Color[] colors = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.RED};
        int i = 0;

        for (AccountItem acc : accounts) {
            float arc = (float) (Math.abs(acc.getAmount().getValue()) / total * 360);
            template.setColorFill(colors[i % colors.length]);
            template.moveTo(centerX, centerY);
            template.arc(centerX - radius, centerY - radius, centerX + radius, centerY + radius, startAngle, arc);
            template.lineTo(centerX, centerY);
            template.fill();
            
            // Legend Item
            template.rectangle(300, centerY + 60 - (i * 15), 10, 10);
            template.fill();
            template.beginText();
            template.setFontAndSize(FontFactory.getFont(FontFactory.HELVETICA).getBaseFont(), 8);
            template.setColorFill(Color.BLACK);
            template.showTextAligned(PdfContentByte.ALIGN_LEFT, acc.getName(), 315, centerY + 62 - (i * 15), 0);
            template.endText();

            startAngle += arc;
            i++;
        }
        return Image.getInstance(template);
    }

    private void writeTableHeader(PdfPTable table, Font font, String[] headers) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(0, 159, 251));
        cell.setPadding(5);

        for (String h : headers) {
            cell.setPhrase(new Phrase(h, font));
            table.addCell(cell);
        }
    }
}

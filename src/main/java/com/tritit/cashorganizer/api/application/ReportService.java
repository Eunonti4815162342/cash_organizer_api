package com.tritit.cashorganizer.api.application;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public byte[] generatePdfReport(String title, String chartType, String startDate, String endDate, List<Long> accountIds, List<Long> categoryIds) {
        User user = getCurrentUser();
        
        // 1. Obtención de datos filtrados
        List<AccountItem> allAccounts = accountRepository.findAllByUser(user);
        List<AccountItem> filteredAccounts = (accountIds == null || accountIds.isEmpty()) 
                ? allAccounts 
                : allAccounts.stream().filter(a -> accountIds.contains(a.getId())).collect(Collectors.toList());

        List<TransactionItem> transactions;
        if (startDate != null && endDate != null) {
            transactions = transactionRepository.findAllByUserAndDateRange(user, startDate, endDate);
        } else {
            transactions = transactionRepository.findAllByUser(user);
        }

        // Filtro adicional por cuentas y categorías en memoria (para mayor flexibilidad)
        List<TransactionItem> filteredTransactions = transactions.stream()
                .filter(t -> (accountIds == null || accountIds.isEmpty() || (t.getAccount() != null && accountIds.contains(t.getAccount().getId()))))
                .filter(t -> (categoryIds == null || categoryIds.isEmpty() || (t.getCategory() != null && categoryIds.contains(t.getCategory().getId()))))
                .collect(Collectors.toList());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(0, 159, 251));
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Header
            Paragraph pTitle = new Paragraph("Cash Organizer - Custom Financial Report", fontTitle);
            pTitle.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(pTitle);

            Paragraph pUser = new Paragraph("User: " + user.getEmail() + " | Period: " + (startDate != null ? startDate : "All") + " to " + (endDate != null ? endDate : "All"), fontBody);
            pUser.setAlignment(Paragraph.ALIGN_CENTER);
            pUser.setSpacingAfter(10);
            document.add(pUser);

            Paragraph pDesc = new Paragraph(title, fontSubtitle);
            pDesc.setAlignment(Paragraph.ALIGN_CENTER);
            pDesc.setSpacingAfter(20);
            document.add(pDesc);

            // Chart Section
            Image chartImage = chartType.equalsIgnoreCase("PIE") 
                    ? createPieChartImage(writer, filteredAccounts) 
                    : createBarChartImage(writer, filteredAccounts);
            
            if (chartImage != null) {
                chartImage.setAlignment(Image.ALIGN_CENTER);
                document.add(chartImage);
            }
            
            document.add(new Paragraph(" ")); 

            // Accounts Table
            document.add(new Paragraph("Filtered Accounts Summary", fontSubtitle));
            PdfPTable accTable = new PdfPTable(3);
            accTable.setWidthPercentage(100);
            accTable.setSpacingBefore(10);
            writeTableHeader(accTable, fontHeader, new String[]{"Account", "Type", "Balance"});
            for (AccountItem acc : filteredAccounts) {
                accTable.addCell(new Phrase(acc.getName(), fontBody));
                accTable.addCell(new Phrase(acc.getAccountType() != null ? acc.getAccountType() : "N/A", fontBody));
                double bal = (acc.getAmount() != null ? acc.getAmount().getValue() : 0) / 100.0;
                accTable.addCell(new Phrase("EUR " + String.format("%.2f", bal), fontBody));
            }
            document.add(accTable);

            // Transactions Table
            document.add(new Paragraph("Filtered Transactions", fontSubtitle));
            PdfPTable txTable = new PdfPTable(4);
            txTable.setWidthPercentage(100);
            txTable.setSpacingBefore(10);
            writeTableHeader(txTable, fontHeader, new String[]{"Date", "Category", "Description", "Amount"});
            for (TransactionItem tx : filteredTransactions) {
                txTable.addCell(new Phrase(tx.getDate() != null ? tx.getDate().split("T")[0] : "N/A", fontBody));
                txTable.addCell(new Phrase(tx.getCategory() != null ? tx.getCategory().getName() : "General", fontBody));
                txTable.addCell(new Phrase(tx.getDescription(), fontBody));
                double val = (tx.getAmount() != null ? tx.getAmount().getValue() : 0) / 100.0;
                String sign = (tx.getAmount() != null && tx.getAmount().isNegative()) ? "-" : "";
                txTable.addCell(new Phrase(sign + "EUR " + String.format("%.2f", val), fontBody));
            }
            document.add(txTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    private Image createBarChartImage(PdfWriter writer, List<AccountItem> accounts) throws BadElementException {
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate template = cb.createTemplate(400, 200);
        float x = 50; float y = 30; float width = 30; float maxHeight = 130;
        if (accounts.isEmpty()) return null;
        double maxVal = accounts.stream().mapToDouble(a -> a.getAmount() != null ? Math.abs(a.getAmount().getValue()) : 0).max().orElse(100.0);
        for (AccountItem acc : accounts) {
            double val = acc.getAmount() != null ? acc.getAmount().getValue() : 0;
            float normalizedHeight = (float) ((Math.abs(val) / maxVal) * maxHeight);
            template.setColorFill(val < 0 ? Color.RED : new Color(0, 159, 251));
            template.rectangle(x, y, width, normalizedHeight);
            template.fill();
            x += 50;
        }
        return Image.getInstance(template);
    }

    private Image createPieChartImage(PdfWriter writer, List<AccountItem> accounts) throws BadElementException {
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate template = cb.createTemplate(400, 200);
        float centerX = 150; float centerY = 100; float radius = 80;
        if (accounts.isEmpty()) return null;
        double total = accounts.stream().mapToDouble(a -> a.getAmount() != null ? Math.abs(a.getAmount().getValue()) : 0).sum();
        if (total == 0) return null;
        float startAngle = 0;
        Color[] colors = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN, Color.RED};
        int i = 0;
        for (AccountItem acc : accounts) {
            double val = acc.getAmount() != null ? Math.abs(acc.getAmount().getValue()) : 0;
            float arc = (float) (val / total * 360);
            template.setColorFill(colors[i % colors.length]);
            template.moveTo(centerX, centerY);
            template.arc(centerX - radius, centerY - radius, centerX + radius, centerY + radius, startAngle, arc);
            template.lineTo(centerX, centerY);
            template.fill();
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

package com.tritit.cashorganizer.api.application;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.Amount;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.AccountRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.PersistenceMapper;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.TransactionRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.UserRepository;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.TransactionItemEntity;
import com.tritit.cashorganizer.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PersistenceMapper mapper;

    private UserEntity getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public Map<String, Long> getCategoryGroupedData(String startDate, String endDate, List<Long> accountIds, boolean groupBySubcategory) {
        UserEntity user = getCurrentUserEntity();
        List<TransactionItemEntity> transactions = (startDate != null && endDate != null)
                ? transactionRepository.findAllByUserAndDateRange(user, startDate, endDate, Pageable.unpaged()).getContent()
                : transactionRepository.findAllByUser(user, Pageable.unpaged()).getContent();

        return transactions.stream()
                .filter(t -> t.getCategory() != null)
                .filter(t -> accountIds == null || accountIds.isEmpty() || (t.getAccount() != null && accountIds.contains(t.getAccount().getId())))
                .collect(Collectors.groupingBy(
                        t -> (groupBySubcategory && t.getSubcategory() != null) 
                             ? t.getCategory().getName() + " > " + t.getSubcategory().getName() 
                             : t.getCategory().getName(),
                        Collectors.summingLong(t -> t.getAmount().getValue())
                ));
    }

    public byte[] generatePdfReport(String title, String chartType, String startDate, String endDate, List<Long> accountIds, List<Long> categoryIds) {
        UserEntity user = getCurrentUserEntity();
        
        List<AccountEntity> allAccounts = accountRepository.findAllByUser(user);
        List<AccountEntity> filteredAccounts = (accountIds == null || accountIds.isEmpty()) 
                ? allAccounts 
                : allAccounts.stream().filter(a -> accountIds.contains(a.getId())).collect(Collectors.toList());

        List<TransactionItemEntity> transactions;
        if (startDate != null && endDate != null) {
            transactions = transactionRepository.findAllByUserAndDateRange(user, startDate, endDate, Pageable.unpaged()).getContent();
        } else {
            transactions = transactionRepository.findAllByUser(user, Pageable.unpaged()).getContent();
        }

        List<TransactionItemEntity> filteredTransactions = transactions.stream()
                .filter(t -> (accountIds == null || accountIds.isEmpty() || (t.getAccount() != null && accountIds.contains(t.getAccount().getId()))))
                .filter(t -> (categoryIds == null || categoryIds.isEmpty() || (t.getCategory() != null && categoryIds.contains(t.getCategory().getId()))))
                .collect(Collectors.toList());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Modern Colors
            Color primaryBlue = new Color(0, 159, 251);
            Color lightGray = new Color(245, 245, 245);
            Color darkBlue = new Color(74, 99, 111);

            // Fonts
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryBlue);
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, darkBlue);
            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.GRAY);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10, darkBlue);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // 1. TOP HEADER SECTION
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{2, 1});

            PdfPCell titleCell = new PdfPCell(new Phrase("CASH ORGANIZER", fontTitle));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerTable.addCell(titleCell);

            PdfPCell infoCell = new PdfPCell();
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            infoCell.addElement(new Paragraph("FINANCIAL REPORT", fontLabel));
            infoCell.addElement(new Paragraph(title.toUpperCase(), fontSubtitle));
            headerTable.addCell(infoCell);
            document.add(headerTable);

            document.add(new Paragraph(" "));
            LineSeparator ls = new LineSeparator(1f, 100, primaryBlue, Element.ALIGN_CENTER, -2);
            document.add(ls);
            document.add(new Paragraph(" "));

            // 2. SUMMARY CARDS
            double totalBalance = filteredAccounts.stream().mapToDouble(a -> a.getAmount().getValue()).sum() / 100.0;
            double totalExpenses = filteredTransactions.stream()
                .filter(t -> t.getAmount().isNegative())
                .mapToDouble(t -> Math.abs(t.getAmount().getValue())).sum() / 100.0;

            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(10);
            summaryTable.setSpacingAfter(20);

            addSummaryCard(summaryTable, "CURRENT BALANCE", "EUR " + String.format("%.2f", totalBalance), primaryBlue, fontLabel, fontSubtitle);
            addSummaryCard(summaryTable, "PERIOD EXPENSES", "EUR " + String.format("%.2f", totalExpenses), Color.RED, fontLabel, fontSubtitle);
            addSummaryCard(summaryTable, "REPORT DATE", java.time.LocalDate.now().toString(), darkBlue, fontLabel, fontSubtitle);
            document.add(summaryTable);

            // 3. CHART AND ACCOUNTS
            document.add(new Paragraph("ACCOUNTS OVERVIEW", fontSubtitle));
            document.add(new Paragraph(" ", fontBody));

            PdfPTable accTable = new PdfPTable(3);
            accTable.setWidthPercentage(100);
            writeModernHeader(accTable, fontHeader, primaryBlue, new String[]{"Account Name", "Type", "Balance"});
            for (AccountEntity acc : filteredAccounts) {
                accTable.addCell(createCell(acc.getName(), fontBody, false));
                accTable.addCell(createCell(acc.getAccountType() != null ? acc.getAccountType() : "Standard", fontBody, false));
                double bal = acc.getAmount().getValue() / 100.0;
                accTable.addCell(createCell("EUR " + String.format("%.2f", bal), fontBody, true));
            }
            document.add(accTable);

            document.add(new Paragraph(" "));

            // 4. TRANSACTION HISTORY
            document.add(new Paragraph("TRANSACTION DETAILS", fontSubtitle));
            document.add(new Paragraph(" ", fontBody));

            PdfPTable txTable = new PdfPTable(4);
            txTable.setWidthPercentage(100);
            txTable.setWidths(new float[]{1, 2, 3, 1});
            writeModernHeader(txTable, fontHeader, primaryBlue, new String[]{"Date", "Category", "Description", "Amount"});
            
            for (TransactionItemEntity tx : filteredTransactions) {
                String catName = tx.getCategory() != null ? tx.getCategory().getName() : "General";
                if (tx.getSubcategory() != null) catName += " > " + tx.getSubcategory().getName();

                txTable.addCell(createCell(tx.getDate().split("T")[0], fontBody, false));
                txTable.addCell(createCell(catName, fontBody, false));
                txTable.addCell(createCell(tx.getDescription(), fontBody, false));
                
                double val = tx.getAmount().getValue() / 100.0;
                String sign = tx.getAmount().isNegative() ? "-" : "+";
                Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, tx.getAmount().isNegative() ? Color.RED : new Color(39, 174, 96));
                txTable.addCell(createCell(sign + " €" + String.format("%.2f", Math.abs(val)), amountFont, true));
            }
            document.add(txTable);

            // Footer
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating modern PDF", e);
        }
    }

    private void addSummaryCard(PdfPTable table, String label, String value, Color valueColor, Font fontLabel, Font fontValue) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBackgroundColor(new Color(250, 250, 250));
        cell.setBorderColor(new Color(230, 230, 230));
        cell.addElement(new Paragraph(label, fontLabel));
        Font customFont = new Font(fontValue);
        customFont.setColor(valueColor);
        cell.addElement(new Paragraph(value, customFont));
        table.addCell(cell);
    }

    private void writeModernHeader(PdfPTable table, Font font, Color bgColor, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String text, Font font, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderColor(new Color(240, 240, 240));
        if (alignRight) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}

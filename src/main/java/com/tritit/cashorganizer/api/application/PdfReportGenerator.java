package com.tritit.cashorganizer.api.application;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.tritit.cashorganizer.api.domain.model.AccountItem;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import com.tritit.cashorganizer.api.domain.model.User;
import com.tritit.cashorganizer.api.domain.port.out.AccountPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.TransactionPersistencePort;
import com.tritit.cashorganizer.api.domain.port.out.UserContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfReportGenerator {

    private final AccountPersistencePort accountPersistencePort;
    private final TransactionPersistencePort transactionPersistencePort;
    private final UserContextPort userContextPort;
    private final ReportDataService reportDataService;
    private final ReportTranslationService translationService;
    private final ReportStyler styler;

    public byte[] generatePdfReport(String title, String chartType, String reportType, String startDate, String endDate, List<Long> accountIds, List<Long> categoryIds, String lang) {
        User user = userContextPort.getCurrentUser();
        String type = (reportType != null) ? reportType.toUpperCase() : "CATEGORY";

        // 1. Obtener estadísticas agrupadas según el tipo explícito
        Map<String, Long> stats;
        if ("ENTITY".equals(type)) {
            stats = reportDataService.getEntityGroupedData(startDate, endDate, accountIds);
        } else if ("BENEFICIARY".equals(type)) {
            stats = reportDataService.getBeneficiaryGroupedData(startDate, endDate, accountIds);
        } else {
            stats = reportDataService.getCategoryGroupedData(startDate, endDate, accountIds, false);
        }

        // 2. Obtener cuentas filtradas
        List<AccountItem> allAccounts = accountPersistencePort.findAllByUser(user);
        List<AccountItem> filteredAccounts = (accountIds == null || accountIds.isEmpty())
                ? allAccounts
                : allAccounts.stream().filter(a -> accountIds.contains(a.getId())).collect(Collectors.toList());

        // 3. Obtener transacciones detalladas
        var transactions = (startDate != null && endDate != null)
                ? transactionPersistencePort.findAllByUserAndDateRange(user, startDate, endDate, Pageable.unpaged()).getContent()
                : transactionPersistencePort.findAllByUser(user, Pageable.unpaged()).getContent();

        List<TransactionItem> filteredTransactions = transactions.stream()
                .filter(t -> (accountIds == null || accountIds.isEmpty() || (t.getAccount() != null && accountIds.contains(t.getAccount().getId()))))
                .filter(t -> (categoryIds == null || categoryIds.isEmpty() || (t.getCategory() != null && categoryIds.contains(t.getCategory().getId()))))
                .sorted(Comparator.comparing(TransactionItem::getDate).reversed())
                .collect(Collectors.toList());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            ReportStyler.ReportColors colors = styler.getColors();
            ReportStyler.ReportFonts fonts = styler.getFonts(colors);

            addHeader(document, title, lang, colors, fonts);
            addStatsSummary(document, stats, colors, fonts, lang);
            addAccountsOverview(document, filteredAccounts, colors, fonts, lang);
            
            // AGRUPACIÓN DINÁMICA DE DETALLES
            if ("ENTITY".equals(type)) {
                addDetailsGroupedByEntity(document, filteredTransactions, colors, fonts, lang);
            } else if ("BENEFICIARY".equals(type)) {
                addDetailsGroupedByBeneficiary(document, filteredTransactions, colors, fonts, lang);
            } else {
                addDetailsGroupedByMonth(document, filteredTransactions, colors, fonts, lang);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating NATAVE report PDF", e);
        }
    }

    private void addHeader(Document document, String title, String lang, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 1});
        
        PdfPCell titleCell = new PdfPCell(new Phrase("NATAVE", fonts.fontTitle));
        titleCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(titleCell);
        
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        infoCell.addElement(new Paragraph(translationService.getLabel("title", lang), fonts.fontLabel));
        infoCell.addElement(new Paragraph(title.toUpperCase(), fonts.fontSubtitle));
        headerTable.addCell(infoCell);
        
        document.add(headerTable);
        document.add(new Paragraph(" "));
        document.add(new LineSeparator(1f, 100, colors.primaryBlue, Element.ALIGN_CENTER, -2));
        document.add(new Paragraph(" "));
    }

    private void addStatsSummary(Document document, Map<String, Long> stats, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts, String lang) throws DocumentException {
        if (stats == null || stats.isEmpty()) return;

        document.add(new Paragraph(translationService.getLabel("stats_summary", lang), fonts.fontSubtitle));
        document.add(new Paragraph(" ", fonts.fontBody));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 1});

        styler.writeModernHeader(table, fonts.fontHeader, colors.primaryBlue, new String[]{
                translationService.getLabel("item_name", lang),
                translationService.getLabel("item_amt", lang),
                "%"
        });

        long total = stats.values().stream().mapToLong(Long::longValue).sum();

        for (Map.Entry<String, Long> entry : stats.entrySet()) {
            double val = entry.getValue() / 100.0;
            double pct = total == 0 ? 0 : (entry.getValue() * 100.0 / total);

            table.addCell(styler.createCell(entry.getKey(), fonts.fontBody, false));
            table.addCell(styler.createCell("€ " + String.format("%.2f", val), fonts.fontBody, true));
            table.addCell(styler.createCell(String.format("%.1f", pct) + "%", fonts.fontBody, true));
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addAccountsOverview(Document document, List<AccountItem> accounts, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts, String lang) throws DocumentException {
        document.add(new Paragraph(translationService.getLabel("overview", lang), fonts.fontSubtitle));
        document.add(new Paragraph(" ", fonts.fontBody));
        PdfPTable accTable = new PdfPTable(3);
        accTable.setWidthPercentage(100);
        styler.writeModernHeader(accTable, fonts.fontHeader, colors.primaryBlue, new String[]{
                translationService.getLabel("acc_name", lang),
                translationService.getLabel("acc_type", lang),
                translationService.getLabel("acc_bal", lang)
        });
        for (AccountItem acc : accounts) {
            accTable.addCell(styler.createCell(acc.getName(), fonts.fontBody, false));
            accTable.addCell(styler.createCell(acc.getAccountType() != null ? acc.getAccountType() : "Standard", fonts.fontBody, false));
            double bal = acc.getAmount().getValue() / 100.0;
            accTable.addCell(styler.createCell("€ " + String.format("%.2f", bal), fonts.fontBody, true));
        }
        document.add(accTable);
        document.add(new Paragraph(" "));
    }

    private void addDetailsGroupedByMonth(Document document, List<TransactionItem> transactions, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts, String lang) throws DocumentException {
        Map<String, List<TransactionItem>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getDate().substring(0, 7), LinkedHashMap::new, Collectors.toList()));
        renderGroupedDetails(document, grouped, colors, fonts, lang);
    }

    private void addDetailsGroupedByEntity(Document document, List<TransactionItem> transactions, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts, String lang) throws DocumentException {
        Map<String, List<TransactionItem>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> (t.getCategory() != null && t.getCategory().getFinancialEntity() != null) 
                    ? t.getCategory().getFinancialEntity().getName() : "Personal / Other", LinkedHashMap::new, Collectors.toList()));
        renderGroupedDetails(document, grouped, colors, fonts, lang);
    }

    private void addDetailsGroupedByBeneficiary(Document document, List<TransactionItem> transactions, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts, String lang) throws DocumentException {
        Map<String, List<TransactionItem>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getBeneficiary() != null ? t.getBeneficiary().getName() : "No Beneficiary", LinkedHashMap::new, Collectors.toList()));
        renderGroupedDetails(document, grouped, colors, fonts, lang);
    }

    private void renderGroupedDetails(Document document, Map<String, List<TransactionItem>> grouped, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts, String lang) throws DocumentException {
        document.add(new Paragraph(translationService.getLabel("details", lang), fonts.fontSubtitle));
        document.add(new Paragraph(" ", fonts.fontBody));

        for (Map.Entry<String, List<TransactionItem>> entry : grouped.entrySet()) {
            String groupKey = entry.getKey();
            List<TransactionItem> groupTxs = entry.getValue();

            double income = groupTxs.stream().filter(t -> !t.getAmount().isNegative()).mapToDouble(t -> t.getAmount().getValue()).sum() / 100.0;
            double expense = groupTxs.stream().filter(t -> t.getAmount().isNegative()).mapToDouble(t -> Math.abs(t.getAmount().getValue())).sum() / 100.0;
            double net = income - expense;

            PdfPTable groupHeader = new PdfPTable(4);
            groupHeader.setWidthPercentage(100);
            groupHeader.setSpacingBefore(10);

            PdfPCell gCell = new PdfPCell(new Phrase(groupKey, fonts.fontMonth));
            gCell.setBorder(Rectangle.NO_BORDER);
            gCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            groupHeader.addCell(gCell);

            styler.addSummaryMiniCard(groupHeader, translationService.getLabel("mo_inc", lang), income, colors.positiveGreen, fonts.fontLabel, fonts.fontSummaryVal);
            styler.addSummaryMiniCard(groupHeader, translationService.getLabel("mo_exp", lang), expense, Color.RED, fonts.fontLabel, fonts.fontSummaryVal);
            styler.addSummaryMiniCard(groupHeader, translationService.getLabel("mo_net", lang), net, net >= 0 ? Color.BLUE : Color.RED, fonts.fontLabel, fonts.fontSummaryVal);
            document.add(groupHeader);

            document.add(new Paragraph(" ", fonts.fontBody));

            PdfPTable txTable = new PdfPTable(4);
            txTable.setWidthPercentage(100);
            txTable.setWidths(new float[]{1, 2, 3, 1});
            styler.writeModernHeader(txTable, fonts.fontHeader, colors.darkBlue, new String[]{
                    translationService.getLabel("tx_date", lang),
                    translationService.getLabel("tx_cat", lang),
                    translationService.getLabel("tx_desc", lang),
                    translationService.getLabel("tx_amt", lang)
            });

            for (TransactionItem tx : groupTxs) {
                String catName = tx.getCategory() != null ? tx.getCategory().getName() : "General";
                if (tx.getSubcategory() != null) catName += " > " + tx.getSubcategory().getName();
                txTable.addCell(styler.createCell(tx.getDate().split("T")[0], fonts.fontBody, false));
                txTable.addCell(styler.createCell(catName, fonts.fontBody, false));
                txTable.addCell(styler.createCell(tx.getDescription(), fonts.fontBody, false));
                double val = tx.getAmount().getValue() / 100.0;
                Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, tx.getAmount().isNegative() ? Color.RED : colors.positiveGreen);
                txTable.addCell(styler.createCell((tx.getAmount().isNegative() ? "-" : "+") + " €" + String.format("%.2f", Math.abs(val)), amountFont, true));
            }
            document.add(txTable);
            document.add(new Paragraph(" "));
        }
    }
}

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
    private final ReportTranslationService translationService;
    private final ReportStyler styler;

    public byte[] generatePdfReport(String title, String chartType, String startDate, String endDate, List<Long> accountIds, List<Long> categoryIds, String lang) {
        User user = userContextPort.getCurrentUser();

        List<AccountItem> allAccounts = accountPersistencePort.findAllByUser(user);
        List<AccountItem> filteredAccounts = (accountIds == null || accountIds.isEmpty())
                ? allAccounts
                : allAccounts.stream().filter(a -> accountIds.contains(a.getId())).collect(Collectors.toList());

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
            addAccountsOverview(document, filteredAccounts, colors, fonts, lang);
            addTransactionDetails(document, filteredTransactions, colors, fonts, lang);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating monthly summary PDF", e);
        }
    }

    private void addHeader(Document document, String title, String lang, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 1});
        PdfPCell titleCell = new PdfPCell(new Phrase("CASHKEEP", fonts.fontTitle));
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

    private void addTransactionDetails(Document document, List<TransactionItem> transactions, ReportStyler.ReportColors colors, ReportStyler.ReportFonts fonts, String lang) throws DocumentException {
        document.add(new Paragraph(translationService.getLabel("details", lang), fonts.fontSubtitle));
        document.add(new Paragraph(" ", fonts.fontBody));

        Map<String, List<TransactionItem>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getDate().substring(0, 7), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<TransactionItem>> monthEntry : grouped.entrySet()) {
            String monthKey = monthEntry.getKey();
            List<TransactionItem> monthTxs = monthEntry.getValue();

            double monthIncome = monthTxs.stream().filter(t -> !t.getAmount().isNegative()).mapToDouble(t -> t.getAmount().getValue()).sum() / 100.0;
            double monthExpense = monthTxs.stream().filter(t -> t.getAmount().isNegative()).mapToDouble(t -> Math.abs(t.getAmount().getValue())).sum() / 100.0;
            double monthNet = monthIncome - monthExpense;

            PdfPTable monthHeader = new PdfPTable(4);
            monthHeader.setWidthPercentage(100);
            monthHeader.setSpacingBefore(10);

            PdfPCell mCell = new PdfPCell(new Phrase(monthKey, fonts.fontMonth));
            mCell.setBorder(Rectangle.NO_BORDER);
            mCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            monthHeader.addCell(mCell);

            styler.addSummaryMiniCard(monthHeader, translationService.getLabel("mo_inc", lang), monthIncome, colors.positiveGreen, fonts.fontLabel, fonts.fontSummaryVal);
            styler.addSummaryMiniCard(monthHeader, translationService.getLabel("mo_exp", lang), monthExpense, Color.RED, fonts.fontLabel, fonts.fontSummaryVal);
            styler.addSummaryMiniCard(monthHeader, translationService.getLabel("mo_net", lang), monthNet, monthNet >= 0 ? Color.BLUE : Color.RED, fonts.fontLabel, fonts.fontSummaryVal);
            document.add(monthHeader);

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

            for (TransactionItem tx : monthTxs) {
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

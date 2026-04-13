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
import java.util.*;
import java.util.List;
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

    private String getLabel(String key, String lang) {
        Map<String, Map<String, String>> translations = new HashMap<>();
        
        Map<String, String> es = new HashMap<>();
        es.put("title", "INFORME FINANCIERO");
        es.put("balance", "SALDO ACTUAL");
        es.put("expenses", "GASTOS DEL PERIODO");
        es.put("date", "FECHA DEL INFORME");
        es.put("overview", "VISTA GENERAL DE CUENTAS");
        es.put("details", "DETALLE DE TRANSACCIONES");
        es.put("acc_name", "Nombre de Cuenta");
        es.put("acc_type", "Tipo");
        es.put("acc_bal", "Saldo");
        es.put("tx_date", "Fecha");
        es.put("tx_cat", "Categoría");
        es.put("tx_desc", "Descripción");
        es.put("tx_amt", "Importe");
        es.put("mo_inc", "Ingresos");
        es.put("mo_exp", "Gastos");
        es.put("mo_net", "Neto");
        translations.put("es", es);

        Map<String, String> pt = new HashMap<>();
        pt.put("title", "RELATÓRIO FINANCEIRO");
        pt.put("balance", "SALDO ATUAL");
        pt.put("expenses", "DESPESAS DO PERÍODO");
        pt.put("date", "DATA DO RELATÓRIO");
        pt.put("overview", "VISÃO GERAL DAS CONTAS");
        pt.put("details", "DETALHES DAS TRANSAÇÕES");
        pt.put("acc_name", "Nome da Conta");
        pt.put("acc_type", "Tipo");
        pt.put("acc_bal", "Saldo");
        pt.put("tx_date", "Data");
        pt.put("tx_cat", "Categoria");
        pt.put("tx_desc", "Descrição");
        pt.put("tx_amt", "Valor");
        pt.put("mo_inc", "Receitas");
        pt.put("mo_exp", "Despesas");
        pt.put("mo_net", "Líquido");
        translations.put("pt", pt);

        Map<String, String> en = new HashMap<>();
        en.put("title", "FINANCIAL REPORT");
        en.put("balance", "CURRENT BALANCE");
        en.put("expenses", "PERIOD EXPENSES");
        en.put("date", "REPORT DATE");
        en.put("overview", "ACCOUNTS OVERVIEW");
        en.put("details", "TRANSACTION DETAILS");
        en.put("acc_name", "Account Name");
        en.put("acc_type", "Type");
        en.put("acc_bal", "Balance");
        en.put("tx_date", "Date");
        en.put("tx_cat", "Category");
        en.put("tx_desc", "Description");
        en.put("tx_amt", "Amount");
        en.put("mo_inc", "Income");
        en.put("mo_exp", "Expenses");
        en.put("mo_net", "Net");
        translations.put("en", en);

        return translations.getOrDefault(lang.toLowerCase(), translations.get("en")).getOrDefault(key, key);
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

    public byte[] generatePdfReport(String title, String chartType, String startDate, String endDate, List<Long> accountIds, List<Long> categoryIds, String lang) {
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
                .sorted(Comparator.comparing(TransactionItemEntity::getDate).reversed())
                .collect(Collectors.toList());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            Color primaryBlue = new Color(0, 159, 251);
            Color darkBlue = new Color(74, 99, 111);
            Color softBg = new Color(245, 248, 250);

            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, primaryBlue);
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, darkBlue);
            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, darkBlue);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontMonth = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primaryBlue);
            Font fontSummaryVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkBlue);

            // 1. TOP HEADER
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{2, 1});
            PdfPCell titleCell = new PdfPCell(new Phrase("CASHKEEP", fontTitle));
            titleCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(titleCell);
            PdfPCell infoCell = new PdfPCell();
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            infoCell.addElement(new Paragraph(getLabel("title", lang), fontLabel));
            infoCell.addElement(new Paragraph(title.toUpperCase(), fontSubtitle));
            headerTable.addCell(infoCell);
            document.add(headerTable);
            document.add(new Paragraph(" "));
            document.add(new LineSeparator(1f, 100, primaryBlue, Element.ALIGN_CENTER, -2));
            document.add(new Paragraph(" "));

            // 2. ACCOUNTS OVERVIEW
            document.add(new Paragraph(getLabel("overview", lang), fontSubtitle));
            document.add(new Paragraph(" ", fontBody));
            PdfPTable accTable = new PdfPTable(3);
            accTable.setWidthPercentage(100);
            writeModernHeader(accTable, fontHeader, primaryBlue, new String[]{getLabel("acc_name", lang), getLabel("acc_type", lang), getLabel("acc_bal", lang)});
            for (AccountEntity acc : filteredAccounts) {
                accTable.addCell(createCell(acc.getName(), fontBody, false));
                accTable.addCell(createCell(acc.getAccountType() != null ? acc.getAccountType() : "Standard", fontBody, false));
                double bal = acc.getAmount().getValue() / 100.0;
                accTable.addCell(createCell("€ " + String.format("%.2f", bal), fontBody, true));
            }
            document.add(accTable);
            document.add(new Paragraph(" "));

            // 3. TRANSACTION HISTORY GROUPED BY MONTH WITH SUMMARIES
            document.add(new Paragraph(getLabel("details", lang), fontSubtitle));
            document.add(new Paragraph(" ", fontBody));

            Map<String, List<TransactionItemEntity>> grouped = filteredTransactions.stream()
                .collect(Collectors.groupingBy(t -> t.getDate().substring(0, 7), LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<TransactionItemEntity>> monthEntry : grouped.entrySet()) {
                String monthKey = monthEntry.getKey();
                List<TransactionItemEntity> monthTxs = monthEntry.getValue();

                // Calcula totales del mes
                double monthIncome = monthTxs.stream().filter(t -> !t.getAmount().isNegative()).mapToDouble(t -> t.getAmount().getValue()).sum() / 100.0;
                double monthExpense = monthTxs.stream().filter(t -> t.getAmount().isNegative()).mapToDouble(t -> Math.abs(t.getAmount().getValue())).sum() / 100.0;
                double monthNet = monthIncome - monthExpense;

                // Título y Resumen del mes
                PdfPTable monthHeader = new PdfPTable(4);
                monthHeader.setWidthPercentage(100);
                monthHeader.setSpacingBefore(10);
                
                PdfPCell mCell = new PdfPCell(new Phrase(monthKey, fontMonth));
                mCell.setBorder(Rectangle.NO_BORDER);
                mCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                monthHeader.addCell(mCell);

                addSummaryMiniCard(monthHeader, getLabel("mo_inc", lang), monthIncome, new Color(39, 174, 96), fontLabel, fontSummaryVal);
                addSummaryMiniCard(monthHeader, getLabel("mo_exp", lang), monthExpense, Color.RED, fontLabel, fontSummaryVal);
                addSummaryMiniCard(monthHeader, getLabel("mo_net", lang), monthNet, monthNet >= 0 ? Color.BLUE : Color.RED, fontLabel, fontSummaryVal);
                document.add(monthHeader);

                document.add(new Paragraph(" ", fontBody));

                PdfPTable txTable = new PdfPTable(4);
                txTable.setWidthPercentage(100);
                txTable.setWidths(new float[]{1, 2, 3, 1});
                writeModernHeader(txTable, fontHeader, darkBlue, new String[]{getLabel("tx_date", lang), getLabel("tx_cat", lang), getLabel("tx_desc", lang), getLabel("tx_amt", lang)});
                
                for (TransactionItemEntity tx : monthTxs) {
                    String catName = tx.getCategory() != null ? tx.getCategory().getName() : "General";
                    if (tx.getSubcategory() != null) catName += " > " + tx.getSubcategory().getName();
                    txTable.addCell(createCell(tx.getDate().split("T")[0], fontBody, false));
                    txTable.addCell(createCell(catName, fontBody, false));
                    txTable.addCell(createCell(tx.getDescription(), fontBody, false));
                    double val = tx.getAmount().getValue() / 100.0;
                    Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, tx.getAmount().isNegative() ? Color.RED : new Color(39, 174, 96));
                    txTable.addCell(createCell((tx.getAmount().isNegative() ? "-" : "+") + " €" + String.format("%.2f", Math.abs(val)), amountFont, true));
                }
                document.add(txTable);
                document.add(new Paragraph(" "));
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating monthly summary PDF", e);
        }
    }

    private void addSummaryMiniCard(PdfPTable table, String label, double value, Color color, Font fontLabel, Font fontVal) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(250, 250, 250));
        cell.setPadding(5);
        cell.setBorderColor(new Color(230, 230, 230));
        cell.addElement(new Phrase(label, fontLabel));
        Font vFont = new Font(fontVal);
        vFont.setColor(color);
        cell.addElement(new Phrase("€ " + String.format("%.2f", value), vFont));
        table.addCell(cell);
    }

    private void writeModernHeader(PdfPTable table, Font font, Color bgColor, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(6);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private PdfPCell createCell(String text, Font font, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(new Color(240, 240, 240));
        if (alignRight) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }
}

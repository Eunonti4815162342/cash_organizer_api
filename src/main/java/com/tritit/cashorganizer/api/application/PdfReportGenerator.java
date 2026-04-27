package com.tritit.cashorganizer.api.application;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.tritit.cashorganizer.api.domain.model.DetailedReport;
import com.tritit.cashorganizer.api.domain.model.TransactionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfReportGenerator {

    private final ReportStyler styler;
    private final ReportTranslationService translationService;

    public byte[] generatePdfReport(DetailedReport report, String title, String lang) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, out);
            document.open();

            ReportStyler.ReportColors colors = styler.getColors();
            ReportStyler.ReportFonts fonts = styler.getFonts(colors);

            // 1. Cabecera Minimalista
            addModernHeader(document, title, report.getPeriod(), fonts, lang);
            
            // 2. Resumen General en Tabla Estilada
            addModernSummary(document, report.getCategorySummary(), fonts, colors, lang);
            
            document.add(new Paragraph(" "));
            document.add(new LineSeparator(1f, 100, colors.border, Element.ALIGN_CENTER, -2));
            document.add(new Paragraph(" "));

            // 3. Contenido Segregado
            if (report.getSegregatedData().isEmpty()) {
                document.add(new Paragraph(translationService.getLabel("no_data", lang), fonts.fontBody));
            } else {
                renderModernSegregatedContent(document, report.getSegregatedData(), fonts, colors, lang);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error rendering modern PDF", e);
        }
    }

    private void addModernHeader(Document document, String title, String period, ReportStyler.ReportFonts fonts, String lang) throws DocumentException {
        Paragraph titleP = new Paragraph("NATAVE", fonts.fontTitle);
        titleP.setAlignment(Element.ALIGN_LEFT);
        document.add(titleP);

        Paragraph subtitleP = new Paragraph(title.toUpperCase(), fonts.fontSubtitle);
        subtitleP.setAlignment(Element.ALIGN_LEFT);
        document.add(subtitleP);

        String displayPeriod = (period != null) ? period.replaceAll("T[0-9:.]*", "") : "";
        Paragraph periodP = new Paragraph("PERIODO: " + displayPeriod, fonts.fontSmall);
        periodP.setAlignment(Element.ALIGN_LEFT);
        document.add(periodP);
        
        document.add(new Paragraph(" "));
    }

    private void addModernSummary(Document document, Map<String, Long> summary, ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors, String lang) throws DocumentException {
        if (summary.isEmpty()) return;
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        
        PdfPCell header = new PdfPCell(new Phrase(translationService.getLabel("overview", lang).toUpperCase(), fonts.fontHeader));
        header.setColspan(2);
        header.setBorder(Rectangle.NO_BORDER);
        header.setPaddingBottom(10);
        table.addCell(header);

        for (var entry : summary.entrySet()) {
            table.addCell(styler.createCell(entry.getKey(), fonts.fontBody, false));
            table.addCell(styler.createCell("€ " + String.format("%.2f", entry.getValue()/100.0), fonts.fontHeader, true));
        }
        document.add(table);
    }

    private void renderModernSegregatedContent(Document document, Map<String, Map<String, List<TransactionItem>>> data, ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors, String lang) throws DocumentException {
        for (var entityEntry : data.entrySet()) {
            String entityName = entityEntry.getKey();
            
            document.add(new Paragraph(" "));
            PdfPTable badge = new PdfPTable(1);
            badge.setWidthPercentage(100);
            PdfPCell cell = new PdfPCell(new Phrase("EMPRESA: " + entityName.toUpperCase(), fonts.fontSubtitle));
            cell.setBackgroundColor(new Color(240, 247, 255));
            cell.setPadding(12);
            cell.setBorder(Rectangle.LEFT);
            cell.setBorderWidthLeft(4f);
            cell.setBorderColorLeft(colors.primaryBlue);
            badge.addCell(cell);
            document.add(badge);
            document.add(new Paragraph(" "));

            for (var accEntry : entityEntry.getValue().entrySet()) {
                Paragraph accTitle = new Paragraph(translationService.getLabel("acc_name", lang).toUpperCase() + ": " + accEntry.getKey(), fonts.fontHeader);
                accTitle.setSpacingAfter(8);
                document.add(accTitle);

                PdfPTable txTable = new PdfPTable(4);
                txTable.setWidthPercentage(100);
                txTable.setWidths(new float[]{1.2f, 2, 3, 1.2f});
                styler.writeModernHeader(txTable, fonts.fontHeader, null, new String[]{
                    translationService.getLabel("tx_date", lang),
                    translationService.getLabel("tx_cat", lang),
                    translationService.getLabel("tx_desc", lang),
                    translationService.getLabel("tx_amt", lang)
                });

                for (TransactionItem tx : accEntry.getValue()) {
                    String cat = (tx.getCategory() != null ? tx.getCategory().getName() : "Otros") + 
                                (tx.getSubcategory() != null ? " > " + tx.getSubcategory().getName() : "");
                    String date = tx.getDate().split("T")[0];
                    
                    txTable.addCell(styler.createCell(date, fonts.fontBody, false));
                    txTable.addCell(styler.createCell(cat, fonts.fontBody, false));
                    txTable.addCell(styler.createCell(tx.getDescription() != null ? tx.getDescription() : "", fonts.fontBody, false));
                    
                    double val = tx.getAmount().getValue() / 100.0;
                    String sign = tx.getAmount().isNegative() ? "-" : "+";
                    txTable.addCell(styler.createCell(sign + " €" + String.format("%.2f", Math.abs(val)), fonts.fontBody, true));
                }
                document.add(txTable);
                document.add(new Paragraph(" "));
            }
            document.newPage(); 
        }
    }
}

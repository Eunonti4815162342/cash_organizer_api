package com.tritit.cashorganizer.api.application;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
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
            Document document = new Document(PageSize.A4, 40, 40, 40, 50);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte cb = writer.getDirectContent();
                    try {
                        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                        Font footerFont = new Font(bf, 7, Font.NORMAL, new Color(160, 170, 182));

                        cb.saveState();
                        cb.setColorStroke(new Color(220, 228, 238));
                        cb.setLineWidth(0.5f);
                        cb.moveTo(document.left(), document.bottom() - 12);
                        cb.lineTo(document.right(), document.bottom() - 12);
                        cb.stroke();
                        cb.restoreState();

                        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                            new Phrase("NATAVE", footerFont),
                            document.left(), document.bottom() - 24, 0);
                        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                            new Phrase("Página " + writer.getPageNumber(), footerFont),
                            document.right(), document.bottom() - 24, 0);
                    } catch (Exception ignored) {}
                }
            });

            document.open();

            ReportStyler.ReportColors colors = styler.getColors();
            ReportStyler.ReportFonts fonts   = styler.getFonts(colors);

            addHeader(document, report, fonts, colors, lang);
            addBalanceSummary(document, report, fonts, colors, lang);
            spacer(document);
            addCategorySummary(document, report.getCategorySummary(), fonts, colors, lang);
            spacer(document);

            if (!report.getSegregatedData().isEmpty()) {
                renderSegregatedContent(document, report.getSegregatedData(), fonts, colors, lang);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error rendering PDF", e);
        }
    }

    // ─── Header ──────────────────────────────────────────────────────────────

    private void addHeader(Document document, DetailedReport report,
                           ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors,
                           String lang) throws DocumentException {
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);

        Font appFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, Color.WHITE);
        Font subFont   = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(190, 225, 255));
        Font metaFont  = FontFactory.getFont(FontFactory.HELVETICA, 8,  new Color(170, 210, 250));

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(colors.primaryBlue);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(28);
        cell.setPaddingBottom(24);
        cell.setPaddingLeft(32);
        cell.setPaddingRight(32);

        cell.addElement(new Paragraph("NATAVE", appFont));

        Paragraph sub = new Paragraph(translationService.getLabel("title", lang).toUpperCase(), subFont);
        sub.setSpacingBefore(4);
        cell.addElement(sub);

        Paragraph meta = new Paragraph(
            translationService.getLabel("period_label", lang) + ": " + report.getPeriod()
            + "   ·   " + report.getTotalTransactions() + " movimientos", metaFont);
        meta.setSpacingBefore(12);
        cell.addElement(meta);

        band.addCell(cell);
        document.add(band);
        spacer(document);
    }

    // ─── KPI Balance ─────────────────────────────────────────────────────────

    private void addBalanceSummary(Document document, DetailedReport report,
                                   ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors,
                                   String lang) throws DocumentException {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8);

        long balance = report.getTotalIncomes() - report.getTotalExpenses();
        Color netColor = balance >= 0 ? colors.primaryBlue : new Color(230, 126, 34);

        addKpiCell(table, translationService.getLabel("mo_inc", lang), report.getTotalIncomes() / 100.0, colors.incomeGreen);
        addKpiCell(table, translationService.getLabel("mo_exp", lang), report.getTotalExpenses() / 100.0, colors.expenseRed);
        addKpiCell(table, translationService.getLabel("mo_net", lang), balance / 100.0, netColor);

        document.add(table);
    }

    private void addKpiCell(PdfPTable table, String label, double value, Color accentColor) {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 8,  new Color(125, 138, 155));
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, accentColor);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColorTop(accentColor);
        cell.setBorderWidthTop(3f);
        cell.setBackgroundColor(Color.WHITE);
        cell.setPaddingTop(14);
        cell.setPaddingBottom(16);
        cell.setPaddingLeft(16);
        cell.setPaddingRight(16);

        cell.addElement(new Paragraph(label.toUpperCase(), labelFont));
        Paragraph vp = new Paragraph("€ " + String.format("%.2f", value), valueFont);
        vp.setSpacingBefore(6);
        cell.addElement(vp);
        table.addCell(cell);
    }

    // ─── Category summary ────────────────────────────────────────────────────

    private void addCategorySummary(Document document, Map<String, Long> summary,
                                    ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors,
                                    String lang) throws DocumentException {
        if (summary.isEmpty()) return;

        Paragraph label = new Paragraph(translationService.getLabel("overview", lang).toUpperCase(),
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, colors.textLight));
        label.setSpacingAfter(8);
        document.add(label);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.5f});

        String catLabel = translationService.getLabel("tx_cat", lang);
        String amtLabel = translationService.getLabel("tx_amt", lang);
        writeHeaderCell(table, catLabel, fonts, colors, false);
        writeHeaderCell(table, amtLabel, fonts, colors, true);

        long total = summary.values().stream().mapToLong(Long::longValue).sum();
        int row = 0;
        for (var entry : summary.entrySet()) {
            Color bg = row++ % 2 == 0 ? Color.WHITE : colors.rowAlt;
            double pct = total > 0 ? entry.getValue() * 100.0 / total : 0;

            table.addCell(styler.createDataCell(
                entry.getKey() + "  (" + String.format("%.0f", pct) + "%)", fonts.fontBody, bg, false));
            table.addCell(styler.createDataCell(
                "€ " + String.format("%.2f", entry.getValue() / 100.0), fonts.fontHeader, bg, true));
        }
        document.add(table);
    }

    // ─── Segregated transactions ─────────────────────────────────────────────

    private void renderSegregatedContent(Document document,
                                         Map<String, Map<String, List<TransactionItem>>> data,
                                         ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors,
                                         String lang) throws DocumentException {
        for (var entityEntry : data.entrySet()) {
            spacer(document);
            addEntityHeader(document, entityEntry.getKey(), fonts, colors, lang);

            for (var accEntry : entityEntry.getValue().entrySet()) {
                spacer(document);
                addAccountHeader(document, accEntry.getKey(), fonts, colors, lang);
                spacer(document);
                addTransactionTable(document, accEntry.getValue(), fonts, colors, lang);
            }
            document.newPage();
        }
    }

    private void addEntityHeader(Document document, String name,
                                  ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors,
                                  String lang) throws DocumentException {
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);

        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        Font subFont  = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(190, 225, 255));

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(colors.primaryBlue);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(12);
        cell.setPaddingBottom(12);
        cell.setPaddingLeft(16);
        cell.setPaddingRight(16);

        cell.addElement(new Paragraph(name.toUpperCase(), nameFont));
        Paragraph sub = new Paragraph(translationService.getLabel("company", lang), subFont);
        sub.setSpacingBefore(2);
        cell.addElement(sub);
        band.addCell(cell);
        document.add(band);
    }

    private void addAccountHeader(Document document, String name,
                                   ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors,
                                   String lang) throws DocumentException {
        PdfPTable band = new PdfPTable(1);
        band.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(name, fonts.fontSubtitle));
        cell.setBackgroundColor(new Color(240, 246, 253));
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderWidthLeft(3f);
        cell.setBorderColorLeft(colors.primaryBlue);
        cell.setPadding(10);
        cell.setPaddingLeft(14);
        band.addCell(cell);
        document.add(band);
    }

    private void addTransactionTable(Document document, List<TransactionItem> txs,
                                      ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors,
                                      String lang) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.1f, 1.8f, 3f, 1.3f});

        styler.writeModernHeader(table, fonts.fontHeader, null, new String[]{
            translationService.getLabel("tx_date", lang),
            translationService.getLabel("tx_cat", lang),
            translationService.getLabel("tx_desc", lang),
            translationService.getLabel("tx_amt", lang)
        });

        int row = 0;
        for (TransactionItem tx : txs) {
            Color bg  = row++ % 2 == 0 ? Color.WHITE : colors.rowAlt;
            String cat = (tx.getCategory() != null ? tx.getCategory().getName() : "Otros")
                       + (tx.getSubcategory() != null ? " › " + tx.getSubcategory().getName() : "");
            String date = tx.getDate().contains("T") ? tx.getDate().split("T")[0] : tx.getDate();

            table.addCell(styler.createDataCell(date, fonts.fontBody, bg, false));
            table.addCell(styler.createDataCell(cat, fonts.fontBody, bg, false));
            table.addCell(styler.createDataCell(
                tx.getDescription() != null ? tx.getDescription() : "", fonts.fontBody, bg, false));

            boolean isNeg   = tx.getAmount().isNegative();
            double  val     = tx.getAmount().getValue() / 100.0;
            String  sign    = isNeg ? "−" : "+";
            Color   amtClr  = isNeg ? new Color(210, 50, 50) : new Color(0, 155, 90);
            Font    amtFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, amtClr);

            PdfPCell amtCell = new PdfPCell(new Phrase(sign + " €" + String.format("%.2f", Math.abs(val)), amtFont));
            amtCell.setBackgroundColor(bg);
            amtCell.setBorder(Rectangle.BOTTOM);
            amtCell.setBorderColor(new Color(238, 242, 246));
            amtCell.setPadding(8);
            amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amtCell);
        }
        document.add(table);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void writeHeaderCell(PdfPTable table, String text,
                                  ReportStyler.ReportFonts fonts, ReportStyler.ReportColors colors,
                                  boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text.toUpperCase(), fonts.fontHeader));
        cell.setBackgroundColor(new Color(242, 246, 250));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(colors.border);
        cell.setBorderWidthBottom(1.5f);
        cell.setPadding(10);
        if (alignRight) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private void spacer(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
    }
}

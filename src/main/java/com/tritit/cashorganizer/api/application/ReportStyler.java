package com.tritit.cashorganizer.api.application;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import java.awt.Color;

@Service
public class ReportStyler {

    public void addSummaryMiniCard(PdfPTable table, String label, double value, Color color, Font fontLabel, Font fontVal) {
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

    public void writeModernHeader(PdfPTable table, Font font, Color bgColor, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(bgColor);
            cell.setPadding(6);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    public PdfPCell createCell(String text, Font font, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(new Color(240, 240, 240));
        if (alignRight) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    public ReportColors getColors() {
        return new ReportColors();
    }

    public ReportFonts getFonts(ReportColors colors) {
        return new ReportFonts(colors);
    }

    public static class ReportColors {
        public final Color primaryBlue = new Color(0, 159, 251);
        public final Color darkBlue = new Color(74, 99, 111);
        public final Color softBg = new Color(245, 248, 250);
        public final Color positiveGreen = new Color(39, 174, 96);
    }

    public static class ReportFonts {
        public final Font fontTitle;
        public final Font fontSubtitle;
        public final Font fontLabel;
        public final Font fontBody;
        public final Font fontHeader;
        public final Font fontMonth;
        public final Font fontSummaryVal;

        public ReportFonts(ReportColors colors) {
            this.fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, colors.primaryBlue);
            this.fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, colors.darkBlue);
            this.fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.GRAY);
            this.fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, colors.darkBlue);
            this.fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            this.fontMonth = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, colors.primaryBlue);
            this.fontSummaryVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, colors.darkBlue);
        }
    }
}

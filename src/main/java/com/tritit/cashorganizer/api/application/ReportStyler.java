package com.tritit.cashorganizer.api.application;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import java.awt.Color;

@Service
public class ReportStyler {

    public PdfPCell createCell(String text, Font font, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(8);
        cell.setBorderColor(new Color(230, 235, 240));
        cell.setBorder(Rectangle.BOTTOM);
        if (alignRight) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    public PdfPCell createDataCell(String text, Font font, Color background, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(background);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(238, 242, 246));
        cell.setPadding(8);
        if (alignRight) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    public void writeModernHeader(PdfPTable table, Font font, Color bgColor, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h.toUpperCase(), font));
            cell.setBackgroundColor(new Color(242, 246, 250));
            cell.setPadding(10);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(new Color(200, 210, 220));
            cell.setBorderWidthBottom(1.5f);
            table.addCell(cell);
        }
    }

    public ReportColors getColors() {
        return new ReportColors();
    }

    public ReportFonts getFonts(ReportColors colors) {
        return new ReportFonts(colors);
    }

    public static class ReportColors {
        public final Color primaryBlue  = new Color(0, 159, 251);
        public final Color incomeGreen  = new Color(0, 168, 107);
        public final Color expenseRed   = new Color(220, 53, 69);
        public final Color textMain     = new Color(30, 40, 55);
        public final Color textLight    = new Color(110, 125, 145);
        public final Color softBg       = new Color(247, 250, 252);
        public final Color rowAlt       = new Color(250, 252, 254);
        public final Color border       = new Color(226, 232, 240);
    }

    public static class ReportFonts {
        public final Font fontTitle;
        public final Font fontSubtitle;
        public final Font fontHeader;
        public final Font fontBody;
        public final Font fontSmall;

        public ReportFonts(ReportColors colors) {
            this.fontTitle    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, colors.primaryBlue);
            this.fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, colors.textMain);
            this.fontHeader   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9,  colors.textMain);
            this.fontBody     = FontFactory.getFont(FontFactory.HELVETICA, 9,        colors.textMain);
            this.fontSmall    = FontFactory.getFont(FontFactory.HELVETICA, 8,        colors.textLight);
        }
    }
}

package com.tritit.cashorganizer.api.application;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import java.awt.Color;

@Service
public class ReportStyler {

    public PdfPCell createCell(String text, Font font, boolean alignRight) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setBorderColor(new Color(230, 235, 240));
        cell.setBorder(Rectangle.BOTTOM); // Estilo moderno: solo línea inferior
        if (alignRight) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    public void writeModernHeader(PdfPTable table, Font font, Color bgColor, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h.toUpperCase(), font));
            cell.setBackgroundColor(new Color(245, 248, 250)); // Fondo gris muy suave
            cell.setPadding(10);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(new Color(200, 210, 220));
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
        public final Color primaryBlue = new Color(0, 159, 251);
        public final Color textMain = new Color(45, 55, 72);
        public final Color textLight = new Color(113, 128, 150);
        public final Color softBg = new Color(247, 250, 252);
        public final Color border = new Color(226, 232, 240);
    }

    public static class ReportFonts {
        public final Font fontTitle;
        public final Font fontSubtitle;
        public final Font fontHeader;
        public final Font fontBody;
        public final Font fontSmall;

        public ReportFonts(ReportColors colors) {
            this.fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, colors.primaryBlue);
            this.fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, colors.textMain);
            this.fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, colors.textMain);
            this.fontBody = FontFactory.getFont(FontFactory.HELVETICA, 9, colors.textMain);
            this.fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 8, colors.textLight);
        }
    }
}

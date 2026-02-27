package utils;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.borders.Border;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PdfLogger {
    private static PdfWriter writer;
    private static PdfDocument pdf;
    private static Document document;

    public static void initReport(String filePath) {
        try {
            writer = new PdfWriter(filePath);
            pdf = new PdfDocument(writer);
            document = new Document(pdf);

            // Title (centered using table)
            float[] titleColWidth = {1};
            Table titleTable = new Table(titleColWidth);
            titleTable.setWidth(500);
            Cell titleCell = new Cell().add(new Paragraph("County Management Test Report")
                    .setBold().setFontSize(18));
            titleCell.setBorder(Border.NO_BORDER);
            titleCell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            titleTable.addCell(titleCell);
            document.add(titleTable);

            // Date/Time (right aligned using table)
            float[] dateColWidth = {1};
            Table dateTable = new Table(dateColWidth);
            dateTable.setWidth(500);
            Cell dateCell = new Cell().add(new Paragraph("Generated on: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFontSize(10));
            dateCell.setBorder(Border.NO_BORDER);
            dateCell.setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT);
            dateTable.addCell(dateCell);
            document.add(dateTable);

            document.add(new Paragraph("\n")); // spacing

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void logPass(String message) {
        document.add(new Paragraph("✅ PASS: " + message)
                .setFontSize(12)
                .setFontColor(ColorConstants.GREEN));
    }

    public static void logFail(String message) {
        document.add(new Paragraph("❌ FAIL: " + message)
                .setFontSize(12)
                .setFontColor(ColorConstants.RED));
    }

    public static void logInfo(String message) {
        document.add(new Paragraph("ℹ️ INFO: " + message)
                .setFontSize(12)
                .setFontColor(ColorConstants.BLUE));
    }

    public static void closeReport() {
        if (document != null) {
            document.close();
            System.out.println("✅ PDF Report generated successfully!");
        }
    }

	public static void close() {
		// TODO Auto-generated method stub
		
	}
}

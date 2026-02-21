package utils;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PdfLogger {
    private static PdfDocument pdfDoc;
    private static Document document;

    public static void initPdfLogger() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String pdfFile = "logs/test_log_" + timestamp + ".pdf";
            pdfDoc = new PdfDocument(new PdfWriter(pdfFile));
            document = new Document(pdfDoc);
            log("📝 PDF Log file created: " + pdfFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        String timeStamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String finalMsg = "[" + timeStamp + "] " + message;
        System.out.println(finalMsg);  // console
        if (document != null) {
            document.add(new Paragraph(finalMsg));
        }
    }

    public static void closePdfLogger() {
        if (document != null) {
            document.close();
        }
    }
}

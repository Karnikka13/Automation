package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AdvancedSearchLogger {

    private static final String FILE_PATH = "src/test/resources/testdata/AdvancedSearchResults.xlsx";

    private static Workbook workbook;
    private static Sheet sheet;
    private static int currentRow = 1;

    static {
        initializeWorkbook();
    }

    /** Create new Excel with styled headers */
    private static void initializeWorkbook() {
        try {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("RowBased Results");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 13);

            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row header = sheet.createRow(0);

            String[] columns = {
                    "Scenario", "Timestamp", "Expected Result",
                    "Actual Result", "Status", "Error Message"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(columns[i]);
                c.setCellStyle(headerStyle);
            }

            save();

        } catch (Exception e) {
            System.out.println("❌ Failed to init workbook: " + e.getMessage());
        }
    }

    /** Write a row as log */
    public static synchronized void log(
            String scenario,
            String expected,
            String actual,
            String status,
            String errorMessage
    ) {
        try {

            Row row = sheet.createRow(currentRow++);
            String timestamp =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // Common style
            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setWrapText(true);

            // PASS Style
            CellStyle passStyle = workbook.createCellStyle();
            passStyle.cloneStyleFrom(normalStyle);
            passStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // FAIL Style
            CellStyle failStyle = workbook.createCellStyle();
            failStyle.cloneStyleFrom(normalStyle);
            failStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Write columns
            row.createCell(0).setCellValue(scenario);
            row.createCell(1).setCellValue(timestamp);
            row.createCell(2).setCellValue(expected);
            row.createCell(3).setCellValue(actual);

            // Status
            Cell statusCell = row.createCell(4);
            statusCell.setCellValue(status);
            if (status.equalsIgnoreCase("PASS")) statusCell.setCellStyle(passStyle);
            else if (status.equalsIgnoreCase("FAIL")) statusCell.setCellStyle(failStyle);

            // Error message
            row.createCell(5).setCellValue(errorMessage == null ? "" : errorMessage);

            // Auto size
            for (int i = 0; i < 6; i++) sheet.autoSizeColumn(i);

            save();

        } catch (Exception e) {
            System.out.println("❌ Failed to write log row: " + e.getMessage());
        }
    }


    private static synchronized void save() {
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            workbook.write(fos);
            fos.flush();
        } catch (Exception e) {
            System.out.println("❌ Failed to save Excel file: " + e.getMessage());
        }
    }
}

package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExcelRowLogger {

    private static final String FILE_PATH = "AdvancedSearch_Log.xlsx";

    private static Workbook workbook;
    private static Sheet sheet;

    static {
        initializeWorkbook();
    }

    /** Initialize sheet with blue header (only once) */
    private static void initializeWorkbook() {
        try {
            File file = new File(FILE_PATH);

            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheetAt(0);
                fis.close();
                return;
            }

            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Advanced Search Logs");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);

            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row header = sheet.createRow(0);

            String[] cols = { "Scenario", "Timestamp", "Expected Result",
                    "Actual Result", "Status", "Error Message" };

            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            save();

        } catch (Exception e) {
            System.out.println("❌ Failed initializing Excel header: " + e.getMessage());
        }
    }

    /**
     *  LOGGING FUNCTION (OVERWRITES SAME ROW)
     */
    public static synchronized void logOverwrite(
            int rowIndex,
            String scenario,
            String expected,
            String actual,
            String status,
            String errorMsg
    ) {

        try {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                row = sheet.createRow(rowIndex);
            }

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            CellStyle statusStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);

            if (status.equalsIgnoreCase("PASS")) {
                font.setColor(IndexedColors.WHITE.getIndex());
                statusStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            } else if (status.equalsIgnoreCase("FAIL")) {
                font.setColor(IndexedColors.WHITE.getIndex());
                statusStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
            } else {
                font.setColor(IndexedColors.BLACK.getIndex());
                statusStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }

            statusStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            statusStyle.setFont(font);

            row.createCell(0).setCellValue(scenario);
            row.createCell(1).setCellValue(time);
            row.createCell(2).setCellValue(expected);
            row.createCell(3).setCellValue(actual);

            Cell statusCell = row.createCell(4);
            statusCell.setCellValue(status);
            statusCell.setCellStyle(statusStyle);

            row.createCell(5).setCellValue(errorMsg == null ? "" : errorMsg);

            for (int i = 0; i <= 5; i++) sheet.autoSizeColumn(i);

            save();

        } catch (Exception e) {
            System.out.println("❌ Error logging row: " + e.getMessage());
        }
    }

    private static void save() {
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
            workbook.write(fos);
        } catch (Exception e) {
            System.out.println("❌ Excel save failed: " + e.getMessage());
        }
    }
}

package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestLogger {

    private static final String LOG_PATH = "src/test/resources/testlog/test_log.xlsx";

    public static synchronized void log(String step, boolean status, String error) {
        log(step, status, error, getCallerClassName());
    }

    public static synchronized void log(String step, boolean status, String error, String sheetName) {
        try {
            File file = new File(LOG_PATH);
            File parentDir = file.getParentFile();

            // ✅ Auto-create folder if missing
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Workbook workbook;
            Sheet sheet;

            // ✅ Create new workbook or open existing
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    workbook = new XSSFWorkbook(fis);
                }
            } else {
                workbook = new XSSFWorkbook();
            }

            // ✅ Create or get specific sheet
            sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Timestamp");
                header.createCell(1).setCellValue("Scenario/Step");
                header.createCell(2).setCellValue("Status");
                header.createCell(3).setCellValue("Error Message");
            }

            int lastRowNum = sheet.getLastRowNum();
            Row row = sheet.createRow(lastRowNum + 1);

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            row.createCell(0).setCellValue(timestamp);
            row.createCell(1).setCellValue(step);
            row.createCell(2).setCellValue(status ? "PASS" : "FAIL");
            row.createCell(3).setCellValue(error == null ? "" : error);

            // ✅ Autosize columns
            for (int i = 0; i <= 3; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            workbook.close();

        } catch (Exception e) {
            System.out.println("⚠️ Failed to write log: " + e.getMessage());
        }
    }

    // ✅ Automatically detect which class called the logger (e.g. SearchTest)
    private static String getCallerClassName() {
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if (ste.getClassName().startsWith("stepdefinitions.")) {
                String[] parts = ste.getClassName().split("\\.");
                return parts[parts.length - 1].replace("Steps", "").replace("Test", "");
            }
        }
        return "TestLogs";
    }
}

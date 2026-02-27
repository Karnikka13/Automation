package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExcelLogger {

    private static final String DEFAULT_FILE_NAME = "ExecutionLogs.xlsx";
    private static Workbook workbook;
    private static Sheet sheet;
    private static int currentRow = 0;
    private static String reportPath = null;

    // styles
    private static CellStyle headerStyle;
    private static CellStyle normalStyle;
    private static CellStyle passStyle;
    private static CellStyle failStyle;
    private static CellStyle sectionTitleStyle;

    /**
     * Initialize report with a path (E:\\selenium\\CountyReport.xlsx).
     * If path is null or empty, defaults to working dir DEFAULT_FILE_NAME.
     * ALWAYS deletes existing file and creates a fresh one.
     */
    public static synchronized void initReport(String path) {
        reportPath = (path == null || path.trim().isEmpty()) ? DEFAULT_FILE_NAME : path;
        
        // Delete existing file if it exists to ensure fresh start
        try {
            File existingFile = new File(reportPath);
            if (existingFile.exists()) {
                if (existingFile.delete()) {
                    System.out.println("🗑️ Deleted existing report: " + reportPath);
                } else {
                    System.out.println("⚠️ Could not delete existing file: " + reportPath);
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not delete existing file: " + e.getMessage());
        }
        
        createNewWorkbook();
    }

    private static void createNewWorkbook() {
        try {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Execution Logs");
            currentRow = 0;

            createStyles();

            save();
            System.out.println("✅ ExcelLogger: New workbook created at: " + reportPath);
        } catch (Exception e) {
            System.out.println("❌ ExcelLogger.createNewWorkbook error: " + e.getMessage());
        }
    }

    private static void createStyles() {
        // === HEADER FONT ===
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        // === HEADER STYLE ===
        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);

        // === NORMAL STYLE ===
        normalStyle = workbook.createCellStyle();
        normalStyle.setWrapText(true);

        // === PASS STYLE (GREEN) ===
        passStyle = workbook.createCellStyle();
        passStyle.setWrapText(true);
        passStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        passStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font passFont = workbook.createFont();
        passFont.setBold(true);
        passFont.setColor(IndexedColors.BLACK.getIndex());
        passStyle.setFont(passFont);

        // === FAIL STYLE (RED) ===
        failStyle = workbook.createCellStyle();
        failStyle.setWrapText(true);
        failStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font failFont = workbook.createFont();
        failFont.setBold(true);
        failFont.setColor(IndexedColors.BLACK.getIndex());
        failStyle.setFont(failFont);

        // === SECTION TITLE STYLE ===
        sectionTitleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        sectionTitleStyle.setFont(titleFont);
        sectionTitleStyle.setAlignment(HorizontalAlignment.CENTER);
    }

    /**
     * Writes a section header block with styled header row.
     */
    public static synchronized void setSection(String sectionTitle) {
        if (workbook == null || sheet == null) {
            initReport(reportPath);
        }
        try {
            // blank row
            currentRow++;
            sheet.createRow(currentRow - 1);

            // title row
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("=== " + safeString(sectionTitle).toUpperCase() + " ===");
            titleCell.setCellStyle(sectionTitleStyle);

            sheet.addMergedRegion(new CellRangeAddress(titleRow.getRowNum(), titleRow.getRowNum(), 0, 5));

            // blank row
            currentRow++;
            sheet.createRow(currentRow - 1);

            // === HEADER ROW (STYLED LIKE FIRST FORMAT) ===
            Row header = sheet.createRow(currentRow++);
            header.setHeightInPoints(25);

            String[] titles = {
                "Scenario", "Timestamp", "Expected Result",
                "Actual Result", "Status", "Error Message"
            };

            for (int i = 0; i < titles.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(titles[i]);
                cell.setCellStyle(headerStyle);
            }

            // Auto-size columns
            for (int i = 0; i < titles.length; i++) {
                sheet.autoSizeColumn(i);
            }

            save();
            System.out.println("📑 ExcelLogger: Section set -> " + sectionTitle);
        } catch (Exception e) {
            System.out.println("❌ ExcelLogger.setSection error: " + e.getMessage());
        }
    }

    /**
     * Main log method for writing a row.
     */
    public static synchronized void log(String scenario, String expected, String actual, String status, String errorMessage) {
        try {
            if (workbook == null || sheet == null) {
                initReport(reportPath);
            }

            Row row = sheet.createRow(currentRow++);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            Cell c0 = row.createCell(0);
            c0.setCellValue(safeString(scenario));
            c0.setCellStyle(normalStyle);

            Cell c1 = row.createCell(1);
            c1.setCellValue(timestamp);
            c1.setCellStyle(normalStyle);

            Cell c2 = row.createCell(2);
            c2.setCellValue(safeString(expected));
            c2.setCellStyle(normalStyle);

            Cell c3 = row.createCell(3);
            c3.setCellValue(safeString(actual));
            c3.setCellStyle(normalStyle);

            Cell c4 = row.createCell(4);
            String s = safeString(status).toUpperCase();
            c4.setCellValue(s);

            if ("PASS".equalsIgnoreCase(s)) {
                c4.setCellStyle(passStyle);
            } else if ("FAIL".equalsIgnoreCase(s)) {
                c4.setCellStyle(failStyle);
            } else {
                c4.setCellStyle(normalStyle);
            }

            Cell c5 = row.createCell(5);
            c5.setCellValue(safeString(errorMessage));
            c5.setCellStyle(normalStyle);

            for (int i = 0; i <= 5; i++) sheet.autoSizeColumn(i);

            save();
            System.out.println("✅ ExcelLogger: Logged [" + scenario + "] → " + status);
        } catch (Exception e) {
            System.out.println("❌ ExcelLogger.log error: " + e.getMessage());
        }
    }

    // Wrapper methods
    public static void logPass(String scenario, String expected, String actual) {
        log(scenario, expected, actual, "PASS", "");
    }

    public static void logFail(String scenario, String expected, String actual, String errorMessage) {
        log(scenario, expected, actual, "FAIL", errorMessage);
    }

    public static void logInfo(String scenario, String expected, String actual) {
        log(scenario, expected, actual, "INFO", "");
    }

    /**
     * Save workbook to disk.
     */
    private static synchronized void save() {
        if (workbook == null) return;
        String pathToSave = (reportPath == null || reportPath.trim().isEmpty()) ? DEFAULT_FILE_NAME : reportPath;
        
        try {
            File file = new File(pathToSave);
            
            // Ensure parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
                fos.flush();
            }
        } catch (Exception e) {
            System.out.println("❌ ExcelLogger.save error: " + e.getMessage());
        }
    }

    /**
     * Close and reset workbook.
     */
    public static synchronized void closeReport() {
        if (workbook == null) return;
        try {
            save();
            workbook.close();
            workbook = null;
            sheet = null;
            currentRow = 0;
            System.out.println("📊 ExcelLogger: Report saved and closed (" + (reportPath == null ? DEFAULT_FILE_NAME : reportPath) + ")");
        } catch (Exception e) {
            System.out.println("❌ ExcelLogger.closeReport error: " + e.getMessage());
        }
    }

    private static String safeString(String s) {
        return s == null ? "" : s;
    }
}
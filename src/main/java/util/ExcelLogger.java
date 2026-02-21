package util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import util.TestContext;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelLogger {

    private static final String DEFAULT_FILE = "ForwardAgencyLogs.xlsx";

    // Holds workbook + sheet + row index per file
    private static final Map<String, WorkbookContext> CONTEXT_MAP = new HashMap<>();

    // ================= PUBLIC API =================

    // Existing calls → unchanged
    public static synchronized void log(
            String scenario,
            String expected,
            String actual,
            String status,
            String errorMessage
    ) {
        // 🚫 Skip framework logging for Active Account Summary runs
    	if (TestContext.isActiveSummaryRun || TestContext.isCashBalancingRun) {
    	    return;
    	}


        logToFile(DEFAULT_FILE, scenario, expected, actual, status, errorMessage);
    }

    public static void logIfAllowed(
            String action,
            String expected,
            String actual,
            String status,
            String remarks
    ) {
    	if (TestContext.isActiveSummaryRun || TestContext.isCashBalancingRun) {
    	    return;
    	}

        log(action, expected, actual, status, remarks);
    }

    // Flexible logger
    public static synchronized void logToFile(
            String fileName,
            String scenario,
            String expected,
            String actual,
            String status,
            String errorMessage
    ) {
        try {
            WorkbookContext ctx = CONTEXT_MAP.computeIfAbsent(
                    fileName, ExcelLogger::createNewContext
            );

            writeRow(ctx, scenario, expected, actual, status, errorMessage);
            save(ctx);

            System.out.println("✅ Logged to [" + fileName + "] → " + status);

        } catch (Exception e) {
            System.out.println("❌ Failed to log in Excel (" + fileName + "): " + e.getMessage());
        }
    }

    // ================= INTERNAL =================

    private static WorkbookContext createNewContext(String fileName) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Execution Logs");

            sheet.setDefaultRowHeightInPoints(22);
            createHeader(workbook, sheet);

            WorkbookContext ctx = new WorkbookContext();
            ctx.fileName = fileName;
            ctx.workbook = workbook;
            ctx.sheet = sheet;
            ctx.currentRow = 1;

            save(ctx);
            System.out.println("✅ New Excel log file created: " + fileName);

            return ctx;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Excel file: " + fileName, e);
        }
    }

    private static void createHeader(Workbook workbook, Sheet sheet) {

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);

        Row header = sheet.createRow(0);
        header.setHeightInPoints(26);

        String[] titles = {
                "Scenario",
                "Timestamp",
                "Expected Result",
                "Actual Result",
                "Status",
                "Error Message"
        };

        for (int i = 0; i < titles.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(headerStyle);
        }

        autoSizeAllColumns(sheet, titles.length);
    }

    private static void writeRow(
            WorkbookContext ctx,
            String scenario,
            String expected,
            String actual,
            String status,
            String errorMessage
    ) {

        Workbook workbook = ctx.workbook;
        Sheet sheet = ctx.sheet;

        Row row = sheet.createRow(ctx.currentRow++);
        row.setHeightInPoints(22);

        String timestamp =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // ===== Common Styles =====
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setWrapText(true);
        normalStyle.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        boldStyle.setWrapText(true);
        boldStyle.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle greenStyle = workbook.createCellStyle();
        greenStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font greenFont = workbook.createFont();
        greenFont.setBold(true);
        greenStyle.setFont(greenFont);
        greenStyle.setAlignment(HorizontalAlignment.CENTER);
        greenStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        greenStyle.setWrapText(true);

        CellStyle redStyle = workbook.createCellStyle();
        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font redFont = workbook.createFont();
        redFont.setBold(true);
        redStyle.setFont(redFont);
        redStyle.setAlignment(HorizontalAlignment.CENTER);
        redStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        redStyle.setWrapText(true);

        // ===== Cells =====
        row.createCell(0).setCellValue(scenario);
        row.getCell(0).setCellStyle(boldStyle);

        row.createCell(1).setCellValue(timestamp);
        row.getCell(1).setCellStyle(normalStyle);

        row.createCell(2).setCellValue(expected);
        row.getCell(2).setCellStyle(normalStyle);

        row.createCell(3).setCellValue(actual);
        row.getCell(3).setCellStyle(normalStyle);

        Cell statusCell = row.createCell(4);
        statusCell.setCellValue(status);
        statusCell.setCellStyle(
                "PASS".equalsIgnoreCase(status) ? greenStyle :
                "FAIL".equalsIgnoreCase(status) ? redStyle :
                normalStyle
        );

        row.createCell(5).setCellValue(errorMessage == null ? "" : errorMessage);
        row.getCell(5).setCellStyle(normalStyle);

        // ✅ Resize columns after writing data
        autoSizeAllColumns(sheet, 6);
    }

    private static void autoSizeAllColumns(Sheet sheet, int totalColumns) {
        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);

            // Add spacing padding
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 1500, 255 * 256));
        }
    }

    private static void save(WorkbookContext ctx) {
        try (FileOutputStream fos = new FileOutputStream(ctx.fileName)) {
            ctx.workbook.write(fos);
        } catch (Exception e) {
            System.out.println("❌ Failed to save Excel file: " + ctx.fileName);
        }
    }

    // ================= HELPER =================
    private static class WorkbookContext {
        String fileName;
        Workbook workbook;
        Sheet sheet;
        int currentRow;
    }
}

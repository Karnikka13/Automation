package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;

public class ExcelUtils {
	public static void formatHeaderRow(String filePath, String sheetName) {
		
		  try {
		        File file = new File(filePath);
		        Workbook workbook;
		        Sheet sheet;

		        // 1️⃣ Create workbook if file does not exist
		        if (file.exists()) {
		            try (FileInputStream fis = new FileInputStream(file)) {
		                workbook = WorkbookFactory.create(fis);
		            }
		        } else {
		            workbook = new XSSFWorkbook();
		        }

		        // 2️⃣ Create sheet if missing
		        sheet = workbook.getSheet(sheetName);
		        if (sheet == null) {
		            sheet = workbook.createSheet(sheetName);
		        }

		        // 3️⃣ Create header row if missing
		        Row header = sheet.getRow(0);
		        if (header == null) {
		            header = sheet.createRow(0);

		            String[] headers = {
		                    "Scenario",
		                    "Timestamp",
		                    "Expected Result",
		                    "Actual Result",
		                    "Status",
		                    "Error Message"
		            };

		            for (int i = 0; i < headers.length; i++) {
		                header.createCell(i).setCellValue(headers[i]);
		            }
		        }

		        // 4️⃣ Header styling
		        CellStyle headerStyle = workbook.createCellStyle();
		        Font headerFont = workbook.createFont();

		        headerFont.setBold(true);
		        headerFont.setColor(IndexedColors.WHITE.getIndex());

		        headerStyle.setFont(headerFont);
		        headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
		        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		        headerStyle.setAlignment(HorizontalAlignment.CENTER);

		        for (int i = 0; i < header.getLastCellNum(); i++) {
		            Cell cell = header.getCell(i);
		            if (cell == null) cell = header.createCell(i);
		            cell.setCellStyle(headerStyle);
		            sheet.autoSizeColumn(i);
		        }

		        // 5️⃣ Save safely
		        try (FileOutputStream fos = new FileOutputStream(filePath)) {
		            workbook.write(fos);
		        }

		        workbook.close();

		        System.out.println("📘 Output Excel ready → Sheet: " + sheetName);

		    } catch (Exception e) {
		        throw new RuntimeException("Excel header format failed → " + e.getMessage());
		    }
		
	}

    /* ==========================================================
     * READ: Returns list of rows (Map<Column,Value>)
     * ========================================================== */
    public static List<Map<String, String>> getData(String filePath, String sheetName) {

        List<Map<String, String>> rowsList = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("Sheet not found: " + sheetName);

            Row header = sheet.getRow(0);
            if (header == null) throw new RuntimeException("Header missing!");

            int cols = header.getLastCellNum();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> data = new LinkedHashMap<>();

                for (int c = 0; c < cols; c++) {
                    String key = formatter.formatCellValue(header.getCell(c));
                    String value = formatter.formatCellValue(row.getCell(c));
                    data.put(key, value);
                }

                rowsList.add(data);
            }

        } catch (Exception e) {
            throw new RuntimeException("Excel read failed → " + e.getMessage());
        }

        return rowsList;
    }

    /* ==========================================================
     * WRITE: Writes a value into a specific cell WITH STYLING
     * ========================================================== */
    public static synchronized void writeCell(String filePath, String sheetName,
                                              int rowIndex, String columnName, String value) {

        Workbook workbook = null;
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(filePath);
            workbook = new XSSFWorkbook(fis);
            fis.close();

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) throw new RuntimeException("Sheet not found: " + sheetName);

            Row header = sheet.getRow(0);
            if (header == null) throw new RuntimeException("Header row missing!");

            /* ======================================================
             * FIND COLUMN INDEX
             * ====================================================== */
            int colIndex = -1;
            for (int i = 0; i < header.getLastCellNum(); i++) {
                if (header.getCell(i).getStringCellValue().trim()
                        .equalsIgnoreCase(columnName.trim())) {
                    colIndex = i;
                    break;
                }
            }
            if (colIndex == -1)
                throw new RuntimeException("Column not found: " + columnName);

            /* ======================================================
             * PREPARE ROW + CELL
             * ====================================================== */
            Row row = sheet.getRow(rowIndex);
            if (row == null) row = sheet.createRow(rowIndex);

            Cell cell = row.getCell(colIndex);
            if (cell == null) cell = row.createCell(colIndex);

            cell.setCellValue(value);

            /* ======================================================
             * STYLING ZONE
             * ====================================================== */
            // Scenario text → black bold
            if (columnName.equalsIgnoreCase("Scenario")) {
                Font boldBlack = workbook.createFont();
                boldBlack.setBold(true);
                boldBlack.setColor(IndexedColors.BLACK.getIndex());

                CellStyle style = workbook.createCellStyle();
                style.setFont(boldBlack);
                cell.setCellStyle(style);
            }

            // Status → PASS / FAIL background color
            if (columnName.equalsIgnoreCase("Status")) {

                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                font.setColor(IndexedColors.WHITE.getIndex());

                if (value.equalsIgnoreCase("PASS")) {
                    style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                } else if (value.equalsIgnoreCase("FAIL")) {
                    style.setFillForegroundColor(IndexedColors.RED.getIndex());
                }

                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            // Error Message → Blue text
            if (columnName.equalsIgnoreCase("Error Message")) {

                Font blueFont = workbook.createFont();
                blueFont.setBold(false);
                blueFont.setColor(IndexedColors.BLUE.getIndex());

                CellStyle style = workbook.createCellStyle();
                style.setFont(blueFont);
                cell.setCellStyle(style);
            }

            if (rowIndex == 0) {
                for (int i = 0; i < header.getLastCellNum(); i++) {
                    Cell hCell = header.getCell(i);

                    CellStyle hStyle = workbook.createCellStyle();
                    Font hFont = workbook.createFont();

                    hFont.setBold(true);
                    hFont.setColor(IndexedColors.WHITE.getIndex());

                    hStyle.setFont(hFont);
                    hStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
                    hStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    hStyle.setAlignment(HorizontalAlignment.CENTER);

                    hCell.setCellStyle(hStyle);
                }
            }


            // Auto-size all columns
            for (int i = 0; i < header.getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }

            /* ======================================================
             * SAVE FILE
             * ====================================================== */
            FileOutputStream fos = new FileOutputStream(filePath);
            workbook.write(fos);
            fos.close();

            System.out.println("Excel Updated → (" + columnName + ") Row " + rowIndex);

        } catch (Exception e) {
            throw new RuntimeException("Excel write failed → " + e.getMessage());
        } finally {
            try { if (workbook != null) workbook.close(); } catch (Exception ignored) {}
            try { if (fis != null) fis.close(); } catch (Exception ignored) {}
        }
    }
}

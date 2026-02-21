package utils;

import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;

public class ExcelConfigReader {

    /**
     * Reads a single config value from an Excel sheet.
     *
     * @param filePath  Path to Excel file
     * @param sheetName Name of the sheet (example: "Config")
     * @param key       The key to search for (example: "appUrl")
     * @return The corresponding value from Excel
     */
    public static String readConfig(String filePath, String sheetName, String key) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = WorkbookFactory.create(fis)) {

            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("Sheet not found: " + sheetName);
            }

            for (Row row : sheet) {
                Cell keyCell = row.getCell(0);
                Cell valueCell = row.getCell(1);

                if (keyCell == null || valueCell == null) continue;

                String cellKey = keyCell.getStringCellValue().trim();

                if (cellKey.equalsIgnoreCase(key)) {
                    return valueCell.getStringCellValue().trim();
                }
            }

            throw new RuntimeException("Key not found in Excel: " + key);

        } catch (Exception e) {
            throw new RuntimeException("Failed to read Excel config: " + e.getMessage(), e);
        }
    }
}

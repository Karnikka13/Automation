package utils;

import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.util.*;

public class ExcelReader {

    // Holds currently selected test case row
    private static Map<String, String> activeRow = null;

    // ❌ DO NOT CHANGE – used by LoginSteps
    public static List<Map<String,String>> readExcelData(String filePath) {
        List<Map<String,String>> rows = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = WorkbookFactory.create(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            rows.addAll(readSheet(sheet));

        } catch (Exception e) {
            throw new RuntimeException("Failed to read Excel: " + filePath, e);
        }
        return rows;
    }

    public static void loadTestCase(String filePath, String testCase) {

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = WorkbookFactory.create(fis)) {

            // 1️⃣ Try sheet with testcase name (014–016)
            Sheet directSheet = wb.getSheet(testCase);
            if (directSheet != null) {
                List<Map<String, String>> rows = readSheet(directSheet);

                activeRow = rows.stream()
                        .filter(r ->
                                testCase.equalsIgnoreCase(
                                        r.getOrDefault("TestCase", "").trim()
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "TestCase not found in sheet: " + testCase
                                ));

                return;
            }

            // 2️⃣ Fallback: search ALL sheets (017–022)
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                List<Map<String, String>> rows = readSheet(sheet);

                Optional<Map<String, String>> match =
                        rows.stream()
                                .filter(r ->
                                        testCase.equalsIgnoreCase(
                                                r.getOrDefault("TestCase", "").trim()
                                        )
                                )
                                .findFirst();

                if (match.isPresent()) {
                    activeRow = match.get();
                    return;
                }
            }

            // 3️⃣ Not found anywhere
            throw new RuntimeException("TestCase not found in any sheet: " + testCase);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load testcase: " + testCase, e);
        }
    }
    public static void loadTestCase1(String filePath, String testCase) {

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook wb = WorkbookFactory.create(fis)) {

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                List<Map<String, String>> rows = readSheet(sheet);

                Optional<Map<String, String>> match =
                        rows.stream()
                            .filter(r ->
                                testCase.equalsIgnoreCase(
                                    r.getOrDefault("TestCase", "").trim()
                                )
                            )
                            .findFirst();

                if (match.isPresent()) {
                    activeRow = match.get();
                    return;
                }
            }

            throw new RuntimeException("TestCase not found in Excel: " + testCase);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load testcase: " + testCase, e);
        }
    }


    private static List<Map<String,String>> readSheet(Sheet sheet) {

        List<Map<String,String>> rows = new ArrayList<>();
        Row header = sheet.getRow(0);

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Map<String,String> map = new HashMap<>();

            for (int c = 0; c < header.getLastCellNum(); c++) {
                String key = header.getCell(c).getStringCellValue().trim();
                Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String val = getCellValue(cell);
                map.put(key, val);

            }
            rows.add(map);
        }
        return rows;
    }
    private static String getCellValue(Cell cell) {

        if (cell == null) return "";

        DataFormatter formatter = new DataFormatter(Locale.US);

        switch (cell.getCellType()) {

            case STRING:
                return cell.getStringCellValue().trim();

            case NUMERIC:
                // ✅ Works for DATE, TIME, NUMBER exactly as Excel shows
                return formatter.formatCellValue(cell).trim();

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            default:
                return "";
        }
    }


    public static String get(String columnName) {
        if (activeRow == null) {
            throw new IllegalStateException(
                "Excel test case not loaded. Call loadTestCase() first."
            );
        }
        return activeRow.getOrDefault(columnName, "").trim();
    }
}

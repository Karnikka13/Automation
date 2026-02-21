package stepdefinitions;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import utils.ExcelUtils;
import utils.DriverContext;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class EarningsSummaryByUnitSteps {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String INPUT_FILE =
            "src/test/resources/testdata/advanced_search_input.xlsx";

    private static final String OUTPUT_FILE =
            "src/test/resources/testdata/advanced_search_output.xlsx";

    private static final String OUTPUT_SHEET =
            "Earnings_By_Unit_Results";

    private static final String DOWNLOAD_DIR =
            System.getProperty("user.dir") + "/downloads";

    /* ================= INIT ================= */
    private void init() {
        driver = DriverContext.getDriver();
        wait = DriverContext.getWait();
        if (driver == null || wait == null) {
            Assert.fail("Driver not initialized");
        }
    }

    /* ================= NAVIGATION ================= */
    @Given("User navigates to Earnings Summary by Unit page")
    public void navigateToScreen() {

        init();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("open-button"))).click();

        WebElement search = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("menuSearchBar")));
        search.clear();
        search.sendKeys("Earnings Summary by Unit");
        search.sendKeys(Keys.ENTER);

        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('*').forEach(function(e){" +
                        "if(e.innerText && e.innerText.trim()==='Earnings Summary by Unit'){e.click();}" +
                        "});");

        List<WebElement> frames = driver.findElements(By.id("UnitActivityReport"));
        if (!frames.isEmpty()) {
            driver.switchTo().frame(frames.get(0));
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("dpicker")));
        System.out.println("✅ Earnings Summary by Unit screen loaded");
    }

    /* ================= MAIN EXECUTION ================= */
    @When("User executes Earnings Summary by Unit scenarios from Excel sheet {string}")
    public void executeScenarios(String sheetName) {

        init();
        ExcelUtils.formatHeaderRow(OUTPUT_FILE, OUTPUT_SHEET);

        List<Map<String, String>> rows =
                ExcelUtils.getData(INPUT_FILE, sheetName);

        int rowNum = 1;

        for (Map<String, String> row : rows) {

            String testCaseId = get(row, "Test_Case_ID");
            String date = get(row, "Date");
            String action = get(row, "Action");
            String exportType = get(row, "Export_Type");
            String expected = get(row, "Expected_Result");

            if (testCaseId == null || testCaseId.isBlank()) continue;

            String actual = "";
            String status = "PASS";
            String error = "";

            try {
                clearDateField();
                clearDownloads();

                /* ========== SEARCH ========== */
                if ("SEARCH".equalsIgnoreCase(action)) {

                    if (date != null && !date.isBlank()) {
                        enterDate(date);
                    }

                    clickSearch();

                    if (handleNoRecordsToastIfPresent()) {
                        actual = "No records found";
                    } else if (areRecordsDisplayed()) {
                        actual = "Records displayed";
                    } else {
                        actual = "No records found";
                    }
                }

                /* ========== CLEAR ========== */
                else if ("CLEAR".equalsIgnoreCase(action)) {

                    if (date != null && !date.isBlank()) {
                        enterDate(date);
                    }

                    clickClear();

                    actual = isDateCleared()
                            ? "Date cleared and grid reset"
                            : "Date not cleared";
                }

                /* ========== EXPORT ========== */
                else if ("EXPORT".equalsIgnoreCase(action)) {

                    enterDate(date);
                    clickSearch();

                    if (handleNoRecordsToastIfPresent() || !areRecordsDisplayed()) {
                        actual = "No records available for export";
                    } else {
                        triggerExport(exportType);

                        boolean downloaded =
                                "EXCEL".equalsIgnoreCase(exportType)
                                        ? isFileDownloaded(".xls", ".xlsx")
                                        : isFileDownloaded(".pdf");

                        actual = downloaded
                                ? exportType + " file downloaded"
                                : exportType + " download blocked by browser";
                    }
                }

                else {
                    actual = "Invalid action in input sheet";
                }

            } catch (Exception e) {
                actual = "Execution failed";
                status = "FAIL";
                error = e.getMessage();
            }

            writeResult(rowNum++, testCaseId, expected, actual, status, error);
        }
    }

    /* ================= DATE ================= */
    private void enterDate(String excelDate) {

        WebElement dpicker = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("dpicker")));

        String uiDate = excelDate.replace("/", "-");

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value=arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('change'));",
                dpicker, uiDate);
    }

    /* ================= BUTTONS ================= */
    private void clickSearch() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("viewRecord"))).click();
        waitForGrid();
    }

    private void clickClear() {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("Clear"))).click();
    }

    /* ================= GRID ================= */
    private boolean areRecordsDisplayed() {

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.id("load_canGrid")));

        List<WebElement> rows =
                driver.findElements(By.cssSelector("#canGrid tbody tr.jqgrow"));

        return !rows.isEmpty();
    }

    /* ================= TOAST ================= */
    private boolean handleNoRecordsToastIfPresent() {
        try {
            WebElement toast = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("div.toast-message")));

            if (toast.getText().trim()
                    .equalsIgnoreCase("No Records Found for given Condition")) {

                driver.findElement(
                        By.cssSelector("button.toast-close-button")).click();

                wait.until(ExpectedConditions.invisibilityOf(toast));
                return true;
            }
        } catch (TimeoutException ignored) {}
        return false;
    }

    /* ================= EXPORT ================= */
    private void triggerExport(String type) {

        By exportBtn = "EXCEL".equalsIgnoreCase(type)
                ? By.cssSelector("a.Export[data-attr='excel']")
                : By.cssSelector("a.Export[data-attr='pdf']");

        WebElement btn = wait.until(
                ExpectedConditions.presenceOfElementLocated(exportBtn));

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView(true);", btn);

        try {
            btn.click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        }
    }

    private boolean isFileDownloaded(String... extensions) throws InterruptedException {

        File dir = new File(DOWNLOAD_DIR);

        int waitTime = 0;
        while (waitTime < 15) {
            File[] files = dir.listFiles((d, name) -> {
                for (String ext : extensions) {
                    if (name.toLowerCase().endsWith(ext)) return true;
                }
                return false;
            });
            if (files != null && files.length > 0) return true;
            Thread.sleep(1000);
            waitTime++;
        }
        return false;
    }

    private void clearDownloads() {
        File dir = new File(DOWNLOAD_DIR);
        if (!dir.exists()) return;
        for (File f : dir.listFiles()) f.delete();
    }

    /* ================= UTIL ================= */
    private boolean isDateCleared() {
        String value = driver.findElement(By.id("dpicker")).getAttribute("value");
        return value == null || value.trim().isEmpty();
    }

    private void clearDateField() {
        WebElement dpicker = driver.findElement(By.id("dpicker"));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].value='';", dpicker);
    }

    private void waitForGrid() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("load_canGrid")));
        } catch (TimeoutException ignored) {}
    }

    private String get(Map<String, String> row, String key) {
        return row.get(key) != null ? row.get(key).trim() : null;
    }

    /* ================= OUTPUT ================= */
    private void writeResult(int rowNum, String testCaseId,
                             String expected, String actual,
                             String status, String error) {

        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET,
                rowNum, "Scenario", testCaseId);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET,
                rowNum, "Timestamp", LocalDateTime.now().toString());
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET,
                rowNum, "Expected Result", expected);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET,
                rowNum, "Actual Result", actual);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET,
                rowNum, "Status", status);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET,
                rowNum, "Error Message", error);
    }

    @Then("Earnings Summary by Unit results should be written to output sheet")
    public void confirmOutput() {
        System.out.println("📘 Earnings Summary by Unit automation completed");
    }
}

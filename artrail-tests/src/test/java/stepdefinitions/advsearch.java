package stepdefinitions;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import utils.ExcelUtils;
import utils.DriverContext;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class advsearch {

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    private static final String INPUT_FILE =
            "src/test/resources/testdata/advanced_search_input.xlsx";
    private static final String OUTPUT_FILE =
            "src/test/resources/testdata/advanced_search_output.xlsx";
    private static final String OUTPUT_SHEET = "advsearch";

    private static final By EXCLUDE_EMPTY_CONSUMER_CHK = By.id("chkExcludeConsumer");
    private static final By RESULT_GRID_ROWS =
            By.cssSelector("#filterData tbody tr.jqgrow");
    private static final By TOAST_MESSAGE =
            By.cssSelector("div.toast div.toast-message");

    /* ================= INIT ================= */

    private void init() {
        driver = DriverContext.getDriver();
        wait = DriverContext.getWait();
        js = (JavascriptExecutor) driver;

        if (driver == null || wait == null) {
            Assert.fail("Driver not initialized");
        }
    }

    /* ================= NAVIGATION ================= */

    @Given("User navigates to the Advanced Search page")
    public void navigateToAdvancedSearch() {
        init();
        driver.findElement(By.id("advSearch")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("adv-scroll")));
    }

    /* ================= FIELD LOCATORS ================= */

    private Map<String, By> fieldLocatorMap() {
        Map<String, By> map = new HashMap<>();

        map.put("Client's Consumer ID", By.id("txtClientDebtNum"));
        map.put("Last Name", By.id("txtConsumerLastName"));
        map.put("First Name", By.id("txtConsumerFirstName"));
        map.put("DOB", By.id("txtConsmerBDate"));
        map.put("SSN", By.id("txtConsumerSSN"));
        map.put("Phone Number", By.id("txtPhNumber"));
        map.put("Email", By.id("txtEmail"));
        map.put("Address", By.id("txtAddress"));
        map.put("Account Number", By.id("txtAccountNum"));
        map.put("Invoice ID", By.id("txtCltAccId"));
        map.put("Case No", By.id("txCaseNo"));
        map.put("Suit ID", By.id("txtSuitId"));
        map.put("Credit Bureau Account", By.id("txtCbrAccountNum"));
        map.put("Patient Last Name", By.id("txtPatientLastName"));
        map.put("Patient First Name", By.id("txtPatientFirstName"));

        return map;
    }

    /* ================= ACTIONS ================= */

    private void clickSearch() {
        js.executeScript("arguments[0].click();",
                driver.findElement(By.id("mainSearch")));
        waitForBackend();
    }

    private void clickClear() {
        js.executeScript("arguments[0].click();",
                driver.findElement(By.id("mainClear")));
        waitForBackend();
    }

    private void handleExcludeEmptyConsumerCheckbox(String action) {
        WebElement chk = driver.findElement(EXCLUDE_EMPTY_CONSUMER_CHK);
        if ("check".equalsIgnoreCase(action) && !chk.isSelected()) chk.click();
        if ("uncheck".equalsIgnoreCase(action) && chk.isSelected()) chk.click();
    }

    /* ================= STABLE WAIT ================= */

    private void waitForBackend() {
        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
    }

    /* ================= VALIDATIONS ================= */

    private int getResultRowCount() {
        return driver.findElements(RESULT_GRID_ROWS).size();
    }

    private String getToastText() {
        try {
            WebElement toast = new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(ExpectedConditions.visibilityOfElementLocated(TOAST_MESSAGE));
            return toast.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean areFieldsCleared() {
        for (WebElement e : driver.findElements(By.cssSelector("input.searchAll"))) {
            if (!e.getAttribute("value").isEmpty()) return false;
        }
        return true;
    }

    /* ================= MAIN EXECUTION ================= */

    @When("User executes Advanced Search scenarios from Excel sheet {string}")
    public void executeFromExcel(String sheetName) {

        init();
        List<Map<String, String>> rows = ExcelUtils.getData(INPUT_FILE, sheetName);
        ExcelUtils.formatHeaderRow(OUTPUT_FILE, OUTPUT_SHEET);

        Map<String, List<Map<String, String>>> scenarios = new LinkedHashMap<>();
        for (Map<String, String> r : rows)
            scenarios.computeIfAbsent(r.get("Scenario ID"), k -> new ArrayList<>()).add(r);

        int rowNum = 1;

        for (String scenarioId : scenarios.keySet()) {

            String expected = "", actual = "", status = "PASS", error = "";
            int rowCount = 0;
            boolean searchClicked = false;

            try {
                /* 🔹 Always start clean */
                clickClear();

                for (Map<String, String> step : scenarios.get(scenarioId)) {
                    String field = step.get("Field Name");
                    String value = step.get("Input Value");
                    expected = step.get("Expected Behavior");

                    if ("Search".equalsIgnoreCase(field)) {
                        clickSearch();
                        searchClicked = true;
                    }
                    else if ("Clear".equalsIgnoreCase(field)) {
                        clickClear();
                    }
                    else if ("Exclude Empty Consumers".equalsIgnoreCase(field)) {
                        handleExcludeEmptyConsumerCheckbox(value);
                    }
                    else {
                        By loc = fieldLocatorMap().get(field);
                        if (loc != null) {
                            WebElement el = driver.findElement(loc);
                            el.clear();
                            el.sendKeys(value);
                        }
                    }
                }

                if (!searchClicked && !expected.toLowerCase().contains("cleared")) {
                    clickSearch();
                }

                rowCount = getResultRowCount();
                String toast = getToastText();

                /* ===== FINAL VALIDATION ===== */

                if (expected.toLowerCase().contains("toast")) {
                    if (toast.equals("Please Enter any one value")
                            || toast.equals("Error Searching Consumer")) {
                        status = "PASS";
                        actual = "Toast = " + toast;
                    } else {
                        status = "FAIL";
                        actual = "Unexpected toast = " + toast;
                    }
                }
                else if (expected.toLowerCase().contains("cleared")) {
                    status = areFieldsCleared() ? "PASS" : "FAIL";
                    actual = "Fields cleared | Rows=" + rowCount;
                }
                else if (expected.toLowerCase().contains("no records")) {
                    status = rowCount == 0 ? "PASS" : "FAIL";
                    actual = "Rows displayed = " + rowCount;
                }
                else if (expected.toLowerCase().contains("may or may not")) {
                    status = "PASS";
                    actual = "Search executed | Rows displayed = " + rowCount;
                }
                else {
                    status = "PASS";
                    actual = "Search executed | Rows displayed = " + rowCount;
                }

                /* 🔹 Always cleanup */
                clickClear();
                if (!areFieldsCleared()) {
                    status = "FAIL";
                    actual += " | Fields not cleared after scenario";
                }

            } catch (Exception e) {
                status = "FAIL";
                actual = "Execution failed";
                error = e.getMessage();
            }

            ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Scenario", scenarioId);
            ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Timestamp",
                    LocalDateTime.now().toString());
            ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Expected Result", expected);
            ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Actual Result", actual);
            ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Status", status);
            ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Error Message", error);
            rowNum++;
        }
    }

    @Then("Advanced Search results should be written to output sheet")
    public void done() {
        System.out.println("✅ Advanced Search execution completed");
    }
}

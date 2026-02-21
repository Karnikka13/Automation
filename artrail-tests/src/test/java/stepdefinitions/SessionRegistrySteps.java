package stepdefinitions;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import utils.ExcelUtils;
import utils.DriverContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SessionRegistrySteps {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String INPUT_FILE =
    		"src/test/resources/testdata/advanced_search_input.xlsx";

    private static final String OUTPUT_FILE =
    		"src/test/resources/testdata/advanced_search_output.xlsx";

    private static final String OUTPUT_SHEET =
            "Session_Registry_Results";

    /* ================= INIT ================= */
    private void init() {
        driver = DriverContext.getDriver();
        wait = DriverContext.getWait();
        if (driver == null || wait == null) {
            Assert.fail("Driver not initialized");
        }
    }

    /* ================= NAVIGATION ================= */
    @Given("User navigates to Session Registry page")
    public void navigateToSessionRegistry() {

        init();

        // Open menu
        wait.until(ExpectedConditions.elementToBeClickable(By.id("open-button"))).click();

        // Search menu
        WebElement search = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("menuSearchBar")));
        search.clear();
        search.sendKeys("Session Registry");
        search.sendKeys(Keys.ENTER);

        // Click menu entry (JS-safe)
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('*').forEach(function(e){" +
                        "if(e.innerText && e.innerText.trim()==='Session Registry'){e.click();}" +
                        "});");

        /* ===== IFRAME SWITCH (CRITICAL FIX) ===== */
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("iframe")));
        List<WebElement> frames = driver.findElements(By.tagName("iframe"));
        if (!frames.isEmpty()) {
            driver.switchTo().frame(frames.get(0));
        }

        /* ===== WAIT FOR jqGrid CONTAINER ===== */
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("gbox_sessionTable")));

        System.out.println("✅ Session Registry screen loaded");
    }

    /* ================= MAIN EXECUTION ================= */
    @When("User executes Session Registry scenarios from Excel sheet {string}")
    public void executeSessionRegistryScenarios(String sheetName) {

        init();
        ExcelUtils.formatHeaderRow(OUTPUT_FILE, OUTPUT_SHEET);

        List<Map<String, String>> rows =
                ExcelUtils.getData(INPUT_FILE, sheetName);

        int rowNum = 1;

        for (Map<String, String> row : rows) {

            String testCaseId = get(row, "Test_Case_ID");
            String action = get(row, "Action");
            String expected = get(row, "Expected_Result");

            if (testCaseId == null || testCaseId.isBlank()) continue;

            String actual = "";
            String status = "PASS";
            String error = "";

            try {

                switch (action.toUpperCase()) {

                    case "NAVIGATE":
                        actual = isGridVisible()
                                ? "Session Registry screen displayed"
                                : "Screen not loaded";
                        break;

                    case "GRID_LOAD":
                        actual = waitForGrid()
                                ? "Session Registry grid loaded"
                                : "Grid not loaded";
                        break;

                    case "VERIFY_COLUMNS":
                        actual = areColumnsVisible()
                                ? "All expected columns displayed"
                                : "Missing columns";
                        break;

                    case "VERIFY_GROUPING":
                        actual = isGroupingDisplayed()
                                ? "Sessions grouped by user"
                                : "Grouping not displayed";
                        break;

                    case "EXPAND_GROUP":
                        expandGroup();
                        actual = "Session group expanded";
                        break;

                    case "VERIFY_SESSION_ROW":
                        actual = areRowsVisible()
                                ? "Active session rows displayed"
                                : "No active session rows";
                        break;

                    case "VERIFY_CLOSE_ICON":
                        actual = isCloseIconVisible()
                                ? "Close icon visible"
                                : "Close icon missing";
                        break;

                    case "CLOSE_SESSION":
                        closeSession();
                        actual = "Session closed successfully";
                        break;

                    case "VERIFY_SESSION_REMOVAL":
                        actual = areRowsVisible()
                                ? "Session still present"
                                : "Session removed from grid";
                        break;

                    case "VERIFY_EMPTY_STATE":
                        actual = areRowsVisible()
                                ? "Active sessions still present"
                                : "No active sessions available";
                        break;

                    default:
                        actual = "Invalid action";
                }

            } catch (Exception e) {
                actual = "Execution failed";
                status = "FAIL";
                error = e.getMessage();
            }

            writeResult(rowNum++, testCaseId, expected, actual, status, error);
        }
    }

    /* ================= GRID & ACTION HELPERS ================= */

    private boolean isGridVisible() {
        return driver.findElement(By.id("gbox_sessionTable")).isDisplayed();
    }

    private boolean waitForGrid() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("load_sessionTable")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private boolean areColumnsVisible() {
        return driver.findElement(By.id("jqgh_sessionTable_username")).isDisplayed()
                && driver.findElement(By.id("jqgh_sessionTable_uslloginTimeFormatted")).isDisplayed()
                && driver.findElement(By.id("jqgh_sessionTable_browserDetails")).isDisplayed()
                && driver.findElement(By.id("jqgh_sessionTable_lastRequest")).isDisplayed();
    }

    private boolean isGroupingDisplayed() {
        return !driver.findElements(
                By.xpath("//tr[contains(@class,'jqgroup')]")).isEmpty();
    }

    private void expandGroup() {
        WebElement expand =
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//span[contains(@class,'ui-icon-circlesmall-plus')]")));
        expand.click();
    }

    private boolean areRowsVisible() {
        return !driver.findElements(
                By.xpath("//table[@id='sessionTable']//tr[contains(@class,'jqgrow')]")).isEmpty();
    }

    private boolean isCloseIconVisible() {
        return !driver.findElements(
                By.xpath("//table[@id='sessionTable']//em[contains(@class,'fa-close')]")).isEmpty();
    }

    private void closeSession() {
        WebElement close =
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//table[@id='sessionTable']//em[contains(@class,'fa-close')]")));
        close.click();

        try {
            driver.switchTo().alert().accept();
        } catch (NoAlertPresentException ignored) {}
    }

    /* ================= UTIL ================= */

    private String get(Map<String, String> row, String key) {
        return row.get(key) != null ? row.get(key).trim() : null;
    }

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

    @Then("Session Registry results should be written to output sheet")
    public void confirmOutput() {
        System.out.println("📘 Session Registry automation completed");
    }
}

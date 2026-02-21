package stepdefinitions;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import utils.ExcelUtils;
import utils.DriverContext;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DisputeAnalyticsSteps {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String INPUT_FILE =
            "src/test/resources/testdata/advanced_search_input.xlsx";
    private static final String OUTPUT_FILE =
            "src/test/resources/testdata/advanced_search_output.xlsx";
    private static final String OUTPUT_SHEET =
            "Dispute_Analytics_Results";

    private final By TILE = By.id("DTCount");

    private void init() {
        driver = DriverContext.getDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /* ================= NAVIGATION ================= */

    @Given("User navigates to Dispute Analytics page")
    public void navigateToDisputeAnalytics() {

        init();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("open-button"))).click();

        WebElement menuSearch =
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("menuSearchBar")));
        menuSearch.clear();
        menuSearch.sendKeys("Dispute Analytics");
        menuSearch.sendKeys(Keys.ENTER);

        WebElement menu =
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//span[normalize-space()='Dispute Analytics']")));

        jsClick(menu);
        switchToFrame();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Custom")));
    }

    /* ================= MAIN EXECUTION ================= */

    @When("User executes Dispute Analytics scenarios from Excel sheet {string}")
    public void executeScenarios(String sheetName) {

        ExcelUtils.formatHeaderRow(OUTPUT_FILE, OUTPUT_SHEET);
        List<Map<String, String>> rows = ExcelUtils.getData(INPUT_FILE, sheetName);

        int rowNum = 1;

        for (Map<String, String> row : rows) {

            String tc = row.get("Test_Case_ID");
            if (tc == null || tc.trim().isEmpty()) continue;

            String action = row.get("Action");
            String tab = row.get("Tab_Name");
            String expected = row.get("Expected_Result");

            String actual = "Execution failed";
            String status = "PASS";
            String error = "";

            try {

                if ("LOAD".equalsIgnoreCase(action)) {
                    actual = "Dispute Analytics screen loaded";
                }

                else if ("CLICK_TAB".equalsIgnoreCase(action)) {
                    clickTab(tab);
                    actual = tab + " tab opened";
                }

                else if ("APPLY_FILTER".equalsIgnoreCase(action)) {
                    clickTab(tab);
                    applyFilter(row, tab);
                    actual = "Filter applied successfully";
                }

                waitForTiles();

                String dd = getTile("DTCount");
                String d  = getTile("SDCount");
                String c  = getTile("CTCount");
                String cm = getTile("CMTCount");

                actual += " | DD:" + dd + " | D:" + d + " | C:" + c + " | CM:" + cm;

                validateTiles(row);

            } catch (Exception e) {
                status = "FAIL";
                error = e.getMessage();
            }

            if (!actual.contains(expected))
                status = "FAIL";

            write(rowNum++, tc, expected, actual, status, error);
        }
    }

    /* ================= TAB ================= */

    private void clickTab(String tab) {

        By locator;

        if ("CUSTOM".equalsIgnoreCase(tab)) locator = By.linkText("Custom");
        else if ("DAILY".equalsIgnoreCase(tab)) locator = By.linkText("Daily");
        else if ("WEEK".equalsIgnoreCase(tab)) locator = By.linkText("Week");
        else if ("MONTH".equalsIgnoreCase(tab)) locator = By.linkText("Month");
        else if ("QTR".equalsIgnoreCase(tab)) locator = By.linkText("QTR");
        else if ("YEAR".equalsIgnoreCase(tab)) locator = By.linkText("Year");
        else if ("OVERALL".equalsIgnoreCase(tab)) locator = By.id("overallTab");
        else throw new RuntimeException("Invalid tab: " + tab);

        jsClick(locator);
        waitForTiles();
    }

    /* ================= FILTER ================= */

    private void applyFilter(Map<String, String> row, String tab) {

        if ("CUSTOM".equalsIgnoreCase(tab)) {

            openCustomPicker();

            String range = row.get("Custom_Range");

            // 🔹 PREDEFINED RANGE → AUTO APPLY
            if (range != null && !"CUSTOM".equalsIgnoreCase(range)) {
                selectCustomRange(range);
                // NO apply button here (UI auto-applies)
            }

            // 🔹 MANUAL RANGE → APPLY BUTTON REQUIRED
            else {
                setCustomDates(row.get("From_Date"), row.get("To_Date"));
                applyCustom();
            }
        }

        else if ("DAILY".equalsIgnoreCase(tab)) {
            setInputValue("dailyDate", row.get("Daily_Date"));
            jsClick(By.id("dailySubmit"));
        }

        else if ("WEEK".equalsIgnoreCase(tab)) {
            setInputValue("WeekMonth", row.get("Week_Month"));
            selectWeekSafely("ddlWeek", row.get("Week_Range"));
            jsClick(By.id("WeekSubmit"));
        }

        else if ("MONTH".equalsIgnoreCase(tab)) {
            setInputValue("MonthDate", row.get("Month_Year"));
            jsClick(By.id("MonthSubmit"));
        }

        else if ("QTR".equalsIgnoreCase(tab)) {
            setInputValue("txtYear", row.get("Year"));
            selectQuarterSafely("ddlQtr", mapQuarter(row.get("Quarter")));
            jsClick(By.id("QtrSubmit"));
        }

        else if ("YEAR".equalsIgnoreCase(tab)) {
            setInputValue("txtYearPicker", row.get("Year"));
            jsClick(By.id("YearSubmit"));
        }
    }

    /* ================= CUSTOM ================= */

    private void openCustomPicker() {
        jsClick(By.id("reportrange"));
    }

    private void selectCustomRange(String range) {
        jsClick(By.xpath("//div[contains(@class,'ranges')]//li[normalize-space()='"
                + range + "']"));
    }

    private void setCustomDates(String from, String to) {

        if (from == null || to == null) return;

        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement fromEl = driver.findElement(By.name("daterangepicker_start"));
        WebElement toEl = driver.findElement(By.name("daterangepicker_end"));

        js.executeScript("arguments[0].removeAttribute('readonly');", fromEl);
        js.executeScript("arguments[0].removeAttribute('readonly');", toEl);

        js.executeScript("arguments[0].value=arguments[1];", fromEl, from);
        js.executeScript("arguments[0].value=arguments[1];", toEl, to);
    }

    private void applyCustom() {
        jsClick(By.cssSelector(".applyBtn"));
    }

    /* ================= SAFE DROPDOWNS ================= */

    private void selectWeekSafely(String id, String expectedText) {

        if (expectedText == null || expectedText.equalsIgnoreCase("NA")) return;

        WebElement ddl = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
        Select select = new Select(ddl);

        for (WebElement opt : select.getOptions()) {
            if (opt.getText().contains(expectedText.substring(0, 5))) {
                opt.click();
                return;
            }
        }
        throw new NoSuchElementException("Week option not found: " + expectedText);
    }

    private void selectQuarterSafely(String id, String value) {

        WebElement ddl =
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView(true);", ddl);

        new Select(ddl).selectByVisibleText(value);
    }

    /* ================= TILE ================= */

    private void waitForTiles() {
        wait.until(ExpectedConditions.presenceOfElementLocated(TILE));
    }

    private String getTile(String id) {
        try {
            return driver.findElement(By.id(id)).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /* ================= VALIDATION ================= */

    private void validateTiles(Map<String, String> row) {
        validateTile("DTCount", row.get("Expected_Direct_Dispute"));
        validateTile("SDCount", row.get("Expected_Dispute"));
        validateTile("CTCount", row.get("Expected_Complaint"));
        validateTile("CMTCount", row.get("Expected_Compliment"));
    }

    private void validateTile(String id, String expected) {
        if (expected == null || expected.equalsIgnoreCase("NA")) return;
        if (!getTile(id).equals(expected))
            throw new AssertionError(id + " mismatch");
    }

    /* ================= HELPERS ================= */

    private void setInputValue(String id, String value) {
        if (value == null || value.equalsIgnoreCase("NA")) return;

        WebElement el = driver.findElement(By.id(id));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');arguments[0].value=arguments[1];",
                el, value);
    }

    private String mapQuarter(String q) {
        if ("Q1".equalsIgnoreCase(q)) return "January to March";
        if ("Q2".equalsIgnoreCase(q)) return "April to June";
        if ("Q3".equalsIgnoreCase(q)) return "July to September";
        if ("Q4".equalsIgnoreCase(q)) return "October to December";
        return q;
    }

    private void jsClick(By locator) {
        WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    private void jsClick(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    private void switchToFrame() {
        driver.switchTo().defaultContent();
        for (WebElement f : driver.findElements(By.tagName("iframe"))) {
            driver.switchTo().frame(f);
            if (!driver.findElements(TILE).isEmpty()) return;
            driver.switchTo().defaultContent();
        }
    }

    private void write(int r, String tc, String exp, String act, String st, String err) {
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Scenario", tc);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Timestamp", LocalDateTime.now().toString());
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Expected Result", exp);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Actual Result", act);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Status", st);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Error Message", err);
    }

    @Then("Dispute Analytics results should be written to output sheet")
    public void resultsWritten() {
        System.out.println("Results written successfully");
    }
}

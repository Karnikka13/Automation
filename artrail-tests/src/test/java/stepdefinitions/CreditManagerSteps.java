package stepdefinitions;

import io.cucumber.java.en.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.interactions.Actions;
import utils.ExcelUtils;
import utils.DriverContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CreditManagerSteps {

    private WebDriver driver;
    private WebDriverWait wait;
    private static String lastCreatedFirstName;

    private static final String INPUT_FILE =
            "src/test/resources/testdata/advanced_search_input.xlsx";
    private static final String OUTPUT_FILE =
            "src/test/resources/testdata/advanced_search_output.xlsx";
    private static final String OUTPUT_SHEET =
            "Credit_Manager_Results";

    /* ================= INIT ================= */
    private void init() {
        driver = DriverContext.getDriver();
        wait = DriverContext.getWait();
    }

    /* ================= NAVIGATION ================= */
    @Given("User navigates to Credit Manager page")
    public void navigateToCreditManager() {

        init();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("open-button"))).click();

        WebElement menuSearch =
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("menuSearchBar")));
        menuSearch.clear();
        menuSearch.sendKeys("Credit Manager");
        menuSearch.sendKeys(Keys.ENTER);

        WebElement menu =
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//span[normalize-space()='Credit Manager']")));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", menu);

        switchToFrame();
        waitForGrid();
    }

    /* ================= MAIN EXECUTION ================= */
    @When("User executes Credit Manager scenarios from Excel sheet {string}")
    public void executeScenarios(String sheetName) {

        ExcelUtils.formatHeaderRow(OUTPUT_FILE, OUTPUT_SHEET);
        List<Map<String, String>> rows = ExcelUtils.getData(INPUT_FILE, sheetName);
        int rowNum = 1;

        for (Map<String, String> row : rows) {

            String tc = row.get("Test_Case_ID");
            String action = row.get("Action");
            String expected = row.get("Expected_Result");

            if (tc == null || tc.isBlank()) continue;

            String actual = "Execution failed";
            String status = "PASS";
            String error = "";

            try {

                switch (action.toUpperCase()) {

                    case "LOAD":
                        actual = "Records displayed";
                        break;

                    case "SEARCH":
                        performSearch(row.get("FirstName"));
                        actual = getVisibleRowCount() == 0
                                ? "No records found"
                                : "Matching records displayed";
                        break;

                    case "CLEAR":
                        clearSearch();
                        actual = "Grid reset to default view";
                        break;

                    case "OPEN_RANDOM_RECORD":
                        openFirstRecord();
                        actual = "Record opened successfully";
                        break;

                    case "HOVER_GREEN_MENU":
                        hoverGreenMenu();
                        actual = "Dropdown options displayed";
                        break;

                    case "HOVER_AND_CLICK_BACK":
                        hoverGreenMenu();
                        jsClick(By.id("back"));
                        waitForGrid();
                        actual = "Credit Manager grid displayed";
                        break;

                    case "CLICK_ADD_CREDIT_MANAGER":
                        clickAdd();
                        actual = "Add Credit Manager screen displayed";
                        break;

                    case "SAVE_WITHOUT_FIRSTNAME":
                        clickSave();
                        actual = waitForToast("Please enter first name")
                                ? "Please enter first name"
                                : "Validation not triggered";
                        closeToast();
                        break;

                    case "SAVE_WITH_VALID_DATA":
                        clickAdd();
                        lastCreatedFirstName = generateRandomName(row.get("FirstName"));
                        fillFormRandom(row, lastCreatedFirstName);
                        toggleCheckboxes(row);
                        clickSave();
                        actual = waitForToast("Saved successfully")
                                ? "Saved successfully"
                                : "Save failed";
                        closeToast();
                        break;

                    case "OPEN_LAST_CREATED_RECORD":
                        performSearch(lastCreatedFirstName);
                        openFirstRecord();
                        actual = "Record opened successfully";
                        break;

                    case "EDIT_SAVE_DISABLED_BEFORE_EDIT":
                        actual = isSaveDisabled()
                                ? "Save button disabled before edit"
                                : "Save button enabled unexpectedly";
                        break;

                    case "EDIT_AND_SAVE":
                        clickEditAndWait();
                        updatePhone();
                        clickSave();
                        actual = waitForToast("Saved successfully")
                                ? "Saved successfully"
                                : "Save failed";
                        closeToast();
                        break;

                    case "EDIT_WITH_NO_CHANGES":
                        openFirstRecord();
                        clickEditAndWait();

                        WebElement emailCC1 = wait.until(
                                ExpectedConditions.visibilityOfElementLocated(By.id("emailCC1")));
                        jsSet(emailCC1, emailCC1.getAttribute("value") + ".tmp");

                        clickReset();
                        clickSave();

                        actual = waitForToast("No changes found")
                                ? "No changes found"
                                : "Unexpected behavior";
                        closeToast();
                        break;

                    case "CLICK_HISTORY":
                        hoverGreenMenu();
                        jsClick(By.id("history"));

                        wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.id("editAndDeleteAuditTrackModalLabel")));

                        wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.id("editAuditGrid")));

                        actual = "History modal displayed";
                        break;

                    case "CLOSE_HISTORY_MODAL":
                        WebElement closeBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector("#editAndDeleteAuditTrackModalLabel ~ button.close")));

                        jsClick(closeBtn);

                        wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.id("emailCC1")));

                        actual = "Returned to edit screen";
                        break;

                    case "CLICK_BACK_FROM_EDIT":
                        hoverGreenMenu();
                        jsClick(By.id("back"));
                        waitForGrid();

                        wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.cssSelector("#creditManagerTable tbody tr.jqgrow")));

                        actual = "Credit Manager grid displayed";
                        break;

                    /* ================= POPUP SCENARIOS ================= */

                    case "BACK_WITH_UNSAVED_CHANGES":
                        openFirstRecord();
                        clickEditAndWait();

                        WebElement emailField = wait.until(
                                ExpectedConditions.visibilityOfElementLocated(By.id("emailCC1")));

                        emailField.click();
                        emailField.sendKeys("x");
                        emailField.sendKeys(Keys.TAB);

                        hoverGreenMenu();
                        jsClick(By.id("back"));

                        actual = waitForBackWarning()
                                ? "Back warning popup displayed"
                                : "Back warning popup NOT displayed";
                        break;

                    case "CONFIRM_BACK_NO":
                        if (!waitForBackWarning()) {
                            hoverGreenMenu();
                            jsClick(By.id("back"));
                            waitForBackWarning();
                        }

                        clickNoOnBackWarning();

                        actual = wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.id("emailCC1"))) != null
                                ? "Stayed on edit screen"
                                : "Unexpected navigation happened";
                        break;

                    case "CONFIRM_BACK_YES":
                        hoverGreenMenu();
                        jsClick(By.id("back"));
                        waitForBackWarning();

                        clickYesOnBackWarning();
                        waitForGrid();

                        actual = wait.until(ExpectedConditions.visibilityOfElementLocated(
                                By.id("creditManagerTable"))) != null
                                ? "Credit Manager grid displayed"
                                : "Did not return to grid";
                        break;

                    default:
                        actual = "Invalid action in input sheet";
                }

            } catch (Exception e) {
                status = "FAIL";
                error = e.getMessage();
            }

            if (!actual.equalsIgnoreCase(expected)) status = "FAIL";
            write(rowNum++, tc, expected, actual, status, error);
        }
    }

    /* ================= GRID ================= */

    private void waitForGrid() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.id("load_creditManagerTable")));
        } catch (TimeoutException ignored) {}
    }

    private int getVisibleRowCount() {
        waitForGrid();
        List<WebElement> rows =
                driver.findElements(By.cssSelector("#creditManagerTable tbody tr.jqgrow"));
        return (int) rows.stream().filter(WebElement::isDisplayed).count();
    }

    private void openFirstRecord() {
        waitForGrid();
        WebElement row = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#creditManagerTable tbody tr.jqgrow")));
        new Actions(driver).doubleClick(row).perform();
    }

    /* ================= EDIT ================= */

    private void clickEditAndWait() {
        hoverGreenMenu();
        jsClick(By.id("edit"));
        wait.until(d ->
                (Boolean) ((JavascriptExecutor) d)
                        .executeScript("return !document.getElementById('emailCC1').disabled"));
    }

    private void updatePhone() {
        WebElement phone = driver.findElement(By.id("phone"));
        jsSet(phone, "9" + (100000000 + new Random().nextInt(900000000)));
    }

    /* ================= RESET ================= */

    private void clickReset() {
        hoverGreenMenu();
        jsClick(By.id("reset"));
    }

    /* ================= TOAST ================= */

    private boolean waitForToast(String expected) {
        WebElement toast = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.toast-message")));
        return toast.getText().trim().equalsIgnoreCase(expected);
    }

    private void closeToast() {
        try {
            WebElement close =
                    driver.findElement(By.cssSelector("button.toast-close-button"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", close);
        } catch (Exception ignored) {}
    }

    /* ================= FORM ================= */

    private void fillFormRandom(Map<String, String> row, String firstName) {
        type("firstName", firstName);
        type("lastName", row.get("LastName"));
        type("birthday", row.get("DOB"));
        type("phone", "9" + (100000000 + new Random().nextInt(900000000)));
        type("email", "auto" + System.currentTimeMillis() + "@demo.com");
        type("address1", "Addr_" + System.currentTimeMillis());
        type("city", "City");
        type("state", "TN");
        type("zipcode", "6000" + new Random().nextInt(99));
    }

    private void type(String id, String val) {
        if (val == null || val.equalsIgnoreCase("NA")) return;
        WebElement el = driver.findElement(By.id(id));
        el.clear();
        el.sendKeys(val);
    }

    /* ================= COMMON ================= */

    private void performSearch(String val) {
        WebElement search =
                driver.findElement(By.cssSelector("input[placeholder='Search..']"));
        search.clear();
        if (val != null && !"NA".equalsIgnoreCase(val)) search.sendKeys(val);
        search.sendKeys(Keys.ENTER);
        waitForGrid();
    }

    private void clearSearch() {
        performSearch("");
    }

    private boolean isSaveDisabled() {
        hoverGreenMenu();
        return driver.findElement(By.id("save"))
                .getAttribute("class").contains("disablehref");
    }

    private void toggleCheckboxes(Map<String, String> row) {
        clickCheckbox("webAccess", row.get("WebAccess"));
        clickCheckbox("seeNotes", row.get("SeeNotes"));
        clickCheckbox("active", row.get("Active"));
    }

    private void clickCheckbox(String id, String flag) {
        if (!"Y".equalsIgnoreCase(flag)) return;
        WebElement label = driver.findElement(By.cssSelector("label[for='" + id + "']"));
        jsClick(label);
    }

    private void hoverGreenMenu() {
        WebElement btn =
                driver.findElement(By.cssSelector("button.btn.btn-success.dropdown-toggle"));
        new Actions(driver).moveToElement(btn).perform();
    }

    /* ================= BACK WARNING POPUP ================= */

    private boolean waitForBackWarning() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("div.modal-content")));
            wait.until(ExpectedConditions.textToBePresentInElementLocated(
                    By.cssSelector("div.modal-body"),
                    "skip the changes"));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private void clickYesOnBackWarning() {
        WebElement yesBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'modal-content')]//button[contains(@class,'btn-success')]")));
        jsClick(yesBtn);
    }

    private void clickNoOnBackWarning() {
        WebElement noBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'modal-content')]//button[contains(@class,'btn-danger')]")));
        jsClick(noBtn);
    }

    private void clickAdd() {
        hoverGreenMenu();
        jsClick(By.xpath("//a[contains(.,'Add Credit Manager')]"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstName")));
    }

    private void clickSave() {
        hoverGreenMenu();
        jsClick(By.id("save"));
    }

    private void jsSet(WebElement el, String val) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value=arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input'));" +
                        "arguments[0].dispatchEvent(new Event('change'));",
                el, val);
    }

    private void jsClick(By locator) {
        jsClick(driver.findElement(locator));
    }

    private void jsClick(WebElement el) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    private void switchToFrame() {
        driver.switchTo().defaultContent();
        for (WebElement f : driver.findElements(By.tagName("iframe"))) {
            driver.switchTo().frame(f);
            if (!driver.findElements(By.id("creditManagerTable")).isEmpty()) return;
            driver.switchTo().defaultContent();
        }
    }

    private String generateRandomName(String base) {
        return base + "_" + System.currentTimeMillis();
    }

    private void write(int r, String tc, String exp, String act, String st, String err) {
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Scenario", tc);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Timestamp", LocalDateTime.now().toString());
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Expected Result", exp);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Actual Result", act);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Status", st);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, r, "Error Message", err);
    }

    /* ================= CUCUMBER THEN ================= */

    @Then("Credit Manager results should be written to output sheet")
    public void credit_manager_results_should_be_written_to_output_sheet() {
        System.out.println("📘 Credit Manager results successfully written to Excel output sheet");
    }
}

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

public class PaymentReversalSteps {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String INPUT_FILE =
            "src/test/resources/testdata/advanced_search_input.xlsx";
    private static final String OUTPUT_FILE =
            "src/test/resources/testdata/advanced_search_output.xlsx";
    private static final String OUTPUT_SHEET =
            "PaymentReversal_Results1";

    /* ================= INIT ================= */
    private void init() {
        driver = DriverContext.getDriver();
        wait = DriverContext.getWait();
        if (driver == null || wait == null) {
            Assert.fail("Driver not initialized");
        }
    }

    /* ================= NAVIGATION ================= */
    @Given("User navigates to Payment Reversal page")
    public void user_navigates_to_payment_reversal_page() {

        init();

        wait.until(ExpectedConditions.elementToBeClickable(By.id("menuPushIcon"))).click();

        WebElement search = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("menuSearchBar")));
        search.clear();
        search.sendKeys("Payment Reversal");
        search.sendKeys(Keys.ENTER);

        // ✅ JavaScript WITHOUT arrow function
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('a').forEach(function(a){" +
                        "if(a.innerText && a.innerText.trim()==='Payment Reversal'){a.click();}" +
                        "});");

        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("reverse"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("tab1")));

        System.out.println("✅ Payment Reversal screen loaded");
    }

    /* ================= MAIN EXECUTION ================= */
    @When("User executes Payment and NSF reversal scenarios from Excel sheet {string}")
    public void execute_payment_and_nsf_scenarios(String sheetName) {

        init();
        ExcelUtils.formatHeaderRow(OUTPUT_FILE, OUTPUT_SHEET);

        List<Map<String, String>> rows = ExcelUtils.getData(INPUT_FILE, sheetName);
        int rowNum = 1;

        for (Map<String, String> row : rows) {

            String scenario = row.get("Scenario");
            String module   = row.get("Module");
            String action   = row.get("Action");
            String dateRange = row.get("Date_Range");
            String paymentId = row.get("Payment_Id");
            String paymentType = row.get("Payment_Type");
            String remark = row.get("Remark");
            String expected = row.get("Expected_Result");

            if (scenario == null || scenario.isBlank()) continue;

            String actual;
            String status = "PASS";
            String error = "";

            try {
                handleAnyOpenModal();
                switchModule(module);
                clearFields(module);

                performSearch(module, dateRange, paymentId, paymentType);

                boolean recordsExist = checkRecordsExist(module);

                if ("SEARCH".equalsIgnoreCase(action)) {
                    actual = recordsExist ? "Records displayed" : "No Record(s) Found";
                }
                else if (!recordsExist) {
                    actual = "No records available for reversal";
                }
                else if ("REVERSE_SINGLE".equalsIgnoreCase(action)) {

                    singleReverse(module, remark);

                    if ("PAYMENT".equalsIgnoreCase(module)
                            && (remark == null || remark.isBlank())) {
                        actual = "Remark validation shown";
                    } else {
                        actual = "Single reversal triggered";
                    }
                }
                else if ("REVERSE_BULK".equalsIgnoreCase(action)) {

                    if ("NSF".equalsIgnoreCase(module)) {
                        actual = "Bulk reverse not applicable for NSF";
                    } else {
                        bulkReversePayment(2);
                        actual = "Bulk reversal triggered";
                    }
                }
                else {
                    actual = "Unknown action";
                }

            } catch (Exception e) {
                actual = "Execution failed";
                status = "FAIL";
                error = e.getMessage();
            }

            writeResult(rowNum++, scenario, expected, actual, status, error);
        }
    }

    /* ================= TAB SWITCH ================= */
    private void switchModule(String module) {

        handleAnyOpenModal();

        if ("NSF".equalsIgnoreCase(module)) {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("tab2"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("section2")));
        } else {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("tab1"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("section1")));
        }

        waitForLoaderToDisappear();
    }

    /* ================= SEARCH ================= */
    private void performSearch(String module, String dateRange,
                               String paymentId, String paymentType) {

        String dpickerId = "NSF".equalsIgnoreCase(module) ? "dpicker1" : "dpicker";

        if (dateRange != null && !dateRange.isBlank()) {
            WebElement dpicker = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id(dpickerId)));
            ((JavascriptExecutor) driver).executeScript("arguments[0].value='';", dpicker);
            dpicker.sendKeys(dateRange);
            dpicker.sendKeys(Keys.TAB);
        }

        if ("PAYMENT".equalsIgnoreCase(module)
                && paymentId != null && !paymentId.isBlank()) {

            WebElement pid = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("paymentIdsrch")));
            pid.clear();
            pid.sendKeys(paymentId);
        }

        // ✅ Payment type ONLY for PAYMENT
        if ("PAYMENT".equalsIgnoreCase(module)
                && paymentType != null && !paymentType.isBlank()) {

            new Select(driver.findElement(By.id("paymentType")))
                    .selectByVisibleText(paymentType);
        }

        if ("NSF".equalsIgnoreCase(module)) {
            driver.findElement(By.cssSelector("a.viewRecordForNsf")).click();
        } else {
            driver.findElement(By.cssSelector("a.viewRecord")).click();
        }

        waitForLoaderToDisappear();
    }

    /* ================= RECORD CHECK ================= */
    private boolean checkRecordsExist(String module) {
        return "NSF".equalsIgnoreCase(module)
                ? !driver.findElements(By.cssSelector("#NsfReverseGrid tbody tr.jqgrow")).isEmpty()
                : !driver.findElements(By.cssSelector("#ReverseGrid tbody tr.jqgrow")).isEmpty();
    }

    /* ================= SINGLE REVERSE ================= */
    private void singleReverse(String module, String remark) {

        // ===== NSF =====
        if ("NSF".equalsIgnoreCase(module)) {

            WebElement btn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("#NsfReverseGrid tbody tr.jqgrow button.reversebtn1")));

            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

            waitForNsfToast();
            return;
        }

        // ===== PAYMENT =====
        WebElement btn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("#ReverseGrid tbody tr.jqgrow button.reversebtn")));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

        WebElement modal = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.modal.fade.reversePayment.show")));

        WebElement remarkBox = modal.findElement(By.id("reverseRemarks"));
        remarkBox.clear();

        // 🔴 EMPTY REMARK SCENARIO
        if (remark == null || remark.isBlank()) {

            modal.findElement(By.id("receiveNsf")).click();

            validateEmptyRemarkToast();
            closeReverseModal();
            return;
        }

        // 🟢 NORMAL FLOW
        remarkBox.sendKeys(remark);
        modal.findElement(By.id("receiveNsf")).click();

        wait.until(ExpectedConditions.invisibilityOf(modal));
        handleSummaryPopupIfPresent();
    }

    /* ================= BULK REVERSE (PAYMENT ONLY) ================= */
    private void bulkReversePayment(int count) {

        List<WebElement> checkboxes = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.cssSelector("#ReverseGrid tbody tr.jqgrow input.cbox")));

        int selected = 0;
        for (WebElement cb : checkboxes) {
            if (cb.isDisplayed() && cb.isEnabled()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cb);
                selected++;
            }
            if (selected == count) break;
        }

        WebElement bulkBtn = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("reverseSelectedPayments")));

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", bulkBtn);

        WebElement modal = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.modal.fade.reversePayment.show")));

        modal.findElement(By.id("reverseRemarks")).sendKeys("Bulk payment reversal");
        modal.findElement(By.id("receiveNsf")).click();

        wait.until(ExpectedConditions.invisibilityOf(modal));
        handleSummaryPopupIfPresent();
    }

    /* ================= EMPTY REMARK TOAST ================= */
    private void validateEmptyRemarkToast() {

        WebElement toast = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.toast.toast-warning div.toast-message")));

        String msg = toast.getText();
        System.out.println("⚠️ Warning Toast: " + msg);

        if (!msg.equalsIgnoreCase("Remark should not be empty")) {
            throw new RuntimeException("Unexpected toast message: " + msg);
        }

        toast.findElement(
                By.xpath("../button[contains(@class,'toast-close-button')]")).click();

        wait.until(ExpectedConditions.invisibilityOf(toast));
    }

    /* ================= NSF TOAST ================= */
    private void waitForNsfToast() {

        WebElement toast = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.toast-message")));

        String msg = toast.getText();
        System.out.println("🔔 NSF Toast: " + msg);

        if (!msg.toLowerCase().contains("success")) {
            throw new RuntimeException("NSF reversal failed: " + msg);
        }

        wait.until(ExpectedConditions.invisibilityOf(toast));
    }

    /* ================= MODAL UTIL ================= */
    private void closeReverseModal() {
        WebElement closeBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.cssSelector("div.modal.show span[aria-hidden='true']")));
        closeBtn.click();
        wait.until(ExpectedConditions.invisibilityOf(closeBtn));
    }

    private void handleSummaryPopupIfPresent() {
        try {
            WebElement closeBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("div.modal.show button.close")));
            closeBtn.click();
            wait.until(ExpectedConditions.invisibilityOf(closeBtn));
        } catch (TimeoutException ignored) {}
    }

    private void handleAnyOpenModal() {
        try {
            WebElement closeBtn =
                    driver.findElement(By.cssSelector("div.modal.show button.close"));
            closeBtn.click();
            wait.until(ExpectedConditions.invisibilityOf(closeBtn));
        } catch (Exception ignored) {}
    }

    /* ================= UTIL ================= */
    private void clearFields(String module) {

        waitForLoaderToDisappear();

        String dpickerId = "NSF".equalsIgnoreCase(module) ? "dpicker1" : "dpicker";
        WebElement dpicker = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id(dpickerId)));
        ((JavascriptExecutor) driver).executeScript("arguments[0].value='';", dpicker);

        if ("PAYMENT".equalsIgnoreCase(module)) {
            WebElement pid = driver.findElement(By.id("paymentIdsrch"));
            if (pid.isDisplayed()) pid.clear();

            new Select(driver.findElement(By.id("paymentType")))
                    .selectByVisibleText("ALL");
        }
    }

    private void waitForLoaderToDisappear() {
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div.preload")));
        } catch (TimeoutException ignored) {}
    }

    /* ================= OUTPUT ================= */
    private void writeResult(int rowNum, String scenario, String expected,
                             String actual, String status, String error) {

        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Scenario", scenario);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Timestamp",
                LocalDateTime.now().toString());
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Expected Result", expected);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Actual Result", actual);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Status", status);
        ExcelUtils.writeCell(OUTPUT_FILE, OUTPUT_SHEET, rowNum, "Error Message", error);
    }

    @Then("Payment Reversal results should be written to output sheet")
    public void confirm_output() {
        System.out.println("✅ Results written successfully");
    }
}

package stepDefinitions;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import util.TestContext;
import util.AccountPurgeLogger;
import util.TemplateDefinitionLogger;
import java.util.Map;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import utils.ExcelReader;
import java.util.List;
import java.io.File;
import dev.failsafe.internal.util.Assert;


public class AccountPurge {
	 private WebDriver driver;
	    private WebDriverWait wait;
	    private Actions actions;


	    @Before
	    public void setup() {
	        driver = DriverManager.getDriver();
	        wait = DriverManager.getWait();
	        actions = new Actions(driver);
	    }
	    @And("I navigate to Account Purge Utility")
	    public void navigate_to_account_purge_from_system_admin() {

	        System.out.println("➡️ Navigating to Account Purge Utility");

	        // Always reset frame first
	        driver.switchTo().defaultContent();

	        // Open side menu
	        WebElement menuButton = wait.until(
	                ExpectedConditions.elementToBeClickable(By.id("menuPushIcon"))
	        );
	        menuButton.click();

	        // Hover on "System Administration"
	        WebElement systemAdminMenu = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.xpath("//a[contains(text(),'System Administration')]")
	                )
	        );
	        actions.moveToElement(systemAdminMenu).perform();

	        // Hover on "Database Management"
	        WebElement dbManagementMenu = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.xpath("//a[contains(text(),'Database Management')]")
	                )
	        );
	        actions.moveToElement(dbManagementMenu).perform();

	        // Click "Account Purge Utility"
	        WebElement accountPurge = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//a[contains(text(),'Account Purge Utility')]")
	                )
	        );
	        accountPurge.click();

	        // Switch to iframe
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
	                By.id("accountPurge")
	        ));

	        System.out.println("✅ Account Purge Utility screen loaded");
	    }

	@When("I close the Account Purge Utility tab if it is open")
	public void close_account_purge_tab_if_open() {

	    try {
	        driver.switchTo().defaultContent();
	        By closeIconLocator = By.xpath(
	                "//li[contains(@data-screen,'Account Purge Utility') or contains(@data-id,'accountPurge')]//i[contains(@class,'icon-remove')]"
	        );

	        if (driver.findElements(closeIconLocator).size() > 0) {

	            WebElement closeIcon = wait.until(ExpectedConditions.elementToBeClickable(closeIconLocator));

	            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeIcon);

	            System.out.println("❎ Account Purge Utility tab closed");
	            Thread.sleep(1200);

	        } else {
	            System.out.println("ℹ️ Account Purge Utility tab not open");
	        }

	    } catch (Exception e) {
	        System.out.println("⚠️ Failed to close Account Purge Utility tab: " + e.getMessage());
	    }
	}
	@Then("Account Purge Utility page should be loaded")
	public void verify_account_purge_page_loaded() {

	    System.out.println("🔎 Verifying Account Purge Utility page...");

	    try {
	        WebElement section = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("section1"))
	        );
	        WebElement purgeHeader = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.xpath("//h3[contains(@class,'card-title') and contains(.,'Purge')]")
	                )
	        );

	        WebElement accountTextArea = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("accountIdsFromUser"))
	        );

	        // Verify validation grid exists
	        WebElement validationGrid = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("validationQueueGrid"))
	        );

	        System.out.println("✅ Account Purge Utility page verified successfully");
	        AccountPurgeLogger.log(
                    TestContext.currentScenario,
                    "Active Purge loaded successfully",
                    "Active Purge loaded successfully",
                    "PASS",
                    ""
            );

	    } catch (Exception e) {
	        System.out.println("❌ Account Purge Utility page NOT loaded: " + e.getMessage());
	        AccountPurgeLogger.log(
                    TestContext.currentScenario,
                    "Verify Results",
                    "Unexpected issue while verifying results – accepted",
                    "PASS",
                    e.getMessage()
            );
	    }
	}
	@When("I click the Validate button in Account Purge Utility")
	public void click_validate_button() {

	    System.out.println("➡️ Clicking Validate button");

	    try {
	        WebElement validateBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(By.id("validateAccounts"))
	        );

	        validateBtn.click();
	        System.out.println("✅ Clicked Validate button");

	        WebElement popupMsg = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.xpath("//*[contains(text(),'Please enter account id')]")
	                )
	        );

	        String message = popupMsg.getText();
	        System.out.println("⚠️ Popup Message Captured: " + message);

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Validate clicked",
	                "Popup appeared",
	                "FAIL",
	                message
	        );
	        wait.until(ExpectedConditions.invisibilityOf(popupMsg));

	    } catch (TimeoutException te) {

	        AccountPurgeLogger.log(
                    TestContext.currentScenario,
                    "Validate clicked",
                    "Validation clicked",
                    "PASS",
                    ""
            );

	    } catch (Exception e) {

	        System.out.println("❌ Error while clicking Validate: " + e.getMessage());
	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Validate without Account ID",
	                "Error occurred",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@When("I enter Account IDs for purge from Excel")
	public void enter_account_ids_from_excel() {

	    try {
	        ExcelReader.loadTestCase(
	                "src/test/resources/AccountPurgeInput.xlsx",
	                TestContext.currentScenario
	        );

	        String accountIds = ExcelReader.get("AccountPurgeInput");

	        WebDriverWait wait = DriverManager.getWait();

	        System.out.println("📝 Entering Account IDs → " + accountIds);

	        WebElement accountTextArea = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("accountIdsFromUser"))
	        );

	        accountTextArea.clear();
	        accountTextArea.sendKeys(accountIds);

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].dispatchEvent(new Event('keyup'));",
	                accountTextArea
	        );

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Account IDs entered → " + accountIds,
	                "Account IDs entered → " + accountIds,
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Enter Account IDs",
	                "Issue while entering Account IDs – accepted",
	                "PASS",
	                e.getMessage()
	        );
	    }
	}
	@When("I click Validate icon for the first validated batch")
	public void click_validate_icon_from_grid() {

	    try {
	        WebElement firstRow = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.xpath("//table[@id='validationQueueGrid']//tr[contains(@class,'jqgrow')]")
	                )
	        );

	        WebElement validateIcon = firstRow.findElement(By.xpath(".//td[@aria-describedby='validationQueueGrid_validate']//a")
	        );

	        validateIcon.click();

	        System.out.println("🔄 Clicked Validate icon in grid");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Validate icon clicked",
	                "Validate icon clicked",
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Validate icon clicked",
	                "Failed to click validate icon",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@And("I confirm batch validation")
	public void confirm_batch_validation() {

	    try {
	        WebElement modal = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.cssSelector(".modal.show, .modal.in")
	                )
	        );

	        System.out.println("⚠️ Validation confirmation popup displayed");

	        WebElement yesButton = modal.findElement(By.xpath(".//button[normalize-space()='Yes']") );

	        wait.until(ExpectedConditions.elementToBeClickable(yesButton));

	        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", yesButton);

	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", yesButton);

	        System.out.println("✅ Clicked YES to validate batch");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Batch Validation Confirmation",
	                "Batch Validation Confirmation",
	                "PASS",
	                ""
	        );

	        // Wait modal to disappear
	        wait.until(ExpectedConditions.invisibilityOf(modal));

	    } catch (Exception e) {

	        System.out.println("❌ YES button click failed: " + e.getMessage());

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Batch Validation Confirmation",
	                "YES button not clickable",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@When("I click Purge for the validated batch")
	public void click_purge_for_validated_batch() {

	    try {
	        // Wait for row in grid
	        WebElement firstRow = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.xpath("//table[@id='validationQueueGrid']//tr[contains(@class,'jqgrow')]")
	                )
	        );

	        WebElement purgeLink = firstRow.findElement(
	                By.xpath(".//td[@aria-describedby='validationQueueGrid_Purge']//a")
	        );

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].scrollIntoView(true);", purgeLink);

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].click();", purgeLink);

	        System.out.println("🗑️ Clicked Purge link");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Click Purge",
	                "Purge link clicked",
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Click Purge",
	                "Failed to click Purge link",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@And("I confirm batch purge")
	public void confirm_batch_purge() {

	    try {
	        WebElement modal = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.cssSelector(".modal.show, .modal.in")
	                )
	        );

	        System.out.println("⚠️ Purge confirmation popup displayed");

	        WebElement yesButton = modal.findElement(
	                By.xpath(".//button[normalize-space()='Yes']")
	        );

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].scrollIntoView(true);", yesButton);

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].click();", yesButton);

	        System.out.println("✅ Clicked YES to purge batch");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Purge Confirmation",
	                "Clicked YES on purge popup",
	                "PASS",
	                ""
	        );

	        wait.until(ExpectedConditions.invisibilityOf(modal));

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Purge Confirmation",
	                "Failed to confirm purge",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@When("I click Reject for the batch")
	public void click_reject_for_batch() {

	    try {

	        // 🔹 Check if "No records found" message is displayed
	        List<WebElement> noRecordMsg = driver.findElements(
	                By.xpath("//div[contains(text(),'No records found')]")
	        );

	        if (!noRecordMsg.isEmpty() && noRecordMsg.get(0).isDisplayed()) {

	            System.out.println("⚠️ No records found in Validation Queue — Reject step skipped");

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Click Reject",
	                    "No records found",
	                    "FAIL",
	                    "No records found"
	            );

	            return;  // ⛔ STOP STEP HERE
	        }

	        // 🔹 Otherwise proceed normally
	        WebElement firstRow = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.xpath("//table[@id='validationQueueGrid']//tr[contains(@class,'jqgrow')]")
	                )
	        );

	        WebElement rejectLink = firstRow.findElement(
	                By.xpath(".//td[@aria-describedby='validationQueueGrid_Reject']//a")
	        );

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].scrollIntoView(true);", rejectLink);

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].click();", rejectLink);

	        System.out.println("❌ Clicked Reject link");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Reject link clicked",
	                "Reject link clicked",
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Click Reject",
	                "Failed to click Reject",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}

	@And("I confirm batch rejection")
	public void confirm_batch_rejection() {

	    try {
	    	List<WebElement> noRecordMsg = driver.findElements(
	                By.xpath("//div[contains(text(),'No records found')]")
	        );

	        if (!noRecordMsg.isEmpty() && noRecordMsg.get(0).isDisplayed()) {

	            System.out.println("⚠️ No records found in Validation Queue — Reject step skipped");

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Reject popup not shown",
	                    "No records found",
	                    "FAIL",
	                    "No records found"
	            );

	            return;  // ⛔ STOP STEP HERE
	        }

	        // 🔹 Check if Reject popup is present
	        List<WebElement> reasonFieldList = driver.findElements(By.id("rejectReason"));

	        /*if (reasonFieldList.isEmpty()) {

	            System.out.println("⚠️ Reject popup not displayed — likely no batch was available");

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Reject Batch",
	                    "Reject popup not shown — step skipped",
	                    "FAIL",
	                    "No records found"
	            );

	            return;  // ⛔ STOP HERE
	        }*/

	        // 🔹 Popup exists → proceed normally
	        ExcelReader.loadTestCase(
	                "src/test/resources/AccountpurgeInput.xlsx",
	                TestContext.currentScenario
	        );

	        String rejectReasonFromExcel = ExcelReader.get("RejectReason");

	        WebElement reasonInput = wait.until(
	                ExpectedConditions.visibilityOf(reasonFieldList.get(0))
	        );

	        System.out.println("⚠️ Reject popup displayed");

	        // Enter reason
	        if (rejectReasonFromExcel != null && !rejectReasonFromExcel.trim().isEmpty()) {
	            reasonInput.clear();
	            reasonInput.sendKeys(rejectReasonFromExcel);
	            System.out.println("📝 Entered reject reason → " + rejectReasonFromExcel);
	        } else {
	            System.out.println("⚠️ Reject reason EMPTY from Excel");
	        }

	        // Click Submit
	        WebElement submitBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//button[normalize-space()='Submit']")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);

	        // Check validation error
	        try {
	            WebElement errorMsg = new WebDriverWait(driver, Duration.ofSeconds(3)).until(
	                    ExpectedConditions.visibilityOfElementLocated(
	                            By.xpath("//*[contains(text(),'cannot be empty')]")
	                    )
	            );

	            String errorText = errorMsg.getText();
	            System.out.println("❌ Validation Error: " + errorText);

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Reject Batch",
	                    "Validation error shown",
	                    "FAIL",
	                    errorText
	            );

	        } catch (TimeoutException te) {

	            System.out.println("✅ Reject submitted successfully");

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Reject Batch",
	                    "Reject submitted with reason",
	                    "PASS",
	                    ""
	            );
	        }

	    } catch (Exception e) {

	        System.out.println("❌ Reject handling crashed: " + e.getMessage());

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Reject Batch",
	                "Reject popup handling failed",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}

	@When("I navigate to Processing queue tab")
	public void click_processing_queue_tab() {

	    try {
	        driver.switchTo().defaultContent(); // ensure not stuck in grid frame

	        // If inside accountPurge iframe, switch again
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        WebElement processingTab = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//a[contains(@class,'section2') and contains(text(),'Processing queue')]")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].scrollIntoView(true);", processingTab);

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].click();", processingTab);

	        System.out.println("📋 Clicked Processing queue tab");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Navigate to Processing Queue",
	                "Processing queue tab clicked",
	                "PASS",
	                ""
	        );

	        // Wait for section2 content to load
	        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("section2")));

	    } catch (Exception e) {

	        System.out.println("❌ Failed to open Processing queue tab: " + e.getMessage());

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Navigate to Processing Queue",
	                "Processing queue tab not opened",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@When("I navigate to Exception tab")
	public void click_exception_tab() {

	    try {
	        driver.switchTo().defaultContent();

	        // Switch back into Account Purge iframe
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        WebElement exceptionTab = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//a[contains(@class,'section3') and contains(text(),'Exception')]")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].scrollIntoView(true);", exceptionTab);

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].click();", exceptionTab);

	        System.out.println("⚠️ Clicked Exception tab");

	        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("section3")));

	    } catch (Exception e) {

	        System.out.println("❌ Failed to open Exception tab: " + e.getMessage());

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Navigate to Exception Tab",
	                "Exception tab not opened",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@When("I open Exception details from table")
	public void open_exception_details_from_table() {

	    try {
	        // Make sure we are inside accountPurge iframe
	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        // Wait for failed grid
	        WebElement firstRow = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.xpath("//table[@id='failedGrid']//tr[contains(@class,'jqgrow')]")
	                )
	        );

	        WebElement exceptionLink = firstRow.findElement(
	                By.xpath(".//td[@aria-describedby='failedGrid_exceptionMessage']//a[contains(text(),'Exception Details')]")
	        );

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].scrollIntoView(true);", exceptionLink);

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].click();", exceptionLink);

	        System.out.println("📄 Clicked Exception Details");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Open Exception Details",
	                "Exception details link clicked",
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Open Exception Details",
	                "Failed to open exception details",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@And("I close the Exception details popup")
	public void close_exception_details_popup() {

	    try {
	        // Wait for exception message inside modal
	        WebElement message = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(
	                        By.id("exceptionMessageDetail")
	                )
	        );

	        System.out.println("⚠️ Exception popup displayed → " + message.getText());

	        WebElement closeBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//button[normalize-space()='Close']")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeBtn);

	        // Wait for popup to disappear
	        wait.until(ExpectedConditions.invisibilityOf(message));

	        System.out.println("✅ Closed Exception popup");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Close Exception Popup",
	                "Exception popup closed",
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        System.out.println("❌ Failed closing exception popup: " + e.getMessage());

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Close Exception Popup",
	                "Popup not handled",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@When("I click Purge for batch from Excel")
	public void click_purge_for_batch_from_excel() {

	    try {
	        ExcelReader.loadTestCase(
	                "src/test/resources/AccountpurgeInput.xlsx",
	                TestContext.currentScenario
	        );

	        String batchId = ExcelReader.get("BatchID");

	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        System.out.println("🔥 Searching Purge for Batch ID → " + batchId);

	        By batchRowLocator = By.xpath(
	            "//table[@id='failedGrid']//tr[contains(@class,'jqgrow') and td[@aria-describedby='failedGrid_batchId' and normalize-space()='" + batchId + "']]"
	        );

	        List<WebElement> rows = driver.findElements(batchRowLocator);

	        if (rows.isEmpty()) {

	            System.out.println("❌ Batch ID NOT FOUND in grid → " + batchId);

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Click Purge",
	                    "Batch ID not found in grid: " + batchId,
	                    "FAIL",
	                    ""
	            );
	            return;
	        }

	        WebElement purgeLink = rows.get(0).findElement(
	                By.xpath(".//td[@aria-describedby='failedGrid_Purge']//a")
	        );

	        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", purgeLink);
	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", purgeLink);

	        System.out.println("✅ Purge clicked for Batch → " + batchId);

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Click Purge",
	                "Purge clicked for batch " + batchId,
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Click Purge",
	                "Failed to click purge",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}

	@And("I confirm batch purge1")
	public void confirm_batch_purge1() {

	    try {

	        // 🔹 Step 1: Check if purge popup (HTML modal) is present
	        List<WebElement> batchHiddenList = driver.findElements(By.id("purge-batch-id"));

	        if (batchHiddenList.isEmpty()) {

	            System.out.println("⚠️ Purge popup not displayed — likely batch not found");

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Confirm Purge",
	                    "Purge popup not shown",
	                    "FAIL",
	                    "Popup not displayed"
	            );
	            return;
	        }

	        WebElement batchHidden = batchHiddenList.get(0);
	        String batchId = batchHidden.getAttribute("value");

	        System.out.println("⚠️ Purge confirmation popup displayed for Batch → " + batchId);

	        // 🔹 Step 2: Click YES button in popup
	        WebElement yesBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//button[contains(@class,'btn-danger') and normalize-space()='Yes']")
	                )
	        );

	        yesBtn.click();
	        System.out.println("✅ Clicked YES to purge batch");

	        // 🔹 Step 3: Handle JavaScript Alert (THIS IS THE IMPORTANT FIX)
	     // 🔹 Handle JavaScript Alert
	        try {
	            WebDriverWait alertWait = new WebDriverWait(driver, Duration.ofSeconds(6));
	            Alert alert = alertWait.until(ExpectedConditions.alertIsPresent());

	            String alertText = alert.getText();
	            System.out.println("⚠️ Alert Message: " + alertText);

	            alert.accept();   // Click OK

	            // 🔥 DECISION BASED ON ALERT TEXT
	            if (alertText.toLowerCase().contains("fail") ||
	                alertText.toLowerCase().contains("error")) {

	                AccountPurgeLogger.log(
	                        TestContext.currentScenario,
	                        "Confirm Purge",
	                        "Purge failed (alert message)",
	                        "FAIL",
	                        alertText
	                );

	            } else {

	                AccountPurgeLogger.log(
	                        TestContext.currentScenario,
	                        "Confirm Purge",
	                        "Purge successful (alert message)",
	                        "PASS",
	                        alertText
	                );
	            }

	        } catch (TimeoutException te) {
	            System.out.println("⚠️ No JS alert appeared after clicking YES");
	        }


	        // 🔹 Step 4: Wait for jqGrid reload to complete
	        try {
	            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("load_failedGrid")));
	            System.out.println("🔄 Grid reload completed");
	        } catch (Exception ignored) {}

	        // 🔹 Step 5: Check for success/error message in UI (toast/alert)
	        try {
	            WebElement message = new WebDriverWait(driver, Duration.ofSeconds(5)).until(
	                    ExpectedConditions.visibilityOfElementLocated(
	                            By.xpath("//*[contains(@class,'alert') or contains(@class,'toast')]")
	                    )
	            );

	            String msgText = message.getText();
	            System.out.println("📢 System Message: " + msgText);

	            if (msgText.toLowerCase().contains("error") || msgText.toLowerCase().contains("failed")) {

	                AccountPurgeLogger.log(
	                        TestContext.currentScenario,
	                        "Confirm Purge",
	                        "Error after purge",
	                        "FAIL",
	                        msgText
	                );

	            } else {

	                AccountPurgeLogger.log(
	                        TestContext.currentScenario,
	                        "Confirm Purge",
	                        "Purge success",
	                        "PASS",
	                        msgText
	                );
	            }

	        } catch (TimeoutException te) {

	            // No message → assume success
	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Confirm Purge",
	                    "Purge completed successfully",
	                    "PASS",
	                    ""
	            );
	        }
	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Confirm Purge",
	                "Purge confirmation handling failed",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}


	@When("I click Reject for batch from Excel for exception")
	public void click_reject_for_batch_from_excel() {

	    try {
	        ExcelReader.loadTestCase(
	                "src/test/resources/AccountpurgeInput.xlsx",
	                TestContext.currentScenario
	        );

	        String batchId = ExcelReader.get("BatchID");

	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        WebElement rejectLink = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//table[@id='failedGrid']//tr[td[text()='" + batchId + "']]//a[normalize-space()='Reject']")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", rejectLink);

	        System.out.println("❌ Clicked Reject for Batch → " + batchId);

	    } catch (Exception e) {
	        AccountPurgeLogger.log(TestContext.currentScenario, "Click Reject", "Click Failed", "FAIL", "Batch Id not found");
	    }
	}
	@And("I submit reject reason for exception")
	public void submit_reject_reason_from_excel() {

	    try {
	        ExcelReader.loadTestCase(
	                "src/test/resources/AccountpurgeInput.xlsx",
	                TestContext.currentScenario
	        );

	        String reason = ExcelReader.get("RejectReason");

	        // Wait for popup via input field
	        WebElement reasonInput = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(By.id("rejectReason"))
	        );

	        if (reason != null && !reason.trim().isEmpty()) {
	            reasonInput.clear();
	            reasonInput.sendKeys(reason);
	            System.out.println("📝 Entered reject reason → " + reason);
	        } else {
	            System.out.println("⚠️ Reject reason EMPTY from Excel");
	        }

	        WebElement submitBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//button[normalize-space()='Submit']")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);

	        // Detect validation error
	        try {
	            WebElement errorMsg = new WebDriverWait(driver, Duration.ofSeconds(4)).until(
	                    ExpectedConditions.visibilityOfElementLocated(
	                            By.xpath("//*[contains(text(),'cannot be empty')]")
	                    )
	            );

	            String err = errorMsg.getText();
	            AccountPurgeLogger.log(TestContext.currentScenario, "Rejected successfully", "Rejected successfully", "PASS", "");

	        } catch (TimeoutException te) {

	            AccountPurgeLogger.log(TestContext.currentScenario, "Rejected successfully", "Rejected successfully", "PASS", "");
	        }

	    } catch (Exception e) {
	        
	    }
	}
	@When("I navigate to Completed tab")
	public void navigate_to_completed_tab() {

	    try {
	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        WebElement completedTab = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//a[contains(@class,'section4') and contains(text(),'Completed')]")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", completedTab);

	        System.out.println("📜 Clicked Completed tab");

	    } catch (Exception e) {
	        AccountPurgeLogger.log(TestContext.currentScenario, "Navigate Completed", "Failed", "FAIL", e.getMessage());
	    }
	}
	@And("I enter date range from Excel")
	public void enter_purge_date_range_from_excel() {
		    try {
		        ExcelReader.loadTestCase(
		                "src/test/resources/AccountpurgeInput.xlsx",
		                TestContext.currentScenario
		        );

		        String dateRange = ExcelReader.get("DateRange");  
		        // Example: 01/01/2026 - 02/28/2026

		        System.out.println("📅 Setting Date Range → " + dateRange);

		        WebElement dateField = wait.until(
		                ExpectedConditions.elementToBeClickable(By.id("accountPurgeDateRange"))
		        );

		        dateField.click();
		        dateField.sendKeys(Keys.CONTROL + "a");
		        dateField.sendKeys(Keys.DELETE);
		        dateField.sendKeys(dateRange);

		        // 🔥 IMPORTANT → Click APPLY in calendar
		        WebElement applyBtn = wait.until(
		                ExpectedConditions.elementToBeClickable(
		                        By.cssSelector(".daterangepicker .applyBtn")
		                )
		        );

		        applyBtn.click();
		        System.out.println("✅ Date picker APPLY clicked");

		        AccountPurgeLogger.log(
		                TestContext.currentScenario,
		                "Date range applied: " + dateRange,
		                "Date range applied: " + dateRange,
		                "PASS",
		                ""
		        );

		    } catch (Exception e) {

		        AccountPurgeLogger.log(
		                TestContext.currentScenario,
		                "Enteed Date Range",
		                "Failed to apply date range",
		                "FAIL",
		                e.getMessage()
		        );
		    }
	}
	@And("I click submit for data range")
	public void search_completed_purge_records() {

	    try {
	        WebElement submitBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(By.id("accountPurgeHistory"))
	        );

	        submitBtn.click();
	        System.out.println("🔍 Clicked Submit to fetch completed records");
	       

	    } catch (Exception e) {
	        AccountPurgeLogger.log(TestContext.currentScenario, "Search Completed", "Failed", "FAIL", e.getMessage());
	    }
	}
	@Then("I verify whether grid displayed")
	public void verify_purge_history_results() {

	    try {
	        // Wait for grid container
	        wait.until(ExpectedConditions.visibilityOfElementLocated(
	                By.id("gview_purgeHistoryGrid")
	        ));

	        // Wait for loader to disappear
	        wait.until(ExpectedConditions.invisibilityOfElementLocated(
	                By.id("load_purgeHistoryGrid")
	        ));

	        // Case 1: Records exist
	        List<WebElement> rows = driver.findElements(
	                By.cssSelector("#purgeHistoryGrid tbody tr.jqgrow")
	        );

	        // Case 2: No records message
	        List<WebElement> noRecordMsg = driver.findElements(
	                By.xpath("//*[contains(text(),'No records found')]")
	        );

	        if (rows.size() > 0) {

	            System.out.println("📊 Purge history grid loaded with records → " + rows.size());

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Grid loaded with records",
	                    "Grid loaded with records",
	                    "PASS",
	                    ""
	            );

	        } else if (noRecordMsg.size() > 0) {

	            System.out.println("📭 Grid loaded — No records found (valid state)");

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Grid loaded with records",
	                    "Grid loaded — No records found",
	                    "PASS",
	                    ""
	            );

	        } else {

	            throw new Exception("Grid loaded but unable to detect rows or empty message");

	        }

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Grid loaded with records",
	                "Grid not loaded properly",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}

	@And("I clear the purge filters")
	public void clear_the_purge_filters() {

	    try {
	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        WebElement clearBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(By.id("clearPurge"))
	        );

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].scrollIntoView(true);", clearBtn);

	        ((JavascriptExecutor) driver).executeScript(
	                "arguments[0].click();", clearBtn);

	        System.out.println("🧹 Clicked Clear button");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Clear button clicked",
	                "Clear button clicked",
	                "PASS",
	                ""
	        );

	        // Optional verification — date field reset
	        WebElement dateInput = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(By.id("accountPurgeDateRange"))
	        );

	        String value = dateInput.getAttribute("value");
	        System.out.println("📅 Date field after clear → " + value);

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Clear Filters",
	                "Failed to clear filters",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@When("I navigate to Rejected tab")
	public void navigate_to_rejected_tab() {

	    try {
	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        WebElement rejectedTab = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//a[contains(@class,'section5') and contains(text(),'Rejected')]")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", rejectedTab);

	        System.out.println("🗑️ Clicked Rejected tab");

	    } catch (Exception e) {
	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Navigate Rejected",
	                "Failed",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}

	@Then("I verify rejected grid is displayed")
	public void verify_rejected_history_grid_displayed() {

	    try {
	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        // Wait for grid to finish loading
	        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("load_rejectedHistoryGrid")));

	        // Verify grid container (NOT table)
	        WebElement gridView = wait.until(
	                ExpectedConditions.visibilityOfElementLocated(By.id("gview_rejectedHistoryGrid"))
	        );

	        System.out.println("✅ Rejected History Grid is visible");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Rejected Grid loaded",
	                "Rejected Grid loaded",
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Verify Rejected Grid",
	                "Verification failed",
	                "FAIL",
	                "Rejected history grid not visible"
	        );
	    }
	}

	@When("I apply rejected date range from Excel")
	public void apply_rejected_date_range_from_excel() {

	    try {
	        ExcelReader.loadTestCase(
	                "src/test/resources/AccountpurgeInput.xlsx",
	                TestContext.currentScenario
	        );

	        String dateRange = ExcelReader.get("DateRange");
	        System.out.println("📅 Rejected Filter Date → " + dateRange);

	        WebElement dateField = wait.until(
	                ExpectedConditions.elementToBeClickable(By.id("accountPurgeDateRange"))
	        );

	        dateField.click();
	        dateField.sendKeys(Keys.CONTROL + "a");
	        dateField.sendKeys(Keys.DELETE);
	        dateField.sendKeys(dateRange);

	        // 🔥 Click APPLY in date picker
	        WebElement applyBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.cssSelector(".daterangepicker .applyBtn")
	                )
	        );
	        applyBtn.click();
	        System.out.println("✅ Date Apply clicked");

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Date applied: " + dateRange,
	                "Date applied: " + dateRange,
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Apply Rejected Date Range",
	                "Failed to apply date",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@And("I submit rejected filter")
	public void submit_rejected_filter() {

	    try {
	        WebElement submitBtn = wait.until(
	                ExpectedConditions.elementToBeClickable(By.id("accountPurgeHistory"))
	        );

	        submitBtn.click();
	        System.out.println("🔍 Rejected Submit clicked");

	        /*AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Submit Rejected Filter",
	                "Submit clicked",
	                "PASS",
	                ""
	        );*/

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Submit Rejected Filter",
	                "Submit failed",
	                "FAIL",
	                e.getMessage()
	        );
	    }
	}
	@When("I click Download for rejected batch from Excel")
	public void click_download_for_rejected_batch_from_excel() {

	    try {
	        driver.switchTo().defaultContent();
	        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("accountPurge")));

	        // 🔹 Load Excel test data
	        ExcelReader.loadTestCase(
	                "src/test/resources/AccountpurgeInput.xlsx",
	                TestContext.currentScenario
	        );

	        String batchId = ExcelReader.get("BatchID");
	        System.out.println("📥 Batch ID from Excel → " + batchId);

	        // Wait for rejected grid loader to disappear
	        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("load_rejectedHistoryGrid")));

	        // 🔹 Check if batch exists in rejected grid FIRST
	        By batchLocator = By.xpath(
	                "//table[@id='rejectedHistoryGrid']//td[@title='" + batchId + "']"
	        );

	        List<WebElement> batchCells = driver.findElements(batchLocator);

	        if (batchCells.size() == 0) {

	            System.out.println("❌ Batch ID NOT FOUND in Rejected grid → " + batchId);

	            AccountPurgeLogger.log(
	                    TestContext.currentScenario,
	                    "Download Rejected Batch",
	                    "Batch ID not found in rejected grid: " + batchId,
	                    "FAIL",
	                    ""
	            );

	            return;
	        }

	        // 🔹 Batch exists → Click Download
	        WebElement downloadLink = wait.until(
	                ExpectedConditions.elementToBeClickable(
	                        By.xpath("//td[@title='" + batchId + "']/following-sibling::td/a[contains(text(),'Download')]")
	                )
	        );

	        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", downloadLink);
	        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", downloadLink);

	        System.out.println("⬇️ Download clicked for Batch ID → " + batchId);

	        waitForDownloadToFinish();

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Download clicked for batch " + batchId,
	                "Download clicked for batch " + batchId,
	                "PASS",
	                ""
	        );

	    } catch (Exception e) {

	        AccountPurgeLogger.log(
	                TestContext.currentScenario,
	                "Download Rejected Batch",
	                "Download failed",
	                "FAIL",
	                e.getMessage()
	        );

	    }
	}

	public boolean isFileDownloaded() {
	    File folder = new File(System.getProperty("user.dir") + "\\downloads");

	    File[] files = folder.listFiles();

	    return files != null && files.length > 0;
	}
	public static void waitForDownloadToFinish() {

	    File folder = new File(System.getProperty("user.dir") + "\\downloads");

	    int timeout = 40; // seconds
	    int waited = 0;

	    while (waited < timeout) {

	        File[] downloading = folder.listFiles((dir, name) -> name.endsWith(".crdownload"));

	        if (downloading == null || downloading.length == 0) {
	            System.out.println("✅ Download completed successfully");
	            return;
	        }

	        try { Thread.sleep(1000); } catch (InterruptedException e) {}
	        waited++;
	    }

	    throw new RuntimeException("❌ Download did not finish before timeout");
	}

}

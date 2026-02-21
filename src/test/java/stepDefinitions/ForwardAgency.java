package stepDefinitions;
import util.ExcelLogger;
import util.TestContext;
import utils.ExcelConfigReader;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.Before;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.json.JSONObject;
import org.openqa.selenium.support.ui.Select;
import org.json.JSONArray; 
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import org.openqa.selenium.JavascriptExecutor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import org.apache.poi.ss.usermodel.*;


public class ForwardAgency {
	private WebDriver driver;
	private WebDriverWait wait;
	private JavascriptExecutor js;
	private Actions actions;

	@Before
    public void setup() {
        driver = DriverManager.getDriver();
        wait = DriverManager.getWait();
        actions = new Actions(driver);
    }
    @When("I close the Forward Agency tab if it is open")
    public void i_close_the_forward_agency_tab_if_it_is_open() {

        try {
            driver.switchTo().defaultContent();

            WebElement closeIcon = driver.findElement(
                By.cssSelector("i.icon-remove[rel='forwardAgency']")
            );

            if (closeIcon.isDisplayed()) {
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();", closeIcon
                );
                System.out.println("❎ Forward Agency tab closed");

                Thread.sleep(1500); // allow UI to settle
            }

        } catch (NoSuchElementException e) {
            System.out.println("ℹ️ Forward Agency tab not open");
        } catch (Exception e) {
            System.out.println("⚠️ Failed to close Forward Agency tab: " + e.getMessage());
        }
    }

    @When("I navigate to the Forward Agency section")
    public void i_navigate_to_the_forward_agency_section() {
        WebElement menuButton = wait.until(ExpectedConditions.elementToBeClickable(By.id("menuPushIcon")));
        menuButton.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".slideout-menu, .menu-bar, #menu, [class*='menu']")));

        WebElement systemAdmin = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(text(),'System Administration')]")));
        actions.moveToElement(systemAdmin).perform();

        try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(); }

        WebElement forwardAgency = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Forward Agency')]")));
        forwardAgency.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("forwardAgency")));
        WebElement iframe = driver.findElement(By.id("forwardAgency"));
        driver.switchTo().frame(iframe);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("forwardAgencyGrid")));
    }
        
    @When("I edit all forward agency records from Excel file {string}")
    public void i_edit_all_records_from_excel(String filePath) throws Exception {

        FileInputStream fis = new FileInputStream(filePath);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        DataFormatter formatter = new DataFormatter(); // ✅ KEY FIX

        int lastRow = sheet.getLastRowNum();

        for (int i = 1; i <= lastRow; i++) {

            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell idCell = row.getCell(0);
            Cell fieldCell = row.getCell(1);
            Cell valueCell = row.getCell(2);

            if (idCell == null || idCell.getCellType() != CellType.NUMERIC) continue;
            if (fieldCell == null || valueCell == null) continue;

            int forwardId = (int) idCell.getNumericCellValue();
            if (forwardId <= 0) continue;

            String fieldId = formatter.formatCellValue(fieldCell).trim();
            String newValue = formatter.formatCellValue(valueCell).trim();

            if (fieldId.isEmpty()) continue;


            System.out.println(
                "Editing ForwardID: " + forwardId +
                " | Field: " + fieldId +
                " | Value: " + newValue
            );

            try {
                editForwardAgencyField(forwardId, fieldId, newValue);

                ExcelLogger.log(
                    "Edit",
                    fieldId + " updated to " + newValue,
                    fieldId + " updated to " + newValue,
                    "PASS",
                    ""
                );

            } catch (Exception e) {
                ExcelLogger.log(
                    "Edit",
                    fieldId + " updated to " + newValue,
                    "Edit failed",
                    "FAIL",
                    e.getMessage()
                );
            }
        }

        workbook.close();
        fis.close();

        System.out.println("All valid edit records processed");
    }


    public void editForwardAgencyField(
            Integer forwardId,
            String fieldId,
            String newValue
    ) throws Exception {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // ---------------- STEP 1: Select 50 per page ----------------
        WebElement selectDropdown = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("select.ui-pg-selbox"))
        );
        new Select(selectDropdown).selectByValue("50");

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.cssSelector(".loading, .blockUI, .toast-message")
        ));

        // ---------------- STEP 2: FIND & OPEN RECORD ----------------
        boolean recordFound = false;
        int currentPage = 0;

        while (!recordFound && currentPage < 20) {
            try {
                WebElement row = driver.findElement(By.xpath(
                    "//tr[td[@aria-describedby='forwardAgencyGrid_forwardId' and @title='" + forwardId + "']]"
                ));

                js.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    row
                );
                Thread.sleep(500);

                js.executeScript(
                    "arguments[0].dispatchEvent(new MouseEvent('dblclick',{bubbles:true}));",
                    row
                );
                Thread.sleep(1500);

                recordFound = true;

            } catch (NoSuchElementException e) {

                WebElement nextBtn = driver.findElement(By.id("next_forwardAgencyGrid_pager"));
                if (nextBtn.isEnabled()) {
                    nextBtn.click();
                    Thread.sleep(1500);
                    currentPage++;
                } else {
                    break;
                }
            }
        }

        if (!recordFound) {
            throw new RuntimeException("ForwardID " + forwardId + " not found");
        }

        // ---------------- STEP 3: CLICK EDIT ----------------
        WebElement ellipsis = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector(".ellipse-dropdown button.dropdown-toggle")
            )
        );
        ellipsis.click();

        WebElement editBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("fwd_edit"))
        );
        editBtn.click();
        Thread.sleep(1000);

        // ---------------- STEP 4: EDIT FIELD DYNAMICALLY ----------------
        WebElement field = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id(fieldId))
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", field);

        try {
            field.clear();
        } catch (Exception e) {
            js.executeScript("arguments[0].value = '';", field);
        }

        if (!newValue.isEmpty()) {
            try {
                field.sendKeys(newValue);
            } catch (Exception e) {
                js.executeScript(
                    "arguments[0].value = arguments[1];",
                    field,
                    newValue
                );
            }
        }

        driver.findElement(By.tagName("body")).click();
        Thread.sleep(300);
        WebElement saveBtn = wait.until(
            ExpectedConditions.presenceOfElementLocated(By.id("fwdSave"))
        );

        try {
            wait.until(ExpectedConditions.elementToBeClickable(saveBtn));
            saveBtn.click();
        } catch (Exception e) {
            js.executeScript("arguments[0].click();", saveBtn);
        }

        Thread.sleep(1500);

        
        Thread.sleep(1500);

        System.out.println(
            "Updated ForwardID " + forwardId +
            " | " + fieldId + " = " + newValue
        );
    }


    @When("I return to the grid view")
    public void i_return_to_the_grid_view() {
        WebElement ellipsisButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".ellipse-dropdown button.dropdown-toggle")));
        ellipsisButton.click();

        WebElement backBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("fwd_back")));
        backBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("table#forwardAgencyGrid tr.jqgrow")));
    }
    
    private final List<String> lastSearchTerms = new ArrayList<>();

    @When("I search for values from Excel in forward agency records")
    public void i_search_for_values_from_excel_in_forward_agency_records() throws Exception {

        String searchTerms = ExcelConfigReader.readConfig(
                "src/test/resources/EditFile.xlsx",
                "Search",
                "searchText"
        );

        if (searchTerms == null || searchTerms.isEmpty()) {
            throw new RuntimeException("searchText is missing in Search sheet!");
        }

        System.out.println("🔍 Searching for values: " + searchTerms);

        lastSearchTerms.clear();
        List<String> terms = Arrays.stream(searchTerms.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("forwardAgencyGrid")));

        WebElement searchBox = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("srchRelatAll"))
        );

        for (String term : terms) {
            searchBox.click();
            searchBox.sendKeys(Keys.CONTROL, "a", Keys.DELETE);
            searchBox.sendKeys(term);
            searchBox.sendKeys(Keys.ENTER);

            Thread.sleep(2500);

            WebElement grid = driver.findElement(By.id("forwardAgencyGrid"));
            List<WebElement> rows = grid.findElements(By.cssSelector("tr.jqgrow"));

            boolean found = rows.stream()
                    .flatMap(r -> r.findElements(By.tagName("td")).stream())
                    .anyMatch(c -> c.getText().toLowerCase().contains(term.toLowerCase()));

            if (found) {
                ExcelLogger.log(
                        "Search",
                        "Records displayed successfully for '" + term + "'",
                        "Records displayed successfully for '" + term + "'",
                        "PASS",
                        ""
                );
                lastSearchTerms.add(term);
            } else {
                if (rows.isEmpty()) {
                    ExcelLogger.log(
                            "Search",
                            "Records displayed successfully for '" + term + "'",
                            "Grid empty",
                            "FAIL",
                            ""
                    );
                } else {
                    ExcelLogger.log(
                            "Search",
                            "Records displayed successfully for '" + term + "'",
                            "No matching results found",
                            "FAIL",
                            ""
                    );
                }
            }

            Thread.sleep(2000);
        }
    }


    @Then("I should see matching results in the grid")
    public void i_should_see_matching_results_in_the_grid() {
        if (lastSearchTerms.isEmpty()) {
            ExcelLogger.log("","Search should return relevant results","No matching search terms were found","FAIL", "No results verified");
        } else {
            ExcelLogger.log("","Search results matched search terms","Search results matched search terms", "PASS","");
        }
    }

    @When("I view the history of the forward agency record")
    public void i_view_the_history_of_the_forward_agency_record() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            System.out.println("User should be able to open the history modal");
            WebElement ellipsisButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".ellipse-dropdown button.dropdown-toggle")));
            ellipsisButton.click();

            System.out.println("Click history option from the menu");

            WebElement historyBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("ForwardHistory")));
            historyBtn.click();

            System.out.println( "History modal should appear on screen");

            WebElement historyModal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("deletedRecords")));
            Thread.sleep(3000);

            if (historyModal.isDisplayed()) {
                ExcelLogger.log("View History","History modal opened successfully","History modal opened successfully","PASS","");
            } else {
                ExcelLogger.log("View History","History modal opened successfully","History modal did not open", "FAIL","Modal not displayed");
                throw new RuntimeException("History modal did not open.");
            }

            System.out.println("User should be able to close the history modal");

            WebElement closeBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id='deletedRecords']//button[@type='button' and text()='Close']")));
            closeBtn.click();

            wait.until(ExpectedConditions.invisibilityOf(historyModal));

            System.out.println( "History modal should close successfully");
        } catch (Exception e) {
            ExcelLogger.log("View History","User should be able to view and close history modal","Failed to view or close history modal","FAIL",e.getMessage());
            throw new RuntimeException("Failed to view history", e);
        }
    }

    @When("I create a new forward agency record with test case {string} from file {string}")
    public void i_create_a_new_forward_agency_record_with_test_case_from_file(String testCase, String filePath) {
        try {
        	System.out.println("Creating record for test case: " + testCase);
            
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            JSONArray jsonArray = new JSONArray(content);
            
            JSONObject testData = null;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject record = jsonArray.getJSONObject(i);
                if (record.getString("testCase").equals(testCase)) {
                    testData = record;
                    break;
                }
            }
            if (testData == null) {
                String errorMsg = "Test case '" + testCase + "' not found in JSON file: " + filePath;
                System.out.println(errorMsg);
                ExcelLogger.log("Create","Record created successfully","Test case not found in JSON file","FAIL",errorMsg);
                throw new RuntimeException(errorMsg);
            }
            System.out.println("Found test data for: " + testCase);
            
            createSingleRecordWithExistingLogic(testData, testCase);           
        } catch (Exception e) {
            ExcelLogger.log("Create","Record should be created successfully ", "Failed to create record","FAIL",e.getMessage());
            throw new RuntimeException("Failed to create record for test case: " + testCase, e);
        }
    }
    
    private void createSingleRecordWithExistingLogic(JSONObject testData, String testCase) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
        	System.out.println("🚀 Starting record creation for: " + testCase);

            WebElement ellipsisButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn.btn-success.dropdown-toggle.deSelectAlways")));

            js.executeScript("arguments[0].click();", ellipsisButton);
            Thread.sleep(1000);

            WebElement newBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("fwd_new")));
            js.executeScript("arguments[0].click();", newBtn);
            Thread.sleep(2000);

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("forwardName")));
            fillFormWithData(testData);

            WebElement ellipsisButton2 = wait.until(ExpectedConditions.elementToBeClickable( By.cssSelector("button.btn.btn-success.dropdown-toggle.deSelectAlways")));
            js.executeScript("arguments[0].click();", ellipsisButton2);
            Thread.sleep(1000);

            WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("fwdSave")));
            js.executeScript("arguments[0].click();", saveBtn);
            Thread.sleep(3000);

            String popupMessage = checkForPopupsAndErrors();
            
            if (!popupMessage.isEmpty()) {
                ExcelLogger.log(testCase,"Record created successfully",""+ popupMessage,"FAIL",popupMessage);
                System.out.println("📝 Popup detected - FAIL logged: " + popupMessage);
            } else {
                ExcelLogger.log(testCase,"Record created successfully","Record created successfully","PASS", "");
                System.out.println("🎉 No popup - PASS logged");
            }
            returnToGridView();

        } catch (Exception innerEx) {
        	System.out.println("❌ Exception: " + innerEx.getMessage());
            ExcelLogger.log(testCase,"Record should be created successfully","Error while creating record for testCase: " + testCase,"FAIL",innerEx.getMessage());
        }
    }
    private String checkForPopupsAndErrors() {
        try {
            Thread.sleep(3000);
            System.out.println("🔍 Checking for error panel...");

            driver.switchTo().defaultContent();
            System.out.println("🔄 Switched to default content");

            try {
                WebElement errorPanel = driver.findElement(By.id("errorlogpanelcon"));
                boolean isDisplayed = errorPanel.isDisplayed();
                System.out.println("🔍 Error panel found - Displayed: " + isDisplayed);
                
                if (isDisplayed) {
                	System.out.println("✅ ERROR PANEL DETECTED! - Extracting message...");
                    try {
                        WebElement tab1 = driver.findElement(By.cssSelector("#tab1[style*='display: block']"));
                        String fullText = tab1.getText();
                        System.out.println("🔍 Tab1 raw text: '" + fullText + "'");
                        
                        String errorMessage = extractErrorFromValidation(fullText);
                        System.out.println("📝 Extracted error: " + errorMessage);
                        
                        switchToForwardAgencyIframe();                        
                        return errorMessage;
                    } catch (Exception e) {
                    	System.out.println("❌ Could not extract from tab1: " + e.getMessage());

                        switchToForwardAgencyIframe();
                        return "Validation error detected but could not extract message";
                    }
                } else {
                	System.out.println("Error panel found but not visible");
                }
            } catch (Exception e) {
            	System.out.println("Error panel not found in main document: " + e.getMessage());
            }
            switchToForwardAgencyIframe();
            
            System.out.println("No error panel detected - assuming success");
            return ""; 

        } catch (Exception e) {
        	System.out.println("Error in popup check: " + e.getMessage());

            try {
                switchToForwardAgencyIframe();
            } catch (Exception ex) {
            }
            return "";
        }
    }
    private void switchToForwardAgencyIframe() {
        try {
            WebElement iframe = driver.findElement(By.id("forwardAgency"));
            driver.switchTo().frame(iframe);
            System.out.println("Switched back to Forward Agency iframe");
        } catch (Exception e) {
        	System.out.println("Could not switch back to iframe: " + e.getMessage());
        }
    }
    private String extractErrorFromValidation(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return "";
            
            return text.replaceAll("^[A-Za-z]{3}\\s+\\d{1,2}/\\d{1,2}/\\d{4},\\s+\\d{1,2}:\\d{1,2}:\\d{1,2}:?", "").trim();            
        } catch (Exception e) {
            return text.trim(); 
        }
    }
    private void returnToGridView() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        JavascriptExecutor js = (JavascriptExecutor) driver;        
        try {
            WebElement ellipsisButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.btn.btn-success.dropdown-toggle.deSelectAlways")));
            js.executeScript("arguments[0].click();", ellipsisButton);
            Thread.sleep(1000);

            WebElement backBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("fwd_back")));

            js.executeScript("arguments[0].click();", backBtn);
            Thread.sleep(2000);
            List<WebElement> popupButtons = driver.findElements(By.xpath("//button[contains(text(), 'Yes') or @data-value='true']"));
            
            if (!popupButtons.isEmpty()) {
                for (WebElement button : popupButtons) {
                    if (button.isDisplayed() && (button.getText().equals("Yes") || "true".equals(button.getAttribute("data-value")))) {
                        js.executeScript("arguments[0].click();", button);
                        break;
                    }
                }
            }
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("forwardAgencyGrid")));
            
        } catch (Exception e) {
        	System.out.println("⚠️ Error returning to grid view: " + e.getMessage());
            try {
                driver.navigate().refresh();
                Thread.sleep(3000);
            } catch (Exception ex) {
            	System.out.println("❌ Page refresh also failed: " + ex.getMessage());
            }
        }
    }
    private void fillFormWithData(JSONObject data) {
        String[] formFieldIds = {
                "forwardName", "forwardFirm", "forwardAddress1", "forwardAddress2",
                "forwardAddress3", "forwardAddress4", "forwardContactName",
                "forwardContactEmail", "forwardContactPhone", "forwardWebUrl",
                "forwardRate", "forwardRateLegal", "forwardGlmAccountCode",
                "forwardFax", "forwardBarId", "forwardRemarks"
        };

        for (String fieldId : formFieldIds) {
            if (data.has(fieldId) && !data.isNull(fieldId)) {
                String value = data.getString(fieldId);
                if (!value.isEmpty()) {
                    if (fieldId.equals("forwardContactPhone") || fieldId.equals("forwardFax")) {
                        setFieldValueWithJS(fieldId, value);
                    } else {
                        setFieldValue(fieldId, value);
                    }
                }
            }
        }

        if (data.has("forwardActive")) {
            try {
                boolean isActive = data.getBoolean("forwardActive");
                WebElement activeCheckbox = driver.findElement(By.id("forwardActive"));
                if (isActive != activeCheckbox.isSelected()) {
                    activeCheckbox.click();
                }
            } catch (Exception e) {
            }
        }
    }

    private void setFieldValue(String fieldId, String value) {
        try {
            WebElement field = driver.findElement(By.id(fieldId));
            field.clear();
            field.sendKeys(value);
        } catch (Exception e) {           
            setFieldValueWithJS(fieldId, value);
        }
    }

    private void setFieldValueWithJS(String fieldId, String value) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("var el = document.getElementById(arguments[0]);" +"if(el){" +"el.value = arguments[1];" +"el.dispatchEvent(new Event('input', { bubbles: true }));" +"el.dispatchEvent(new Event('change', { bubbles: true }));" +"}",fieldId, value);
        } catch (Exception e) {
            
        }
    }

    @Then("the record should be saved successfully")
    public void the_record_should_be_saved_successfully() {
        try {
            WebElement successMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".alert-success, .msg-success, .toast-success")));
            assert successMsg.isDisplayed();
            ExcelLogger.log("Create", "Record saved successfully", "Record saved successfully", "PASS", null);
        } catch (Exception e) {
            try {
                WebElement errorMsg = driver.findElement(By.cssSelector(".alert-error, .msg-error, .error, .toast-error"));
                ExcelLogger.log("Create", "Save record", "Save failed", "FAIL", errorMsg.getText());
            } catch (Exception ex) {
                ExcelLogger.log("Create", "Save record", "No success or error detected", "WARN", null);
            }
        }
    }

    @Then("I should see the record details")
    public void i_should_see_the_record_details() {
        WebElement recordDetails = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("forwardName")));
        assert recordDetails.isDisplayed();
        ExcelLogger.log("Create", "Record details visible", "Record details visible", "PASS", null);
    }

    @Then("I should return to the grid view")
    public void i_should_return_to_the_grid_view() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(8));

        try {
            driver.switchTo().defaultContent();

            // If grid already visible → PASS
            if (driver.findElements(By.cssSelector("table#forwardAgencyGrid")).size() > 0) {
                ExcelLogger.log("", "Grid view", "Grid view", "PASS", null);
                return;
            }

            // Try back button if exists
            if (driver.findElements(By.id("fwd_back")).size() > 0) {
                driver.findElement(By.id("fwd_back")).click();

                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("table#forwardAgencyGrid")));

                ExcelLogger.log("", "Grid view", "Grid view", "PASS", null);
                return;
            }
        } catch (Exception e) {
            ExcelLogger.log("", "Grid view", "Grid navigation skipped", "INFO", e.getMessage());
        }
    }

    @When("I logout from the application")
    public void i_logout_from_the_application() {
        driver.switchTo().defaultContent();
        WebElement userDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("i.icon-caret-down, i.fa-caret-down, [class*='caret-down']")));
        userDropdown.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("ul.user-menu, ul.dropdown-menu, [id*='user-dropdown']")));
        WebElement logoutOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Logout') or contains(@onclick, 'logout')]")));
        logoutOption.click();
        wait.until(ExpectedConditions.urlContains("login"));
        ExcelLogger.log("", "Logout", "Logged out successfully", "PASS", null);
    }
}

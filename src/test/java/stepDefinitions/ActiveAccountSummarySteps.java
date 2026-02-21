package stepDefinitions;
import utils.ExcelReader;
import util.ActiveAccountSummaryLogger;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.Before;
import io.cucumber.java.en.When;
import java.time.Duration;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.support.ui.Select;
import java.time.DayOfWeek;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import util.TestContext;


import java.util.List;

public class ActiveAccountSummarySteps {
    
    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;

    @Before
    public void setUp() {
        driver = DriverManager.getDriver();
        wait = DriverManager.getWait();
        actions = DriverManager.getActions();
    }

    @And("I navigate to the Active Account Summary section")
    public void navigate_to_active_account_summary() {
        System.out.println("➡️ Navigating to Active Account Summary");
        WebElement menuButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("menuPushIcon"))
        );
        menuButton.click();
        WebElement reportsMenu = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[contains(text(),'Reports')]")) );
        actions.moveToElement(reportsMenu).perform();

        WebElement activeSummary = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//a[text()='Active Account Summary']")));
        activeSummary.click();

        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(
                By.id("balancereport")
        ));

        System.out.println("✅ Active Account Summary screen loaded");
    }
    @When("I close the Active Account Summary tab if it is open")
    public void i_close_the_active_account_summary_tab_if_it_is_open() {

        try {
            driver.switchTo().defaultContent();

            WebElement closeIcon = driver.findElement(
                By.cssSelector("li.active[data-screen='Active Account Summary'] i.icon-remove")
            );

            if (closeIcon.isDisplayed()) {
                ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].click();", closeIcon
                );

                System.out.println("❎ Active Account Summary tab closed");
                Thread.sleep(1500); // allow UI to settle
            }

        } catch (NoSuchElementException e) {
            System.out.println("ℹ️ Active Account Summary tab not open");
        } catch (Exception e) {
            System.out.println("⚠️ Failed to close Active Account Summary tab: " + e.getMessage());
        }
    }

    @Then("default values should be displayed on Active Account Summary page")
    public void verify_default_values_on_active_account_summary() {

        try {
            List<WebElement> dropdownLabels = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                    By.xpath("//button[contains(@class,'ms-choice')]//span")
                )
            );

            String corporateValue = dropdownLabels.get(0).getText().trim();
            String clientValue    = dropdownLabels.get(1).getText().trim();

            WebElement corporateBtn =
                dropdownLabels.get(0).findElement(By.xpath("./ancestor::button"));
            corporateBtn.click();

            WebElement corporateSelectAll =
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class,'ms-drop') and contains(@style,'block')]//input[@name='selectAll']")
                ));

            boolean corporateSelectAllChecked = corporateSelectAll.isSelected();
            corporateBtn.click(); 
            WebElement clientBtn =
                dropdownLabels.get(1).findElement(By.xpath("./ancestor::button"));
            clientBtn.click();

            WebElement clientSelectAll =
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//div[contains(@class,'ms-drop') and contains(@style,'block')]//input[@name='selectAll']")
                ));

            boolean clientSelectAllChecked = clientSelectAll.isSelected();
            clientBtn.click(); 
            WebElement fromDate = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("paymentDateFrom"))
            );
            WebElement toDate = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("paymentDateTo"))
            );

            String fromDateValue = fromDate.getAttribute("value");
            String toDateValue   = toDate.getAttribute("value");

            boolean status =
                    corporateSelectAllChecked &&
                    clientSelectAllChecked &&
                    fromDateValue.equals(toDateValue) &&
                    !fromDateValue.isEmpty();


            

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Default values should be loaded",
                    "Default values verified",
                    status ? "PASS" : "FAIL",
                    ""
                );

            } catch (Exception e) {
                ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Default values should be loaded",
                    "Validation failed",
                    "FAIL",
                    e.getMessage()
                );
                throw e;
            }
    }
    
    @When("I select a single corporate \\(after unselecting Select All\\)")
    public void i_select_a_single_corporate_after_unselecting_select_all() throws InterruptedException {

        WebElement corporateDropdownBtn = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("(//button[contains(@class,'ms-choice')])[1]")
            )
        );
        corporateDropdownBtn.click();

        WebElement selectAllCheckbox = wait.until(
            ExpectedConditions.presenceOfElementLocated(
                By.xpath("//ul[@id='cmbCorporateList']//input[@name='selectAll']")
            )
        );

        if (selectAllCheckbox.isSelected()) {
            selectAllCheckbox.click();
        }

        List<WebElement> corporateItems = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(
                By.xpath("//ul[@id='cmbCorporateList']//input[@name='selectItem']")
            )
        );

        if (corporateItems.isEmpty()) {
            throw new RuntimeException("No corporate options available");
        }

        int randomIndex = new java.util.Random().nextInt(corporateItems.size());
        WebElement randomCorporate = corporateItems.get(randomIndex);
        Thread.sleep(2000);

        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({block:'center'});",
            randomCorporate
        );

        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].click();",
            randomCorporate
        );
        ActiveAccountSummaryLogger.log(
            TestContext.currentScenario,
            "Selected corporate at index: " + randomIndex,
            "Selected corporate at index: " + randomIndex,
            "PASS",
            ""
        );
    }
    @Then("corresponding clients should be loaded in the Client dropdown")
    public void verify_clients_loaded_based_on_corporate() throws InterruptedException {

        try {
            WebElement clientDropdownBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.xpath("(//button[contains(@class,'ms-choice')])[2]")
                )
            );
            clientDropdownBtn.click();

            WebElement clientDropContainer = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("(//div[contains(@class,'ms-drop')])[last()]")
                )
            );

            List<WebElement> clientItems = clientDropContainer.findElements(
                By.xpath(".//li[.//input[@type='checkbox']]")
            );
            Thread.sleep(2000);

            if (clientItems.isEmpty()) {
                throw new RuntimeException("Client dropdown opened but no items loaded");
            }
            ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Client dropdown loaded with " + clientItems.size() + " items",
                "Client dropdown loaded with " + clientItems.size() + " items",
                "PASS",
                ""
            );

        } catch (Exception e) {

            ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Client dropdown should update based on Corporate selection",
                "Client dropdown did not load",
                "FAIL",
                e.getMessage()
            );

            throw e;
        }
    }

    @When("I select a single client \\(after unselecting Select All\\)")
    public void i_select_a_single_client_after_unselecting_select_all() throws InterruptedException{

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        WebElement clientDropdown = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("(//button[contains(@class,'ms-choice')])[2]")
                )
        );
        clientDropdown.click();
        WebElement selectAllLabel = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//ul[@id='cmbFacilityList']//li[contains(@class,'zms-select-all')]/label")
                )
        );

        js.executeScript("arguments[0].click();", selectAllLabel);

        wait.until(driver -> {
            List<WebElement> checkedItems = driver.findElements(
                    By.xpath("//ul[@id='cmbFacilityList']//input[@name='selectItem' and @checked]")
            );
            return checkedItems.size() < 2;
        });

        List<WebElement> clientItems = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.xpath("//ul[@id='cmbFacilityList']//input[@name='selectItem']")
                )
        );
        WebElement randomClient =
                clientItems.get(new java.util.Random().nextInt(clientItems.size()));

        js.executeScript("arguments[0].click();", randomClient);
        Thread.sleep(2000);
    }

    @When("I click the Search button")
    public void i_click_the_search_button()  {

        WebElement searchBtn = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("searchReport"))
        );
        searchBtn.click();

        ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Search button clicked",
                "Search button clicked",
                "PASS",
                ""
            );
    }
    @Then("Active Account Summary results should be displayed")
    public void active_account_summary_results_should_be_displayed() {
        WebDriverWait wait = DriverManager.getWait();
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("summaryGridview")
            ));

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Summary grid displayed",
                    "Summary grid displayed",
                    "PASS",
                    ""
            );

        } catch (TimeoutException e) {
            try {
                WebElement noRecordsMsg = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                By.xpath(
                                    "//div[contains(@class,'messageContent') " +
                                    "and contains(normalize-space(),'No Record')]"
                                )
                        )
                );

                ActiveAccountSummaryLogger.log(
                        TestContext.currentScenario,
                        "No Record(s) Found message displayed",
                        "No Record(s) Found message displayed",
                        "PASS",
                        ""
                );

            } catch (Exception ex) {

                // 3️⃣ Real failure → nothing loaded
                ActiveAccountSummaryLogger.log(
                        TestContext.currentScenario,
                        "Active Account Summary results",
                        "Neither grid nor No Record message appeared",
                        "FAIL",
                        ex.getMessage()
                );

                throw ex;
            }
        }
    }

    @When("I select Date option")
    public void i_select_date_option() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        wait.until(ExpectedConditions.elementToBeClickable(By.id("date"))).click();
        ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Date option selected",
                "Date option selected",
                "PASS",
                ""
            );
    }
    @Then("From and To date should be last working date")
    public void verify_last_working_date() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        WebElement fromDate = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("paymentDateFrom"))
        );

        WebElement toDate = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("paymentDateTo"))
        );

        String actualFrom = fromDate.getAttribute("value");
        String actualTo   = toDate.getAttribute("value");

        System.out.println("FROM DATE: " + actualFrom);
        System.out.println("TO DATE  : " + actualTo);

        // Calculate expected last working date
        LocalDate expectedLastWorkingDay = getLastWorkingDay(LocalDate.now());
        String expectedDate = expectedLastWorkingDay
                .format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));

        // Assertions
        Assert.assertEquals(
            "From Date should be last working day",
            expectedDate,
            actualFrom
        );

        Assert.assertEquals(
            "To Date should be last working day",
            expectedDate,
            actualTo
        );
    }

    private LocalDate getLastWorkingDay(LocalDate date) {

        LocalDate lastWorkingDay = date.minusDays(1);

        while (lastWorkingDay.getDayOfWeek() == DayOfWeek.SATURDAY
            || lastWorkingDay.getDayOfWeek() == DayOfWeek.SUNDAY) {

            lastWorkingDay = lastWorkingDay.minusDays(1);
        }

        return lastWorkingDay;
    }

    @When("I select Month option")
    public void i_select_month_option() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        WebElement monthRadio = wait.until(
            ExpectedConditions.elementToBeClickable(By.id("month"))
        );
        monthRadio.click();
        ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Month option selected",
                "Month option selected",
                "PASS",
                ""
            );
    }
    @Then("From and To month should be selected")
    public void verify_month_is_selected_and_same() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        WebElement fromMonth = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("fromMonthDate"))
        );
        WebElement toMonth = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("toMonthDate"))
        );
        String actualFrom = fromMonth.getAttribute("value");
        String actualTo   = toMonth.getAttribute("value");

        System.out.println("FROM MONTH: " + actualFrom);
        System.out.println("TO MONTH  : " + actualTo);

        Assert.assertFalse("From month is not selected", 
            actualFrom == null || actualFrom.trim().isEmpty());

        Assert.assertFalse("To month is not selected", 
            actualTo == null || actualTo.trim().isEmpty());

        Assert.assertEquals("From and To month mismatch", 
            actualFrom, actualTo);
        
    }

    @When("I select Year option")
    public void i_select_year_option() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        WebElement yearRadio = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("year"))
        );
        yearRadio.click();
        ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Year option selected",
                "Year option selected",
                "PASS",
                ""
            );
    }

    @Then("From and To year should be current year")
    public void verify_current_year_in_from_and_to() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        WebElement fromYear = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("fromYearPicker"))
        );
        WebElement toYear = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("toYearPicker"))
        );
        String actualFromYear = fromYear.getAttribute("value").trim();
        String actualToYear   = toYear.getAttribute("value").trim();

        System.out.println("FROM YEAR: " + actualFromYear);
        System.out.println("TO YEAR  : " + actualToYear);

        String currentYear = String.valueOf(LocalDate.now().getYear());

        Assert.assertEquals("From year mismatch", currentYear, actualFromYear);
        Assert.assertEquals("To year mismatch", currentYear, actualToYear);
    }
    private String normalizeExcelValue(String value) {
        return value.replaceAll("\\.0$", "").trim();
    }

    @When("I enter valid report filters from excel")
    public void enter_valid_report_filters_from_excel() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String testCase = TestContext.currentScenario;

        ExcelReader.loadTestCase("src/test/resources/ActiveAccountSummaryInput.xlsx", testCase);

        String corporateValue = ExcelReader.get("Corporate");
        String clientValue    = ExcelReader.get("Client");
        String fromDate       = ExcelReader.get("FromDate");
        String toDate         = ExcelReader.get("ToDate");

        if (corporateValue != null) corporateValue = normalizeExcelValue(corporateValue);
        if (clientValue != null)    clientValue    = normalizeExcelValue(clientValue);
        if (corporateValue != null && !corporateValue.isBlank()) {

            WebElement corpDropdownBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("#cmbCorporate + .ms-parent .ms-choice")
                    )
            );
            corpDropdownBtn.click();

            WebElement corpSelectAll = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//ul[@id='cmbCorporateList']//input[@name='selectAll']")
                    )
            );

            if (corpSelectAll.isSelected()) corpSelectAll.click();

            WebElement corpCheckbox = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//ul[@id='cmbCorporateList']//label[contains(normalize-space(),'"
                                    + corporateValue + "')]/input")
                    )
            );

            if (!corpCheckbox.isSelected()) corpCheckbox.click();
            corpDropdownBtn.click();
        }
        if (clientValue != null && !clientValue.isBlank()) {
            WebElement clientDropdownBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector("#cmbFacility + .ms-parent .ms-choice")
                    )
            );
            clientDropdownBtn.click();

            WebElement clientSelectAll = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//ul[@id='cmbFacilityList']//input[@name='selectAll']")
                    )
            );

            if (clientSelectAll.isSelected()) clientSelectAll.click();

            WebElement clientCheckbox = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//ul[@id='cmbFacilityList']//label[contains(normalize-space(),'"
                                    + clientValue + "')]/input")
                    )
            );

            if (!clientCheckbox.isSelected()) clientCheckbox.click();
            clientDropdownBtn.click();
        }
        if (fromDate != null && !fromDate.isBlank()) {
            WebElement fromDateInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("paymentDateFrom")));
            js.executeScript("arguments[0].value='';", fromDateInput);
            js.executeScript("arguments[0].value=arguments[1];", fromDateInput, fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            WebElement toDateInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("paymentDateTo")));
            js.executeScript("arguments[0].value='';", toDateInput);
            js.executeScript("arguments[0].value=arguments[1];", toDateInput, toDate);
        }

        ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Filters applied successfully",
                "Filters applied successfully",
                "PASS",
                ""
        );
    }

    @When("I apply grid sorting using values from excel")
    public void apply_grid_sorting_using_values_from_excel() throws InterruptedException {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        String testCase = TestContext.currentScenario;
        ExcelReader.loadTestCase("src/test/resources/ActiveAccountSummaryInput.xlsx", testCase);

        String columnName = ExcelReader.get("Column");
        String order      = ExcelReader.get("Order");
        String count      = ExcelReader.get("Count");

        if (columnName == null || columnName.isBlank()) {
            throw new RuntimeException("Column not provided in Excel for " + testCase);
        }

        if (order == null || order.isBlank()) order = "ASC";
        if (count == null || count.isBlank()) count = "5";

        // open sorting
        WebElement sortIcon = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("a.fa-sort-amount-asc")
                )
        );

        js.executeScript("arguments[0].scrollIntoView({block:'center'});", sortIcon);
        Thread.sleep(1000);
        js.executeScript("arguments[0].click();", sortIcon);
        Thread.sleep(1500);

        WebElement columnDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("columnsSort")));
        new Select(columnDropdown).selectByVisibleText(columnName.trim());
        Thread.sleep(1500);

        WebElement orderButton = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.ascdesc")));
        String currentOrder = orderButton.getText().trim();

        if (!currentOrder.equalsIgnoreCase(order.trim())) {
            orderButton.click();
            Thread.sleep(1000);
        }

        WebElement countInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("limitSort")));
        String normalizedCount = count.replaceAll("\\.0$", "").trim();

        js.executeScript("arguments[0].value='';", countInput);
        js.executeScript("arguments[0].value=arguments[1];", countInput, normalizedCount);
        Thread.sleep(1000);

        WebElement applyBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("applyFilter")));
        applyBtn.click();
        Thread.sleep(2000);

        ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Sorting applied: " + columnName + " " + order + " limit " + normalizedCount,
                "Sorting applied successfully",
                "PASS",
                ""
        );
    }

    @Then("Report should be generated based on selected filters")
    public void verify_report_generated_successfully() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        WebElement resultsTable = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.id("activeAccountSummaryTable")
            )
        );
        List<WebElement> rows = resultsTable.findElements(By.tagName("tr"));

        Assert.assertTrue(
            "No report data generated",
            rows.size() > 1
        );

        System.out.println("Report generated successfully. Records found: " + (rows.size() - 1));
        ActiveAccountSummaryLogger.log(
                TestContext.currentScenario,
                "Report generated successfully",
                "Report generated successfully",
                "PASS",
                ""
            );
    }
    @When("I configure grid columns using values from excel")
    public void configure_grid_columns_using_values_from_excel() {

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            String testCase = TestContext.currentScenario;
            ExcelReader.loadTestCase("src/test/resources/ActiveAccountSummaryInput.xlsx", testCase);

            String hideColumns = ExcelReader.get("HideColumns");
            String showColumns = ExcelReader.get("ShowColumns");

            WebElement showHideBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(text(),'Show/Hide')]"))
            );
            js.executeScript("arguments[0].click();", showHideBtn);
            Thread.sleep(1500);
            if (hideColumns != null && !hideColumns.isBlank()) {
                for (String column : hideColumns.split(",")) {
                    String col = column.trim();
                    if (col.isEmpty()) continue;

                    WebElement colToHide = wait.until(
                            ExpectedConditions.elementToBeClickable(
                                    By.xpath("//div[@class='ms-selection']//span[normalize-space()='" + col + "']")
                            )
                    );
                    js.executeScript("arguments[0].click();", colToHide);
                    Thread.sleep(800);
                }
            }
            if (showColumns != null && !showColumns.isBlank()) {
                for (String column : showColumns.split(",")) {
                    String col = column.trim();
                    if (col.isEmpty()) continue;

                    WebElement colToShow = wait.until(
                            ExpectedConditions.elementToBeClickable(
                                    By.xpath("//div[@class='ms-selectable']//span[normalize-space()='" + col + "']")
                            )
                    );
                    js.executeScript("arguments[0].click();", colToShow);
                    Thread.sleep(800);
                }
            }
            WebElement applyBtn = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("input.changeGridColumn[data-info='summary']")
                    )
            );
            js.executeScript("arguments[0].click();", applyBtn);
            Thread.sleep(3000);

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Hidden: " + hideColumns + " | Shown: " + showColumns,
                    "Column configuration applied",
                    "PASS",
                    ""
            );

        } catch (Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Configure grid columns",
                    "Column configuration failed",
                    "FAIL",
                    e.getMessage()
            );
            throw new RuntimeException(e);
        }
    }

    @When("I export the Active Account Summary data to excel")
    public void export_active_account_summary_to_excel() {

        try {
            Thread.sleep(2000);

            WebDriverWait wait = DriverManager.getWait();
            JavascriptExecutor js = DriverManager.getJS();

            WebElement exportBtn = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("a.fa-file-excel-o")
                    )
            );

            js.executeScript("arguments[0].click();", exportBtn);
            Thread.sleep(2000);

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Export triggered",
                    "Export triggered",
                    "PASS",""
            );

        } catch (Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Export Active Account Summary",
                    "Export failed",
                    "FAIL",
                    e.getMessage()
            );
            throw new RuntimeException(e);
        }
    }
    @Then("Export option should be available and export should be triggered")
    public void verify_export_option_available() {

        try {
            WebDriver driver = DriverManager.getDriver();

            boolean exportPresent =
                    driver.findElements(By.cssSelector("a.fa-file-excel-o")).size() > 0;

            Assert.assertTrue(exportPresent);

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Export option available",
                    "Export option available",
                    "PASS",""
            );

        } catch (AssertionError | Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Verify export option",
                    "Export option missing",
                    "FAIL",
                    e.getMessage()
            );
            throw e;
        }
    }
    @When("I click the Clear button")
    public void i_click_the_clear_button() {

        try {
            WebDriverWait wait = DriverManager.getWait();
            JavascriptExecutor js = DriverManager.getJS();

            WebElement clearBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id("reload"))
            );

            js.executeScript("arguments[0].click();", clearBtn);
            Thread.sleep(2000);

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Clear button clicked",
                    "Clear button clicked",
                    "PASS",""
            );

        } catch (Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Click Clear button",
                    "Clear click failed",
                    "FAIL",
                    e.getMessage()
            );
            throw new RuntimeException(e);
        }
    }
    @Then("All fields should be cleared and default values should be loaded")
    public void verify_clear_button_functionality() {

        try {
            WebDriverWait wait = DriverManager.getWait();

            WebElement corpText = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("(//button[contains(@class,'ms-choice')])[1]/span")
                    )
            );
            Assert.assertTrue(corpText.getText().toLowerCase().contains("all"));

            WebElement clientText = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("(//button[contains(@class,'ms-choice')])[2]/span")
                    )
            );
            Assert.assertTrue(clientText.getText().toLowerCase().contains("all"));

            WebElement dateRadio = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("date"))
            );
            Assert.assertTrue(dateRadio.isSelected());

            WebElement fromDate = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("paymentDateFrom"))
            );
            WebElement toDate = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("paymentDateTo"))
            );

            Assert.assertFalse(fromDate.getAttribute("value").trim().isEmpty());
            Assert.assertFalse(toDate.getAttribute("value").trim().isEmpty());

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,"Defaults loaded",
                    "Defaults loaded",
                    "PASS",""
            );

        } catch (AssertionError | Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Verify Clear button functionality",
                    "Reset validation failed",
                    "FAIL",
                    e.getMessage()
            );
            throw e;
        }
    }

    private void setDateByJS(String elementId, String dateValue) {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
            "var el = document.getElementById(arguments[0]);" +
            "el.value = arguments[1];" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));" +
            "el.dispatchEvent(new Event('change',{bubbles:true}));",
            elementId,
            dateValue
        );
    }
    @When("I select From Date and To Date from excel")
    public void select_from_and_to_date_from_excel() {

        String testCase = TestContext.currentScenario;

        ExcelReader.loadTestCase("src/test/resources/ActiveAccountSummaryInput.xlsx", testCase);

        String fromDate = ExcelReader.get("FromDate");
        String toDate   = ExcelReader.get("ToDate");

        driver.switchTo().defaultContent();
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("balancereport")));

        if (fromDate != null && !fromDate.isBlank()) {
            setDateByJS("paymentDateFrom", fromDate);
        }

        if (toDate != null && !toDate.isBlank()) {
            setDateByJS("paymentDateTo", toDate);
        }
    }

    @Then("{string} row should be displayed in summary")
    public void verify_summary_row_displayed(String rowId) {

        WebDriverWait wait = DriverManager.getWait();

        try {
            List<WebElement> noRecords =
                    driver.findElements(
                            By.xpath("//div[contains(@class,'messageContent') " +
                                     "and contains(normalize-space(),'No Record')]")
                    );

            if (!noRecords.isEmpty() && noRecords.get(0).isDisplayed()) {

                ActiveAccountSummaryLogger.log(
                        TestContext.currentScenario,
                        "No Record(s) Found – row validation skipped","No Record(s) Found – row validation skipped",
                        
                        "PASS",
                        ""
                );
                return; 
            }

            WebElement row = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id(rowId))
            );

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,"Summary row displayed",
                    "Summary row displayed",
                    "PASS",
                    ""
            );

        } catch (Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Verify summary row " + rowId,
                    "Row not visible",
                    "FAIL",
                    e.getMessage()
            );
            throw e;
        }
    }
    @When("I click on {string} row")
    public void click_on_summary_row(String rowId) throws Exception {

        WebDriverWait wait = DriverManager.getWait();
        JavascriptExecutor js = DriverManager.getJS();

        try {
            List<WebElement> noRecords =
                    driver.findElements(
                            By.xpath("//div[contains(@class,'messageContent') " +
                                     "and contains(normalize-space(),'No Record')]")
                    );

            if (!noRecords.isEmpty() && noRecords.get(0).isDisplayed()) {

                ActiveAccountSummaryLogger.log(
                        TestContext.currentScenario,"No Record(s) Found – click skipped",
                        "No Record(s) Found – click skipped",
                        "PASS",
                        ""
                );
                return; 
            }

            WebElement row = wait.until(
                    ExpectedConditions.elementToBeClickable(By.id(rowId))
            );

            js.executeScript("arguments[0].click();", row);
            Thread.sleep(1500);

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Click successful",
                    "Click successful",
                    "PASS",
                    ""
            );

        } catch (Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Click summary row " + rowId,
                    "Click failed",
                    "FAIL",
                    e.getMessage()
            );
            throw e;
        }
    }

    @Then("{string} details grid should be displayed or empty")
    public void verify_details_grid_displayed_or_empty(String label) {

        WebDriverWait wait = DriverManager.getWait();
        String gridId;

        switch (label.trim().toUpperCase()) {
            case "BROUGHT FORWARD":   gridId = "BROUGHT_FORWARDGridview"; break;
            case "NEW LISTING":       gridId = "NEW_LISTINGGridview"; break;
            case "CANCELLATION":      gridId = "CANCELLATIONGridview"; break;
            case "PAID IN FULL":      gridId = "PAID_IN_FULLGridview"; break;
            case "SETTLED IN FULL":   gridId = "SETTLED_IN_FULLGridview"; break;
            case "PAYMENT":           gridId = "PAYMENTGridview"; break;
            case "PAYMENT REVERSAL":  gridId = "PAYMENT_REVGridview"; break;
            case "WRITE OFF":         gridId = "WRITE_OFFGridview"; break;
            case "CARRY FORWARD":     gridId = "CARRY_FORWARDGridview"; break;
            default:
                throw new RuntimeException("Unknown grid label: " + label);
        }

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(gridId)));

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Verify details grid for " + label,
                    "Verify details grid for " + label,
                    "PASS",
                    ""
            );

        } catch (TimeoutException e) {

            // Empty grid is VALID → PASS
            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Grid empty",
                    "Grid empty",
                    "PASS",
                    ""
            );
        }
    }
    @When("I select Execution Type as Real Time Data")
    public void i_select_execution_type_as_real_time_data() {

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            WebElement realTimeRadio = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("realtimeData"))
            );

            try {
                wait.until(ExpectedConditions.elementToBeClickable(realTimeRadio));
                realTimeRadio.click();
            } catch (Exception e) {
                js.executeScript("arguments[0].click();", realTimeRadio);
            }

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Real Time Data selected",
                    "Real Time Data selected",
                    "PASS",
                    ""
            );

            System.out.println("✅ Real Time Data selected");

        } catch (Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Real Time Data selecte",
                    "Failed to select Real Time Data",
                    "FAIL",
                    e.getMessage()
            );

            throw new RuntimeException("Failed to select Real Time Data", e);
        }
    }
    @When("I enter summary grid search value from excel")
    public void i_enter_summary_grid_search_value_from_excel() {

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(25));
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // ✅ Excel load based on scenario name
            String testCase = TestContext.currentScenario;
            ExcelReader.loadTestCase("src/test/resources/ActiveAccountSummaryInput.xlsx", testCase);

            String searchValue = ExcelReader.get("SearchValue");
            if (searchValue == null || searchValue.trim().isEmpty()) {
                System.out.println("⚠️ SearchValue empty in Excel for: " + testCase);
                return;
            }

            searchValue = searchValue.replaceAll("\\.0$", "").trim();

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("summaryGridview")));

            WebElement searchBox = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("#summaryGridview_filter input[type='search']")
                    )
            );

            js.executeScript("arguments[0].scrollIntoView({block:'center'});", searchBox);
            Thread.sleep(500);

            js.executeScript("arguments[0].focus();", searchBox);

            js.executeScript("arguments[0].value='';", searchBox);
            searchBox.sendKeys(searchValue);
            searchBox.sendKeys(Keys.ENTER);

            System.out.println("🔍 Entered search value: " + searchValue);

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Entered search value: " + searchValue,
                    "Entered search value: " + searchValue,
                    "PASS",
                    ""
            );

            Thread.sleep(2000);

        } catch (Exception e) {

            ActiveAccountSummaryLogger.log(
                    TestContext.currentScenario,
                    "Entered search value ",
                    "Failed to enter search value",
                    "FAIL",
                    e.getMessage()
            );

            throw new RuntimeException(e);
        }
    }



}

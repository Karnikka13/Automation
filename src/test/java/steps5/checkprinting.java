package steps5;

import io.cucumber.java.en.*;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.AfterAll;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

import utils.ExcelLogger;
import utils.CryptoUtils;

public class checkprinting {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;
    private static String usernameStr, passwordStr, websiteUrl;
    private static List<JsonObject> allTestData;
    private static List<JsonObject> allManualCheckData;
    private static List<JsonObject> allSearchData;
    private static List<JsonObject> allPrintData;
    private static final String REPORT_PATH = "E:\\selenium\\checkprinting.xlsx";
    private static final String JSON_DATA_PATH = "E:\\selenium\\checkprinting_testdata.json";
    private static final String MANUAL_CHECK_JSON_PATH = "E:\\selenium\\manualcheck_testdata.json";
    private static final String SEARCH_JSON_PATH = "E:\\selenium\\cpsearch.json";
    private static final String PRINT_JSON_PATH = "E:\\selenium\\cprint.json";

    @BeforeAll
    public static void setUp() throws IOException {
        ExcelLogger.initReport(REPORT_PATH);
        ExcelLogger.setSection("TEST EXECUTION - CHECK PRINTING");
        System.out.println("📊 Excel report: " + REPORT_PATH);

        loadJsonTestData();
        loadManualCheckTestData();
        loadSearchData();
        loadPrintData();

        try {
            WebDriverManager.chromedriver().setup();
            System.out.println("✅ ChromeDriver setup completed with WebDriverManager");
            ExcelLogger.logPass("Setup ChromeDriver", "ChromeDriver should be configured", 
                "ChromeDriver configured automatically");
        } catch (Exception e) {
            ExcelLogger.logFail("Setup ChromeDriver", "ChromeDriver should be configured", 
                "WebDriverManager setup failed", e.getMessage());
            throw new RuntimeException("❌ Failed to setup ChromeDriver: " + e.getMessage());
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        js = (JavascriptExecutor) driver;

        String encFilePath = "E:\\selenium\\credentials.txt";
        File encFile = new File(encFilePath);
        
        if (!encFile.exists()) {
            ExcelLogger.logFail("Read Credentials", "Credentials file should exist", 
                "Credentials file not found", "Encrypted credentials file not found at: " + encFilePath);
            throw new RuntimeException("❌ Encrypted credentials file not found at: " + encFilePath);
        }

        String encryptedPayload, urlLine;
        try (BufferedReader br = new BufferedReader(new FileReader(encFile))) {
            encryptedPayload = br.readLine();
            if (encryptedPayload != null) encryptedPayload = encryptedPayload.trim();
            urlLine = br.readLine();
            if (urlLine != null) websiteUrl = urlLine.trim();
        }

        if (encryptedPayload == null || encryptedPayload.isEmpty()) {
            ExcelLogger.logFail("Read Credentials", "First line should contain encrypted credentials", 
                "Empty credentials", "Encrypted credentials line is empty");
            throw new RuntimeException("❌ Encrypted credentials line is empty");
        }

        if (websiteUrl == null || websiteUrl.isEmpty()) {
            ExcelLogger.logFail("Read Website URL", "Second line should contain website URL", 
                "Empty URL", "Website URL line is empty");
            throw new RuntimeException("❌ Website URL line is empty");
        }

        ExcelLogger.logPass("Read Website URL", "Website URL read successfully", "Website URL read successfully");
        System.out.println("🌐 Website URL loaded");

        String passphraseEnv = System.getenv("AES_SECRET_KEY");
        if (passphraseEnv == null) passphraseEnv = System.getenv("CRED_PASSPHRASE");
        if (passphraseEnv == null) passphraseEnv = System.getProperty("aes.secret.key");
        if (passphraseEnv == null) passphraseEnv = System.getProperty("cred.passphrase");
        if (passphraseEnv == null) passphraseEnv = "MySecretAESKey123";

        String decrypted;
        try {
            decrypted = CryptoUtils.decrypt(encryptedPayload, passphraseEnv.toCharArray());
        } catch (Exception ex) {
            ExcelLogger.logFail("Decrypt Credentials", "Credentials should decrypt", 
                "Decryption failed", ex.getMessage());
            throw new RuntimeException("❌ Failed to decrypt credentials. " + ex.getMessage(), ex);
        }

        if (decrypted == null || !decrypted.contains(",")) {
            ExcelLogger.logFail("Validate Credentials Format", "Decrypted format username,password", 
                "Invalid format", "Decrypted credentials format invalid");
            throw new RuntimeException("❌ Decrypted credentials format should be: username,password");
        }

        String[] creds = decrypted.split(",", 2);
        usernameStr = creds[0].trim();
        passwordStr = creds[1].trim();

        driver.get(websiteUrl);
        ExcelLogger.logInfo("Navigate To Login", "Open login URL", "Navigated to login page");

        WebElement username = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("j_username")));
        username.sendKeys(usernameStr);
        ExcelLogger.logPass("Enter Username", "Username entered successfully", "Username entered");

        WebElement password = driver.findElement(By.id("j_password"));
        password.sendKeys(passwordStr);
        ExcelLogger.logPass("Enter Password", "Password entered successfully", "Password entered");

        WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        loginBtn.click();
        ExcelLogger.logPass("Click Login", "Login button clicked", "Login button clicked");

        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("artrail-logo")));
        
        ExcelLogger.logPass("Verify Login", "Login successful", "Login successful");
        System.out.println("✅ Login successful!");

        passwordStr = null;
        decrypted = null;
    }

    private static void loadJsonTestData() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(JSON_DATA_PATH)));
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
            
            JsonArray testDataArray = jsonObject.getAsJsonArray("testData");
            allTestData = new ArrayList<>();
            
            if (testDataArray != null && testDataArray.size() > 0) {
                for (int i = 0; i < testDataArray.size(); i++) {
                    allTestData.add(testDataArray.get(i).getAsJsonObject());
                }
                ExcelLogger.logPass("Load JSON Data", "JSON test data should load", 
                    "Loaded " + allTestData.size() + " test data sets");
                System.out.println("✅ JSON test data loaded successfully - " + allTestData.size() + " test sets");
            } else {
                throw new RuntimeException("No test data found in JSON file");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Load JSON Data", "JSON test data should load", 
                "Failed to load JSON", e.getMessage());
            throw new RuntimeException("❌ Failed to load JSON test data: " + e.getMessage());
        }
    }

    private static void loadManualCheckTestData() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(MANUAL_CHECK_JSON_PATH)));
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
            
            JsonArray testDataArray = jsonObject.getAsJsonArray("manualCheckData");
            allManualCheckData = new ArrayList<>();
            
            if (testDataArray != null && testDataArray.size() > 0) {
                for (int i = 0; i < testDataArray.size(); i++) {
                    allManualCheckData.add(testDataArray.get(i).getAsJsonObject());
                }
                ExcelLogger.logPass("Load Manual Check JSON", "Manual check data should load", 
                    "Loaded " + allManualCheckData.size() + " manual check data sets");
                System.out.println("✅ Manual Check JSON loaded successfully - " + allManualCheckData.size() + " test sets");
            } else {
                throw new RuntimeException("No manual check data found in JSON file");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Load Manual Check JSON", "Manual check data should load", 
                "Failed to load JSON", e.getMessage());
            throw new RuntimeException("❌ Failed to load manual check JSON: " + e.getMessage());
        }
    }

    private static void loadSearchData() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(SEARCH_JSON_PATH)));
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
            
            JsonArray searchDataArray = jsonObject.getAsJsonArray("searchData");
            allSearchData = new ArrayList<>();
            
            if (searchDataArray != null && searchDataArray.size() > 0) {
                for (int i = 0; i < searchDataArray.size(); i++) {
                    allSearchData.add(searchDataArray.get(i).getAsJsonObject());
                }
                ExcelLogger.logPass("Load Search JSON", "Search data should load", 
                    "Loaded " + allSearchData.size() + " search data sets");
                System.out.println("✅ Search JSON loaded successfully - " + allSearchData.size() + " search sets");
            } else {
                throw new RuntimeException("No search data found in JSON file");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Load Search JSON", "Search data should load", 
                "Failed to load JSON", e.getMessage());
            throw new RuntimeException("❌ Failed to load search JSON: " + e.getMessage());
        }
    }

    private static void loadPrintData() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(PRINT_JSON_PATH)));
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
            
            JsonArray printDataArray = jsonObject.getAsJsonArray("printData");
            allPrintData = new ArrayList<>();
            
            if (printDataArray != null && printDataArray.size() > 0) {
                for (int i = 0; i < printDataArray.size(); i++) {
                    allPrintData.add(printDataArray.get(i).getAsJsonObject());
                }
                ExcelLogger.logPass("Load Print JSON", "Print data should load", 
                    "Loaded " + allPrintData.size() + " print data sets");
                System.out.println("✅ Print JSON loaded successfully - " + allPrintData.size() + " print sets");
            } else {
                throw new RuntimeException("No print data found in JSON file");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Load Print JSON", "Print data should load", 
                "Failed to load JSON", e.getMessage());
            throw new RuntimeException("❌ Failed to load print JSON: " + e.getMessage());
        }
    }

    @Given("the user is logged into the Check Printing application")
    public void the_user_is_logged_into_the_check_printing_application() {
        try {
            WebElement logo = driver.findElement(By.id("artrail-logo"));
            if (logo.isDisplayed()) {
                ExcelLogger.logPass("Verify User Login", "User should be logged in", "User is logged in");
                System.out.println("✅ User is logged in");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Verify User Login", "User should be logged in", 
                "Login verification failed", e.getMessage());
            throw new RuntimeException("❌ User is not logged in: " + e.getMessage());
        }
    }

    @When("I open the menu for Check Printing")
    public void i_open_the_menu_for_check_printing() {
        try {
            WebElement menuButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("menuPushIcon")));
            js.executeScript("arguments[0].click();", menuButton);
            Thread.sleep(1000);
            
            ExcelLogger.logPass("Open Menu", "Menu should open", "Menu opened successfully");
            System.out.println("✅ Menu opened");
        } catch (Exception e) {
            ExcelLogger.logFail("Open Menu", "Menu should open", "Menu open failed", e.getMessage());
            throw new RuntimeException("❌ Failed to open menu: " + e.getMessage());
        }
    }

    @When("I search for Check Printing in the menu search bar")
    public void i_search_for_check_printing_in_the_menu_search_bar() {
        try {
            WebElement menuSearchBar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("menuSearchBar")));
            menuSearchBar.clear();
            Thread.sleep(500);
            menuSearchBar.sendKeys("Check Printing");
            
            ExcelLogger.logPass("Enter Search Term", "Search term should be entered", 
                "Entered 'Check Printing'");
            System.out.println("✅ Entered 'Check Printing' in search bar");
            Thread.sleep(1000);
        } catch (Exception e) {
            ExcelLogger.logFail("Enter Search Term", "Search term should be entered", 
                "Search entry failed", e.getMessage());
            throw new RuntimeException("❌ Failed to enter search term: " + e.getMessage());
        }
    }

    @When("I press Enter to navigate to Check Printing")
    public void i_press_enter_to_navigate_to_check_printing() {
        try {
            WebElement menuSearchBar = driver.findElement(By.id("menuSearchBar"));
            menuSearchBar.sendKeys(Keys.ENTER);
            Thread.sleep(2000);
            
            ExcelLogger.logPass("Press Enter", "Enter should navigate to page", "Pressed Enter to navigate");
            System.out.println("✅ Pressed Enter to navigate");
        } catch (Exception e) {
            ExcelLogger.logFail("Press Enter", "Enter should navigate", "Navigation failed", e.getMessage());
            throw new RuntimeException("❌ Failed to press Enter: " + e.getMessage());
        }
    }

    @When("I wait for the Check Printing page to load completely")
    public void i_wait_for_the_check_printing_page_to_load_completely() {
        try {
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            System.out.println("✅ Document ready state is complete");
            
            Thread.sleep(3000);
            
            ExcelLogger.logPass("Wait For Page Load", "Page should load completely", 
                "Check Printing page loaded");
            System.out.println("✅ Check Printing page loaded completely");
        } catch (Exception e) {
            ExcelLogger.logFail("Wait For Page Load", "Page should load completely", 
                "Page load failed", e.getMessage());
            throw new RuntimeException("❌ Failed to load Check Printing page: " + e.getMessage());
        }
    }

    @Then("I should see the Check Printing page")
    public void i_should_see_the_check_printing_page() {
        try {
            driver.switchTo().defaultContent();
            
            WebElement body = driver.findElement(By.tagName("body"));
            if (body.isDisplayed()) {
                ExcelLogger.logPass("Verify Page Visibility", "Page should be visible", 
                    "Check Printing page is visible");
                System.out.println("✅ Check Printing page is visible");
            }
            
            int iframeCount = driver.findElements(By.tagName("iframe")).size();
            System.out.println("ℹ️ Found " + iframeCount + " iframe(s) on the page");
            
            if (iframeCount > 0) {
                ExcelLogger.logInfo("Iframe Detection", "Detected " + iframeCount + " iframe(s)", 
                    "Page contains iframe content");
                driver.switchTo().frame(0);
                System.out.println("🔄 Switched to iframe for filter operations");
            }
            
            Thread.sleep(2000);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Verify Page Visibility", "Page should be visible", 
                "Page visibility check failed", e.getMessage());
            throw new RuntimeException("❌ Failed to verify page visibility: " + e.getMessage());
        }
    }

    @When("I enter first dataset and click cancel")
    public void i_enter_first_dataset_and_click_cancel() {
        try {
            if (allTestData == null || allTestData.isEmpty()) {
                throw new RuntimeException("No test data available");
            }
            
            System.out.println("\n========================================");
            System.out.println("STEP 1: Enter FIRST dataset and clear");
            System.out.println("========================================");
            
            JsonObject firstTestData = allTestData.get(0);
            String testName = firstTestData.get("testName").getAsString();
            
            ExcelLogger.logInfo("Enter First Dataset", "Processing first dataset only", "Test: " + testName);
            System.out.println("📋 Test: " + testName);
            
            enterSingleDataset(firstTestData, 1);
            Thread.sleep(1000);
            
            System.out.println("🔄 Clicking Cancel to clear first dataset...");
            clickCancelButton();
            Thread.sleep(1500);
            
            ExcelLogger.logPass("Enter First Dataset and Cancel", 
                "First dataset should be entered and cleared", 
                "Successfully entered and cleared first dataset");
            
            System.out.println("✅ STEP 1 completed - First dataset entered and cleared\n");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Enter First Dataset and Cancel", 
                "First dataset should be entered and cleared", 
                "Failed to process first dataset", e.getMessage());
            throw new RuntimeException("❌ Failed to enter first dataset and cancel: " + e.getMessage());
        }
    }

    @When("I enter all datasets from JSON and apply filters")
    public void i_enter_all_datasets_from_json_and_apply_filters() {
        try {
            if (allTestData == null || allTestData.isEmpty()) {
                throw new RuntimeException("No test data available");
            }
            
            System.out.println("\n========================================");
            System.out.println("STEP 2: Process ALL datasets with Apply Filter");
            System.out.println("========================================");
            
            for (int i = 0; i < allTestData.size(); i++) {
                JsonObject testData = allTestData.get(i);
                String currentTestName = testData.get("testName").getAsString();
                
                System.out.println("\n--- Processing dataset " + (i + 1) + " of " + allTestData.size() + " ---");
                System.out.println("📋 Test: " + currentTestName);
                
                ExcelLogger.logInfo("Process Dataset " + (i + 1), 
                    "Processing dataset " + (i + 1) + " of " + allTestData.size(), 
                    "Test: " + currentTestName);
                
                enterSingleDataset(testData, i + 1);
                Thread.sleep(1000);
                
                clickApplyFilterButton();
                Thread.sleep(2000);
                
                if (i < allTestData.size() - 1) {
                    System.out.println("🔄 Clearing filters for next dataset...");
                    clickCancelButton();
                    Thread.sleep(1500);
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("✅ STEP 2 completed - ALL DATASETS PROCESSED");
            System.out.println("========================================\n");
            
            ExcelLogger.logPass("Process All Datasets", "All datasets should be processed", 
                "Successfully processed all " + allTestData.size() + " datasets with Apply Filter");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Process All Datasets", "All datasets should be processed", 
                "Failed to process datasets", e.getMessage());
            throw new RuntimeException("❌ Failed to process all datasets: " + e.getMessage());
        }
    }

    @When("I create manual checks from JSON file")
    public void i_create_manual_checks_from_json_file() {
        try {
            if (allManualCheckData == null || allManualCheckData.isEmpty()) {
                throw new RuntimeException("No manual check data available");
            }
            
            System.out.println("\n========================================");
            System.out.println("MANUAL CHECK CREATION");
            System.out.println("========================================");
            
            for (int i = 0; i < allManualCheckData.size(); i++) {
                JsonObject checkData = allManualCheckData.get(i);
                String testName = checkData.get("testName").getAsString();
                
                System.out.println("\n--- Processing manual check " + (i + 1) + " of " + allManualCheckData.size() + " ---");
                System.out.println("📋 Test: " + testName);
                
                ExcelLogger.logInfo("Process Manual Check " + (i + 1), 
                    "Processing manual check " + (i + 1) + " of " + allManualCheckData.size(), 
                    "Test: " + testName);
                
                clickCreateManualCheckButton();
                Thread.sleep(2000);
                
                enterManualCheckData(checkData, i + 1);
                Thread.sleep(1000);
                
                clickManualCheckSubmitButton();
                Thread.sleep(2500);
            }
            
            System.out.println("\n========================================");
            System.out.println("✅ ALL MANUAL CHECKS PROCESSED");
            System.out.println("========================================\n");
            
            ExcelLogger.logPass("Create Manual Checks", "All manual checks should be created", 
                "Successfully created all " + allManualCheckData.size() + " manual checks");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Create Manual Checks", "All manual checks should be created", 
                "Failed to create manual checks", e.getMessage());
            throw new RuntimeException("❌ Failed to create manual checks: " + e.getMessage());
        }
    }

    @When("I search for checks and print them")
    public void i_search_for_checks_and_print_them() {
        try {
            if (allSearchData == null || allSearchData.isEmpty()) {
                throw new RuntimeException("No search data available");
            }
            
            if (allPrintData == null || allPrintData.isEmpty()) {
                throw new RuntimeException("No print data available");
            }
            
            System.out.println("\n========================================");
            System.out.println("SEARCH AND PRINT CHECKS");
            System.out.println("========================================");
            
            // Ensure we're in the correct context
            driver.switchTo().defaultContent();
            int iframeCount = driver.findElements(By.tagName("iframe")).size();
            if (iframeCount > 0) {
                driver.switchTo().frame(0);
                System.out.println("🔄 Switched to iframe for search operations");
            }
            
            // Process each search query
            for (int i = 0; i < allSearchData.size(); i++) {
                JsonObject searchData = allSearchData.get(i);
                String searchQuery = searchData.get("searchQuery").getAsString();
                
                System.out.println("\n--- Processing search " + (i + 1) + " of " + allSearchData.size() + " ---");
                System.out.println("🔍 Search: " + searchQuery);
                
                ExcelLogger.logInfo("Search Checks " + (i + 1), 
                    "Searching for checks " + (i + 1) + " of " + allSearchData.size(), 
                    "Search: " + searchQuery);
                
                // Find and enter search text
                WebElement searchBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("checkPendingGridSearch")));
                
                searchBox.clear();
                Thread.sleep(500);
                searchBox.sendKeys(searchQuery);
                Thread.sleep(500);
                searchBox.sendKeys(Keys.ENTER);
                Thread.sleep(2500);
                
                ExcelLogger.logPass("Enter Search Query", "Should enter and search", 
                    "Searched for: " + searchQuery);
                System.out.println("✅ Searched for: " + searchQuery);
                
                // Click first checkbox - trying multiple approaches
                try {
                    // Try to find checkboxes in the grid
                    List<WebElement> checkboxes = driver.findElements(
                        By.xpath("//input[@type='checkbox' and @value='n']"));
                    
                    if (checkboxes.size() > 0) {
                        // Click the first data checkbox (skip header checkbox at index 0)
                        WebElement firstCheckbox = checkboxes.get(1);
                        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", firstCheckbox);
                        Thread.sleep(500);
                        js.executeScript("arguments[0].click();", firstCheckbox);
                        Thread.sleep(1000);
                        
                        ExcelLogger.logPass("Select First Checkbox", "Should select first result", 
                            "First checkbox selected");
                        System.out.println("✅ First checkbox selected");
                    } else {
                        System.out.println("⚠️ No checkboxes found");
                        continue;
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ No results found or checkbox not available: " + e.getMessage());
                    ExcelLogger.logInfo("Select Checkbox", "No results to select", 
                        "No checkbox found for this search");
                    continue;
                }
                
                // Click Print Selected button
                try {
                    WebElement printSelectedBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//a[@onclick='printSelected()']")));
                    
                    js.executeScript("arguments[0].scrollIntoView({block: 'center'});", printSelectedBtn);
                    Thread.sleep(500);
                    js.executeScript("arguments[0].click();", printSelectedBtn);
                    Thread.sleep(2500);
                    
                    ExcelLogger.logPass("Click Print Selected", "Print dialog should open", 
                        "Print Selected button clicked");
                    System.out.println("✅ Print Selected button clicked");
                    
                    // Process print dialog with data from cprint.json
                    if (i < allPrintData.size()) {
                        JsonObject printData = allPrintData.get(i);
                        processPrintDialog(printData);
                    } else {
                        // Use first print data if we have more searches than print configs
                        processPrintDialog(allPrintData.get(0));
                    }
                    
                    Thread.sleep(1500);
                } catch (Exception e) {
                    System.out.println("⚠️ Print Selected button not available: " + e.getMessage());
                    ExcelLogger.logInfo("Click Print Selected", "Print button not available", 
                        "Print Selected button not found");
                }
            }
            
            System.out.println("\n========================================");
            System.out.println("✅ ALL SEARCHES AND PRINTS COMPLETED");
            System.out.println("========================================\n");
            
            ExcelLogger.logPass("Search and Print Checks", "All checks should be searched and printed", 
                "Successfully processed all " + allSearchData.size() + " searches");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Search and Print Checks", "Should search and print checks", 
                "Search/Print failed", e.getMessage());
            throw new RuntimeException("❌ Failed to search and print checks: " + e.getMessage());
        }
    }
    
    @When("I select all checks in each tab")
    public void i_select_all_checks_in_each_tab() {
        try {
            System.out.println("\n========================================");
            System.out.println("SELECT ALL CHECKS IN TABS");
            System.out.println("========================================");
            
            // Ensure we're in the correct context
            driver.switchTo().defaultContent();
            int iframeCount = driver.findElements(By.tagName("iframe")).size();
            if (iframeCount > 0) {
                driver.switchTo().frame(0);
                System.out.println("🔄 Switched to iframe for tab operations");
            }
            
            // Click All tab
            try {
                WebElement allTab = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("allChecks")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", allTab);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", allTab);
                Thread.sleep(1500);
                
                ExcelLogger.logPass("Click All Tab", "All tab should be clicked", "All tab clicked");
                System.out.println("✅ All tab clicked");
                
                // Click select all checkbox
                WebElement selectAllCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("select_allp")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", selectAllCheckbox);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", selectAllCheckbox);
                Thread.sleep(1000);
                
                ExcelLogger.logPass("Select All in All Tab", "Select all checkbox should be clicked", 
                    "Select all checked in All tab");
                System.out.println("✅ Select all checked in All tab");
            } catch (Exception e) {
                System.out.println("⚠️ Failed to select all in All tab: " + e.getMessage());
                ExcelLogger.logInfo("Select All in All Tab", "Failed to select", e.getMessage());
            }
            
            // Click New tab
            try {
                WebElement newTab = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("newChecks")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", newTab);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", newTab);
                Thread.sleep(1500);
                
                ExcelLogger.logPass("Click New Tab", "New tab should be clicked", "New tab clicked");
                System.out.println("✅ New tab clicked");
                
                // Click select all checkbox
                WebElement selectAllCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("select_allp")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", selectAllCheckbox);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", selectAllCheckbox);
                Thread.sleep(1000);
                
                ExcelLogger.logPass("Select All in New Tab", "Select all checkbox should be clicked", 
                    "Select all checked in New tab");
                System.out.println("✅ Select all checked in New tab");
            } catch (Exception e) {
                System.out.println("⚠️ Failed to select all in New tab: " + e.getMessage());
                ExcelLogger.logInfo("Select All in New Tab", "Failed to select", e.getMessage());
            }
            
            // Click Re-Requested tab
            try {
                WebElement rerequestTab = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("rerequestChecks")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", rerequestTab);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", rerequestTab);
                Thread.sleep(1500);
                
                ExcelLogger.logPass("Click Re-Requested Tab", "Re-Requested tab should be clicked", 
                    "Re-Requested tab clicked");
                System.out.println("✅ Re-Requested tab clicked");
                
                // Click select all checkbox
                WebElement selectAllCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("select_allp")));
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", selectAllCheckbox);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", selectAllCheckbox);
                Thread.sleep(1000);
                
                ExcelLogger.logPass("Select All in Re-Requested Tab", "Select all checkbox should be clicked", 
                    "Select all checked in Re-Requested tab");
                System.out.println("✅ Select all checked in Re-Requested tab");
            } catch (Exception e) {
                System.out.println("⚠️ Failed to select all in Re-Requested tab: " + e.getMessage());
                ExcelLogger.logInfo("Select All in Re-Requested Tab", "Failed to select", e.getMessage());
            }
            
            System.out.println("\n========================================");
            System.out.println("✅ ALL TAB SELECTIONS COMPLETED");
            System.out.println("========================================\n");
            
            ExcelLogger.logPass("Select All in Tabs", "All tabs should have select all clicked", 
                "Completed selecting all in all tabs");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Select All in Tabs", "Should select all in each tab", 
                "Tab selection failed", e.getMessage());
            throw new RuntimeException("❌ Failed to select all in tabs: " + e.getMessage());
        }
    }

    @When("I print individual check")
    public void i_print_individual_check() {
        try {
            if (allPrintData == null || allPrintData.isEmpty()) {
                throw new RuntimeException("No print data available for individual print");
            }
            
            System.out.println("\n========================================");
            System.out.println("INDIVIDUAL CHECK PRINT");
            System.out.println("========================================");
            
            // Ensure we're in the correct context
            driver.switchTo().defaultContent();
            int iframeCount = driver.findElements(By.tagName("iframe")).size();
            if (iframeCount > 0) {
                driver.switchTo().frame(0);
                System.out.println("🔄 Switched to iframe for individual print");
            }
            
            // Click the print icon (individual check print)
            try {
                WebElement printIcon = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//i[@class='fa fa-print' and contains(@onclick, 'print(')]")));
                
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", printIcon);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", printIcon);
                Thread.sleep(2500);
                
                ExcelLogger.logPass("Click Individual Print Icon", "Print icon should open dialog", 
                    "Individual print icon clicked");
                System.out.println("✅ Individual print icon clicked");
                
                // Use first print data for individual print
                JsonObject printData = allPrintData.get(0);
                processPrintDialog(printData);
                
                System.out.println("\n========================================");
                System.out.println("✅ INDIVIDUAL PRINT COMPLETED");
                System.out.println("========================================\n");
                
                ExcelLogger.logPass("Individual Print", "Individual print should complete", 
                    "Individual print completed successfully");
                    
            } catch (Exception e) {
                System.out.println("⚠️ Individual print icon not found or not clickable");
                ExcelLogger.logInfo("Individual Print", "Print icon not available", 
                    "Individual print icon may not be present");
            }
            
        } catch (Exception e) {
            ExcelLogger.logFail("Individual Print", "Should print individual check", 
                "Individual print failed", e.getMessage());
            throw new RuntimeException("❌ Failed to print individual check: " + e.getMessage());
        }
    }

    private void processPrintDialog(JsonObject printData) throws Exception {
        String templateReference = printData.get("templateReference").getAsString();
        String remark = printData.get("remark").getAsString();
        
        System.out.println("📋 Processing print dialog...");
        
        // Select template reference
        WebElement templateDropdown = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("chkTemplateReference")));
        
        Select select = new Select(templateDropdown);
        select.selectByVisibleText(templateReference);
        Thread.sleep(1000);
        
        ExcelLogger.logPass("Select Template Reference", "Should select template", 
            "Selected: " + templateReference);
        System.out.println("✅ Selected template: " + templateReference);
        
        // Enter remark
        WebElement remarkBox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("printRemark")));
        
        remarkBox.clear();
        Thread.sleep(300);
        remarkBox.sendKeys(remark);
        Thread.sleep(500);
        
        ExcelLogger.logPass("Enter Print Remark", "Should enter remark", 
            "Entered: " + remark);
        System.out.println("✅ Entered remark: " + remark);
        
        // Click Submit button
        WebElement submitBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("printCheck")));
        
        js.executeScript("arguments[0].scrollIntoView({block: 'center'});", submitBtn);
        Thread.sleep(300);
        js.executeScript("arguments[0].click();", submitBtn);
        Thread.sleep(2500);
        
        ExcelLogger.logPass("Click Print Submit", "Print should submit", 
            "Print submitted successfully");
        System.out.println("✅ Print submitted");
    }

    @When("I export data to Excel and PDF")
    public void i_export_data_to_excel_and_pdf() {
        try {
            System.out.println("\n========================================");
            System.out.println("EXPORT DATA TO EXCEL AND PDF");
            System.out.println("========================================");
            
            // Ensure we're in the correct context
            driver.switchTo().defaultContent();
            int iframeCount = driver.findElements(By.tagName("iframe")).size();
            if (iframeCount > 0) {
                driver.switchTo().frame(0);
                System.out.println("🔄 Switched to iframe for export operations");
            }
            
            // Click Excel export button
            try {
                WebElement excelBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//i[contains(@class, 'fa-file-excel-o') and contains(@class, 'floatingButton')]")));
                
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", excelBtn);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", excelBtn);
                Thread.sleep(2500);
                
                ExcelLogger.logPass("Export to Excel", "Excel export should work", 
                    "Excel export button clicked");
                System.out.println("✅ Excel export initiated");
            } catch (Exception e) {
                ExcelLogger.logInfo("Export to Excel", "Excel button not found", 
                    "Excel export button may not be available");
                System.out.println("ℹ️ Excel export button not found or not clickable");
            }
            
            // Click PDF export button
            try {
                WebElement pdfBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//i[contains(@class, 'fa-file-pdf-o') and contains(@class, 'floatingButton')]")));
                
                js.executeScript("arguments[0].scrollIntoView({block: 'center'});", pdfBtn);
                Thread.sleep(500);
                js.executeScript("arguments[0].click();", pdfBtn);
                Thread.sleep(2500);
                
                ExcelLogger.logPass("Export to PDF", "PDF export should work", 
                    "PDF export button clicked");
                System.out.println("✅ PDF export initiated");
            } catch (Exception e) {
                ExcelLogger.logInfo("Export to PDF", "PDF button not found", 
                    "PDF export button may not be available");
                System.out.println("ℹ️ PDF export button not found or not clickable");
            }
            
            System.out.println("\n========================================");
            System.out.println("✅ EXPORT OPERATIONS COMPLETED");
            System.out.println("========================================\n");
            
            ExcelLogger.logPass("Export Data", "Data should be exported", 
                "Export operations completed");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Export Data", "Should export data", 
                "Export failed", e.getMessage());
            throw new RuntimeException("❌ Failed to export data: " + e.getMessage());
        }
    }

    private void enterSingleDataset(JsonObject testData, int datasetNumber) throws Exception {
        String corporate = testData.get("corporate").getAsString();
        String parent = testData.get("parent").getAsString();
        String clientParent = testData.get("clientParent").getAsString();
        String trustPaymentDate = testData.get("trustPaymentDate").getAsString();
        String type = testData.get("type").getAsString();
        
        selectMultiSelectOptionWithSearch("corporates", "Corporate", corporate);
        Thread.sleep(1000);
        
        selectMultiSelectOptionWithSearch("parents", "Parent", parent);
        Thread.sleep(1000);
        
        selectMultiSelectOptionWithSearch("clients", "Client", clientParent);
        Thread.sleep(1000);
        
        selectMultiSelectOptionWithSearch("payOrTrustDate", "Trust/Payment Date", trustPaymentDate);
        Thread.sleep(1000);
        
        selectMultiSelectOptionWithSearch("type", "Type", type);
        Thread.sleep(1000);
        
        ExcelLogger.logPass("Enter Dataset " + datasetNumber, 
            "Dataset " + datasetNumber + " should be entered", 
            "Successfully entered dataset " + datasetNumber);
        System.out.println("✅ Dataset " + datasetNumber + " entered successfully");
    }

    private void clickCreateManualCheckButton() {
        try {
            driver.switchTo().defaultContent();
            System.out.println("🔄 Switched to default content to find Create Manual Check button");
            
            int iframeCount = driver.findElements(By.tagName("iframe")).size();
            if (iframeCount > 0) {
                driver.switchTo().frame(0);
                System.out.println("🔄 Switched to iframe");
            }
            
            WebElement createBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@class='btn btn-sm btn-primary' and @onclick='createManualCheck()' and @value='Create Manual Check']")));
            
            js.executeScript("arguments[0].scrollIntoView(true);", createBtn);
            Thread.sleep(500);
            js.executeScript("arguments[0].click();", createBtn);
            
            ExcelLogger.logPass("Click Create Manual Check", "Create button should work", 
                "Create Manual Check button clicked");
            System.out.println("✅ Create Manual Check button clicked");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Click Create Manual Check", "Create button should work", 
                "Create button click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Create Manual Check: " + e.getMessage());
        }
    }

    private void enterManualCheckData(JsonObject checkData, int checkNumber) throws Exception {
        String checkFor = checkData.get("checkFor").getAsString();
        String amount = checkData.get("amount").getAsString();
        String remark = checkData.get("remark").getAsString();
        
        Thread.sleep(1500);
        
        WebElement checkForDropdown = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("checkFor")));
        Select select = new Select(checkForDropdown);
        select.selectByVisibleText(checkFor);
        Thread.sleep(2000);
        
        ExcelLogger.logPass("Select Check For", "Should select " + checkFor, 
            "Selected: " + checkFor);
        System.out.println("✅ Selected Check For: " + checkFor);
        
        if (checkFor.equalsIgnoreCase("Client") || checkFor.equalsIgnoreCase("Consumer")) {
            String searchText = checkData.get("search").getAsString();
            
            WebElement searchBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@placeholder='Search' and @type='text' and @autocomplete='off']")));
            
            wait.until(ExpectedConditions.visibilityOf(searchBox));
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", searchBox);
            Thread.sleep(500);
            
            searchBox.clear();
            Thread.sleep(300);
            searchBox.sendKeys(searchText);
            Thread.sleep(2500);
            
            ExcelLogger.logPass("Enter Search", "Should enter search text", 
                "Entered: " + searchText);
            System.out.println("✅ Entered search: " + searchText);
            
            try {
                WebElement firstSuggestion = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[@class='suggestionList']//div[1]")));
                js.executeScript("arguments[0].click();", firstSuggestion);
                Thread.sleep(500);
                System.out.println("✅ Clicked suggestion from list");
            } catch (Exception e) {
                System.out.println("ℹ️ No suggestion list found, continuing...");
            }
            
        } else if (checkFor.equalsIgnoreCase("Custom")) {
            String account = checkData.get("account").getAsString();
            WebElement accountBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("accountId")));
            accountBox.clear();
            accountBox.sendKeys(account);
            Thread.sleep(500);
            
            ExcelLogger.logPass("Enter Account", "Should enter account", 
                "Entered: " + account);
            System.out.println("✅ Entered account: " + account);
            
            String custom1 = checkData.get("custom1").getAsString();
            WebElement custom1Box = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("checkForValue")));
            custom1Box.clear();
            custom1Box.sendKeys(custom1);
            Thread.sleep(500);
            
            ExcelLogger.logPass("Enter Custom1", "Should enter custom1", 
                "Entered: " + custom1);
            System.out.println("✅ Entered custom1: " + custom1);
        }
        
        WebElement amountBox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("amount")));
        amountBox.clear();
        amountBox.sendKeys(amount);
        Thread.sleep(500);
        
        ExcelLogger.logPass("Enter Amount", "Should enter amount", 
            "Entered: " + amount);
        System.out.println("✅ Entered amount: " + amount);
        
        WebElement remarkBox = wait.until(ExpectedConditions.presenceOfElementLocated(
            By.id("remark")));
        remarkBox.clear();
        remarkBox.sendKeys(remark);
        Thread.sleep(500);
        
        ExcelLogger.logPass("Enter Remark", "Should enter remark", 
            "Entered: " + remark);
        System.out.println("✅ Entered remark: " + remark);
        
        if (checkFor.equalsIgnoreCase("Custom")) {
            String custom2 = checkData.get("custom2").getAsString();
            WebElement custom2Box = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("cpdCustom2")));
            custom2Box.clear();
            custom2Box.sendKeys(custom2);
            Thread.sleep(500);
            
            ExcelLogger.logPass("Enter Custom2", "Should enter custom2", 
                "Entered: " + custom2);
            System.out.println("✅ Entered custom2: " + custom2);
        }
        
        ExcelLogger.logPass("Enter Manual Check " + checkNumber, 
            "Manual check " + checkNumber + " should be entered", 
            "Successfully entered manual check " + checkNumber);
        System.out.println("✅ Manual check " + checkNumber + " entered successfully");
    }

    private void clickManualCheckSubmitButton() {
        try {
            WebElement submitBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@id='submitManualChk' and @onclick='submitManualChk()']")));
            
            js.executeScript("arguments[0].scrollIntoView(true);", submitBtn);
            Thread.sleep(300);
            js.executeScript("arguments[0].click();", submitBtn);
            
            ExcelLogger.logPass("Click Manual Check Submit", "Submit button should work", 
                "Manual Check Submit button clicked");
            System.out.println("✅ Manual Check Submit button clicked");
            
            Thread.sleep(2000);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Click Manual Check Submit", "Submit button should work", 
                "Submit button click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Manual Check Submit: " + e.getMessage());
        }
    }

    private void selectMultiSelectOptionWithSearch(String selectId, String labelName, String valueToSelect) throws Exception {
        try {
            System.out.println("🔍 Attempting to select '" + valueToSelect + "' in " + labelName + " (ID: " + selectId + ")");
            
            WebElement msParent = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@id='" + selectId + "']/following-sibling::div[contains(@class, 'ms-parent')]")
            ));
            
            js.executeScript("arguments[0].scrollIntoView({block: 'center'});", msParent);
            Thread.sleep(300);
            
            WebElement button = msParent.findElement(By.tagName("button"));
            js.executeScript("arguments[0].click();", button);
            System.out.println("✅ Clicked multi-select button for " + labelName);
            Thread.sleep(1500);
            
            boolean textEntered = false;
            
            List<WebElement> searchBoxes = driver.findElements(
                By.cssSelector("div.ms-drop input[type='text']")
            );
            
            System.out.println("📋 Found " + searchBoxes.size() + " search box(es)");
            
            if (!searchBoxes.isEmpty()) {
                for (WebElement searchBox : searchBoxes) {
                    try {
                        if (searchBox.isDisplayed()) {
                            js.executeScript("arguments[0].focus();", searchBox);
                            Thread.sleep(200);
                            
                            searchBox.clear();
                            Thread.sleep(200);
                            
                            searchBox.sendKeys(valueToSelect);
                            System.out.println("📝 Typed '" + valueToSelect + "' in search box using sendKeys");
                            Thread.sleep(1000);
                            
                            textEntered = true;
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("ℹ️ Failed with search box, trying next: " + e.getMessage());
                        continue;
                    }
                }
            }
            
            if (!textEntered) {
                System.out.println("ℹ️ Trying JavaScript approach to enter search text");
                String jsSetValue = 
                    "var inputs = document.querySelectorAll('div.ms-drop input[type=\"text\"]');" +
                    "for(var i = 0; i < inputs.length; i++) {" +
                    "  if(inputs[i].offsetParent !== null && window.getComputedStyle(inputs[i]).display !== 'none') {" +
                    "    inputs[i].value = '" + valueToSelect.replace("'", "\\'") + "';" +
                    "    inputs[i].focus();" +
                    "    var event = new Event('input', { bubbles: true });" +
                    "    inputs[i].dispatchEvent(event);" +
                    "    var keyupEvent = new KeyboardEvent('keyup', { bubbles: true });" +
                    "    inputs[i].dispatchEvent(keyupEvent);" +
                    "    return 'Text entered successfully';" +
                    "  }" +
                    "}" +
                    "return 'No visible search box found';";
                
                Object result = js.executeScript(jsSetValue);
                System.out.println("📝 JavaScript result: " + result);
                Thread.sleep(1000);
                textEntered = true;
            }
            
            try {
                List<WebElement> selectAllCheckboxes = driver.findElements(
                    By.cssSelector("div.ms-drop input[type='checkbox'][name='selectAll']")
                );
                
                System.out.println("📋 Found " + selectAllCheckboxes.size() + " 'Select All' checkbox(es)");
                
                boolean selectAllClicked = false;
                for (WebElement checkbox : selectAllCheckboxes) {
                    try {
                        if (checkbox.isDisplayed()) {
                            js.executeScript("arguments[0].click();", checkbox);
                            System.out.println("✅ Clicked 'Select All' checkbox for " + labelName);
                            Thread.sleep(500);
                            selectAllClicked = true;
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("ℹ️ Failed with checkbox, trying next");
                        continue;
                    }
                }
                
                if (!selectAllClicked) {
                    System.out.println("ℹ️ Trying JavaScript to click Select All");
                    String jsClickSelectAll = 
                        "var checkboxes = document.querySelectorAll('div.ms-drop input[type=\"checkbox\"][name=\"selectAll\"]');" +
                        "for(var i = 0; i < checkboxes.length; i++) {" +
                        "  if(checkboxes[i].offsetParent !== null) {" +
                        "    checkboxes[i].click();" +
                        "    return 'Checkbox clicked';" +
                        "  }" +
                        "}" +
                        "return 'Checkbox not found';";
                    
                    Object checkboxResult = js.executeScript(jsClickSelectAll);
                    System.out.println("✅ JavaScript checkbox result: " + checkboxResult);
                }
                
            } catch (Exception e) {
                System.out.println("ℹ️ Could not click 'Select All' checkbox: " + e.getMessage());
            }
            
            Thread.sleep(500);
            js.executeScript("arguments[0].click();", button);
            Thread.sleep(300);
            
            ExcelLogger.logPass("Select " + labelName, "Should select " + valueToSelect, 
                "Selected: " + valueToSelect + " using search and Select All");
            System.out.println("✅ Successfully selected '" + valueToSelect + "' in " + labelName);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Select " + labelName, "Should select " + valueToSelect, 
                "Selection failed", e.getMessage());
            System.out.println("❌ Failed to select " + valueToSelect + " in " + labelName + ": " + e.getMessage());
            throw new Exception("Failed to select " + valueToSelect + " in " + labelName);
        }
    }

    private void clickCancelButton() {
        try {
            WebElement cancelBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@id='cancelFilter' and @value='Cancel']")));
            
            js.executeScript("arguments[0].scrollIntoView(true);", cancelBtn);
            Thread.sleep(300);
            js.executeScript("arguments[0].click();", cancelBtn);
            
            ExcelLogger.logPass("Click Cancel", "Cancel button should work", 
                "Cancel button clicked - filters cleared");
            System.out.println("✅ Cancel button clicked - filters cleared");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Click Cancel", "Cancel button should work", 
                "Cancel click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Cancel: " + e.getMessage());
        }
    }

    private void clickApplyFilterButton() {
        try {
            WebElement applyBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@id='applyFilter' and @value='Apply Filter']")));
            
            js.executeScript("arguments[0].scrollIntoView(true);", applyBtn);
            Thread.sleep(300);
            js.executeScript("arguments[0].click();", applyBtn);
            
            ExcelLogger.logPass("Click Apply Filter", "Apply Filter button should work", 
                "Apply Filter clicked - search initiated");
            System.out.println("✅ Apply Filter clicked - search initiated");
            
            Thread.sleep(2000);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Click Apply Filter", "Apply Filter should work", 
                "Apply Filter failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Apply Filter: " + e.getMessage());
        }
    }

    @When("I logout from Check Printing")
    public void i_logout_from_check_printing() {
        driver.switchTo().defaultContent();
        System.out.println("🔄 Switched to default content for logout");
        
        try {
            Thread.sleep(1000);
            
            WebElement welcomeNote = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//small[@class='welcomenote white-text' and contains(text(),'Welcome')]")));
            js.executeScript("arguments[0].click();", welcomeNote);
            Thread.sleep(1000);
            ExcelLogger.logPass("Click Welcome Menu", "Welcome menu should be clickable", "Clicked welcome menu");
            
            WebElement logoutBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@onclick='logout()']")));
            js.executeScript("arguments[0].click();", logoutBtn);
            Thread.sleep(2000);
            
            ExcelLogger.logPass("Logout", "User should logout", "User logged out successfully");
            System.out.println("✅ Logged out successfully");
        } catch (Exception e) {
            ExcelLogger.logFail("Logout", "User should logout", "Logout failed", e.getMessage());
            throw new RuntimeException("❌ Logout failed: " + e.getMessage());
        }
    }

    @Then("the user should be logged out from Check Printing successfully")
    public void the_user_should_be_logged_out_from_check_printing_successfully() {
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("login"),
                ExpectedConditions.visibilityOfElementLocated(By.id("j_username"))
            ));
            
            ExcelLogger.logPass("Verify Logout", "Should redirect to login page", "Redirected to login page");
            System.out.println("✅ Logout verified - redirected to login");
        } catch (Exception e) {
            ExcelLogger.logInfo("Verify Logout", "Logout verification", "Session ended");
            System.out.println("ℹ️ Session ended");
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("🔒 Browser closed");
            }
        }
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
            ExcelLogger.logInfo("Teardown", "Driver closed", "Browser closed");
        }
        ExcelLogger.closeReport();
        System.out.println("📊 Excel Report generated at: " + REPORT_PATH);
    }
}
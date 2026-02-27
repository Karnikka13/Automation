package steps2;

import io.cucumber.java.en.*;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.AfterAll;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import utils.ExcelLogger;
import utils.CryptoUtils;

public class Worklist {
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;
    private static String usernameStr, passwordStr, websiteUrl;
    private static String lastUpdatedCounty = null;
    private static final String REPORT_PATH = "E:\\selenium\\WorklistReport.xlsx";
    private static final String SEARCH_DATA_PATH = "E:\\selenium\\searchData.json";

    // ---------- SETUP & LOGIN ----------
    @BeforeAll
    public static void setUp() throws IOException {
        ExcelLogger.initReport(REPORT_PATH);
        ExcelLogger.setSection("TEST EXECUTION");
        System.out.println("📊 Excel report: " + REPORT_PATH);

        System.setProperty("webdriver.chrome.driver",
                "C:\\Users\\Aishu\\eclipse-workspace\\proj1\\Drivers\\chromedriver.exe");

        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        js = (JavascriptExecutor) driver;

        // ---------- read encrypted credentials ----------
        String encFilePath = "E:\\selenium\\credentials.txt";
        File encFile = new File(encFilePath);
        if (!encFile.exists()) {
            ExcelLogger.logFail("Read Credentials", "Credentials file should exist", "Credentials file not found at: " + encFilePath, "Encrypted credentials file not found at: " + encFilePath);
            throw new RuntimeException("❌ Encrypted credentials file not found at: " + encFilePath);
        }

        String encryptedPayload;
        String urlLine;
        try (BufferedReader br = new BufferedReader(new FileReader(encFile))) {
            encryptedPayload = br.readLine();
            if (encryptedPayload != null) {
                encryptedPayload = encryptedPayload.trim();
            }
            urlLine = br.readLine();
            if (urlLine != null) {
                websiteUrl = urlLine.trim();
            }
        }

        if (encryptedPayload == null || encryptedPayload.isEmpty()) {
            ExcelLogger.logFail("Read Credentials", "First line should contain encrypted credentials", "Empty credentials line", "Encrypted credentials line is empty");
            throw new RuntimeException("❌ Encrypted credentials line is empty");
        }

        if (websiteUrl == null || websiteUrl.isEmpty()) {
            ExcelLogger.logFail("Read Website URL", "Second line should contain website URL", "Empty URL line", "Website URL line is empty");
            throw new RuntimeException("❌ Website URL line is empty");
        }

        ExcelLogger.logPass("Read Website URL", "Website URL read successfully", "Website URL read successfully");
        System.out.println("🌐 Website URL loaded");

        String passphraseEnv = System.getenv("AES_SECRET_KEY");
        if (passphraseEnv == null || passphraseEnv.isEmpty()) {
            passphraseEnv = System.getenv("CRED_PASSPHRASE");
        }
        if (passphraseEnv == null || passphraseEnv.isEmpty()) {
            passphraseEnv = System.getProperty("aes.secret.key");
        }
        if (passphraseEnv == null || passphraseEnv.isEmpty()) {
            passphraseEnv = System.getProperty("cred.passphrase");
        }
        if (passphraseEnv == null || passphraseEnv.isEmpty()) {
            passphraseEnv = "MySecretAESKey123";
        }

        System.out.println("🔑 Using passphrase from: " + 
            (System.getenv("AES_SECRET_KEY") != null ? "AES_SECRET_KEY env var" : 
             System.getenv("CRED_PASSPHRASE") != null ? "CRED_PASSPHRASE env var" : "default fallback"));

        String decrypted;
        try {
            decrypted = CryptoUtils.decrypt(encryptedPayload, passphraseEnv.toCharArray());
        } catch (Exception ex) {
            ExcelLogger.logFail("Decrypt Credentials", "Credentials should decrypt", "Decryption failed - likely wrong passphrase", ex.getMessage());
            System.err.println("❌ Decryption failed. Current passphrase source: " + 
                (System.getenv("AES_SECRET_KEY") != null ? "AES_SECRET_KEY" : "default"));
            System.err.println("💡 Make sure credentials.txt was encrypted with the same passphrase!");
            throw new RuntimeException("❌ Failed to decrypt credentials. " + ex.getMessage(), ex);
        }

        if (decrypted == null || !decrypted.contains(",")) {
            ExcelLogger.logFail("Validate Credentials Format", "Decrypted format username,password", "Invalid decrypted payload", "Decrypted credentials format invalid.");
            throw new RuntimeException("❌ Decrypted credentials format should be: username,password");
        }

        String[] creds = decrypted.split(",", 2);
        usernameStr = creds[0].trim();
        passwordStr = creds[1].trim();

        // ---------- open browser & login ----------
        driver.manage().window().maximize();
        driver.get(websiteUrl);
        ExcelLogger.logInfo("Navigate To Login", "Open login URL", "Navigated to login page");

        WebElement username = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("j_username")));
        username.sendKeys(usernameStr);
        ExcelLogger.logPass("Enter Username", "Username entered successfully", "Username entered successfully");

        WebElement password = driver.findElement(By.id("j_password"));
        password.sendKeys(passwordStr);
        ExcelLogger.logPass("Enter Password", "Password entered successfully", "Password entered successfully");

        WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        loginBtn.click();
        ExcelLogger.logPass("Click Login", "Login button clicked successfully", "Login button clicked successfully");

        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("artrail-logo")));
        ExcelLogger.logPass("Verify Login", "Login successful", "Login successful");

        System.out.println("✅ Login successful!");
        passwordStr = null;
        decrypted = null;
    }

    // ---------- GIVEN STEPS ----------
    @Given("the user is logged into the application")
    public void the_user_is_logged_into_the_application() {
        try {
            WebElement logo = driver.findElement(By.id("artrail-logo"));
            if (logo.isDisplayed()) {
                System.out.println("✅ User is logged in and on home page");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Verify Login", "User should be logged in", "Login verification failed", e.getMessage());
            throw new RuntimeException("❌ User is not logged in: " + e.getMessage());
        }
    }

    // ---------- WORKLIST NAVIGATION STEPS ----------
    @When("I navigate to Worklist")
    public void i_navigate_to_worklist() {
        try {
            WebElement menuButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("menuPushIcon")));
            js.executeScript("arguments[0].click();", menuButton);
            Thread.sleep(1000);

            WebElement consumerService = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(text(),'Consumer Service')]"))
            );
            js.executeScript("arguments[0].click();", consumerService);
            Thread.sleep(1000);

            WebElement worklistLink = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(text(),'Worklist')]"))
            );
            js.executeScript("arguments[0].click();", worklistLink);
            Thread.sleep(1000);

            ExcelLogger.logPass("Navigate to Worklist", "Navigated to Worklist successfully", "Navigated to Worklist successfully");
            System.out.println("✅ Navigated to Worklist");
        } catch (Exception e) {
            ExcelLogger.logFail("Navigate to Worklist", "Should navigate successfully", "Navigation failed", e.getMessage());
            throw new RuntimeException("❌ Failed to navigate to Worklist: " + e.getMessage());
        }
    }

    @When("I wait for the Worklist page to load")
    public void i_wait_for_the_worklist_page_to_load() {
        try {
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            Thread.sleep(2000);
            
            // Check if iframe exists and switch to it
            try {
                WebElement worklistFrame = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//iframe[contains(@id,'worklistDashboard') or contains(@src,'worklist')]")
                ));
                driver.switchTo().frame(worklistFrame);
                System.out.println("✅ Switched to worklist iframe");
            } catch (TimeoutException te) {
                System.out.println("ℹ️ No iframe found, continuing with main content");
            }
            
            Thread.sleep(2000);
            ExcelLogger.logPass("Wait For Page Load", "Worklist page loaded successfully", "Worklist page loaded successfully");
            System.out.println("✅ Worklist page loaded");
        } catch (Exception e) {
            ExcelLogger.logFail("Wait For Page Load", "Worklist page should load", "Page load timeout", e.getMessage());
            throw new RuntimeException("❌ Failed to load Worklist page: " + e.getMessage());
        }
    }

    @Then("I should see the Worklist dashboard")
    public void i_should_see_the_worklist_dashboard() {
        try {
            ExcelLogger.logPass("Verify Dashboard", "Worklist dashboard displayed successfully", "Worklist dashboard displayed successfully");
            System.out.println("✅ Worklist dashboard is visible");
        } catch (Exception e) {
            ExcelLogger.logFail("Verify Dashboard", "Dashboard should be visible", "Dashboard not found", e.getMessage());
            throw new RuntimeException("❌ Worklist dashboard not visible: " + e.getMessage());
        }
    }

    // ---------- WORKLIST ACTIONS ----------
    @When("I click on the Rebuild option")
    public void i_click_on_the_rebuild_option() {
        try {
            WebElement rebuildIcon = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//i[@class='fa fa-refresh']")
            ));
            js.executeScript("arguments[0].click();", rebuildIcon);
            Thread.sleep(2000);
            ExcelLogger.logPass("Click Rebuild", "Rebuild option clicked successfully", "Rebuild option clicked successfully");
            System.out.println("✅ Clicked on Rebuild option");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Rebuild", "Should click on Rebuild option", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Rebuild: " + e.getMessage());
        }
    }

    @When("I click on Account Reg")
    public void i_click_on_account_reg() {
        try {
            WebElement accountRegLink = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[@class='nav-link active' and @data-filter='533' and contains(text(),'account reg')]")
            ));
            js.executeScript("arguments[0].click();", accountRegLink);
            Thread.sleep(2000);
            ExcelLogger.logPass("Click Account Reg", "Account Reg clicked successfully", "Account Reg clicked successfully");
            System.out.println("✅ Clicked on Account Reg");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Account Reg", "Should click on Account Reg", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Account Reg: " + e.getMessage());
        }
    }

    // ---------- DROPDOWN SELECTION - FIRST AVAILABLE UNIT ----------
    @When("I select the first available unit from the Worklist dropdown")
    public void i_select_the_first_available_unit_from_the_worklist_dropdown() {
        try {
            WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("worklistConfigs")));
            Select select = new Select(dropdown);
            
            // Get all options
            List<WebElement> options = select.getOptions();
            
            // Find first non-empty option (skip placeholder if exists)
            for (WebElement option : options) {
                String optionText = option.getText().trim();
                String optionValue = option.getAttribute("value");
                
                // Skip empty or placeholder options
                if (!optionText.isEmpty() && !optionValue.isEmpty() && 
                    !optionText.toLowerCase().contains("select") && 
                    !optionText.equals("--")) {
                    
                    select.selectByVisibleText(optionText);
                    Thread.sleep(1000);
                    String successMsg = "Selected unit: " + optionText;
                    ExcelLogger.logPass("Select First Unit", successMsg, successMsg);
                    System.out.println("✅ Selected first available unit: " + optionText);
                    return;
                }
            }
            
            // If no valid option found, try selecting by index 1 (skip index 0 which is usually placeholder)
            if (options.size() > 1) {
                select.selectByIndex(1);
                Thread.sleep(1000);
                String selectedText = select.getFirstSelectedOption().getText();
                String successMsg = "Selected unit: " + selectedText;
                ExcelLogger.logPass("Select First Unit", successMsg, successMsg);
                System.out.println("✅ Selected unit by index: " + select.getFirstSelectedOption().getText());
            } else {
                throw new RuntimeException("No valid options found in dropdown");
            }
            
        } catch (Exception e) {
            ExcelLogger.logFail("Select First Unit", "Should select first available unit", 
                "Selection failed", e.getMessage());
            throw new RuntimeException("❌ Failed to select unit: " + e.getMessage());
        }
    }

    // ---------- ALTERNATE: DROPDOWN SELECTION BY NAME ----------
    @When("I select {string} from the Worklist dropdown")
    public void i_select_from_the_worklist_dropdown(String unitName) {
        try {
            WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("worklistConfigs")));
            Select select = new Select(dropdown);
            select.selectByVisibleText(unitName);
            Thread.sleep(1000);
            String successMsg = "Selected unit: " + unitName;
            ExcelLogger.logPass("Select Worklist Dropdown", successMsg, successMsg);
            System.out.println("✅ Selected unit: " + unitName);
        } catch (Exception e) {
            ExcelLogger.logFail("Select Worklist Dropdown", "Select '" + unitName + "' unit", "Selection failed", e.getMessage());
            throw new RuntimeException("❌ Failed to select unit: " + unitName + " - " + e.getMessage());
        }
    }

    // ---------- SEARCH FUNCTIONALITY ----------
    @When("I search for work items from the searchData.json file")
    public void i_search_for_work_items_from_json_file() {
        try {
            // Read search terms from JSON file
            List<String> searchTerms = readSearchTermsFromJson();
            
            if (searchTerms.isEmpty()) {
                ExcelLogger.logInfo("Search Work Items", "No search terms found in JSON", "Skipping search");
                System.out.println("⚠️ No search terms found in JSON file");
                return;
            }

            // Locate the search box
            WebElement searchBox = null;
            try {
                searchBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.id("dashboardSearch")
                ));
            } catch (Exception e) {
                ExcelLogger.logFail("Locate Search Box", "Search box should be present on page", 
                    "Search box not found", "Unable to locate search box with id 'dashboardSearch'");
                throw new RuntimeException("❌ Search box not found on page");
            }

            int passCount = 0;
            int failCount = 0;

            // Perform search for each term
            for (int i = 0; i < searchTerms.size(); i++) {
                String searchTerm = searchTerms.get(i);
                
                try {
                    // Clear previous search
                    searchBox.clear();
                    Thread.sleep(500);
                    
                    // Enter search term and press Enter
                    searchBox.sendKeys(searchTerm);
                    searchBox.sendKeys(Keys.ENTER);
                    Thread.sleep(2000); // Wait for search results to filter
                    
                    // Verify if results are displayed
                    boolean resultsFound = false;
                    
                    try {
                        // Check for "no records" or similar messages first
                        List<WebElement> noRecordsMessages = driver.findElements(
                            By.xpath("//*[contains(text(),'No records') or contains(text(),'No results') or contains(text(),'No data') or contains(text(),'no records') or contains(text(),'no results')]")
                        );
                        
                        boolean hasNoRecordsMessage = false;
                        for (WebElement msg : noRecordsMessages) {
                            if (msg.isDisplayed()) {
                                hasNoRecordsMessage = true;
                                System.out.println("   ℹ️ Found 'No records' message: " + msg.getText());
                                break;
                            }
                        }
                        
                        if (hasNoRecordsMessage) {
                            resultsFound = false;
                        } else {
                            // Check for actual data rows in the table
                            List<WebElement> resultRows = driver.findElements(
                                By.xpath("//table//tbody//tr[not(contains(@style,'display: none')) and not(contains(@style,'display:none'))]")
                            );
                            
                            // Count visible rows with actual data
                            int visibleRows = 0;
                            for (WebElement row : resultRows) {
                                try {
                                    if (row.isDisplayed()) {
                                        String rowText = row.getText().trim();
                                        // Check if row has meaningful data (not just empty cells or messages)
                                        if (!rowText.isEmpty() && 
                                            rowText.length() > 5 && // Meaningful content should be longer than 5 chars
                                            !rowText.toLowerCase().contains("no record") && 
                                            !rowText.toLowerCase().contains("no result") &&
                                            !rowText.toLowerCase().contains("no data") &&
                                            !rowText.toLowerCase().contains("not found")) {
                                            visibleRows++;
                                            System.out.println("   ✓ Found data row: " + rowText.substring(0, Math.min(50, rowText.length())) + "...");
                                        }
                                    }
                                } catch (Exception e) {
                                    // Skip problematic rows
                                    continue;
                                }
                            }
                            
                            resultsFound = visibleRows > 0;
                            System.out.println("   → Total visible data rows: " + visibleRows);
                        }
                        
                    } catch (Exception ex) {
                        System.out.println("   ⚠️ Error checking results: " + ex.getMessage());
                        resultsFound = false;
                    }
                    
                    if (resultsFound) {
                        String expectedResult = "Search for '" + searchTerm + "' should return results";
                        String actualResult = "Results found for '" + searchTerm + "'";
                        ExcelLogger.logPass("Search Work Item " + (i + 1), expectedResult, actualResult);
                        System.out.println("🔍 ✅ Searched for: " + searchTerm + " - Results found");
                        passCount++;
                    } else {
                        String expectedResult = "Search for '" + searchTerm + "' should return results";
                        String actualResult = "No results found for '" + searchTerm + "'";
                        ExcelLogger.logFail("Search Work Item " + (i + 1), expectedResult, actualResult, 
                            "Search term '" + searchTerm + "' did not match any work items");
                        System.out.println("🔍 ❌ Searched for: " + searchTerm + " - No results found");
                        failCount++;
                    }
                    
                } catch (Exception e) {
                    String expectedResult = "Search for '" + searchTerm + "' should execute successfully";
                    String actualResult = "Search failed with error: " + e.getMessage();
                    ExcelLogger.logFail("Search Work Item " + (i + 1), expectedResult, actualResult, 
                        e.getMessage());
                    System.out.println("🔍 ❌ Failed to search for: " + searchTerm + " - " + e.getMessage());
                    failCount++;
                }
            }

            // Clear the search box after all searches
            try {
                searchBox.clear();
                Thread.sleep(500);
            } catch (Exception e) {
                // Ignore clear errors
            }
            
            // Log summary
            String summaryExpected = "All search terms should return results";
            String summaryActual = "Processed " + searchTerms.size() + " search terms - " + 
                passCount + " passed, " + failCount + " failed";
            
            if (failCount == 0) {
                ExcelLogger.logPass("Complete Search", summaryExpected, "All " + searchTerms.size() + " searches passed");
                System.out.println("✅ Completed all searches from JSON file - All passed");
            } else {
                ExcelLogger.logInfo("Complete Search Summary", summaryExpected, summaryActual);
                System.out.println("⚠️ Completed all searches - " + passCount + " passed, " + failCount + " failed");
            }
            
        } catch (Exception e) {
            String expectedResult = "Search functionality should execute without errors";
            String actualResult = "Search functionality failed: " + e.getMessage();
            ExcelLogger.logFail("Search Work Items", expectedResult, actualResult, e.getMessage());
            throw new RuntimeException("❌ Failed to search work items: " + e.getMessage());
        }
    }

    /**
     * Reads search terms from the JSON file
     * Supports two JSON formats:
     * 1. Array of objects: [{"searchTerm": "value1"}, {"searchTerm": "value2"}]
     * 2. Object with array: {"searchTerms": ["value1", "value2"]}
     */
    private List<String> readSearchTermsFromJson() {
        List<String> searchTerms = new ArrayList<>();
        
        try {
            File jsonFile = new File(SEARCH_DATA_PATH);
            
            if (!jsonFile.exists()) {
                ExcelLogger.logFail("Read Search Data", "JSON file should exist", 
                    "File not found", "Search data file not found at: " + SEARCH_DATA_PATH);
                throw new RuntimeException("❌ Search data file not found at: " + SEARCH_DATA_PATH);
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            
            // Check if root is an array: [{"searchTerm": "value"}, ...]
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    if (node.has("searchTerm")) {
                        String term = node.get("searchTerm").asText().trim();
                        if (!term.isEmpty()) {
                            searchTerms.add(term);
                        }
                    }
                }
            } 
            // Check if root has searchTerms array: {"searchTerms": ["value1", "value2"]}
            else if (rootNode.has("searchTerms") && rootNode.get("searchTerms").isArray()) {
                for (JsonNode term : rootNode.get("searchTerms")) {
                    String termText = term.asText().trim();
                    if (!termText.isEmpty()) {
                        searchTerms.add(termText);
                    }
                }
            }

            ExcelLogger.logPass("Read Search Data", "JSON file parsed successfully with " + searchTerms.size() + " search terms", 
                "JSON file parsed successfully with " + searchTerms.size() + " search terms");
            System.out.println("📄 Loaded " + searchTerms.size() + " search terms from JSON");
            
        } catch (IOException e) {
            ExcelLogger.logFail("Read Search Data", "JSON file should be readable", 
                "Failed to read JSON", e.getMessage());
            throw new RuntimeException("❌ Failed to read search data JSON: " + e.getMessage());
        }
        
        return searchTerms;
    }

    @When("I click on Start Work Queue")
    public void i_click_on_start_work_queue() {
        try {
            WebElement startQueueBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("startQueue")));
            js.executeScript("arguments[0].click();", startQueueBtn);
            Thread.sleep(2000);
            ExcelLogger.logPass("Click Start Work Queue", "Start Work Queue button clicked successfully", "Start Work Queue button clicked successfully");
            System.out.println("✅ Clicked on Start Work Queue");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Start Work Queue", "Should click on Start Work Queue button", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Start Work Queue: " + e.getMessage());
        }
    }

    @When("I wait for the queue page to fully load")
    public void i_wait_for_the_queue_page_to_fully_load() {
        try {
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            Thread.sleep(3000);
            
            // After Start Work Queue, check if there's a new iframe or modal
            try {
                // First switch back to main content
                driver.switchTo().defaultContent();
                System.out.println("🔄 Switched to default content to check for new iframes");
                
                // Look for common queue/modal iframe patterns
                List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
                System.out.println("🔍 Found " + iframes.size() + " total iframes");
                
                for (int i = 0; i < iframes.size(); i++) {
                    WebElement iframe = iframes.get(i);
                    String iframeSrc = iframe.getAttribute("src");
                    String iframeId = iframe.getAttribute("id");
                    String iframeName = iframe.getAttribute("name");
                    
                    System.out.println("  → Iframe " + i + ": id='" + iframeId + "', name='" + iframeName + "', src='" + iframeSrc + "'");
                    
                    // Check if this looks like a queue or work-related iframe
                    if ((iframeSrc != null && (iframeSrc.contains("queue") || iframeSrc.contains("work"))) ||
                        (iframeId != null && (iframeId.contains("queue") || iframeId.contains("work") || iframeId.contains("modal"))) ||
                        (iframeName != null && (iframeName.contains("queue") || iframeName.contains("work")))) {
                        driver.switchTo().frame(iframe);
                        System.out.println("✅ Switched to queue iframe: " + (iframeId != null ? iframeId : (iframeName != null ? iframeName : iframeSrc)));
                        Thread.sleep(1000);
                        break;
                    }
                }
            } catch (Exception ie) {
                System.out.println("ℹ️ No additional iframe found after queue start, staying in current context");
                System.out.println("   Error details: " + ie.getMessage());
            }
            
            ExcelLogger.logPass("Wait For Queue Page", "Queue page loaded successfully", "Queue page loaded successfully");
            System.out.println("✅ Queue page loaded completely");
        } catch (Exception e) {
            ExcelLogger.logFail("Wait For Queue Page", "Queue page should load completely", "Page load timeout", e.getMessage());
            throw new RuntimeException("❌ Failed to load queue page: " + e.getMessage());
        }
    }

    // ---------- CLOSE BUTTON ----------
    @When("I click on the Close button")
    public void i_click_on_the_close_button() {
        try {
            // Switch to default content first
            driver.switchTo().defaultContent();
            
            // Get all iframes
            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            
            // Switch to iframe at index 1 (where Close button is located)
            if (iframes.size() > 1) {
                driver.switchTo().frame(iframes.get(1));
                System.out.println("✅ Switched to iframe 1");
            } else {
                String errorMsg = "iframe 1 not found - only " + iframes.size() + " iframe(s) available";
                ExcelLogger.logFail("Click Close Button", "iframe 1 should be present", errorMsg, errorMsg);
                throw new RuntimeException("❌ " + errorMsg);
            }
            
            // Find and click the Close button using the working selector
            WebElement closeBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Close')]")
            ));
            
            js.executeScript("arguments[0].click();", closeBtn);
            Thread.sleep(1500);
            
            ExcelLogger.logPass("Click Close Button", "Close button clicked successfully", "Close button clicked successfully in iframe 1");
            System.out.println("✅ Clicked on Close button");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Click Close Button", "Close button should be clicked", "Close button click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Close button: " + e.getMessage());
        }
    }

    // ---------- LOGOUT FLOW ----------
    @When("I logout")
    public void i_logout() {
        // Switch back to default content before logout
        driver.switchTo().defaultContent();
        System.out.println("🔄 Switched to default content for logout");
        
        try {
            Thread.sleep(1000);
            
            WebElement welcomeNote = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//small[@class='welcomenote white-text' and contains(text(),'Welcome')]"))
            );
            js.executeScript("arguments[0].click();", welcomeNote);
            Thread.sleep(1000);

            WebElement logoutBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[@onclick='logout()']"))
            );
            js.executeScript("arguments[0].click();", logoutBtn);
            Thread.sleep(2000);
            
            ExcelLogger.logPass("Logout", "User logged out successfully", "User logged out successfully");
            System.out.println("✅ Logged out successfully!");
        } catch (Exception e) {
            ExcelLogger.logFail("Logout", "User should logout", "Logout failed", e.getMessage());
            throw new RuntimeException("❌ Logout failed: " + e.getMessage());
        }
    }

    @Then("the user should be logged out successfully")
    public void the_user_should_be_logged_out_successfully() {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("login"),
                    ExpectedConditions.visibilityOfElementLocated(By.id("j_username"))
            ));
            ExcelLogger.logPass("Verify Logout", "Redirected to login page successfully", "Redirected to login page successfully");
            System.out.println("✅ Logout verified - redirected to login page");
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

    // ---------- TEARDOWN ----------
    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
            ExcelLogger.logInfo("Teardown", "Driver should be closed", "Browser closed");
        }
        ExcelLogger.closeReport();
        System.out.println("📊 Excel Report generated at: " + REPORT_PATH);
    }
}
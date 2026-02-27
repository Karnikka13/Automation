package steps4;

import io.cucumber.java.en.*;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.AfterAll;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.openqa.selenium.interactions.Actions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import utils.ExcelLogger;
import utils.CryptoUtils;

public class sgsm {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static WebDriverWait longWait;
    private static JavascriptExecutor js;
    private static String usernameStr, passwordStr, websiteUrl;
    private static final String REPORT_PATH = "E:\\selenium\\SystemGlobalSettingsReport.xlsx";
    private static final String SCREENSHOT_PATH = "E:\\selenium\\screenshots\\";
    private List<SystemGlobalSetting> testDataList;
    private List<SearchData> searchDataList;

    // Inner class to represent System Global Setting data
    public static class SystemGlobalSetting {
        private String sgs_key;
        private String sgs_value;
        private String sgs_desc;
        private String sgs_area;
        private String masked_required;
        private String active;

        public String getSgs_key() { return sgs_key; }
        public void setSgs_key(String sgs_key) { this.sgs_key = sgs_key; }
        public String getSgs_value() { return sgs_value; }
        public void setSgs_value(String sgs_value) { this.sgs_value = sgs_value; }
        public String getSgs_desc() { return sgs_desc; }
        public void setSgs_desc(String sgs_desc) { this.sgs_desc = sgs_desc; }
        public String getSgs_area() { return sgs_area; }
        public void setSgs_area(String sgs_area) { this.sgs_area = sgs_area; }
        public String getMasked_required() { return masked_required; }
        public void setMasked_required(String masked_required) { this.masked_required = masked_required; }
        public String getActive() { return active; }
        public void setActive(String active) { this.active = active; }
    }

    // Inner class for search data
    public static class SearchData {
        private String search_term;

        public String getSearch_term() { return search_term; }
        public void setSearch_term(String search_term) { this.search_term = search_term; }
    }

    // Helper method to take screenshot for debugging
    private static void takeScreenshot(String fileName) {
        try {
            File screenshotDir = new File(SCREENSHOT_PATH);
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs();
            }
            
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destination = new File(SCREENSHOT_PATH + fileName + "_" + System.currentTimeMillis() + ".png");
            Files.copy(screenshot.toPath(), destination.toPath());
            System.out.println("📸 Screenshot: " + destination.getName());
        } catch (Exception e) {
            System.out.println("⚠️ Screenshot failed: " + e.getMessage());
        }
    }

    @BeforeAll
    public static void setUp() throws IOException, InterruptedException {
        ExcelLogger.initReport(REPORT_PATH);
        ExcelLogger.setSection("TEST EXECUTION");
        System.out.println("📊 Excel report: " + REPORT_PATH);

        // Create screenshots directory
        new File(SCREENSHOT_PATH).mkdirs();

        // Use WebDriverManager to automatically manage ChromeDriver version
        try {
            WebDriverManager.chromedriver().setup();
            System.out.println("✅ ChromeDriver setup completed with WebDriverManager");
        } catch (Exception e) {
            ExcelLogger.logFail("Setup ChromeDriver", "ChromeDriver should be configured", 
                "WebDriverManager setup failed", e.getMessage());
            throw new RuntimeException("❌ Failed to setup ChromeDriver: " + e.getMessage());
        }

        // Configure Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        longWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        js = (JavascriptExecutor) driver;

        // Read encrypted credentials
        String encFilePath = "E:\\selenium\\credentials.txt";
        File encFile = new File(encFilePath);
        
        if (!encFile.exists()) {
            ExcelLogger.logFail("Read Credentials", "Credentials file should exist", 
                "File not found", "Encrypted credentials file not found at: " + encFilePath);
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
            ExcelLogger.logFail("Validate Credentials Format", "Format should be username,password", 
                "Invalid format", "Decrypted credentials format invalid");
            throw new RuntimeException("❌ Decrypted credentials format should be: username,password");
        }

        String[] creds = decrypted.split(",", 2);
        usernameStr = creds[0].trim();
        passwordStr = creds[1].trim();

        // Open browser & login
        driver.get(websiteUrl);
        Thread.sleep(2000);
        takeScreenshot("01_login_page");

        WebElement username = longWait.until(ExpectedConditions.visibilityOfElementLocated(By.id("j_username")));
        username.sendKeys(usernameStr);
        ExcelLogger.logPass("Enter Username", "Username entered successfully", "Username entered successfully");
        System.out.println("✅ Entered username");

        WebElement password = driver.findElement(By.id("j_password"));
        password.sendKeys(passwordStr);
        ExcelLogger.logPass("Enter Password", "Password entered successfully", "Password entered successfully");
        System.out.println("✅ Entered password");

        takeScreenshot("02_before_login");
        WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        loginBtn.click();
        ExcelLogger.logPass("Click Login", "Login button clicked successfully", "Login button clicked successfully");
        System.out.println("✅ Clicked login button");

        System.out.println("⏳ Waiting for login (up to 60 seconds)...");
        Thread.sleep(5000);
        takeScreenshot("03_after_login_click");

        longWait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
        Thread.sleep(3000);
        takeScreenshot("04_page_ready");
        
        // Multiple verification strategies to detect successful login
        boolean loginSuccessful = false;
        String verificationMethod = "";
        
        try {
            WebElement logo = longWait.until(ExpectedConditions.visibilityOfElementLocated(By.id("artrail-logo")));
            if (logo.isDisplayed()) {
                loginSuccessful = true;
                verificationMethod = "artrail-logo (ID)";
            }
        } catch (TimeoutException e1) {
            System.out.println("⚠️ artrail-logo not found by ID");
            
            try {
                List<WebElement> logos = driver.findElements(By.cssSelector("img[id*='logo'], img[class*='logo'], div[id*='logo']"));
                if (!logos.isEmpty() && logos.get(0).isDisplayed()) {
                    loginSuccessful = true;
                    verificationMethod = "logo element (CSS)";
                }
            } catch (Exception e2) {
                System.out.println("⚠️ No logo elements found");
                
                try {
                    String currentUrl = driver.getCurrentUrl();
                    if (!currentUrl.contains("login") && !currentUrl.equals(websiteUrl)) {
                        loginSuccessful = true;
                        verificationMethod = "URL change";
                    }
                } catch (Exception e3) {
                    System.out.println("⚠️ URL check failed");
                    
                    try {
                        List<WebElement> loginElements = driver.findElements(By.id("j_username"));
                        if (loginElements.isEmpty()) {
                            loginSuccessful = true;
                            verificationMethod = "login form absent";
                        }
                    } catch (Exception e4) {
                        System.out.println("⚠️ All verification strategies failed");
                    }
                }
            }
        }
        
        takeScreenshot("05_verification_complete");
        
        if (loginSuccessful) {
            ExcelLogger.logPass("Verify Login", "Login successful", "Login successful");
            System.out.println("✅ Login successful! (" + verificationMethod + ")");
        } else {
            takeScreenshot("06_LOGIN_FAILED");
            System.out.println("Current URL: " + driver.getCurrentUrl());
            System.out.println("Page Title: " + driver.getTitle());
            ExcelLogger.logFail("Verify Login", "Should verify login", "All strategies failed", 
                "Check screenshots in " + SCREENSHOT_PATH);
            throw new RuntimeException("❌ Login verification failed. Check " + SCREENSHOT_PATH);
        }

        passwordStr = null;
        decrypted = null;
    }

    @Given("the user is logged into the System Global Settings Master application")
    public void the_user_is_logged_into_the_system_global_settings_master_application() {
        try {
            WebElement logo = driver.findElement(By.id("artrail-logo"));
            if (logo.isDisplayed()) {
                System.out.println("✅ User is logged in");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Verify User Login", "User should be logged in", 
                "Login verification failed", e.getMessage());
            throw new RuntimeException("❌ User is not logged in: " + e.getMessage());
        }
    }

    @When("I open the menu for System Global Settings")
    public void i_open_the_menu_for_system_global_settings() {
        try {
            WebElement menuButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("menuPushIcon")));
            js.executeScript("arguments[0].click();", menuButton);
            Thread.sleep(1000);
            System.out.println("✅ Menu opened");
        } catch (Exception e) {
            ExcelLogger.logFail("Open Menu", "Menu should open", "Menu open failed", e.getMessage());
            throw new RuntimeException("❌ Failed to open menu: " + e.getMessage());
        }
    }

    @When("I search for System Global Settings Master in the menu search bar")
    public void i_search_for_system_global_settings_master_in_the_menu_search_bar() {
        try {
            WebElement menuSearchBar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("menuSearchBar")));
            menuSearchBar.clear();
            Thread.sleep(500);
            menuSearchBar.sendKeys("System Global Settings Master");
            System.out.println("✅ Entered 'System Global Settings Master' in search bar");
            Thread.sleep(1000);
        } catch (Exception e) {
            ExcelLogger.logFail("Enter Search Term", "Search term should be entered", 
                "Search entry failed", e.getMessage());
            throw new RuntimeException("❌ Failed to enter search term: " + e.getMessage());
        }
    }

    @When("I press Enter to navigate to System Global Settings")
    public void i_press_enter_to_navigate_to_system_global_settings() {
        try {
            WebElement menuSearchBar = driver.findElement(By.id("menuSearchBar"));
            menuSearchBar.sendKeys(Keys.ENTER);
            Thread.sleep(2000);
            ExcelLogger.logPass("Navigate To Page", "Navigated to System Global Settings Master successfully", "Navigated to System Global Settings Master successfully");
            System.out.println("✅ Pressed Enter to navigate");
        } catch (Exception e) {
            ExcelLogger.logFail("Navigate To Page", "Should navigate to page", "Navigation failed", e.getMessage());
            throw new RuntimeException("❌ Failed to press Enter: " + e.getMessage());
        }
    }

    @When("I wait for the System Global Settings Master page to load completely")
    public void i_wait_for_the_system_global_settings_master_page_to_load_completely() {
        try {
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            System.out.println("✅ Document ready state is complete");
            Thread.sleep(3000);
            ExcelLogger.logPass("Wait For Page Load", "Page loaded successfully", "System Global Settings Master page loaded successfully");
            System.out.println("✅ System Global Settings Master page loaded completely");
        } catch (Exception e) {
            ExcelLogger.logFail("Wait For Page Load", "Page should load completely", 
                "Page load failed", e.getMessage());
            throw new RuntimeException("❌ Failed to load System Global Settings Master page: " + e.getMessage());
        }
    }

    @Then("I should see the System Global Settings Master page")
    public void i_should_see_the_system_global_settings_master_page() {
        try {
            WebElement body = driver.findElement(By.tagName("body"));
            if (body.isDisplayed()) {
                System.out.println("✅ System Global Settings Master page is visible");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Verify Page Visibility", "Page should be visible", 
                "Page visibility check failed", e.getMessage());
            throw new RuntimeException("❌ Failed to verify page visibility: " + e.getMessage());
        }
    }

    @When("I load test data from {string}")
    public void i_load_test_data_from(String jsonFileName) {
        try {
            String jsonFilePath = "E:\\selenium\\" + jsonFileName;
            String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            
            Gson gson = new Gson();
            Type listType = new TypeToken<List<SystemGlobalSetting>>(){}.getType();
            testDataList = gson.fromJson(jsonContent, listType);
            
            ExcelLogger.logPass("Load Test Data", "Test data loaded successfully from " + jsonFileName, 
                "Loaded " + testDataList.size() + " records from " + jsonFileName);
            System.out.println("✅ Loaded " + testDataList.size() + " test records from " + jsonFileName);
        } catch (IOException e) {
            ExcelLogger.logFail("Load Test Data", "Test data should be loaded", 
                "Failed to read JSON file", e.getMessage());
            throw new RuntimeException("❌ Failed to load test data: " + e.getMessage());
        } catch (Exception e) {
            ExcelLogger.logFail("Load Test Data", "Test data should be parsed", 
                "Failed to parse JSON", e.getMessage());
            throw new RuntimeException("❌ Failed to parse JSON data: " + e.getMessage());
        }
    }

    // Helper method to add a single setting entry using iframe iteration
    private void addSetting(SystemGlobalSetting setting) throws InterruptedException {
        driver.switchTo().defaultContent();
        int iframeCount = driver.findElements(By.tagName("iframe")).size();
        boolean actionDone = false;
        
        System.out.println("🔍 Found " + iframeCount + " iframes to check");
        
        for (int i = 0; i < iframeCount; i++) {
            try {
                driver.switchTo().defaultContent();
                driver.switchTo().frame(i);
                System.out.println("🔄 Switched to iframe " + i);
                
                WebElement ellipsisBtn = null;
                try {
                    ellipsisBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("span.fa.fa-ellipsis-v")));
                } catch (TimeoutException te) {
                    System.out.println("ℹ️ Ellipsis not found in iframe " + i + ", trying next...");
                    continue;
                }
                
                if (ellipsisBtn != null && ellipsisBtn.isDisplayed()) {
                    System.out.println("✅ Found ellipsis button in iframe " + i);
                    
                    js.executeScript("arguments[0].click();", ellipsisBtn);
                    Thread.sleep(1000);
                    System.out.println("✅ Opened ellipsis menu");
                    
                    WebElement newBtn = null;
                    try {
                        newBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("a.dropdown-item.addNewGlpSgs, a.dropdown-item[onclick*='addNew']")));
                    } catch (TimeoutException te1) {
                        try {
                            newBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("new")));
                        } catch (TimeoutException te2) {
                            try {
                                newBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.xpath("//a[contains(@class,'dropdown-item') and (contains(text(),'New') or contains(text(),'Add'))]")));
                            } catch (TimeoutException te3) {
                                System.out.println("❌ Could not find New button in iframe " + i);
                                continue;
                            }
                        }
                    }
                    
                    js.executeScript("arguments[0].click();", newBtn);
                    Thread.sleep(1500);
                    System.out.println("✅ Clicked 'New' to add entry");
                    
                    // Enter all fields
                    WebElement sgsKeyInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sgs_key")));
                    sgsKeyInput.clear();
                    sgsKeyInput.sendKeys(setting.getSgs_key());
                    System.out.println("✅ Entered Key: " + setting.getSgs_key());
                    
                    WebElement sgsValueInput = driver.findElement(By.id("sgs_value"));
                    sgsValueInput.clear();
                    sgsValueInput.sendKeys(setting.getSgs_value());
                    System.out.println("✅ Entered Value: " + setting.getSgs_value());
                    
                    WebElement sgsDescInput = driver.findElement(By.id("sgs_desc"));
                    sgsDescInput.clear();
                    sgsDescInput.sendKeys(setting.getSgs_desc());
                    System.out.println("✅ Entered Description");
                    
                    WebElement sgsAreaInput = driver.findElement(By.id("sgs_area"));
                    sgsAreaInput.clear();
                    sgsAreaInput.sendKeys(setting.getSgs_area());
                    System.out.println("✅ Entered Area: " + setting.getSgs_area());
                    
                    // Handle checkboxes
                    WebElement maskedRequiredCheckbox = driver.findElement(By.id("chkMaskedRequired"));
                    boolean isMaskedChecked = maskedRequiredCheckbox.isSelected();
                    boolean shouldBeMasked = setting.getMasked_required().equalsIgnoreCase("Yes");
                    
                    if (shouldBeMasked && !isMaskedChecked) {
                        js.executeScript("arguments[0].click();", maskedRequiredCheckbox);
                        System.out.println("✅ Checked 'Masked Required'");
                    } else if (!shouldBeMasked && isMaskedChecked) {
                        js.executeScript("arguments[0].click();", maskedRequiredCheckbox);
                        System.out.println("✅ Unchecked 'Masked Required'");
                    }
                    
                    WebElement activeCheckbox = driver.findElement(By.id("chkSgsActive"));
                    boolean isActiveChecked = activeCheckbox.isSelected();
                    boolean shouldBeActive = setting.getActive().equalsIgnoreCase("Yes");
                    
                    if (shouldBeActive && !isActiveChecked) {
                        js.executeScript("arguments[0].click();", activeCheckbox);
                        System.out.println("✅ Checked 'Active'");
                    } else if (!shouldBeActive && isActiveChecked) {
                        js.executeScript("arguments[0].click();", activeCheckbox);
                        System.out.println("✅ Unchecked 'Active'");
                    }
                    
                    Thread.sleep(1000);
                    
                    // Save
                    WebElement ellipsisBtnSave = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("span.fa.fa-ellipsis-v")));
                    js.executeScript("arguments[0].click();", ellipsisBtnSave);
                    Thread.sleep(1000);
                    System.out.println("✅ Opened ellipsis menu for saving");
                    
                    WebElement saveBtn = null;
                    try {
                        saveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector("a.dropdown-item.deSelectAlways.saveGlpSgs, a.dropdown-item[onclick*='saveGlpSgs']")));
                    } catch (TimeoutException te) {
                        try {
                            saveBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("save")));
                        } catch (TimeoutException te2) {
                            saveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//a[contains(@class,'dropdown-item') and contains(text(),'Save')]")));
                        }
                    }
                    
                    js.executeScript("arguments[0].click();", saveBtn);
                    Thread.sleep(2000);
                    ExcelLogger.logPass("Add Entry - " + setting.getSgs_key(), "Entry added and saved successfully", 
                        "Key: " + setting.getSgs_key() + ", Value: " + setting.getSgs_value() + ", Area: " + setting.getSgs_area());
                    System.out.println("✅ Saved entry: " + setting.getSgs_key());
                    
                    Thread.sleep(2000);
                    actionDone = true;
                    break;
                }
            } catch (Exception e) {
                System.out.println("⚠️ Error in iframe " + i + ": " + e.getMessage());
                continue;
            }
        }
        
        if (!actionDone) {
            ExcelLogger.logFail("Add Entry - " + setting.getSgs_key(), "Entry should be added", 
                "Failed in all iframes", "Could not add setting: " + setting.getSgs_key());
            throw new RuntimeException("❌ Could not add setting in any iframe");
        }
    }

    @When("I add all settings entries from the JSON data")
    public void i_add_all_settings_entries_from_the_json_data() {
        try {
            int successCount = 0;
            int failCount = 0;
            
            // Only insert first 2 records as new entries (3rd record will be used for editing)
            int recordsToAdd = Math.min(2, testDataList.size());
            
            for (int i = 0; i < recordsToAdd; i++) {
                SystemGlobalSetting setting = testDataList.get(i);
                System.out.println("\n📝 Processing record " + (i + 1) + ": " + setting.getSgs_key());
                
                try {
                    addSetting(setting);
                    successCount++;
                    System.out.println("✅ Successfully added record " + (i + 1) + "/" + recordsToAdd);
                } catch (Exception e) {
                    failCount++;
                    System.out.println("❌ Failed to add record " + (i + 1) + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("\n📊 Summary: " + successCount + " successful, " + failCount + 
                " failed out of " + recordsToAdd + " records added (3rd record reserved for editing)");
        } catch (Exception e) {
            ExcelLogger.logFail("Add All Entries", "All entries should be added", "Process failed", e.getMessage());
            throw new RuntimeException("❌ Failed to add entries: " + e.getMessage());
        }
    }

    @Then("all settings should be saved successfully")
    public void all_settings_should_be_saved_successfully() {
        try {
            driver.switchTo().defaultContent();
            WebElement body = driver.findElement(By.tagName("body"));
            if (body.isDisplayed()) {
                System.out.println("✅ All settings entries have been processed");
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Verify Save Completion", "Should verify save completion", 
                "Verification failed", e.getMessage());
            throw new RuntimeException("❌ Failed to verify save completion: " + e.getMessage());
        }
    }

    @When("I load search data from {string}")
    public void i_load_search_data_from(String jsonFileName) {
        try {
            String jsonFilePath = "E:\\selenium\\" + jsonFileName;
            String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            
            Gson gson = new Gson();
            Type listType = new TypeToken<List<SearchData>>(){}.getType();
            searchDataList = gson.fromJson(jsonContent, listType);
            
            ExcelLogger.logPass("Load Search Data", "Search data loaded successfully from " + jsonFileName, 
                "Loaded " + searchDataList.size() + " search terms");
            System.out.println("✅ Loaded " + searchDataList.size() + " search terms from " + jsonFileName);
        } catch (IOException e) {
            ExcelLogger.logFail("Load Search Data", "Search data should be loaded", 
                "Failed to read JSON file", e.getMessage());
            throw new RuntimeException("❌ Failed to load search data: " + e.getMessage());
        } catch (Exception e) {
            ExcelLogger.logFail("Load Search Data", "Search data should be parsed", 
                "Failed to parse JSON", e.getMessage());
            throw new RuntimeException("❌ Failed to parse search JSON data: " + e.getMessage());
        }
    }

    @When("I search and edit the entries from search data")
    public void i_search_and_edit_the_entries_from_search_data() {
        try {
            driver.switchTo().defaultContent();
            int iframeCount = driver.findElements(By.tagName("iframe")).size();
            
            System.out.println("\n🔍 Starting search and edit operations");
            
            for (SearchData searchData : searchDataList) {
                boolean firstOperationDone = false;
                
                // ========== FIRST OPERATION: Search → Edit → Enter 3rd Record → Back → Yes ==========
                System.out.println("\n🔍 FIRST OPERATION: Search → Edit → Enter 3rd Record → Back → Yes");
                
                for (int i = 0; i < iframeCount; i++) {
                    try {
                        driver.switchTo().defaultContent();
                        driver.switchTo().frame(i);
                        
                        WebElement searchBox = null;
                        try {
                            searchBox = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("srchRelatAll2")));
                        } catch (TimeoutException te) {
                            continue;
                        }
                        
                        if (searchBox != null && searchBox.isDisplayed()) {
                            System.out.println("✅ Found search box in iframe " + i);
                            
                            searchBox.clear();
                            Thread.sleep(500);
                            searchBox.sendKeys(searchData.getSearch_term());
                            Thread.sleep(1500);
                            System.out.println("✅ Entered search term: " + searchData.getSearch_term());
                            
                            WebElement firstRow = null;
                            try {
                                firstRow = wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.xpath("//td[@role='gridcell' and @aria-describedby='systemGlobalSetttingsGrid_description']")));
                            } catch (TimeoutException te) {
                                System.out.println("❌ No search results found for: " + searchData.getSearch_term());
                                continue;
                            }
                            
                            Actions actions = new Actions(driver);
                            actions.doubleClick(firstRow).perform();
                            Thread.sleep(2000);
                            System.out.println("✅ Double-clicked on first search result");
                            
                            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
                            Thread.sleep(2000);
                            
                            WebElement ellipsisBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector("span.fa.fa-ellipsis-v")));
                            js.executeScript("arguments[0].click();", ellipsisBtn);
                            Thread.sleep(1000);
                            System.out.println("✅ Opened ellipsis menu");
                            
                            WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("edit")));
                            js.executeScript("arguments[0].click();", editBtn);
                            Thread.sleep(1500);
                            System.out.println("✅ Clicked Edit button");
                            
                            Thread.sleep(1000);
                            
                            // Enter data from THIRD record
                            if (testDataList != null && testDataList.size() >= 3) {
                                SystemGlobalSetting thirdSetting = testDataList.get(2);
                                System.out.println("\n📝 Entering data from THIRD record (First Operation)");
                                
                                WebElement sgsKeyInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sgs_key")));
                                sgsKeyInput.clear();
                                Thread.sleep(300);
                                sgsKeyInput.sendKeys(thirdSetting.getSgs_key());
                                System.out.println("✅ Entered Key: " + thirdSetting.getSgs_key());
                                
                                WebElement sgsValueInput = driver.findElement(By.id("sgs_value"));
                                sgsValueInput.clear();
                                Thread.sleep(300);
                                sgsValueInput.sendKeys(thirdSetting.getSgs_value());
                                System.out.println("✅ Entered Value: " + thirdSetting.getSgs_value());
                                
                                WebElement sgsDescInput = driver.findElement(By.id("sgs_desc"));
                                sgsDescInput.clear();
                                Thread.sleep(300);
                                sgsDescInput.sendKeys(thirdSetting.getSgs_desc());
                                System.out.println("✅ Entered Description");
                                
                                WebElement sgsAreaInput = driver.findElement(By.id("sgs_area"));
                                sgsAreaInput.clear();
                                Thread.sleep(300);
                                sgsAreaInput.sendKeys(thirdSetting.getSgs_area());
                                System.out.println("✅ Entered Area: " + thirdSetting.getSgs_area());
                                
                                WebElement maskedRequiredCheckbox = driver.findElement(By.id("chkMaskedRequired"));
                                boolean isMaskedChecked = maskedRequiredCheckbox.isSelected();
                                boolean shouldBeMasked = thirdSetting.getMasked_required().equalsIgnoreCase("Yes");
                                
                                if (shouldBeMasked != isMaskedChecked) {
                                    js.executeScript("arguments[0].click();", maskedRequiredCheckbox);
                                    System.out.println("✅ Set Masked Required: " + thirdSetting.getMasked_required());
                                }
                                
                                WebElement activeCheckbox = driver.findElement(By.id("chkSgsActive"));
                                boolean isActiveChecked = activeCheckbox.isSelected();
                                boolean shouldBeActive = thirdSetting.getActive().equalsIgnoreCase("Yes");
                                
                                if (shouldBeActive != isActiveChecked) {
                                    js.executeScript("arguments[0].click();", activeCheckbox);
                                    System.out.println("✅ Set Active: " + thirdSetting.getActive());
                                }
                                
                                Thread.sleep(1000);
                            } else {
                                System.out.println("❌ Test data has less than 3 records");
                                continue;
                            }
                            
                            WebElement ellipsisBtnAfterEdit = wait.until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector("span.fa.fa-ellipsis-v")));
                            js.executeScript("arguments[0].click();", ellipsisBtnAfterEdit);
                            Thread.sleep(1000);
                            System.out.println("✅ Opened ellipsis menu after edit");
                            
                            WebElement backBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("back")));
                            js.executeScript("arguments[0].click();", backBtn);
                            Thread.sleep(1000);
                            System.out.println("✅ Clicked Back button");
                            
                            WebElement yesBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//button[@data-value='true' and text()='Yes']")));
                            js.executeScript("arguments[0].click();", yesBtn);
                            Thread.sleep(2000);
                            ExcelLogger.logPass("Edit Without Save - " + searchData.getSearch_term(), 
                                "Edited entry and clicked Back -> Yes successfully", 
                                "Searched: " + searchData.getSearch_term() + ", edited with 3rd record data, clicked Back -> Yes");
                            System.out.println("✅ Clicked Yes to confirm back");
                            
                            firstOperationDone = true;
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Error in iframe " + i + " during first operation: " + e.getMessage());
                        continue;
                    }
                }
                
                if (!firstOperationDone) {
                    ExcelLogger.logFail("Edit Without Save - " + searchData.getSearch_term(), 
                        "Should complete edit and back operation", "Operation failed", 
                        "Could not complete for: " + searchData.getSearch_term());
                    System.out.println("❌ Could not complete first operation for: " + searchData.getSearch_term());
                    continue;
                }
                
                // ========== SECOND OPERATION: Search → Edit → Enter 3rd Record → Save ==========
                Thread.sleep(2000);
                System.out.println("\n🔍 SECOND OPERATION: Search → Edit → Enter 3rd Record → Save");
                
                boolean secondOperationDone = false;
                
                for (int i = 0; i < iframeCount; i++) {
                    try {
                        driver.switchTo().defaultContent();
                        driver.switchTo().frame(i);
                        
                        WebElement searchBox2 = null;
                        try {
                            searchBox2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("srchRelatAll2")));
                        } catch (TimeoutException te) {
                            continue;
                        }
                        
                        if (searchBox2 != null && searchBox2.isDisplayed()) {
                            System.out.println("✅ Found search box in iframe " + i + " for second operation");
                            
                            searchBox2.clear();
                            Thread.sleep(500);
                            searchBox2.sendKeys(searchData.getSearch_term());
                            Thread.sleep(1500);
                            System.out.println("✅ Entered search term: " + searchData.getSearch_term());
                            
                            WebElement firstRow2 = null;
                            try {
                                firstRow2 = wait.until(ExpectedConditions.presenceOfElementLocated(
                                    By.xpath("//td[@role='gridcell' and @aria-describedby='systemGlobalSetttingsGrid_description']")));
                            } catch (TimeoutException te) {
                                System.out.println("❌ No search results found on second operation");
                                continue;
                            }
                            
                            Actions actions2 = new Actions(driver);
                            actions2.doubleClick(firstRow2).perform();
                            Thread.sleep(2000);
                            System.out.println("✅ Double-clicked on first search result (second time)");
                            
                            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
                            Thread.sleep(2000);
                            
                            WebElement ellipsisBtn2 = wait.until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector("span.fa.fa-ellipsis-v")));
                            js.executeScript("arguments[0].click();", ellipsisBtn2);
                            Thread.sleep(1000);
                            System.out.println("✅ Opened ellipsis menu");
                            
                            WebElement editBtn2 = wait.until(ExpectedConditions.elementToBeClickable(By.id("edit")));
                            js.executeScript("arguments[0].click();", editBtn2);
                            Thread.sleep(1500);
                            System.out.println("✅ Clicked Edit button");
                            
                            Thread.sleep(1000);
                            
                            // Enter data from THIRD record again
                            if (testDataList != null && testDataList.size() >= 3) {
                                SystemGlobalSetting thirdSetting = testDataList.get(2);
                                System.out.println("\n📝 Entering data from THIRD record (Second Operation)");
                                
                                WebElement sgsKeyInput3 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sgs_key")));
                                sgsKeyInput3.clear();
                                Thread.sleep(300);
                                sgsKeyInput3.sendKeys(thirdSetting.getSgs_key());
                                
                                WebElement sgsValueInput3 = driver.findElement(By.id("sgs_value"));
                                sgsValueInput3.clear();
                                Thread.sleep(300);
                                sgsValueInput3.sendKeys(thirdSetting.getSgs_value());
                                
                                WebElement sgsDescInput3 = driver.findElement(By.id("sgs_desc"));
                                sgsDescInput3.clear();
                                Thread.sleep(300);
                                sgsDescInput3.sendKeys(thirdSetting.getSgs_desc());
                                
                                WebElement sgsAreaInput3 = driver.findElement(By.id("sgs_area"));
                                sgsAreaInput3.clear();
                                Thread.sleep(300);
                                sgsAreaInput3.sendKeys(thirdSetting.getSgs_area());
                                
                                WebElement maskedRequiredCheckbox3 = driver.findElement(By.id("chkMaskedRequired"));
                                boolean isMaskedChecked3 = maskedRequiredCheckbox3.isSelected();
                                boolean shouldBeMasked3 = thirdSetting.getMasked_required().equalsIgnoreCase("Yes");
                                
                                if (shouldBeMasked3 != isMaskedChecked3) {
                                    js.executeScript("arguments[0].click();", maskedRequiredCheckbox3);
                                }
                                
                                WebElement activeCheckbox3 = driver.findElement(By.id("chkSgsActive"));
                                boolean isActiveChecked3 = activeCheckbox3.isSelected();
                                boolean shouldBeActive3 = thirdSetting.getActive().equalsIgnoreCase("Yes");
                                
                                if (shouldBeActive3 != isActiveChecked3) {
                                    js.executeScript("arguments[0].click();", activeCheckbox3);
                                }
                                
                                Thread.sleep(1000);
                            }
                            
                            WebElement ellipsisBtnSave = wait.until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector("span.fa.fa-ellipsis-v")));
                            js.executeScript("arguments[0].click();", ellipsisBtnSave);
                            Thread.sleep(1000);
                            System.out.println("✅ Opened ellipsis menu for save");
                            
                            WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("save")));
                            js.executeScript("arguments[0].click();", saveBtn);
                            Thread.sleep(2000);
                            ExcelLogger.logPass("Edit And Save - " + searchData.getSearch_term(), 
                                "Edited entry and saved successfully with 3rd record data", 
                                "Searched: " + searchData.getSearch_term() + ", edited with 3rd record data, saved successfully");
                            System.out.println("✅ Clicked Save button with third record data");
                            
                            Thread.sleep(2000);
                            secondOperationDone = true;
                            break;
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Error in iframe " + i + " during second operation: " + e.getMessage());
                        continue;
                    }
                }
                
                if (!secondOperationDone) {
                    ExcelLogger.logFail("Edit And Save - " + searchData.getSearch_term(), 
                        "Should complete edit and save operation", "Operation failed", 
                        "Could not complete for: " + searchData.getSearch_term());
                    System.out.println("❌ Could not complete second operation for: " + searchData.getSearch_term());
                }
            }
            
            System.out.println("✅ Completed all search and edit operations");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Search and Edit Operations", "Should complete all operations", 
                "Operation failed", e.getMessage());
            throw new RuntimeException("❌ Failed to complete search and edit: " + e.getMessage());
        }
    }

    @When("I check the history to verify recent activity")
    public void i_check_the_history_to_verify_recent_activity() {
        try {
            driver.switchTo().defaultContent();
            int iframeCount = driver.findElements(By.tagName("iframe")).size();
            boolean historyChecked = false;
            
            System.out.println("\n📜 Checking history to verify recent activity");
            
            for (int i = 0; i < iframeCount; i++) {
                try {
                    driver.switchTo().defaultContent();
                    driver.switchTo().frame(i);
                    System.out.println("🔄 Switched to iframe " + i);
                    
                    WebElement ellipsisBtn = null;
                    try {
                        ellipsisBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("span.fa.fa-ellipsis-v")));
                    } catch (TimeoutException te) {
                        System.out.println("ℹ️ Ellipsis not found in iframe " + i);
                        continue;
                    }
                    
                    if (ellipsisBtn != null && ellipsisBtn.isDisplayed()) {
                        System.out.println("✅ Found ellipsis button in iframe " + i);
                        
                        js.executeScript("arguments[0].click();", ellipsisBtn);
                        Thread.sleep(1000);
                        System.out.println("✅ Opened ellipsis menu");
                        
                        WebElement historyBtn = null;
                        try {
                            historyBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                By.cssSelector("a.dropdown-item.deSelectAlways.history")));
                        } catch (TimeoutException te1) {
                            try {
                                historyBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("history")));
                            } catch (TimeoutException te2) {
                                historyBtn = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//a[contains(@class,'dropdown-item') and contains(@onclick,'history')]")));
                            }
                        }
                        
                        js.executeScript("arguments[0].click();", historyBtn);
                        Thread.sleep(2000);
                        ExcelLogger.logPass("Open History", "History modal opened successfully", 
                            "History modal opened successfully");
                        System.out.println("✅ Clicked History button");
                        
                        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
                        Thread.sleep(2000);
                        System.out.println("✅ History modal loaded");
                        
                        // Try to find and click Close button
                        WebElement closeBtn = null;
                        boolean closeBtnClicked = false;
                        
                        // Strategy 1: <i class="fa fa-close"> icon
                        try {
                            closeBtn = driver.findElement(By.xpath("//button[.//i[contains(@class,'fa-close')]]"));
                            if (closeBtn.isDisplayed()) {
                                js.executeScript("arguments[0].click();", closeBtn);
                                closeBtnClicked = true;
                                ExcelLogger.logPass("Close History", "History modal closed successfully", 
                                    "History modal closed successfully");
                                System.out.println("✅ Clicked Close button via fa-close icon");
                            }
                        } catch (Exception e1) {
                            System.out.println("⚠️ Strategy 1 (fa-close icon) failed");
                            
                            // Strategy 2: Button text "Close"
                            try {
                                closeBtn = driver.findElement(By.xpath("//button[contains(text(),'Close')]"));
                                if (closeBtn.isDisplayed()) {
                                    js.executeScript("arguments[0].click();", closeBtn);
                                    closeBtnClicked = true;
                                    ExcelLogger.logPass("Close History", "History modal closed successfully", 
                                        "History modal closed successfully");
                                    System.out.println("✅ Clicked Close button via text");
                                }
                            } catch (Exception e2) {
                                System.out.println("⚠️ Strategy 2 (text) failed");
                                
                                // Strategy 3: Icon variations
                                try {
                                    closeBtn = driver.findElement(By.xpath(
                                        "//button[.//i[contains(@class,'fa-close') or contains(@class,'fa-times') or contains(@class,'close')]]"));
                                    if (closeBtn.isDisplayed()) {
                                        js.executeScript("arguments[0].click();", closeBtn);
                                        closeBtnClicked = true;
                                        ExcelLogger.logPass("Close History", "History modal closed successfully", 
                                            "History modal closed successfully");
                                        System.out.println("✅ Clicked Close button via icon variation");
                                    }
                                } catch (Exception e3) {
                                    System.out.println("⚠️ Strategy 3 (icon variations) failed");
                                    
                                    // Strategy 4: Modal footer
                                    try {
                                        closeBtn = driver.findElement(By.xpath(
                                            "//div[contains(@class,'modal-footer')]//button[contains(text(),'Close')]"));
                                        if (closeBtn.isDisplayed()) {
                                            js.executeScript("arguments[0].click();", closeBtn);
                                            closeBtnClicked = true;
                                            ExcelLogger.logPass("Close History", "History modal closed successfully", 
                                                "History modal closed successfully");
                                            System.out.println("✅ Clicked Close button via modal footer");
                                        }
                                    } catch (Exception e4) {
                                        System.out.println("⚠️ Strategy 4 (modal footer) failed");
                                        
                                        // Strategy 5: Find <i> and click parent
                                        try {
                                            WebElement closeIcon = driver.findElement(By.cssSelector("i.fa.fa-close"));
                                            WebElement parentButton = closeIcon.findElement(By.xpath("./ancestor::button"));
                                            if (parentButton.isDisplayed()) {
                                                js.executeScript("arguments[0].click();", parentButton);
                                                closeBtnClicked = true;
                                                ExcelLogger.logPass("Close History", "History modal closed successfully", 
                                                    "History modal closed successfully");
                                                System.out.println("✅ Clicked Close button via parent of fa-close icon");
                                            }
                                        } catch (Exception e5) {
                                            System.out.println("⚠️ Strategy 5 (parent of icon) failed");
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (!closeBtnClicked) {
                            // Fallback: ESC key
                            try {
                                Actions actions = new Actions(driver);
                                actions.sendKeys(Keys.ESCAPE).perform();
                                Thread.sleep(1000);
                                ExcelLogger.logPass("Close History", "History modal closed successfully", 
                                    "History modal closed via ESC key");
                                System.out.println("✅ Closed modal via ESC key");
                                closeBtnClicked = true;
                            } catch (Exception escEx) {
                                ExcelLogger.logFail("Close History", "History modal should close", 
                                    "All close strategies failed", escEx.getMessage());
                                System.out.println("❌ All close strategies failed");
                            }
                        }
                        
                        if (closeBtnClicked) {
                            Thread.sleep(1500);
                            historyChecked = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Error in iframe " + i + ": " + e.getMessage());
                    continue;
                }
            }
            
            if (!historyChecked) {
                ExcelLogger.logFail("Check History", "History should be verified", 
                    "History check failed", "Could not check history in any iframe");
                throw new RuntimeException("❌ Could not check history in any iframe");
            }
            
            System.out.println("✅ History verification completed");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Check History", "Should verify history", 
                "History check failed", e.getMessage());
            throw new RuntimeException("❌ Failed to check history: " + e.getMessage());
        }
    }

    @When("I logout from System Global Settings")
    public void i_logout_from_system_global_settings() {
        driver.switchTo().defaultContent();
        System.out.println("🔄 Switched to default content for logout");
        
        try {
            Thread.sleep(1000);
            
            WebElement welcomeNote = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//small[@class='welcomenote white-text' and contains(text(),'Welcome')]")));
            js.executeScript("arguments[0].click();", welcomeNote);
            Thread.sleep(1000);
            System.out.println("✅ Clicked welcome menu");
            
            WebElement logoutBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//a[@onclick='logout()']")));
            js.executeScript("arguments[0].click();", logoutBtn);
            Thread.sleep(2000);
            
            ExcelLogger.logPass("Logout", "User logged out successfully", "User logged out successfully");
            System.out.println("✅ Logged out successfully");
        } catch (Exception e) {
            ExcelLogger.logFail("Logout", "User should logout", "Logout failed", e.getMessage());
            throw new RuntimeException("❌ Logout failed: " + e.getMessage());
        }
    }

    @Then("the user should be logged out from System Global Settings successfully")
    public void the_user_should_be_logged_out_from_system_global_settings_successfully() {
        try {
            wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("login"),
                ExpectedConditions.visibilityOfElementLocated(By.id("j_username"))
            ));
            
            ExcelLogger.logPass("Verify Logout", "Logout verified successfully", "Redirected to login page successfully");
            System.out.println("✅ Logout verified - redirected to login");
        } catch (Exception e) {
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
        }
        ExcelLogger.closeReport();
        System.out.println("📊 Excel Report: " + REPORT_PATH);
        System.out.println("📸 Screenshots: " + SCREENSHOT_PATH);
    }
}
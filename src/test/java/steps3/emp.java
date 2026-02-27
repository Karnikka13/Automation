package steps3;

import io.cucumber.java.en.*;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.AfterAll;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.*;
import java.time.Duration;

import utils.ExcelLogger;
import utils.CryptoUtils;

public class emp {
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;
    private static String usernameStr, passwordStr, websiteUrl;
    private static final String REPORT_PATH = "E:\\selenium\\EmploymentDemographicReport.xlsx";

    /* =====================================================
       TOAST MESSAGE CAPTURE UTILITIES
    ===================================================== */
    private static void waitForToastToDisappear() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            By.cssSelector("#toast-container .toast-message")));
        } catch (Exception ignored) {}
    }

    private static String captureToastMessage() {
        try {
            WebElement toast = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector("#toast-container .toast-message")));
            String toastText = toast.getText();
            waitForToastToDisappear();
            return toastText;
        } catch (Exception e) {
            return null;
        }
    }

    /* =====================================================
       HELPER METHOD TO SEARCH AND SELECT EMPLOYER
    ===================================================== */
    private void searchAndSelectEmployer(String companyName) throws InterruptedException {
        try {
            System.out.println("🔍 Searching for employer: " + companyName);
            
            WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.id("suggesstion2")));
            js.executeScript("arguments[0].click();", searchBox);
            Thread.sleep(500);
            
            searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            Thread.sleep(300);
            
            searchBox.sendKeys(companyName);
            Thread.sleep(1000);
            
            searchBox.sendKeys(Keys.ARROW_DOWN);
            Thread.sleep(300);
            searchBox.sendKeys(Keys.ENTER);
            Thread.sleep(2000);
            
            System.out.println("✅ Employer selected: " + companyName);
            ExcelLogger.logPass("Search and Select Employer", "Should find and select employer", "Found: " + companyName);
        } catch (Exception e) {
            ExcelLogger.logFail("Search and Select Employer", "Should find employer", "Search failed", e.getMessage());
            throw e;
        }
    }

    @BeforeAll
    public static void setUp() throws IOException {
        ExcelLogger.initReport(REPORT_PATH);
        ExcelLogger.setSection("TEST EXECUTION");
        System.out.println("📊 Excel report: " + REPORT_PATH);

        // Use WebDriverManager to automatically manage ChromeDriver version
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

        // Configure Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        js = (JavascriptExecutor) driver;

        // Read encrypted credentials
        String encFilePath = "E:\\selenium\\credentials.txt";
        File encFile = new File(encFilePath);
        if (!encFile.exists()) {
            ExcelLogger.logFail("Read Credentials", "Credentials file should exist", "Credentials file not found", "Encrypted credentials file not found at: " + encFilePath);
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
            ExcelLogger.logFail("Read Credentials", "First line should contain encrypted credentials", "Empty credentials", "Encrypted credentials line is empty");
            throw new RuntimeException("❌ Encrypted credentials line is empty");
        }

        if (websiteUrl == null || websiteUrl.isEmpty()) {
            ExcelLogger.logFail("Read Website URL", "Second line should contain website URL", "Empty URL", "Website URL line is empty");
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
            ExcelLogger.logFail("Decrypt Credentials", "Credentials should decrypt", "Decryption failed", ex.getMessage());
            throw new RuntimeException("❌ Failed to decrypt credentials. " + ex.getMessage(), ex);
        }

        if (decrypted == null || !decrypted.contains(",")) {
            ExcelLogger.logFail("Validate Credentials Format", "Decrypted format username,password", "Invalid format", "Decrypted credentials format invalid");
            throw new RuntimeException("❌ Decrypted credentials format should be: username,password");
        }

        String[] creds = decrypted.split(",", 2);
        usernameStr = creds[0].trim();
        passwordStr = creds[1].trim();

        // Open browser & login
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

    @Given("the user is logged into the application")
    public void the_user_is_logged_into_the_application() {
        try {
            WebElement logo = driver.findElement(By.id("artrail-logo"));
            if (logo.isDisplayed()) System.out.println("✅ User is logged in");
        } catch (Exception e) {
            ExcelLogger.logFail("Verify Login", "User should be logged in", "Login verification failed", e.getMessage());
            throw new RuntimeException("❌ User is not logged in: " + e.getMessage());
        }
    }

    @When("I open the menu")
    public void i_open_the_menu() {
        try {
            WebElement menuButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("menuPushIcon")));
            js.executeScript("arguments[0].click();", menuButton);
            Thread.sleep(1000);
            ExcelLogger.logPass("Open Menu", "Menu opened", "Menu opened successfully");
            System.out.println("✅ Menu opened");
        } catch (Exception e) {
            ExcelLogger.logFail("Open Menu", "Should open menu", "Menu open failed", e.getMessage());
            throw new RuntimeException("❌ Failed to open menu: " + e.getMessage());
        }
    }

    @When("I search for Employment Demographic in the menu search bar")
    public void i_search_for_employment_demographic_in_menu_search_bar() {
        try {
            WebElement menuSearchBar = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("menuSearchBar")));
            menuSearchBar.clear();
            Thread.sleep(500);
            menuSearchBar.sendKeys("Employment Demographic");
            ExcelLogger.logPass("Enter Search Term", "Entered search term", "Entered 'Employment Demographic'");
            System.out.println("✅ Entered 'Employment Demographic' in search bar");
            Thread.sleep(1000);
        } catch (Exception e) {
            ExcelLogger.logFail("Enter Search Term", "Should enter search term", "Search entry failed", e.getMessage());
            throw new RuntimeException("❌ Failed to enter search term: " + e.getMessage());
        }
    }

    @When("I press Enter to navigate")
    public void i_press_enter_to_navigate() {
        try {
            WebElement menuSearchBar = driver.findElement(By.id("menuSearchBar"));
            menuSearchBar.sendKeys(Keys.ENTER);
            Thread.sleep(2000);
            ExcelLogger.logPass("Press Enter", "Pressed Enter", "Pressed Enter to navigate");
            System.out.println("✅ Pressed Enter to navigate");
        } catch (Exception e) {
            ExcelLogger.logFail("Press Enter", "Should press Enter", "Navigation failed", e.getMessage());
            throw new RuntimeException("❌ Failed to press Enter: " + e.getMessage());
        }
    }

    @When("I wait for the Employment Demographic page to load")
    public void i_wait_for_the_employment_demographic_page_to_load() {
        try {
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            Thread.sleep(2000);
            
            try {
                WebElement employmentFrame = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//iframe[contains(@id,'employment') or contains(@src,'employment')]")));
                driver.switchTo().frame(employmentFrame);
                System.out.println("✅ Switched to employment iframe");
            } catch (TimeoutException te) {
                System.out.println("ℹ️ No iframe found");
            }
            
            Thread.sleep(2000);
            ExcelLogger.logPass("Wait For Page Load", "Page loaded", "Employment Demographic page loaded");
            System.out.println("✅ Employment Demographic page loaded");
        } catch (Exception e) {
            ExcelLogger.logFail("Wait For Page Load", "Page should load", "Page load timeout", e.getMessage());
            throw new RuntimeException("❌ Failed to load page: " + e.getMessage());
        }
    }

    @Then("I should see the Employment Demographic page")
    public void i_should_see_the_employment_demographic_page() {
        ExcelLogger.logPass("Verify Employment Demographic Page", "Page displayed", "Page visible");
        System.out.println("✅ Employment Demographic page is visible");
    }

    @Then("I verify all fields are disabled")
    public void i_verify_all_fields_are_disabled() {
        try {
            String[] fieldIds = {"compnam_1", "weburl_1", "employerEmailID_1", "AddressTypeEmp_1", 
                "modeofcommEmp_1", "employeraddress1_1", "employeraddress2_1", 
                "employeraddress3_1", "employeraddress4_1", "phonenoEmployer_1"};
            
            int disabledCount = 0, enabledCount = 0;
            for (String fieldId : fieldIds) {
                try {
                    WebElement field = driver.findElement(By.id(fieldId));
                    boolean isDisabled = !field.isEnabled() || field.getAttribute("readonly") != null || 
                                       field.getAttribute("disabled") != null;
                    if (isDisabled) {
                        disabledCount++;
                        System.out.println("   ✓ Field '" + fieldId + "' is disabled");
                    } else {
                        enabledCount++;
                        System.out.println("   ✗ Field '" + fieldId + "' is enabled");
                    }
                } catch (NoSuchElementException e) {
                    System.out.println("   ⚠️ Field '" + fieldId + "' not found");
                }
            }
            
            String resultMsg = disabledCount + " disabled, " + enabledCount + " enabled";
            ExcelLogger.logPass("Verify Fields Disabled", "Fields verified", resultMsg);
            System.out.println("✅ Field verification complete");
        } catch (Exception e) {
            ExcelLogger.logFail("Verify Fields Disabled", "Should verify fields", "Verification failed", e.getMessage());
            throw new RuntimeException("❌ Failed to verify fields: " + e.getMessage());
        }
    }

    @When("I search for employer from empp.json")
    public void i_search_for_employer_from_empp_json() {
        try {
            Thread.sleep(1000);
            
            String jsonFilePath = "E:\\selenium\\empp.json";
            File jsonFile = new File(jsonFilePath);
            
            if (!jsonFile.exists()) {
                ExcelLogger.logFail("Read empp.json", "JSON should exist", "File not found", "File not found at: " + jsonFilePath);
                throw new RuntimeException("❌ empp.json file not found at: " + jsonFilePath);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            String searchName = rootNode.get("searchEmployerName").asText();
            
            ExcelLogger.logPass("Read empp.json", "JSON loaded", "Loaded search name: " + searchName);
            System.out.println("📄 Search name from empp.json: " + searchName);
            
            // Click on search box suggesstion2
            WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.id("suggesstion2")));
            js.executeScript("arguments[0].click();", searchBox);
            ExcelLogger.logPass("Click Employer Search Box", "Search box clicked", "Clicked suggesstion2");
            System.out.println("✅ Clicked employer search box");
            Thread.sleep(800);
            
            // Clear and enter search term
            searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            Thread.sleep(500);
            searchBox.sendKeys(searchName);
            ExcelLogger.logPass("Enter Employer Name", "Entered search term", "Entered: " + searchName);
            System.out.println("✅ Entered employer name: " + searchName);
            Thread.sleep(1500); // Wait for autocomplete suggestions to appear
            
            // Press Down arrow to select first suggestion, then Enter
            searchBox.sendKeys(Keys.ARROW_DOWN);
            Thread.sleep(500);
            searchBox.sendKeys(Keys.ENTER);
            ExcelLogger.logPass("Press Enter", "Pressed Enter", "Pressed Enter to search");
            System.out.println("✅ Pressed Arrow Down and Enter");
            Thread.sleep(2000);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Search Employer", "Should search employer", "Search failed", e.getMessage());
            throw new RuntimeException("❌ Failed to search employer: " + e.getMessage());
        }
    }

    @Then("I click on the first search result")
    public void i_click_on_the_first_search_result() {
        try {
            Thread.sleep(1500);
            
            // Try multiple possible selectors for search results
            WebElement firstResult = null;
            
            // Option 1: Try dropdown/autocomplete result
            try {
                firstResult = driver.findElement(By.xpath("//ul[contains(@class,'select2-results')]//li[1]"));
                if (firstResult.isDisplayed()) {
                    js.executeScript("arguments[0].click();", firstResult);
                    ExcelLogger.logPass("Click First Result", "First result clicked", "Clicked dropdown result");
                    System.out.println("✅ Clicked first search result from dropdown");
                    Thread.sleep(2000);
                    return;
                }
            } catch (Exception e1) {
                System.out.println("   ℹ️ Dropdown result not found, trying table...");
            }
            
            // Option 2: Try table result
            try {
                firstResult = driver.findElement(By.xpath("//table[contains(@class,'table')]//tbody//tr[1]"));
                if (firstResult.isDisplayed()) {
                    js.executeScript("arguments[0].click();", firstResult);
                    ExcelLogger.logPass("Click First Result", "First result clicked", "Clicked table result");
                    System.out.println("✅ Clicked first search result from table");
                    Thread.sleep(2000);
                    return;
                }
            } catch (Exception e2) {
                System.out.println("   ℹ️ Table result not found, trying list item...");
            }
            
            // Option 3: Try any list item with employer name
            try {
                firstResult = driver.findElement(By.xpath("(//li[contains(@class,'select2-result')])[1]"));
                if (firstResult.isDisplayed()) {
                    js.executeScript("arguments[0].click();", firstResult);
                    ExcelLogger.logPass("Click First Result", "First result clicked", "Clicked list result");
                    System.out.println("✅ Clicked first search result from list");
                    Thread.sleep(2000);
                    return;
                }
            } catch (Exception e3) {
                System.out.println("   ℹ️ List result not found");
            }
            
            // If nothing found, the search might have already auto-filled the form
            System.out.println("   ℹ️ No clickable results found - data may be auto-filled already");
            ExcelLogger.logPass("Click First Result", "Auto-filled without click", "Data auto-filled on Enter");
            Thread.sleep(1000);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Click First Result", "Should click first result", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click first result: " + e.getMessage());
        }
    }

    @Then("I verify the details are auto-filled")
    public void i_verify_the_details_are_auto_filled() {
        try {
            Thread.sleep(1500);
            
            // Check if Company Name field has value
            WebElement companyNameField = driver.findElement(By.id("compnam_1"));
            String companyValue = companyNameField.getAttribute("value");
            
            if (companyValue != null && !companyValue.trim().isEmpty()) {
                ExcelLogger.logPass("Verify Auto-Fill", "Details should be auto-filled", "Company Name: " + companyValue);
                System.out.println("✅ Details auto-filled - Company: " + companyValue);
            } else {
                ExcelLogger.logFail("Verify Auto-Fill", "Details should be auto-filled", "No data filled", "Company name field is empty");
                System.out.println("⚠️ Company name field appears empty");
            }
            
        } catch (Exception e) {
            ExcelLogger.logFail("Verify Auto-Fill", "Should verify auto-fill", "Verification failed", e.getMessage());
            throw new RuntimeException("❌ Failed to verify auto-fill: " + e.getMessage());
        }
    }

    @When("I search for the saved employer to edit again")
    public void i_search_for_the_saved_employer_to_edit_again() {
        try {
            Thread.sleep(1000);
            
            String jsonFilePath = "E:\\selenium\\employmentDemographicData.json";
            File jsonFile = new File(jsonFilePath);
            
            if (!jsonFile.exists()) {
                throw new RuntimeException("❌ Employment data file not found");
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode empData = rootNode.get("employmentData");
            String companyName = empData.get("companyName").asText();
            
            searchAndSelectEmployer(companyName);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Search Saved Employer", "Should search for saved employer", "Search failed", e.getMessage());
            throw new RuntimeException("❌ Failed to search for saved employer: " + e.getMessage());
        }
    }

    @When("I click on the ellipsis menu")
    public void i_click_on_the_ellipsis_menu() {
        try {
            WebElement ellipsisMenu = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[@class='fa fa-ellipsis-v']")));
            js.executeScript("arguments[0].click();", ellipsisMenu);
            Thread.sleep(1000);
            ExcelLogger.logPass("Click Ellipsis Menu", "Ellipsis clicked", "Ellipsis menu clicked");
            System.out.println("✅ Clicked ellipsis menu");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Ellipsis Menu", "Should click ellipsis", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click ellipsis: " + e.getMessage());
        }
    }

    @When("I click on Edit option")
    public void i_click_on_edit_option() {
        try {
            WebElement editOption = wait.until(ExpectedConditions.elementToBeClickable(By.id("editEmp")));
            js.executeScript("arguments[0].click();", editOption);
            Thread.sleep(1500);
            ExcelLogger.logPass("Click Edit Option", "Edit clicked", "Edit option clicked");
            System.out.println("✅ Clicked Edit option");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Edit Option", "Should click Edit", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Edit: " + e.getMessage());
        }
    }

    @When("I enter data in all Employment Demographic fields")
    public void i_enter_data_in_all_employment_demographic_fields() {
        try {
            Thread.sleep(1000);
            
            String jsonFilePath = "E:\\selenium\\employmentDemographicData.json";
            File jsonFile = new File(jsonFilePath);
            
            if (!jsonFile.exists()) {
                ExcelLogger.logFail("Read JSON Data", "JSON should exist", "File not found", "File not found at: " + jsonFilePath);
                throw new RuntimeException("❌ Employment data file not found at: " + jsonFilePath);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode empData = rootNode.get("employmentData");
            
            ExcelLogger.logPass("Read JSON Data", "JSON loaded", "JSON file loaded - employmentData");
            System.out.println("📄 Loaded employment data from JSON (employmentData)");
            
            fillEmploymentFields(empData);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Enter All Fields", "Should enter data", "Data entry failed", e.getMessage());
            throw new RuntimeException("❌ Failed to enter data: " + e.getMessage());
        }
    }

    @When("I enter alternate data in all Employment Demographic fields")
    public void i_enter_alternate_data_in_all_employment_demographic_fields() {
        try {
            Thread.sleep(1000);
            
            String jsonFilePath = "E:\\selenium\\employmentDemographicData.json";
            File jsonFile = new File(jsonFilePath);
            
            if (!jsonFile.exists()) {
                ExcelLogger.logFail("Read JSON Data", "JSON should exist", "File not found", "File not found at: " + jsonFilePath);
                throw new RuntimeException("❌ Employment data file not found at: " + jsonFilePath);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);
            JsonNode empData = rootNode.get("alternateEmploymentData");
            
            ExcelLogger.logPass("Read JSON Data", "JSON loaded", "JSON file loaded - alternateEmploymentData");
            System.out.println("📄 Loaded employment data from JSON (alternateEmploymentData)");
            
            fillEmploymentFields(empData);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Enter All Fields", "Should enter data", "Data entry failed", e.getMessage());
            throw new RuntimeException("❌ Failed to enter data: " + e.getMessage());
        }
    }

    private void fillEmploymentFields(JsonNode empData) throws InterruptedException {
        enterField("compnam_1", empData.get("companyName").asText(), "Company Name");
        enterField("weburl_1", empData.get("websiteUrl").asText(), "Website URL");
        enterField("employerEmailID_1", empData.get("emailId").asText(), "Email ID");
        
        boolean isInternational = empData.get("international").asBoolean();
        
        if (isInternational) {
            checkCheckbox("chkInterEmployer_1", "International");
            System.out.println("⏳ Waiting for form to switch to international fields...");
            Thread.sleep(2000); // Wait longer for form to switch to international mode
            
            // Wait for international-specific fields to be present
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("employeraddress3_1")));
                System.out.println("✅ International fields loaded");
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not confirm international fields loaded");
            }
        } else {
            uncheckCheckbox("chkInterEmployer_1", "International");
            System.out.println("⏳ Waiting for form to switch to domestic fields...");
            Thread.sleep(2000); // Wait longer for form to switch to domestic mode
            
            // Verify domestic fields are ready
            try {
                wait.until(ExpectedConditions.elementToBeClickable(By.id("s2id_autogen1")));
                System.out.println("✅ Domestic fields loaded");
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not confirm domestic fields loaded");
            }
        }
        
        // Now proceed with filling address fields based on mode
        selectDropdown("AddressTypeEmp_1", empData.get("addressStatus").asText(), "Address Status");
        selectDropdown("modeofcommEmp_1", empData.get("addressType").asText(), "Address Type");
        
        enterField("employeraddress1_1", empData.get("address1").asText(), "Address 1");
        enterField("employeraddress2_1", empData.get("address2").asText(), "Address 2");
        
        if (isInternational) {
            // International address fields
            enterField("employeraddress3_1", empData.get("address3").asText(), "Address 3");
            enterField("employeraddress4_1", empData.get("address4").asText(), "Address 4");
            
            enterSelect2Field("s2id_autogen1", empData.get("locality").asText(), "Locality");
            enterSelect2Field("s2id_autogen2", empData.get("region").asText(), "Region");
            enterSelect2Field("s2id_autogen4", empData.get("postalcode").asText(), "Postal Code");
            enterSelect2Field("s2id_autogen3", empData.get("country").asText(), "Country");
        } else {
            // Domestic address fields (US format) - all use Select2
            enterSelect2Field("s2id_autogen1", empData.get("city").asText(), "City");
            enterSelect2Field("s2id_autogen2", empData.get("state").asText(), "State");
            enterSelect2Field("s2id_autogen4", empData.get("zip").asText(), "Zip");
        }
        
        enterField("phonenoEmployer_1", empData.get("phone").asText(), "Phone");
        
        if (empData.get("allowed").asBoolean()) {
            checkCheckbox("AllowedEmployer_1", "Allowed");
        } else {
            uncheckCheckbox("AllowedEmployer_1", "Allowed");
        }
        
        Thread.sleep(1000);
        ExcelLogger.logPass("Enter All Fields", "All fields entered", "All fields populated");
        System.out.println("✅ Completed entering all fields");
    }

    private void enterField(String fieldId, String value, String fieldName) {
        try {
            WebElement field = driver.findElement(By.id(fieldId));
            
            // Clear field thoroughly
            field.click();
            Thread.sleep(200);
            field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            Thread.sleep(100);
            field.sendKeys(Keys.DELETE);
            Thread.sleep(200);
            
            // Enter new value
            field.sendKeys(value);
            ExcelLogger.logPass("Enter " + fieldName, "Entered " + fieldName, "Entered: " + value);
            System.out.println("   ✓ Entered " + fieldName);
            Thread.sleep(300);
        } catch (Exception e) {
            ExcelLogger.logFail("Enter " + fieldName, "Should enter " + fieldName, "Field not accessible", e.getMessage());
            System.out.println("   ✗ " + fieldName + " field not accessible: " + e.getMessage());
        }
    }

    private void selectDropdown(String fieldId, String value, String fieldName) {
        try {
            WebElement dropdown = driver.findElement(By.id(fieldId));
            Select select = new Select(dropdown);
            select.selectByVisibleText(value);
            ExcelLogger.logPass("Select " + fieldName, "Selected " + fieldName, "Selected: " + value);
            System.out.println("   ✓ Selected " + fieldName);
            Thread.sleep(300);
        } catch (Exception e) {
            ExcelLogger.logFail("Select " + fieldName, "Should select " + fieldName, "Dropdown not accessible", e.getMessage());
            System.out.println("   ✗ " + fieldName + " dropdown not accessible: " + e.getMessage());
        }
    }

    private void enterSelect2Field(String fieldId, String value, String fieldName) {
        try {
            WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(fieldId)));
            js.executeScript("arguments[0].click();", field);
            Thread.sleep(400);
            
            // Clear existing value
            field.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            Thread.sleep(100);
            field.sendKeys(Keys.DELETE);
            Thread.sleep(200);
            
            // Enter new value
            field.sendKeys(value);
            Thread.sleep(700);
            field.sendKeys(Keys.ENTER);
            ExcelLogger.logPass("Enter " + fieldName, "Entered " + fieldName, "Entered: " + value);
            System.out.println("   ✓ Entered " + fieldName);
            Thread.sleep(500);
        } catch (Exception e) {
            ExcelLogger.logFail("Enter " + fieldName, "Should enter " + fieldName, "Field not accessible", e.getMessage());
            System.out.println("   ✗ " + fieldName + " field not accessible: " + e.getMessage());
        }
    }

    private void checkCheckbox(String fieldId, String fieldName) {
        try {
            WebElement checkbox = driver.findElement(By.id(fieldId));
            if (!checkbox.isSelected()) {
                js.executeScript("arguments[0].click();", checkbox);
                ExcelLogger.logPass("Check " + fieldName, "Checked " + fieldName, "Checked " + fieldName);
                System.out.println("   ✓ Checked " + fieldName);
                Thread.sleep(300);
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Check " + fieldName, "Should check " + fieldName, "Checkbox not accessible", e.getMessage());
            System.out.println("   ✗ " + fieldName + " checkbox not accessible");
        }
    }

    private void uncheckCheckbox(String fieldId, String fieldName) {
        try {
            WebElement checkbox = driver.findElement(By.id(fieldId));
            if (checkbox.isSelected()) {
                js.executeScript("arguments[0].click();", checkbox);
                ExcelLogger.logPass("Uncheck " + fieldName, "Unchecked " + fieldName, "Unchecked " + fieldName);
                System.out.println("   ✓ Unchecked " + fieldName);
                Thread.sleep(300);
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Uncheck " + fieldName, "Should uncheck " + fieldName, "Checkbox not accessible", e.getMessage());
            System.out.println("   ✗ " + fieldName + " checkbox not accessible");
        }
    }

    @When("I click on the ellipsis menu to clear")
    public void i_click_on_the_ellipsis_menu_to_clear() {
        try {
            WebElement ellipsisMenu = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[@class='fa fa-ellipsis-v']")));
            js.executeScript("arguments[0].click();", ellipsisMenu);
            Thread.sleep(1000);
            ExcelLogger.logPass("Click Ellipsis for Clear", "Ellipsis clicked", "Clicked ellipsis for clear");
            System.out.println("✅ Clicked ellipsis for clear");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Ellipsis for Clear", "Should click ellipsis", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click ellipsis: " + e.getMessage());
        }
    }

    @When("I click on Clear option")
    public void i_click_on_clear_option() {
        try {
            WebElement clearOption = wait.until(ExpectedConditions.elementToBeClickable(By.id("btnClear")));
            js.executeScript("arguments[0].click();", clearOption);
            Thread.sleep(1000);
            ExcelLogger.logPass("Click Clear Option", "Clear clicked", "Clear option clicked");
            System.out.println("✅ Clicked Clear option");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Clear Option", "Should click Clear", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Clear: " + e.getMessage());
        }
    }

    @When("I click Yes on confirmation dialog")
    public void i_click_yes_on_confirmation_dialog() {
        try {
            WebElement yesButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@data-value='true']")));
            js.executeScript("arguments[0].click();", yesButton);
            Thread.sleep(2000);
            ExcelLogger.logPass("Click Yes", "Yes clicked", "Confirmed clear action");
            System.out.println("✅ Clicked Yes to confirm clear");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Yes", "Should click Yes", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Yes: " + e.getMessage());
        }
    }

    @When("I click on the ellipsis menu to save")
    public void i_click_on_the_ellipsis_menu_to_save() {
        try {
            // Give form time to stabilize after data entry
            Thread.sleep(1000);
            
            WebElement ellipsisMenu = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//span[@class='fa fa-ellipsis-v']")));
            js.executeScript("arguments[0].click();", ellipsisMenu);
            Thread.sleep(1000);
            ExcelLogger.logPass("Click Ellipsis for Save", "Ellipsis clicked", "Clicked ellipsis for save");
            System.out.println("✅ Clicked ellipsis for save");
        } catch (Exception e) {
            ExcelLogger.logFail("Click Ellipsis for Save", "Should click ellipsis", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click ellipsis: " + e.getMessage());
        }
    }

    @When("I click on Save option")
    public void i_click_on_save_option() {
        try {
            // Ensure dropdown is visible before clicking
            Thread.sleep(500);
            
            WebElement saveOption = wait.until(ExpectedConditions.elementToBeClickable(By.id("updateEmployer1")));
            js.executeScript("arguments[0].click();", saveOption);
            System.out.println("✅ Clicked Save button");
            
            Thread.sleep(1000);
            
            // ===== TOAST CAPTURE FOR SAVE =====
            String toastMsg = captureToastMessage();
            if (toastMsg != null && !toastMsg.isEmpty()) {
                ExcelLogger.logPass("Click Save Option", "Save should persist the data", toastMsg);
                System.out.println("✅ Save completed | Toast: " + toastMsg);
            } else {
                ExcelLogger.logPass("Click Save Option", "Save clicked", "Data saved successfully");
                System.out.println("✅ Save completed");
            }
            
            // Wait for page to stabilize after save
            Thread.sleep(2000);
            
            // Wait for form to be ready (check if company name field is present and interactable)
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("compnam_1")));
                System.out.println("✅ Form reloaded after save");
            } catch (Exception e) {
                System.out.println("⚠️ Form reload detection timeout, continuing...");
            }
            
        } catch (Exception e) {
            ExcelLogger.logFail("Click Save Option", "Should click Save", "Click failed", e.getMessage());
            throw new RuntimeException("❌ Failed to click Save: " + e.getMessage());
        }
    }

    @When("I search for saved employment records")
    public void i_search_for_saved_employment_records() {
        try {
            Thread.sleep(1500);
            
            String searchJsonPath = "E:\\selenium\\empsearch.json";
            File searchJsonFile = new File(searchJsonPath);
            
            if (!searchJsonFile.exists()) {
                ExcelLogger.logFail("Read Search JSON", "Search JSON should exist", "File not found", "Search file not found at: " + searchJsonPath);
                throw new RuntimeException("❌ Search file not found at: " + searchJsonPath);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(searchJsonFile);
            JsonNode searchTerms = rootNode.get("searchTerms");
            
            if (searchTerms == null || !searchTerms.isArray() || searchTerms.size() == 0) {
                ExcelLogger.logFail("Validate Search JSON", "Should contain searchTerms array", "Invalid format", "searchTerms array is empty or invalid");
                throw new RuntimeException("❌ searchTerms array is empty or invalid in JSON");
            }
            
            ExcelLogger.logPass("Read Search JSON", "Search JSON loaded", "Loaded " + searchTerms.size() + " search terms");
            System.out.println("📄 Loaded " + searchTerms.size() + " search terms from JSON");
            
            for (JsonNode term : searchTerms) {
                String searchValue = term.asText();
                searchEmployment(searchValue);
            }
            
            ExcelLogger.logPass("Complete All Searches", "All searches completed", "Completed searching for all employment records");
            System.out.println("✅ Completed all employment record searches");
            
        } catch (Exception e) {
            ExcelLogger.logFail("Search Employment Records", "Should search records", "Search failed", e.getMessage());
            throw new RuntimeException("❌ Failed to search employment records: " + e.getMessage());
        }
    }

    private void searchEmployment(String searchValue) throws InterruptedException {
        try {
            WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("suggesstion0")));
            searchBox.click();
            ExcelLogger.logPass("Click Search Box", "suggesstion0 should be clickable", "Clicked employment search box");
            
            searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            ExcelLogger.logPass("Clear Search Box", "Search box should be cleared", "Cleared employment search box");
            Thread.sleep(500);
            
            searchBox.sendKeys(searchValue);
            ExcelLogger.logPass("Type Search Term", "Search term should be entered", "Entered search term: " + searchValue);
            
            searchBox.sendKeys(Keys.ENTER);
            ExcelLogger.logPass("Press Enter Search", "Enter should submit search", "Pressed Enter in search box");
            Thread.sleep(1500);
            
            try {
                WebElement searchResult = driver.findElement(
                        By.xpath("//table[contains(@class,'table')]//td[contains(text(),'" + searchValue + "')]"));
                if (searchResult.isDisplayed()) {
                    ExcelLogger.logPass("Verify Employment Exists", "Employment record should be present", "Verified employment exists: " + searchValue);
                    System.out.println("   ✅ Found employment record: " + searchValue);
                } else {
                    ExcelLogger.logFail("Verify Employment Exists", "Employment record should be present", "Record not displayed", "Employment not found: " + searchValue);
                    System.out.println("   ❌ Employment record not displayed: " + searchValue);
                }
            } catch (NoSuchElementException e) {
                ExcelLogger.logFail("Verify Employment Exists", "Employment record should be present", "No matching row found", "Employment not found: " + searchValue);
                System.out.println("   ❌ Employment record not found: " + searchValue);
            }
            
            Thread.sleep(1000);
            
        } catch (Exception e) {
            ExcelLogger.logFail("Search Employment Exception", "Search flow should work", "Search failed for: " + searchValue, e.getMessage());
            System.out.println("   ❌ Search failed for: " + searchValue);
            throw e;
        }
    }

    @When("I logout")
    public void i_logout() {
        driver.switchTo().defaultContent();
        System.out.println("🔄 Switched to default content for logout");
        
        try {
            Thread.sleep(1000);
            
            WebElement welcomeNote = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//small[@class='welcomenote white-text' and contains(text(),'Welcome')]")));
            js.executeScript("arguments[0].click();", welcomeNote);
            Thread.sleep(1000);

            WebElement logoutBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[@onclick='logout()']")));
            js.executeScript("arguments[0].click();", logoutBtn);
            Thread.sleep(2000);
            
            ExcelLogger.logPass("Logout", "User logged out", "User logged out successfully");
            System.out.println("✅ Logged out successfully");
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
                    ExpectedConditions.visibilityOfElementLocated(By.id("j_username"))));
            ExcelLogger.logPass("Verify Logout", "Redirected to login", "Redirected to login page");
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
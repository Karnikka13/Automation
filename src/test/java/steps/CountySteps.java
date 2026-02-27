package steps;

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

import utils.ExcelLogger;
import utils.CryptoUtils;

public class CountySteps {
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;
    private static String usernameStr, passwordStr, websiteUrl;
    private static String lastUpdatedCounty = null;
    private static final String REPORT_PATH = "E:\\selenium\\CountyReport.xlsx";

    // ---------- SETUP & LOGIN ----------
    @BeforeAll
    public static void setUp() throws IOException {
        // Initialize report - this will overwrite any existing file
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
            // Read first line (encrypted credentials)
            encryptedPayload = br.readLine();
            if (encryptedPayload != null) {
                encryptedPayload = encryptedPayload.trim();
            }
            
            // Read second line (website URL)
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

        ExcelLogger.logPass("Read Website URL", "Website URL should be read from file", "Website URL read successfully");
        System.out.println("🌐 Website URL loaded");

        // 🔧 FIXED: Check for AES_SECRET_KEY first, then CRED_PASSPHRASE, then fallback
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
            passphraseEnv = "MySecretAESKey123"; // 🔧 FIXED: Updated default to match your env var
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
        ExcelLogger.logPass("Enter Username", "Username should be entered into j_username", "Entered username");

        WebElement password = driver.findElement(By.id("j_password"));
        password.sendKeys(passwordStr);
        ExcelLogger.logPass("Enter Password", "Password should be entered into j_password", "Entered password");

        WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        loginBtn.click();
        ExcelLogger.logPass("Click Login", "Click login button", "Clicked login button");

        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("artrail-logo")));
        ExcelLogger.logPass("Verify Login", "artrail-logo should be visible after login", "Login successful");

        System.out.println("✅ Login successful!");

        // clear sensitive variables
        passwordStr = null;
        decrypted = null;
    }

    /* =====================================================
       TOAST MESSAGE CAPTURE UTILITIES
    ===================================================== */
    private void waitForToastToDisappear() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            By.cssSelector("#toast-container .toast-message")));
        } catch (Exception ignored) {}
    }

    private String captureToastMessage() {
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

    // ---------- NAVIGATION ----------
    @When("I navigate to County Master")
    public void i_navigate_to_county_master() {
        try {
            WebElement menuButton = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("menuPushIcon")));
            js.executeScript("arguments[0].click();", menuButton);
            ExcelLogger.logPass("Click Menu", "Menu button should be clicked", "Clicked menu button");

            WebElement sysAdmin = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(text(),'System Administration')]"))
            );
            js.executeScript("arguments[0].click();", sysAdmin);
            ExcelLogger.logPass("Click System Admin", "System Administration link should be clicked", "Clicked System Administration");

            WebElement countyMaster = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(text(),'County Master')]"))
            );
            js.executeScript("arguments[0].click();", countyMaster);
            ExcelLogger.logPass("Click County Master", "County Master link should be clicked", "Clicked County Master");

            ExcelLogger.logInfo("Navigate", "Should land on County Master", "Navigated to County Master");
            System.out.println("✅ Navigated to County Master");
        } catch (Exception e) {
            ExcelLogger.logFail("Navigate to County Master", "Should navigate successfully", "Navigation failed", e.getMessage());
            throw e;
        }
    }

    // ---------- SEARCH FLOW ----------
    @When("I search counties from JSON file {string}")
    public void i_search_counties_from_json_file(String jsonFilePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new File(jsonFilePath));
        JsonNode counties = rootNode.get("counties");

        if (counties == null || !counties.isArray()) {
            ExcelLogger.logFail("Read JSON for Search", "JSON contains 'counties' array", "Invalid JSON", "Invalid JSON format. Expected 'counties' array.");
            throw new RuntimeException("❌ Invalid JSON format. Expected 'counties' array.");
        }

        for (JsonNode county : counties) {
            String countyName = county.asText();
            searchCounty(countyName);
        }
    }

    private void searchCounty(String countyName) throws InterruptedException {
        try {
            driver.switchTo().defaultContent();
            driver.switchTo().frame(0);

            WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("countySearch")));
            searchBox.click();
            ExcelLogger.logPass("Click Search Box", "countySearch should be clickable", "Clicked county search box");

            searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            ExcelLogger.logPass("Clear Search Box", "Search box should be cleared", "Cleared county search box");
            Thread.sleep(500);

            searchBox.sendKeys(countyName);
            ExcelLogger.logPass("Type County Name", "County name should be entered", "Entered county name: " + countyName);

            searchBox.sendKeys(Keys.ENTER);
            ExcelLogger.logPass("Press Enter Search", "Enter should submit the search", "Pressed Enter in search box");
            Thread.sleep(1000);

            try {
                WebElement searchResult = driver.findElement(
                        By.xpath("//table[contains(@class,'table')]//td[contains(text(),'" + countyName + "')]"));
                if (searchResult.isDisplayed()) {
                    ExcelLogger.logPass("Verify County Exists", "County row should be present", "Verified county exists: " + countyName);
                } else {
                    ExcelLogger.logFail("Verify County Exists", "County row should be present", "County not displayed", "County not found: " + countyName);
                }
            } catch (NoSuchElementException e) {
                ExcelLogger.logFail("Verify County Exists", "County row should be present", "No matching row found", "County not found: " + countyName);
            }
        } catch (Exception e) {
            ExcelLogger.logFail("Search County Exception", "Search flow should work", "Search failed for: " + countyName, e.getMessage());
            throw e;
        }
    }

    // ---------- INSERT FLOW ----------
    @When("I insert counties from JSON file {string}")
    public void i_insert_counties_from_json_file(String jsonFilePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new File(jsonFilePath));
        JsonNode counties = rootNode.get("counties");

        for (JsonNode county : counties) {
            String countyName = county.get("county").asText();
            String state = county.get("state").asText();
            String status = county.get("status").asText();

            try {
                addCounty(countyName, state, status);
                verifyCounty(countyName);
                ExcelLogger.logPass("Insert County", "County should be inserted and verified", "Inserted and verified county: " + countyName);
            } catch (Exception e) {
                ExcelLogger.logFail("Insert County", "County should be inserted", "Insertion failed for: " + countyName, e.getMessage());
            }
        }
    }

    private void addCounty(String countyNameCsv, String countyStateCsv, String status) throws InterruptedException {
        driver.switchTo().defaultContent();
        int iframeCount = driver.findElements(By.tagName("iframe")).size();
        boolean actionDone = false;

        for (int i = 0; i < iframeCount; i++) {
            try {
                driver.switchTo().defaultContent();
                driver.switchTo().frame(i);

                WebElement dropdownBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("button.btn.btn-success.dropdown-toggle.edit-btn.deSelectAlways")));
                js.executeScript("arguments[0].click();", dropdownBtn);
                ExcelLogger.logPass("Open Add Menu", "Add menu should open", "Clicked + dropdown button");

                WebElement addNewCountyOption = wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                By.cssSelector("a.dropdown-item.addCounty.deSelectAlways"))
                );
                js.executeScript("arguments[0].click();", addNewCountyOption);
                ExcelLogger.logPass("Select Add New County", "Add New County option should be clicked", "Clicked Add New County option");

                WebElement countyName = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Co_name")));
                countyName.clear();
                countyName.sendKeys(countyNameCsv);
                ExcelLogger.logPass("Enter County Name", "County name should be entered", "Entered county name: " + countyNameCsv);

                WebElement countyState = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Co_state-styled")));
                countyState.clear();
                countyState.sendKeys(countyStateCsv);
                ExcelLogger.logPass("Enter County State", "County state should be entered", "Entered county state: " + countyStateCsv);

                WebElement activeCheckbox = driver.findElement(By.id("chkActive"));
                if (status.equalsIgnoreCase("inactive") && activeCheckbox.isSelected()) {
                    js.executeScript("arguments[0].click();", activeCheckbox);
                    ExcelLogger.logPass("Set Status Inactive", "Active checkbox should be unchecked", "Unchecked Active checkbox for inactive status");
                } else if (status.equalsIgnoreCase("active") && !activeCheckbox.isSelected()) {
                    js.executeScript("arguments[0].click();", activeCheckbox);
                    ExcelLogger.logPass("Set Status Active", "Active checkbox should be checked", "Checked Active checkbox for active status");
                }

                WebElement ellipseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("span.fa.fa-ellipsis-v")));
                js.executeScript("arguments[0].click();", ellipseBtn);
                ExcelLogger.logPass("Open Ellipsis", "Ellipsis should be clicked", "Clicked ellipsis button");

                WebElement saveBtn = wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a.dropdown-item.deSelectAlways.saveCounty")));
                js.executeScript("arguments[0].click();", saveBtn);
                
                Thread.sleep(1500);
                
                // ===== TOAST CAPTURE FOR INSERT =====
                String toastMsg = captureToastMessage();
                if (toastMsg != null && !toastMsg.isEmpty()) {
                    ExcelLogger.logPass("Save County", "Save should persist the county", toastMsg);
                    System.out.println("✅ Saved County: " + countyNameCsv + " | Toast: " + toastMsg);
                } else {
                    ExcelLogger.logPass("Save County", "Save should persist the county", "Clicked Save button");
                    System.out.println("✅ Saved County: " + countyNameCsv);
                }
                
                actionDone = true;
                break;
            } catch (Exception e) {
                // try next iframe
            }
        }

        if (!actionDone) {
            ExcelLogger.logFail("Add County", "County should be added via any iframe", "No iframe succeeded to add county: " + countyNameCsv, "Failed to insert county");
            throw new RuntimeException("⚠️ Could not insert county: " + countyNameCsv);
        }
    }

    private void verifyCounty(String countyNameCsv) throws InterruptedException {
        searchCounty(countyNameCsv);
    }

    // ---------- UPDATE FLOW ----------
    @When("I update counties from JSON file {string}")
    public void i_update_counties_from_json_file(String jsonFilePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile = new File(jsonFilePath);

        JsonNode rootNode = mapper.readTree(jsonFile);
        JsonNode counties = rootNode.get("counties");

        for (JsonNode county : counties) {
            String oldName = county.get("oldName").asText();
            String newName = county.get("newName").asText();
            String newState = county.get("newState").asText();
            String newStatus = county.get("newStatus").asText();

            try {
                updateCounty(oldName, newName, newState, newStatus);
                lastUpdatedCounty = newName;
                ExcelLogger.logPass("Update County", "County should be updated and verified", "Updated county: " + oldName + " → " + newName);
            } catch (Exception e) {
                ExcelLogger.logFail("Update County", "County should be updated", "Update failed for: " + oldName, e.getMessage());
            }
        }
    }

    private void updateCounty(String oldName, String newName, String newState, String newStatus) throws InterruptedException {
        driver.switchTo().defaultContent();
        driver.switchTo().frame(0);

        WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.id("countySearch")));
        searchBox.click();
        searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        Thread.sleep(500);

        searchBox.sendKeys(oldName);
        ExcelLogger.logPass("Search Old County", "Old county name should be entered", "Entered old county name: " + oldName);
        searchBox.sendKeys(Keys.ENTER);
        ExcelLogger.logPass("Submit Old County Search", "Enter should search", "Pressed Enter to search old county");

        WebElement searchResult = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//table[contains(@class,'table')]//td[@role='gridcell' and normalize-space(text())='" + oldName + "']"))
        );
        js.executeScript("arguments[0].click();", searchResult);
        ExcelLogger.logPass("Select County Row", "County row should be selected", "Selected county row: " + oldName);

        WebElement ellipseBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("span.fa.fa-ellipsis-v")));
        js.executeScript("arguments[0].click();", ellipseBtn);
        ExcelLogger.logPass("Open Ellipsis For Edit", "Ellipsis should be clicked", "Clicked ellipsis button");

        WebElement editBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a.dropdown-item.docEditMode.deSelectAlways")));
        js.executeScript("arguments[0].click();", editBtn);
        ExcelLogger.logPass("Click Edit", "Edit option should be clicked", "Clicked Edit option");

        WebElement countyNameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Co_name")));
        countyNameField.clear();
        countyNameField.sendKeys(newName);
        ExcelLogger.logPass("Update County Name", "County name should be updated", "Updated county name: " + newName);

        WebElement countyStateField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("Co_state-styled")));
        countyStateField.clear();
        countyStateField.sendKeys(newState);
        ExcelLogger.logPass("Update County State", "County state should be updated", "Updated county state: " + newState);

        WebElement activeCheckbox = driver.findElement(By.id("chkActive"));
        if (newStatus.equalsIgnoreCase("inactive") && activeCheckbox.isSelected()) {
            js.executeScript("arguments[0].click();", activeCheckbox);
            ExcelLogger.logPass("Set Status Inactive", "Active checkbox should be unchecked", "Unchecked Active checkbox for inactive status");
        } else if (newStatus.equalsIgnoreCase("active") && !activeCheckbox.isSelected()) {
            js.executeScript("arguments[0].click();", activeCheckbox);
            ExcelLogger.logPass("Set Status Active", "Active checkbox should be checked", "Checked Active checkbox for active status");
        }

        js.executeScript("arguments[0].click();", ellipseBtn);
        WebElement updateBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("span.ellipseSaveUpdate")));
        js.executeScript("arguments[0].click();", updateBtn);
        
        Thread.sleep(1500);
        
        // ===== TOAST CAPTURE FOR UPDATE =====
        String toastMsg = captureToastMessage();
        if (toastMsg != null && !toastMsg.isEmpty()) {
            ExcelLogger.logPass("Click Update", "Update button should be clicked", toastMsg);
        } else {
            ExcelLogger.logPass("Click Update", "Update button should be clicked", "Clicked Update button");
        }

        searchCounty(newName);
        ExcelLogger.logPass("Verify Updated County", "Updated county should be searchable", "Verified updated county: " + newName);
    }

    // ---------- CLEAR FLOW ----------
    @When("I clear counties from JSON file {string}")
    public void i_clear_counties_from_json_file(String jsonFilePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(new File(jsonFilePath));
        JsonNode counties = rootNode.get("counties");

        if (counties == null || !counties.isArray()) {
            ExcelLogger.logFail("Read JSON for Clear", "JSON contains 'counties' array", "Invalid JSON", "Invalid JSON format for clear. Expected 'counties' array.");
            throw new RuntimeException("❌ Invalid JSON format for clear. Expected 'counties' array.");
        }

        for (JsonNode county : counties) {
            String countyName = county.asText();
            try {
                clearCounty(countyName);
                ExcelLogger.logPass("Clear County", "County should be cleared", "Cleared county: " + countyName);
                lastUpdatedCounty = countyName;
                i_view_county_history();
            } catch (Exception e) {
                ExcelLogger.logFail("Clear County", "County should be cleared", "Clear failed for: " + countyName, e.getMessage());
            }
        }
    }

    private void clearCounty(String countyName) throws InterruptedException {
        driver.switchTo().defaultContent();
        driver.switchTo().frame(0);

        WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("countySearch")));
        searchBox.click();
        searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        Thread.sleep(500);

        searchBox.sendKeys(countyName);
        ExcelLogger.logPass("Type County For Clear", "County name should be entered for clear", "Entered county name in search box for clear: " + countyName);
        searchBox.sendKeys(Keys.ENTER);
        Thread.sleep(1000);

        WebElement row = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//table[contains(@class,'table')]//td[normalize-space(text())='" + countyName + "']")));
        js.executeScript("arguments[0].click();", row);
        ExcelLogger.logPass("Select Row For Clear", "County row should be selected", "Selected county row for clear: " + countyName);

        WebElement ellipseBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("span.fa.fa-ellipsis-v")));
        js.executeScript("arguments[0].click();", ellipseBtn);
        ExcelLogger.logPass("Open Ellipsis For Clear", "Ellipsis should be clicked", "Clicked ellipsis for clear: " + countyName);

        WebElement clearBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("i.fa.fa-window-close")));
        js.executeScript("arguments[0].click();", clearBtn);
        ExcelLogger.logPass("Confirm Clear", "Clear option should be clicked", "Clicked Clear option for: " + countyName);

        Thread.sleep(1000);
    }

    // ---------- HISTORY FLOW ----------
    @And("I view county history")
    public void i_view_county_history() throws InterruptedException {
        if (lastUpdatedCounty == null) {
            ExcelLogger.logFail("View History", "There should be a last updated county", "No lastUpdatedCounty available", "No updated county found to check history!");
            throw new RuntimeException("⚠️ No updated county found to check history!");
        }

        searchCounty(lastUpdatedCounty);

        WebElement row = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//table[contains(@class,'table')]//td[normalize-space(text())='" + lastUpdatedCounty + "']")));
        js.executeScript("arguments[0].click();", row);
        ExcelLogger.logPass("Select Row For History", "County row should be selected", "Selected row for county: " + lastUpdatedCounty);

        WebElement ellipseBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("span.fa.fa-ellipsis-v")));
        js.executeScript("arguments[0].click();", ellipseBtn);
        ExcelLogger.logPass("Open Ellipsis For History", "Ellipsis should be clicked", "Clicked ellipsis for history");

        WebElement historyBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a.dropdown-item.deSelectAlways.mdlFire.history")));
        js.executeScript("arguments[0].click();", historyBtn);
        ExcelLogger.logPass("Open History Modal", "History option should open modal", "Clicked History option");

        WebElement historyModal = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.modal-content")));
        ExcelLogger.logPass("Verify History Modal", "History modal should be visible", "History modal opened");

        WebElement closeBtn = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("i.fa.fa-close")));
        js.executeScript("arguments[0].click();", closeBtn);
        ExcelLogger.logPass("Close History Modal", "History modal should be closable", "Closed history modal");

        System.out.println("✅ History checked successfully for county: " + lastUpdatedCounty);
    }

    // ---------- LOGOUT FLOW ----------
    @And("I logout")
    public void i_logout() {
        driver.switchTo().defaultContent();
        try {
            WebElement welcomeNote = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//small[@class='welcomenote white-text' and contains(text(),'Welcome')]"))
            );
            js.executeScript("arguments[0].click();", welcomeNote);
            ExcelLogger.logPass("Click Welcome", "Welcome note should be clicked", "Clicked Welcome note");

            WebElement logoutBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//a[@onclick='logout()']"))
            );
            js.executeScript("arguments[0].click();", logoutBtn);
            ExcelLogger.logPass("Logout", "User should be logged out", "Clicked logout button");
            System.out.println("✅ Logged out successfully!");
        } catch (Exception e) {
            ExcelLogger.logFail("Logout", "User should logout cleanly", "Logout failed", e.getMessage());
            System.out.println("❌ Logout failed for this session.");
        } finally {
            if (driver != null) {
                driver.quit();
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
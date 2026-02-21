package stepDefinitions;


import utils.ExcelConfigReader;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class CommonLogin{

    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;
    private Actions actions;

    public static boolean isLoggedIn = false;

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    @Before
    public void beforeScenario() {

        // ✅ ALWAYS initialize driver from DriverManager
        driver = DriverManager.getDriver();
        wait = DriverManager.getWait();
        js = DriverManager.getJS();
        actions = DriverManager.getActions();

        if (!isLoggedIn) {
            try {
                System.out.println("Performing initial login...");
                performLogin();
                isLoggedIn = true;
                System.out.println("Successfully logged in - Ready for scenarios");
            } catch (Exception e) {
                System.out.println("Initial login failed: " + e.getMessage());
                throw new RuntimeException("Login failed", e);
            }
        } else {
            System.out.println("Already logged in - Skipping login step");
        }
    }

    private static String decrypt(String base64Data, byte[] keyBytes) throws Exception {
        byte[] allBytes = Base64.getDecoder().decode(base64Data);
        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(allBytes, 0, iv, 0, IV_LENGTH);
        byte[] cipherBytes = new byte[allBytes.length - IV_LENGTH];
        System.arraycopy(allBytes, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new GCMParameterSpec(TAG_LENGTH, iv));

        return new String(cipher.doFinal(cipherBytes), "UTF-8");
    }

    private void performLogin() throws Exception {

        byte[] keyBytes = Base64.getDecoder().decode(System.getenv("CRED_ENC_KEY"));
        if (keyBytes == null) throw new IllegalStateException("CRED_ENC_KEY not set");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> creds = mapper.readValue(
                Paths.get("src/test/resources/credentials.json").toFile(),
                Map.class
        );

        String username = decrypt(creds.get("username"), keyBytes);
        String password = decrypt(creds.get("password"), keyBytes);

        String appUrl = ExcelConfigReader.readConfig(
                "src/test/resources/EditFile.xlsx",
                "Config",
                "appUrl"
        );

        if (appUrl == null || appUrl.isEmpty()) {
            throw new RuntimeException("❌ appUrl is missing in Config sheet!");
        }

        System.out.println("Navigating to URL from Excel: " + appUrl);

        driver.get(appUrl);

        // ✅ SSL handling (if warning exists)
        try {
            driver.findElement(By.id("details-button")).click();
            Thread.sleep(500);
            driver.findElement(By.id("proceed-link")).click();
            Thread.sleep(500);
        } catch (Exception ignored) {
        }

        WebElement usernameField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("j_username"))
        );
        usernameField.sendKeys(username);

        WebElement passwordField = driver.findElement(By.id("j_password"));
        passwordField.sendKeys(password);

        WebElement loginBtn = driver.findElement(By.cssSelector("button[type='submit']"));
        loginBtn.click();

        wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("artrail-logo")));

        System.out.println("Login successful");
    }

    @Given("I am logged into the application using encrypted credentials from {string}")
    public void i_am_logged_into_the_application_using_encrypted_credentials_from(String filePath) throws Exception {
        if (!isLoggedIn) {
            System.out.println("Logging in using credentials from: " + filePath);
            performLogin();
            isLoggedIn = true;
        } else {
            System.out.println("Already logged in - Skipping login step");
        }
    }
}


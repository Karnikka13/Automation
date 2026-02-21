package stepdefinitions;

import io.cucumber.java.en.Given;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import utils.DecryptCredentials;
import utils.DriverContext;
import utils.ExcelUtils;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class CommonLoginSteps {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String CONFIG_FILE =
            "src/test/resources/testdata/advanced_search_input.xlsx";

    private static final String CONFIG_SHEET =
            "LoginConfig";

    /* ================= DRIVER SETUP ================= */

    private void setupDriver() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.manage().window().maximize();
    }

    /* ================= READ URL FROM EXCEL ================= */

    private String getApplicationUrl() {

        List<Map<String, String>> rows =
                ExcelUtils.getData(CONFIG_FILE, CONFIG_SHEET);

        for (Map<String, String> row : rows) {
            if ("url".equalsIgnoreCase(row.get("Key"))) {
                return row.get("Value");
            }
        }

        throw new RuntimeException(
                "Login URL not found in Excel sheet: " + CONFIG_SHEET);
    }

    /* ================= LOGIN ================= */

    @Given("User is already logged in")
    public void user_is_already_logged_in() {

        try {
            setupDriver();

            // ✅ URL FROM EXCEL
            String appUrl = getApplicationUrl();

            JSONObject creds = DecryptCredentials.getDecryptedCredentials(
                    "src/test/resources/testdata/creds.enc.json"
            );

            driver.get(appUrl);

            // Handle browser security warning (if present)
            try {
                driver.findElement(By.id("details-button")).click();
                driver.findElement(By.id("proceed-link")).click();
            } catch (Exception ignored) {}

            driver.findElement(By.id("j_username"))
                    .sendKeys(creds.getString("username"));

            driver.findElement(By.id("j_password"))
                    .sendKeys(creds.getString("password"));

            driver.findElement(By.cssSelector("button[type='submit']")).click();

            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id("advSearch")));

            DriverContext.set(driver, wait);

            System.out.println("✅ Login successful (URL loaded from Excel)");

        } catch (Exception e) {
            Assert.fail("Login failed → " + e.getMessage());
        }
    }
}

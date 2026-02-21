package stepdefinitions;

import io.cucumber.java.en.*;
import io.github.bonigarcia.wdm.WebDriverManager;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import utils.ExcelUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class LoginExcelSteps {

    private WebDriver driver;
    private WebDriverWait wait;

    private static final String INPUT_FILE =
            "src/test/resources/testdata/advanced_search_input.xlsx";

    private static final String OUTPUT_FILE =
            "src/test/resources/testdata/advanced_search_output.xlsx";

    /* =============================
       GIVEN
    ============================== */
    @Given("Excel login page is opened")
    public void open_login_page() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.manage().window().maximize();
        driver.get("http://192.168.1.18:9090/succeed/login");
    }
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
    /* =============================
       WHEN
    ============================== */
    @When("User logs in using credentials from Excel sheet {string}")
    public void login_using_excel(String sheetName) {

        List<Map<String, String>> users =
                ExcelUtils.getData(INPUT_FILE, sheetName);

        ExcelUtils.formatHeaderRow(OUTPUT_FILE, "Login_Results");

        int rowNum = 1;

        for (Map<String, String> user : users) {

            String username = user.get("username");
            String password = user.get("password");

            String actualResult;
            String status;
            String error = "";

            try {
                driver.findElement(By.id("j_username")).clear();
                driver.findElement(By.id("j_username")).sendKeys(username);

                driver.findElement(By.id("j_password")).clear();
                driver.findElement(By.id("j_password")).sendKeys(password);

                driver.findElement(By.cssSelector("button[type='submit']")).click();

                wait.until(ExpectedConditions.or(
                        ExpectedConditions.visibilityOfElementLocated(By.id("open-button")),
                        ExpectedConditions.visibilityOfElementLocated(By.id("LoginErrMsh"))
                ));

                if (driver.findElements(By.id("open-button")).size() > 0) {
                    actualResult = "Login Successful";
                    status = "PASS";
                    logout();
                } else {
                    actualResult = "Login Failed";
                    status = "FAIL";
                    error = driver.findElement(By.id("LoginErrMsh")).getText();
                }

            } catch (Exception e) {
                actualResult = "Error during login";
                status = "FAIL";
                error = e.getMessage();
            }

            ExcelUtils.writeCell(OUTPUT_FILE, "Login_Results", rowNum,
                    "Scenario", username);

            ExcelUtils.writeCell(OUTPUT_FILE, "Login_Results", rowNum,
                    "Timestamp", LocalDateTime.now().toString());

            ExcelUtils.writeCell(OUTPUT_FILE, "Login_Results", rowNum,
                    "Expected Result", "User should login");

            ExcelUtils.writeCell(OUTPUT_FILE, "Login_Results", rowNum,
                    "Actual Result", actualResult);

            ExcelUtils.writeCell(OUTPUT_FILE, "Login_Results", rowNum,
                    "Status", status);

            ExcelUtils.writeCell(OUTPUT_FILE, "Login_Results", rowNum,
                    "Error Message", error);

            rowNum++;
        }
    }

    /* =============================
       PRIVATE LOGOUT (NO CUCUMBER)
    ============================== */
    private void logout() {
        try {
            driver.findElement(By.cssSelector("a.dropdown-toggle")).click();
            driver.findElement(By.xpath("//a[contains(text(),'Logout')]")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("j_username")));
        } catch (Exception ignored) {}
    }

    /* =============================
       THEN
    ============================== */
    @Then("Login results should be written to Excel output")
    public void close_browser() {
        if (driver != null) {
            driver.quit();
        }
    }
}

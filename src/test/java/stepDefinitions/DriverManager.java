package stepDefinitions;

import util.TestContext;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DriverManager {

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;
    private static Actions actions;

    private DriverManager() {}

    public static WebDriver getDriver() {
        if (driver == null) {

            WebDriverManager.chromedriver().setup();

            Map<String, Object> prefs = new HashMap<>();
            prefs.put("download.default_directory",
                    System.getProperty("user.dir") + "\\downloads");
            prefs.put("download.prompt_for_download", false);
            prefs.put("safebrowsing.enabled", true);
            prefs.put("profile.default_content_setting_values.automatic_downloads", 1);

            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("prefs", prefs);

            // ⭐ ALLOW INSECURE CONTENT
            options.addArguments("--allow-running-insecure-content");
            options.addArguments("--disable-features=InsecureDownloadWarnings");

            driver = new ChromeDriver(options);
            driver.manage().window().maximize();

            wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            js = (JavascriptExecutor) driver;
            actions = new Actions(driver);
        }
        return driver;
    }


    public static WebDriverWait getWait() {
        getDriver();
        return wait;
    }

    public static JavascriptExecutor getJS() {
        getDriver();
        return js;
    }

    public static Actions getActions() {
        getDriver();
        return actions;
    }

    
    public static void quitDriver() {

        if (TestContext.isCashBalancingRun
            || TestContext.isActiveSummaryRun
            || TestContext.isTemplateDefinitionRun) {

            System.out.println("🔒 Driver not closed (module execution)");
            return;
        }

        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }
    



    
}

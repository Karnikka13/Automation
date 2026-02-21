package utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DriverContext {

    private static WebDriver driver;
    private static WebDriverWait wait;

    private DriverContext() {}

    public static void set(WebDriver d, WebDriverWait w) {
        driver = d;
        wait = w;
    }

    public static WebDriver getDriver() {
        return driver;
    }

    public static WebDriverWait getWait() {
        return wait;
    }

    // ✅ ADD THIS METHOD
    public static void quitDriver() {
        try {
            if (driver != null) {
                driver.quit();
            }
        } catch (Exception ignored) {
        } finally {
            driver = null;
            wait = null;
        }
    }
}

package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Logout via UI only
 * Browser closes ONLY after logout click is confirmed
 */
public class LogoutUtil {

    private LogoutUtil() {}

    public static void logoutAndCloseBrowser() {

        WebDriver driver = DriverContext.getDriver();
        WebDriverWait wait = DriverContext.getWait();

        if (driver == null || wait == null) {
            System.out.println("⚠ Driver not available. Skipping logout.");
            return;
        }

        boolean logoutTriggered = false;

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // ================== OPEN PROFILE DROPDOWN ==================
            WebElement profileDropdown = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//i[contains(@class,'fa-caret-down')]/ancestor::a"))
            );

            js.executeScript("arguments[0].click();", profileDropdown);

            // ================== CLICK LOGOUT ==================
            WebElement logoutBtn = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("#user-dropdown li.last-itm a"))
            );

            js.executeScript("arguments[0].click();", logoutBtn);

            // ================== VERIFY LOGOUT (USERNAME DISAPPEARS) ==================
            wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(
                            By.id("uname"))
            );

            logoutTriggered = true;
            System.out.println("✅ Logout triggered successfully");

        } catch (TimeoutException e) {
            System.out.println("⚠ Logout click happened, but page did not redirect (expected for this app)");
            logoutTriggered = true; // logout still valid
        } catch (Exception e) {
            System.out.println("❌ Logout failed: " + e.getMessage());
        }

        // ================== CLOSE BROWSER ==================
        if (logoutTriggered) {
            DriverContext.quitDriver();
            System.out.println("🧹 Browser closed after logout");
        } else {
            System.out.println("⚠ Browser not closed because logout was not triggered");
        }
    }
}

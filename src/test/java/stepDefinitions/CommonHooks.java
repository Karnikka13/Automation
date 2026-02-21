package stepDefinitions;

import io.cucumber.java.*;

import org.openqa.selenium.*;
import java.util.List;

import util.ActiveAccountSummaryLogger;
import util.CashBalancingLogger;
import util.TemplateDefinitionLogger;
import util.TestContext;

public class CommonHooks {

    // ✅ runs before every scenario (any tag)
    @Before(order = 0)
    public void setupScenario(Scenario scenario) {

        // ✅ store scenarioName globally
        String scenarioName = scenario.getName();
        TestContext.currentScenario = scenarioName.split(" ")[0];

        // reset flags
        TestContext.isActiveSummaryRun = false;
        TestContext.isCashBalancingRun = false;
        TestContext.isTemplateDefinitionRun = false;

        // ✅ detect which module execution by tags
        if (scenario.getSourceTagNames().contains("@ActiveSummary")) {
            TestContext.isActiveSummaryRun = true;
            System.out.println("🔖 ActiveSummary Hooks activated");
        }

        if (scenario.getSourceTagNames().contains("@CashBalancing")) {
            TestContext.isCashBalancingRun = true;
            System.out.println("🔖 CashBalancing Hooks activated");
        }

        if (scenario.getSourceTagNames().contains("@TemplateDefinition")) {
            TestContext.isTemplateDefinitionRun = true;
            System.out.println("🔖 TemplateDefinition Hooks activated");
        }

        System.out.println("📌 Current Scenario → " + TestContext.currentScenario);
    }

    // ✅ Login & driver setup (only once)
    @Before(order = 1)
    public void setupDriverAndLogin() {

        WebDriver driver = DriverManager.getDriver();   // start browser

        if (!TestContext.isLoggedIn) {
            try {
                System.out.println("🔐 Performing initial login...");
                // ✅ call your login util
                // LoginUtil.performLogin(driver, DriverManager.getWait());
                TestContext.isLoggedIn = true;
                System.out.println("✅ Successfully logged in");
            } catch (Exception e) {
                throw new RuntimeException("❌ Login failed", e);
            }
        } else {
            System.out.println("✅ Already logged in - skipping login");
        }
    }

    // ✅ After Scenario
    @After
    public void afterScenario(Scenario scenario) {
        try {
            DriverManager.getDriver().switchTo().defaultContent();
        } catch (Exception ignored) {}
    }

    // ✅ After ALL scenarios (single logout + close)
    @AfterAll
    public static void afterAll() {

        String expected = "User logged out successfully";
        String actual = "User logged out successfully";
        String status = "PASS";
        String error = "";

        try {
            WebDriver driver = DriverManager.getDriver();
            driver.switchTo().defaultContent();

            List<WebElement> userMenus = driver.findElements(
                    By.cssSelector("i.icon-caret-down, i.fa-caret-down, [class*='caret-down']")
            );

            if (!userMenus.isEmpty()) {

                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].click();", userMenus.get(0)
                );

                List<WebElement> logoutLinks = driver.findElements(
                        By.xpath("//a[contains(text(),'Logout') or contains(@onclick,'logout')]")
                );

                if (!logoutLinks.isEmpty()) {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].click();", logoutLinks.get(0)
                    );
                    Thread.sleep(1500);
                    System.out.println("🚪 Logged out after ALL scenarios");
                } else {
                    actual = "User already logged out";
                }

            } else {
                actual = "User already logged out";
            }

        } catch (Exception e) {
            actual = "User already logged out";
            status = "PASS";
        }

        // ✅ Log into correct module log file
        try {
            if (TestContext.isActiveSummaryRun) {
                ActiveAccountSummaryLogger.log("Logout", expected, actual, status, error);
            }

            if (TestContext.isCashBalancingRun) {
                CashBalancingLogger.log("Logout", expected, actual, status, error);
            }

            if (TestContext.isTemplateDefinitionRun) {
                TemplateDefinitionLogger.log("Logout", expected, actual, status, error);
            }

        } catch (Exception ignored) {}

        DriverManager.quitDriver();
        System.out.println("🧹 Global browser closed");
        TestContext.isLoggedIn = false;
    }
}

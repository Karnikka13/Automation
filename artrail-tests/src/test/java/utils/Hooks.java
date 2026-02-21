package utils;

import io.cucumber.java.AfterAll;

/**
 * Global hooks class
 * - Performs logout once after all scenarios
 * - Uses reusable LogoutUtil
 */
public class Hooks {

    @AfterAll
    public static void tearDown() {

        System.out.println("📘 All scenarios completed. Triggering logout...");

        LogoutUtil.logoutAndCloseBrowser();
    }
}

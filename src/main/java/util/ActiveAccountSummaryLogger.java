package util;

public class ActiveAccountSummaryLogger {

    private static final String FILE_NAME = "ActiveAccountSummaryLogs.xlsx";

    // Wrapper around your existing ExcelLogger
    public static void log(
            String action,
            String expected,
            String actual,
            String status,
            String remarks
    ) {
        ExcelLogger.logToFile(
                FILE_NAME,
                action,
                expected,
                actual,
                status,
                remarks
        );
    }
}

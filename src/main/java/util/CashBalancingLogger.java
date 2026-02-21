package util;

public class CashBalancingLogger {

    private static final String FILE_NAME = "CashBalancingReportLogs.xlsx";

    // Wrapper around existing ExcelLogger
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

package util;

public class TemplateDefinitionLogger {

    private static final String FILE_NAME = "TemplateDefinitionLogs.xlsx";

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

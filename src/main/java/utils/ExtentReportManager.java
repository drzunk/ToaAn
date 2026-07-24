package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ExtentReportManager {
    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();
    private static final ThreadLocal<Long> TEST_START_MS = new ThreadLocal<>();
    private static final ThreadLocal<String> LAST_STEP = new ThreadLocal<>();
    private static long suiteStartMs;

    public static void initReport() {
        if (extentReports != null) {
            return;
        }
        suiteStartMs = System.currentTimeMillis();

        ExtentSparkReporter spark = new ExtentSparkReporter("test-output/ExtentReport.html");
        spark.config().setTheme(Theme.DARK);
        spark.config().setEncoding("utf-8");
        spark.config().setTimelineEnabled(true);
        spark.config().setOfflineMode(true);
        spark.config().setDocumentTitle("Báo cáo Automation — Dịch vụ Tư pháp Tòa án");
        spark.config().setReportName("Kết quả kiểm thử Tạo đơn điện tử");
        spark.viewConfigurer().viewOrder()
                .as(new ViewName[]{
                        ViewName.DASHBOARD,
                        ViewName.TEST,
                        ViewName.CATEGORY,
                        ViewName.EXCEPTION,
                        ViewName.LOG
                }).apply();

        extentReports = new ExtentReports();
        extentReports.attachReporter(spark);

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        extentReports.setSystemInfo("Dự án", "Demo Dịch vụ Tư pháp — Tòa án");
        extentReports.setSystemInfo("Module", "Tạo đơn mới (E2E)");
        extentReports.setSystemInfo("Môi trường", "Demo GSFPT");
        extentReports.setSystemInfo("URL", "https://demo-dichvutuphap.gsfpt.com/");
        extentReports.setSystemInfo("Framework", "Selenium 4 + TestNG + Page Object");
        extentReports.setSystemInfo("Ngôn ngữ", "Java " + System.getProperty("java.version"));
        extentReports.setSystemInfo("Hệ điều hành", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        extentReports.setSystemInfo("Máy chạy", System.getProperty("user.name"));
        extentReports.setSystemInfo("Thời gian bắt đầu", now);
        extentReports.setSystemInfo("Locale", Locale.getDefault().toString());
    }

    public static void createTest(String testName, String description, String... categories) {
        ExtentTest test = extentReports.createTest(testName, description);
        if (categories != null) {
            for (String category : categories) {
                if (category != null && !category.isBlank()) {
                    test.assignCategory(category);
                }
            }
        }
        test.assignCategory("Tạo đơn E2E");
        CURRENT_TEST.set(test);
        TEST_START_MS.set(System.currentTimeMillis());
        LAST_STEP.set("Khởi tạo testcase");
    }

    /** Giữ tương thích code cũ. */
    public static void createTest(String testName) {
        createTest(testName, "", "Tạo đơn E2E");
    }

    private static ExtentTest test() {
        ExtentTest t = CURRENT_TEST.get();
        if (t == null) {
            throw new IllegalStateException("Chưa khởi tạo ExtentTest cho testcase hiện tại.");
        }
        return t;
    }

    public static void logSection(String title) {
        test().info(MarkupHelper.createLabel(title, ExtentColor.CYAN));
        setLastStep(title);
        System.out.println("📌 " + title);
    }

    public static long markStepStart() {
        return System.currentTimeMillis();
    }

    public static void logStepDone(int step, int total, String name, long startMs, String[][] highlights) {
        long elapsed = System.currentTimeMillis() - startMs;
        String label = String.format("✓ Bước %d/%d — %s · %s",
                step, total, name, TaoDonReportBuilder.formatDuration(elapsed));
        test().info(MarkupHelper.createLabel(label, ExtentColor.GREEN));
        if (highlights != null && highlights.length > 0) {
            logTable(null, highlights);
        }
        setLastStep(String.format("Bước %d/%d: %s", step, total, name));
    }

    public static void logTable(String caption, String[][] rows) {
        if (rows == null || rows.length == 0) {
            return;
        }
        test().info(MarkupHelper.createTable(rows));
        if (caption != null && !caption.isBlank()) {
            test().info("<small><i>" + escapeHtml(caption) + "</i></small>");
        }
    }

    public static void logCodeBlock(String title, String content) {
        if (title != null && !title.isBlank()) {
            test().info("<b>" + escapeHtml(title) + "</b>");
        }
        test().info(MarkupHelper.createCodeBlock(content));
    }

    public static void logStep(String message) {
        test().info("▶ " + message);
        setLastStep(message);
        System.out.println("✅ " + message);
    }

    public static void logInfo(String message) {
        test().info(message);
    }

    public static void logPass(String message) {
        test().pass(message);
        System.out.println("🎉 " + message);
    }

    public static void logPassWithScreenshot(String message, String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            logPass(message);
            return;
        }
        test().pass(message, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
        System.out.println("🎉 " + message);
    }

    public static void logInfoWithScreenshot(String message, String base64Image) {
        if (base64Image == null || base64Image.isBlank()) {
            logInfo(message);
            return;
        }
        try {
            test().info(message, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
        } catch (Exception e) {
            test().info(message);
        }
        System.out.println("📸 " + message);
    }

    public static void logFail(String message) {
        test().fail("❌ " + message);
        System.out.println("❌ [FAILED] " + message);
    }

    public static void logFailWithScreenshot(String message, String base64Image) {
        String detail = buildFailureDetail(message);
        if (base64Image == null || base64Image.isBlank()) {
            test().fail(detail);
        } else {
            test().fail(detail, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
        }
        System.out.println("❌ " + message);
    }

    public static void logSkip(String message) {
        test().skip(message);
        System.out.println("⚠️ [SKIPPED] " + message);
    }

    public static void logWarning(String message) {
        test().warning(message);
        System.out.println("⚠️ [WARNING] " + message);
    }

    public static void logSuiteSummary(int passed, int failed, int skipped) {
        long elapsed = System.currentTimeMillis() - suiteStartMs;
        if (extentReports != null) {
            extentReports.setSystemInfo("Kết quả", passed + " pass / " + failed + " fail / " + skipped + " skip");
            extentReports.setSystemInfo("Thời gian chạy suite", TaoDonReportBuilder.formatDuration(elapsed));
        }
    }

    public static long getTestElapsedMs() {
        Long start = TEST_START_MS.get();
        if (start == null) {
            return 0;
        }
        return System.currentTimeMillis() - start;
    }

    public static String getLastStep() {
        String step = LAST_STEP.get();
        return step != null ? step : "—";
    }

    public static void setLastStep(String step) {
        if (step != null && !step.isBlank()) {
            LAST_STEP.set(step);
        }
    }

    public static void clearTestContext() {
        CURRENT_TEST.remove();
        TEST_START_MS.remove();
        LAST_STEP.remove();
    }

    public static void flushReport() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }

    private static String buildFailureDetail(String message) {
        return "<b>❌ Lỗi:</b> " + escapeHtml(nullToEmpty(message))
                + "<br/><b>📍 Bước cuối:</b> " + escapeHtml(getLastStep())
                + "<br/><b>⏱ Thời gian trước khi fail:</b> "
                + TaoDonReportBuilder.formatDuration(getTestElapsedMs());
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}

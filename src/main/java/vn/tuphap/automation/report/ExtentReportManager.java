package vn.tuphap.automation.report;

import vn.tuphap.automation.config.ConfigReader;

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
import java.util.List;

public class ExtentReportManager {
    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> CURRENT_TEST = new ThreadLocal<>();
    private static final ThreadLocal<Long> TEST_START_MS = new ThreadLocal<>();
    private static final ThreadLocal<String> LAST_STEP = new ThreadLocal<>();
    /** Đã ghi fail lên Extent/console — tránh TestListener log lần 2 (nhân đôi ❌). */
    private static final ThreadLocal<Boolean> FAIL_ALREADY_LOGGED = ThreadLocal.withInitial(() -> false);
    private static long suiteStartMs;

    private static final String PASS_BG = "#198754";
    private static final String FAIL_BG = "#dc3545";
    private static final String WARN_BG = "#ffc107";

    public static void initReport() {
        initReport(null);
    }

    /**
     * Khởi tạo Extent — luôn ghi {@code test-output/ExtentReport.html} (1 file).
     *
     * @param suiteName tên suite TestNG (để gắn loại SMOKE/MID/FULL/LOGIN trên tiêu đề)
     */
    public static synchronized void initReport(String suiteName) {
        if (extentReports != null) {
            return;
        }
        suiteStartMs = System.currentTimeMillis();

        TaoDonExcelTestLog.SuiteKind kind = TaoDonExcelTestLog.SuiteKind.fromSuiteName(suiteName);

        ExtentSparkReporter spark = new ExtentSparkReporter("test-output/ExtentReport.html");
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setEncoding("utf-8");
        spark.config().setTimelineEnabled(false);
        spark.config().setOfflineMode(true);
        spark.config().setDocumentTitle(
                "Báo cáo kiểm thử — " + TaoDonExcelTestLog.PROJECT_CODE + " — Tạo đơn điện tử");
        spark.config().setReportName(kind.titleVi());
        spark.viewConfigurer().viewOrder()
                .as(new ViewName[]{ViewName.DASHBOARD, ViewName.TEST, ViewName.EXCEPTION})
                .apply();

        extentReports = new ExtentReports();
        extentReports.attachReporter(spark);

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        extentReports.setSystemInfo("Mã dự án", TaoDonExcelTestLog.PROJECT_CODE);
        extentReports.setSystemInfo("Module", TaoDonExcelTestLog.MODULE_CODE);
        extentReports.setSystemInfo("Loại bộ kiểm thử", kind.fileTag());
        extentReports.setSystemInfo("Địa chỉ hệ thống (URL)", ConfigReader.getValue("baseUrl", "https://demo-dichvutuphap.gsfpt.com/"));
        extentReports.setSystemInfo("Thời điểm bắt đầu chạy", now);
        extentReports.setSystemInfo("Công cụ", "Selenium + TestNG (tự động hóa trình duyệt)");
        if (vn.tuphap.automation.core.ParallelConfig.isParallel()) {
            extentReports.setSystemInfo("Parallel browsers",
                    String.valueOf(vn.tuphap.automation.config.RunFlowConfig.browsers()));
            extentReports.setSystemInfo("Window size",
                    vn.tuphap.automation.config.RunFlowConfig.windowWidth() + "x"
                            + vn.tuphap.automation.config.RunFlowConfig.windowHeight());
        }
    }

    public static void createTest(String testName, String description, String... categories) {
        initReport();
        ExtentTest test;
        synchronized (ExtentReportManager.class) {
            test = extentReports.createTest(testName, description);
        }
        if (categories != null) {
            for (String category : categories) {
                if (category != null && !category.isBlank()) {
                    // ExtentReports 5.x: assignCategory() xóa mọi \\s+ → Tags/Name dính chữ.
                    // NBSP (\u00A0) không bị xóa và vẫn hiện khoảng trắng trên HTML.
                    test.assignCategory(giuKhoangTrangTag(category));
                }
            }
        }
        CURRENT_TEST.set(test);
        TEST_START_MS.set(System.currentTimeMillis());
        LAST_STEP.set("Bắt đầu kịch bản");
        FAIL_ALREADY_LOGGED.set(false);
    }

    /** Giữ khoảng trắng hiển thị trong Tags (Name / badge) của Extent Spark. */
    private static String giuKhoangTrangTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        return raw.replace(' ', '\u00A0');
    }

    public static void createTest(String testName) {
        createTest(testName, "");
    }

    private static ExtentTest testOrNull() {
        return CURRENT_TEST.get();
    }

    public static void logSection(String title) {
        ExtentTest t = testOrNull();
        if (t == null) {
            return;
        }
        t.info(MarkupHelper.createLabel(title, ExtentColor.BLUE));
        setLastStep(title);
    }

    public static long markStepStart() {
        return System.currentTimeMillis();
    }

    public static void logStepDone(int step, int total, String name, long startMs) {
        logStepDone(step, total, name, startMs, (List<String>) null);
    }

    /** Tương thích: 1 ảnh. */
    public static void logStepDone(int step, int total, String name, long startMs, String base64Screenshot) {
        List<String> shots = null;
        if (base64Screenshot != null && !base64Screenshot.isBlank()) {
            shots = List.of(base64Screenshot);
        }
        logStepDone(step, total, name, startMs, shots);
    }

    public static void logStepDone(int step, int total, String name, long startMs, List<String> screenshots) {
        ExtentTest t = testOrNull();
        if (t == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - startMs;
        String tenBuoc = (name == null || name.isBlank())
                ? TaoDonReportBuilder.tenBuocDayDu(step)
                : name;
        String label = String.format(
                "Hoàn thành bước %d/%d: %s — thời gian thực hiện %s",
                step, total, tenBuoc, TaoDonReportBuilder.formatDuration(elapsed));
        setLastStep(label);
        // Pass chữ xanh; toàn bộ ảnh (1–3) gắn Info có nhãn Đầu/Giữa/Cuối — tránh trùng ảnh.
        logPassInternal(t, label, null);
        logScreenshots("Ảnh ngữ cảnh — " + tenBuoc, screenshots);
    }

    /**
     * Đính kèm 1–3 ảnh: Đầu / Giữa / Cuối form.
     * Ảnh đầu đã gắn vào Pass (nếu có) — các ảnh còn lại gắn Info để không trùng cảm giác.
     */
    public static void logScreenshots(String caption, List<String> screenshots) {
        ExtentTest t = testOrNull();
        if (t == null || screenshots == null || screenshots.isEmpty()) {
            return;
        }
        String[] parts = {"Đầu biểu mẫu (phía trên)", "Giữa biểu mẫu", "Cuối biểu mẫu (phía dưới)"};
        int n = screenshots.size();
        for (int i = 0; i < n; i++) {
            String shot = screenshots.get(i);
            if (shot == null || shot.isBlank()) {
                continue;
            }
            String part = n == 1 ? "Toàn bộ khung nhìn hiện tại"
                    : (i < parts.length ? parts[i] : ("Đoạn " + (i + 1)));
            String msg = caption + " — " + part + " (" + (i + 1) + "/" + n + ")";
            try {
                t.info(msg, MediaEntityBuilder.createScreenCaptureFromBase64String(shot).build());
            } catch (Exception e) {
                t.info(msg);
            }
        }
    }

    public static void logPassWithScreenshots(String message, List<String> screenshots) {
        ExtentTest t = testOrNull();
        if (t != null) {
            logPassInternal(t, message, firstShot(screenshots));
            logScreenshots(message, screenshots);
        }
        System.out.println("🎉 " + message);
    }

    public static void logWarningWithScreenshots(String message, List<String> screenshots) {
        ExtentTest t = testOrNull();
        if (t != null) {
            String box = coloredBox("Cảnh báo — " + message, WARN_BG, "#212529");
            try {
                String first = firstShot(screenshots);
                if (first == null) {
                    t.warning(box);
                } else {
                    t.warning(box, MediaEntityBuilder.createScreenCaptureFromBase64String(first).build());
                }
            } catch (Exception e) {
                t.warning(box);
            }
            if (screenshots != null && screenshots.size() > 1) {
                logScreenshots("Ảnh bổ sung sau Gửi đơn", screenshots.subList(1, screenshots.size()));
            }
        }
        System.out.println("⚠️ [CẢNH BÁO] " + message);
    }

    private static String firstShot(List<String> screenshots) {
        if (screenshots == null || screenshots.isEmpty()) {
            return null;
        }
        for (String s : screenshots) {
            if (s != null && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    public static void logTable(String caption, String[][] rows) {
        ExtentTest t = testOrNull();
        if (t == null || rows == null || rows.length == 0) {
            return;
        }
        if (caption != null && !caption.isBlank()) {
            t.info("<b>" + escapeHtml(caption) + "</b>");
        }
        t.info(MarkupHelper.createTable(rows));
    }

    public static void logCodeBlock(String title, String content) {
        ExtentTest t = testOrNull();
        if (t == null) {
            return;
        }
        if (title != null && !title.isBlank()) {
            t.info("<b>" + escapeHtml(title) + "</b>");
        }
        t.info(MarkupHelper.createCodeBlock(content));
    }

    public static void logInfo(String message) {
        ExtentTest t = testOrNull();
        if (t == null) {
            return;
        }
        t.info(message);
    }

    public static void logPass(String message) {
        ExtentTest t = testOrNull();
        if (t != null) {
            logPassInternal(t, message, null);
        }
        System.out.println("🎉 " + message);
    }

    public static void logInfoWithScreenshot(String message, String base64Image) {
        ExtentTest t = testOrNull();
        if (t == null) {
            return;
        }
        if (base64Image == null || base64Image.isBlank()) {
            t.info(message);
            return;
        }
        try {
            t.info(message, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
        } catch (Exception e) {
            t.info(message);
        }
    }

    public static void logPassWithScreenshot(String message, String base64Image) {
        ExtentTest t = testOrNull();
        if (t != null) {
            logPassInternal(t, message, base64Image);
        }
        System.out.println("🎉 " + message);
    }

    public static void logWarningWithScreenshot(String message, String base64Image) {
        ExtentTest t = testOrNull();
        if (t != null) {
            String box = coloredBox("Cảnh báo — " + message, WARN_BG, "#212529");
            try {
                if (base64Image == null || base64Image.isBlank()) {
                    t.warning(box);
                } else {
                    t.warning(box, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
                }
            } catch (Exception e) {
                t.warning(box);
            }
        }
        System.out.println("⚠️ [CẢNH BÁO] " + message);
    }

    public static void logFail(String message) {
        ExtentTest t = testOrNull();
        if (t == null) {
            System.out.println("❌ [THẤT BẠI] (chưa gắn kịch bản báo cáo) " + message);
            return;
        }
        logFailInternal(t, message, null);
        System.out.println("❌ [THẤT BẠI] " + message);
    }

    public static void logFailWithScreenshot(String message, String base64Image) {
        String detail = "Kịch bản thất bại. Chi tiết lỗi: " + compactFailMessage(nullToEmpty(message));
        ExtentTest t = testOrNull();
        if (t == null) {
            System.out.println("❌ [THẤT BẠI] (chưa gắn kịch bản báo cáo) " + message);
            return;
        }
        logFailInternal(t, detail, base64Image);
        System.out.println("❌ " + message);
    }

    /** Một dòng gọn — bỏ xuống dòng thừa, không kèm bước gần nhất. */
    private static String compactFailMessage(String message) {
        return message.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    public static void logSkip(String message) {
        ExtentTest t = testOrNull();
        if (t != null) {
            t.skip(MarkupHelper.createLabel("Kịch bản bị bỏ qua: " + message, ExtentColor.ORANGE));
        }
    }

    public static void logWarning(String message) {
        ExtentTest t = testOrNull();
        if (t != null) {
            t.warning(coloredBox("Cảnh báo — " + message, WARN_BG, "#212529"));
        }
        System.out.println("⚠️ [CẢNH BÁO] " + message);
    }

    public static void logSuiteSummary(int passed, int failed, int skipped) {
        long elapsed = System.currentTimeMillis() - suiteStartMs;
        if (extentReports != null) {
            extentReports.setSystemInfo(
                    "Kết quả tổng hợp",
                    passed + " đạt / " + failed + " thất bại / " + skipped + " bỏ qua");
            extentReports.setSystemInfo("Tổng thời gian chạy", TaoDonReportBuilder.formatDuration(elapsed));
        }
    }

    public static long getTestElapsedMs() {
        Long start = TEST_START_MS.get();
        return start == null ? 0 : System.currentTimeMillis() - start;
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
        FAIL_ALREADY_LOGGED.remove();
    }

    /** True nếu test đã gọi {@link #logFail} / {@link #logFailWithScreenshot} trước Assert.fail. */
    public static boolean wasFailAlreadyLogged() {
        return Boolean.TRUE.equals(FAIL_ALREADY_LOGGED.get());
    }

    public static synchronized void flushReport() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }

    private static void logPassInternal(ExtentTest t, String message, String base64Screenshot) {
        String box = coloredBox(message, PASS_BG, "#ffffff");
        try {
            if (base64Screenshot != null && !base64Screenshot.isBlank()) {
                t.pass(box, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
            } else {
                t.pass(MarkupHelper.createLabel(message, ExtentColor.GREEN));
            }
        } catch (Exception e) {
            t.pass(MarkupHelper.createLabel(message, ExtentColor.GREEN));
        }
    }

    private static void logFailInternal(ExtentTest t, String message, String base64Screenshot) {
        FAIL_ALREADY_LOGGED.set(true);
        String box = coloredBox(message, FAIL_BG, "#ffffff");
        try {
            if (base64Screenshot != null && !base64Screenshot.isBlank()) {
                t.fail(box, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
            } else {
                t.fail(MarkupHelper.createLabel(message, ExtentColor.RED));
            }
        } catch (Exception e) {
            t.fail(MarkupHelper.createLabel(message, ExtentColor.RED));
        }
    }

    private static String coloredBox(String message, String background, String textColor) {
        return "<div style=\"background-color:" + background + ";color:" + textColor
                + ";padding:8px 12px;border-radius:4px;font-weight:600;line-height:1.4\">"
                + escapeHtml(nullToEmpty(message))
                + "</div>";
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}

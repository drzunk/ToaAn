package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

public class ExtentReportManager {
    private static ExtentReports extentReports;
    private static ExtentTest extentTest;

    public static void initReport() {
        if (extentReports == null) {
            ExtentSparkReporter sparkReporter = new ExtentSparkReporter("test-output/ExtentReport.html");
            sparkReporter.config().setTheme(Theme.STANDARD);
            sparkReporter.config().setDocumentTitle("Báo cáo Automation Tòa Án");
            sparkReporter.config().setReportName("Kết quả kiểm thử Dự án Demo");

            extentReports = new ExtentReports();
            extentReports.attachReporter(sparkReporter);
            extentReports.setSystemInfo("Dự án", "Tòa Án GSFPT");
            extentReports.setSystemInfo("Người chạy", "QA Automation");
        }
    }

    public static void createTest(String testName) {
        extentTest = extentReports.createTest(testName);
    }

    public static void logStep(String message) {
        if (extentTest != null) {
            extentTest.info(message);
            System.out.println("✅ " + message);
        }
    }

    // Ghi log khi test Pass (Màu xanh)
    public static void logPass(String message) {
        if (extentTest != null) {
            extentTest.pass(message);
            System.out.println("🎉 " + message);
        }
    }
    // Dùng để in ra dòng log trạng thái LỖI (Màu Đỏ)
    public static void logFail(String message) {
        if (extentTest != null) {
            extentTest.fail("❌ [FAILED] " + message);
            System.out.println("❌ [FAILED] " + message);
        }
    }

    // Ghi log khi test Fail kèm Ảnh màn hình (Màu đỏ)
    public static void logFailWithScreenshot(String message, String base64Image) {
        if (extentTest != null) {
            extentTest.fail(message, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
            System.out.println("❌ " + message);
        }
    }

    // Ghi log khi test Pass kèm Ảnh màn hình (Màu xanh)
    public static void logPassWithScreenshot(String message, String base64Image) {
        if (extentTest != null) {
            extentTest.pass(message, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
            System.out.println("🎉 " + message);
        }
    }

    // ================== THÊM MỚI 2 HÀM NÀY ==================

    // Dùng để in ra dòng log trạng thái Bỏ Qua (Màu Vàng)
    public static void logSkip(String message) {
        if (extentTest != null) {
            extentTest.skip(message);
            System.out.println("⚠️ [SKIPPED] " + message);
        }
    }

    // Dùng để in ra dòng log trạng thái Cảnh Báo (Màu Cam)
    public static void logWarning(String message) {
        if (extentTest != null) {
            extentTest.warning(message);
            System.out.println("⚠️ [WARNING] " + message);
        }
    }
    // ========================================================

    public static void flushReport() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
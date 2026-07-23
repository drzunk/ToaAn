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

    // Bổ sung: Ghi log khi test Pass (Màu xanh)
    public static void logPass(String message) {
        if (extentTest != null) {
            extentTest.pass(message);
            System.out.println("🎉 " + message);
        }
    }

    // Bổ sung: Ghi log khi test Fail kèm Ảnh màn hình (Màu đỏ)
    public static void logFailWithScreenshot(String message, String base64Image) {
        if (extentTest != null) {
            // Đính kèm ảnh Base64 trực tiếp vào báo cáo
            extentTest.fail(message, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
            System.out.println("❌ " + message);
        }

    }
    // Bổ sung: Ghi log khi test Pass kèm Ảnh màn hình (Màu xanh)
    public static void logPassWithScreenshot(String message, String base64Image) {
        if (extentTest != null) {
            extentTest.pass(message, MediaEntityBuilder.createScreenCaptureFromBase64String(base64Image).build());
            System.out.println("🎉 " + message);
        }
    }

    public static void flushReport() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
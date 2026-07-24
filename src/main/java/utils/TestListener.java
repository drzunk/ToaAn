package utils;

import core.BaseTest;
import core.TaoDonBaseTest;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    @Override
    public void onStart(ITestContext context) {
        ExtentReportManager.initReport();
    }

    @Override
    public void onTestStart(ITestResult result) {
        Object[] parameters = result.getParameters();
        String testName;
        String description;
        String category;

        String loaiViecCategory = null;
        if (parameters != null && parameters.length >= 6) {
            testName = TaoDonReportBuilder.buildTestTitle(parameters);
            description = TaoDonReportBuilder.buildTestDescription(parameters);
            category = TaoDonReportBuilder.getLoaiDonCategory(parameters);
            loaiViecCategory = TaoDonReportBuilder.getLoaiViecCategory(parameters);
        } else {
            testName = result.getMethod().getMethodName();
            description = result.getMethod().getDescription();
            if (description == null || description.isBlank()) {
                description = "Automation testcase";
            }
            category = result.getTestClass().getName();
        }

        if (loaiViecCategory != null) {
            ExtentReportManager.createTest(testName, description, category, loaiViecCategory);
        } else {
            ExtentReportManager.createTest(testName, description, category);
        }

        ExtentReportManager.logSection("🚀 Bắt đầu thực thi");
        logRuntimeContext(result);
        TaoDonReportBuilder.logExecutionPlan();

        if (parameters != null && parameters.length >= 50) {
            TaoDonReportBuilder.logScenarioOverview(parameters);
            TaoDonReportBuilder.logBranchStrategy(
                    parameters[1].toString(),
                    parameters[5].toString(),
                    parameters[25].toString());
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long elapsed = ExtentReportManager.getTestElapsedMs();
        attachScreenshotIfPossible(result, "📸 Màn hình sau khi hoàn tất");

        ExtentReportManager.logTable("📊 Kết quả testcase", new String[][]{
                {"Trạng thái", "✅ PASSED"},
                {"Thời gian thực thi", TaoDonReportBuilder.formatDuration(elapsed)},
                {"Bước cuối cùng", ExtentReportManager.getLastStep()}
        });
        ExtentReportManager.logPass("Kịch bản hoàn tất thành công trong "
                + TaoDonReportBuilder.formatDuration(elapsed));
        ExtentReportManager.clearTestContext();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String errorMsg = extractErrorMessage(result);

        ExtentReportManager.logSection("💥 Phân tích lỗi");
        ExtentReportManager.logTable("Thông tin khi fail", new String[][]{
                {"Thông báo lỗi", errorMsg},
                {"Bước cuối ghi nhận", ExtentReportManager.getLastStep()},
                {"Thời gian trước fail", TaoDonReportBuilder.formatDuration(ExtentReportManager.getTestElapsedMs())},
                {"Test method", result.getMethod().getMethodName()}
        });

        attachScreenshotOnFailure(result, errorMsg);

        if (result.getThrowable() != null) {
            ExtentReportManager.logCodeBlock("Stack trace (rút gọn)",
                    stackTraceSummary(result.getThrowable()));
        }

        ExtentReportManager.clearTestContext();
        ExtentReportManager.flushReport();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String skipMessage = result.getThrowable() != null
                ? result.getThrowable().getMessage()
                : "Testcase bị bỏ qua";
        ExtentReportManager.logSkip(skipMessage);
        ExtentReportManager.clearTestContext();
        ExtentReportManager.flushReport();
    }

    @Override
    public void onFinish(ITestContext context) {
        int passed = context.getPassedTests().size();
        int failed = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();
        ExtentReportManager.logSuiteSummary(passed, failed, skipped);
        ExtentReportManager.flushReport();
    }

    private static String extractErrorMessage(ITestResult result) {
        if (result.getThrowable() == null) {
            return "Lỗi không xác định";
        }
        String msg = result.getThrowable().getMessage();
        if (msg == null || msg.isBlank()) {
            return result.getThrowable().getClass().getSimpleName();
        }
        return msg;
    }

    private static void attachScreenshotOnFailure(ITestResult result, String errorMsg) {
        try {
            WebDriver driver = getDriver(result);
            if (driver != null) {
                String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                ExtentReportManager.logFailWithScreenshot(errorMsg, base64);
            } else {
                ExtentReportManager.logFail(errorMsg);
            }
        } catch (Exception e) {
            ExtentReportManager.logFail(errorMsg + " (Không chụp được screenshot: " + e.getMessage() + ")");
        }
    }

    private static void attachScreenshotIfPossible(ITestResult result, String caption) {
        try {
            WebDriver driver = getDriver(result);
            if (driver != null) {
                String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                ExtentReportManager.logInfoWithScreenshot(caption, base64);
            }
        } catch (Exception ignored) {
        }
    }

    private static void logRuntimeContext(ITestResult result) {
        WebDriver driver = getDriver(result);
        if (driver == null) {
            ExtentReportManager.logInfo("Trình duyệt sẽ khởi tạo khi testcase bắt đầu.");
            return;
        }
        String sessionMode = result.getInstance() instanceof TaoDonBaseTest
                ? "Dùng chung — 1 lần đăng nhập/suite"
                : "Mới — 1 browser/testcase";
        String url;
        try {
            url = driver.getCurrentUrl();
        } catch (Exception e) {
            url = "—";
        }
        ExtentReportManager.logTable("Môi trường runtime", new String[][]{
                {"Trình duyệt", driver.getClass().getSimpleName()},
                {"URL hiện tại", url},
                {"Chế độ session", sessionMode},
                {"Test method", result.getMethod().getMethodName()}
        });
    }

    private static WebDriver getDriver(ITestResult result) {
        Object instance = result.getInstance();
        if (instance instanceof BaseTest baseTest) {
            return baseTest.getDriver();
        }
        return null;
    }

    private static String stackTraceSummary(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getName());
        if (t.getMessage() != null) {
            sb.append(": ").append(t.getMessage());
        }
        sb.append("\n");
        StackTraceElement[] trace = t.getStackTrace();
        int limit = Math.min(trace.length, 8);
        for (int i = 0; i < limit; i++) {
            sb.append("  at ").append(trace[i]).append("\n");
        }
        if (trace.length > limit) {
            sb.append("  ... ").append(trace.length - limit).append(" more");
        }
        return sb.toString();
    }
}

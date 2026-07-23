package utils;

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
        // 1. Lấy tên hàm (Ví dụ: testFlowTaoDon)
        String testName = result.getMethod().getMethodName();

        // 2. ĐỘT PHÁ: Lấy tham số STT từ file Excel để phân biệt từng kịch bản
        Object[] parameters = result.getParameters();
        if (parameters.length > 0) {
            // parameter[0] chính là biến 'stt' đầu tiên mà bạn đã thêm vào hàm @Test
            testName = testName + " - STT " + parameters[0].toString();
        }

        ExtentReportManager.createTest("Test Case: " + testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentReportManager.logPass("✅ Kịch bản chạy thành công rực rỡ!");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String errorMsg = result.getThrowable() != null ? result.getThrowable().getMessage() : "Lỗi không xác định";

        try {
            // ĐÃ SỬA THÀNH core.BaseTest
            Object testClass = result.getInstance();
            org.openqa.selenium.WebDriver driver = ((core.BaseTest) testClass).getDriver();

            if (driver != null) {
                String base64Img = ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.BASE64);
                utils.ExtentReportManager.logFailWithScreenshot(errorMsg, base64Img);
            } else {
                utils.ExtentReportManager.logFail(errorMsg);
            }
        } catch (Exception e) {
            // Fallback: Lỡ lỗi không lấy được driver thì vẫn in log Đỏ ra
            utils.ExtentReportManager.logFail(errorMsg);
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String skipMessage = result.getThrowable() != null ? result.getThrowable().getMessage() : "Bị bỏ qua";
        ExtentReportManager.logSkip(skipMessage);
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flushReport();
    }
}

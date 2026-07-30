package vn.tuphap.automation.report;

import vn.tuphap.automation.data.TaoDonScenario;

import vn.tuphap.automation.config.ConfigReader;

import vn.tuphap.automation.core.BaseTest;
import vn.tuphap.automation.flow.StepBlockedException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import vn.tuphap.automation.ui.WebUI;

public class TestListener implements ITestListener {

    @Override
    public void onStart(ITestContext context) {
        String suiteName = context.getSuite().getName();
        vn.tuphap.automation.config.RunFlowConfig.applyKnownSystemAliases();
        vn.tuphap.automation.config.RunFlowConfig.printSummary(suiteName);
        vn.tuphap.automation.core.ScenarioDispatch.reset();
        ExtentReportManager.initReport(suiteName);
        TaoDonExcelTestLog.SuiteKind kind = TaoDonExcelTestLog.SuiteKind.fromSuiteName(suiteName);
        System.setProperty("taodon.suite", kind.folder());
        if (vn.tuphap.automation.config.RunFlowConfig.parallel()
                || (suiteName != null && suiteName.toLowerCase().contains("parallel"))) {
            System.setProperty("taodon.parallel", "true");
            System.setProperty("taodon.threads",
                    String.valueOf(vn.tuphap.automation.config.RunFlowConfig.browsers()));
        }
        TaoDonExcelTestLog.initSuite(suiteName);
    }

    @Override
    public void onTestStart(ITestResult result) {
        TestActionLog.beginTest();
        TaoDonScenario scenario = TaoDonReportBuilder.asScenario(result.getParameters());
        String testName;
        String description;
        String category = "Kiểm thử chung";

        if (scenario != null) {
            testName = TaoDonReportBuilder.buildTestTitle(scenario);
            description = TaoDonReportBuilder.buildTestDescription(scenario);
            category = TaoDonReportBuilder.getLoaiDonCategory(scenario);
            ExtentReportManager.createTest(testName, description, category);
            TaoDonReportBuilder.logScenarioOverview(scenario);
        } else {
            testName = tenTestDeHieu(result);
            description = result.getMethod().getDescription();
            if (description == null || description.isBlank()) {
                description = "Kịch bản kiểm thử không dùng dữ liệu tạo đơn động.";
            }
            ExtentReportManager.createTest(testName, description, category);
            ExtentReportManager.logInfo("Bắt đầu: " + description);
            if (isLoginTest(result)) {
                TaoDonExcelTestLog.bindLoginCase(
                        ConfigReader.getValue("username", ""),
                        ConfigReader.getValue("password", ""),
                        ConfigReader.getValue("baseUrl", "https://demo-dichvutuphap.gsfpt.com/"));
            }
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long elapsed = ExtentReportManager.getTestElapsedMs();
        ExtentReportManager.logPass(
                "Kịch bản đạt — tổng thời gian "
                        + TaoDonReportBuilder.formatDuration(elapsed));
        if (isLoginTest(result)) {
            TaoDonExcelTestLog.setTrangThai(TaoDonExcelTestLog.ST_DAT);
            TaoDonExcelTestLog.setKetQuaThucTe(
                    "Đăng nhập thành công, bảng điều khiển hiển thị Nộp đơn mới.");
            TaoDonExcelTestLog.setGhiChuKetQua("");
        }
        TaoDonExcelTestLog.recordFinished(TaoDonExcelTestLog.ST_DAT, elapsed);
        ExtentReportManager.clearTestContext();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String errorMsg = extractErrorMessage(result);
        if (WebUI.isBrowserClosed(result.getThrowable())
                || result.getThrowable() instanceof vn.tuphap.automation.flow.BrowserClosedException) {
            // Chỉ dừng thread/browser này — không quitAll / không kéo sập suite.
            vn.tuphap.automation.core.DriverContext.abortCurrentThread(errorMsg);
        }
        // StepBlockedException / đã logFail trước Assert.fail — không ghi Extent lần 2.
        if (!isAlreadyReportedStepBlock(result.getThrowable())
                && !ExtentReportManager.wasFailAlreadyLogged()) {
            attachScreenshotOnFailure(result, errorMsg);
        }
        String shortMsg = rutGonLoiChoTester(errorMsg);
        TaoDonExcelTestLog.setTrangThai(TaoDonExcelTestLog.ST_THAT_BAI);
        TaoDonExcelTestLog.setKetQuaThucTe(shortMsg);
        TaoDonExcelTestLog.recordFinished(TaoDonExcelTestLog.ST_THAT_BAI, ExtentReportManager.getTestElapsedMs());
        ExtentReportManager.clearTestContext();
        ExtentReportManager.flushReport();
    }

    /** Fail bước đã được báo cáo (Extent + ảnh) trước khi ném exception. */
    private static boolean isAlreadyReportedStepBlock(Throwable t) {
        return t instanceof StepBlockedException;
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String skipMessage = result.getThrowable() != null
                ? WebUI.friendlyBrowserMessage(result.getThrowable())
                : "Không có lý do chi tiết";
        ExtentReportManager.logSkip(skipMessage);
        TaoDonExcelTestLog.setGhiChuKetQua(rutGonLoiChoTester(skipMessage));
        TaoDonExcelTestLog.setTrangThai(TaoDonExcelTestLog.ST_BO_QUA);
        TaoDonExcelTestLog.setKetQuaThucTe(rutGonLoiChoTester(skipMessage));
        TaoDonExcelTestLog.recordFinished(TaoDonExcelTestLog.ST_BO_QUA, ExtentReportManager.getTestElapsedMs());
        ExtentReportManager.clearTestContext();
        ExtentReportManager.flushReport();
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.logSuiteSummary(
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
        ExtentReportManager.flushReport();
        TaoDonExcelTestLog.saveIfNeeded();
    }

    private static boolean isLoginTest(ITestResult result) {
        if (result.getMethod() == null) {
            return false;
        }
        String method = result.getMethod().getMethodName();
        return "testDangNhapThanhCong".equals(method)
                || TaoDonExcelTestLog.currentKind() == TaoDonExcelTestLog.SuiteKind.LOGIN;
    }

    private static String rutGonLoiChoTester(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        if (raw.contains("dừng kịch bản ngay")) {
            return raw.replace("❌ ", "").trim();
        }
        if (WebUI.isBrowserClosed(new RuntimeException(raw))
                || raw.toLowerCase().contains("no such window")
                || raw.toLowerCase().contains("target window already closed")
                || raw.toLowerCase().contains("web view not found")) {
            return "Trình duyệt đã đóng hoặc mất kết nối — không nhận được phản hồi từ trang web.";
        }
        String msg = raw.replace("❌ ", "").trim();
        String lower = msg.toLowerCase();
        if (lower.contains("timeout") || lower.contains("hết thời gian chờ")) {
            return "Không nhận được phản hồi từ trang web trong thời gian chờ.";
        }
        if (msg.length() > 300) {
            return msg.substring(0, 300) + "…";
        }
        return msg;
    }

    private static String tenTestDeHieu(ITestResult result) {
        String method = result.getMethod().getMethodName();
        return switch (method) {
            case "testDangNhapThanhCong" -> "Đăng nhập thành công vào hệ thống";
            case "testChinhSuaNoiDungTuXemLai" ->
                    "Chỉnh sửa đơn từ Xem lại (Xem trước đơn → bước 1), nộp lại rồi đối chiếu Xem lại";
            case "testFlowTaoDon" -> "Luồng tạo đơn đầy đủ 6 bước";
            case "syncMasterDataFromUi" -> "Đồng bộ danh mục dữ liệu từ giao diện";
            default -> method;
        };
    }

    private static String extractErrorMessage(ITestResult result) {
        if (result.getThrowable() == null) {
            return "Lỗi không xác định";
        }
        return WebUI.friendlyBrowserMessage(result.getThrowable());
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
            System.out.println("❌ [THẤT BẠI] " + errorMsg + " (ảnh chụp: " + e.getMessage() + ")");
            try {
                ExtentReportManager.logFail(errorMsg);
            } catch (Exception ignored) {
            }
        }
    }

    private static WebDriver getDriver(ITestResult result) {
        Object instance = result.getInstance();
        if (instance instanceof BaseTest baseTest) {
            return baseTest.getDriver();
        }
        return null;
    }
}

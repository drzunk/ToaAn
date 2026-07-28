package vn.tuphap.automation.core;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import vn.tuphap.automation.flow.BrowserClosedException;
import vn.tuphap.automation.pages.DashboardPage;
import vn.tuphap.automation.pages.LoginPage;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.report.ExtentReportManager;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

/**
 * Một browser + một lần đăng nhập cho cả class TaoDonTest (setup, không phải testcase trùng LoginTest).
 * Trình duyệt bị đóng tay → dừng ngay, không tự mở lại.
 */
public abstract class TaoDonBaseTest extends BaseTest {

    @Override
    protected boolean reuseBrowserSession() {
        return true;
    }

    @BeforeClass(alwaysRun = true)
    public void loginOnceForSuite() {
        createDriver();
        System.out.println("=== THIẾT LẬP SESSION — ĐĂNG NHẬP MỘT LẦN ===");
        TestActionLog.pause();
        ExtentReportManager.initReport();
        ExtentReportManager.createTest(
                "Thiết lập session — đăng nhập dùng chung",
                "Setup trước khi chạy kịch bản tạo đơn (không phải test đăng nhập độc lập).");
        try {
            performLogin();
            new DashboardPage(driver).waitForDashboard(WaitConfig.DASHBOARD);
            ExtentReportManager.logPass("Session sẵn sàng — các kịch bản tạo đơn sẽ dùng chung trình duyệt này.");
            System.out.println(" ✅ Session dùng chung — sẵn sàng chạy các testcase tạo đơn.");
        } catch (BrowserClosedException ex) {
            ExtentReportManager.logFail(ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            ExtentReportManager.logFail("Đăng nhập dùng chung thất bại: " + ex.getMessage());
            throw ex;
        } finally {
            ExtentReportManager.clearTestContext();
            ExtentReportManager.flushReport();
            TestActionLog.resume();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void prepareNewDonForm() {
        ensureDashboard();
    }

    @AfterMethod(alwaysRun = true)
    public void resetToDashboardAfterTest() {
        try {
            ensureDashboard();
        } catch (BrowserClosedException ex) {
            System.out.println(" ⚠ " + ex.getMessage());
        } catch (Exception e) {
            System.out.println(" ⚠ Không reset được về Dashboard: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeSharedBrowser() {
        destroyDriver();
    }

    protected void performLogin() {
        webUI.failIfBrowserClosed();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.openPage();
        webUI.failIfBrowserClosed();
        loginPage.chonDangNhapBangTaiKhoan();
        loginPage.thucHienDangNhap(
                ConfigReader.getValue("username"),
                ConfigReader.getValue("password"),
                "");
    }

    /**
     * Về Dashboard nhanh: nếu đang ở form/xem lại thì mở lại trang chủ ngay,
     * không chờ lâu trên trang sai. Sau navigate mới chờ đủ {@link WaitConfig#DASHBOARD_AFTER_NAV}.
     */
    protected void ensureDashboard() {
        if (driver == null || webUI == null) {
            throw new BrowserClosedException(
                    "Trình duyệt chưa mở hoặc đã đóng — dừng kịch bản ngay.");
        }
        webUI.failIfBrowserClosed();

        DashboardPage dashboardPage = new DashboardPage(driver);
        if (dashboardPage.isDashboardVisible()) {
            return;
        }

        LoginPage loginPage = new LoginPage(driver);
        try {
            driver.get(loginPage.getPageUrl());
        } catch (BrowserClosedException ex) {
            throw ex;
        } catch (Exception e) {
            webUI.failIfBrowserClosed(e);
            System.out.println(" ⚠ Không mở được trang chủ: " + e.getMessage());
        }
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        webUI.failIfBrowserClosed();

        if (dashboardPage.isDashboardVisible()) {
            return;
        }

        try {
            dashboardPage.waitForDashboard(WaitConfig.DASHBOARD_AFTER_NAV);
            return;
        } catch (BrowserClosedException ex) {
            throw ex;
        } catch (RuntimeException ignored) {
        }

        if (loginPage.isLoginFormVisible()) {
            System.out.println(" ⚠ Session hết hạn — đăng nhập lại...");
            performLogin();
            dashboardPage.waitForDashboard(WaitConfig.DASHBOARD);
        }
    }
}

package core;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import pages.DashboardPage;
import pages.LoginPage;
import utils.ConfigReader;

/**
 * Option 1: Một browser + một lần đăng nhập cho cả class TaoDonTest.
 */
public abstract class TaoDonBaseTest extends BaseTest {

    @Override
    protected boolean reuseBrowserSession() {
        return true;
    }

    @BeforeClass(alwaysRun = true)
    public void loginOnceForSuite() {
        createDriver();
        System.out.println("=== ĐĂNG NHẬP MỘT LẦN CHO CẢ SUITE ===");
        performLogin();
        webUI.zoomPage("80%");
        ensureDashboard();
        System.out.println(" ✅ Session dùng chung — sẵn sàng chạy các testcase tạo đơn.");
    }

    @BeforeMethod(alwaysRun = true)
    public void prepareNewDonForm() {
        ensureDashboard();
    }

    @AfterMethod(alwaysRun = true)
    public void resetToDashboardAfterTest() {
        try {
            ensureDashboard();
        } catch (Exception e) {
            System.out.println(" ⚠ Không reset được về Dashboard: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeSharedBrowser() {
        destroyDriver();
    }

    protected void performLogin() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.openPage();
        loginPage.chonDangNhapBangTaiKhoan();
        loginPage.thucHienDangNhap(
                ConfigReader.getValue("username"),
                ConfigReader.getValue("password"),
                "");
    }

    protected void ensureDashboard() {
        DashboardPage dashboardPage = new DashboardPage(driver);
        if (dashboardPage.isDashboardVisible()) {
            return;
        }

        LoginPage loginPage = new LoginPage(driver);
        driver.get(loginPage.getPageUrl());
        webUI.sleep(2);

        if (loginPage.isLoginFormVisible()) {
            System.out.println(" ⚠ Session hết hạn — đăng nhập lại...");
            performLogin();
        }

        dashboardPage.waitForDashboard(15);
    }
}

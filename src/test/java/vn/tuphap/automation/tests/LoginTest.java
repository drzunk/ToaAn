package vn.tuphap.automation.tests;

import vn.tuphap.automation.core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import vn.tuphap.automation.pages.DashboardPage;
import vn.tuphap.automation.pages.LoginPage;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

/** Kiểm tra đăng nhập độc lập — chạy suite testng-login.xml (không nằm trong smoke). */
public class LoginTest extends BaseTest {

    @Test(groups = {"login"},
            description = "Đăng nhập thành công và thấy Dashboard")
    public void testDangNhapThanhCong() {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginUntilDashboard(
                ConfigReader.getValue("username"),
                ConfigReader.getValue("password"),
                3,
                WaitConfig.DASHBOARD_LOGIN);

        DashboardPage dashboardPage = new DashboardPage(driver);
        Assert.assertTrue(dashboardPage.isDashboardVisible(),
                "Sau đăng nhập phải thấy Dashboard [Nộp đơn mới]");
        new WebUI(driver).captureOverview("Ảnh tổng quan — Dashboard sau đăng nhập thành công");
    }
}

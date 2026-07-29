package vn.tuphap.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

public class DashboardPage {
    private WebDriver driver;
    private WebUI webUI;

    // 1. Khai báo Locators trên màn hình Dashboard
    private By btnNopDonMoi = By.xpath(
            "//a[contains(., 'Nộp đơn mới')]"
                    + " | //button[contains(., 'Nộp đơn mới')]"
                    + " | //*[@role='link' and contains(., 'Nộp đơn mới')]");

    // 2. Constructor
    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    // 3. Các hàm thao tác (Nghiệp vụ)
    public boolean isDashboardVisible() {
        return webUI.existsNow(btnNopDonMoi);
    }

    public void waitForDashboard(int timeoutSeconds) {
        webUI.waitUntilVisible(btnNopDonMoi, timeoutSeconds, "Bảng điều khiển [Nộp đơn mới]");
    }

    /**
     * Về trang chủ nếu cần, chờ [Nộp đơn mới]; {@code reLogin} khi form đăng nhập hiện lại.
     */
    public void ensureReady(LoginPage loginPage, Runnable reLogin, int timeoutSeconds) {
        if (isDashboardVisible()) {
            return;
        }
        System.out.println(" ⏳ Bảng điều khiển chưa hiện — mở lại trang chủ...");
        loginPage.openPage();
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        if (isDashboardVisible()) {
            return;
        }
        try {
            waitForDashboard(WaitConfig.DASHBOARD_AFTER_NAV);
            return;
        } catch (RuntimeException ignored) {
        }
        if (loginPage.isLoginFormVisible() && reLogin != null) {
            System.out.println(" ⚠ Session hết hạn — đăng nhập lại...");
            reLogin.run();
            waitForDashboard(timeoutSeconds);
            return;
        }
        waitForDashboard(timeoutSeconds);
    }

    public void clickNopDonMoi() {
        webUI.clickElement(btnNopDonMoi, "Nút [Nộp đơn mới]");
    }
}
package vn.tuphap.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.List;

public class LoginPage {
    private WebDriver driver;
    private WebUI webUI;

    private final String pageUrl;

    private By btnDangNhapBangTaiKhoan = By.xpath("//button[contains(., 'Đăng nhập bằng tài khoản')]");
    private By txtUsername = By.id("login-cccd");
    private By txtPassword = By.id("login-password");
    private By txtCaptcha = By.id("login-captcha");
    private By lblCaptchaText = By.xpath("//span[contains(@class, 'font-mono') and contains(@style, 'letter-spacing')]");
    private By btnSubmitLogin = By.xpath("//button[text()='Đăng nhập']");
    private By btnRefreshCaptcha = By.xpath(
            "//button[contains(@aria-label, 'captcha') or contains(@aria-label, 'Captcha')"
                    + " or contains(@title, 'captcha') or contains(@title, 'Captcha')"
                    + " or contains(., 'làm mới') or contains(@class, 'refresh')]"
                    + "[ancestor::form[.//input[@id='login-captcha']] or preceding::input[@id='login-captcha']"
                    + " or following::input[@id='login-captcha']]");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
        this.pageUrl = ConfigReader.getValue("baseUrl", "https://demo-dichvutuphap.gsfpt.com/");
    }

    public void openPage() {
        try {
            driver.get(pageUrl);
            System.out.println("Đã mở trang: " + pageUrl);
        } catch (Exception e) {
            webUI.failIfBrowserClosed(e);
            throw e;
        }
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public boolean isLoginFormVisible() {
        return webUI.existsNow(btnDangNhapBangTaiKhoan) || webUI.existsNow(txtUsername);
    }

    public void chonDangNhapBangTaiKhoan() {
        webUI.clickElement(btnDangNhapBangTaiKhoan, "Nút [Đăng nhập bằng tài khoản]");
    }

    public void thucHienDangNhap(String user, String pass, String manualCaptcha) {
        // ÉP DÙNG setText (KHÔNG KIỂM TRA ĐIỀU KIỆN) ĐỂ LỖI PHÁT LÀ FAILED LUÔN
        webUI.setText(txtUsername, user, "Ô nhập [CCCD/Tên đăng nhập]");
        webUI.setText(txtPassword, pass, "Ô nhập [Mật khẩu]");

        String captcha = manualCaptcha;
        if(captcha == null || captcha.isEmpty()) {
            captcha = webUI.docCaptcha(lblCaptchaText);
        }

        // CHỐT CHẶN: Rỗng captcha là văng lỗi
        if (captcha == null || captcha.isEmpty()) {
            throw new RuntimeException("❌ Lỗi: Không thể đọc được Captcha!");
        }

        webUI.setText(txtCaptcha, captcha, "Ô nhập [Captcha]");
        webUI.captureOverview("Ảnh tổng quan — biểu mẫu đăng nhập đã điền");
        webUI.clickElement(btnSubmitLogin, "Nút [Đăng nhập]");
    }

    /**
     * Đăng nhập kèm retry captcha — poll Dashboard sau mỗi lần submit (dev cluster đôi khi chậm).
     */
    public void loginUntilDashboard(String user, String pass, int maxAttempts, int dashboardTimeoutSec) {
        DashboardPage dashboard = new DashboardPage(driver);
        int attempts = Math.max(1, maxAttempts);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (attempt > 1) {
                System.out.println(" ⏳ Thử đăng nhập lại (lần " + attempt + "/" + attempts + ")...");
            }
            openPage();
            // Poll ngắn thay cho "ngủ một nhịp rồi hỏi đúng một lần": trả lời ngay khi Dashboard
            // hiện, mà lỡ nhịp cũng không đẩy cả case đi đăng nhập captcha lại từ đầu — nên khoảng
            // này không được nằm trong taodon.wait.scale.
            if (pollDashboardVisible(dashboard, 2)) {
                System.out.println(" ✅ Đã có session — Dashboard sẵn sàng.");
                return;
            }
            chonDangNhapBangTaiKhoan();
            if (attempt > 1) {
                refreshCaptchaIfPresent();
            }
            thucHienDangNhap(user, pass, "");
            if (pollDashboardVisible(dashboard, dashboardTimeoutSec)) {
                System.out.println(" ✅ Đăng nhập thành công — Dashboard sẵn sàng.");
                return;
            }
            System.out.println(" ⚠ Lần " + attempt + ": chưa thấy Dashboard sau "
                    + dashboardTimeoutSec + "s (có thể sai captcha hoặc hệ thống chậm).");
        }
        dashboard.waitForDashboard(dashboardTimeoutSec);
    }

    /**
     * Đọc captcha thật rồi trả về một chuỗi CHẮC CHẮN sai (đảo ngược, hoặc thêm hậu tố nếu đảo ngược
     * trùng bản gốc) — dùng cho ca âm "sai captcha". Không đọc được captcha thật thì trả về 1 chuỗi
     * cố định rõ ràng là rác.
     */
    public String layCaptchaSai() {
        String dungThuc = webUI.docCaptcha(lblCaptchaText);
        if (dungThuc == null || dungThuc.isBlank()) {
            return "XX0000";
        }
        String daoNguoc = new StringBuilder(dungThuc).reverse().toString();
        return daoNguoc.equals(dungThuc) ? dungThuc + "X" : daoNguoc;
    }

    /** Thông báo lỗi/toast hệ thống đang hiển thị sau khi thử đăng nhập — dùng cho ca âm đăng nhập. */
    public List<String> layThongBaoLoiDangNhap() {
        return webUI.collectSystemFeedbackMessages();
    }

    /**
     * Chờ 1 trong 2: Dashboard hiện ra (đăng nhập thành công) hoặc hệ thống hiện thông báo lỗi —
     * dùng sau ca âm, khi biết trước sẽ KHÔNG đăng nhập được nên không cần retry như
     * {@link #loginUntilDashboard}.
     */
    public boolean choPhanHoiSauDangNhap(int timeoutSec) {
        DashboardPage dashboard = new DashboardPage(driver);
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (dashboard.isDashboardVisible() || webUI.hasSystemFeedback()) {
                return true;
            }
            webUI.sleepMillis(400);
        }
        return dashboard.isDashboardVisible() || webUI.hasSystemFeedback();
    }

    private void refreshCaptchaIfPresent() {
        if (!webUI.existsNow(btnRefreshCaptcha)) {
            return;
        }
        try {
            webUI.clickElement(btnRefreshCaptcha, "Nút [Làm mới Captcha]");
            webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        } catch (RuntimeException ignored) {
        }
    }

    private boolean pollDashboardVisible(DashboardPage dashboard, int timeoutSec) {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (dashboard.isDashboardVisible()) {
                return true;
            }
            webUI.sleepMillis(400);
        }
        return dashboard.isDashboardVisible();
    }
}

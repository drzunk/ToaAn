package vn.tuphap.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.WebUI;

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

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
        this.pageUrl = ConfigReader.getValue("baseUrl", "https://demo-dichvutuphap.gsfpt.com/");
    }

    public void openPage() {
        driver.get(pageUrl);
        System.out.println("Đã mở trang: " + pageUrl);
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
        TestActionLog.buoc(0, "Đăng nhập hệ thống", "Màn đăng nhập");
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
}

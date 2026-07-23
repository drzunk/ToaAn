package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WebUI;

public class LoginPage {
    private WebDriver driver;
    private WebUI webUI;

    private String pageUrl = "https://demo-dichvutuphap.gsfpt.com/";

    private By btnDangNhapBangTaiKhoan = By.xpath("//button[contains(., 'Đăng nhập bằng tài khoản')]");
    private By txtUsername = By.id("login-cccd");
    private By txtPassword = By.id("login-password");
    private By txtCaptcha = By.id("login-captcha");
    private By lblCaptchaText = By.xpath("//span[contains(@class, 'font-mono') and contains(@style, 'letter-spacing')]");
    private By btnSubmitLogin = By.xpath("//button[text()='Đăng nhập']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void openPage() {
        driver.get(pageUrl);
        System.out.println("Đã mở trang: " + pageUrl);
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
            throw new RuntimeException("❌ Lỗi: Bot không thể đọc được Captcha!");
        }

        webUI.setText(txtCaptcha, captcha, "Ô nhập [Mã Captcha]");

        webUI.captureScreen("Đã điền đầy đủ thông tin Đăng nhập");
        webUI.clickElement(btnSubmitLogin, "Nút [Đăng nhập]");
    }
}

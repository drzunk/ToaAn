package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.ExtentReportManager;
import utils.WebUI;

public class LoginPage {
    private WebDriver driver;
    private WebUI webUI;

    // 1. Khai báo URL của trang Demo
    private String pageUrl = "https://demo-dichvutuphap.gsfpt.com/";

    // 2. Khai báo các Locators
    private By btnDangNhapBangTaiKhoan = By.xpath("//button[contains(., 'Đăng nhập bằng tài khoản')]");
    private By txtUsername = By.id("login-cccd");
    private By txtPassword = By.id("login-password");

    // --- KHU VỰC CAPTCHA ---
    // 1. Ô NHẬP CAPTCHA (Nơi bot sẽ điền chữ vào - Bạn f12 kiểm tra lại XPath này nhé)
// 1. Ô NHẬP CAPTCHA (Đã bắt chính xác 100% bằng ID)
    private By txtCaptcha = By.id("login-captcha");
    // 2. THẺ CHỨA CHỮ CAPTCHA (Lỗ hổng của Dev, nơi Bot sẽ đọc chữ ra)
    private By lblCaptchaText = By.xpath("//span[contains(@class, 'font-mono') and contains(@style, 'letter-spacing')]");
    // -----------------------

    private By btnSubmitLogin = By.xpath("//button[text()='Đăng nhập']");

    // 3. Constructor khởi tạo
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    // 4. Các hàm thao tác (Nghiệp vụ)
    public void openPage() {
        driver.get(pageUrl);
        System.out.println("Đã mở trang: " + pageUrl);
    }

    public void chonDangNhapBangTaiKhoan() {
        webUI.clickElement(btnDangNhapBangTaiKhoan, "Nút [Đăng nhập bằng tài khoản]");
    }

    public void thucHienDangNhap(String cccd, String password, String captchaTuExcel) {
        webUI.setText(txtUsername, cccd, "Ô nhập [CCCD/Tên đăng nhập]");
        webUI.setText(txtPassword, password, "Ô nhập [Mật khẩu]");

        if (captchaTuExcel != null && !captchaTuExcel.isEmpty()) {
            // Nếu Excel có sẵn Captcha tĩnh thì điền luôn
            webUI.setText(txtCaptcha, captchaTuExcel, "Ô nhập [Mã Captcha]");
        } else {
            // HACK CAPTCHA: Lấy trực tiếp text từ DOM vì Dev để lộ HTML :))
            String textCaptcha = driver.findElement(lblCaptchaText).getText().trim();

            ExtentReportManager.logStep("✅ Đã bắt sống Captcha từ HTML: <b>" + textCaptcha + "</b>");

            // Điền chữ vừa lấy được vào ô nhập
            webUI.setText(txtCaptcha, textCaptcha, "Ô nhập [Mã Captcha]");
        }

        // Click nút đăng nhập
        webUI.clickElement(btnSubmitLogin, "Nút [Đăng nhập]");
    }
}
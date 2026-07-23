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
    // 1. Ô NHẬP CAPTCHA (Bắt chính xác bằng ID)
    private By txtCaptcha = By.id("login-captcha");

    // 2. THẺ CHỨA CHỮ CAPTCHA (Đã đồng bộ lại tên biến thành lblCaptchaText cho khớp)
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

    public void thucHienDangNhap(String user, String pass, String manualCaptcha) {
        // PHẢI GỌI QUA webUI ĐỂ ĐƯỢC BẢO VỆ VÀ CHECK ĐIỀU KIỆN
        webUI.setTextWithCheck(txtUsername, user, "Ô nhập [CCCD/Tên đăng nhập]");
        webUI.setTextWithCheck(txtPassword, pass, "Ô nhập [Mật khẩu]");

        String captcha = manualCaptcha;
        if(captcha == null || captcha.isEmpty()) {
            // Đọc Captcha bằng AI từ thẻ lblCaptchaText
            captcha = webUI.docCaptcha(lblCaptchaText);
        }
        webUI.setTextWithCheck(txtCaptcha, captcha, "Ô nhập [Mã Captcha]");

        // CHỤP ẢNH TẠI ĐÂY ĐỂ LẤY KHOẢNH KHẮC ĐIỀN ĐỦ USER, PASS, CAPTCHA
        webUI.captureScreen("Đã điền đầy đủ thông tin Đăng nhập");

        // Click nút đăng nhập thông qua webUI
        webUI.clickElement(btnSubmitLogin, "Nút [Đăng nhập]");
    }
}
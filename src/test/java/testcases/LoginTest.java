package testcases;

import core.BaseTest;
import org.testng.annotations.Test;
import pages.LoginPage;

public class LoginTest extends BaseTest {

    @Test
    public void testDangNhapThanhCong() {
        System.out.println("=== BẮT ĐẦU KỊCH BẢN ĐĂNG NHẬP ===");

        // Khởi tạo trang Login
        LoginPage loginPage = new LoginPage(driver);

        // Bước 1: Mở trang web Demo
        loginPage.openPage();

        // Bước 2: Bấm chọn phương thức Đăng nhập bằng tài khoản
        loginPage.chonDangNhapBangTaiKhoan();

        // Bước 3: Điền form đăng nhập
        // Bot sẽ tự điền Username và Password cực nhanh.
        // Tham số thứ 3 (Captcha) đang để rỗng "" để kích hoạt chế độ gõ tay.
        loginPage.thucHienDangNhap("040092000547", "Admin@123", "");

        // Dừng thêm 5 giây để bạn nhìn thấy thông báo đăng nhập thành công
        // hoặc giao diện Dashboard hiện ra trước khi Bot tự tắt trình duyệt.
        webUI.sleep(5);

        System.out.println("=== KẾT THÚC KỊCH BẢN ===");
    }
}
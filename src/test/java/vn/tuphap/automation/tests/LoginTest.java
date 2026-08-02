package vn.tuphap.automation.tests;

import vn.tuphap.automation.core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import vn.tuphap.automation.pages.DashboardPage;
import vn.tuphap.automation.pages.LoginPage;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.report.BaoCao;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.List;

/** Kiểm tra đăng nhập độc lập — chạy suite testng-login.xml (không nằm trong smoke). */
public class LoginTest extends BaseTest {

    @Test(groups = {"login"},
            description = "Đăng nhập thành công và thấy Dashboard")
    public void testDangNhapThanhCong() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.loginUntilDashboard(
                ConfigReader.getValue("username"),
                ConfigReader.getValue("password"),
                3,
                WaitConfig.DASHBOARD_LOGIN);

        DashboardPage dashboardPage = new DashboardPage(getDriver());
        Assert.assertTrue(dashboardPage.isDashboardVisible(),
                "Sau đăng nhập phải thấy Dashboard [Nộp đơn mới]");
        new WebUI(getDriver()).captureOverview("Ảnh tổng quan — Dashboard sau đăng nhập thành công");
    }

    /**
     * 3 ca âm đăng nhập — trước đây MÀN ĐĂNG NHẬP CHƯA CÓ CA ÂM NÀO. Mỗi ca đều assert 2 việc: (1)
     * không được vào Dashboard (an ninh — điều kiện bắt buộc), (2) hệ thống phải hiện thông báo lỗi
     * rõ ràng (trải nghiệm — nếu hệ thống chỉ lặng lẽ không chuyển trang mà không nói lý do, đó cũng
     * là một lỗi UX đáng ghi nhận, không chỉ riêng lỗ hổng bảo mật).
     */
    @Test(groups = {"login"},
            description = "Ca âm — sai mật khẩu: không vào được Dashboard, hệ thống phải báo lỗi")
    public void testDangNhapSaiMatKhau() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.openPage();
        loginPage.chonDangNhapBangTaiKhoan();
        loginPage.thucHienDangNhap(
                ConfigReader.getValue("username"),
                "SaiMatKhau_" + (System.nanoTime() % 100000),
                "");
        loginPage.choPhanHoiSauDangNhap(WaitConfig.DASHBOARD_LOGIN);
        ketLuanCaAmDangNhap(loginPage, "sai mật khẩu");
    }

    @Test(groups = {"login"},
            description = "Ca âm — sai captcha: không vào được Dashboard, hệ thống phải báo lỗi")
    public void testDangNhapSaiCaptcha() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.openPage();
        loginPage.chonDangNhapBangTaiKhoan();
        String captchaSai = loginPage.layCaptchaSai();
        loginPage.thucHienDangNhap(
                ConfigReader.getValue("username"),
                ConfigReader.getValue("password"),
                captchaSai);
        loginPage.choPhanHoiSauDangNhap(WaitConfig.DASHBOARD_LOGIN);
        ketLuanCaAmDangNhap(loginPage, "sai captcha (\"" + captchaSai + "\")");
    }

    @Test(groups = {"login"},
            description = "Ca âm — bỏ trống mật khẩu: không vào được Dashboard, hệ thống phải báo lỗi")
    public void testDangNhapBoTrongMatKhau() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.openPage();
        loginPage.chonDangNhapBangTaiKhoan();
        loginPage.thucHienDangNhap(ConfigReader.getValue("username"), "", "");
        loginPage.choPhanHoiSauDangNhap(WaitConfig.DASHBOARD_LOGIN);
        ketLuanCaAmDangNhap(loginPage, "bỏ trống mật khẩu");
    }

    /** Chung cho cả 3 ca âm: ghi nhận kết quả thực tế vào báo cáo rồi assert không đăng nhập được. */
    private void ketLuanCaAmDangNhap(LoginPage loginPage, String moTaCaAm) {
        // Đè lại "kết quả mong đợi" mà TestListener.isLoginTest đã đặt sẵn lúc onTestStart (nói
        // chung chung "đăng nhập thành công và thấy bảng điều khiển") — ca âm thì mong đợi NGƯỢC LẠI.
        BaoCao.ketQuaMongDoi("Đăng nhập " + moTaCaAm
                + " phải bị từ chối — không vào được Dashboard, có thông báo lỗi rõ ràng trên giao diện.");
        DashboardPage dashboardPage = new DashboardPage(getDriver());
        boolean vaoDuocDashboard = dashboardPage.isDashboardVisible();
        List<String> thongBao = loginPage.layThongBaoLoiDangNhap();
        String ket = "Ca âm " + moTaCaAm + " — Vào Dashboard: " + (vaoDuocDashboard ? "Có" : "Không")
                + " | Thông báo hệ thống: "
                + (thongBao.isEmpty() ? "(không đọc được)" : String.join(" | ", thongBao));
        BaoCao.ketQuaThucTe(ket);
        new WebUI(getDriver()).captureOverview("Ảnh tổng quan — ca âm đăng nhập: " + moTaCaAm);
        Assert.assertFalse(vaoDuocDashboard,
                "❌ Đăng nhập " + moTaCaAm + " nhưng vẫn vào được Dashboard — lỗ hổng bảo mật nghiêm trọng.");
        Assert.assertFalse(thongBao.isEmpty(),
                "⚠ Đăng nhập " + moTaCaAm + " nhưng không thấy thông báo lỗi nào trên giao diện"
                        + " — trải nghiệm không rõ ràng cho người dùng.");
    }
}

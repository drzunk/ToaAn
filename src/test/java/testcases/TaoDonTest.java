package testcases;

import core.BaseTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pages.DashboardPage;
import pages.LoginPage;
import pages.NguyenDonPage;
import pages.TaoDonPage;
import utils.ExcelHelper;
import utils.ExtentReportManager;
public class TaoDonTest extends BaseTest {

    @DataProvider(name = "DuLieuTaoDon")
    public Object[][] getData() {
        String filePath = "src/test/resources/TestData/DuLieuTaoDon.xlsx";
        Object[][] rawData = ExcelHelper.getExcelData(filePath, "Sheet1");

        // LỌC DỮ LIỆU: Loại bỏ hoàn toàn các "dòng ma" trước khi đưa vào chạy Test
        java.util.List<Object[]> validData = new java.util.ArrayList<>();
        for (Object[] row : rawData) {
            // Chỉ lấy những dòng mà Cột 1 (Loại đơn) có chứa chữ
            if (row[0] != null && !row[0].toString().trim().isEmpty()) {
                validData.add(row);
            }
        }

        // Trả về danh sách dữ liệu "sạch" 100%
        return validData.toArray(new Object[0][]);
    }

    // KỊCH BẢN GỘP: CHUẨN ĐỘC LẬP (Mỗi dòng Excel là 1 phiên chạy đầy đủ)
    // LƯU Ý: Phải thêm các biến tương ứng với số cột mới trong Excel vào trong hàm này
    // Đã thêm biến thứ 16: coNguoiDaiDien
    // Cập nhật tham số: Thêm tenNguoiDaiDien và quanHeDaiDien vào cuối cùng
    @Test(dataProvider = "DuLieuTaoDon")
    public void testFlowTaoDon(String stt,
                               String loaiDon, String loaiViec, String toaAn, String tomTat,
                               String loaiChuThe, String hoTen, String ngaySinh, String gioiTinh,
                               String cccd, String ngayCap, String noiCap,
                               String thuongTru, String lienLac, String sdt, String email,
                               String coNguoiDaiDien, String tenNguoiDaiDien, String quanHeDaiDien) {

        // --- MỐC 1: ĐĂNG NHẬP ---
        ExtentReportManager.logStep("=== THỰC HIỆN ĐĂNG NHẬP ===");
        LoginPage loginPage = new LoginPage(driver);
        loginPage.openPage();
        webUI.zoomPage("80%");
        loginPage.chonDangNhapBangTaiKhoan();

        // Gọi hàm đăng nhập (Bên trong LoginPage đã có lệnh chụp ảnh lúc điền đủ thông tin)
        loginPage.thucHienDangNhap("040092000547", "Admin@123", "");

        // --- MỐC 2: TẠO ĐƠN (BƯỚC 1) ---
        ExtentReportManager.logStep("=== THỰC HIỆN TẠO ĐƠN ===");
        DashboardPage dashboardPage = new DashboardPage(driver);
        dashboardPage.clickNopDonMoi();

        TaoDonPage taoDonPage = new TaoDonPage(driver);
        taoDonPage.dienFormBuoc1(loaiDon, loaiViec, toaAn, tomTat);

        // Chụp ảnh kết quả Bước 1
        webUI.captureScreen("Đã điền đầy đủ dữ liệu Bước 1 - đơn " + loaiDon.toLowerCase() + " " + loaiDon);
        taoDonPage.clickTiepTheo();

        webUI.sleep(2);

        // --- MỐC 3: NGUYÊN ĐƠN (BƯỚC 2) ---
        ExtentReportManager.logStep("--- ĐIỀN THÔNG TIN NGUYÊN ĐƠN ---");
        NguyenDonPage nguyenDonPage = new NguyenDonPage(driver);

        nguyenDonPage.dienThongTinCaNhan(loaiChuThe, hoTen, ngaySinh, gioiTinh, cccd, ngayCap, noiCap);
        nguyenDonPage.dienThongTinLienHe(thuongTru, lienLac, sdt, email);
        nguyenDonPage.chonNguoiDaiDien(coNguoiDaiDien, tenNguoiDaiDien, quanHeDaiDien);

        // Chụp ảnh kết quả Nguyên Đơn
        webUI.captureScreen("Đã điền đầy đủ dữ liệu Nguyên Đơn");
        nguyenDonPage.clickTiepTheo();

        webUI.sleep(2);
    }
}
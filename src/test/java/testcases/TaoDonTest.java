package testcases;

import core.BaseTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.BiDonPage;
import pages.DashboardPage;
import pages.LoginPage;
import pages.NguyenDonPage;
import pages.TaoDonPage;
import utils.ConfigReader;
import utils.ExcelHelper;
import utils.ExtentReportManager;

public class TaoDonTest extends BaseTest {

    @DataProvider(name = "DuLieuTaoDon")
    public Object[][] getData() {
        String filePath = "src/test/resources/TestData/DuLieuTaoDon.xlsx";
        Object[][] rawData = ExcelHelper.getExcelData(filePath, "Sheet1");

        java.util.List<Object[]> validData = new java.util.ArrayList<>();
        for (Object[] row : rawData) {
            if (row[0] != null && !row[0].toString().trim().isEmpty()) {
                validData.add(row);
            }
        }
        return validData.toArray(new Object[0][]);
    }

    // LƯU Ý: SỐ LƯỢNG BIẾN ĐÃ TĂNG LÊN ĐỂ CHỨA DATA CỦA BỊ ĐƠN
    // TỔNG CỘNG CÓ 41 CỘT TRONG EXCEL CỦA BẠN BÂY GIỜ!
    @Test(dataProvider = "DuLieuTaoDon")
    public void testFlowTaoDon(String stt,
                               String loaiDon, String loaiViec, String toaAn, String tomTat,

                               // =================== NGUYÊN ĐƠN ===================
                               String loaiChuThe,
                               // Nguyên đơn - Cá nhân
                               String hoTen, String ngaySinh, String gioiTinh, String cccd, String ngayCap, String noiCap, String thuongTru, String lienLac,
                               // Nguyên đơn - Tổ chức
                               String tenToChuc, String loaiHinhToChuc, String mst, String diaChiToChuc, String nguoiDaiDienToChuc, String chucVuToChuc,
                               // Nguyên đơn - Dùng chung
                               String sdt, String email, String coNguoiDaiDien, String tenNguoiDaiDien, String quanHeDaiDien,

                               // =================== BỊ ĐƠN ===================
                               String loaiBiDon,
                               // Bị đơn - Cá nhân
                               String hoTenBD, String cccdBD, String namSinhBD, String diaChiCaNhanBD,
                               // Bị đơn - Tổ chức
                               String tenToChucBD, String loaiHinhBD, String mstBD, String diaChiTruSoBD, String nguoiDaiDienBD,
                               // Bị đơn - Dùng chung
                               String sdtBD, String emailBD,

                               // =================== NGƯỜI LIÊN QUAN ===================
                               String coNguoiLienQuan, String hoTenNLQ, String lyDoNLQ, String thongTinLienLacNLQ) {

        // --- MỐC 1: ĐĂNG NHẬP ---
        ExtentReportManager.logStep("=== THỰC HIỆN ĐĂNG NHẬP ===");
        LoginPage loginPage = new LoginPage(driver);
        loginPage.openPage();
        webUI.zoomPage("80%");
        loginPage.chonDangNhapBangTaiKhoan();

        String user = ConfigReader.getValue("username");
        String pass = ConfigReader.getValue("password");
        loginPage.thucHienDangNhap(user, pass, "");

        // --- MỐC 2: TẠO ĐƠN (BƯỚC 1) ---
        ExtentReportManager.logStep("=== THỰC HIỆN TẠO ĐƠN ===");
        DashboardPage dashboardPage = new DashboardPage(driver);
        dashboardPage.clickNopDonMoi();

        TaoDonPage taoDonPage = new TaoDonPage(driver);
        taoDonPage.dienFormBuoc1(loaiDon, loaiViec, toaAn, tomTat);

        webUI.captureScreen("Đã điền đầy đủ dữ liệu Bước 1 - đơn " + loaiDon.toLowerCase());
        taoDonPage.clickTiepTheo();

        webUI.sleep(2);

        // --- MỐC 3: NGUYÊN ĐƠN (BƯỚC 2) ---
        ExtentReportManager.logStep("--- ĐIỀN THÔNG TIN NGUYÊN ĐƠN ---");
        NguyenDonPage nguyenDonPage = new NguyenDonPage(driver);

        nguyenDonPage.chonLoaiChuThe(loaiChuThe);
        webUI.sleep(1);

        boolean isToChuc = loaiChuThe != null &&
                (loaiChuThe.toLowerCase().contains("tổ chức") ||
                        loaiChuThe.toLowerCase().contains("doanh nghiệp"));

        if (isToChuc) {
            nguyenDonPage.dienThongTinToChuc(tenToChuc, loaiHinhToChuc, mst, diaChiToChuc, nguoiDaiDienToChuc, chucVuToChuc, sdt, email);
        } else {
            nguyenDonPage.dienThongTinCaNhan(hoTen, ngaySinh, gioiTinh, cccd, ngayCap, noiCap);
            nguyenDonPage.dienThongTinLienHe(thuongTru, lienLac, sdt, email);
        }

        nguyenDonPage.chonNguoiDaiDien(coNguoiDaiDien, tenNguoiDaiDien, quanHeDaiDien);

        webUI.captureScreen("Đã điền đầy đủ dữ liệu Nguyên Đơn");
        nguyenDonPage.clickTiepTheo();

        webUI.sleep(2);

        // ==============================================================
        // --- MỐC 4: BỊ ĐƠN (BƯỚC 3) ---
        // ==============================================================
        ExtentReportManager.logStep("--- ĐIỀN THÔNG TIN BỊ ĐƠN ---");
        BiDonPage biDonPage = new BiDonPage(driver);

        // Mặc định điền cho Bị đơn số 1
        int indexBiDon = 1;

        biDonPage.chonLoaiBiDon(indexBiDon, loaiBiDon);
        webUI.sleep(1);

        boolean isBiDonToChuc = loaiBiDon != null &&
                (loaiBiDon.toLowerCase().contains("tổ chức") ||
                        loaiBiDon.toLowerCase().contains("doanh nghiệp"));

        if (isBiDonToChuc) {
            biDonPage.dienThongTinToChuc(indexBiDon, tenToChucBD, loaiHinhBD, mstBD, diaChiTruSoBD, nguoiDaiDienBD, sdtBD);
        } else {
            biDonPage.dienThongTinCaNhan(indexBiDon, hoTenBD, cccdBD, namSinhBD, diaChiCaNhanBD, sdtBD, emailBD);
        }

        // Người có quyền lợi, nghĩa vụ liên quan
        biDonPage.dienNguoiLienQuan(coNguoiLienQuan, hoTenNLQ, lyDoNLQ, thongTinLienLacNLQ);

        webUI.captureScreen("Đã điền đầy đủ dữ liệu Bị Đơn");
        biDonPage.clickTiepTheo();
        webUI.sleep(2);
    }
}

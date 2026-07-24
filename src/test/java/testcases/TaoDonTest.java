package testcases;

import core.TaoDonBaseTest;
import org.openqa.selenium.By;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.BiDonPage;
import pages.DashboardPage;
import pages.NguyenDonPage;
import pages.NoiDungDonPage;
import pages.TaiLieuPage;
import pages.TaoDonPage;
import pages.XemLaiGuiDonPage;
import utils.DataDictionary;
import utils.ExtentReportManager;

public class TaoDonTest extends TaoDonBaseTest {

    private static final int TOTAL_STEPS = 6;

    @DataProvider(name = "DuLieuTaoDon")
    public Object[][] getData() {
        return utils.DataGenerator.generateFullCoverageData();
    }

    // 50 cột: bước 1–3 (44) + nội dung đơn (5) + tài liệu (1)
    @Test(dataProvider = "DuLieuTaoDon")
    public void testFlowTaoDon(String stt,
                               String loaiDon, String loaiViec, String toaAn, String tomTat,

                               // =================== NGUYÊN ĐƠN ===================
                               String loaiChuThe,
                               String hoTen, String ngaySinh, String gioiTinh, String cccd, String ngayCap, String noiCap, String thuongTru, String lienLac,
                               String tenToChuc, String loaiHinhToChuc, String mst, String diaChiToChuc, String nguoiDaiDienToChuc, String chucVuToChuc,
                               String sdt, String email, String coNguoiDaiDien, String tenNguoiDaiDien, String quanHeDaiDien,

                               // =================== BỊ ĐƠN ===================
                               String loaiBiDon,
                               String hoTenBD, String cccdBD, String namSinhBD, String diaChiCaNhanBD,
                               String tenToChucBD, String loaiHinhBD, String mstBD, String diaChiTruSoBD, String nguoiDaiDienBD,
                               String sdtBD, String emailBD,

                               // =================== NGƯỜI LIÊN QUAN ===================
                               String coNguoiLienQuan, String hoTenNLQ, String lyDoNLQ, String thongTinLienLacNLQ,

                               // =================== HÀNH CHÍNH ===================
                               String tenCoQuanHC, String chucDanhHC, String nguoiThamQuyenHC,

                               // =================== NỘI DUNG ĐƠN (BƯỚC 4) ===================
                               String thoiDiemPhatSinh, String giaTriTranhChap,
                               String tomTatQuaTrinh, String yeuCauCuThe, String canCuPhapLy,

                               // =================== TÀI LIỆU (BƯỚC 5) ===================
                               String coTaiLieuBoSung) {

        ExtentReportManager.logSection("⚡ Thực thi kịch bản STT " + stt);
        DashboardPage dashboardPage = new DashboardPage(driver);
        dashboardPage.clickNopDonMoi();

        long stepStart = ExtentReportManager.markStepStart();
        ExtentReportManager.logSection("🔹 Bước 1/" + TOTAL_STEPS + " — Loại đơn & Tòa án");
        TaoDonPage taoDonPage = new TaoDonPage(driver);
        taoDonPage.dienFormBuoc1(loaiDon, loaiViec, toaAn, tomTat);
        webUI.captureScreen("Bước 1 — Đã chọn loại đơn");
        taoDonPage.clickTiepTheo();
        webUI.waitUntilVisible(
                By.xpath("//label[contains(., 'Họ và tên') or contains(., 'Tên tổ chức')]"),
                15,
                "Bước 2 [Nguyên đơn]");
        ExtentReportManager.logStepDone(1, TOTAL_STEPS, "Loại đơn & Tòa án", stepStart, new String[][]{
                {"Loại đơn", loaiDon},
                {"Loại việc", loaiViec},
                {"Tòa án", toaAn}
        });

        stepStart = ExtentReportManager.markStepStart();
        ExtentReportManager.logSection("🔹 Bước 2/" + TOTAL_STEPS + " — Nguyên đơn");
        NguyenDonPage nguyenDonPage = new NguyenDonPage(driver);
        nguyenDonPage.chonLoaiChuThe(loaiChuThe);
        webUI.sleep(1);

        if (DataDictionary.isToChuc(loaiChuThe)) {
            nguyenDonPage.dienThongTinToChuc(tenToChuc, loaiHinhToChuc, mst, diaChiToChuc, nguoiDaiDienToChuc, chucVuToChuc, sdt, email);
        } else {
            nguyenDonPage.dienThongTinCaNhan(hoTen, ngaySinh, gioiTinh, cccd, ngayCap, noiCap);
            nguyenDonPage.dienThongTinLienHe(thuongTru, lienLac, sdt, email);
        }

        nguyenDonPage.chonNguoiDaiDien(coNguoiDaiDien, tenNguoiDaiDien, quanHeDaiDien);
        webUI.captureScreen("Bước 2 — Đã điền nguyên đơn");
        nguyenDonPage.clickTiepTheo();
        webUI.sleep(3);
        ExtentReportManager.logStepDone(2, TOTAL_STEPS, "Nguyên đơn", stepStart,
                DataDictionary.isToChuc(loaiChuThe)
                        ? new String[][]{{"Chủ thể", "Tổ chức"}, {"Tên", tenToChuc}, {"MST", mst}}
                        : new String[][]{{"Chủ thể", "Cá nhân"}, {"Họ tên", hoTen}, {"CCCD", cccd}});

        stepStart = ExtentReportManager.markStepStart();
        ExtentReportManager.logSection("🔹 Bước 3/" + TOTAL_STEPS + " — Bị đơn / Bên bị kiện");
        BiDonPage biDonPage = new BiDonPage(driver);
        int indexBiDon = 1;
        String buoc3Mode;

        if (DataDictionary.isHanhChinh(loaiDon)) {
            buoc3Mode = "Hành chính — Cơ quan";
            biDonPage.dienThongTinNguoiBiKienHanhChinh(indexBiDon, tenCoQuanHC, diaChiTruSoBD, chucDanhHC, nguoiThamQuyenHC, sdtBD);
        } else if (DataDictionary.isPhaSan(loaiDon)) {
            buoc3Mode = "Phá sản — Tổ chức";
            biDonPage.dienThongTinToChuc(indexBiDon, loaiDon, tenToChucBD, loaiHinhBD, mstBD, diaChiTruSoBD, nguoiDaiDienBD, sdtBD);
        } else {
            biDonPage.chonLoaiBiDon(indexBiDon, loaiBiDon, loaiDon);
            webUI.sleep(1);
            if (DataDictionary.isToChuc(loaiBiDon)) {
                buoc3Mode = "Bị đơn tổ chức";
                biDonPage.dienThongTinToChuc(indexBiDon, loaiDon, tenToChucBD, loaiHinhBD, mstBD, diaChiTruSoBD, nguoiDaiDienBD, sdtBD);
            } else {
                buoc3Mode = "Bị đơn cá nhân";
                biDonPage.dienThongTinCaNhan(indexBiDon, loaiDon, hoTenBD, cccdBD, namSinhBD, diaChiCaNhanBD, sdtBD, emailBD);
            }
        }

        biDonPage.dienNguoiLienQuan(loaiDon, coNguoiLienQuan, hoTenNLQ, lyDoNLQ, thongTinLienLacNLQ);
        webUI.captureScreen("Bước 3 — Đã điền bị đơn");
        biDonPage.clickTiepTheo();
        ExtentReportManager.logStepDone(3, TOTAL_STEPS, "Bị đơn", stepStart, new String[][]{
                {"Chế độ form", buoc3Mode},
                {"Người liên quan", coNguoiLienQuan}
        });

        stepStart = ExtentReportManager.markStepStart();
        ExtentReportManager.logSection("🔹 Bước 4/" + TOTAL_STEPS + " — Nội dung đơn");
        NoiDungDonPage noiDungDonPage = new NoiDungDonPage(driver);
        noiDungDonPage.dienForm(loaiDon, thoiDiemPhatSinh, giaTriTranhChap,
                tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy);
        webUI.captureScreen("Bước 4 — Đã điền nội dung");
        noiDungDonPage.clickTiepTheo();
        webUI.sleep(2);
        if (DataDictionary.hasGiaTriTranhChap(loaiDon)) {
            ExtentReportManager.logStepDone(4, TOTAL_STEPS, "Nội dung đơn", stepStart, new String[][]{
                    {"Thời điểm", thoiDiemPhatSinh},
                    {"Giá trị tranh chấp", giaTriTranhChap + " VNĐ"},
                    {"Độ dài tóm tắt", tomTatQuaTrinh.length() + " ký tự"}
            });
        } else {
            ExtentReportManager.logStepDone(4, TOTAL_STEPS, "Nội dung đơn", stepStart, new String[][]{
                    {"Thời điểm", thoiDiemPhatSinh},
                    {"Độ dài tóm tắt", tomTatQuaTrinh.length() + " ký tự"}
            });
        }

        stepStart = ExtentReportManager.markStepStart();
        ExtentReportManager.logSection("🔹 Bước 5/" + TOTAL_STEPS + " — Tài liệu & chứng cứ");
        TaiLieuPage taiLieuPage = new TaiLieuPage(driver);
        taiLieuPage.uploadTaiLieuBatBuoc();
        taiLieuPage.uploadTaiLieuBoSung(coTaiLieuBoSung);
        webUI.captureScreen("Bước 5 — Đã tải tài liệu");
        taiLieuPage.clickTiepTheo();
        webUI.sleep(2);
        ExtentReportManager.logStepDone(5, TOTAL_STEPS, "Tài liệu", stepStart, new String[][]{
                {"Tài liệu bắt buộc", "sample.pdf"},
                {"Tài liệu bổ sung", coTaiLieuBoSung}
        });

        stepStart = ExtentReportManager.markStepStart();
        ExtentReportManager.logSection("🔹 Bước 6/" + TOTAL_STEPS + " — Xem lại & Gửi đơn");
        XemLaiGuiDonPage xemLaiGuiDonPage = new XemLaiGuiDonPage(driver);
        xemLaiGuiDonPage.xemLaiVaGuiDon();
        webUI.captureScreen("Bước 6 — Gửi đơn thành công");
        ExtentReportManager.logStepDone(6, TOTAL_STEPS, "Gửi đơn", stepStart, new String[][]{
                {"Hành động", "Xác nhận + Gửi đơn"},
                {"Kết quả", "Chờ thông báo thành công"}
        });

        System.out.println("🎉 ✅ Kịch bản chạy thành công rực rỡ!");
    }
}

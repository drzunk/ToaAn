package vn.tuphap.automation.flow;

import vn.tuphap.automation.report.TaoDonReportBuilder;

import vn.tuphap.automation.report.TestActionLog;

import vn.tuphap.automation.report.ExtentReportManager;

import vn.tuphap.automation.data.TaoDonScenario;

import vn.tuphap.automation.data.DataDictionary;

import vn.tuphap.automation.ui.WaitConfig;

import vn.tuphap.automation.ui.WebUI;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import vn.tuphap.automation.pages.BiDonPage;
import vn.tuphap.automation.pages.DashboardPage;
import vn.tuphap.automation.pages.NguyenDonPage;
import vn.tuphap.automation.pages.NoiDungDonPage;
import vn.tuphap.automation.pages.TaiLieuPage;
import vn.tuphap.automation.pages.TaoDonPage;
import vn.tuphap.automation.pages.XemLaiGuiDonPage;

/**
 * Các bước điền biểu mẫu dùng chung cho TaoDonTest.
 * Extent: không ảnh từng bước 1→5; chỉ ảnh tổng quan ở mốc Xem lại / kết thúc.
 * Chi tiết dữ liệu: báo cáo Excel.
 */
public final class TaoDonFlow {

    public static final By MARKER_NGUYEN_DON =
            By.xpath("//label[contains(., 'Họ và tên') or contains(., 'Tên tổ chức')]");
    public static final By MARKER_BI_DON = By.xpath(
            "//button[contains(., 'Thêm bị đơn') or contains(., 'Thêm người bị yêu cầu')"
                    + " or contains(., 'Thêm người bị kiện') or contains(., 'Thêm người được yêu cầu')]"
                    + " | //span[contains(., 'Người yêu cầu 2')]"
                    + " | //h2[contains(., 'Doanh nghiệp') and contains(., 'phá sản')]"
                    + " | //label[contains(., 'Tên cơ quan')]"
                    + " | //label[contains(., 'Năm sinh') or contains(., 'Tên tổ chức')]");
    public static final By MARKER_NOI_DUNG =
            By.xpath("//h2[contains(., 'Nội dung đơn')]/parent::div//label[contains(., 'Thời điểm phát sinh')]");
    public static final By MARKER_TAI_LIEU =
            By.xpath("//h2[contains(., 'Tài liệu') and contains(., 'chứng')]/parent::div//div[contains(., 'Tài liệu bắt buộc')]");
    public static final By MARKER_XEM_LAI =
            By.xpath("//h2[contains(., 'Xem lại') and contains(., 'Gửi đơn')]/parent::div"
                    + "//span[contains(., 'Loại đơn') or contains(., 'Nguyên đơn')]");

    private final WebDriver driver;
    private final WebUI webUI;

    public TaoDonFlow(WebDriver driver, WebUI webUI) {
        this.driver = driver;
        this.webUI = webUI;
    }

    public void moFormNopDonMoi() {
        TestActionLog.buoc(0, "Mở biểu mẫu Nộp đơn mới", "Bảng điều khiển");
        DashboardPage dashboard = new DashboardPage(driver);
        Assert.assertTrue(dashboard.isDashboardVisible(), "Bảng điều khiển phải sẵn sàng trước khi nộp đơn");
        dashboard.clickNopDonMoi();
        TestActionLog.trangThaiBuoc("Đạt");
    }

    public void dienBuoc1(TaoDonScenario s) {
        long t = ExtentReportManager.markStepStart();
        TestActionLog.buoc(1, TaoDonReportBuilder.tenBuocDayDu(1), "Trang tạo đơn");
        TaoDonPage page = new TaoDonPage(driver);
        page.dienFormBuoc1(s.loaiDon(), s.loaiViec(), s.toaAn(), s.tomTat());
        page.clickTiepTheo();
        webUI.waitUntilVisible(MARKER_NGUYEN_DON, WaitConfig.STEP, "Đã chuyển sang bước Nguyên đơn");
        ExtentReportManager.logStepDone(1, 6, TaoDonReportBuilder.tenBuocDayDu(1), t);
        TestActionLog.trangThaiBuoc("Đạt");
    }

    public void dienBuoc2(TaoDonScenario s) {
        long t = ExtentReportManager.markStepStart();
        TestActionLog.buoc(2, TaoDonReportBuilder.tenBuocDayDu(2), "Trang nguyên đơn");
        NguyenDonPage page = new NguyenDonPage(driver);
        page.chonLoaiChuThe(s.loaiChuThe());
        if (DataDictionary.isToChuc(s.loaiChuThe())) {
            page.dienThongTinToChuc(
                    s.tenToChuc(), s.loaiHinhToChuc(), s.mst(), s.diaChiToChuc(),
                    s.nguoiDaiDienToChuc(), s.chucVuToChuc(), s.sdt(), s.email());
        } else {
            page.dienThongTinCaNhan(s.hoTen(), s.ngaySinh(), s.gioiTinh(), s.cccd(), s.ngayCap(), s.noiCap());
            page.dienThongTinLienHe(s.thuongTru(), s.lienLac(), s.sdt(), s.email());
        }
        page.chonNguoiDaiDien(s.coNguoiDaiDien(), s.tenNguoiDaiDien(), s.quanHeDaiDien());
        if (DataDictionary.isPhaSan(s.loaiDon())) {
            page.chonTuCachNguoiNopDon(s.tuCachNopDon());
        }
        page.clickTiepTheo();
        webUI.waitUntilVisible(MARKER_BI_DON, WaitConfig.STEP, "Đã chuyển sang bước Bị đơn / bên bị kiện");
        ExtentReportManager.logStepDone(2, 6, TaoDonReportBuilder.tenBuocDayDu(2), t);
        TestActionLog.trangThaiBuoc("Đạt");
    }

    public void dienBuoc3(TaoDonScenario s) {
        long t = ExtentReportManager.markStepStart();
        TestActionLog.buoc(3, TaoDonReportBuilder.tenBuocDayDu(3), "Trang bị đơn");
        BiDonPage page = new BiDonPage(driver);
        page.dienBuoc3(s);
        page.clickTiepTheo();
        webUI.waitUntilVisible(MARKER_NOI_DUNG, WaitConfig.STEP, "Đã chuyển sang bước Nội dung đơn");
        String moTaBuoc3 = TaoDonReportBuilder.tenBuocDayDu(3)
                + (s.soLuongBiDon() > 1
                ? " (đã điền " + s.soLuongBiDon() + " bị đơn)"
                : " (1 bị đơn)");
        ExtentReportManager.logStepDone(3, 6, moTaBuoc3, t);
        TestActionLog.trangThaiBuoc("Đạt");
    }

    public void dienBuoc4(TaoDonScenario s) {
        long t = ExtentReportManager.markStepStart();
        TestActionLog.buoc(4, TaoDonReportBuilder.tenBuocDayDu(4), "Trang nội dung đơn");
        NoiDungDonPage page = new NoiDungDonPage(driver);
        page.dienForm(s.loaiDon(), s.thoiDiemPhatSinh(), s.giaTriTranhChap(),
                s.tomTatQuaTrinh(), s.yeuCauCuThe(), s.canCuPhapLy());
        page.clickTiepTheo();
        webUI.waitUntilVisible(MARKER_TAI_LIEU, WaitConfig.STEP, "Đã chuyển sang bước Tài liệu và chứng cứ");
        ExtentReportManager.logStepDone(4, 6, TaoDonReportBuilder.tenBuocDayDu(4), t);
        TestActionLog.trangThaiBuoc("Đạt");
    }

    public void dienBuoc4CapNhatYeuCau(TaoDonScenario s, String yeuCauMoi) {
        long t = ExtentReportManager.markStepStart();
        TestActionLog.buoc(4, "Cập nhật lại nội dung đơn (yêu cầu cụ thể đã chỉnh sửa)", "Trang nội dung đơn");
        NoiDungDonPage page = new NoiDungDonPage(driver);
        page.dienForm(s.loaiDon(), s.thoiDiemPhatSinh(), s.giaTriTranhChap(),
                s.tomTatQuaTrinh(), yeuCauMoi, s.canCuPhapLy());
        page.clickTiepTheo();
        webUI.waitUntilVisible(MARKER_TAI_LIEU, WaitConfig.STEP, "Đã sửa nội dung — chuyển sang Tài liệu");
        ExtentReportManager.logStepDone(4, 6,
                "Cập nhật lại nội dung đơn (yêu cầu cụ thể đã chỉnh sửa)", t);
        TestActionLog.trangThaiBuoc("Đạt");
    }

    public void dienBuoc5(TaoDonScenario s) {
        long t = ExtentReportManager.markStepStart();
        TestActionLog.buoc(5, TaoDonReportBuilder.tenBuocDayDu(5), "Trang tài liệu");
        TaiLieuPage page = new TaiLieuPage(driver);
        page.uploadTaiLieuBatBuoc();
        page.uploadTaiLieuBoSung(s.coTaiLieuBoSung());
        page.clickTiepTheo();
        webUI.waitUntilVisible(MARKER_XEM_LAI, WaitConfig.STEP, "Đã chuyển sang màn Xem lại và Gửi đơn");
        ExtentReportManager.logStepDone(5, 6, TaoDonReportBuilder.tenBuocDayDu(5), t);
        TestActionLog.trangThaiBuoc("Đạt");
    }

    /** Điền bước 1→5 rồi dừng ở màn Xem lại (+ ảnh tổng quan). */
    public XemLaiGuiDonPage denManXemLai(TaoDonScenario s) {
        moFormNopDonMoi();
        dienBuoc1(s);
        dienBuoc2(s);
        dienBuoc3(s);
        dienBuoc4(s);
        dienBuoc5(s);
        TestActionLog.buoc(6, TaoDonReportBuilder.tenBuocDayDu(6), "Trang xem lại");
        XemLaiGuiDonPage review = new XemLaiGuiDonPage(driver);
        review.waitStepReady();
        webUI.captureOverview("Ảnh tổng quan — màn Xem lại (đã điền đủ bước 1→5)");
        return review;
    }

    public void tuXemLaiQuaBuoc5DenXemLai(TaoDonScenario s) {
        dienBuoc5(s);
        TestActionLog.buoc(6, TaoDonReportBuilder.tenBuocDayDu(6) + " (sau chỉnh sửa)", "Trang xem lại");
    }
}

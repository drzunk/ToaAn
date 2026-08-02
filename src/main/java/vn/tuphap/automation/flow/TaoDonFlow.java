package vn.tuphap.automation.flow;

import vn.tuphap.automation.report.TaoDonReportBuilder;

import vn.tuphap.automation.report.TestActionLog;

import vn.tuphap.automation.report.BaoCao;

import vn.tuphap.automation.data.TaoDonScenario;

import vn.tuphap.automation.data.DataDictionary;

import vn.tuphap.automation.ui.DriverCallCounter;
import vn.tuphap.automation.ui.UiProfiler;
import vn.tuphap.automation.ui.WaitConfig;

import vn.tuphap.automation.ui.WebUI;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.pages.BiDonPage;
import vn.tuphap.automation.pages.DashboardPage;
import vn.tuphap.automation.pages.LoginPage;
import vn.tuphap.automation.pages.NguyenDonPage;
import vn.tuphap.automation.pages.NoiDungDonPage;
import vn.tuphap.automation.pages.TaiLieuPage;
import vn.tuphap.automation.pages.TaoDonPage;
import vn.tuphap.automation.pages.XemLaiGuiDonPage;

import java.util.List;

/**
 * Các bước điền biểu mẫu dùng chung cho TaoDonTest.
 * Khi hệ thống báo lỗi/validate: dừng ngay, ghi log message và chụp ảnh (báo cáo HTML + Excel).
 * Không chụp ảnh từng bước 1→5 khi thành công; chỉ ảnh lỗi kèm message khi bị chặn.
 * Chi tiết dữ liệu: báo cáo Excel.
 */
public final class TaoDonFlow {

    public static final By MARKER_NGUYEN_DON = NguyenDonPage.MARKER_NGUYEN_DON_CHINH;
    public static final By MARKER_BI_DON = By.xpath(
            "//h2[contains(., 'Bị đơn') or contains(., 'bị kiện') or contains(., 'bị yêu cầu')"
                    + " or contains(., 'Người bị') or contains(., 'Doanh nghiệp')]/parent::div"
                    + "//button[contains(., 'Thêm bị đơn') or contains(., 'Thêm người bị yêu cầu')"
                    + " or contains(., 'Thêm người bị kiện') or contains(., 'Thêm người được yêu cầu')]"
                    + " | //span[contains(., 'Người yêu cầu 2')]"
                    + " | //h2[contains(., 'Doanh nghiệp') and contains(., 'phá sản')]/parent::div//label"
                    + " | //h2[contains(., 'Bị đơn') or contains(., 'bị kiện') or contains(., 'bị yêu cầu')"
                    + " or contains(., 'Người bị')]/parent::div//label[contains(., 'Tên cơ quan')]"
                    + " | //h2[contains(., 'Bị đơn') or contains(., 'bị kiện') or contains(., 'bị yêu cầu')"
                    + " or contains(., 'Người bị')]/parent::div//label[contains(., 'Họ và tên')]"
                    + " | //h2[contains(., 'Bị đơn') or contains(., 'bị kiện') or contains(., 'bị yêu cầu')"
                    + " or contains(., 'Người bị')]/parent::div//label[contains(., 'Năm sinh')]"
                    + " | //span[starts-with(normalize-space(.), 'Bị đơn ') or starts-with(normalize-space(.), 'Người bị kiện ')]"
                    + " | //div[starts-with(normalize-space(.), 'Bị đơn ') or starts-with(normalize-space(.), 'Người bị kiện ')]");
    public static final By MARKER_NOI_DUNG = NoiDungDonPage.MARKER_STEP_READY;
    public static final By MARKER_TAI_LIEU = TaiLieuPage.MARKER_STEP_READY;
    public static final By MARKER_XEM_LAI = XemLaiGuiDonPage.MARKER_STEP_READY;

    private final WebDriver driver;
    private final WebUI webUI;

    public TaoDonFlow(WebDriver driver, WebUI webUI) {
        this.driver = driver;
        this.webUI = webUI;
    }

    /** In bảng phân tích thời gian theo loại thao tác của bước vừa xong (tắt: -Dtaodon.profile=false). */
    private static void printStepProfile(long tongMs) {
        printStepProfile(null, tongMs);
    }

    /**
     * In + <b>ghi vào báo cáo</b> bảng phân tích thời gian theo loại thao tác.
     * <p>
     * Trước đây chỉ {@code System.out.println}, mà {@code run-flow.ps1} ghi đè log mỗi lượt chạy
     * nên số liệu mất sạch sau lần chạy kế tiếp. Đẩy vào báo cáo để so sánh được trước/sau.
     */
    private static void printStepProfile(String nhan, long tongMs) {
        String chiTiet = UiProfiler.summary(tongMs);
        if (!chiTiet.isBlank()) {
            String dong = (nhan == null || nhan.isBlank() ? "" : nhan + " — ") + chiTiet;
            System.out.println("   ↳ " + dong);
            BaoCao.logInfo("↳ Phân tích thời gian: " + dong);
        }
        String soLuot = DriverCallCounter.summary(tongMs);
        if (!soLuot.isBlank()) {
            System.out.println("   ↳ " + soLuot);
            BaoCao.logInfo("↳ " + soLuot);
        }
    }

    public void moFormNopDonMoi() {
        long t = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(0, 6, "Mở biểu mẫu Nộp đơn mới");
        DashboardPage dashboard = new DashboardPage(driver);
        LoginPage loginPage = new LoginPage(driver);
        dashboard.ensureReady(loginPage, () -> reloginIfNeeded(loginPage), WaitConfig.DASHBOARD);
        dashboard.clickNopDonMoi();
        long elapsed = System.currentTimeMillis() - t;
        printStepProfile("Mở biểu mẫu", elapsed);
        System.out.println("⏱ Mở biểu mẫu Nộp đơn mới: "
                + TaoDonReportBuilder.formatDuration(elapsed));
        BaoCao.endStepNode("Đạt", elapsed);
    }

    private void reloginIfNeeded(LoginPage loginPage) {
        loginPage.loginUntilDashboard(
                ConfigReader.getValue("username"),
                ConfigReader.getValue("password"),
                2,
                WaitConfig.DASHBOARD_LOGIN);
    }

    public void dienBuoc1(TaoDonScenario s) {
        dienBuoc1(s, true);
    }

    /**
     * @param boQuaDonNhap {@code false} khi đã [Chỉnh sửa] từ Xem lại về bước 1 — không bấm Bắt đầu mới.
     */
    public void dienBuoc1(TaoDonScenario s, boolean boQuaDonNhap) {
        long t = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(1, 6, TaoDonReportBuilder.tenBuocDayDu(1));
        TaoDonPage page = new TaoDonPage(driver);
        page.dienFormBuoc1(s.loaiDon(), s.loaiViec(), s.toaAn(), s.tomTat(), boQuaDonNhap);
        page.clickTiepTheo();
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        // Giữ nguyên phép kiểm tra này: trần 15s chỉ bị chạm khi bước 1 thật sự không rời được,
        // còn happy-path thoát ngay khi marker biến mất (~1 nhịp poll). Bỏ đi tiết kiệm không đáng
        // kể mà mất một phép kiểm tra và thông báo lỗi riêng.
        webUI.waitUntilInvisible(TaoDonPage.MARKER_BUOC1, WaitConfig.STEP, "Đã rời bước 1");
        webUI.waitForStepTransition(1, TaoDonReportBuilder.tenBuocDayDu(1), MARKER_NGUYEN_DON,
                WaitConfig.STEP, "Đã chuyển sang bước Nguyên đơn");
        printStepProfile(System.currentTimeMillis() - t);
        System.out.println("⏱ Bước 1/6 (" + TaoDonReportBuilder.tenBuocDayDu(1) + "): "
                + TaoDonReportBuilder.formatDuration(System.currentTimeMillis() - t));
        BaoCao.logStepDone(1, 6, TaoDonReportBuilder.tenBuocDayDu(1), t);
    }

    public void dienBuoc2(TaoDonScenario s) {
        long t = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(2, 6, TaoDonReportBuilder.tenBuocDayDu(2));
        NguyenDonPage page = new NguyenDonPage(driver);
        page.chonLoaiChuThe(s.loaiChuThe());
        // Framework thực tế: loaiChuThe() ≡ "loại" Nguyên đơn (Cá nhân | Tổ chức).
        if (DataDictionary.isToChuc(s.loaiChuThe())) {
            TestActionLog.ghiChu("Đang xử lý luồng Nguyên đơn: Tổ chức");
            page.dienThongTinToChuc(
                    s.tenToChuc(), s.loaiHinhToChuc(), s.mst(), s.diaChiToChuc(),
                    s.nguoiDaiDienToChuc(), s.chucVuToChuc(), s.sdt(), s.email(),
                    s.ngaySinh(), s.gioiTinh(), s.cccd(), s.ngayCap(), s.noiCap());
        } else {
            TestActionLog.ghiChu("Đang xử lý luồng Nguyên đơn: Cá nhân");
            page.dienThongTinCaNhan(s.hoTen(), s.ngaySinh(), s.gioiTinh(), s.cccd(), s.ngayCap(), s.noiCap());
            page.dienThongTinLienHe(s.thuongTru(), s.lienLac(), s.sdt(), s.email());
        }
        page.chonNguoiDaiDien(s.coNguoiDaiDien(), s.tenNguoiDaiDien(), s.quanHeDaiDien());
        if (DataDictionary.isPhaSan(s.loaiDon())) {
            page.chonTuCachNguoiNopDon(s.tuCachNopDon());
        }
        page.xuLyDongNguyenDon(s.coDongNguyenDon(), s.dongNguyenDon());
        boolean coTickDinhDanh = page.chonDongYLuuThongTinDinhDanh();
        driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        if (coTickDinhDanh) {
            // Chỉ khi tick TTĐD (VNeID có thể đè form) — kiểm tra lại ô lệch/thiếu.
            if (DataDictionary.isToChuc(s.loaiChuThe())) {
                page.dienDayDuToChucTruocTiepTheo(
                        s.tenToChuc(), s.loaiHinhToChuc(), s.mst(), s.diaChiToChuc(),
                        s.nguoiDaiDienToChuc(), s.chucVuToChuc(), s.sdt(), s.email(),
                        s.ngaySinh(), s.gioiTinh(), s.cccd(), s.ngayCap(), s.noiCap());
            } else {
                page.dienDayDuCaNhanTruocTiepTheo(
                        s.hoTen(), s.ngaySinh(), s.gioiTinh(), s.cccd(), s.ngayCap(), s.noiCap(),
                        s.thuongTru(), s.lienLac(), s.sdt(), s.email());
            }
        } else {
            System.out.println(" ⏩ Bước 2 — không tick TTĐD → bỏ pass điền lại (tránh chọn/điền 2 lần trên UI).");
            if (!DataDictionary.isToChuc(s.loaiChuThe())) {
                page.chuanBiDiaChiTruocTiepTheo(s.thuongTru(), s.lienLac());
            }
        }
        webUI.logValidationMessages("Trước Tiếp theo — bước 2");
        page.clickTiepTheo();
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        webUI.waitForStepTransition(2, TaoDonReportBuilder.tenBuocDayDu(2), MARKER_BI_DON,
                WaitConfig.STEP, "Đã chuyển sang bước Bị đơn / bên bị kiện");
        printStepProfile(System.currentTimeMillis() - t);
        System.out.println("⏱ Bước 2/6 (" + TaoDonReportBuilder.tenBuocDayDu(2) + "): "
                + TaoDonReportBuilder.formatDuration(System.currentTimeMillis() - t));
        BaoCao.logStepDone(2, 6, TaoDonReportBuilder.tenBuocDayDu(2), t);
    }

    public void dienBuoc3(TaoDonScenario s) {
        long t = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(3, 6, TaoDonReportBuilder.tenBuocDayDu(3));
        BiDonPage page = new BiDonPage(driver);
        page.dienBuoc3(s);
        webUI.logValidationMessages("Trước Tiếp theo — bước 3");
        page.clickTiepTheo();
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        webUI.waitForStepTransition(3, TaoDonReportBuilder.tenBuocDayDu(3), MARKER_NOI_DUNG,
                WaitConfig.STEP, "Đã chuyển sang bước Nội dung đơn");
        String moTaBuoc3 = TaoDonReportBuilder.tenBuocDayDu(3)
                + (s.soLuongBiDon() > 1
                ? " (đã điền " + s.soLuongBiDon() + " bị đơn)"
                : " (1 bị đơn)");
        printStepProfile(System.currentTimeMillis() - t);
        System.out.println("⏱ Bước 3/6 (" + moTaBuoc3 + "): "
                + TaoDonReportBuilder.formatDuration(System.currentTimeMillis() - t));
        BaoCao.logStepDone(3, 6, moTaBuoc3, t);
    }

    public void dienBuoc4(TaoDonScenario s) {
        long t = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(4, 6, TaoDonReportBuilder.tenBuocDayDu(4));
        NoiDungDonPage page = new NoiDungDonPage(driver);
        page.dienForm(s.loaiDon(), s.thoiDiemPhatSinh(), s.giaTriTranhChap(),
                s.tomTatQuaTrinh(), s.yeuCauCuThe(), s.canCuPhapLy());
        if (page.isIframeMode()) {
            String gaps = page.describeIframeFillGaps();
            if (!gaps.isBlank()) {
                webUI.failStepWithSystemFeedback(4, TaoDonReportBuilder.tenBuocDayDu(4),
                        "Eform chưa điền đủ trước khi Tiếp theo", List.of(gaps));
            }
            System.out.println(" ✅ Bước 4 — đã điền đủ thông tin trong eform iframe.");
        }
        webUI.logValidationMessages("Trước Tiếp theo — bước 4");
        page.clickTiepTheo();
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        webUI.waitForStepTransition(4, TaoDonReportBuilder.tenBuocDayDu(4), MARKER_TAI_LIEU,
                WaitConfig.STEP, "Đã chuyển sang bước Tài liệu và chứng cứ");
        printStepProfile(System.currentTimeMillis() - t);
        System.out.println("⏱ Bước 4/6 (" + TaoDonReportBuilder.tenBuocDayDu(4) + "): "
                + TaoDonReportBuilder.formatDuration(System.currentTimeMillis() - t));
        BaoCao.logStepDone(4, 6, TaoDonReportBuilder.tenBuocDayDu(4), t);
    }

    public void dienBuoc4CapNhatYeuCau(TaoDonScenario s, String yeuCauMoi) {
        long t = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(4, 6, "Cập nhật lại nội dung đơn (yêu cầu cụ thể đã chỉnh sửa)");
        NoiDungDonPage page = new NoiDungDonPage(driver);
        page.waitStepReady();
        if (page.isUploadMode()) {
            page.uploadNoiDungFile();
            TestActionLog.ghiChu("Bước 4 dạng tải file — tải lại file thay vì sửa textarea yêu cầu cụ thể");
        } else if (page.isIframeMode()) {
            page.prepareIframeResubmitAfterEdit(yeuCauMoi);
        } else {
            page.dienForm(s.loaiDon(), s.thoiDiemPhatSinh(), s.giaTriTranhChap(),
                    s.tomTatQuaTrinh(), yeuCauMoi, s.canCuPhapLy());
        }
        page.clickTiepTheo();
        webUI.waitForStepTransition(4, TaoDonReportBuilder.tenBuocDayDu(4), MARKER_TAI_LIEU,
                WaitConfig.STEP, "Đã sửa nội dung — chuyển sang Tài liệu");
        printStepProfile(System.currentTimeMillis() - t);
        System.out.println("⏱ Bước 4/6 (Cập nhật lại nội dung đơn): "
                + TaoDonReportBuilder.formatDuration(System.currentTimeMillis() - t));
        BaoCao.logStepDone(4, 6,
                "Cập nhật lại nội dung đơn (yêu cầu cụ thể đã chỉnh sửa)", t);
    }

    public void dienBuoc5(TaoDonScenario s) {
        long t = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(5, 6, TaoDonReportBuilder.tenBuocDayDu(5));
        TaiLieuPage page = new TaiLieuPage(driver);
        page.uploadTaiLieuBatBuoc();
        page.uploadTaiLieuBoSung(s.coTaiLieuBoSung());
        webUI.logValidationMessages("Trước Tiếp theo — bước 5");
        page.clickTiepTheo();
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
        webUI.waitForStepTransition(5, TaoDonReportBuilder.tenBuocDayDu(5), MARKER_XEM_LAI,
                WaitConfig.STEP, "Đã chuyển sang màn Xem lại và Gửi đơn");
        printStepProfile(System.currentTimeMillis() - t);
        System.out.println("⏱ Bước 5/6 (" + TaoDonReportBuilder.tenBuocDayDu(5) + "): "
                + TaoDonReportBuilder.formatDuration(System.currentTimeMillis() - t));
        BaoCao.logStepDone(5, 6, TaoDonReportBuilder.tenBuocDayDu(5), t);
    }

    /** Điền bước 1→5 rồi dừng ở màn Xem lại (+ ảnh tổng quan). */
    public XemLaiGuiDonPage denManXemLai(TaoDonScenario s) {
        moFormNopDonMoi();
        dienBuoc1(s);
        dienBuoc2(s);
        dienBuoc3(s);
        dienBuoc4(s);
        dienBuoc5(s);
        long t6 = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(6, 6, TaoDonReportBuilder.tenBuocDayDu(6));
        XemLaiGuiDonPage review = new XemLaiGuiDonPage(driver);
        review.waitStepReady();
        webUI.captureOverview("Ảnh tổng quan — màn Xem lại (đã điền đủ bước 1→5)");
        long elapsed6 = System.currentTimeMillis() - t6;
        printStepProfile("Màn Xem lại", elapsed6);
        System.out.println("⏱ Bước 6/6 (" + TaoDonReportBuilder.tenBuocDayDu(6) + "): "
                + TaoDonReportBuilder.formatDuration(elapsed6));
        BaoCao.logStepDone(6, 6, TaoDonReportBuilder.tenBuocDayDu(6), t6);
        return review;
    }

    /**
     * Fall-through theo {@code run-flow.properties}:
     * {@code run.untilStep} = login|0…6 ; {@code run.submit} chỉ khi đến bước 6.
     * Không tương tác WebDriver trực tiếp — chỉ gọi Page Object / helper đã bọc.
     *
     * @return màn Xem lại nếu {@code untilStep >= 6}, ngược lại {@code null}
     */
    public XemLaiGuiDonPage chayTheoMasterConfig(TaoDonScenario s) {
        int until = vn.tuphap.automation.config.RunFlowConfig.untilStep();
        TestActionLog.ghiChu("Master run: untilStep=" + until
                + " (" + vn.tuphap.automation.config.RunFlowConfig.untilStepRaw() + ")"
                + ", submit=" + vn.tuphap.automation.config.RunFlowConfig.submit());

        try {
            if (until <= 0) {
                TestActionLog.ghiChu("untilStep=login — chỉ giữ session đăng nhập, không mở form nộp đơn.");
                return null;
            }

            moFormNopDonMoi();

            if (until >= 1) {
                dienBuoc1(s);
            }
            if (until < 2) {
                TestActionLog.ghiChu("Dừng sau bước 1 theo run-flow.properties.");
                return null;
            }

            if (until >= 2) {
                dienBuoc2(s);
            }
            if (until < 3) {
                TestActionLog.ghiChu("Dừng sau bước 2 (Nguyên đơn) theo run-flow.properties.");
                return null;
            }

            if (until >= 3) {
                dienBuoc3(s);
            }
            if (until < 4) {
                TestActionLog.ghiChu("Dừng sau bước 3 (Bị đơn) theo run-flow.properties.");
                return null;
            }

            if (until >= 4) {
                dienBuoc4(s);
            }
            if (until < 5) {
                TestActionLog.ghiChu("Dừng sau bước 4 (Nội dung) theo run-flow.properties.");
                return null;
            }

            if (until >= 5) {
                dienBuoc5(s);
            }
            if (until < 6) {
                TestActionLog.ghiChu("Dừng sau bước 5 (Tài liệu) theo run-flow.properties.");
                return null;
            }

            // Mở node bước 6 rồi PHẢI đóng. Bản cũ mở mà không bao giờ đóng: danh sách bước ghi
            // "Đạt" (do ketThucCase đóng hộ) trong khi StepOutcome không có bước 6 nên bảng tóm
            // tắt ghi "Chưa chạy tới" — hai bảng cạnh nhau nói ngược nhau, ở MỌI kịch bản của bộ
            // Master chạy với cấu hình mặc định.
            long t6 = BaoCao.markStepStart();
            BaoCao.beginStepNode(6, 6, TaoDonReportBuilder.tenBuocDayDu(6));
            XemLaiGuiDonPage review = new XemLaiGuiDonPage(driver);
            review.waitStepReady();
            webUI.captureOverview("Ảnh tổng quan — màn Xem lại (master untilStep=6)");
            BaoCao.logStepDone(6, 6, TaoDonReportBuilder.tenBuocDayDu(6), t6);
            return review;
        } catch (RuntimeException ex) {
            TestActionLog.ghiChu("Master run bị chặn: " + ex.getMessage());
            throw ex;
        }
    }

    /** Điền bước 1→3 rồi dừng (kiểm tra luồng nguyên đơn / bị đơn). */
    public void denHetBuoc3(TaoDonScenario s) {
        moFormNopDonMoi();
        dienBuoc1(s);
        dienBuoc2(s);
        dienBuoc3(s);
        webUI.captureOverview("Ảnh tổng quan — đã điền bước 1→3 [" + s.loaiDon() + "]");
    }

    public void tuXemLaiQuaBuoc5DenXemLai(TaoDonScenario s) {
        dienBuoc5(s);
    }

    /**
     * Sau [Chỉnh sửa] Xem trước đơn (đã về bước 1): form còn dữ liệu cũ —
     * chỉ Tiếp theo đến bước 4, cập nhật yêu cầu cụ thể, rồi bước 5 → Xem lại.
     * Tránh điền lại bước 2/3 (checkbox đại diện / địa chỉ đã sẵn → click nhầm tắt).
     */
    public void tiepTucNopDonTuBuoc1SauChinhSua(TaoDonScenario s, String yeuCauMoi) {
        chuyenDenBuoc4GiuDuLieuSauChinhSua();
        dienBuoc4CapNhatYeuCau(s, yeuCauMoi);
        dienBuoc5(s);
    }

    /** Từ bước 1 (sau Chỉnh sửa): bấm Tiếp theo đến khi thấy form bước 4. */
    private void chuyenDenBuoc4GiuDuLieuSauChinhSua() {
        long t = BaoCao.markStepStart();
        UiProfiler.reset();
        DriverCallCounter.reset();
        BaoCao.beginStepNode(1, 6, "Tiếp tục từ bước 1→4 (giữ dữ liệu sau Chỉnh sửa)");
        By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");
        for (int i = 0; i < 5; i++) {
            webUI.failIfBrowserClosed();
            if (webUI.existsNow(MARKER_NOI_DUNG)) {
                System.out.println(" ✅ Đã đến bước 4 [Nội dung đơn] (giữ dữ liệu sau Chỉnh sửa)");
                printStepProfile(System.currentTimeMillis() - t);
                System.out.println("⏱ Bước 1→4 (giữ dữ liệu sau Chỉnh sửa): "
                        + TaoDonReportBuilder.formatDuration(System.currentTimeMillis() - t));
                BaoCao.logStepDone(1, 6, "Tiếp tục đến bước 4 sau Chỉnh sửa", t);
                return;
            }
            if (!webUI.existsNow(btnTiepTheo)) {
                break;
            }
            webUI.logValidationMessages("Trước Tiếp theo — sau Chỉnh sửa (lần " + (i + 1) + ")");
            webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] (giữ dữ liệu sau Chỉnh sửa)");
            webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
        }
        webUI.waitUntilVisible(MARKER_NOI_DUNG, WaitConfig.STEP, "Bước 4 [Nội dung đơn] sau Chỉnh sửa");
        printStepProfile(System.currentTimeMillis() - t);
        System.out.println("⏱ Bước 1→4 (giữ dữ liệu sau Chỉnh sửa): "
                + TaoDonReportBuilder.formatDuration(System.currentTimeMillis() - t));
        BaoCao.logStepDone(1, 6, "Tiếp tục đến bước 4 sau Chỉnh sửa", t);
    }
}

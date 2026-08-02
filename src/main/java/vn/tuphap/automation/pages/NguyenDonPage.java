package vn.tuphap.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.data.DongNguyenDonData;
import vn.tuphap.automation.report.BaoCao;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.UiSynonyms;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.List;

public class NguyenDonPage {
    private WebDriver driver;
    private WebUI webUI;

    public static final String DONG_NGUYEN_DON_LABEL =
            "contains(., 'đồng nguyên đơn') or contains(., 'Đồng nguyên đơn')"
                    + " or contains(., 'đồng người khởi kiện') or contains(., 'Đồng người khởi kiện')";

    /** UAT — nút thêm đồng nguyên đơn (border-dashed, lucide-plus). */
    public static final By BTN_THEM_DONG_NGUYEN_DON = UiSynonyms.buttonThemDongNguyenDonVariants();

    /**
     * Khối đồng nguyên đơn UAT — neo theo nút [Thêm nguyên đơn] (border-dashed).
     */
    public static final String DONG_NGUYEN_DON_BLOCK =
            "(//button[contains(., 'Thêm nguyên đơn') or contains(., 'Thêm người khởi kiện')"
                    + " or contains(., 'Thêm đồng nguyên đơn') or contains(., 'Thêm đồng người khởi kiện')]"
                    + "/ancestor::div[contains(@class,'border') or contains(@class,'rounded')"
                    + " or contains(@class,'space-y') or contains(@class,'space')][1]"
                    + " | //div[" + DONG_NGUYEN_DON_LABEL + "]/ancestor::div[contains(@class,'border')"
                    + " or contains(@class,'rounded')][1])[last()]";

    /** Nút Thêm trong khối đồng nguyên đơn — tránh nhầm nút Thêm bị đơn ở bước khác. */
    private static final By BTN_THEM_TRONG_KHOI_DONG = By.xpath(
            DONG_NGUYEN_DON_BLOCK + "//button[(contains(@class,'border-dashed')"
                    + " or .//svg[contains(@class,'lucide-plus')])"
                    + " and (contains(., 'Thêm nguyên đơn') or contains(., 'Thêm người khởi kiện')"
                    + " or contains(., 'Thêm đồng nguyên đơn') or contains(., 'Thêm đồng người khởi kiện'))]");

    /**
     * Khối nguyên đơn chính — h2 (nếu có) hoặc card có tab Cá nhân/Tổ chức; loại trừ đồng nguyên đơn.
     */
    public static final String MAIN_SECTION =
            "("
                    + "(//h2[(contains(., 'Nguyên đơn') or contains(., 'người khởi kiện')"
                    + " or contains(., 'Người khởi kiện') or contains(., 'Thông tin nguyên'))"
                    + " and not(contains(., 'đồng')) and not(contains(., 'Đồng'))]/parent::div)[1]"
                    + " | (//div[.//div[contains(@class, 'cursor-pointer')"
                    + " and (contains(., 'Cá nhân') or contains(., 'Tổ chức'))]"
                    + " and (.//label[contains(., 'Họ và tên')] or .//label[contains(., 'Tên tổ chức')])"
                    + " and not(.//*[" + DONG_NGUYEN_DON_LABEL + "])])[1]"
                    + ")";

    /** Marker wizard bước 2 — chỉ trong khối nguyên đơn chính; tránh nhầm label [Họ và tên] ở bước 3 Bị đơn (Dân sự). */
    public static final By MARKER_NGUYEN_DON_CHINH = By.xpath(
            MAIN_SECTION + "//label[contains(., 'Họ và tên') or contains(., 'Tên tổ chức')]"
                    + " | " + MAIN_SECTION + "//label[contains(., 'Tỉnh') and contains(., 'thành phố')]"
                    + " | " + MAIN_SECTION + "//div[contains(@class, 'cursor-pointer')"
                    + " and (contains(., 'Cá nhân') or contains(., 'Tổ chức'))]"
                    + " | //h2[(contains(., 'Nguyên đơn') or contains(., 'người khởi kiện')"
                    + " or contains(., 'Người khởi kiện') or contains(., 'Thông tin nguyên'))"
                    + " and not(contains(., 'đồng')) and not(contains(., 'Đồng'))]");

    // ==========================================
    // 1. SMART LOCATORS (CHỐNG LỖI HOA/THƯỜNG & KHOẢNG TRẮNG)
    // ==========================================

    // Hàm bắt Loại chủ thể (Excel nhập "Cá nhân", "cá nhân", "TỔ CHỨC", "doanh nghiệp" đều nhận hết)
    private By getLoaiChuThe(String loai) {
        String tuKhoa = loai.trim().toLowerCase();
        String tabInline = MAIN_SECTION + "//div[contains(@class,'inline-flex')"
                + " and contains(@class,'overflow-hidden')]"
                + "//div[contains(@class,'cursor-pointer')";
        String tabFallback = MAIN_SECTION + "//div[contains(@class,'cursor-pointer')";

        if (tuKhoa.contains("tổ chức") || tuKhoa.contains("doanh nghiệp")) {
            return By.xpath(tabInline
                    + " and (contains(normalize-space(.), 'Tổ chức') or contains(normalize-space(.), 'Doanh nghiệp'))"
                    + " and not(contains(normalize-space(.), 'Cá nhân'))]"
                    + " | " + tabFallback
                    + " and (contains(., 'Tổ chức') or contains(., 'Doanh nghiệp')) and not(contains(., 'Cá nhân'))]"
                    + " | " + MAIN_SECTION + "//button[contains(., 'Tổ chức') or contains(., 'Doanh nghiệp')]"
                    + " | " + MAIN_SECTION + "//*[@role='tab' and (contains(., 'Tổ chức') or contains(., 'Doanh nghiệp'))]");
        }
        return By.xpath(tabInline
                + " and contains(normalize-space(.), 'Cá nhân') and not(contains(normalize-space(.), 'Tổ chức'))]"
                + " | " + tabFallback
                + " and contains(., 'Cá nhân') and not(contains(., 'Tổ chức'))]"
                + " | " + MAIN_SECTION + "//button[contains(., 'Cá nhân') and not(contains(., 'Tổ chức'))]"
                + " | " + MAIN_SECTION + "//*[@role='tab' and contains(., 'Cá nhân') and not(contains(., 'Tổ chức'))]");
    }

    // Hàm bắt Giới tính (Chống lỗi gõ "nam", "NỮ", "khác ")
    private By getGioiTinh(String gioiTinh) {
        return gioiTinhTrongScope(MAIN_SECTION, gioiTinh);
    }

    private By gioiTinhTrongScope(String scope, String gioiTinh) {
        String tuKhoa = gioiTinh == null || gioiTinh.isBlank() ? "nam" : gioiTinh.trim().toLowerCase();
        String value = tuKhoa.contains("nữ") || tuKhoa.contains("nu") ? "Nữ"
                : (tuKhoa.contains("khác") || tuKhoa.contains("khac") ? "Khác" : "Nam");
        // UAT Tổ chức: label Giới tính + sibling div chip Nam/Nữ (cursor-pointer).
        return By.xpath(scope
                + "//label[contains(normalize-space(.), 'Giới tính')]/following-sibling::div"
                + "//div[contains(@class,'cursor-pointer') and normalize-space()='" + value + "']"
                + " | " + scope
                + "//label[contains(., 'Giới tính')]/following-sibling::div"
                + "//div[contains(@class,'cursor-pointer') and contains(normalize-space(.), '" + value + "')]"
                + " | " + scope
                + "//label[contains(., 'Giới tính')]/following-sibling::div//div[contains(text(), '" + value + "')]"
                + " | " + scope
                + "//label[contains(., 'Giới tính')]/parent::div//div[contains(@class,'cursor-pointer')"
                + " and normalize-space()='" + value + "']"
                + " | " + scope
                + "//*[contains(@class,'font-medium') or self::label][contains(., 'Giới tính')]"
                + "/following::div[contains(@class,'cursor-pointer') and normalize-space()='" + value + "'][1]");
    }

    private void chonGioiTinhNguoiDaiDien(String gioiTinh) {
        String gt = (gioiTinh == null || gioiTinh.isBlank()) ? "Nam" : gioiTinh.trim();
        // Form tổ chức thường chỉ còn Nam/Nữ — không chọn Khác.
        if (gt.toLowerCase().contains("khác") || gt.toLowerCase().contains("khac")) {
            gt = "Nam";
        }
        By[] candidates = {
                getGioiTinhNguoiDaiDien(gt),
                getGioiTinh(gt),
                By.xpath(MAIN_SECTION
                        + "//label[contains(., 'Giới tính')]/following-sibling::div"
                        + "//div[contains(@class,'cursor-pointer') and normalize-space()='"
                        + (gt.toLowerCase().contains("nữ") || gt.toLowerCase().contains("nu") ? "Nữ" : "Nam")
                        + "']")
        };
        for (By by : candidates) {
            if (webUI.existsNow(by)) {
                try {
                    webUI.clickElement(by, "Giới tính người đại diện: [" + gt + "]");
                    return;
                } catch (RuntimeException ignored) {
                    webUI.clickElementOnceJs(by, "Giới tính người đại diện (JS): [" + gt + "]", 3);
                    return;
                }
            }
        }
        // Fallback: click chip Nam/Nữ đầu tiên trong khối người đại diện
        String chip = gt.toLowerCase().contains("nữ") || gt.toLowerCase().contains("nu") ? "Nữ" : "Nam";
        By fallback = By.xpath(
                "(" + MAIN_SECTION + "//label[contains(., 'Họ và tên người đại diện')]"
                        + "/ancestor::div[contains(@class,'border') or contains(@class,'rounded')"
                        + " or contains(@class,'space-y') or contains(@class,'space')][1]"
                        + " | " + MAIN_SECTION + ")"
                        + "//div[contains(@class,'cursor-pointer') and normalize-space()='" + chip + "']");
        if (webUI.existsNow(fallback)) {
            webUI.clickElementOnceJs(fallback, "Giới tính người đại diện (fallback): [" + chip + "]", 3);
            return;
        }
        throw new RuntimeException(
                "❌ Không chọn được Giới tính người đại diện [" + gt + "] trên form Tổ chức.");
    }

    // --- LOCATOR CÁ NHÂN ---
    private By txtHoTen = By.xpath(MAIN_SECTION + "//label[contains(text(),'Họ và tên')]/following-sibling::input");
    private By txtNgaySinh = By.xpath(MAIN_SECTION + "//label[contains(text(),'Ngày sinh')]/following-sibling::div/input");
    private By txtCCCD = By.xpath(MAIN_SECTION + "//label[contains(text(),'Số CCCD')]/following-sibling::input");
    private By txtNgayCapCCCD = By.xpath(MAIN_SECTION + "//label[contains(text(),'Ngày cấp CCCD')]/following-sibling::div/input");
    private By txtNoiCapCCCD = By.xpath(MAIN_SECTION + "//label[contains(text(),'Nơi cấp CCCD')]/following-sibling::input");
    private By txtDiaChiThuongTru = By.xpath(MAIN_SECTION + "//label[contains(text(),'Địa chỉ thường trú')]/following-sibling::textarea");

    /**
     * UAT: {@code div.cursor-pointer} + vòng tròn (không phải {@code label}/checkbox).
     * Neo theo text trên toàn trang — hai lựa chọn này unique ở bước Nguyên đơn.
     */
    private By optGiongThuongTru = By.xpath(
            "//div[contains(@class,'cursor-pointer')]"
                    + "[contains(., 'Địa chỉ liên lạc giống địa chỉ thường trú')"
                    + " or .//span[contains(., 'giống địa chỉ thường trú')]]"
                    + " | //span[contains(., 'giống địa chỉ thường trú')]"
                    + "/ancestor::div[contains(@class,'cursor-pointer')][1]"
                    + " | //label[.//span[contains(., 'giống địa chỉ thường trú')]"
                    + " or contains(., 'giống địa chỉ thường trú')]");

    /** Đồng ý lưu vào Thông tin định danh — cùng kiểu toggle vòng tròn. */
    private By optDongYLuuDinhDanh = By.xpath(
            "//div[contains(@class,'cursor-pointer')]"
                    + "[contains(., 'Thông tin định danh') and contains(., 'lưu các thông tin')]"
                    + " | //div[contains(@class,'cursor-pointer')]"
                    + "[.//span[contains(., 'lưu các thông tin đã nhập')]]"
                    + " | //span[contains(., 'lưu các thông tin đã nhập')]"
                    + "/ancestor::div[contains(@class,'cursor-pointer')][1]"
                    + " | //label[contains(., 'Thông tin định danh') and contains(., 'lưu')]");

    private By txtDiaChiLienLac = By.xpath(MAIN_SECTION + "//label[contains(text(),'Địa chỉ liên lạc')]/following-sibling::textarea");

    // --- LOCATOR TỔ CHỨC / DOANH NGHIỆP ---
    private By txtTenToChuc = By.xpath(MAIN_SECTION + "//label[contains(text(), 'Tên tổ chức')]/following-sibling::input");
    private By btnLoaiHinhToChuc = By.xpath(MAIN_SECTION + "//label[contains(text(), 'Loại hình tổ chức')]/following-sibling::div//button");
    private By listOptionsLoaiHinh = By.xpath(MAIN_SECTION + "//label[contains(text(), 'Loại hình tổ chức')]/following-sibling::div//div[@role='option']");
    private By txtMaSoThue = By.xpath(MAIN_SECTION + "//label[contains(text(), 'Mã số thuế')]/following-sibling::input");
    private By txtDiaChiTruSo = By.xpath(MAIN_SECTION + "//label[contains(text(), 'Địa chỉ trụ sở')]/following-sibling::textarea");
    private By txtNguoiDaiDienPL = By.xpath(MAIN_SECTION + "//label[contains(text(), 'Người đại diện pháp luật')]/following-sibling::input");
    private By txtChucVu = By.xpath(MAIN_SECTION + "//label[contains(text(), 'Chức vụ')]/following-sibling::input");

    // --- LOCATOR DÙNG CHUNG ---
    private By txtSoDienThoai = By.xpath(MAIN_SECTION + "//label[contains(text(),'Số điện thoại')]/following-sibling::input");
    private By txtEmail = By.xpath(MAIN_SECTION + "//label[contains(text(),'Email')]/following-sibling::input");

    private By chkNguoiDaiDien = By.xpath(
            MAIN_SECTION + "//span[contains(text(), 'Tôi có người đại diện pháp lý')]"
                    + " | //span[contains(text(), 'Tôi có người đại diện pháp lý')]");
    private By txtTenNguoiDaiDien = By.xpath(
            MAIN_SECTION + "//label[contains(text(),'Người đại diện pháp lý')]/following-sibling::input"
                    + " | //label[contains(text(),'Người đại diện pháp lý')]/following-sibling::input");
    private By btnDropdownQuanHe = By.xpath(
            MAIN_SECTION + "//label[contains(text(),'Quan hệ')]/following-sibling::div//button"
                    + " | //label[contains(text(),'Quan hệ đại diện')]/following-sibling::div//button");
    private By listOptionsQuanHe = By.xpath(
            MAIN_SECTION + "//label[contains(text(),'Quan hệ')]/following-sibling::div//div[@role='option']"
                    + " | //label[contains(text(),'Quan hệ đại diện')]/following-sibling::div//div[@role='option']");

    private By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    // ==========================================
    // 2. CONSTRUCTOR
    // ==========================================
    public NguyenDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    // ==========================================
    // 3. CÁC HÀM NGHIỆP VỤ
    // ==========================================

    public void chonLoaiChuThe(String loaiChuThe) {
        if (loaiChuThe == null || loaiChuThe.isBlank()) {
            return;
        }
        boolean wantOrg = DataDictionary.isToChuc(loaiChuThe);
        if (wantOrg ? mainToChucFormVisible() : mainCaNhanFormVisible()) {
            System.out.println(" ⏩ Tab nguyên đơn chính [" + loaiChuThe + "] đã hiển thị — không cần click.");
            TestActionLog.chon("Nguyên đơn — loại chủ thể", loaiChuThe);
            return;
        }
        webUI.clickElement(getLoaiChuThe(loaiChuThe), "Thẻ Loại chủ thể: [" + loaiChuThe + "]");
        TestActionLog.chon("Nguyên đơn — loại chủ thể", loaiChuThe);
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        if (wantOrg) {
            webUI.waitUntilVisible(txtTenToChuc, WaitConfig.FIELD, "Form Tổ chức sau chọn tab");
        }
    }

    private boolean mainCaNhanFormVisible() {
        return webUI.existsNow(txtHoTen) || webUI.existsNow(txtCCCD);
    }

    private boolean mainToChucFormVisible() {
        return webUI.existsNow(txtTenToChuc);
    }

    /** Tab Cá nhân/Tổ chức trong một scope (nguyên đơn chính hoặc card đồng ND). */
    private By tabChuTheTrongScope(String scope, String loaiChuThe) {
        boolean org = DataDictionary.isToChuc(loaiChuThe);
        String tabInline = scope + "//div[contains(@class,'inline-flex')"
                + " and contains(@class,'overflow-hidden')]"
                + "//div[contains(@class,'cursor-pointer')";
        String tabFallback = scope + "//div[contains(@class,'cursor-pointer')";
        if (org) {
            return By.xpath(tabInline
                    + " and (contains(normalize-space(.), 'Tổ chức') or contains(normalize-space(.), 'Doanh nghiệp'))"
                    + " and not(contains(normalize-space(.), 'Cá nhân'))]"
                    + " | " + tabFallback
                    + " and (contains(., 'Tổ chức') or contains(., 'Doanh nghiệp')) and not(contains(., 'Cá nhân'))]"
                    + " | " + scope + "//button[contains(., 'Tổ chức') or contains(., 'Doanh nghiệp')]"
                    + " | " + scope + "//*[@role='tab' and (contains(., 'Tổ chức') or contains(., 'Doanh nghiệp'))]");
        }
        return By.xpath(tabInline
                + " and contains(normalize-space(.), 'Cá nhân') and not(contains(normalize-space(.), 'Tổ chức'))]"
                + " | " + tabFallback
                + " and contains(., 'Cá nhân') and not(contains(., 'Tổ chức'))]"
                + " | " + scope + "//button[contains(., 'Cá nhân') and not(contains(., 'Tổ chức'))]"
                + " | " + scope + "//*[@role='tab' and contains(., 'Cá nhân') and not(contains(., 'Tổ chức'))]");
    }

    private void chonTabChuTheTrongScope(String tabScope, String fieldScope, String loaiChuThe, String logPrefix) {
        if (loaiChuThe == null || loaiChuThe.isBlank()) {
            return;
        }
        boolean wantOrg = DataDictionary.isToChuc(loaiChuThe);
        By tab = tabChuTheTrongScope(tabScope, loaiChuThe);
        if (!webUI.existsNow(tab)) {
            System.out.println(" ⚠ Không tìm thấy tab [" + loaiChuThe + "] trong form đồng ND — thử điền form mặc định.");
            TestActionLog.ghiChu("Không tìm thấy tab " + loaiChuThe + " trong form đồng ND");
            return;
        }
        webUI.scrollToElement(tab);
        webUI.clickElement(tab, logPrefix + ": [" + loaiChuThe + "]");
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        long deadline = System.currentTimeMillis() + WaitConfig.FIELD * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (wantOrg ? dongFieldVisible(fieldScope, "contains(., 'Tên tổ chức')")
                    : dongFieldVisible(fieldScope, "contains(., 'Số CCCD')")
                    || dongFieldVisible(fieldScope, "contains(., 'Hộ chiếu')")
                    || dongFieldVisible(fieldScope, "contains(., 'Họ và tên')")) {
                return;
            }
            webUI.sleepMillis(250);
        }
        System.out.println(" ⚠ Chưa xác nhận tab đồng ND [" + loaiChuThe + "] — vẫn thử điền form.");
    }

    /**
     * Kiểm tra không chờ — người gọi ({@code chonTabChuTheTrongScope}) đã tự giữ deadline
     * {@link WaitConfig#FIELD} và ngủ 250ms mỗi vòng. Dùng {@code isElementVisible} ở đây khiến
     * mỗi "vòng 250ms" thực tế tốn tới 3×{@link WaitConfig#PROBE_MS}, nên ngân sách 8s chỉ chạy
     * được 2 vòng. {@code existsNow} giữ nguyên trần mà poll đúng nhịp 250ms.
     */
    private boolean dongFieldVisible(String scope, String labelMatch) {
        return webUI.existsNow(By.xpath(scope + "//label[" + labelMatch + "]/following-sibling::div/input"
                + " | " + scope + "//label[" + labelMatch + "]/following-sibling::input"
                + " | " + scope + "//label[" + labelMatch + "]/following-sibling::textarea"
                + " | " + scope + "//label[" + labelMatch + "]/parent::div//input[not(@type='hidden')]"
                + " | " + scope + "//label[" + labelMatch + "]/parent::div//textarea"));
    }

    public void dienThongTinCaNhan(String hoTen, String ngaySinh, String gioiTinh, String cccd, String ngayCap, String noiCap) {
        webUI.setTextWithCheck(txtHoTen, hoTen, "Ô nhập [Họ và tên]");
        webUI.setTextForMaskedInput(txtNgaySinh, ngaySinh, "Ô nhập [Ngày sinh]");

        if (gioiTinh != null && !gioiTinh.isEmpty()) {
            webUI.clickChoiceChipIfNeeded(getGioiTinh(gioiTinh), "Thẻ Giới tính: [" + gioiTinh + "]");
        }

        webUI.setTextWithCheck(txtCCCD, cccd, "Ô nhập [Số CCCD / CMND]");
        webUI.setTextForMaskedInput(txtNgayCapCCCD, ngayCap, "Ô nhập [Ngày cấp CCCD]");
        webUI.setTextWithCheck(txtNoiCapCCCD, noiCap, "Ô nhập [Nơi cấp CCCD]");
    }

    public void dienThongTinLienHe(String thuongTru, String lienLac, String sdt, String email) {
        hoanThienDiaChiNguyenDon(thuongTru, lienLac);
        webUI.setTextWithCheck(txtSoDienThoai, sdt, "Ô nhập [Số điện thoại]");
        webUI.setTextWithCheck(txtEmail, email, "Ô nhập [Email]");
    }

    /** Hoàn thiện tỉnh/phường/chi tiết — một lần theo đúng thứ tự form. */
    public void hoanThienDiaChiNguyenDon(String thuongTru, String lienLac) {
        boolean giongThuongTru = isGiongThuongTru(lienLac, thuongTru);
        long t0 = System.currentTimeMillis();
        webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 0, thuongTru, "Thường trú");
        // Chỉ khối 0. Quét cả scope ở đây sẽ điền luôn tỉnh/phường ngẫu nhiên cho thẻ Liên lạc,
        // rồi chonDiaChiLienLacGiongThuongTru ngay dưới tick "giống thường trú" làm thẻ đó ẩn đi.
        webUI.forceSelectAdministrativeWardsInScope(MAIN_SECTION, 0);
        System.out.println(" ⏱ Khối địa chỉ Thường trú: " + (System.currentTimeMillis() - t0) + "ms");
        chonDiaChiLienLacGiongThuongTru(giongThuongTru);
        if (giongThuongTru) {
            return;
        }
        int blocks = webUI.countVisibleAddressBlocks(MAIN_SECTION);
        if (blocks >= 2) {
            long t1 = System.currentTimeMillis();
            webUI.dismissOpenDropdowns();
            webUI.sleepMillis(WaitConfig.ADDRESS_BLOCK_GAP_MS);
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 1, lienLac, "Liên lạc");
            webUI.forceSelectAdministrativeWardsInScope(MAIN_SECTION, 1);
            System.out.println(" ⏱ Khối địa chỉ Liên lạc: " + (System.currentTimeMillis() - t1) + "ms");
        } else if (webUI.existsNow(txtDiaChiLienLac) && webUI.isElementEnabledNow(txtDiaChiLienLac)) {
            webUI.setTextWithCheck(txtDiaChiLienLac, lienLac, "Ô nhập [Địa chỉ liên lạc]");
        }
    }

    /**
     * Chọn “Địa chỉ liên lạc giống địa chỉ thường trú”.
     * Xác nhận bằng: toggle đã chọn <b>và/hoặc</b> thẻ địa chỉ liên lạc ẩn (còn ≤1 thẻ).
     */
    public void chonDiaChiLienLacGiongThuongTru(boolean giong) {
        if (!webUI.existsNow(optGiongThuongTru)) {
            webUI.scrollToElement(By.xpath(
                    "//*[contains(., 'Địa chỉ thường trú') or contains(., 'giống địa chỉ thường trú')]"));
            webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        }
        if (!webUI.existsNow(optGiongThuongTru)) {
            System.out.println(" ⏩ Bỏ qua [Địa chỉ liên lạc giống thường trú] — không có trên biểu mẫu.");
            TestActionLog.boQua("Địa chỉ liên lạc giống địa chỉ thường trú", "Không có trên biểu mẫu");
            return;
        }
        webUI.scrollToElement(optGiongThuongTru);
        if (!giong) {
            webUI.ensureCustomToggleSelected(optGiongThuongTru, false,
                    "Hộp kiểm [Địa chỉ liên lạc giống địa chỉ thường trú]");
            return;
        }
        if (daChonGiongThuongTru()) {
            System.out.println(" ⏩ [Địa chỉ liên lạc giống địa chỉ thường trú] đã chọn sẵn.");
            TestActionLog.chon("Hộp kiểm [Địa chỉ liên lạc giống địa chỉ thường trú]", "Đã chọn sẵn");
            return;
        }
        webUI.clickElement(optGiongThuongTru,
                "Hộp kiểm [Địa chỉ liên lạc giống địa chỉ thường trú]");
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        waitLienLacAddressHidden();
        if (daChonGiongThuongTru()) {
            System.out.println(" ✅ Đã chọn [Địa chỉ liên lạc giống địa chỉ thường trú]"
                    + " — thẻ liên lạc đã ẩn / toggle active.");
            return;
        }
        // Click chưa ăn — thử 1 lần nữa rồi xác nhận; không thì chụp ảnh (không chặn nếu vẫn đi tiếp được).
        System.out.println(" ⚠ Toggle giống thường trú chưa xác nhận — click lại 1 lần...");
        webUI.clickElement(optGiongThuongTru,
                "Hộp kiểm [Địa chỉ liên lạc giống địa chỉ thường trú] (lần 2)");
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        waitLienLacAddressHidden();
        if (daChonGiongThuongTru()) {
            System.out.println(" ✅ Đã chọn [Địa chỉ liên lạc giống địa chỉ thường trú] sau lần 2.");
            return;
        }
        String shot = webUI.takeScreenshotPreserveToast();
        String msg = "Chưa xác nhận được chọn 'Địa chỉ liên lạc giống địa chỉ thường trú'"
                + " (toggle/thẻ liên lạc vẫn hiện).";
        System.out.println(" ⚠ " + msg);
        TestActionLog.validation("Địa chỉ liên lạc giống thường trú", msg);
        if (shot != null) {
            BaoCao.logWarningWithScreenshot(msg, shot);
        } else {
            BaoCao.logWarning(msg);
        }
    }

    /** Đã chọn giống thường trú? — ưu tiên thẻ liên lạc ẩn; fallback trạng thái vòng tròn. */
    private boolean daChonGiongThuongTru() {
        int blocks = webUI.countVisibleAddressBlocks(MAIN_SECTION);
        if (blocks <= 1 && webUI.existsNow(optGiongThuongTru)) {
            return true;
        }
        return webUI.isCustomToggleSelected(optGiongThuongTru);
    }

    /**
     * Đồng ý lưu thông tin vào 'Thông tin định danh'.
     * @return {@code true} nếu đã tick checkbox (có thể gây VNeID prefill) — cần pass kiểm tra lại;
     *         {@code false} nếu không có / bỏ qua.
     */
    public boolean chonDongYLuuThongTinDinhDanh() {
        if (!webUI.existsNow(optDongYLuuDinhDanh)) {
            webUI.scrollToElement(By.xpath(
                    "//*[contains(., 'Thông tin định danh') or contains(., 'lưu các thông tin đã nhập')]"));
            webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        }
        if (!webUI.existsNow(optDongYLuuDinhDanh)) {
            System.out.println(" ⏩ Bỏ qua [Đồng ý lưu Thông tin định danh] — không có trên biểu mẫu.");
            TestActionLog.boQua("Đồng ý lưu Thông tin định danh", "Không có trên biểu mẫu");
            return false;
        }
        webUI.scrollToElement(optDongYLuuDinhDanh);
        webUI.ensureCustomToggleSelected(optDongYLuuDinhDanh, true,
                "Hộp kiểm [Đồng ý lưu Thông tin định danh]");
        // Banner VNeID / toast thường hiện ngay sau tick — chụp trước khi cuộn form điền lại.
        webUI.logFeedbackAfterIdentitySave();
        return true;
    }

    private void waitLienLacAddressHidden() {
        long deadline = System.currentTimeMillis() + WaitConfig.FIELD * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (webUI.countVisibleAddressBlocks(MAIN_SECTION) <= 1) {
                return;
            }
            webUI.sleepMillis(150);
        }
    }

    /** Chỉ bổ sung khối địa chỉ còn thiếu — không click lại toggle giống thường trú. */
    public void chuanBiDiaChiTruocTiepTheo(String thuongTru, String lienLac) {
        boolean giongThuongTru = isGiongThuongTru(lienLac, thuongTru);
        if (!webUI.isAdministrativeAddressBlockComplete(MAIN_SECTION, 0)) {
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 0, thuongTru);
        }
        if (giongThuongTru) {
            return;
        }
        int blocks = webUI.countVisibleAddressBlocks(MAIN_SECTION);
        if (blocks >= 2 && !webUI.isAdministrativeAddressBlockComplete(MAIN_SECTION, 1)) {
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 1, lienLac);
        }
    }

    private static boolean isGiongThuongTru(String lienLac, String thuongTru) {
        return lienLac == null || lienLac.isBlank()
                || lienLac.toLowerCase().contains("giống thường trú")
                || lienLac.trim().equals(thuongTru == null ? "" : thuongTru.trim());
    }

    private void dienChiTietDiaChi(int blockIndex, String value, String label, By legacyTextarea) {
        if (value == null || value.isBlank()) {
            return;
        }
        By detail = webUI.addressDetailTextareaAtBlock(MAIN_SECTION, blockIndex);
        if (!webUI.existsNow(detail)) {
            detail = webUI.addressDetailTextareaInScope(MAIN_SECTION);
        }
        if (webUI.existsNow(detail)) {
            webUI.waitUntilVisible(detail, WaitConfig.FIELD, "Chi tiết [" + label + "]");
            webUI.setTextWithCheck(detail, value, "Ô nhập [Chi tiết — " + label + "]");
            return;
        }
        // Chỉ tới đây sau 2 lần existsNow trượt ở trên; không có gì render lại xen giữa.
        if (webUI.isElementVisible(legacyTextarea)) {
            webUI.setTextWithCheck(legacyTextarea, value, "Ô nhập [" + label + "]");
        }
    }

    private By fieldInMain(String labelMatch) {
        return By.xpath(MAIN_SECTION + "//label[" + labelMatch + "]/following-sibling::input"
                + " | " + MAIN_SECTION + "//label[" + labelMatch + "]/following-sibling::div//input"
                + " | " + MAIN_SECTION + "//label[" + labelMatch + "]/following-sibling::textarea"
                + " | " + MAIN_SECTION + "//label[" + labelMatch + "]/parent::div//input[not(@type='hidden')]"
                + " | " + MAIN_SECTION + "//label[" + labelMatch + "]/parent::div//textarea");
    }

    private By getGioiTinhNguoiDaiDien(String gioiTinh) {
        String repBlock = MAIN_SECTION
                + "//label[contains(., 'Họ và tên người đại diện')]/ancestor::div[contains(@class,'border')"
                + " or contains(@class,'rounded') or contains(@class,'space-y') or contains(@class,'space')][1]";
        return gioiTinhTrongScope("(" + repBlock + " | " + MAIN_SECTION + ")", gioiTinh);
    }

    private By getGioiTinhTrongDong(String gioiTinh) {
        return gioiTinhTrongScope(dongFormScope(), gioiTinh);
    }

    public void dienThongTinToChuc(String tenToChuc, String loaiHinh, String mst, String diaChi,
                                   String nguoiDaiDien, String chucVu, String sdt, String email,
                                   String repNgaySinh, String repGioiTinh, String repCccd,
                                   String repNgayCap, String noiCap) {
        webUI.setTextWithCheck(txtTenToChuc, tenToChuc, "Ô nhập [Tên tổ chức / doanh nghiệp]");
        webUI.selectDropdownWithCheck(btnLoaiHinhToChuc, listOptionsLoaiHinh, loaiHinh, "Dropdown [Loại hình tổ chức]");
        webUI.setTextWithCheck(txtMaSoThue, mst, "Ô nhập [Mã số thuế / MSDN]");
        dienDiaChiToChuc(diaChi);
        dienNguoiDaiDienToChuc(nguoiDaiDien, repNgaySinh, repGioiTinh, repCccd, repNgayCap, noiCap,
                chucVu, diaChi, email);
        webUI.setTextWithCheck(txtSoDienThoai, sdt, "Ô nhập [Số điện thoại tổ chức]");
    }

    private void dienDiaChiToChuc(String diaChi) {
        webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 0, diaChi);
    }

    private void dienNguoiDaiDienToChuc(String hoTen, String ngaySinh, String gioiTinh, String cccd,
                                        String ngayCap, String noiCap, String chucVu,
                                        String diaChiCuTru, String email) {
        By txtHoTenRep = fieldInMain("contains(., 'Họ và tên người đại diện')");
        if (webUI.isElementVisible(txtHoTenRep)) {
            webUI.setTextWithCheck(txtHoTenRep, hoTen, "Ô nhập [Họ và tên người đại diện]");
            // Từ đây trở xuống đều nằm trong khối người đại diện đã xác nhận hiển thị ở trên,
            // không có re-render xen giữa → kiểm tra không chờ.
            By txtNgaySinhRep = fieldInMain("contains(., 'Ngày, tháng, năm sinh')");
            if (webUI.isElementVisible(txtNgaySinhRep)) {
                webUI.setTextForMaskedInput(txtNgaySinhRep, ngaySinh, "Ô nhập [Ngày, tháng, năm sinh người đại diện]");
            }
            chonGioiTinhNguoiDaiDien(gioiTinh);
            By txtCccdRep = fieldInMain("contains(., 'Số CCCD') or contains(., 'Hộ chiếu')");
            if (webUI.isElementVisible(txtCccdRep)) {
                webUI.setTextWithCheck(txtCccdRep, cccd, "Ô nhập [Số CCCD / Hộ chiếu người đại diện]");
            }
            By txtCapNgay = fieldInMain("contains(., 'Cấp ngày')");
            if (webUI.isElementVisible(txtCapNgay)) {
                webUI.setTextForMaskedInput(txtCapNgay, ngayCap, "Ô nhập [Cấp ngày CCCD người đại diện]");
            }
            By txtNoiCapRep = fieldInMain("contains(., 'Nơi cấp')");
            if (webUI.isElementVisible(txtNoiCapRep) && noiCap != null && !noiCap.isBlank()) {
                webUI.setTextWithCheck(txtNoiCapRep, noiCap, "Ô nhập [Nơi cấp người đại diện]");
            }
            dienDiaChiNguoiDaiDienToChuc(diaChiCuTru);
        } else if (webUI.isElementVisible(txtNguoiDaiDienPL)) {
            webUI.setTextWithCheck(txtNguoiDaiDienPL, hoTen, "Ô nhập [Người đại diện pháp luật]");
        } else {
            TestActionLog.boQua("Người đại diện tổ chức", "Không có trên biểu mẫu");
        }
        if (chucVu != null && !chucVu.isBlank()) {
            webUI.setTextWithCheck(txtChucVu, chucVu, "Ô nhập [Chức vụ]");
        }
        By txtEmailRep = fieldInMain("contains(., 'thư điện tử') or contains(., 'Email')");
        if (webUI.isElementVisible(txtEmailRep) && email != null && !email.isBlank()) {
            webUI.setTextWithCheck(txtEmailRep, email, "Ô nhập [Địa chỉ thư điện tử]");
        }
    }

    /** Khối địa chỉ thứ 2 — nơi cư trú người đại diện theo pháp luật (UAT Tổ chức). */
    private void dienDiaChiNguoiDaiDienToChuc(String diaChiCuTru) {
        if (diaChiCuTru == null || diaChiCuTru.isBlank()) {
            return;
        }
        int blocks = webUI.countVisibleAddressBlocks(MAIN_SECTION);
        if (blocks >= 2) {
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 1, diaChiCuTru, "Nơi cư trú");
            return;
        }
        By txtCuTru = fieldInMain("contains(., 'Địa chỉ nơi cư trú')");
        if (webUI.isElementVisible(txtCuTru)) {
            webUI.setTextWithCheck(txtCuTru, diaChiCuTru, "Ô nhập [Địa chỉ nơi cư trú người đại diện]");
        }
    }

    public void chonNguoiDaiDien(String coNguoiDaiDien, String tenNguoiDaiDien, String quanHe) {
        revealNguoiDaiDienSection();
        if (!webUI.existsNow(chkNguoiDaiDien) && !webUI.existsNow(txtTenNguoiDaiDien)) {
            System.out.println(" ⏩ Bỏ qua [Người đại diện pháp lý] — không có trên biểu mẫu (vd. Tổ chức / Hành chính).");
            TestActionLog.boQua("Người đại diện pháp lý", "Không có trên biểu mẫu");
            return;
        }
        if (coNguoiDaiDien != null && coNguoiDaiDien.trim().equalsIgnoreCase("có")) {
            // Form đã mở sẵn (vd. sau Chỉnh sửa từ Xem lại) — không click lại checkbox (sẽ tắt).
            boolean alreadyOpen = webUI.existsNow(txtTenNguoiDaiDien);
            if (!alreadyOpen && webUI.existsNow(chkNguoiDaiDien)) {
                webUI.clickElement(chkNguoiDaiDien, "Checkbox [Tôi có người đại diện pháp lý]");
            } else if (alreadyOpen) {
                System.out.println(" ⏩ [Người đại diện pháp lý] đã mở sẵn — không click lại checkbox.");
            }
            webUI.waitUntilVisible(txtTenNguoiDaiDien, WaitConfig.FIELD, "Ô [Người đại diện pháp lý]");
            webUI.setTextWithCheck(txtTenNguoiDaiDien, tenNguoiDaiDien, "Ô nhập [Người đại diện pháp lý]");
            if (webUI.existsNow(btnDropdownQuanHe)) {
                webUI.selectDropdownWithCheck(btnDropdownQuanHe, listOptionsQuanHe, quanHe, "Dropdown [Quan hệ đại diện]");
            }
        } else {
            System.out.println(" ⏩ Bỏ qua Checkbox [Người đại diện] vì không yêu cầu.");
            TestActionLog.boQua("Checkbox [Người đại diện pháp lý]", "Không yêu cầu");
        }
    }

    private void revealNguoiDaiDienSection() {
        if (webUI.countNow(chkNguoiDaiDien) > 0) {
            webUI.scrollToElement(chkNguoiDaiDien);
        } else if (webUI.countNow(txtTenNguoiDaiDien) > 0) {
            webUI.scrollToElement(txtTenNguoiDaiDien);
        }
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
    }

    private void revealDongNguyenDonSection() {
        if (webUI.countNow(BTN_THEM_DONG_NGUYEN_DON) > 0) {
            webUI.scrollToElement(BTN_THEM_DONG_NGUYEN_DON);
        } else {
            By dongBlock = By.xpath(DONG_NGUYEN_DON_BLOCK);
            if (webUI.countNow(dongBlock) > 0) {
                webUI.scrollToElement(dongBlock);
            }
        }
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
    }

    /**
     * Phá sản — danh sách thả xuống "Tư cách người nộp đơn" (Chủ nợ, Người lao động, …).
     * Bỏ qua nếu không có trên biểu mẫu (các loại đơn khác).
     */
    public void chonTuCachNguoiNopDon(String tuCach) {
        By btn = By.xpath(
                MAIN_SECTION + "//label[contains(., 'Tư cách người nộp đơn') or contains(., 'Tư cách')]"
                        + "/following-sibling::div//button"
                        + " | " + MAIN_SECTION
                        + "//label[contains(., 'Tư cách người nộp đơn') or contains(., 'Tư cách')]"
                        + "/ancestor::div[contains(@class,'space') or contains(@class,'grid') or contains(@class,'flex')][1]"
                        + "//button[contains(., 'Chọn') or contains(., '—') or contains(., 'Chủ nợ')]"
                        + " | //label[contains(., 'Tư cách người nộp đơn') or contains(., 'Tư cách')]"
                        + "/following-sibling::div//button");
        By options = By.xpath("//div[@role='option']");
        if (!webUI.existsNow(btn)) {
            System.out.println(" ⏩ Bỏ qua [Tư cách người nộp đơn] — không có trên biểu mẫu.");
            TestActionLog.boQua("Tư cách người nộp đơn", "Không có trên biểu mẫu");
            return;
        }
        if (tuCach == null || tuCach.isBlank()) {
            tuCach = "Chủ nợ";
        }
        webUI.selectDropdownWithCheck(btn, options, tuCach, "Dropdown [Tư cách người nộp đơn]");
    }

    /**
     * UAT — đồng nguyên đơn chỉ có nút [Thêm nguyên đơn] (không có toggle Có/Không).
     * {@code Không} = không bấm Thêm; {@code Có} = bấm Thêm và điền form con nếu có dữ liệu.
     */
    public void chonDongNguyenDon(String coDongNguyenDon) {
        xuLyDongNguyenDon(coDongNguyenDon, null);
    }

    public void xuLyDongNguyenDon(String coDongNguyenDon, DongNguyenDonData data) {
        revealDongNguyenDonSection();
        String luaChon = (coDongNguyenDon == null || coDongNguyenDon.isBlank()) ? "Không" : coDongNguyenDon.trim();

        if (!coKhoiDongNguyenDonTrenForm()) {
            System.out.println(" ⏩ Bỏ qua [Đồng nguyên đơn] — không có trên biểu mẫu.");
            TestActionLog.boQua("Đồng nguyên đơn / đồng người khởi kiện", "Không có trên biểu mẫu");
            return;
        }

        if (!luaChon.equalsIgnoreCase("có")) {
            System.out.println(" ⏩ [Đồng nguyên đơn]: Không — bỏ qua nút [Thêm nguyên đơn].");
            TestActionLog.chon("Đồng nguyên đơn", "Không (không bấm Thêm)");
            xoaDongNguyenDonTrongNeuCo();
            return;
        }

        clickThemDongNguyenDonNeuCan();
        choFormDongNguyenDonSanSang();
        if (data != null) {
            dienDongNguyenDon(data);
        } else {
            System.out.println(" ⚠ Scenario yêu cầu đồng nguyên đơn nhưng thiếu dữ liệu form con.");
            TestActionLog.boQua("Form đồng nguyên đơn", "Thiếu DongNguyenDonData trong kịch bản");
        }
    }

    private void choFormDongNguyenDonSanSang() {
        By marker = By.xpath(
                dongFormScope() + "//label[contains(., 'Ngày, tháng, năm sinh') or contains(., 'Năm sinh')"
                        + " or contains(., 'Số CCCD / Hộ chiếu') or contains(., 'Số CCCD')"
                        + " or contains(., 'Địa chỉ nơi cư trú') or contains(., 'Họ và tên')"
                        + " or contains(., 'Tên tổ chức')]");
        webUI.waitUntilVisible(marker, WaitConfig.FIELD, "Form đồng nguyên đơn sau [Thêm]");
    }

    /**
     * Card form con vừa mở — neo nút xóa (trash) trong khối đồng ND.
     * Fallback label nếu chưa có trash.
     */
    private String dongFormScope() {
        String expanded = "(" + DONG_NGUYEN_DON_BLOCK
                + "//button[.//svg[contains(@class,'lucide-trash')]]"
                + "/ancestor::div[contains(@class,'border') or contains(@class,'rounded')"
                + " or contains(@class,'space-y')][1])[last()]";
        if (webUI.existsNow(By.xpath(expanded + "//label"))) {
            return expanded;
        }
        return "(" + DONG_NGUYEN_DON_BLOCK
                + "//label[contains(., 'Ngày, tháng, năm sinh') or contains(., 'Số CCCD / Hộ chiếu')"
                + " or contains(., 'Số CCCD') or contains(., 'Địa chỉ nơi cư trú')"
                + " or contains(., 'Tên tổ chức') or contains(., 'Họ và tên')]"
                + "/ancestor::div[contains(@class,'border') or contains(@class,'rounded')"
                + " or contains(@class,'space-y') or contains(@class,'space')][1])";
    }

    private By fieldInDongScope(String labelMatch) {
        String scope = dongFormScope();
        return By.xpath(scope + "//label[" + labelMatch + "]/following-sibling::div/input"
                + " | " + scope + "//label[" + labelMatch + "]/following-sibling::input"
                + " | " + scope + "//label[" + labelMatch + "]/following-sibling::textarea"
                + " | " + scope + "//label[" + labelMatch + "]/parent::div//textarea"
                + " | " + scope + "//label[" + labelMatch + "]/following-sibling::div/textarea"
                + " | " + scope + "//label[" + labelMatch + "]/parent::div//input[not(@type='hidden')]");
    }

    private boolean dongCaNhanFormVisible() {
        String scope = dongFormScope();
        return dongFieldVisible(scope, "contains(., 'Số CCCD') or contains(., 'Hộ chiếu')")
                || dongFieldVisible(scope, "contains(., 'Họ và tên')")
                || dongFieldVisible(scope, "contains(., 'Ngày, tháng, năm sinh')")
                || dongFieldVisible(scope, "contains(., 'Ngày sinh')")
                || dongFieldVisible(scope, "contains(., 'Năm sinh')");
    }

    private boolean dongToChucFormVisible() {
        return dongFieldVisible(dongFormScope(), "contains(., 'Tên tổ chức')");
    }

    /** Scope chứa tab Cá nhân/Tổ chức trong card đồng ND (thường cao hơn form field scope). */
    private String dongTabScope() {
        String tabHost = DONG_NGUYEN_DON_BLOCK
                + "//div[contains(@class,'inline-flex') and contains(@class,'overflow-hidden')]"
                + "/ancestor::div[contains(@class,'border') or contains(@class,'rounded')][1]";
        By probe = By.xpath("(" + tabHost + ")[last()]");
        if (webUI.existsNow(probe)) {
            return "(" + tabHost + ")[last()]";
        }
        return dongFormScope();
    }

    private void chonLoaiChuTheDong(String loaiChuThe) {
        if (loaiChuThe == null || loaiChuThe.isBlank()) {
            return;
        }
        boolean wantOrg = DataDictionary.isToChuc(loaiChuThe);
        if (wantOrg && dongToChucFormVisible()) {
            System.out.println(" ⏩ Tab đồng ND [Tổ chức] đã hiển thị — không cần click.");
            TestActionLog.chon("Đồng nguyên đơn — loại chủ thể", loaiChuThe);
            return;
        }
        if (!wantOrg && dongCaNhanFormVisible() && !dongToChucFormVisible()) {
            System.out.println(" ⏩ Tab đồng ND [Cá nhân] đã hiển thị — không cần click.");
            TestActionLog.chon("Đồng nguyên đơn — loại chủ thể", loaiChuThe);
            return;
        }
        chonTabChuTheTrongScope(dongTabScope(), dongFormScope(), loaiChuThe, "Tab đồng nguyên đơn");
        TestActionLog.chon("Đồng nguyên đơn — loại chủ thể", loaiChuThe);
    }

    public void dienDongNguyenDon(DongNguyenDonData data) {
        if (data == null) {
            return;
        }
        TestActionLog.dien("Đồng nguyên đơn", "Điền form con sau [Thêm]");
        webUI.scrollToElement(By.xpath(dongFormScope()));
        chonLoaiChuTheDong(data.loai());

        if (DataDictionary.isToChuc(data.loai())) {
            dienDongNguyenDonToChuc(data);
        } else {
            dienDongNguyenDonCaNhan(data);
        }
        kiemTraVaXuLyValidationDongNd(data);
    }

    /** Tỉnh/Phường + textarea địa chỉ bắt buộc trong card đồng ND. */
    private void dienDiaChiDong(String diaChiChiTiet, String noiOHienTai, String... labelDiaChi) {
        if (diaChiChiTiet == null || diaChiChiTiet.isBlank()) {
            return;
        }
        String scope = dongFormScope();
        webUI.scrollToElement(By.xpath(scope));
        webUI.selectAdministrativeAddressInScope(scope);
        By txtDiaChi = resolveDongAddressTextarea(scope, labelDiaChi);
        webUI.waitUntilVisible(txtDiaChi, WaitConfig.FIELD, "Đồng ND — ô địa chỉ");
        webUI.scrollToElement(txtDiaChi);
        webUI.setTextWithCheck(txtDiaChi, diaChiChiTiet, "Đồng ND — [Địa chỉ chi tiết]");
        String noiO = (noiOHienTai == null || noiOHienTai.isBlank()) ? diaChiChiTiet : noiOHienTai;
        By txtNoiO = fieldInDongScope("contains(., 'Nơi ở hiện tại')");
        // Thẻ đồng ND vừa được setTextWithCheck ngay trên → chắc chắn đã render.
        if (webUI.isElementVisible(txtNoiO)) {
            webUI.setTextWithCheck(txtNoiO, noiO, "Đồng ND — [Nơi ở hiện tại]");
        }
    }

    private By resolveDongAddressTextarea(String scope, String... labelDiaChi) {
        for (String label : labelDiaChi) {
            By candidate = fieldInDongScope(label);
            if (webUI.existsNow(candidate)) {
                return candidate;
            }
        }
        By byLabel = fieldInDongScope("contains(., 'Địa chỉ nơi cư trú') or contains(., 'Địa chỉ trụ sở')");
        if (webUI.existsNow(byLabel)) {
            return byLabel;
        }
        By placeholder = webUI.addressDetailTextareaInScope(scope);
        if (webUI.existsNow(placeholder)) {
            return placeholder;
        }
        return webUI.addressDetailTextareaInScope(DONG_NGUYEN_DON_BLOCK);
    }

    private void kiemTraVaXuLyValidationDongNd(DongNguyenDonData data) {
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        List<String> scoped = webUI.collectValidationMessagesInScope(dongFormScope());
        if (scoped.isEmpty()) {
            return;
        }
        System.out.println(" ⚠ Validation [Đồng nguyên đơn]: " + String.join(" | ", scoped));
        for (String msg : scoped) {
            TestActionLog.validation("Đồng nguyên đơn", msg);
        }
        String joined = String.join(" ", scoped).toLowerCase();
        if (joined.contains("địa chỉ") || joined.contains("tỉnh") || joined.contains("phường")
                || joined.contains("xã") || joined.contains("cư trú") || joined.contains("trụ sở")) {
            if (DataDictionary.isToChuc(data.loai())) {
                dienDiaChiDong(data.diaChiTruSo(), data.diaChiTruSo(),
                        "contains(., 'Địa chỉ trụ sở')", "contains(., 'Địa chỉ nơi cư trú')");
            } else {
                dienDiaChiDong(data.diaChiCuTru(), data.noiOHienTai(),
                        "contains(., 'Địa chỉ nơi cư trú')");
            }
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
            webUI.logValidationMessages("Đồng nguyên đơn (sau điền lại địa chỉ)");
        } else if (joined.contains("giới tính") && !DataDictionary.isToChuc(data.loai())) {
            String gt = (data.gioiTinh() == null || data.gioiTinh().isBlank()) ? "Nam" : data.gioiTinh();
            By gioiTinhDong = getGioiTinhTrongDong(gt);
            if (webUI.isElementVisible(gioiTinhDong)) {
                webUI.clickElement(gioiTinhDong, "Đồng ND — Giới tính (bổ sung): [" + gt + "]");
            }
        }
    }

    private static String namSinhTuNgay(String ngaySinh) {
        if (ngaySinh == null || ngaySinh.isBlank()) {
            return "1990";
        }
        String[] parts = ngaySinh.trim().split("[/.\\-]");
        if (parts.length >= 3) {
            return parts[2].length() == 4 ? parts[2] : parts[parts.length - 1];
        }
        if (parts.length == 1 && parts[0].matches("\\d{4}")) {
            return parts[0];
        }
        return "1990";
    }

    private void dienDongNguyenDonCaNhan(DongNguyenDonData data) {
        By txtHoTen = fieldInDongScope("contains(., 'Họ và tên')");
        if (data.hoTen() != null && !data.hoTen().isBlank()) {
            webUI.setTextWithCheck(txtHoTen, data.hoTen(), "Đồng ND — [Họ và tên]");
        }
        By txtNgaySinh = fieldInDongScope("contains(., 'Ngày, tháng, năm sinh') or contains(., 'Ngày sinh')");
        By txtNamSinh = fieldInDongScope("contains(., 'Năm sinh')");
        // Biểu mẫu chỉ có một trong hai ô → luôn có đúng 1 lần trượt. Với isElementVisible thì
        // lần trượt đó đốt trọn PROBE_MS (1.2s) cho mỗi đồng nguyên đơn.
        if (webUI.isElementVisible(txtNgaySinh)) {
            webUI.setTextForMaskedInput(txtNgaySinh, data.ngaySinh(), "Đồng ND — [Ngày, tháng, năm sinh]");
        } else if (webUI.isElementVisible(txtNamSinh)) {
            webUI.setTextWithCheck(txtNamSinh, namSinhTuNgay(data.ngaySinh()), "Đồng ND — [Năm sinh]");
        } else {
            webUI.setTextForMaskedInput(txtNgaySinh, data.ngaySinh(), "Đồng ND — [Ngày, tháng, năm sinh]");
        }
        String gt = (data.gioiTinh() == null || data.gioiTinh().isBlank()) ? "Nam" : data.gioiTinh();
        By gioiTinhDong = getGioiTinhTrongDong(gt);
        if (webUI.isElementVisible(gioiTinhDong)) {
            webUI.clickElement(gioiTinhDong, "Đồng ND — Giới tính: [" + gt + "]");
        }
        webUI.setTextWithCheck(
                fieldInDongScope("contains(., 'Số CCCD') or contains(., 'Hộ chiếu')"),
                data.cccd(), "Đồng ND — [Số CCCD / Hộ chiếu]");
        dienDiaChiDong(data.diaChiCuTru(), data.noiOHienTai(), "contains(., 'Địa chỉ nơi cư trú')");
        if (data.ngheNghiep() != null && !data.ngheNghiep().isBlank()) {
            webUI.setTextWithCheck(
                    fieldInDongScope("contains(., 'Nghề nghiệp') or contains(., 'nơi làm việc')"),
                    data.ngheNghiep(), "Đồng ND — [Nghề nghiệp, nơi làm việc]");
        }
        webUI.setTextWithCheck(
                fieldInDongScope("contains(., 'Số điện thoại')"),
                data.sdt(), "Đồng ND — [Số điện thoại]");
        webUI.setTextWithCheck(
                fieldInDongScope("contains(., 'thư điện tử') or contains(., 'Email')"),
                data.email(), "Đồng ND — [Địa chỉ thư điện tử]");
    }

    private void dienDongNguyenDonToChuc(DongNguyenDonData data) {
        webUI.setTextWithCheck(
                fieldInDongScope("contains(., 'Tên tổ chức')"),
                data.tenToChuc(), "Đồng ND — [Tên tổ chức]");
        String scope = dongFormScope();
        By btnLoaiHinh = By.xpath(scope + "//label[contains(., 'Loại hình')]/following-sibling::div//button"
                + " | " + scope + "//label[contains(., 'Loại hình')]/parent::div//button");
        By listOptions = By.xpath("//div[@role='listbox']//div[@role='option'] | //div[@role='option']");
        if (webUI.existsNow(btnLoaiHinh) && data.loaiHinh() != null && !data.loaiHinh().isBlank()) {
            webUI.selectDropdownWithCheck(btnLoaiHinh, listOptions, data.loaiHinh(), "Đồng ND — [Loại hình tổ chức]");
        }
        webUI.setTextWithCheck(
                fieldInDongScope("contains(., 'Mã số thuế') or contains(., 'MSDN')"),
                data.mst(), "Đồng ND — [Mã số thuế]");
        dienDiaChiDong(data.diaChiTruSo(), data.diaChiTruSo(),
                "contains(., 'Địa chỉ trụ sở')", "contains(., 'Địa chỉ nơi cư trú')");
        if (data.nguoiDaiDien() != null && !data.nguoiDaiDien().isBlank()) {
            webUI.setTextWithCheck(
                    fieldInDongScope("contains(., 'Người đại diện pháp luật') or contains(., 'Người đại diện')"),
                    data.nguoiDaiDien(), "Đồng ND — [Người đại diện]");
        }
        if (data.chucVu() != null && !data.chucVu().isBlank()) {
            webUI.setTextWithCheck(
                    fieldInDongScope("contains(., 'Chức vụ')"),
                    data.chucVu(), "Đồng ND — [Chức vụ]");
        }
        webUI.setTextWithCheck(
                fieldInDongScope("contains(., 'Số điện thoại')"),
                data.sdt(), "Đồng ND — [Số điện thoại]");
        webUI.setTextWithCheck(
                fieldInDongScope("contains(., 'thư điện tử') or contains(., 'Email')"),
                data.email(), "Đồng ND — [Email]");
    }

    /** Gọi sau sync catalog (đã bấm Thêm để đọc label) — gỡ card trống trước khi rời form. */
    public void dongBoSauScrapeDongNguyenDon() {
        xoaDongNguyenDonTrongNeuCo();
    }

    private boolean coKhoiDongNguyenDonTrenForm() {
        return webUI.existsNow(BTN_THEM_TRONG_KHOI_DONG) || webUI.existsNow(BTN_THEM_DONG_NGUYEN_DON);
    }

    private By nutThemDongNguyenDon() {
        return webUI.existsNow(BTN_THEM_TRONG_KHOI_DONG) ? BTN_THEM_TRONG_KHOI_DONG : BTN_THEM_DONG_NGUYEN_DON;
    }

    private void clickThemDongNguyenDonNeuCan() {
        By coPlaintiffInBlock = By.xpath(
                DONG_NGUYEN_DON_BLOCK + "//label[contains(., 'Ngày, tháng, năm sinh')"
                        + " or contains(., 'Số CCCD / Hộ chiếu') or contains(., 'Số CCCD')"
                        + " or contains(., 'Địa chỉ nơi cư trú') or contains(., 'Tên tổ chức')]");
        if (webUI.existsNow(coPlaintiffInBlock)) {
            return;
        }
        By btn = nutThemDongNguyenDon();
        if (!webUI.existsNow(btn)) {
            return;
        }
        webUI.clickElement(btn, "Nút [Thêm nguyên đơn]");
        TestActionLog.chon("Đồng nguyên đơn", "Có (đã bấm Thêm)");
    }

    /**
     * Xóa card đồng nguyên đơn trống (sau scrape catalog hoặc lỡ bấm Thêm).
     */
    private void xoaDongNguyenDonTrongNeuCo() {
        By coPlaintiffInBlock = By.xpath(
                DONG_NGUYEN_DON_BLOCK + "//label[contains(., 'Ngày, tháng, năm sinh')"
                        + " or contains(., 'Số CCCD / Hộ chiếu') or contains(., 'Số CCCD')"
                        + " or contains(., 'Địa chỉ nơi cư trú') or contains(., 'Tên tổ chức')]");
        if (!webUI.existsNow(coPlaintiffInBlock)) {
            return;
        }
        By trashInDong = By.xpath(
                "(" + DONG_NGUYEN_DON_BLOCK + "//button[.//svg[contains(@class,'lucide-trash')]])[last()]");
        if (webUI.existsNow(trashInDong)) {
            webUI.clickElement(trashInDong, "Xóa đồng nguyên đơn trống");
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        }
    }

    /** Kiểm tra nhanh địa chỉ trụ sở + nơi cư trú đại diện trước Tiếp theo. */
    public void chuanBiDiaChiToChucTruocTiepTheo(String diaChiTruSo) {
        if (!webUI.isAdministrativeAddressBlockComplete(MAIN_SECTION, 0)) {
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 0, diaChiTruSo, "Trụ sở");
        }
        if (webUI.countVisibleAddressBlocks(MAIN_SECTION) >= 2
                && !webUI.isAdministrativeAddressBlockComplete(MAIN_SECTION, 1)) {
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 1, diaChiTruSo, "Nơi cư trú");
        }
    }

    public void clickTiepTheo() {
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước Nguyên Đơn");
    }

    /** Sửa tỉnh/phường + chi tiết khi VNeID prefill lệch hoặc thiếu. */
    public void damBaoDiaChiNguyenDon(String thuongTru, String lienLac) {
        damBaoDiaChiNguyenDonDayDu(thuongTru, lienLac);
    }

    /**
     * Luôn nhập tay đủ địa chỉ (tỉnh → phường → chi tiết) — không tin prefill VNeID.
     * Gọi lại trước Tiếp theo nếu checkbox định danh làm form thay đổi async.
     */
    public void damBaoDiaChiNguyenDonDayDu(String thuongTru, String lienLac) {
        boolean giongThuongTru = isGiongThuongTru(lienLac, thuongTru);
        chonDiaChiLienLacGiongThuongTru(giongThuongTru);
        webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 0, thuongTru, "Thường trú");
        webUI.forceSelectAdministrativeWardsInScope(MAIN_SECTION, 0);
        if (!giongThuongTru) {
            int blocks = webUI.countVisibleAddressBlocks(MAIN_SECTION);
            if (blocks >= 2) {
                webUI.dismissOpenDropdowns();
                webUI.sleepMillis(WaitConfig.ADDRESS_BLOCK_GAP_MS);
                webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 1, lienLac, "Liên lạc");
                webUI.forceSelectAdministrativeWardsInScope(MAIN_SECTION, 1);
            } else if (webUI.existsNow(txtDiaChiLienLac) && webUI.isElementEnabledNow(txtDiaChiLienLac)) {
                webUI.setTextWithCheck(txtDiaChiLienLac, lienLac, "Ô nhập [Địa chỉ liên lạc]");
            }
        }
    }

    /**
     * Sau tick VNeID: chỉ điền lại ô lệch / thiếu — không xóa-gõ lại ô đã đúng.
     * Địa chỉ chỉ bổ sung khối còn thiếu (không force chọn lại tỉnh/phường đã đủ).
     */
    public void dienDayDuCaNhanTruocTiepTheo(String hoTen, String ngaySinh, String gioiTinh,
                                            String cccd, String ngayCap, String noiCap,
                                            String thuongTru, String lienLac, String sdt, String email) {
        System.out.println(" ℹ Bước 2 — kiểm tra lại sau VNeID (chỉ sửa ô lệch/thiếu)...");
        dienThongTinCaNhan(hoTen, ngaySinh, gioiTinh, cccd, ngayCap, noiCap);
        chuanBiDiaChiTruocTiepTheo(thuongTru, lienLac);
        webUI.setTextWithCheck(txtSoDienThoai, sdt, "Ô nhập [Số điện thoại]");
        webUI.setTextWithCheck(txtEmail, email, "Ô nhập [Email]");
    }

    /** Kiểm tra lại nguyên đơn tổ chức — skip ô/dropdown đã đúng. */
    public void dienDayDuToChucTruocTiepTheo(String tenToChuc, String loaiHinh, String mst, String diaChi,
                                             String nguoiDaiDien, String chucVu, String sdt, String email,
                                             String repNgaySinh, String repGioiTinh, String repCccd,
                                             String repNgayCap, String noiCap) {
        System.out.println(" ℹ Bước 2 — kiểm tra lại tổ chức (chỉ sửa ô lệch/thiếu)...");
        dienThongTinToChuc(tenToChuc, loaiHinh, mst, diaChi, nguoiDaiDien, chucVu, sdt, email,
                repNgaySinh, repGioiTinh, repCccd, repNgayCap, noiCap);
        if (!webUI.isAdministrativeAddressBlockComplete(MAIN_SECTION, 0)) {
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 0, diaChi, "Trụ sở");
        }
        int blocks = webUI.countVisibleAddressBlocks(MAIN_SECTION);
        if (blocks >= 2 && !webUI.isAdministrativeAddressBlockComplete(MAIN_SECTION, 1)) {
            webUI.dismissOpenDropdowns();
            webUI.sleepMillis(WaitConfig.ADDRESS_BLOCK_GAP_MS);
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 1, diaChi, "Nơi cư trú");
        }
    }

    /** Trụ sở (#1) + nơi cư trú đại diện (#2) — tỉnh → phường → chi tiết. */
    public void damBaoDiaChiToChucDayDu(String diaChiTruSo) {
        webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 0, diaChiTruSo, "Trụ sở");
        int blocks = webUI.countVisibleAddressBlocks(MAIN_SECTION);
        if (blocks >= 2) {
            webUI.dismissOpenDropdowns();
            webUI.sleepMillis(WaitConfig.ADDRESS_BLOCK_GAP_MS);
            webUI.ensureAdministrativeAddressBlockInScope(MAIN_SECTION, 1, diaChiTruSo, "Nơi cư trú");
        }
        webUI.forceSelectAdministrativeWardsInScope(MAIN_SECTION);
    }

    /** @deprecated dùng {@link #damBaoDiaChiNguyenDon(String, String)} */
    public void damBaoPhuongXaDaChon() {
        webUI.forceSelectAdministrativeWardsInScope(MAIN_SECTION);
    }
}

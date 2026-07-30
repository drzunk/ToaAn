package vn.tuphap.automation.pages;

import vn.tuphap.automation.data.BiDonData;

import vn.tuphap.automation.data.TaoDonScenario;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.UiSynonyms;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

public class BiDonPage {
    private WebDriver driver;
    private WebUI webUI;

    /**
     * Khối bước 3 chứa card bị đơn / người bị kiện và nút Thêm.
     * Không phụ thuộc tab Cá nhân (Hành chính không có tab đó).
     */
    public static final String BIDON_SECTION =
            "//button[(contains(@class,'border-dashed') or .//svg[contains(@class,'lucide-plus')])"
                    + " and (" + UiSynonyms.containsAnyDot(UiSynonyms.THEM_BI_DON) + ")]"
                    + "/ancestor::div[.//span[starts-with(normalize-space(.), 'Bị đơn ')"
                    + " or starts-with(normalize-space(.), 'Người bị kiện ')"
                    + " or starts-with(normalize-space(.), 'Người được yêu cầu ')"
                    + " or starts-with(normalize-space(.), 'Người bị yêu cầu ')]"
                    + " or .//div[starts-with(normalize-space(.), 'Bị đơn ')"
                    + " or starts-with(normalize-space(.), 'Người bị kiện ')"
                    + " or starts-with(normalize-space(.), 'Người được yêu cầu ')"
                    + " or starts-with(normalize-space(.), 'Người bị yêu cầu ')]"
                    + " or .//div[contains(@class,'cursor-pointer') and contains(., 'Cá nhân')]"
                    + " or .//label[contains(., 'Tên cơ quan')]][1]";

    private static final String CARD_CA_NHAN =
            "//div[contains(@class, 'border') and contains(@class, 'rounded')"
                    + " and .//div[contains(@class, 'cursor-pointer') and contains(., 'Cá nhân')"
                    + " and not(contains(., 'Tổ chức'))]"
                    + " and not(ancestor::*[contains(., 'Nguyên đơn')])"
                    // Chỉ card ngoài cùng — tránh đếm wrapper lồng nhau thành 2 slot ảo
                    + " and not(ancestor::div[contains(@class,'border') and contains(@class,'rounded')"
                    + " and .//div[contains(@class,'cursor-pointer') and contains(., 'Cá nhân')"
                    + " and not(contains(., 'Tổ chức'))]])]";

    /** Nhãn tên cơ quan — UI Hành chính có thể đổi wording. */
    private static final String CO_QUAN_LABEL_MATCH =
            "contains(., 'Tên cơ quan') or contains(., 'Cơ quan bị kiện')"
                    + " or (contains(., 'Tên') and contains(., 'cơ quan'))";

    private static final String CARD_TEN_CO_QUAN =
            "//div[contains(@class, 'border') and contains(@class, 'rounded')"
                    + " and .//label[" + CO_QUAN_LABEL_MATCH + "]"
                    + " and not(ancestor::div[contains(@class,'border') and contains(@class,'rounded')"
                    + " and .//label[" + CO_QUAN_LABEL_MATCH + "]])]";

    /** Badge đúng số {@code index} — khớp chính xác hoặc prefix + khoảng trắng (không dùng contains). */
    private static String slotBadgeXpath(int index) {
        String[] prefixes = UiSynonyms.SLOT_BADGE_PREFIXES;
        StringBuilder sb = new StringBuilder(
                "//*[self::span or self::div][string-length(normalize-space(.)) < 48 and (");
        for (int i = 0; i < prefixes.length; i++) {
            if (i > 0) {
                sb.append(" or ");
            }
            String exact = prefixes[i] + index;
            sb.append("normalize-space(.) = ").append(UiSynonyms.xpathLiteral(exact))
                    .append(" or starts-with(normalize-space(.), ")
                    .append(UiSynonyms.xpathLiteral(exact + " "))
                    .append(")");
        }
        sb.append(")]");
        return sb.toString();
    }

    // Hôn nhân bước 3 — 2 UI khác nhau tùy loại việc:
    // A) "Người bị yêu cầu …" + nút Thêm (Ly hôn đơn phương, …)
    // B) "Người yêu cầu 2 (vợ / chồng)" — không có nút Thêm (Thuận tình ly hôn, …)
    private static final By BTN_THEM_NGUOI_BI_YEU_CAU =
            By.xpath("//button[contains(normalize-space(.), 'Thêm người bị yêu cầu')]");
    /**
     * Markup demo: button border-dashed + svg.lucide-plus + text Thêm bị đơn / biến thể.
     */
    public static final By BTN_THEM_BI_DON = By.xpath(
            "//button[(contains(@class,'border-dashed') or .//svg[contains(@class,'lucide-plus')])"
                    + " and (" + UiSynonyms.containsAnyDot(UiSynonyms.THEM_BI_DON) + ")]");
    private static final By HON_NHAN_H2_ANY =
            By.xpath("//h2[contains(., 'Người bị yêu cầu') or contains(., 'Người yêu cầu 2') or contains(., 'vợ / chồng')]");
    private static final By HON_NHAN_VO_CHONG_BADGE =
            By.xpath("//span[contains(normalize-space(.), 'Người yêu cầu 2')]");
    private static final String HON_NHAN_SECTION_BI_YEU_CAU =
            "//button[contains(normalize-space(.), 'Thêm người bị yêu cầu')]/parent::div";
    private static final String HON_NHAN_SECTION_VO_CHONG =
            "//h2[contains(., 'Người yêu cầu 2') or contains(., 'vợ / chồng')]/parent::div";

    private boolean isHonNhanVoChongUi() {
        return webUI.existsNow(HON_NHAN_VO_CHONG_BADGE);
    }

    private String honNhanSectionScope() {
        return isHonNhanVoChongUi() ? HON_NHAN_SECTION_VO_CHONG : HON_NHAN_SECTION_BI_YEU_CAU;
    }

    private String honNhanCard(int index) {
        if (isHonNhanVoChongUi()) {
            return "//span[contains(normalize-space(.), 'Người yêu cầu 2')]"
                    + "/ancestor::div[contains(@class, 'border') and contains(@class, 'rounded')][1]";
        }
        return "//*[self::span or self::div][contains(., 'Người bị yêu cầu " + index + "')"
                + " or contains(., 'Người yêu cầu " + index + "')]"
                + "/ancestor::div[contains(@class, 'border') and contains(@class, 'rounded')][1]";
    }

    /**
     * Card bị đơn / người bị kiện thứ {@code index} (1-based).
     * Ưu tiên badge đúng số. Với index&gt;1 không fallback card giả (tránh trúng wrapper card 1).
     */
    private String biDonCard(int index) {
        String byBadge = "(" + slotBadgeXpath(index)
                + "/ancestor::div[contains(@class,'border') and contains(@class,'rounded')])[1]";
        if (webUI.existsNow(By.xpath(byBadge))) {
            return byBadge;
        }
        if (index <= 1) {
            // Hành chính: card cơ quan bị kiện — ưu tiên trước CARD_CA_NHAN (wrapper tab Cá nhân hay false-positive).
            String byCoQuan = "(" + CARD_TEN_CO_QUAN + ")[1]";
            if (webUI.existsNow(By.xpath(byCoQuan))) {
                return byCoQuan;
            }
            String byCaNhan = "(" + CARD_CA_NHAN + ")[1]";
            if (webUI.existsNow(By.xpath(byCaNhan))) {
                return byCaNhan;
            }
            return byCaNhan;
        }
        // Chờ badge #N — không dùng CARD[N] (dễ false-positive do div lồng nhau)
        return byBadge;
    }

    /** Scope ô nhập theo đúng card — tránh (//label)[2] lấy nhầm field ngoài bị đơn. */
    private String formScope(String loaiDon, int index) {
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            return honNhanCard(index);
        }
        if (DataDictionary.isPhaSan(loaiDon)) {
            // Một form DN/HTX — không dùng "|" ở đây (sẽ gãy khi nối //label... thành xpath union).
            return "//h2[contains(., 'Doanh nghiệp') and contains(., 'phá sản')]"
                    + "/ancestor::div[.//label[contains(., 'Tên tổ chức')]][1]";
        }
        return biDonCard(index);
    }

    /** Bọc scope có "|" để nối //label không làm hỏng thứ tự ưu tiên XPath. */
    private String scoped(String loaiDon, int index) {
        String scope = formScope(loaiDon, index);
        if (scope == null || scope.isBlank()) {
            return "";
        }
        if (scope.contains("|")) {
            return "(" + scope + ")";
        }
        return scope;
    }

    private By inputInScope(String loaiDon, int index, String labelMatch) {
        String scope = scoped(loaiDon, index);
        return By.xpath(scope + "//label[" + labelMatch + "]/parent::div//input"
                + " | " + scope + "//label[" + labelMatch + "]/following-sibling::input"
                + " | " + scope + "//label[" + labelMatch + "]/following-sibling::div//input"
                + " | " + scope + "//label[" + labelMatch + "]/parent::div//textarea"
                + " | " + scope + "//label[" + labelMatch + "]/following-sibling::textarea");
    }

    private String honNhanWhoLabel(int index) {
        return isHonNhanVoChongUi() ? "Người yêu cầu 2" : "Người bị yêu cầu " + index;
    }

    private By honNhanCardBadge(int index) {
        if (isHonNhanVoChongUi()) {
            return HON_NHAN_VO_CHONG_BADGE;
        }
        return By.xpath("//*[self::span or self::div][contains(., 'Người bị yêu cầu " + index + "')"
                + " or contains(., 'Người yêu cầu " + index + "')]");
    }

    private int countHonNhanCards() {
        if (isHonNhanVoChongUi()) {
            return webUI.existsNow(HON_NHAN_VO_CHONG_BADGE) ? 1 : 0;
        }
        // Khớp badge của ensure/honNhanCardBadge — cả "Người bị yêu cầu" và "Người yêu cầu N"
        return webUI.countNow(By.xpath(
                honNhanSectionScope()
                        + "//div[contains(@class, 'border') and contains(@class, 'rounded')"
                        + " and .//*[self::span or self::div]["
                        + "contains(., 'Người bị yêu cầu') or ("
                        + "contains(., 'Người yêu cầu') and not(contains(., 'Người yêu cầu 2')))]]"));
    }

    private void waitHonNhanStepReady() {
        int maxSec = WaitConfig.HON_NHAN;
        System.out.println(" ⏳ Chờ bước 3 [Hôn nhân và gia đình]...");
        long deadline = System.currentTimeMillis() + maxSec * 1000L;
        int lastLogged = -1;
        while (System.currentTimeMillis() < deadline) {
            if (webUI.existsNow(BTN_THEM_NGUOI_BI_YEU_CAU)
                    || webUI.existsNow(HON_NHAN_H2_ANY)
                    || webUI.existsNow(HON_NHAN_VO_CHONG_BADGE)
                    || webUI.existsNow(honNhanCardBadge(1))
                    || webUI.existsNow(By.xpath(honNhanCard(1) + "//label[contains(., 'Năm sinh') or contains(., 'Tên tổ chức')]"))) {
                int elapsed = (int) ((maxSec * 1000L - (deadline - System.currentTimeMillis()) + 999) / 1000);
                String ui = isHonNhanVoChongUi() ? "Người yêu cầu 2 (vợ/chồng)" : "Người bị yêu cầu";
                System.out.println(" ✅ Bước 3 Hôn nhân sẵn sàng — giao diện: " + ui + " (" + Math.max(1, elapsed) + "s).");
                return;
            }
            int elapsed = (int) ((maxSec * 1000L - (deadline - System.currentTimeMillis()) + 999) / 1000);
            elapsed = Math.min(Math.max(elapsed, 1), maxSec);
            if (elapsed != lastLogged && (elapsed == 1 || elapsed % 4 == 0)) {
                lastLogged = elapsed;
                System.out.println(" ⏳ Chờ bước 3 Hôn nhân... (" + elapsed + "/" + maxSec + "s)");
            }
            webUI.sleepMillis(250);
        }
        throw new RuntimeException(
                "❌ Hết thời gian chờ: Không thấy bước 3 Hôn nhân sau " + maxSec + "s. "
                        + "Có thể bước Nguyên đơn chưa chuyển trang.");
    }

    private void ensureHonNhanCard(int index) {
        waitHonNhanStepReady();
        if (isHonNhanVoChongUi()) {
            return;
        }
        if (webUI.existsNow(honNhanCardBadge(index))) {
            return;
        }
        if (!webUI.existsNow(BTN_THEM_NGUOI_BI_YEU_CAU)) {
            throw new RuntimeException("❌ Lỗi: Không thấy card [" + honNhanWhoLabel(index)
                    + "] và cũng không có nút [Thêm người bị yêu cầu].");
        }
        for (int next = Math.max(countHonNhanCards(), 1) + 1; next <= index; next++) {
            if (webUI.existsNow(honNhanCardBadge(next))) {
                continue;
            }
            int before = countHonNhanCards();
            boolean added = clickThemHonNhanVaCho(next, before);
            if (!added) {
                webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
                added = clickThemHonNhanVaCho(next, countHonNhanCards());
            }
            if (!added) {
                throw new RuntimeException(
                        "❌ Đã bấm [Thêm người bị yêu cầu] nhưng thẻ #" + next
                                + " không xuất hiện (trước=" + before
                                + ", sau=" + countHonNhanCards() + ").");
            }
        }
    }

    private boolean clickThemHonNhanVaCho(int next, int before) {
        webUI.scrollToElement(BTN_THEM_NGUOI_BI_YEU_CAU);
        webUI.clickElement(BTN_THEM_NGUOI_BI_YEU_CAU, "Nút [Thêm người bị yêu cầu] → #" + next);
        long deadline = System.currentTimeMillis() + WaitConfig.FORM * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (countHonNhanCards() > before || webUI.existsNow(honNhanCardBadge(next))) {
                System.out.println(" ✅ Đã thêm " + honNhanWhoLabel(next)
                        + " (cards=" + Math.max(countHonNhanCards(), next) + ").");
                return true;
            }
            webUI.sleepMillis(250);
        }
        return false;
    }

    private By honNhanTab(int index, String option) {
        return By.xpath(honNhanCard(index)
                + "//div[contains(@class, 'inline-flex') and contains(@class, 'overflow-hidden')]"
                + "//div[contains(@class, 'cursor-pointer') and normalize-space(.)='" + option + "']");
    }

    private By honNhanCaNhanFormMarker(int index) {
        return By.xpath(honNhanCard(index) + "//label[contains(., 'Năm sinh')]");
    }

    private By honNhanToChucFormMarker(int index) {
        return By.xpath(honNhanCard(index) + "//label[contains(., 'Tên tổ chức')]");
    }

    private void waitHonNhanForm(int index, String option) {
        By marker = "Cá nhân".equals(option) ? honNhanCaNhanFormMarker(index) : honNhanToChucFormMarker(index);
        webUI.waitUntilExists(marker, WaitConfig.FORM, "Biểu mẫu " + honNhanWhoLabel(index) + " [" + option + "]");
    }

    private void chonLoaiHonNhan(int index, String loaiBiDon) {
        System.out.println(" ⏳ Xử lý Hôn nhân — " + honNhanWhoLabel(index) + ", loại: " + loaiBiDon);
        ensureHonNhanCard(index);
        String option = honNhanOption(loaiBiDon);
        String who = honNhanWhoLabel(index);

        if ("Cá nhân".equals(option) && webUI.existsNow(honNhanCaNhanFormMarker(index))) {
            System.out.println(" ⏩ Biểu mẫu Cá nhân [" + who + "] đã hiển thị — không cần click tab.");
            return;
        }
        if ("Tổ chức".equals(option) && webUI.existsNow(honNhanToChucFormMarker(index))) {
            System.out.println(" ⏩ Biểu mẫu Tổ chức [" + who + "] đã hiển thị — không cần click tab.");
            return;
        }

        webUI.clickElement(honNhanTab(index, option), who + ": [" + loaiBiDon + "]");
        waitHonNhanForm(index, option);
    }

    private static final By LIST_OPTIONS_LOAI_HINH =
            By.xpath("//div[@role='listbox']//div[@role='option']");

    private String honNhanOption(String loaiBiDon) {
        String tuKhoa = loaiBiDon.trim().toLowerCase();
        return (tuKhoa.contains("tổ chức") || tuKhoa.contains("doanh nghiệp")) ? "Tổ chức" : "Cá nhân";
    }

    private By getLoaiBiDon(int index, String loai, String loaiDon) {
        String tuKhoa = loai.trim().toLowerCase();
        boolean isToChuc = tuKhoa.contains("tổ chức") || tuKhoa.contains("doanh nghiệp");
        // Ưu tiên tab trong đúng card; fallback index toàn cục (loại trừ khối Nguyên đơn)
        String inCard = biDonCard(index)
                + "//div[contains(@class, 'cursor-pointer') and ";
        if (isToChuc) {
            By scoped = By.xpath(inCard
                    + "(contains(normalize-space(.), 'Tổ chức') or contains(normalize-space(.), 'Doanh nghiệp'))"
                    + " and not(contains(normalize-space(.), 'Cá nhân'))]");
            if (webUI.existsNow(scoped)) {
                return scoped;
            }
            return By.xpath("(//div[contains(@class, 'cursor-pointer')"
                    + " and (contains(normalize-space(.), 'Tổ chức') or contains(normalize-space(.), 'Doanh nghiệp'))"
                    + " and not(contains(normalize-space(.), 'Cá nhân'))"
                    + " and not(ancestor::*[contains(., 'Nguyên đơn')])])[" + index + "]");
        }
        By scopedCaNhan = By.xpath(inCard
                + "contains(normalize-space(.), 'Cá nhân') and not(contains(., 'Tổ chức'))]");
        if (webUI.existsNow(scopedCaNhan)) {
            return scopedCaNhan;
        }
        return By.xpath("(//div[contains(@class, 'cursor-pointer')"
                + " and contains(normalize-space(.), 'Cá nhân') and not(contains(., 'Tổ chức'))"
                + " and not(ancestor::*[contains(., 'Nguyên đơn')])])[" + index + "]");
    }

    private By getTxtHoTen(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Họ và tên')");
    }

    private By getTxtNamSinh(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Năm sinh')");
    }

    private By getTxtNgaySinh(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Ngày sinh') or contains(., 'Ngày, tháng, năm sinh')");
    }

    private static String ngaySinhTuNamSinh(String namSinh) {
        if (namSinh != null && namSinh.trim().matches("\\d{4}")) {
            return "01/01/" + namSinh.trim();
        }
        return "01/01/1990";
    }

    private void dienSinhBiDonCaNhan(int index, String loaiDon, String namSinh, String who) {
        By txtNamSinh = getTxtNamSinh(index, loaiDon);
        By txtNgaySinh = getTxtNgaySinh(index, loaiDon);
        if (webUI.existsNow(txtNamSinh)) {
            webUI.setTextWithCheck(txtNamSinh, namSinh, "Ô nhập [Năm sinh] (" + who + ")");
        } else if (webUI.existsNow(txtNgaySinh)) {
            webUI.setTextForMaskedInput(txtNgaySinh, ngaySinhTuNamSinh(namSinh),
                    "Ô nhập [Ngày sinh] (" + who + ")");
        } else {
            System.out.println(" ⏩ Bỏ qua [Năm/Ngày sinh] (" + who + ") — không có trên biểu mẫu.");
        }
    }

    private void chonGioiTinhBiDon(int index, String loaiDon, String gioiTinh, String who) {
        String scope = scoped(loaiDon, index);
        if (scope.isBlank()) {
            return;
        }
        String tuKhoa = gioiTinh == null || gioiTinh.isBlank() ? "nam" : gioiTinh.trim().toLowerCase();
        String value = tuKhoa.contains("nữ") || tuKhoa.contains("nu") ? "Nữ"
                : (tuKhoa.contains("khác") || tuKhoa.contains("khac") ? "Khác" : "Nam");
        By by = By.xpath(scope
                + "//label[contains(., 'Giới tính')]/following-sibling::div//div[contains(text(), '" + value + "')]"
                + " | " + scope
                + "//label[contains(., 'Giới tính')]/following-sibling::div//div[contains(@class,'cursor-pointer') and contains(., '"
                + value + "')]");
        if (webUI.existsNow(by)) {
            webUI.clickElement(by, "Giới tính (" + who + "): [" + value + "]");
        }
    }

    private By getTxtDiaChiCaNhan(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Địa chỉ') and not(contains(., 'trụ sở'))");
    }

    private By getTxtTenToChuc(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Tên tổ chức')");
    }

    private By getBtnLoaiHinhToChuc(int index, String loaiDon) {
        String scope = scoped(loaiDon, index);
        return By.xpath(scope + "//label[contains(., 'Loại hình')]/parent::div//button"
                + " | " + scope + "//label[contains(., 'Loại hình')]/following-sibling::div//button");
    }

    private By getTxtMaSoThue(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Mã số thuế')");
    }

    private By getTxtDiaChiTruSo(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Địa chỉ trụ sở')");
    }

    private By getTxtNguoiDaiDien(int index, String loaiDon) {
        String scope = scoped(loaiDon, index);
        return By.xpath(scope + "//label[normalize-space(.)='Người đại diện' or contains(., 'Người đại diện')]"
                + "/parent::div//input"
                + " | " + scope + "//label[contains(., 'Người đại diện')]/following-sibling::input");
    }

    private By getTxtTenCoQuan(int index) {
        String scope = "(" + CARD_TEN_CO_QUAN + ")[" + index + "]";
        return By.xpath(scope + "//label[" + CO_QUAN_LABEL_MATCH + "]/parent::div//input"
                + " | " + scope + "//label[" + CO_QUAN_LABEL_MATCH + "]/following-sibling::input"
                + " | //label[" + CO_QUAN_LABEL_MATCH + "]/parent::div//input"
                + " | //label[" + CO_QUAN_LABEL_MATCH + "]/following-sibling::input");
    }

    private By getTxtChucDanh(int index) {
        String scope = biDonCard(index);
        return By.xpath(scope + "//label[contains(., 'Chức danh')]/parent::div//input"
                + " | " + scope + "//label[contains(., 'Chức danh')]/following-sibling::input");
    }

    private By getTxtNguoiCoThamQuyen(int index) {
        String scope = biDonCard(index);
        return By.xpath(scope + "//label[contains(., 'Người có thẩm quyền')]/parent::div//input"
                + " | " + scope + "//label[contains(., 'Người có thẩm quyền')]/following-sibling::input");
    }

    private By getTxtSoDienThoai(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Số điện thoại')");
    }

    private By getTxtCCCD(int index, String loaiDon) {
        return inputInScope(loaiDon, index,
                "contains(., 'Số CCCD') or contains(., 'Hộ chiếu') or contains(., 'CMND')");
    }

    private By getTxtNgheNghiep(int index, String loaiDon) {
        return inputInScope(loaiDon, index,
                "contains(., 'Nghề nghiệp') or contains(., 'nơi làm việc')");
    }

    private By getTxtNoiOHienTai(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Nơi ở hiện tại')");
    }

    private By getTxtChucVu(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Chức vụ')");
    }

    private By getTxtEmail(int index, String loaiDon) {
        return inputInScope(loaiDon, index,
                "contains(., 'Email') or contains(., 'thư điện tử') or contains(., 'Thư điện tử')");
    }

    private By getBtnXoaBiDon(int index) {
        return By.xpath("(//button[.//svg[contains(@class, 'lucide-trash')]])[" + index + "]");
    }

    private By getToggleNguoiLienQuan(String luaChon, String loaiDon) {
        String value = luaChon.trim().toLowerCase().equals("có") ? "Có" : "Không";
        return By.xpath(nguoiLienQuanToggleRow(loaiDon)
                + "//div[contains(@class, 'inline-flex')]//div[normalize-space(.)='" + value + "']");
    }

    /** Hàng toggle NLQ — neo chặt, tránh //*[text] khớp body/main. */
    private String nguoiLienQuanToggleRow(String loaiDon) {
        return "(" + getNguoiLienQuanToggleScope(loaiDon)
                + "//div[(contains(., 'Người có quyền lợi') and contains(., 'liên quan'))"
                + " or contains(., 'Người liên quan')]"
                + "/ancestor::div[contains(@class, 'justify-between')][1])[1]";
    }

    /** Ô nhập sau hàng toggle NLQ (form hiện khi chọn Có). */
    private By getTxtHoTenNguoiLienQuan(String loaiDon) {
        String after = nguoiLienQuanToggleRow(loaiDon) + "/following::";
        return By.xpath("("
                + after + "label[contains(., 'Họ tên người liên quan')"
                + " or (contains(., 'liên quan') and (" + UiSynonyms.containsAnyDot(UiSynonyms.HO_TEN) + "))]"
                + "[1]/parent::div//input"
                + " | " + after + "label[" + UiSynonyms.containsAnyDot(UiSynonyms.HO_TEN) + "][1]/parent::div//input"
                + " | " + after + "label[contains(., 'Tên tổ chức')][1]/parent::div//input"
                + " | " + after + "input[(contains(@placeholder,'Họ') or contains(@placeholder,'tên')"
                + " or contains(@placeholder,'Tên'))][1]"
                + ")[1]");
    }

    private By getTxtLyDoLienQuan(String loaiDon) {
        String after = nguoiLienQuanToggleRow(loaiDon) + "/following::";
        // Nhãn UAT: "Mối quan hệ / Lý do liên quan" — có thể là textarea hoặc input.
        // Không dùng following::textarea[1] (dễ trúng Chi tiết địa chỉ NLQ).
        String label = "label[contains(., 'Mối quan hệ') or contains(., 'Lý do liên quan')"
                + " or (contains(., 'Lý do') and contains(., 'liên quan'))]";
        return By.xpath("("
                + after + label + "[1]/parent::div//textarea"
                + " | " + after + label + "[1]/parent::div//input[not(@type='hidden')]"
                + " | " + after + label + "[1]/following-sibling::textarea"
                + " | " + after + label + "[1]/following-sibling::input[not(@type='hidden')]"
                + " | " + after + label + "[1]/following-sibling::div//textarea"
                + " | " + after + label + "[1]/following-sibling::div//input[not(@type='hidden')]"
                + ")[1]");
    }

    private By getTxtLienLacNguoiLienQuan(String loaiDon) {
        String after = nguoiLienQuanToggleRow(loaiDon) + "/following::";
        return By.xpath("("
                + after + "label[contains(., 'Thông tin liên lạc')]/parent::div//input"
                + ")[1]");
    }

    private final By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    public BiDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void clickXoaBiDon(int index) {
        webUI.clickElement(getBtnXoaBiDon(index), "Nút [Xóa Bị Đơn " + index + "]");
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
    }

    /** UI có nút thêm bị đơn / người bị yêu cầu (không áp dụng Thuận tình ly hôn / phá sản 1 bên). */
    public boolean coTheThemBiDon(String loaiDon) {
        if (DataDictionary.isPhaSan(loaiDon)) {
            return false;
        }
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            waitHonNhanStepReady();
            return !isHonNhanVoChongUi() && webUI.existsNow(BTN_THEM_NGUOI_BI_YEU_CAU);
        }
        return webUI.existsNow(BTN_THEM_BI_DON);
    }

    /**
     * Đảm bảo có đủ slot bị đơn tới {@code index} (1-based) bằng nút Thêm nếu cần.
     * Chỉ tin badge đúng số (hoặc card Tên cơ quan ngoài cùng với Hành chính) — không dùng
     * CARD_CA_NHAN[N] vì wrapper lồng nhau dễ tạo false-positive → bỏ qua Thêm → fail chọn loại #N.
     */
    public void ensureBiDonSlot(int index, String loaiDon) {
        if (index <= 1) {
            if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
                ensureHonNhanCard(1);
            }
            return;
        }
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            ensureHonNhanCard(index);
            return;
        }
        if (DataDictionary.isPhaSan(loaiDon)) {
            return;
        }
        if (hasConfirmedSlot(index, loaiDon)) {
            return;
        }

        webUI.waitUntilVisible(BTN_THEM_BI_DON, WaitConfig.STEP, "Nút [Thêm bị đơn]");
        // Chỉ tiến theo hasConfirmedSlot(N) — không tin count() xpath badge chung
        // (span+div cùng "Bị đơn 1" dễ đếm 2 trong khi chưa có slot 2).
        int current = 1;
        while (current < index && hasConfirmedSlot(current + 1, loaiDon)) {
            current++;
        }

        for (int next = current + 1; next <= index; next++) {
            if (hasConfirmedSlot(next, loaiDon)) {
                continue;
            }
            boolean added = clickThemVaChoSlot(next, loaiDon);
            if (!added) {
                webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
                added = clickThemVaChoSlot(next, loaiDon);
            }
            if (!added) {
                throw new RuntimeException(
                        "❌ Đã bấm [Thêm bị đơn] nhưng không thấy ô #" + next
                                + " (huy hiệu/thẻ xác nhận). số ô=" + countConfirmedSlots(loaiDon)
                                + ", loại đơn=" + loaiDon + ").");
            }
        }
    }

    /** Click nút Thêm rồi chờ badge/card {@code next} xuất hiện thật. */
    private boolean clickThemVaChoSlot(int next, String loaiDon) {
        webUI.scrollToElement(BTN_THEM_BI_DON);
        webUI.clickElement(BTN_THEM_BI_DON, "Nút [Thêm bị đơn] → #" + next);
        long deadline = System.currentTimeMillis() + WaitConfig.FORM * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasConfirmedSlot(next, loaiDon)) {
                System.out.println(" ✅ Đã thêm bị đơn #" + next
                        + " (slots=" + countConfirmedSlots(loaiDon) + ").");
                return true;
            }
            webUI.sleepMillis(250);
        }
        return false;
    }

    private boolean hasSlotBadge(int index) {
        return index > 0 && webUI.existsNow(By.xpath(slotBadgeXpath(index)));
    }

    private boolean hasOutermostCoQuanCard(int index) {
        return index > 0 && webUI.existsNow(By.xpath("(" + CARD_TEN_CO_QUAN + ")[" + index + "]"));
    }

    /** Slot N đã được UI tạo thật (không đếm wrapper giả). */
    private boolean hasConfirmedSlot(int index, String loaiDon) {
        if (hasSlotBadge(index)) {
            return true;
        }
        return DataDictionary.isHanhChinh(loaiDon) && hasOutermostCoQuanCard(index);
    }

    /** Đếm theo chỉ số badge/card liên tiếp — không dùng count() trên xpath badge chung. */
    private int countConfirmedSlots(String loaiDon) {
        int n = 0;
        for (int i = 1; i <= 12; i++) {
            if (!hasConfirmedSlot(i, loaiDon)) {
                break;
            }
            n = i;
        }
        if (n > 0) {
            return n;
        }
        // Không có badge: tối thiểu coi như đang có 1 slot đang điền
        if (DataDictionary.isHanhChinh(loaiDon) && webUI.existsNow(By.xpath("(" + CARD_TEN_CO_QUAN + ")[1]"))) {
            return 1;
        }
        return webUI.existsNow(By.xpath("(" + CARD_CA_NHAN + ")[1]")) ? 1 : 0;
    }

    /**
     * @deprecated Dùng {@link #hasConfirmedSlot}/{@link #countConfirmedSlots}.
     */
    private int countBiDonSlots(String loaiDon) {
        return countConfirmedSlots(loaiDon);
    }

    public void chonLoaiBiDon(int index, String loaiBiDon, String loaiDon) {
        if (loaiBiDon == null || loaiBiDon.isEmpty()) {
            return;
        }
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            chonLoaiHonNhan(index, loaiBiDon);
            return;
        }
        if (index > 1 && !hasConfirmedSlot(index, loaiDon)) {
            ensureBiDonSlot(index, loaiDon);
        }
        // Form đúng loại đã hiện trong card này → không click tab (tránh miss locator)
        boolean toChuc = DataDictionary.isToChuc(loaiBiDon);
        By formMarker = toChuc
                ? By.xpath(scoped(loaiDon, index) + "//label[contains(., 'Tên tổ chức') or contains(., 'Mã số thuế')]")
                : By.xpath(scoped(loaiDon, index) + "//label[contains(., 'Họ và tên') or contains(., 'Năm sinh')"
                + " or contains(., 'Ngày, tháng, năm sinh') or contains(., 'Số CCCD')]");
        if (webUI.existsNow(formMarker)) {
            System.out.println(" ⏩ Biểu mẫu bị đơn #" + index + " [" + (toChuc ? "Tổ chức" : "Cá nhân")
                    + "] đã sẵn sàng — bỏ qua click tab.");
            return;
        }
        By tab = getLoaiBiDon(index, loaiBiDon, loaiDon);
        webUI.waitUntilVisible(tab, WaitConfig.FORM, "Loại Bị đơn " + index + ": [" + loaiBiDon + "]");
        webUI.clickElement(tab, "Loại Bị đơn " + index + ": [" + loaiBiDon + "]");
        waitBiDonFormReady(index, loaiDon, loaiBiDon);
    }

    /** Chờ form trong đúng card hiện ra sau khi chọn Cá nhân/Tổ chức. */
    private void waitBiDonFormReady(int index, String loaiDon, String loaiBiDon) {
        boolean toChuc = DataDictionary.isToChuc(loaiBiDon);
        By marker = toChuc
                ? By.xpath(scoped(loaiDon, index) + "//label[contains(., 'Tên tổ chức') or contains(., 'Mã số thuế')]")
                : By.xpath(scoped(loaiDon, index) + "//label[contains(., 'Họ và tên') or contains(., 'Năm sinh')"
                + " or contains(., 'Ngày, tháng, năm sinh') or contains(., 'Số CCCD')]");
        webUI.waitUntilExists(marker, WaitConfig.FORM, "Biểu mẫu bị đơn #" + index + " [" + (toChuc ? "Tổ chức" : "Cá nhân") + "]");
    }

    /** Tỉnh/Phường + chi tiết trong card bị đơn. */
    private void dienDiaChiBiDon(String loaiDon, int index, String diaChi, boolean truSo, String who) {
        if (diaChi == null || diaChi.isBlank()) {
            return;
        }
        String scope = scoped(loaiDon, index);
        if (scope.isBlank()) {
            return;
        }
        webUI.scrollToElement(By.xpath(scope));
        webUI.dismissOpenDropdowns();
        webUI.ensureAdministrativeAddressBlockInScope(scope, 0, diaChi, who);
    }

    /**
     * Bổ sung địa chỉ còn thiếu sau NLQ — chỉ sửa khối chưa đủ (không chọn lại tỉnh/phường đã đúng).
     */
    public void damBaoDiaChiTatCaBiDon(TaoDonScenario s) {
        int total = Math.max(1, s.soLuongBiDon());
        for (int i = 1; i <= total; i++) {
            if (i > 1 && !hasConfirmedSlot(i, s.loaiDon())) {
                continue;
            }
            BiDonData data = (i == 2 && s.biDonThem() != null) ? s.biDonThem() : s.biDonChinh();
            if (data == null) {
                continue;
            }
            String diaChi = DataDictionary.isHanhChinh(s.loaiDon()) ? data.diaChiTruSo()
                    : (DataDictionary.isPhaSan(s.loaiDon()) || DataDictionary.isToChuc(data.loai())
                    ? data.diaChiTruSo() : data.diaChiCaNhan());
            String who = DataDictionary.isPhaSan(s.loaiDon()) ? "Doanh nghiệp bị yêu cầu" : "Bị đơn " + i;
            boSungDiaChiNeuThieu(s.loaiDon(), i, diaChi, who);
            if (!DataDictionary.isToChuc(data.loai()) && !DataDictionary.isHanhChinh(s.loaiDon())
                    && !DataDictionary.isPhaSan(s.loaiDon())
                    && data.noiOHienTai() != null && !data.noiOHienTai().isBlank()) {
                String scope = scoped(s.loaiDon(), i);
                if (webUI.countVisibleAddressBlocks(scope) >= 2
                        && !webUI.isAdministrativeAddressBlockComplete(scope, 1)) {
                    webUI.ensureAdministrativeAddressBlockInScope(scope, 1, data.noiOHienTai(),
                            who + " — Nơi ở hiện tại");
                }
            }
        }
        damBaoDiaChiNguoiLienQuan(s);
    }

    private void boSungDiaChiNeuThieu(String loaiDon, int index, String diaChi, String who) {
        if (diaChi == null || diaChi.isBlank()) {
            return;
        }
        String scope = scoped(loaiDon, index);
        if (scope.isBlank()) {
            return;
        }
        if (webUI.isAdministrativeAddressBlockComplete(scope, 0)) {
            System.out.println(" ⏩ Địa chỉ [" + who + "] đã đủ — bỏ qua chọn lại.");
            return;
        }
        webUI.scrollToElement(By.xpath(scope));
        webUI.dismissOpenDropdowns();
        webUI.ensureAdministrativeAddressBlockInScope(scope, 0, diaChi, who);
    }

    /** Địa chỉ nơi cư trú người liên quan (điền lần đầu / bổ sung nếu còn thiếu). */
    private void damBaoDiaChiNguoiLienQuan(TaoDonScenario s) {
        if (s.coNguoiLienQuan() == null || !s.coNguoiLienQuan().trim().equalsIgnoreCase("có")) {
            return;
        }
        String scope = nguoiLienQuanFormScope(s.loaiDon());
        if (scope.isBlank()) {
            return;
        }
        By tinhLabel = By.xpath(scope + "//label[contains(., 'Tỉnh') and contains(., 'thành phố')]");
        if (!webUI.existsNow(tinhLabel)) {
            return;
        }
        String diaChi = firstNonBlank(
                s.thuongTru(),
                s.diaChiToChuc(),
                s.biDonChinh() != null ? s.biDonChinh().diaChiCaNhan() : null,
                s.biDonChinh() != null ? s.biDonChinh().diaChiTruSo() : null);
        if (diaChi == null || diaChi.isBlank()) {
            return;
        }
        if (webUI.isAdministrativeAddressBlockComplete(scope, 0)) {
            System.out.println(" ⏩ Địa chỉ [Người liên quan] đã đủ — bỏ qua chọn lại.");
            return;
        }
        webUI.scrollToElement(By.xpath(scope));
        webUI.dismissOpenDropdowns();
        webUI.ensureAdministrativeAddressBlockInScope(scope, 0, diaChi, "Người liên quan");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    /**
     * Scope địa chỉ NLQ — chỉ khối của người liên quan, không lấy section Hôn nhân
     * (section đó chứa địa chỉ bị yêu cầu đã điền → false-positive "đã đủ").
     */
    private String nguoiLienQuanFormScope(String loaiDon) {
        String after = nguoiLienQuanToggleRow(loaiDon) + "/following::";

        String byHoTen = "(" + after
                + "label[contains(., 'Họ tên người liên quan')"
                + " or (contains(., 'liên quan') and (" + UiSynonyms.containsAnyDot(UiSynonyms.HO_TEN) + "))"
                + " or " + UiSynonyms.containsAnyDot(UiSynonyms.HO_TEN) + "]"
                + "/ancestor::div[contains(@class,'border') and contains(@class,'rounded')])[1]";
        if (webUI.existsNow(By.xpath(byHoTen + "//label[contains(., 'Tỉnh')]"))) {
            return byHoTen;
        }

        String byCard = "(" + after
                + "div[contains(@class,'border') and contains(@class,'rounded')]"
                + "[(.//label[contains(., 'liên quan') or contains(., 'Lý do') or contains(., 'Mối quan hệ')"
                + " or " + UiSynonyms.containsAnyDot(UiSynonyms.HO_TEN) + "]"
                + " or .//*[contains(., 'Người có quyền lợi')])"
                + " and .//label[contains(., 'Tỉnh') and contains(., 'thành phố')]])[1]";
        if (webUI.existsNow(By.xpath(byCard))) {
            return byCard;
        }

        // Thẻ địa chỉ ngay sau ô NLQ (Địa chỉ nơi cư trú).
        String afterNlq = "(" + after
                + "div[contains(@class,'rounded') and contains(@class,'border')]"
                + "[.//label[contains(., 'Tỉnh') and contains(., 'thành phố')]"
                + " and (.//*[contains(@class,'font-semibold') and ("
                + "contains(normalize-space(.), 'Địa chỉ nơi cư trú')"
                + " or starts-with(normalize-space(.), 'Địa chỉ'))]"
                + " or .//label[contains(., 'Chi tiết') or contains(., 'số nhà')])])[1]";
        if (webUI.existsNow(By.xpath(afterNlq))) {
            return afterNlq;
        }

        return "";
    }

    /** @deprecated dùng {@link #damBaoDiaChiTatCaBiDon} sau khi điền xong bước 3. */
    public void chuanBiDiaChiTruocTiepTheo(TaoDonScenario s) {
        damBaoDiaChiTatCaBiDon(s);
    }

    public void dienThongTinCaNhan(int index, String loaiDon, String hoTen, String cccd, String namSinh,
                                   String diaChi, String sdt, String email) {
        dienThongTinCaNhan(index, loaiDon, hoTen, cccd, namSinh, "Nam", diaChi, diaChi, "", sdt, email);
    }

    public void dienThongTinCaNhan(int index, String loaiDon, String hoTen, String cccd, String namSinh,
                                   String gioiTinh, String diaChi, String noiOHienTai, String ngheNghiep,
                                   String sdt, String email) {
        String who = DataDictionary.isHonNhanGiaDinh(loaiDon) ? honNhanWhoLabel(index) : "Bị đơn " + index;
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            waitHonNhanForm(index, "Cá nhân");
        } else {
            waitBiDonFormReady(index, loaiDon, "Cá nhân");
        }
        webUI.waitUntilVisible(getTxtHoTen(index, loaiDon), WaitConfig.FIELD, "Ô [Họ và tên] (" + who + ")");
        webUI.setText(getTxtHoTen(index, loaiDon), hoTen, "Ô nhập [Họ và tên] (" + who + ")");
        // UI: Họ tên → Ngày/Năm sinh → Giới tính → CCCD → Địa chỉ nơi cư trú → Nơi ở hiện tại → Nghề nghiệp → SĐT → Email
        dienSinhBiDonCaNhan(index, loaiDon, namSinh, who);
        chonGioiTinhBiDon(index, loaiDon, gioiTinh, who);
        webUI.setTextWithCheck(getTxtCCCD(index, loaiDon), cccd, "Ô nhập [Số CCCD / Hộ chiếu] (" + who + ")");
        dienDiaChiBiDon(loaiDon, index, diaChi, false, who);
        dienNoiOHienTaiBiDon(loaiDon, index, noiOHienTai, who);
        webUI.setTextWithCheck(getTxtNgheNghiep(index, loaiDon), ngheNghiep,
                "Ô nhập [Nghề nghiệp, nơi làm việc] (" + who + ")");
        webUI.setTextWithCheck(getTxtSoDienThoai(index, loaiDon), sdt, "Ô nhập [Số điện thoại] (" + who + ")");
        webUI.setTextWithCheck(getTxtEmail(index, loaiDon), email, "Ô nhập [Địa chỉ thư điện tử] (" + who + ")");
    }

    /** Nơi ở hiện tại: thẻ địa chỉ #2 (nếu có) hoặc ô text đơn. */
    private void dienNoiOHienTaiBiDon(String loaiDon, int index, String noiO, String who) {
        if (noiO == null || noiO.isBlank()) {
            return;
        }
        String scope = scoped(loaiDon, index);
        int blocks = webUI.countVisibleAddressBlocks(scope);
        if (blocks >= 2) {
            webUI.dismissOpenDropdowns();
            webUI.sleepMillis(WaitConfig.ADDRESS_BLOCK_GAP_MS);
            webUI.ensureAdministrativeAddressBlockInScope(scope, 1, noiO, who + " — Nơi ở hiện tại");
            return;
        }
        By txt = getTxtNoiOHienTai(index, loaiDon);
        if (webUI.existsNow(txt)) {
            webUI.setTextWithCheck(txt, WebUI.toAddressStreetDetail(noiO) != null
                            ? WebUI.toAddressStreetDetail(noiO) : noiO,
                    "Ô nhập [Nơi ở hiện tại] (" + who + ")");
        }
    }

    public void dienThongTinToChuc(int index, String loaiDon, String tenToChuc, String loaiHinh, String mst,
                                   String diaChi, String nguoiDaiDien, String sdt) {
        dienThongTinToChuc(index, loaiDon, tenToChuc, loaiHinh, mst, diaChi, nguoiDaiDien, "", sdt, "");
    }

    public void dienThongTinToChuc(int index, String loaiDon, String tenToChuc, String loaiHinh, String mst,
                                   String diaChi, String nguoiDaiDien, String chucVu, String sdt, String email) {
        String who = DataDictionary.isHonNhanGiaDinh(loaiDon)
                ? honNhanWhoLabel(index)
                : (DataDictionary.isPhaSan(loaiDon) ? "Doanh nghiệp bị yêu cầu" : "Bị đơn " + index);
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            waitHonNhanForm(index, "Tổ chức");
        } else if (!DataDictionary.isPhaSan(loaiDon)) {
            waitBiDonFormReady(index, loaiDon, "Tổ chức");
        }
        webUI.waitUntilVisible(getTxtTenToChuc(index, loaiDon), WaitConfig.FIELD, "Ô [Tên tổ chức] (" + who + ")");
        webUI.setText(getTxtTenToChuc(index, loaiDon), tenToChuc, "Ô nhập [Tên tổ chức] (" + who + ")");
        if (loaiHinh != null && !loaiHinh.isBlank() && webUI.existsNow(getBtnLoaiHinhToChuc(index, loaiDon))) {
            webUI.selectDropdownWithCheck(getBtnLoaiHinhToChuc(index, loaiDon), LIST_OPTIONS_LOAI_HINH, loaiHinh,
                    "Dropdown [Loại hình] (" + who + ")");
        }
        webUI.setTextWithCheck(getTxtMaSoThue(index, loaiDon), mst, "Ô nhập [Mã số thuế] (" + who + ")");
        dienDiaChiBiDon(loaiDon, index, diaChi, true, who);
        webUI.setTextWithCheck(getTxtNguoiDaiDien(index, loaiDon), nguoiDaiDien, "Ô nhập [Người đại diện] (" + who + ")");
        webUI.setTextWithCheck(getTxtChucVu(index, loaiDon), chucVu, "Ô nhập [Chức vụ] (" + who + ")");
        webUI.setTextWithCheck(getTxtSoDienThoai(index, loaiDon), sdt, "Ô nhập [Số điện thoại tổ chức] (" + who + ")");
        webUI.setTextWithCheck(getTxtEmail(index, loaiDon), email, "Ô nhập [Email tổ chức] (" + who + ")");
    }

    public void dienThongTinNguoiBiKienHanhChinh(int index, String loaiDon, String tenCoQuan, String diaChi,
                                                 String chucDanh, String nguoiThamQuyen, String sdt) {
        String who = "Bị kiện " + index;
        By markerCoQuan = By.xpath("//label[" + CO_QUAN_LABEL_MATCH + "]"
                + " | (" + CARD_TEN_CO_QUAN + ")[" + index + "]");
        // Một số loại việc Hành chính (vd. Khác) dùng form Bị đơn chuẩn Cá nhân/Tổ chức.
        By markerCaNhan = By.xpath(biDonCard(index) + "//label[contains(., 'Họ và tên')]"
                + " | //h2[contains(., 'Bị đơn') or contains(., 'bị kiện')]/ancestor::div[1]"
                + "//label[contains(., 'Họ và tên')]");
        long deadline = System.currentTimeMillis() + WaitConfig.STEP * 1000L;
        boolean coQuan = false;
        boolean caNhan = false;
        System.out.println(" ⏳ Chờ form bước 3 Hành chính (" + who + ")...");
        while (System.currentTimeMillis() < deadline) {
            if (webUI.existsNow(markerCoQuan) || webUI.existsNow(getTxtTenCoQuan(index))) {
                coQuan = true;
                break;
            }
            if (webUI.existsNow(markerCaNhan)) {
                caNhan = true;
                break;
            }
            webUI.sleepMillis(250);
        }
        if (caNhan && !coQuan) {
            System.out.println(" ℹ Hành chính — UI dạng Bị đơn Cá nhân (không có Tên cơ quan).");
            BiDonData fallback = BiDonData.builder()
                    .loai("Cá nhân")
                    .hoTen(tenCoQuan != null && !tenCoQuan.isBlank() ? tenCoQuan : "Nguyễn Văn A")
                    .cccd("0" + String.format("%011d", Math.abs(System.nanoTime() % 100_000_000_000L)))
                    .namSinh("1985")
                    .diaChiCaNhan(diaChi)
                    .sdt(sdt)
                    .email("")
                    .build();
            dienThongTinCaNhan(index, loaiDon, fallback.hoTen(), fallback.cccd(), fallback.namSinh(),
                    fallback.diaChiCaNhan(), fallback.sdt(), fallback.email());
            return;
        }
        if (!coQuan) {
            throw new RuntimeException(
                    "❌ Hết thời gian chờ: Không thấy form Hành chính [Tên cơ quan] hoặc [Họ và tên] ("
                            + who + ") sau " + WaitConfig.STEP + "s.");
        }
        webUI.waitUntilVisible(getTxtTenCoQuan(index), WaitConfig.FIELD, "Ô [Tên cơ quan] (" + who + ")");
        webUI.setText(getTxtTenCoQuan(index), tenCoQuan, "Ô nhập [Tên cơ quan] (" + who + ")");
        dienDiaChiBiDon(loaiDon, index, diaChi, true, who);
        webUI.setTextWithCheck(getTxtChucDanh(index), chucDanh, "Ô nhập [Chức danh] (" + who + ")");
        webUI.setTextWithCheck(getTxtNguoiCoThamQuyen(index), nguoiThamQuyen,
                "Ô nhập [Người có thẩm quyền] (" + who + ")");
        webUI.setTextWithCheck(getTxtSoDienThoai(index, loaiDon), sdt, "Ô nhập [Số điện thoại] (" + who + ")");
    }

    public void dienNguoiLienQuan(String loaiDon, String luaChon, String hoTen, String lyDo, String lienLac) {
        if (luaChon == null || luaChon.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua phần [Người liên quan] do dữ liệu trống.");
            TestActionLog.boQua("Người liên quan", "Giá trị trống");
            return;
        }
        String value = luaChon.trim().toLowerCase().equals("có") ? "Có" : "Không";
        By activeToggle = By.xpath(nguoiLienQuanToggleRow(loaiDon)
                + "//div[contains(@class, 'inline-flex')]//div[contains(@class, 'bg-toa-do-co') and normalize-space(.)='"
                + value + "']");
        if (!webUI.existsNow(activeToggle)) {
            webUI.clickElement(getToggleNguoiLienQuan(luaChon, loaiDon), "Nút chuyển Người liên quan: [" + luaChon + "]");
        } else {
            System.out.println(" ⏩ Nút chuyển Người liên quan [" + luaChon + "] đã được chọn sẵn.");
            TestActionLog.chon("Nút chuyển Người liên quan", luaChon);
        }
        if (luaChon.trim().toLowerCase().equals("có")) {
            webUI.waitUntilVisible(getTxtHoTenNguoiLienQuan(loaiDon), WaitConfig.FIELD, "Ô [Họ tên người liên quan]");
            webUI.setTextWithCheck(getTxtHoTenNguoiLienQuan(loaiDon), hoTen, "Ô nhập [Họ tên người liên quan]");
            webUI.waitUntilVisible(getTxtLyDoLienQuan(loaiDon), WaitConfig.FIELD, "Ô [Mối quan hệ / Lý do liên quan]");
            webUI.setText(getTxtLyDoLienQuan(loaiDon), lyDo, "Ô nhập [Mối quan hệ / Lý do liên quan]");
            webUI.setTextWithCheck(getTxtLienLacNguoiLienQuan(loaiDon), lienLac, "Ô nhập [Thông tin liên lạc người LQ]");
            // Chờ thẻ địa chỉ NLQ render trước khi damBaoDiaChiNguoiLienQuan.
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        }
    }

    /**
     * Điền toàn bộ bước 3 theo scenario — hỗ trợ 1–N bị đơn (nút Thêm nếu UI cho phép).
     */
    public void dienBuoc3(TaoDonScenario s) {
        int desired = s.soLuongBiDon();
        if (desired >= 2 && !coTheThemBiDon(s.loaiDon())) {
            System.out.println(" ⏩ Scenario yêu cầu " + desired + " bị đơn nhưng UI không có nút Thêm — chỉ điền 1.");
            TestActionLog.boQua("Thêm bị đơn", "Giao diện không có nút Thêm — chỉ điền 1 (yêu cầu " + desired + ")");
            desired = 1;
        }

        dienMotBiDon(1, s.loaiDon(), s.biDonChinh());
        for (int i = 2; i <= desired; i++) {
            BiDonData data = (i == 2 && s.biDonThem() != null) ? s.biDonThem() : s.biDonChinh();
            dienMotBiDon(i, s.loaiDon(), data);
        }

        dienNguoiLienQuan(s.loaiDon(), s.coNguoiLienQuan(), s.hoTenNLQ(), s.lyDoNLQ(), s.thongTinLienLacNLQ());
        damBaoDiaChiTatCaBiDon(s);
    }

    public void dienMotBiDon(int index, String loaiDon, BiDonData data) {
        if (data == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu bị đơn #" + index);
        }
        ensureBiDonSlot(index, loaiDon);

        // 4 UI bước 3 theo loại đơn (catalog):
        // Hành chính → cơ quan bị kiện | Phá sản → DN/HTX | Hôn nhân → người bị yêu cầu |
        // Còn lại (Dân sự/Lao động/KDTM/SHTT) → Bị đơn chuẩn Cá nhân/Tổ chức
        if (DataDictionary.isHanhChinh(loaiDon)) {
            dienThongTinNguoiBiKienHanhChinh(
                    index, loaiDon, data.tenCoQuanHC(), data.diaChiTruSo(),
                    data.chucDanhHC(), data.nguoiThamQuyenHC(), data.sdt());
            return;
        }
        if (DataDictionary.isPhaSan(loaiDon)) {
            dienThongTinToChuc(
                    index, loaiDon, data.tenToChuc(), data.loaiHinh(), data.mst(),
                    data.diaChiTruSo(), data.nguoiDaiDien(), data.chucVu(), data.sdt(), data.email());
            return;
        }

        String loai = data.loai() == null || data.loai().isBlank() ? "Cá nhân" : data.loai();
        chonLoaiBiDon(index, loai, loaiDon);
        if (DataDictionary.isToChuc(loai)) {
            TestActionLog.ghiChu("Đang xử lý luồng Bị đơn #" + index + ": Tổ chức");
            dienThongTinToChuc(
                    index, loaiDon, data.tenToChuc(), data.loaiHinh(), data.mst(),
                    data.diaChiTruSo(), data.nguoiDaiDien(), data.chucVu(), data.sdt(), data.email());
        } else {
            TestActionLog.ghiChu("Đang xử lý luồng Bị đơn #" + index + ": Cá nhân");
            dienThongTinCaNhan(
                    index, loaiDon, data.hoTen(), data.cccd(), data.namSinh(), data.gioiTinh(),
                    data.diaChiCaNhan(), data.noiOHienTai(), data.ngheNghiep(),
                    data.sdt(), data.email());
        }
    }

    private String getNguoiLienQuanToggleScope(String loaiDon) {
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            return honNhanSectionScope();
        }
        return "";
    }

    public void clickTiepTheo() {
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước 3");
    }
}

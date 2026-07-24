package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.DataDictionary;
import utils.WebUI;

public class BiDonPage {
    private WebDriver driver;
    private WebUI webUI;

    // Dân sự / Lao động / ...: block có nút "Thêm bị đơn"
    private static final String BIDON_SECTION =
            "//div[.//button[contains(., 'Thêm bị đơn') or contains(., 'Thêm người bị kiện') or contains(., 'Thêm người được yêu cầu')]]";

    // Hôn nhân bước 3 — 2 UI khác nhau tùy loại việc:
    // A) "Người bị yêu cầu …" + nút Thêm (Ly hôn đơn phương, …)
    // B) "Người yêu cầu 2 (vợ / chồng)" — không có nút Thêm (Thuận tình ly hôn, …)
    private static final By BTN_THEM_NGUOI_BI_YEU_CAU =
            By.xpath("//button[contains(normalize-space(.), 'Thêm người bị yêu cầu')]");
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
        return "//span[contains(., 'Người bị yêu cầu " + index + "')"
                + " or contains(., 'Người yêu cầu " + index + "')]"
                + "/ancestor::div[contains(@class, 'border') and contains(@class, 'rounded')][1]";
    }

    private String formScope(String loaiDon, int index) {
        return DataDictionary.isHonNhanGiaDinh(loaiDon) ? honNhanCard(index) : null;
    }

    private String honNhanWhoLabel(int index) {
        return isHonNhanVoChongUi() ? "Người yêu cầu 2" : "Người bị yêu cầu " + index;
    }

    private By honNhanCardBadge(int index) {
        if (isHonNhanVoChongUi()) {
            return HON_NHAN_VO_CHONG_BADGE;
        }
        return By.xpath("//span[contains(., 'Người bị yêu cầu " + index + "')"
                + " or contains(., 'Người yêu cầu " + index + "')]");
    }

    private int countHonNhanCards() {
        if (isHonNhanVoChongUi()) {
            return webUI.existsNow(HON_NHAN_VO_CHONG_BADGE) ? 1 : 0;
        }
        return webUI.countNow(By.xpath(
                honNhanSectionScope()
                        + "//div[contains(@class, 'border') and contains(@class, 'rounded')"
                        + " and .//span[contains(., 'Người bị yêu cầu')]]"));
    }

    private void waitHonNhanStepReady() {
        System.out.println(" ⏳ Chờ bước 3 [Hôn nhân và gia đình]...");
        for (int i = 1; i <= 30; i++) {
            if (webUI.existsNow(BTN_THEM_NGUOI_BI_YEU_CAU)
                    || webUI.existsNow(HON_NHAN_H2_ANY)
                    || webUI.existsNow(HON_NHAN_VO_CHONG_BADGE)
                    || webUI.existsNow(honNhanCardBadge(1))
                    || webUI.existsNow(By.xpath(honNhanCard(1) + "//label[contains(., 'Năm sinh') or contains(., 'Tên tổ chức')]"))) {
                String ui = isHonNhanVoChongUi() ? "Người yêu cầu 2 (vợ/chồng)" : "Người bị yêu cầu";
                System.out.println(" ✅ Bước 3 Hôn nhân sẵn sàng — UI: " + ui + " (" + i + "s).");
                return;
            }
            if (i == 1 || i % 5 == 0) {
                System.out.println(" ⏳ Chờ bước 3 Hôn nhân... (" + i + "/30s)");
            }
            webUI.sleep(1);
        }
        throw new RuntimeException(
                "❌ Timeout: Không thấy bước 3 Hôn nhân sau 30s. "
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
        for (int next = countHonNhanCards() + 1; next <= index; next++) {
            webUI.clickElement(BTN_THEM_NGUOI_BI_YEU_CAU, "Nút [Thêm người bị yêu cầu]");
            webUI.sleep(1);
            webUI.waitUntilExists(honNhanCardBadge(next), 10, honNhanWhoLabel(next));
        }
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
        webUI.waitUntilExists(marker, 10, "Form " + honNhanWhoLabel(index) + " [" + option + "]");
    }

    private void chonLoaiHonNhan(int index, String loaiBiDon) {
        System.out.println(" ⏳ Xử lý Hôn nhân — " + honNhanWhoLabel(index) + ", loại: " + loaiBiDon);
        ensureHonNhanCard(index);
        String option = honNhanOption(loaiBiDon);
        String who = honNhanWhoLabel(index);

        if ("Cá nhân".equals(option) && webUI.existsNow(honNhanCaNhanFormMarker(index))) {
            System.out.println(" ⏩ Form Cá nhân [" + who + "] đã hiển thị — không cần click tab.");
            return;
        }
        if ("Tổ chức".equals(option) && webUI.existsNow(honNhanToChucFormMarker(index))) {
            System.out.println(" ⏩ Form Tổ chức [" + who + "] đã hiển thị — không cần click tab.");
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

        if (isToChuc) {
            return By.xpath("(" + BIDON_SECTION
                    + "//div[contains(@class, 'cursor-pointer') and (contains(., 'Tổ chức') or contains(., 'Doanh nghiệp'))])["
                    + index + "]");
        }
        return By.xpath("(" + BIDON_SECTION
                + "//div[contains(@class, 'cursor-pointer') and contains(., 'Cá nhân') and not(contains(., 'Tổ chức'))])["
                + index + "]");
    }

    private By inputInScope(String loaiDon, int index, String labelMatch) {
        String scope = formScope(loaiDon, index);
        if (scope != null) {
            return By.xpath(scope + "//label[" + labelMatch + "]/parent::div//input");
        }
        return By.xpath("(//label[" + labelMatch + "]/parent::div//input)[" + index + "]");
    }

    private By getTxtHoTen(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Họ và tên')");
    }

    private By getTxtCCCD(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Số CCCD')");
    }

    private By getTxtNamSinh(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Năm sinh')");
    }

    private By getTxtDiaChiCaNhan(int index, String loaiDon) {
        String scope = formScope(loaiDon, index);
        String label = "starts-with(normalize-space(.), 'Địa chỉ') and not(contains(., 'trụ sở'))";
        if (scope != null) {
            return By.xpath(scope + "//label[" + label + "]/parent::div//input");
        }
        return By.xpath("(//label[" + label + "]/parent::div//input)[" + index + "]");
    }

    private By getTxtTenToChuc(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Tên tổ chức')");
    }

    private By getBtnLoaiHinhToChuc(int index, String loaiDon) {
        String scope = formScope(loaiDon, index);
        if (scope != null) {
            return By.xpath(scope + "//label[contains(., 'Loại hình')]/parent::div//button");
        }
        return By.xpath("(//label[contains(., 'Loại hình')]/parent::div//button)[" + index + "]");
    }

    private By getTxtMaSoThue(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Mã số thuế')");
    }

    private By getTxtDiaChiTruSo(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Địa chỉ trụ sở')");
    }

    private By getTxtNguoiDaiDien(int index, String loaiDon) {
        String scope = formScope(loaiDon, index);
        if (scope != null) {
            return By.xpath(scope
                    + "//label[normalize-space(.)='Người đại diện']/parent::div//input");
        }
        return By.xpath("(//label[contains(., 'Người đại diện')]/parent::div//input)[" + index + "]");
    }

    private By getTxtTenCoQuan(int index) {
        return By.xpath("(//label[contains(., 'Tên cơ quan')]/parent::div//input)[" + index + "]");
    }

    private By getTxtChucDanh(int index) {
        return By.xpath("(//label[contains(., 'Chức danh')]/parent::div//input)[" + index + "]");
    }

    private By getTxtNguoiCoThamQuyen(int index) {
        return By.xpath("(//label[contains(., 'Người có thẩm quyền')]/parent::div//input)[" + index + "]");
    }

    private By getTxtSoDienThoai(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Số điện thoại')");
    }

    private By getTxtEmail(int index, String loaiDon) {
        return inputInScope(loaiDon, index, "contains(., 'Email')");
    }

    private By getBtnXoaBiDon(int index) {
        return By.xpath("(//button[.//svg[contains(@class, 'lucide-trash')]])[" + index + "]");
    }

    private By getToggleNguoiLienQuan(String luaChon, String loaiDon) {
        String value = luaChon.trim().toLowerCase().equals("có") ? "Có" : "Không";
        return By.xpath(getNguoiLienQuanToggleScope(loaiDon)
                + "//div[contains(., 'Người có quyền lợi')]/ancestor::div[contains(@class, 'justify-between')][1]"
                + "//div[contains(@class, 'inline-flex')]//div[normalize-space(.)='" + value + "']");
    }

    private By getTxtHoTenNguoiLienQuan(String loaiDon) {
        String section = DataDictionary.isHonNhanGiaDinh(loaiDon) ? honNhanSectionScope() : "";
        return By.xpath(section
                + "//label[contains(., 'Họ tên') or contains(., 'Tên tổ chức')]/parent::div//input");
    }

    private By getTxtLyDoLienQuan(String loaiDon) {
        String section = DataDictionary.isHonNhanGiaDinh(loaiDon) ? honNhanSectionScope() : "";
        return By.xpath(section
                + "//label[contains(., 'Mối quan hệ') or contains(., 'Lý do')]/parent::div//textarea");
    }

    private By getTxtLienLacNguoiLienQuan(String loaiDon) {
        String section = DataDictionary.isHonNhanGiaDinh(loaiDon) ? honNhanSectionScope() : "";
        return By.xpath(section + "//label[contains(., 'Thông tin liên lạc')]/parent::div//input");
    }

    private final By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    public BiDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void clickXoaBiDon(int index) {
        webUI.clickElement(getBtnXoaBiDon(index), "Nút [Xóa Bị Đơn " + index + "]");
        webUI.sleep(1);
    }

    public void chonLoaiBiDon(int index, String loaiBiDon, String loaiDon) {
        if (loaiBiDon == null || loaiBiDon.isEmpty()) {
            return;
        }
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            chonLoaiHonNhan(index, loaiBiDon);
            return;
        }
        webUI.clickElement(getLoaiBiDon(index, loaiBiDon, loaiDon),
                "Loại Bị đơn " + index + ": [" + loaiBiDon + "]");
    }

    public void dienThongTinCaNhan(int index, String loaiDon, String hoTen, String cccd, String namSinh,
                                   String diaChi, String sdt, String email) {
        String who = DataDictionary.isHonNhanGiaDinh(loaiDon) ? honNhanWhoLabel(index) : "Bị đơn " + index;
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            waitHonNhanForm(index, "Cá nhân");
        }
        webUI.setTextWithCheck(getTxtHoTen(index, loaiDon), hoTen, "Ô nhập [Họ và tên] (" + who + ")");
        webUI.setTextWithCheck(getTxtCCCD(index, loaiDon), cccd, "Ô nhập [Số CCCD/CMND] (" + who + ")");
        webUI.setTextWithCheck(getTxtNamSinh(index, loaiDon), namSinh, "Ô nhập [Năm sinh] (" + who + ")");
        webUI.setTextWithCheck(getTxtDiaChiCaNhan(index, loaiDon), diaChi, "Ô nhập [Địa chỉ] (" + who + ")");
        webUI.setTextWithCheck(getTxtSoDienThoai(index, loaiDon), sdt, "Ô nhập [Số điện thoại] (" + who + ")");
        webUI.setTextWithCheck(getTxtEmail(index, loaiDon), email, "Ô nhập [Email] (" + who + ")");
    }

    public void dienThongTinToChuc(int index, String loaiDon, String tenToChuc, String loaiHinh, String mst,
                                   String diaChi, String nguoiDaiDien, String sdt) {
        String who = DataDictionary.isHonNhanGiaDinh(loaiDon) ? honNhanWhoLabel(index) : "Bị đơn " + index;
        if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            waitHonNhanForm(index, "Tổ chức");
        }
        webUI.setTextWithCheck(getTxtTenToChuc(index, loaiDon), tenToChuc, "Ô nhập [Tên tổ chức] (" + who + ")");
        webUI.selectDropdownWithCheck(getBtnLoaiHinhToChuc(index, loaiDon), LIST_OPTIONS_LOAI_HINH, loaiHinh,
                "Dropdown [Loại hình] (" + who + ")");
        webUI.setTextWithCheck(getTxtMaSoThue(index, loaiDon), mst, "Ô nhập [Mã số thuế] (" + who + ")");
        webUI.setTextWithCheck(getTxtDiaChiTruSo(index, loaiDon), diaChi, "Ô nhập [Địa chỉ trụ sở] (" + who + ")");
        webUI.setTextWithCheck(getTxtNguoiDaiDien(index, loaiDon), nguoiDaiDien, "Ô nhập [Người đại diện] (" + who + ")");
        webUI.setTextWithCheck(getTxtSoDienThoai(index, loaiDon), sdt, "Ô nhập [Số điện thoại tổ chức] (" + who + ")");
    }

    public void dienThongTinNguoiBiKienHanhChinh(int index, String tenCoQuan, String diaChi, String chucDanh,
                                                 String nguoiThamQuyen, String sdt) {
        webUI.setTextWithCheck(getTxtTenCoQuan(index), tenCoQuan, "Ô nhập [Tên cơ quan] (Bị kiện " + index + ")");
        webUI.setTextWithCheck(getTxtDiaChiTruSo(index, ""), diaChi, "Ô nhập [Địa chỉ trụ sở] (Bị kiện " + index + ")");
        webUI.setTextWithCheck(getTxtChucDanh(index), chucDanh, "Ô nhập [Chức danh] (Bị kiện " + index + ")");
        webUI.setTextWithCheck(getTxtNguoiCoThamQuyen(index), nguoiThamQuyen,
                "Ô nhập [Người có thẩm quyền] (Bị kiện " + index + ")");
        webUI.setTextWithCheck(getTxtSoDienThoai(index, ""), sdt, "Ô nhập [Số điện thoại] (Bị kiện " + index + ")");
    }

    public void dienNguoiLienQuan(String loaiDon, String luaChon, String hoTen, String lyDo, String lienLac) {
        if (luaChon == null || luaChon.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua phần [Người liên quan] do Excel trống.");
            return;
        }
        String value = luaChon.trim().toLowerCase().equals("có") ? "Có" : "Không";
        By activeToggle = By.xpath(getNguoiLienQuanToggleScope(loaiDon)
                + "//div[contains(@class, 'inline-flex')]//div[contains(@class, 'bg-toa-do-co') and normalize-space(.)='"
                + value + "']");
        if (!webUI.existsNow(activeToggle)) {
            webUI.clickElement(getToggleNguoiLienQuan(luaChon, loaiDon), "Toggle Người liên quan: [" + luaChon + "]");
        } else {
            System.out.println(" ⏩ Toggle Người liên quan [" + luaChon + "] đã được chọn sẵn.");
        }
        if (luaChon.trim().toLowerCase().equals("có")) {
            webUI.sleep(1);
            webUI.setTextWithCheck(getTxtHoTenNguoiLienQuan(loaiDon), hoTen, "Ô nhập [Họ tên người liên quan]");
            webUI.setTextWithCheck(getTxtLyDoLienQuan(loaiDon), lyDo, "Ô nhập [Lý do liên quan]");
            webUI.setTextWithCheck(getTxtLienLacNguoiLienQuan(loaiDon), lienLac, "Ô nhập [Thông tin liên lạc người LQ]");
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

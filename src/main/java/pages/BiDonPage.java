package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WebUI;

public class BiDonPage {
    private WebDriver driver;
    private WebUI webUI;

    // ==========================================
    // 1. SMART LOCATORS (HỖ TRỢ NHIỀU BỊ ĐƠN BẰNG INDEX)
    // ==========================================

    // --- LOẠI BỊ ĐƠN ---
    private By getLoaiBiDon(int index, String loai) {
        String tuKhoa = loai.trim().toLowerCase();
        if (tuKhoa.contains("tổ chức") || tuKhoa.contains("doanh nghiệp")) {
            return By.xpath("(//div[contains(@class, 'cursor-pointer') and contains(text(), 'Tổ chức')])[" + index + "]");
        } else {
            return By.xpath("(//div[contains(@class, 'cursor-pointer') and contains(text(), 'Cá nhân')])[" + index + "]");
        }
    }

    // --- FORM CÁ NHÂN ---
    private By getTxtHoTen(int index) { return By.xpath("(//label[contains(., 'Họ và tên')]/parent::div//input)[" + index + "]"); }
    private By getTxtCCCD(int index) { return By.xpath("(//label[contains(., 'Số CCCD')]/parent::div//input)[" + index + "]"); }
    private By getTxtNamSinh(int index) { return By.xpath("(//label[contains(., 'Năm sinh')]/parent::div//input)[" + index + "]"); }
    // Dùng starts-with để phân biệt "Địa chỉ" của cá nhân và "Địa chỉ trụ sở" của tổ chức
    private By getTxtDiaChiCaNhan(int index) { return By.xpath("(//label[starts-with(normalize-space(.), 'Địa chỉ') and not(contains(., 'trụ sở'))]/parent::div//input)[" + index + "]"); }

    // --- FORM TỔ CHỨC ---
    private By getTxtTenToChuc(int index) { return By.xpath("(//label[contains(., 'Tên tổ chức')]/parent::div//input)[" + index + "]"); }
    private By getBtnLoaiHinhToChuc(int index) { return By.xpath("(//label[contains(., 'Loại hình')]/parent::div//button)[" + index + "]"); }
    private By listOptionsLoaiHinh = By.xpath("//div[@role='listbox']//div[@role='option']"); // List dùng chung
    private By getTxtMaSoThue(int index) { return By.xpath("(//label[contains(., 'Mã số thuế')]/parent::div//input)[" + index + "]"); }
    private By getTxtDiaChiTruSo(int index) { return By.xpath("(//label[contains(., 'Địa chỉ trụ sở')]/parent::div//input)[" + index + "]"); }
    private By getTxtNguoiDaiDien(int index) { return By.xpath("(//label[contains(., 'Người đại diện')]/parent::div//input)[" + index + "]"); }

    // --- FORM DÙNG CHUNG CỦA BỊ ĐƠN ---
    private By getTxtSoDienThoai(int index) { return By.xpath("(//label[contains(., 'Số điện thoại')]/parent::div//input)[" + index + "]"); }
    private By getTxtEmail(int index) { return By.xpath("(//label[contains(., 'Email')]/parent::div//input)[" + index + "]"); }

    // --- CÁC NÚT THAO TÁC BỊ ĐƠN ---
    private By btnThemBiDon = By.xpath("//button[contains(., 'Thêm bị đơn')]");
    private By getBtnXoaBiDon(int index) { return By.xpath("(//button[.//svg[contains(@class, 'lucide-trash')]])[" + index + "]"); }

    // --- NGƯỜI CÓ QUYỀN LỢI & NGHĨA VỤ LIÊN QUAN ---
    private By getToggleNguoiLienQuan(String luaChon) {
        if (luaChon.trim().toLowerCase().equals("có")) {
            return By.xpath("//div[contains(text(), 'Người có quyền lợi')]/parent::div/following-sibling::div//div[normalize-space(text())='Có']");
        } else {
            return By.xpath("//div[contains(text(), 'Người có quyền lợi')]/parent::div/following-sibling::div//div[normalize-space(text())='Không']");
        }
    }
    private By txtHoTenNguoiLienQuan = By.xpath("//label[contains(., 'Họ tên / Tên tổ chức')]/parent::div//input");
    private By txtLyDoLienQuan = By.xpath("//label[contains(., 'Mối quan hệ')]/parent::div//textarea");
    private By txtLienLacNguoiLienQuan = By.xpath("//label[contains(., 'Thông tin liên lạc')]/parent::div//input");

    private By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    // ==========================================
    // 2. CONSTRUCTOR
    // ==========================================
    public BiDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    // ==========================================
    // 3. CÁC HÀM NGHIỆP VỤ (ACTION METHODS)
    // ==========================================

    public void clickThemBiDonMoi() {
        webUI.clickElement(btnThemBiDon, "Nút [Thêm bị đơn]");
        webUI.sleep(1);
    }

    public void clickXoaBiDon(int index) {
        webUI.clickElement(getBtnXoaBiDon(index), "Nút [Xóa Bị Đơn " + index + "]");
        webUI.sleep(1);
    }

    // --- ĐIỀN THÔNG TIN 1 BỊ ĐƠN BẤT KỲ (XÁC ĐỊNH BẰNG INDEX) ---
    public void chonLoaiBiDon(int index, String loaiBiDon) {
        if (loaiBiDon != null && !loaiBiDon.isEmpty()) {
            webUI.clickElement(getLoaiBiDon(index, loaiBiDon), "Loại Bị đơn " + index + ": [" + loaiBiDon + "]");
        }
    }

    public void dienThongTinCaNhan(int index, String hoTen, String cccd, String namSinh, String diaChi, String sdt, String email) {
        webUI.setTextWithCheck(getTxtHoTen(index), hoTen, "Ô nhập [Họ và tên] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtCCCD(index), cccd, "Ô nhập [Số CCCD/CMND] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtNamSinh(index), namSinh, "Ô nhập [Năm sinh] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtDiaChiCaNhan(index), diaChi, "Ô nhập [Địa chỉ] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtSoDienThoai(index), sdt, "Ô nhập [Số điện thoại] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtEmail(index), email, "Ô nhập [Email] (Bị đơn " + index + ")");
    }

    public void dienThongTinToChuc(int index, String tenToChuc, String loaiHinh, String mst, String diaChi, String nguoiDaiDien, String sdt) {
        webUI.setTextWithCheck(getTxtTenToChuc(index), tenToChuc, "Ô nhập [Tên tổ chức] (Bị đơn " + index + ")");
        webUI.selectDropdownWithCheck(getBtnLoaiHinhToChuc(index), listOptionsLoaiHinh, loaiHinh, "Dropdown [Loại hình] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtMaSoThue(index), mst, "Ô nhập [Mã số thuế] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtDiaChiTruSo(index), diaChi, "Ô nhập [Địa chỉ trụ sở] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtNguoiDaiDien(index), nguoiDaiDien, "Ô nhập [Người đại diện] (Bị đơn " + index + ")");
        webUI.setTextWithCheck(getTxtSoDienThoai(index), sdt, "Ô nhập [Số điện thoại tổ chức] (Bị đơn " + index + ")");
    }

    // --- NGƯỜI CÓ QUYỀN LỢI, NGHĨA VỤ LIÊN QUAN ---
    public void dienNguoiLienQuan(String luaChon, String hoTen, String lyDo, String lienLac) {
        if (luaChon == null || luaChon.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua phần [Người liên quan] do Excel trống.");
            return;
        }

        webUI.clickElement(getToggleNguoiLienQuan(luaChon), "Toggle Người liên quan: [" + luaChon + "]");

        if (luaChon.trim().toLowerCase().equals("có")) {
            webUI.sleep(1); // Chờ form xổ xuống
            webUI.setTextWithCheck(txtHoTenNguoiLienQuan, hoTen, "Ô nhập [Họ tên người liên quan]");
            webUI.setTextWithCheck(txtLyDoLienQuan, lyDo, "Ô nhập [Lý do liên quan]");
            webUI.setTextWithCheck(txtLienLacNguoiLienQuan, lienLac, "Ô nhập [Thông tin liên lạc người LQ]");
        }
    }

    public void clickTiepTheo() {
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước Bị Đơn");
    }
}

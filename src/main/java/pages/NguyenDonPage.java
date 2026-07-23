package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WebUI;

public class NguyenDonPage {
    private WebDriver driver;
    private WebUI webUI;

    // ==========================================
    // 1. SMART LOCATORS (CHỐNG LỖI HOA/THƯỜNG & KHOẢNG TRẮNG)
    // ==========================================

    // Hàm bắt Loại chủ thể (Excel nhập "Cá nhân", "cá nhân", "TỔ CHỨC", "doanh nghiệp" đều nhận hết)
    private By getLoaiChuThe(String loai) {
        String tuKhoa = loai.trim().toLowerCase(); // Ép về chữ thường để so sánh

        if (tuKhoa.contains("tổ chức") || tuKhoa.contains("doanh nghiệp")) {
            // Chỉ cần HTML chứa chữ "Tổ chức" là click trúng phóc
            return By.xpath("//div[contains(@class, 'cursor-pointer') and contains(text(), 'Tổ chức')]");
        } else {
            return By.xpath("//div[contains(@class, 'cursor-pointer') and contains(text(), 'Cá nhân')]");
        }
    }

    // Hàm bắt Giới tính (Chống lỗi gõ "nam", "NỮ", "khác ")
    private By getGioiTinh(String gioiTinh) {
        String tuKhoa = gioiTinh.trim().toLowerCase();

        if (tuKhoa.equals("nam")) {
            return By.xpath("//label[contains(text(),'Giới tính')]/following-sibling::div//div[contains(text(), 'Nam')]");
        } else if (tuKhoa.contains("nữ")) {
            return By.xpath("//label[contains(text(),'Giới tính')]/following-sibling::div//div[contains(text(), 'Nữ')]");
        } else {
            return By.xpath("//label[contains(text(),'Giới tính')]/following-sibling::div//div[contains(text(), 'Khác')]");
        }
    }

    // --- LOCATOR CÁ NHÂN ---
    private By txtHoTen = By.xpath("//label[contains(text(),'Họ và tên')]/following-sibling::input");
    private By txtNgaySinh = By.xpath("//label[contains(text(),'Ngày sinh')]/following-sibling::div/input");
    private By txtCCCD = By.xpath("//label[contains(text(),'Số CCCD')]/following-sibling::input");
    private By txtNgayCapCCCD = By.xpath("//label[contains(text(),'Ngày cấp CCCD')]/following-sibling::div/input");
    private By txtNoiCapCCCD = By.xpath("//label[contains(text(),'Nơi cấp CCCD')]/following-sibling::input");
    private By txtDiaChiThuongTru = By.xpath("//label[contains(text(),'Địa chỉ thường trú')]/following-sibling::textarea");

    // Đã đổi text() thành contains() để né lỗi dư dấu cách trên HTML
    private By chkGiongThuongTru = By.xpath("//span[contains(text(), 'giống địa chỉ thường trú')]");
    private By txtDiaChiLienLac = By.xpath("//label[contains(text(),'Địa chỉ liên lạc')]/following-sibling::textarea");

    // --- LOCATOR TỔ CHỨC / DOANH NGHIỆP ---
    private By txtTenToChuc = By.xpath("//label[contains(text(), 'Tên tổ chức')]/following-sibling::input");
    private By btnLoaiHinhToChuc = By.xpath("//label[contains(text(), 'Loại hình tổ chức')]/following-sibling::div//button");
    private By listOptionsLoaiHinh = By.xpath("//div[@role='listbox']//div[@role='option']");
    private By txtMaSoThue = By.xpath("//label[contains(text(), 'Mã số thuế')]/following-sibling::input");
    private By txtDiaChiTruSo = By.xpath("//label[contains(text(), 'Địa chỉ trụ sở')]/following-sibling::textarea");
    private By txtNguoiDaiDienPL = By.xpath("//label[contains(text(), 'Người đại diện pháp luật')]/following-sibling::input");
    private By txtChucVu = By.xpath("//label[contains(text(), 'Chức vụ')]/following-sibling::input");

    // --- LOCATOR DÙNG CHUNG ---
    private By txtSoDienThoai = By.xpath("//label[contains(text(),'Số điện thoại')]/following-sibling::input");
    private By txtEmail = By.xpath("//label[contains(text(),'Email')]/following-sibling::input");

    // Đã đổi text() thành contains()
    private By chkNguoiDaiDien = By.xpath("//span[contains(text(), 'Tôi có người đại diện pháp lý')]");
    private By txtTenNguoiDaiDien = By.xpath("//label[contains(text(),'Người đại diện pháp lý')]/following-sibling::input");
    private By btnDropdownQuanHe = By.xpath("//label[contains(text(),'Quan hệ')]/following-sibling::div//button");
    private By listOptionsQuanHe = By.xpath("//div[@role='listbox']//div[@role='option']");

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
        if (loaiChuThe != null && !loaiChuThe.isEmpty()) {
            // Giờ đây getLoaiChuThe đã tự chuẩn hóa, bạn không cần lo Excel viết hoa/thường nữa
            webUI.clickElement(getLoaiChuThe(loaiChuThe), "Thẻ Loại chủ thể: [" + loaiChuThe + "]");
        }
    }

    public void dienThongTinCaNhan(String hoTen, String ngaySinh, String gioiTinh, String cccd, String ngayCap, String noiCap) {
        webUI.setTextWithCheck(txtHoTen, hoTen, "Ô nhập [Họ và tên]");
        webUI.setTextForMaskedInput(txtNgaySinh, ngaySinh, "Ô nhập [Ngày sinh]");

        if (gioiTinh != null && !gioiTinh.isEmpty()) {
            webUI.clickElement(getGioiTinh(gioiTinh), "Thẻ Giới tính: [" + gioiTinh + "]");
        }

        webUI.setTextWithCheck(txtCCCD, cccd, "Ô nhập [Số CCCD / CMND]");
        webUI.setTextForMaskedInput(txtNgayCapCCCD, ngayCap, "Ô nhập [Ngày cấp CCCD]");
        webUI.setTextWithCheck(txtNoiCapCCCD, noiCap, "Ô nhập [Nơi cấp CCCD]");
    }

    public void dienThongTinLienHe(String thuongTru, String lienLac, String sdt, String email) {
        webUI.setTextWithCheck(txtDiaChiThuongTru, thuongTru, "Ô nhập [Địa chỉ thường trú]");

        boolean yeuCauGiong = (lienLac == null || lienLac.trim().isEmpty() ||
                lienLac.toLowerCase().contains("giống thường trú") ||
                lienLac.trim().equals(thuongTru.trim()));

        if (yeuCauGiong) {
            if (webUI.isElementVisible(txtDiaChiLienLac)) {
                webUI.clickElement(chkGiongThuongTru, "Checkbox [Địa chỉ liên lạc giống địa chỉ thường trú]");
                webUI.sleep(1);
            }
        } else {
            if (!webUI.isElementVisible(txtDiaChiLienLac)) {
                webUI.clickElement(chkGiongThuongTru, "Bỏ chọn Checkbox [Địa chỉ liên lạc giống địa chỉ thường trú]");
                webUI.sleep(1);
            }
            webUI.setTextWithCheck(txtDiaChiLienLac, lienLac, "Ô nhập [Địa chỉ liên lạc]");
        }

        webUI.setTextWithCheck(txtSoDienThoai, sdt, "Ô nhập [Số điện thoại]");
        webUI.setTextWithCheck(txtEmail, email, "Ô nhập [Email]");
    }

    public void dienThongTinToChuc(String tenToChuc, String loaiHinh, String mst, String diaChi,
                                   String nguoiDaiDien, String chucVu, String sdt, String email) {
        webUI.setTextWithCheck(txtTenToChuc, tenToChuc, "Ô nhập [Tên tổ chức / doanh nghiệp]");
        webUI.selectDropdownWithCheck(btnLoaiHinhToChuc, listOptionsLoaiHinh, loaiHinh, "Dropdown [Loại hình tổ chức]");
        webUI.setTextWithCheck(txtMaSoThue, mst, "Ô nhập [Mã số thuế / MSDN]");
        webUI.setTextWithCheck(txtDiaChiTruSo, diaChi, "Ô nhập [Địa chỉ trụ sở]");
        webUI.setTextWithCheck(txtNguoiDaiDienPL, nguoiDaiDien, "Ô nhập [Người đại diện pháp luật]");
        webUI.setTextWithCheck(txtChucVu, chucVu, "Ô nhập [Chức vụ]");
        webUI.setTextWithCheck(txtSoDienThoai, sdt, "Ô nhập [Số điện thoại tổ chức]");
        webUI.setTextWithCheck(txtEmail, email, "Ô nhập [Email tổ chức]");
    }

    public void chonNguoiDaiDien(String coNguoiDaiDien, String tenNguoiDaiDien, String quanHe) {
        if (coNguoiDaiDien != null && coNguoiDaiDien.trim().toLowerCase().equals("có")) {
            webUI.clickElement(chkNguoiDaiDien, "Checkbox [Tôi có người đại diện pháp lý]");
            webUI.sleep(1);
            webUI.setTextWithCheck(txtTenNguoiDaiDien, tenNguoiDaiDien, "Ô nhập [Người đại diện pháp lý]");
            webUI.selectDropdownWithCheck(btnDropdownQuanHe, listOptionsQuanHe, quanHe, "Dropdown [Quan hệ đại diện]");
        } else {
            System.out.println(" ⏩ Bỏ qua Checkbox [Người đại diện] vì Excel không yêu cầu.");
        }
    }

    public void clickTiepTheo() {
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước Nguyên Đơn");
    }
}

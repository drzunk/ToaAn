package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WebUI;

public class NguyenDonPage {
    private WebDriver driver;
    private WebUI webUI;

    // ==========================================
    // 1. KHAI BÁO LOCATORS
    // ==========================================

    private By getLoaiChuThe(String loai) {
        return By.xpath("//div[text()='" + loai + "']");
    }

    private By txtHoTen = By.xpath("//label[contains(text(),'Họ và tên')]/following-sibling::input");
    private By txtNgaySinh = By.xpath("//label[contains(text(),'Ngày sinh')]/following-sibling::div/input");

    private By getGioiTinh(String gioiTinh) {
        return By.xpath("//label[contains(text(),'Giới tính')]/following-sibling::div//div[text()='" + gioiTinh + "']");
    }

    private By txtCCCD = By.xpath("//label[contains(text(),'Số CCCD')]/following-sibling::input");
    private By txtNgayCapCCCD = By.xpath("//label[contains(text(),'Ngày cấp CCCD')]/following-sibling::div/input");
    private By txtNoiCapCCCD = By.xpath("//label[contains(text(),'Nơi cấp CCCD')]/following-sibling::input");

    private By txtDiaChiThuongTru = By.xpath("//label[contains(text(),'Địa chỉ thường trú')]/following-sibling::textarea");
    private By chkGiongThuongTru = By.xpath("//span[text()='Địa chỉ liên lạc giống địa chỉ thường trú']");
    private By txtDiaChiLienLac = By.xpath("//label[contains(text(),'Địa chỉ liên lạc')]/following-sibling::textarea");

    private By txtSoDienThoai = By.xpath("//label[contains(text(),'Số điện thoại')]/following-sibling::input");
    private By txtEmail = By.xpath("//label[contains(text(),'Email')]/following-sibling::input");

    private By chkNguoiDaiDien = By.xpath("//span[text()='Tôi có người đại diện pháp lý']");
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
    // 3. CÁC HÀM NGHIỆP VỤ (ACTION METHODS)
    // ==========================================

    public void dienThongTinCaNhan(String loaiChuThe, String hoTen, String ngaySinh, String gioiTinh, String cccd, String ngayCap, String noiCap) {
        if (loaiChuThe != null && !loaiChuThe.isEmpty()) {
            webUI.clickElement(getLoaiChuThe(loaiChuThe), "Loại chủ thể: [" + loaiChuThe + "]");
        }

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
        // 1. Luôn điền Địa chỉ thường trú trước
        webUI.setTextWithCheck(txtDiaChiThuongTru, thuongTru, "Ô nhập [Địa chỉ thường trú]");

        // 2. PHÂN TÍCH TRẠNG THÁI ĐỘNG CỦA Ô ĐỊA CHỈ LIÊN LẠC
        boolean yeuCauGiong = (lienLac == null || lienLac.trim().isEmpty() ||
                lienLac.equalsIgnoreCase("Giống thường trú") ||
                lienLac.trim().equals(thuongTru.trim()));

        if (yeuCauGiong) {
            if (webUI.isElementVisible(txtDiaChiLienLac)) {
                webUI.clickElement(chkGiongThuongTru, "Checkbox [Địa chỉ liên lạc giống địa chỉ thường trú]");
                webUI.sleep(1);
            } else {
                utils.ExtentReportManager.logStep("Hệ thống đã tự động ẩn [Địa chỉ liên lạc] (Trùng khớp yêu cầu Excel).");
            }
        } else {
            if (!webUI.isElementVisible(txtDiaChiLienLac)) {
                webUI.clickElement(chkGiongThuongTru, "Bỏ chọn Checkbox [Địa chỉ liên lạc giống địa chỉ thường trú]");
                webUI.sleep(1);
            }
            webUI.setTextWithCheck(txtDiaChiLienLac, lienLac, "Ô nhập [Địa chỉ liên lạc]");
        }

        // 3. Điền các thông tin còn lại
        webUI.setTextWithCheck(txtSoDienThoai, sdt, "Ô nhập [Số điện thoại]");
        webUI.setTextWithCheck(txtEmail, email, "Ô nhập [Email]");
    }

    // ĐÃ BỔ SUNG LẠI HÀM NÀY (HÀM BỊ MẤT TRONG CODE BẠN VỪA GỬI)
    public void chonNguoiDaiDien(String coNguoiDaiDien, String tenNguoiDaiDien, String quanHe) {
        if (coNguoiDaiDien != null && coNguoiDaiDien.trim().equalsIgnoreCase("Có")) {
            webUI.clickElement(chkNguoiDaiDien, "Checkbox [Tôi có người đại diện pháp lý]");
            webUI.sleep(1);
            webUI.setTextWithCheck(txtTenNguoiDaiDien, tenNguoiDaiDien, "Ô nhập [Người đại diện pháp lý]");
            webUI.selectDropdownWithCheck(btnDropdownQuanHe, listOptionsQuanHe, quanHe, "Dropdown [Quan hệ đại diện]");
        } else {
            utils.ExtentReportManager.logStep("Bỏ qua Checkbox [Người đại diện] vì không có yêu cầu từ Excel.");
        }
    }

    public void clickTiepTheo() {
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước Nguyên Đơn");
    }
}
package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WebUI;

public class TaoDonPage {
    private WebDriver driver;
    private WebUI webUI;

    // --- KHAI BÁO LOCATORS ---
    private By btnDropdownLoaiViec = By.xpath("//button[contains(., 'Chọn loại việc cụ thể')]");
    private By listOptionsLoaiViec = By.xpath("//button[contains(., 'Chọn loại việc cụ thể')]/following-sibling::div//div[@role='option']");

    private By btnDropdownToaAn = By.xpath("//button[contains(., 'Chọn tòa án nhận đơn')]");
    // Locator bắt chính xác ô Input tìm kiếm nằm ngay trong khung Tòa án
    private By inputSearchToaAn = By.xpath("//button[contains(., 'Chọn tòa án nhận đơn')]/following-sibling::div//input[@placeholder='Tìm kiếm…']");
    private By listOptionsToaAn = By.xpath("//button[contains(., 'Chọn tòa án nhận đơn')]/following-sibling::div//div[@role='option']");

    private By txtTomTat = By.xpath("//textarea[@placeholder='Mô tả ngắn gọn nội dung tranh chấp (tuỳ chọn, có thể bổ sung sau)']");
    private By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    private By getCardLoaiDon(String tenLoaiDon) {
        return By.xpath("//span[text()='" + tenLoaiDon + "']/ancestor::div[contains(@class, 'cursor-pointer')]");
    }

    public TaoDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    // --- CÁC HÀM NGHIỆP VỤ ---
    public void dienFormBuoc1(String loaiDon, String loaiViecCuThe, String toaAn, String tomTat) {

        By theLoaiDon = getCardLoaiDon(loaiDon);

        // --- CHỐT CHẶN 1: Kiểm tra thẻ có tồn tại không ---
        if (!webUI.isElementVisible(theLoaiDon)) {
            // ĐÃ ĐỔI THÀNH RUNTIME EXCEPTION: LỖI LÀ PHẢI ĐỎ LÒM!
            throw new RuntimeException("❌ Lỗi dữ liệu: Không tìm thấy Thẻ Loại đơn [" + loaiDon + "] trên màn hình.");
        }

        // --- CHỐT CHẶN 2: Kiểm tra thẻ có bị khóa không ---
        String classAttribute = driver.findElement(theLoaiDon).getAttribute("class");
        if (classAttribute != null && classAttribute.contains("opacity-70")) {
            // ĐÃ ĐỔI THÀNH RUNTIME EXCEPTION
            throw new RuntimeException("❌ Lỗi: Thẻ Loại đơn [" + loaiDon + "] đang bị khóa (Chỉ dành cho cơ quan)!");
        }
        // -----------------------------------------------------------------

        // 1. Chọn thẻ Loại đơn
        webUI.clickElement(theLoaiDon, "Thẻ Loại đơn: [" + loaiDon + "]");

        // 2. Chọn Loại việc cụ thể
        webUI.selectDropdownWithCheck(btnDropdownLoaiViec, listOptionsLoaiViec, loaiViecCuThe, "Dropdown [Loại việc cụ thể]");

        // 3. Chọn Tòa án nhận đơn
        webUI.selectToaAnWithCheck(btnDropdownToaAn, inputSearchToaAn, listOptionsToaAn, toaAn, "Dropdown [Tòa án nhận đơn]");

        // 4. Nhập Tóm tắt yêu cầu
        webUI.setTextWithCheck(txtTomTat, tomTat, "Ô nhập [Tóm tắt sơ bộ yêu cầu]");
    }

    public void clickTiepTheo() {
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo]");
    }
}

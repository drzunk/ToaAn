package vn.tuphap.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.ui.LoaiDonLocator;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

public class TaoDonPage {
    private WebDriver driver;
    private WebUI webUI;

    // --- KHAI BÁO LOCATORS ---
    private By btnDropdownLoaiViec = By.xpath(
            "//button[contains(., 'Chọn loại việc') or contains(., 'loại việc cụ thể')]");
    private By listOptionsLoaiViec = By.xpath(
            "//button[contains(., 'Chọn loại việc') or contains(., 'loại việc cụ thể')]"
                    + "/following-sibling::div//div[@role='option']"
                    + " | //div[@role='listbox']//div[@role='option']");

    private By btnDropdownToaAn = By.xpath(
            "//button[contains(., 'Chọn tòa án nhận đơn') or contains(., 'tòa án nhận đơn')]");
    private By inputSearchToaAn = By.xpath(
            "(//button[contains(., 'Chọn tòa án nhận đơn') or contains(., 'tòa án nhận đơn')])[1]"
                    + "/following-sibling::div//input[contains(@placeholder, 'Tìm kiếm')]");
    private By listOptionsToaAn = By.xpath(
            "(//button[contains(., 'Chọn tòa án nhận đơn') or contains(., 'tòa án nhận đơn')])[1]"
                    + "/following-sibling::div//div[@role='option']");

    private By txtTomTat = By.xpath("//textarea[@placeholder='Mô tả ngắn gọn nội dung tranh chấp (tuỳ chọn, có thể bổ sung sau)']");
    private By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");
    private By btnBatDauMoi = By.xpath("//button[contains(., 'Bắt đầu mới')]");

    /** Marker bước 1 — chọn loại đơn / loại việc / tòa án. */
    public static final By MARKER_BUOC1 = By.xpath(
            "//button[contains(., 'Chọn loại việc') or contains(., 'loại việc cụ thể')]"
                    + " | //textarea[contains(@placeholder, 'Mô tả ngắn gọn')]");

    private By getCardLoaiDon(String tenLoaiDon) {
        return LoaiDonLocator.card(tenLoaiDon);
    }

    public TaoDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    /** Nếu còn đơn nháp, chọn bắt đầu mới để biểu mẫu bước 1 sạch. */
    public void boQuaNhapNeuCo() {
        if (webUI.existsNow(btnBatDauMoi)) {
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
            webUI.clickElement(btnBatDauMoi, "Nút [Bắt đầu mới] (bỏ đơn nháp)");
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        }
    }

    // --- CÁC HÀM NGHIỆP VỤ ---
    public void dienFormBuoc1(String loaiDon, String loaiViecCuThe, String toaAn, String tomTat) {
        dienFormBuoc1(loaiDon, loaiViecCuThe, toaAn, tomTat, true);
    }

    /**
     * @param boQuaDonNhap {@code true} khi nộp đơn mới (bấm Bắt đầu mới nếu có nháp);
     *                     {@code false} khi tiếp tục sau [Chỉnh sửa] từ Xem lại — giữ đơn hiện tại.
     */
    public void dienFormBuoc1(String loaiDon, String loaiViecCuThe, String toaAn, String tomTat,
                              boolean boQuaDonNhap) {
        if (boQuaDonNhap) {
            boQuaNhapNeuCo();
        }

        By theLoaiDon = getCardLoaiDon(loaiDon);

        if (!webUI.isElementVisible(theLoaiDon)) {
            throw new RuntimeException("❌ Lỗi dữ liệu: Không tìm thấy Thẻ Loại đơn [" + loaiDon + "] trên màn hình.");
        }

        String classAttribute = driver.findElement(theLoaiDon).getAttribute("class");
        if (classAttribute != null && classAttribute.contains("opacity-70")) {
            throw new RuntimeException("❌ Lỗi: Thẻ Loại đơn [" + loaiDon + "] đang bị khóa (Chỉ dành cho cơ quan)!");
        }

        webUI.clickElement(theLoaiDon, "Thẻ Loại đơn: [" + loaiDon + "]");

        // Phá sản không có danh sách thả xuống loại việc trên UI
        if (DataDictionary.hasLoaiViecDropdown(loaiDon)) {
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
            webUI.selectDropdownWithCheck(btnDropdownLoaiViec, listOptionsLoaiViec, loaiViecCuThe,
                    "Dropdown [Loại việc cụ thể]");
        } else {
            System.out.println(" ⏩ Bỏ qua Dropdown loại việc — loại đơn [" + loaiDon
                    + "] không có trên biểu mẫu (nhãn danh mục: " + loaiViecCuThe + ").");
            TestActionLog.boQua("Dropdown [Loại việc cụ thể]",
                    "Loại đơn [" + loaiDon + "] không có trên biểu mẫu");
            webUI.waitUntilVisible(btnDropdownToaAn, WaitConfig.DROPDOWN, "Dropdown [Tòa án nhận đơn]");
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        }

        webUI.selectToaAnWithCheck(btnDropdownToaAn, inputSearchToaAn, listOptionsToaAn, toaAn,
                "Dropdown [Tòa án nhận đơn]");

        webUI.setTextWithCheck(txtTomTat, tomTat, "Ô nhập [Tóm tắt sơ bộ yêu cầu]");
    }

    /** Điền phần còn lại bước 1 khi loại đơn + loại việc đã chọn sẵn (vd. sau scrape catalog). */
    public void dienToaAnVaTomTat(String toaAn, String tomTat) {
        webUI.waitUntilVisible(btnDropdownToaAn, WaitConfig.DROPDOWN, "Dropdown [Tòa án nhận đơn]");
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        webUI.selectToaAnWithCheck(btnDropdownToaAn, inputSearchToaAn, listOptionsToaAn, toaAn,
                "Dropdown [Tòa án nhận đơn]");
        webUI.setTextWithCheck(txtTomTat, tomTat, "Ô nhập [Tóm tắt sơ bộ yêu cầu]");
    }

    public void waitForBuoc1Ready() {
        webUI.waitUntilVisible(btnDropdownToaAn, WaitConfig.DROPDOWN, "Dropdown [Tòa án nhận đơn]");
    }

    public void clickTiepTheo() {
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo]");
    }
}

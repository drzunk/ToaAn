package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.DataDictionary;
import utils.WebUI;

public class NoiDungDonPage {
    private final WebDriver driver;
    private final WebUI webUI;

    private static final String NOIDUNG_SECTION =
            "//h2[contains(., 'Nội dung đơn')]/parent::div";

    private final By stepReadyMarker = By.xpath(NOIDUNG_SECTION
            + "//label[contains(., 'Thời điểm phát sinh')]");
    private final By txtThoiDiemPhatSinh = By.xpath(NOIDUNG_SECTION
            + "//label[contains(., 'Thời điểm phát sinh')]/following-sibling::div//input");
    private final By txtGiaTriTranhChap = By.xpath(NOIDUNG_SECTION
            + "//label[contains(., 'Giá trị tranh chấp')]/following-sibling::input");
    private final By txtTomTatQuaTrinh = By.xpath(NOIDUNG_SECTION
            + "//label[contains(., 'Tóm tắt quá trình sự việc')]/following-sibling::textarea");
    private final By txtYeuCauCuThe = By.xpath(NOIDUNG_SECTION
            + "//label[contains(., 'Yêu cầu cụ thể')]/following-sibling::textarea");
    private final By txtCanCuPhapLy = By.xpath(NOIDUNG_SECTION
            + "//label[contains(., 'Căn cứ pháp lý')]/following-sibling::textarea");
    private final By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    public NoiDungDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void waitStepReady() {
        webUI.waitUntilVisible(stepReadyMarker, 15, "Bước 4 [Nội dung đơn]");
    }

    public void dienForm(String loaiDon,
                         String thoiDiemPhatSinh,
                         String giaTriTranhChap,
                         String tomTatQuaTrinh,
                         String yeuCauCuThe,
                         String canCuPhapLy) {
        waitStepReady();

        webUI.setTextForMaskedInput(
                txtThoiDiemPhatSinh, thoiDiemPhatSinh, "Ô nhập [Thời điểm phát sinh vụ việc]");

        if (DataDictionary.hasGiaTriTranhChap(loaiDon) && webUI.isElementVisible(txtGiaTriTranhChap)) {
            boolean required = DataDictionary.isGiaTriTranhChapRequired(loaiDon);
            if (required) {
                webUI.setTextWithCheck(txtGiaTriTranhChap, giaTriTranhChap, "Ô nhập [Giá trị tranh chấp (VNĐ)]");
            } else if (giaTriTranhChap != null && !giaTriTranhChap.trim().isEmpty()) {
                webUI.setTextWithCheck(txtGiaTriTranhChap, giaTriTranhChap, "Ô nhập [Giá trị tranh chấp (VNĐ)]");
            } else {
                System.out.println(" ⏩ Bỏ qua [Giá trị tranh chấp] — không bắt buộc với loại đơn này.");
            }
        }

        webUI.setTextWithCheck(txtTomTatQuaTrinh, tomTatQuaTrinh, "Ô nhập [Tóm tắt quá trình sự việc]");
        webUI.setTextWithCheck(txtYeuCauCuThe, yeuCauCuThe, "Ô nhập [Yêu cầu cụ thể]");
        webUI.setTextWithCheck(txtCanCuPhapLy, canCuPhapLy, "Ô nhập [Căn cứ pháp lý]");
    }

    public void clickTiepTheo() {
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước 4");
    }
}

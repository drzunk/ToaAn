package vn.tuphap.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.report.TaoDonReportBuilder;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.TestFileHelper;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.List;
import java.util.Locale;

public class NoiDungDonPage {
    private final WebDriver driver;
    private final WebUI webUI;

    /** Loại biểu mẫu bước 4 — iframe nhúng, tải file, hoặc form textarea legacy. */
    public enum Step4Mode {
        IFRAME, UPLOAD, LEGACY
    }

    private static final String NOIDUNG_SECTION =
            "//h2[contains(., 'Nội dung đơn')]/parent::div";

    /** Wizard bước 4 — loại trừ iframe/text xem trước trên màn Xem lại. */
    private static final String WIZARD_NOIDUNG_SECTION =
            "//h2[contains(., 'Nội dung đơn')]/parent::div"
                    + "[not(ancestor::div[.//h2[contains(., 'Xem lại')]])]";

    /** Iframe biểu mẫu động bước 4 — chỉ trong wizard, không lấy iframe PDF trên Xem lại. */
    public static final By IFRAME_NOI_DUNG = By.xpath(
            WIZARD_NOIDUNG_SECTION + "//iframe[contains(@src,'/f/')]"
                    + " | //iframe[@data-testid='noi-dung-don-iframe']");

    public static final By MARKER_IFRAME_READY = By.cssSelector(".pf-wrap .pf-card");

    private static final By PF_UNPUBLISHED = By.xpath(
            "//*[contains(@class,'pf-wrap')]//h2[contains(., 'Biểu mẫu không tồn tại')"
                    + " or contains(., 'chưa được xuất bản')]");

    private static final By FILE_INPUT_NOI_DUNG = By.xpath(
            NOIDUNG_SECTION + "//input[@type='file']"
                    + " | //h2[contains(., 'Nội dung đơn')]/parent::div//input[@type='file']"
                    + " | //label[contains(., 'Tải lên') and ancestor::div[.//h2[contains(., 'Nội dung')]]]"
                    + "//input[@type='file']"
                    + " | //h2[contains(., 'Nội dung đơn')]/following::input[@type='file'][1]");

    public static final By MARKER_UPLOAD_READY = FILE_INPUT_NOI_DUNG;

    public static final By MARKER_STEP_READY = By.xpath(WIZARD_NOIDUNG_SECTION
            + "//label[contains(., 'Thời điểm phát sinh')]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//label[contains(., 'Thời điểm phát sinh vụ việc')]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//label[contains(., 'Yêu cầu cụ thể')]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//textarea[ancestor::div[.//label[contains(., 'Yêu cầu cụ thể')]]]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//iframe[contains(@src,'/f/')]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//input[@type='file']");

    private final By stepReadyMarker = MARKER_STEP_READY;
    private final By btnTiepTheoWizard = By.xpath("//button[contains(., 'Tiếp theo')]");
    private final By btnTiepTheoIframe = By.xpath(
            "//div[contains(@class,'pf-nav')]//button[contains(., 'Tiếp theo')]"
                    + " | //div[contains(@class,'pf-wrap')]//button[contains(., 'Tiếp theo')]");

    private Step4Mode step4Mode = Step4Mode.LEGACY;

    public NoiDungDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public Step4Mode getStep4Mode() {
        return step4Mode;
    }

    public boolean isIframeMode() {
        return step4Mode == Step4Mode.IFRAME;
    }

    public boolean isUploadMode() {
        return step4Mode == Step4Mode.UPLOAD;
    }

    public void waitStepReady() {
        step4Mode = resolveStep4Mode();
        logStep4Mode();
        webUI.switchToDefaultContent();
        switch (step4Mode) {
            case IFRAME -> waitIframeFormReady();
            case UPLOAD -> webUI.waitUntilVisible(MARKER_UPLOAD_READY, WaitConfig.STEP,
                    "Bước 4 [Nội dung đơn — tải file]");
            case LEGACY -> webUI.waitUntilVisible(stepReadyMarker, WaitConfig.STEP,
                    "Bước 4 [Nội dung đơn]");
            default -> throw new IllegalStateException("Step4Mode không xác định");
        }
    }

    /**
     * Phân loại bước 4:
     * 1. Có iframe /f/ → nếu báo lỗi chưa xuất bản thì dừng ngay; ngược lại điền trong iframe.
     * 2. Không có iframe hợp lệ nhưng có input file → tải file lên.
     * 3. Còn lại → form legacy (textarea).
     */
    private Step4Mode resolveStep4Mode() {
        webUI.switchToDefaultContent();
        if (webUI.existsNow(IFRAME_NOI_DUNG)) {
            webUI.switchToIframe(IFRAME_NOI_DUNG);
            try {
                if (webUI.existsNow(PF_UNPUBLISHED)) {
                    String msg = readUnpublishedMessage();
                    webUI.switchToDefaultContent();
                    webUI.failStepWithSystemFeedback(4, TaoDonReportBuilder.tenBuocDayDu(4),
                            "Iframe biểu mẫu nhúng báo lỗi", List.of(msg));
                }
                return Step4Mode.IFRAME;
            } finally {
                webUI.switchToDefaultContent();
            }
        }
        if (webUI.existsNow(FILE_INPUT_NOI_DUNG)) {
            return Step4Mode.UPLOAD;
        }
        return Step4Mode.LEGACY;
    }

    private void waitIframeFormReady() {
        webUI.switchToIframe(IFRAME_NOI_DUNG);
        try {
            if (webUI.existsNow(PF_UNPUBLISHED)) {
                String msg = readUnpublishedMessage();
                webUI.switchToDefaultContent();
                webUI.failStepWithSystemFeedback(4, TaoDonReportBuilder.tenBuocDayDu(4),
                        "Iframe biểu mẫu nhúng báo lỗi", List.of(msg));
            }
            webUI.waitUntilVisible(MARKER_IFRAME_READY, WaitConfig.STEP,
                    "Bước 4 [Nội dung đơn — iframe biểu mẫu]");
        } finally {
            webUI.switchToDefaultContent();
        }
    }

    private String readUnpublishedMessage() {
        try {
            WebElement el = driver.findElement(PF_UNPUBLISHED);
            String text = el.getText();
            if (text != null && !text.isBlank()) {
                return text.trim().replaceAll("\\s+", " ");
            }
        } catch (Exception ignored) {
        }
        return "Biểu mẫu không tồn tại hoặc chưa được xuất bản";
    }

    private void logStep4Mode() {
        String label = switch (step4Mode) {
            case IFRAME -> "Biểu mẫu nhúng (iframe)";
            case UPLOAD -> "Tải file nội dung đơn";
            case LEGACY -> "Form nhập liệu (legacy)";
        };
        System.out.println(" ℹ Bước 4 — chế độ: " + label);
        TestActionLog.chon("Bước 4 — loại biểu mẫu", label);
    }

    public void dienForm(String loaiDon,
                         String thoiDiemPhatSinh,
                         String giaTriTranhChap,
                         String tomTatQuaTrinh,
                         String yeuCauCuThe,
                         String canCuPhapLy) {
        waitStepReady();
        if (step4Mode == Step4Mode.UPLOAD) {
            uploadNoiDungFile();
            return;
        }
        if (step4Mode == Step4Mode.IFRAME) {
            fillEmbeddedIframeForm(loaiDon, thoiDiemPhatSinh, giaTriTranhChap,
                    tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy);
            return;
        }
        dienFormLegacyOrIframe(loaiDon, thoiDiemPhatSinh, giaTriTranhChap,
                tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy);
    }

    /**
     * Điền biểu mẫu nhúng trong iframe (pf-wrap) — legacy textarea không có trên DOM chính.
     */
    private void fillEmbeddedIframeForm(String loaiDon,
                                        String thoiDiemPhatSinh,
                                        String giaTriTranhChap,
                                        String tomTatQuaTrinh,
                                        String yeuCauCuThe,
                                        String canCuPhapLy) {
        webUI.switchToIframe(IFRAME_NOI_DUNG);
        try {
            fillIframeVisibleFields(loaiDon, thoiDiemPhatSinh, giaTriTranhChap,
                    tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy);
            advanceIframeInternalSteps();
        } finally {
            webUI.switchToDefaultContent();
        }
    }

    /** Điền các trường hiển thị trong iframe — ưu tiên label quen thuộc, fallback input/textarea trống. */
    private void fillIframeVisibleFields(String loaiDon,
                                         String thoiDiemPhatSinh,
                                         String giaTriTranhChap,
                                         String tomTatQuaTrinh,
                                         String yeuCauCuThe,
                                         String canCuPhapLy) {
        webUI.setTextForMaskedInput(
                txtThoiDiemPhatSinh(), thoiDiemPhatSinh, "Ô nhập [Thời điểm phát sinh vụ việc] (iframe)");
        if (DataDictionary.hasGiaTriTranhChap(loaiDon) && webUI.isElementVisible(txtGiaTriTranhChap())) {
            webUI.setTextWithCheck(txtGiaTriTranhChap(), giaTriTranhChap, "Ô nhập [Giá trị tranh chấp] (iframe)");
        }
        webUI.setTextWithCheck(txtTomTatQuaTrinh(), tomTatQuaTrinh, "Ô nhập [Tóm tắt quá trình sự việc] (iframe)");
        webUI.setTextWithCheck(txtYeuCauCuThe(), yeuCauCuThe, "Ô nhập [Yêu cầu cụ thể] (iframe)");
        webUI.setTextWithCheck(txtCanCuPhapLy(), canCuPhapLy, "Ô nhập [Căn cứ pháp lý] (iframe)");
        fillGenericIframeInputs();
    }

    /** Fallback: điền input/textarea trống còn lại trong .pf-wrap. */
    private void fillGenericIframeInputs() {
        List<WebElement> fields = driver.findElements(By.cssSelector(
                ".pf-wrap input:not([type='hidden']):not([type='file']):not([type='checkbox']):not([type='radio']),"
                        + " .pf-wrap textarea"));
        for (WebElement field : fields) {
            try {
                if (!field.isDisplayed() || !field.isEnabled()) {
                    continue;
                }
                String val = field.getAttribute("value");
                if (val != null && !val.isBlank()) {
                    continue;
                }
                String tag = field.getTagName().toLowerCase(Locale.ROOT);
                if ("textarea".equals(tag)) {
                    field.sendKeys("Nội dung kiểm thử tự động.");
                } else {
                    String type = field.getAttribute("type");
                    if (type != null && type.equalsIgnoreCase("date")) {
                        field.sendKeys("01/01/2024");
                    } else {
                        field.sendKeys("Kiểm thử");
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /** Bấm Tiếp theo bên trong iframe nếu biểu mẫu nhiều bước. */
    private void advanceIframeInternalSteps() {
        for (int step = 0; step < 6; step++) {
            if (!webUI.isElementVisible(btnTiepTheoIframe) || !webUI.isElementEnabled(btnTiepTheoIframe)) {
                break;
            }
            fillGenericIframeInputs();
            webUI.clickElement(btnTiepTheoIframe, "Nút [Tiếp theo] trong iframe bước 4", WaitConfig.FIELD);
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        }
    }

    /** Tải file nội dung đơn (PDF / Excel / Word ngẫu nhiên). */
    public void uploadNoiDungFile() {
        if (step4Mode != Step4Mode.UPLOAD) {
            step4Mode = resolveStep4Mode();
            if (step4Mode != Step4Mode.UPLOAD) {
                throw new IllegalStateException("Bước 4 không ở chế độ tải file.");
            }
        }
        webUI.switchToDefaultContent();
        webUI.waitUntilVisible(MARKER_UPLOAD_READY, WaitConfig.FIELD, "Input [Tải nội dung đơn]");
        String filePath = TestFileHelper.pickRandomUploadFile();
        webUI.uploadFile(FILE_INPUT_NOI_DUNG, filePath, "Tải lên [Nội dung đơn]");
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
    }

    private void dienFormLegacyOrIframe(String loaiDon,
                                        String thoiDiemPhatSinh,
                                        String giaTriTranhChap,
                                        String tomTatQuaTrinh,
                                        String yeuCauCuThe,
                                        String canCuPhapLy) {
        webUI.setTextForMaskedInput(
                txtThoiDiemPhatSinh(), thoiDiemPhatSinh, "Ô nhập [Thời điểm phát sinh vụ việc]");

        if (DataDictionary.hasGiaTriTranhChap(loaiDon) && webUI.isElementVisible(txtGiaTriTranhChap())) {
            boolean required = DataDictionary.isGiaTriTranhChapRequired(loaiDon);
            if (required) {
                webUI.setTextWithCheck(txtGiaTriTranhChap(), giaTriTranhChap,
                        "Ô nhập [Giá trị tranh chấp (VNĐ)]");
            } else if (giaTriTranhChap != null && !giaTriTranhChap.trim().isEmpty()) {
                webUI.setTextWithCheck(txtGiaTriTranhChap(), giaTriTranhChap,
                        "Ô nhập [Giá trị tranh chấp (VNĐ)]");
            } else {
                System.out.println(" ⏩ Bỏ qua [Giá trị tranh chấp] — không bắt buộc với loại đơn này.");
                TestActionLog.boQua("Giá trị tranh chấp (VNĐ)", "Không bắt buộc với loại đơn này");
            }
        }

        webUI.setTextWithCheck(txtTomTatQuaTrinh(), tomTatQuaTrinh, "Ô nhập [Tóm tắt quá trình sự việc]");
        webUI.setTextWithCheck(txtYeuCauCuThe(), yeuCauCuThe, "Ô nhập [Yêu cầu cụ thể]");
        webUI.setTextWithCheck(txtCanCuPhapLy(), canCuPhapLy, "Ô nhập [Căn cứ pháp lý]");
    }

    public void clickTiepTheo() {
        if (step4Mode == Step4Mode.IFRAME) {
            webUI.switchToIframe(IFRAME_NOI_DUNG);
            try {
                if (webUI.isElementVisible(btnTiepTheoIframe) && webUI.isElementEnabled(btnTiepTheoIframe)) {
                    webUI.clickElement(btnTiepTheoIframe, "Nút [Tiếp theo] ở Bước 4 (iframe)", WaitConfig.FIELD);
                }
            } finally {
                webUI.switchToDefaultContent();
            }
            webUI.clickElement(btnTiepTheoWizard, "Nút [Tiếp theo] ở Bước 4", WaitConfig.STEP);
            return;
        }
        webUI.clickElement(btnTiepTheoWizard, "Nút [Tiếp theo] ở Bước 4");
    }

    private String scope() {
        if (step4Mode == Step4Mode.IFRAME) {
            return "//div[contains(@class,'pf-wrap')]";
        }
        return "(" + WIZARD_NOIDUNG_SECTION + " | " + NOIDUNG_SECTION + ")";
    }

    private By fieldInScope(String relativeXPath) {
        return By.xpath(scope() + relativeXPath);
    }

    private By txtThoiDiemPhatSinh() {
        return By.xpath(scope()
                + "//label[contains(., 'Thời điểm phát sinh')]/following-sibling::div//input"
                + " | " + scope() + "//label[contains(., 'Thời điểm phát sinh')]/following-sibling::input"
                + " | " + scope() + "//input[@placeholder and (contains(@placeholder, 'Thời điểm')"
                + " or contains(@placeholder, 'dd/mm') or contains(@placeholder, 'DD/MM'))]");
    }

    private By txtGiaTriTranhChap() {
        return fieldInScope("//label[contains(., 'Giá trị tranh chấp')]/following-sibling::input"
                + " | //label[contains(., 'Giá trị tranh chấp')]/parent::div//input");
    }

    private By txtTomTatQuaTrinh() {
        return fieldInScope("//label[contains(., 'Tóm tắt quá trình sự việc')]/following-sibling::textarea"
                + " | //label[contains(., 'Tóm tắt quá trình sự việc')]/parent::div//textarea");
    }

    private By txtYeuCauCuThe() {
        return fieldInScope("//label[contains(., 'Yêu cầu cụ thể')]/following-sibling::textarea"
                + " | //label[contains(., 'Yêu cầu cụ thể')]/parent::div//textarea");
    }

    private By txtCanCuPhapLy() {
        return fieldInScope("//label[contains(., 'Căn cứ pháp lý')]/following-sibling::textarea"
                + " | //label[contains(., 'Căn cứ pháp lý')]/parent::div//textarea");
    }
}

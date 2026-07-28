package vn.tuphap.automation.pages;

import vn.tuphap.automation.report.TestActionLog;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class XemLaiGuiDonPage {
    private final WebDriver driver;
    private final WebUI webUI;

    /** Card bước 6 — tránh khớp label/checkbox ở vùng khác trên trang. */
    private static final String REVIEW_CARD =
            "//h2[contains(normalize-space(.), 'Xem lại')]/ancestor::div[contains(@class,'ds-card')][1]";

    private static final String REVIEW_SECTION =
            REVIEW_CARD
                    + " | //h2[contains(., 'Xem lại')]/parent::div"
                    + " | //h2[contains(., 'Gửi đơn') and (contains(., 'Xem') or contains(., 'xem'))]/parent::div";

    /** Marker bước 6 — dùng chung TaoDonFlow. */
    public static final By MARKER_STEP_READY = By.xpath(
            REVIEW_SECTION + "//span[contains(., 'Loại đơn') or contains(., 'Nguyên đơn')]"
                    + " | " + REVIEW_SECTION + "//label[contains(., 'Tôi xác nhận')]"
                    + " | //button[contains(., 'Gửi đơn')]/ancestor::div[.//h2[contains(., 'Xem lại')]][1]"
                    + " | //div[contains(@class,'rounded') and contains(@class,'border')]"
                    + "[.//span[contains(., 'Loại đơn')] and .//button[contains(., 'Chỉnh sửa')]]");

    private final By stepReadyMarker = MARKER_STEP_READY;
    private final By chkXacNhan = By.xpath(
            REVIEW_CARD + "//label[contains(., 'Tôi xác nhận')]//input[@type='checkbox']");
    private final By lblXacNhan = By.xpath(
            REVIEW_CARD + "//label[contains(., 'Tôi xác nhận')]");
    private final By btnGuiDon = By.xpath("//button[contains(., 'Gửi đơn')]");

    /** Toast/notify góc màn hình — ưu tiên lấy message sạch từ đây. */
    private static final List<By> TOAST_SELECTORS = List.of(
            By.cssSelector(".ant-notification-notice"),
            By.cssSelector(".ant-message-notice"),
            By.cssSelector(".ant-message-notice-content"),
            By.cssSelector(".Toastify__toast"),
            By.cssSelector("[data-sonner-toast]"),
            By.cssSelector("[role='alert']"),
            By.cssSelector("[role='status']"),
            By.xpath("//div[(contains(@class,'toast') or contains(@class,'notification')"
                    + " or contains(@class,'Notification') or contains(@class,'notify'))"
                    + " and string-length(normalize-space(.)) > 0]")
    );

    /** Fallback text-based (khi UI toast không khớp selector thư viện). */
    private final By successHint = By.xpath(
            "//*[contains(., 'Gửi đơn thành công') or contains(., 'Nộp đơn thành công')"
                    + " or contains(., 'gửi đơn thành công') or contains(., 'nộp đơn thành công')]");
    private final By errorHint = By.xpath(
            "//*[contains(., 'thất bại') or contains(., 'Thất bại')"
                    + " or contains(., 'không thành công') or contains(., 'Có lỗi')"
                    + " or contains(., 'lỗi hệ thống') or contains(., 'Lỗi hệ thống')"
                    + " or contains(., 'vui lòng thử lại') or contains(., 'Vui lòng thử lại')]");

    /** Trần chờ toast sau Gửi đơn. Override: -Dtaodon.submit.timeoutSec */
    public static int submitTimeoutSec() {
        return WaitConfig.submitTimeoutSec();
    }

    /** Các mục trên màn Xem lại — nút Chỉnh sửa quay về bước tương ứng. */
    public static final String MUC_LOAI_DON = "Loại đơn";
    public static final String MUC_NGUYEN_DON = "Nguyên đơn";
    public static final String MUC_BI_DON = "Bị đơn";
    public static final String MUC_NOI_DUNG = "Nội dung";
    public static final String MUC_NOI_DUNG_DON = "Nội dung đơn";
    /** UI mới: mục xem trước PDF đơn — nút Chỉnh sửa quay về bước Nội dung. */
    public static final String MUC_XEM_TRUOC_DON = "Xem trước đơn";
    public static final String MUC_TAI_LIEU = "Tài liệu";
    public static final String MUC_DANH_SACH_TAI_LIEU = "Danh sách tài liệu";

    public XemLaiGuiDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void waitStepReady() {
        webUI.waitUntilVisible(stepReadyMarker, WaitConfig.STEP, "Bước 6 [Xem lại & Gửi đơn]");
    }

    private By chinhSuaTrongMuc(String tenMuc) {
        String titleMatch = tieuDeMucMatch(tenMuc);
        return By.xpath(REVIEW_SECTION
                + "//div[contains(@class, 'border') and contains(@class, 'rounded')"
                + " and (.//span[" + titleMatch + "]"
                + " or .//*[self::span or self::h3 or self::p][contains(@class, 'font-bold') and (" + titleMatch + ")]"
                + " or .//*[self::span or self::h3][contains(normalize-space(.), '" + tenMuc + "')])"
                + " and .//button[contains(normalize-space(.), 'Chỉnh sửa')]]"
                + "//button[contains(normalize-space(.), 'Chỉnh sửa')]");
    }

    /** Marker bước 4 wizard — có Tiếp theo (không phải summary read-only trên Xem lại). */
    private static final By MARKER_BUOC4_WIZARD_FORM = By.xpath(
            "//div[.//h2[contains(normalize-space(.), 'Nội dung đơn')]"
                    + " and .//button[contains(normalize-space(.), 'Tiếp theo')]]"
                    + "//label[contains(., 'Yêu cầu cụ thể') or contains(., 'Thời điểm phát sinh')]"
                    + " | //div[.//h2[contains(normalize-space(.), 'Nội dung đơn')]"
                    + " and .//button[contains(normalize-space(.), 'Tiếp theo')]]//textarea");

    /** Khớp tiêu đề card — tránh nhầm text "nội dung đơn" trong checkbox / summary. */
    private static String tieuDeMucMatch(String tenMuc) {
        if ("Danh sách tài liệu".equals(tenMuc)) {
            return "starts-with(normalize-space(.), '" + tenMuc + "')";
        }
        if ("Xem trước đơn".equals(tenMuc)) {
            return "starts-with(normalize-space(.), '" + tenMuc + "')";
        }
        if ("Nội dung đơn".equals(tenMuc)) {
            return "normalize-space(.) = 'Nội dung đơn'"
                    + " or normalize-space(.) = 'Nội dung đơn *'"
                    + " or starts-with(normalize-space(.), 'Nội dung đơn (')";
        }
        if ("Nội dung".equals(tenMuc)) {
            return "(normalize-space(.) = 'Nội dung'"
                    + " or normalize-space(.) = 'Nội dung *'"
                    + " or starts-with(normalize-space(.), 'Nội dung ('))"
                    + " and not(contains(normalize-space(.), 'Nội dung đơn'))";
        }
        return "normalize-space(.) = '" + tenMuc + "'"
                + " or normalize-space(.) = '" + tenMuc + " *'"
                + " or starts-with(normalize-space(.), '" + tenMuc + " (')";
    }

    /**
     * Bấm [Chỉnh sửa] tại một mục trên màn Xem lại và chờ biểu mẫu bước tương ứng hiển thị.
     */
    public void clickChinhSua(String tenMuc, By stepMarker, String moTaBuoc) {
        clickChinhSuaFirstAvailable(List.of(tenMuc), stepMarker, moTaBuoc);
    }

    /** Thử lần lượt các tên mục (UI cũ / mới) cho đến khi thấy nút Chỉnh sửa. */
    private void clickChinhSuaFirstAvailable(List<String> tenMucCandidates, By stepMarker, String moTaBuoc) {
        waitStepReady();
        String matched = null;
        By btnChinhSua = null;
        for (String tenMuc : tenMucCandidates) {
            By candidate = chinhSuaTrongMuc(tenMuc);
            if (webUI.isElementVisible(candidate)) {
                matched = tenMuc;
                btnChinhSua = candidate;
                break;
            }
        }
        if (btnChinhSua == null) {
            throw new RuntimeException("❌ Không thấy nút [Chỉnh sửa] tại các mục: " + tenMucCandidates + ".");
        }
        WebElement btnEl = driver.findElement(btnChinhSua);
        clickChinhSuaElement(btnEl, "Nút [Chỉnh sửa] — " + matched);
        webUI.waitUntilVisible(stepMarker, WaitConfig.STEP, moTaBuoc);
    }

    /** Quay lại bước 1 — Loại đơn. */
    public void clickChinhSuaLoaiDon() {
        clickChinhSua(MUC_LOAI_DON,
                By.xpath("//button[contains(., 'Chọn loại việc') or contains(., 'loại việc cụ thể')"
                        + " or contains(., 'Chọn tòa án nhận đơn')]"),
                "Bước 1 [Loại đơn]");
    }

    /** Quay lại bước 2 — Nguyên đơn. */
    public void clickChinhSuaNguyenDon() {
        clickChinhSua(MUC_NGUYEN_DON,
                By.xpath("//label[contains(., 'Họ và tên') or contains(., 'Tên tổ chức')]"),
                "Bước 2 [Nguyên đơn]");
    }

    /** Quay lại bước 3 — Bị đơn / Người bị yêu cầu / Cơ quan bị kiện. */
    public void clickChinhSuaBiDon() {
        clickChinhSua(MUC_BI_DON,
                By.xpath("//button[contains(., 'Thêm bị đơn') or contains(., 'Thêm người bị yêu cầu')"
                        + " or contains(., 'Thêm người bị kiện') or contains(., 'Thêm người được yêu cầu')]"
                        + " | //span[contains(., 'Người yêu cầu 2')]"
                        + " | //label[contains(., 'Tên cơ quan')]"
                        + " | //label[contains(., 'Năm sinh') or contains(., 'Tên tổ chức')]"),
                "Bước 3 [Bị đơn]");
    }

    /** Quay lại bước 4 — Nội dung đơn (UI cũ / mới / thanh tiến trình wizard). */
    public void clickChinhSuaNoiDung() {
        waitStepReady();
        dismissUiOverlays();

        try {
            clickChinhSuaFirstAvailable(
                    List.of(MUC_NOI_DUNG_DON, MUC_NOI_DUNG),
                    MARKER_BUOC4_WIZARD_FORM,
                    "Bước 4 [Nội dung đơn]");
            return;
        } catch (RuntimeException fromCard) {
            System.out.println(" ℹ Chỉnh sửa card chưa mở wizard — thử điều hướng bước 4...");
            if (!bamChinhSuaNoiDung()) {
                moBuoc4TuChinhSuaTheoThuTu(3, 4);
            }
            if (!dieuHuongWizardBuoc4Aggressive()) {
                throw fromCard;
            }
            webUI.waitUntilVisible(MARKER_BUOC4_WIZARD_FORM, WaitConfig.STEP,
                    "Bước 4 [Nội dung đơn]");
        }
    }

    /** Bấm Chỉnh sửa tại mục Nội dung đơn hoặc Nội dung (không chờ form — dùng kèm điều hướng wizard). */
    private boolean bamChinhSuaNoiDung() {
        for (String muc : List.of(MUC_NOI_DUNG_DON, MUC_NOI_DUNG)) {
            By btn = chinhSuaTrongMuc(muc);
            if (!webUI.isElementVisible(btn)) {
                continue;
            }
            clickChinhSuaElement(driver.findElement(btn), "Nút [Chỉnh sửa] — " + muc);
            return true;
        }
        return false;
    }

    /** Thử mọi nút stepper / tab / số bước 4 trên trang (ngoài card Xem lại). */
    private boolean dieuHuongWizardBuoc4Aggressive() {
        List<By> step4Nav = List.of(
                By.xpath("//*[contains(@class,'step') or contains(@class,'Step')]"
                        + "][contains(., 'Nội dung') or normalize-space(.)='4'"
                        + " or contains(normalize-space(.), 'Bước 4')]"
                        + "[not(contains(., 'Chỉnh sửa'))]"),
                By.xpath("//button[normalize-space(.)='4' or contains(@aria-label, 'Nội dung')"
                        + " or contains(@title, 'Nội dung')]"),
                By.xpath("//*[@role='tab' and (contains(., 'Nội dung đơn') or contains(., 'Nội dung'))]"),
                By.xpath("//nav//*[contains(., 'Nội dung') and not(contains(., 'Chỉnh sửa'))]"
                        + " | //nav//*[normalize-space(.)='4']"),
                By.xpath("//header//*[contains(., 'Nội dung đơn') or contains(., 'Nội dung')]"
                        + "[not(contains(., 'Chỉnh sửa'))]"),
                By.xpath("//div[contains(@class,'sticky') or contains(@class,'top-0')]"
                        + "//*[contains(., 'Nội dung đơn') and not(contains(., 'Chỉnh sửa'))]"),
                By.xpath("(//button[contains(., 'Nội dung đơn') and not(contains(., 'Chỉnh sửa'))])[1]"),
                By.xpath("//ol//li[4]//*[self::button or self::a or self::div[@role='button']]"),
                By.xpath("//*[contains(@class,'ds-step') or contains(@class,'progress')]"
                        + "//*[contains(., 'Nội dung') or normalize-space(.)='4']")
        );
        for (By by : step4Nav) {
            if (!webUI.isElementVisible(by)) {
                continue;
            }
            try {
                webUI.clickElement(by, "Stepper — bước 4 [Nội dung đơn]");
                webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
                if (isBuoc4EditReady()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        if (moBuoc4TuThanhTienTrinh()) {
            return true;
        }
        return moBuoc4TuHeaderWizard();
    }

    /** Form bước 4 đang hiển thị và có thể nhập liệu. */
    private boolean isBuoc4EditReady() {
        webUI.switchToDefaultContent();
        if (webUI.isElementVisible(MARKER_BUOC4_WIZARD_FORM)) {
            return true;
        }
        if (hasWizardStep4Form()) {
            return true;
        }
        if (webUI.isElementVisible(NoiDungDonPage.IFRAME_NOI_DUNG)) {
            webUI.switchToIframe(NoiDungDonPage.IFRAME_NOI_DUNG);
            try {
                return webUI.isElementVisible(NoiDungDonPage.MARKER_IFRAME_READY);
            } finally {
                webUI.switchToDefaultContent();
            }
        }
        return false;
    }

    private boolean hasWizardStep4Form() {
        if (!webUI.isElementVisible(By.xpath("//h2[contains(normalize-space(.), 'Nội dung đơn')]"))) {
            return false;
        }
        boolean coTiepTheo = webUI.isElementVisible(
                By.xpath("//h2[contains(., 'Nội dung đơn')]/ancestor::div[1]"
                        + "//button[contains(normalize-space(.), 'Tiếp theo')]"));
        if (!coTiepTheo) {
            return false;
        }
        return webUI.isElementVisible(MARKER_BUOC4_YEU_CAU_TEXTAREA)
                || webUI.isElementVisible(By.xpath(
                "//h2[contains(., 'Nội dung đơn')]/ancestor::div[1]"
                        + "//label[contains(., 'Thời điểm phát sinh')]"));
    }

    private static final By MARKER_BUOC4_YEU_CAU_TEXTAREA = By.xpath(
            "//div[.//h2[contains(., 'Nội dung đơn')] and .//button[contains(., 'Tiếp theo')]]"
                    + "//label[contains(., 'Yêu cầu cụ thể')]/following-sibling::textarea"
                    + " | //div[.//h2[contains(., 'Nội dung đơn')] and .//button[contains(., 'Tiếp theo')]]"
                    + "//label[contains(., 'Yêu cầu cụ thể')]/parent::div//textarea");

    private boolean hasEnabledBuoc4Field() {
        return hasWizardStep4Form();
    }

    /** @deprecated dùng {@link #isBuoc4EditReady()} */
    private boolean isWizardBuoc4Editable() {
        return isBuoc4EditReady();
    }

    /** Modal xem trước PDF — không phải form chỉnh sửa bước 4. */
    private void dismissPreviewModal() {
        List<By> closeTargets = List.of(
                By.xpath("//div[contains(@role,'dialog')]"
                        + "[.//*[contains(., 'Xem trước') or contains(., 'xem trước')]]"
                        + "//button[@aria-label='Close' or @aria-label='Đóng'"
                        + " or contains(@class,'close') or .//*[contains(@class,'lucide-x')]]"),
                By.xpath("//div[contains(@class,'fixed') and contains(@class,'inset-0')]"
                        + "[.//*[contains(., 'Xem trước')]]//button[.//*[contains(@class,'lucide-x')]]"),
                By.xpath("//button[contains(normalize-space(.), 'Đóng')"
                        + " and ancestor::div[contains(@role,'dialog') or contains(@class,'modal')]]")
        );
        for (By close : closeTargets) {
            if (webUI.isElementVisible(close)) {
                webUI.clickElement(close, "Nút [Đóng xem trước đơn]");
                webUI.sleepMillis(WaitConfig.SETTLE_MS);
                return;
            }
        }
        try {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {
        }
        webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
    }

    private void dismissUiOverlays() {
        try {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {
        }
        webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        List<By> closeButtons = List.of(
                By.xpath("//button[@aria-label='Close' or @aria-label='Đóng']"),
                By.xpath("//div[contains(@class,'dialog') or contains(@role,'dialog')]"
                        + "//button[contains(@class,'close') or .//*[contains(@class,'lucide-x')]]"),
                By.xpath("//button[.//*[contains(@class,'lucide-x')]"
                        + " and ancestor::div[contains(@class,'fixed') or contains(@role,'dialog')]]")
        );
        for (By close : closeButtons) {
            if (webUI.isElementVisible(close)) {
                webUI.clickElement(close, "Nút [Đóng overlay/dialog]");
                webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
                break;
            }
        }
    }

    private void clickChinhSuaElement(WebElement btn, String logName) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", btn);
            webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            btn.click();
        } catch (Exception e) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", btn);
        }
        System.out.println(" ➔ Click vào: [" + logName + "]");
        TestActionLog.click(logName);
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
    }

    /** Bấm nút Chỉnh sửa theo thứ tự card trên màn Xem lại (0 = Loại đơn, 3 ≈ Nội dung…). */
    private boolean moBuoc4TuChinhSuaTheoThuTu(int... zeroBasedIndices) {
        By allEdit = By.xpath(REVIEW_CARD + "//button[contains(normalize-space(.), 'Chỉnh sửa')]");
        List<WebElement> visible = new ArrayList<>();
        for (WebElement el : driver.findElements(allEdit)) {
            try {
                if (el.isDisplayed() && el.isEnabled()) {
                    visible.add(el);
                }
            } catch (Exception ignored) {
            }
        }
        for (int idx : zeroBasedIndices) {
            if (idx < 0 || idx >= visible.size()) {
                continue;
            }
            clickChinhSuaElement(visible.get(idx), "Nút [Chỉnh sửa] — card #" + (idx + 1));
            if (choHienBuoc4(WaitConfig.REVIEW_EDIT_PROBE)) {
                return true;
            }
            dismissPreviewModal();
            dismissUiOverlays();
        }
        return false;
    }

    private boolean moBuoc4TuHeaderWizard() {
        List<By> step4Nav = List.of(
                By.xpath("//*[contains(@class,'step') and contains(@class,'cursor-pointer')]"
                        + "[normalize-space(.)='4' or contains(normalize-space(.), 'Nội dung đơn')"
                        + " or contains(normalize-space(.), 'Nội dung')]"),
                By.xpath("//button[contains(@class,'step') and (normalize-space(.)='4'"
                        + " or contains(., 'Nội dung đơn') or contains(., 'Nội dung'))]"),
                By.xpath("//header//button[contains(normalize-space(.), 'Nội dung')]"
                        + " | //header//a[contains(normalize-space(.), 'Nội dung')]"),
                By.xpath("//div[contains(@class,'sticky') or contains(@class,'top-0')]"
                        + "//button[contains(normalize-space(.), 'Nội dung đơn')"
                        + " or contains(normalize-space(.), 'Nội dung')]"
                        + " | //div[contains(@class,'sticky') or contains(@class,'top-0')]"
                        + "//*[@role='tab' and contains(., 'Nội dung')]"),
                By.xpath("//*[@role='tab' and contains(., 'Nội dung đơn')]")
        );
        for (By by : step4Nav) {
            if (!webUI.isElementVisible(by)) {
                continue;
            }
            webUI.clickElement(by, "Thanh wizard — Nội dung đơn (bước 4)");
            webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
            if (isBuoc4EditReady()) {
                return true;
            }
            dismissUiOverlays();
        }
        return false;
    }

    private boolean moBuoc4TuChinhSuaMuc(List<String> mucs) {
        for (String muc : mucs) {
            By btn = chinhSuaTrongMuc(muc);
            WebElement visibleBtn = null;
            for (WebElement el : driver.findElements(btn)) {
                try {
                    if (el.isDisplayed() && el.isEnabled()) {
                        visibleBtn = el;
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            if (visibleBtn == null) {
                continue;
            }
            clickChinhSuaElement(visibleBtn, "Nút [Chỉnh sửa] — " + muc);
            if (choHienBuoc4(WaitConfig.REVIEW_EDIT_PROBE)) {
                return true;
            }
            dismissPreviewModal();
            dismissUiOverlays();
        }
        return false;
    }

    private boolean moBuoc4TuThanhTienTrinh() {
        List<By> step4Nav = List.of(
                By.xpath("//nav//button[contains(normalize-space(.), 'Nội dung')]"
                        + " | //nav//a[contains(normalize-space(.), 'Nội dung')]"
                        + " | //nav//*[normalize-space(.)='4']"),
                By.xpath("//button[contains(normalize-space(.), 'Nội dung đơn')"
                        + " and not(contains(normalize-space(.), 'Chỉnh sửa'))]"),
                By.xpath("//*[contains(@class,'step') and contains(., 'Nội dung')]//button"),
                By.xpath("//*[contains(@class,'stepper')]//*[normalize-space(.)='4']")
        );
        for (By by : step4Nav) {
            if (!webUI.isElementVisible(by)) {
                continue;
            }
            webUI.clickElement(by, "Thanh tiến trình — Nội dung đơn (bước 4)");
            webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
            if (isBuoc4EditReady()) {
                return true;
            }
            dismissUiOverlays();
        }
        return false;
    }

    private boolean choHienBuoc4(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (isBuoc4EditReady()) {
                System.out.println(" ✅ Đã hiển thị: Bước 4 [Nội dung đơn]");
                return true;
            }
            webUI.sleepMillis(250);
        }
        return false;
    }

    private boolean isBuoc4FormReady() {
        return isBuoc4EditReady();
    }

    /** Modal / panel xem trước PDF — không phải wizard bước 4. */
    private boolean isPreviewModalOpen() {
        return webUI.isElementVisible(By.xpath(
                "//div[contains(@role,'dialog') or contains(@class,'modal')]"
                        + "[.//*[contains(., 'Xem trước') or contains(., 'xem trước')]]"))
                || webUI.isElementVisible(By.xpath(
                REVIEW_CARD + "//div[contains(@class,'border')]"
                        + "[.//*[starts-with(normalize-space(.), 'Xem trước đơn')]"
                        + " and .//iframe and not(.//textarea)]"));
    }

    /** Màn Xem lại vẫn hiển thị (nút Gửi đơn trên card review). */
    private boolean isStillOnReviewPage() {
        By guiDonOnReview = By.xpath(REVIEW_CARD + "//button[contains(normalize-space(.), 'Gửi đơn')]");
        return webUI.isElementVisible(guiDonOnReview);
    }

    /** Quay lại bước 5 — Tài liệu & chứng cứ. */
    public void clickChinhSuaTaiLieu() {
        clickChinhSuaFirstAvailable(
                List.of(MUC_TAI_LIEU, MUC_DANH_SACH_TAI_LIEU),
                TaiLieuPage.MARKER_STEP_READY,
                "Bước 5 [Tài liệu & chứng cứ]");
    }

    /**
     * Quay lại bước 2–5 từ màn Xem lại.
     *
     * @param buoc số bước (2 = Nguyên đơn, 3 = Bị đơn, 4 = Nội dung, 5 = Tài liệu)
     */
    public void quayLaiBuoc(int buoc) {
        switch (buoc) {
            case 2 -> clickChinhSuaNguyenDon();
            case 3 -> clickChinhSuaBiDon();
            case 4 -> clickChinhSuaNoiDung();
            case 5 -> clickChinhSuaTaiLieu();
            default -> throw new IllegalArgumentException(
                    "Chỉ hỗ trợ quay lại bước 2–5 từ Xem lại. Nhận: " + buoc);
        }
    }

    public void xacNhanThongTin() {
        waitStepReady();
        if (!webUI.isElementVisible(lblXacNhan)) {
            throw new RuntimeException("❌ Không thấy Checkbox xác nhận ở Bước 6.");
        }
        webUI.clickCheckboxInLabel(lblXacNhan, "Checkbox [Xác nhận thông tin đơn]");
        waitForSubmitReady(WaitConfig.FIELD);
    }

    /** Chờ checkbox đã tick và nút Gửi đơn sẵn sàng — một vòng poll, không sleep riêng. */
    private void waitForSubmitReady(int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(100))
                    .until(d -> isXacNhanDaChon() && isGuiDonEnabled());
        } catch (TimeoutException e) {
            if (!isXacNhanDaChon()) {
                throw new RuntimeException(
                        "❌ Checkbox xác nhận chưa được tích — kiểm tra lỗi xem trước đơn trên màn hình.");
            }
            throw new RuntimeException(
                    "❌ Nút [Gửi đơn] vẫn bị khóa sau khi tick Checkbox xác nhận ở Bước 6.");
        }
    }

    private WebElement findXacNhanInput() {
        for (WebElement el : driver.findElements(chkXacNhan)) {
            try {
                if (el.isDisplayed()) {
                    return el;
                }
            } catch (Exception ignored) {
            }
        }
        WebElement label = driver.findElement(lblXacNhan);
        return label.findElement(By.xpath(".//input[@type='checkbox']"));
    }

    private boolean isXacNhanDaChon() {
        try {
            return webUI.isCheckboxChecked(findXacNhanInput());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isGuiDonEnabled() {
        try {
            for (WebElement btn : driver.findElements(btnGuiDon)) {
                if (btn.isDisplayed() && btn.isEnabled()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public void clickGuiDon() {
        webUI.clickElement(btnGuiDon, "Nút [Gửi đơn]", WaitConfig.FIELD);
    }

    public void choGuiDonThanhCong() {
        choGuiDonThanhCong(submitTimeoutSec());
    }

    public void choGuiDonThanhCong(int timeoutSeconds) {
        GuiDonKetQua kq = choKetQuaSauGuiDon(timeoutSeconds);
        if (!kq.isSuccess()) {
            throw new RuntimeException("❌ " + kq.message());
        }
    }

    public void xemLaiVaGuiDon() {
        waitStepReady();
        xacNhanThongTin();
        clickGuiDon();
        choGuiDonThanhCong();
    }

    /**
     * Tick xác nhận + bấm Gửi đơn + chờ toast + chụp ngay khi có phản hồi.
     *
     * @return {@link GuiDonKetQua} gồm trạng thái, message hệ thống, ảnh screenshot
     */
    public GuiDonKetQua thuGuiDonVaChoKetQua() {
        waitStepReady();
        xacNhanThongTin();
        clickGuiDon();
        return choKetQuaSauGuiDon(submitTimeoutSec());
    }

    /**
     * Chờ toast success/error (thoát sớm), lấy message, chụp giữ toast.
     * Hết trần → TIMEOUT + chụp màn hình hiện tại.
     */
    public GuiDonKetQua choKetQuaSauGuiDon(int timeoutSeconds) {
        System.out.println(" ⏳ Chờ hệ thống xử lý gửi đơn (tối đa " + timeoutSeconds + "s)...");
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int lastLogged = -1;
        while (System.currentTimeMillis() < deadline) {
            int elapsed = (int) ((timeoutSeconds * 1000L
                    - (deadline - System.currentTimeMillis()) + 999) / 1000);
            elapsed = Math.min(Math.max(elapsed, 1), timeoutSeconds);

            GuiDonKetQua fromToast = tryReadToastKetQua();
            if (fromToast != null) {
                System.out.println(" " + (fromToast.isSuccess() ? "✅" : "❌")
                        + " Toast sau Gửi đơn (" + elapsed + "s): " + fromToast.message());
                return fromToast;
            }

            if (webUI.existsNow(successHint)) {
                String msg = firstVisibleText(successHint, "Gửi đơn thành công");
                String shot = webUI.takeScreenshotPreserveToast();
                System.out.println(" ✅ Gửi đơn thành công (" + elapsed + "s): " + msg);
                return new GuiDonKetQua(GuiDonKetQua.TrangThai.SUCCESS, msg, shot);
            }
            if (webUI.existsNow(errorHint)) {
                String msg = firstVisibleText(errorHint, "Hệ thống báo lỗi sau khi Gửi đơn");
                String shot = webUI.takeScreenshotPreserveToast();
                System.out.println(" ❌ Hệ thống báo lỗi (" + elapsed + "s): " + msg);
                return new GuiDonKetQua(GuiDonKetQua.TrangThai.ERROR, msg, shot);
            }

            if (elapsed != lastLogged && (elapsed == 1 || elapsed % 5 == 0 || elapsed == timeoutSeconds)) {
                lastLogged = elapsed;
                System.out.println(" ⏳ Chờ phản hồi gửi đơn... (" + elapsed + "/" + timeoutSeconds + "s)");
            }
            webUI.sleepMillis(250);
        }

        String timeoutMsg = "Timeout " + timeoutSeconds
                + " giây — không có thông báo từ hệ thống sau khi Gửi đơn.";
        System.out.println(" ⚠ " + timeoutMsg);
        String shot = webUI.takeScreenshotPreserveToast();
        return new GuiDonKetQua(GuiDonKetQua.TrangThai.TIMEOUT, timeoutMsg, shot);
    }

    /** Chỉ coi là thành công khi thấy text thông báo thành công. */
    public boolean isGuiDonThanhCong() {
        GuiDonKetQua toast = tryReadToastKetQua();
        if (toast != null) {
            return toast.isSuccess();
        }
        return webUI.existsNow(successHint);
    }

    public boolean isGuiDonThatBai() {
        GuiDonKetQua toast = tryReadToastKetQua();
        if (toast != null) {
            return toast.isError();
        }
        return webUI.existsNow(errorHint);
    }

    /**
     * Đọc toast visible nếu có; đồng thời chụp ảnh giữ toast.
     *
     * @return null nếu chưa thấy toast
     */
    private GuiDonKetQua tryReadToastKetQua() {
        WebElement toast = findVisibleToast();
        if (toast == null) {
            return null;
        }
        String text = normalizeMessage(toast.getText());
        if (text.isBlank()) {
            return null;
        }
        GuiDonKetQua.TrangThai st = classifyToast(toast, text);
        if (st == null) {
            return null;
        }
        String shot = webUI.takeScreenshotPreserveToast();
        return new GuiDonKetQua(st, text, shot);
    }

    private WebElement findVisibleToast() {
        for (By by : TOAST_SELECTORS) {
            try {
                for (WebElement el : driver.findElements(by)) {
                    try {
                        if (el.isDisplayed()) {
                            String t = normalizeMessage(el.getText());
                            if (!t.isBlank() && t.length() <= 500) {
                                return el;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static GuiDonKetQua.TrangThai classifyToast(WebElement toast, String text) {
        String cls = "";
        try {
            cls = String.valueOf(toast.getAttribute("class")).toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
        }
        String lower = text.toLowerCase(Locale.ROOT);

        boolean knownToastUi = cls.contains("ant-notification")
                || cls.contains("ant-message")
                || cls.contains("toastify")
                || cls.contains("toast")
                || cls.contains("notification")
                || cls.contains("sonner");
        boolean classSuccess = cls.contains("success") || cls.contains("--success");
        boolean classError = cls.contains("error") || cls.contains("danger")
                || cls.contains("fail") || cls.contains("destructive")
                || cls.contains("warning") || cls.contains("--error");

        if (looksLikeSuccess(lower) || classSuccess) {
            return GuiDonKetQua.TrangThai.SUCCESS;
        }
        if (looksLikeError(lower) || classError) {
            return GuiDonKetQua.TrangThai.ERROR;
        }
        // Chỉ chấp nhận toast "lạ" khi chắc là UI notify (tránh role=status có sẵn trên trang)
        if (knownToastUi) {
            return GuiDonKetQua.TrangThai.ERROR;
        }
        return null;
    }

    private static boolean looksLikeSuccess(String lower) {
        return lower.contains("thành công")
                || lower.contains("đã gửi")
                || lower.contains("đã nộp")
                || lower.contains("nộp đơn thành công")
                || lower.contains("gửi đơn thành công");
    }

    private static boolean looksLikeError(String lower) {
        return lower.contains("thất bại")
                || lower.contains("không thành công")
                || lower.contains("có lỗi")
                || lower.contains("lỗi hệ thống")
                || lower.contains("vui lòng thử lại")
                || lower.contains("không thể")
                || lower.contains("error")
                || lower.contains("exception")
                || lower.contains("failed");
    }

    private String firstVisibleText(By by, String fallback) {
        try {
            WebElement best = null;
            int bestLen = Integer.MAX_VALUE;
            for (WebElement el : driver.findElements(by)) {
                try {
                    if (!el.isDisplayed()) {
                        continue;
                    }
                    String t = normalizeMessage(el.getText());
                    if (t.isBlank() || t.length() > 400) {
                        continue;
                    }
                    // Ưu tiên node nhỏ (gần toast) hơn ancestor chứa cả trang
                    if (t.length() < bestLen) {
                        best = el;
                        bestLen = t.length();
                    }
                } catch (Exception ignored) {
                }
            }
            if (best != null) {
                return normalizeMessage(best.getText());
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static String normalizeMessage(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    /** Kiểm tra màn Xem lại có chứa đoạn text (vd. yêu cầu đã sửa). */
    public boolean reviewContains(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String needle = text.trim();
        if (needle.length() > 40) {
            needle = needle.substring(0, 40);
        }
        return webUI.existsNow(By.xpath(REVIEW_SECTION
                + "//*[contains(normalize-space(.), '" + needle.replace("'", "") + "')]"));
    }
}

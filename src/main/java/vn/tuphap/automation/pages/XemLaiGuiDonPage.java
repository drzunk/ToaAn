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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        waitXemTruocDonContentReady();
    }

    /** Marker bước 6 đã hiện (không chờ thêm) — dùng assert mỏng sau khi flow báo đến Xem lại. */
    public boolean isOnReviewScreen() {
        return webUI.existsNow(stepReadyMarker);
    }

    /**
     * Tín hiệu ổn định trên card Xem lại: chuỗi {@code needle} xuất hiện trong vùng review
     * (không {@code getText()} cả card có iframe PDF). Dùng cho loại đơn / nhãn mục.
     */
    public boolean reviewContainsStableSignal(String needle) {
        if (needle == null || needle.isBlank()) {
            return true;
        }
        String safe = needle.replace("'", "").trim();
        if (safe.isEmpty()) {
            return true;
        }
        By by = By.xpath("(" + REVIEW_CARD + " | //h2[contains(., 'Xem lại')]/parent::div)"
                + "//*[contains(normalize-space(.), '" + safe + "')]");
        return webUI.existsNow(by);
    }

    /**
     * Chờ card Xem trước đơn render nội dung eform/PDF (dữ liệu đã nhập ở bước 4).
     * Chỉ sau khi preview sẵn sàng mới tick xác nhận / Gửi đơn.
     */
    public void waitXemTruocDonContentReady() {
        if (!NoiDungDonPage.wasLastStep4Iframe()) {
            System.out.println(" ⏩ Bước 4 không dùng eform iframe (upload/legacy) — bỏ qua chờ preview, gửi đơn luôn.");
            return;
        }
        if (!webUI.existsNow(xemTruocDonCardTitle())) {
            System.out.println(" ⏩ Không có card [Xem trước đơn] — bỏ qua chờ preview eform.");
            return;
        }
        System.out.println(" ⏳ Chờ nội dung [Xem trước đơn] (eform bước 4) load...");
        long deadline = System.currentTimeMillis() + WaitConfig.STEP * 1000L;
        while (System.currentTimeMillis() < deadline) {
            webUI.failIfBrowserClosed();
            if (isXemTruocDonContentReady()) {
                System.out.println(" ✅ Nội dung [Xem trước đơn] đã sẵn sàng.");
                return;
            }
            webUI.sleepMillis(300);
        }
        System.out.println(" ⚠ Hết thời gian chờ [Xem trước đơn] — vẫn thử thao tác gửi đơn.");
    }

    private static By xemTruocDonCardTitle() {
        return By.xpath("(" + REVIEW_CARD + ")//*[self::span or self::h3 or self::p]"
                + "[contains(normalize-space(.), 'Xem trước đơn')]");
    }

    private static String xemTruocDonCardScope() {
        return "(" + REVIEW_CARD + ")//*[self::span or self::h3 or self::p]"
                + "[contains(normalize-space(.), 'Xem trước đơn')]"
                + "/ancestor::div[contains(@class,'border')][1]";
    }

    private boolean isXemTruocDonContentReady() {
        if (isXemTruocDonStillLoading()) {
            return false;
        }
        if (hasXemTruocPreviewIframe()) {
            return true;
        }
        if (hasXemTruocTextSummary()) {
            return true;
        }
        return false;
    }

    private boolean isXemTruocDonStillLoading() {
        return webUI.existsNow(By.xpath(xemTruocDonCardScope()
                + "//*[contains(@class,'animate-spin') or contains(@class,'loading')"
                + " or contains(normalize-space(.),'Đang tải') or contains(normalize-space(.),'đang tải')]"));
    }

    /** iframe preview (eform /f/ hoặc PDF) trong card Xem trước đơn. */
    private boolean hasXemTruocPreviewIframe() {
        By iframe = By.xpath(xemTruocDonCardScope()
                + "//iframe[@src and string-length(normalize-space(@src)) > 4]");
        if (!webUI.existsNow(iframe)) {
            return false;
        }
        try {
            for (WebElement el : driver.findElements(iframe)) {
                if (!el.isDisplayed()) {
                    continue;
                }
                String src = el.getAttribute("src");
                if (isMeaningfulPreviewSrc(src)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /** Fallback: tóm tắt text/HTML khi không có iframe preview. */
    private boolean hasXemTruocTextSummary() {
        return webUI.existsNow(By.xpath(xemTruocDonCardScope()
                + "//*[self::textarea or self::p or contains(@class,'prose')]"
                + "[string-length(normalize-space(.)) > 15]"));
    }

    private static boolean isMeaningfulPreviewSrc(String src) {
        if (src == null) {
            return false;
        }
        String trimmed = src.trim().toLowerCase(Locale.ROOT);
        return trimmed.length() > 4
                && !trimmed.equals("about:blank")
                && !trimmed.startsWith("javascript:");
    }

    /**
     * Nút [Chỉnh sửa] trong card mục trên màn Xem lại.
     * Scope bắt buộc {@code (REVIEW_CARD)} — tránh XPath {@code |} khớp cả card thay vì nút.
     * UI mới chỉ còn card "Xem trước đơn" / "Danh sách tài liệu" (không còn card "Nội dung đơn").
     */
    private By chinhSuaTrongMuc(String tenMuc) {
        String titleMatch = tieuDeMucMatch(tenMuc);
        // Khi tìm mục khác (vd. Nội dung), tránh nhầm card Xem trước — trừ khi đang tìm đúng card đó.
        boolean timXemTruoc = MUC_XEM_TRUOC_DON.equals(tenMuc);
        String excludeXemTruoc = timXemTruoc
                ? ""
                : " and not(.//span[starts-with(normalize-space(.),'Xem trước đơn')])";
        return By.xpath("(" + REVIEW_CARD + ")"
                + "//div[contains(@class,'border') and contains(@class,'rounded')]"
                + "[(.//span[" + titleMatch + "]"
                + " or .//*[self::span or self::h3 or self::p][contains(@class,'font-bold') and (" + titleMatch + ")]"
                + " or .//*[self::span or self::h3 or self::p][" + titleMatch + "])"
                + " and .//button[contains(normalize-space(.),'Chỉnh sửa')]"
                + excludeXemTruoc + "]"
                + "//button[contains(normalize-space(.),'Chỉnh sửa')]");
    }

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
        WebElement btnEl = null;
        for (String tenMuc : tenMucCandidates) {
            WebElement found = findVisibleChinhSuaButton(List.of(tenMuc));
            if (found != null) {
                matched = tenMuc;
                btnEl = found;
                break;
            }
        }
        if (btnEl == null) {
            throw new RuntimeException("❌ Không thấy nút [Chỉnh sửa] tại các mục: " + tenMucCandidates + ".");
        }
        clickChinhSuaElement(btnEl, "Nút [Chỉnh sửa] — " + matched);
        if (!choHienMarker(stepMarker, WaitConfig.STEP, moTaBuoc)) {
            throw new RuntimeException("❌ Hết thời gian chờ: [" + moTaBuoc + "] không hiển thị sau khi Chỉnh sửa.");
        }
    }

    private boolean choHienMarker(By marker, int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ hiển thị: " + description);
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            webUI.failIfBrowserClosed();
            // Kiểm tra không chờ: vòng lặp này đã tự giữ deadline. isElementVisible thêm trần
            // PROBE_MS vào mỗi vòng, biến "poll 250ms" thành ~1.45s/vòng.
            if (webUI.existsNow(marker)) {
                System.out.println(" ✅ Đã hiển thị: " + description);
                return true;
            }
            webUI.sleepMillis(250);
        }
        return false;
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

    /**
     * [Chỉnh sửa] trên card <b>Xem trước đơn</b> → về <b>bước 1</b>, tiếp tục luồng nộp đơn.
     * (UI 2026: không còn chỉnh riêng bước 4 từ Xem lại.)
     */
    public void clickChinhSuaDon() {
        waitStepReady();
        dismissUiOverlaysFast();

        System.out.println(" ℹ Đang tìm nút [Chỉnh sửa] — Xem trước đơn...");
        WebElement btn = findChinhSuaByCardTitle(List.of(MUC_XEM_TRUOC_DON, MUC_NOI_DUNG_DON, MUC_NOI_DUNG));
        if (btn == null) {
            logVisibleReviewEditButtons();
            throw new RuntimeException(
                    "❌ Không thấy nút [Chỉnh sửa] trên card Xem trước đơn (chỉnh sửa đơn → bước 1).");
        }

        clickChinhSuaElement(btn, "Nút [Chỉnh sửa] — Xem trước đơn (về bước 1)");
        if (!choHienBuoc1(WaitConfig.STEP)) {
            throw new RuntimeException(
                    "❌ Sau [Chỉnh sửa] Xem trước đơn kỳ vọng về bước 1 [Loại đơn] nhưng chưa thấy form bước 1.");
        }
    }

    /**
     * @deprecated Dùng {@link #clickChinhSuaDon()} — UI mới về bước 1, không còn về bước 4.
     */
    @Deprecated
    public void clickChinhSuaNoiDung() {
        clickChinhSuaDon();
    }

    /**
     * [Chỉnh sửa] trên card <b>Danh sách tài liệu</b> → về <b>bước 5</b> (đính file).
     */
    public void clickChinhSuaTaiLieu() {
        waitStepReady();
        dismissUiOverlaysFast();

        System.out.println(" ℹ Đang tìm nút [Chỉnh sửa] — Danh sách tài liệu...");
        WebElement btn = findChinhSuaByCardTitle(List.of(MUC_DANH_SACH_TAI_LIEU, MUC_TAI_LIEU));
        if (btn == null) {
            logVisibleReviewEditButtons();
            throw new RuntimeException(
                    "❌ Không thấy nút [Chỉnh sửa] trên card Danh sách tài liệu (chỉnh file → bước 5).");
        }
        clickChinhSuaElement(btn, "Nút [Chỉnh sửa] — Danh sách tài liệu (về bước 5)");
        if (!choHienBuoc5(WaitConfig.STEP)) {
            throw new RuntimeException(
                    "❌ Sau [Chỉnh sửa] Danh sách tài liệu kỳ vọng về bước 5 nhưng chưa thấy form Tài liệu.");
        }
    }

    /** Chờ wizard bước 1 sau khi Chỉnh sửa đơn từ Xem lại — chỉ dùng existsNow (không wait 5s). */
    private boolean choHienBuoc1(int timeoutSeconds) {
        System.out.println(" ⏳ Chờ hiển thị: Bước 1 [Loại đơn] sau Chỉnh sửa đơn");
        By toaAnOrLoaiViec = By.xpath(
                "//button[contains(., 'Chọn tòa án') or contains(., 'tòa án nhận đơn')"
                        + " or contains(., 'Chọn loại việc') or contains(., 'loại việc cụ thể')]"
                        + " | //textarea[contains(@placeholder, 'Mô tả ngắn gọn')]");
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                webUI.failIfBrowserClosed();
                if (webUI.existsNow(TaoDonPage.MARKER_BUOC1) || webUI.existsNow(toaAnOrLoaiViec)) {
                    if (!isStillOnReviewPageFast()) {
                        System.out.println(" ✅ Đã về bước 1 [Loại đơn]");
                        return true;
                    }
                }
            } catch (vn.tuphap.automation.flow.BrowserClosedException e) {
                throw e;
            } catch (RuntimeException ignored) {
            }
            webUI.sleepMillis(250);
        }
        return false;
    }

    private boolean choHienBuoc5(int timeoutSeconds) {
        System.out.println(" ⏳ Chờ hiển thị: Bước 5 [Tài liệu] sau Chỉnh sửa file");
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                webUI.failIfBrowserClosed();
                if (isOnBuoc5TaiLieu()) {
                    System.out.println(" ✅ Đã về bước 5 [Tài liệu & chứng cứ]");
                    return true;
                }
            } catch (vn.tuphap.automation.flow.BrowserClosedException e) {
                throw e;
            } catch (RuntimeException ignored) {
            }
            webUI.sleepMillis(250);
        }
        return false;
    }

    private boolean isOnBuoc5TaiLieu() {
        try {
            if (isStillOnReviewPageFast()) {
                return false;
            }
            return webUI.existsNow(TaiLieuPage.MARKER_STEP_READY);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Tìm nút Chỉnh sửa theo tiêu đề card — tránh XPath sâu + getText() trên card có iframe PDF (treo Chrome).
     */
    private WebElement findChinhSuaByCardTitle(List<String> tenMucCandidates) {
        for (String tenMuc : tenMucCandidates) {
            By titleBtn = By.xpath("(" + REVIEW_CARD + ")"
                    + "//span[starts-with(normalize-space(.), '" + tenMuc + "')]"
                    + "/ancestor::div[contains(@class,'border') and contains(@class,'rounded')][1]"
                    + "//button[contains(normalize-space(.),'Chỉnh sửa')]");
            for (WebElement el : driver.findElements(titleBtn)) {
                try {
                    if (el.isDisplayed() && el.isEnabled()
                            && "button".equalsIgnoreCase(el.getTagName())) {
                        return el;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        // Fallback: liệt kê nút rồi khớp tiêu đề span gần nhất (không getText cả card).
        return findVisibleChinhSuaButton(tenMucCandidates);
    }

    /** Debug — liệt kê các nút Chỉnh sửa đang thấy trên card Xem lại. */
    private void logVisibleReviewEditButtons() {
        try {
            List<WebElement> buttons = driver.findElements(By.xpath(
                    "(" + REVIEW_CARD + ")//button[contains(normalize-space(.),'Chỉnh sửa')]"));
            List<String> labels = new ArrayList<>();
            for (WebElement b : buttons) {
                if (!b.isDisplayed()) {
                    continue;
                }
                String title = readNearestCardTitle(b);
                labels.add(title.isBlank() ? "(không tiêu đề)" : title);
            }
            System.out.println(" ℹ Nút [Chỉnh sửa] đang thấy trên Xem lại: " + labels);
        } catch (Exception ignored) {
        }
    }

    /** Đọc tiêu đề card qua span — không dùng getText() trên cả khối (iframe PDF). */
    private String readNearestCardTitle(WebElement btn) {
        try {
            List<WebElement> spans = btn.findElements(By.xpath(
                    "./ancestor::div[contains(@class,'border')][1]"
                            + "//span[contains(@class,'font-bold') or starts-with(normalize-space(.),'Xem trước')"
                            + " or starts-with(normalize-space(.),'Danh sách')"
                            + " or starts-with(normalize-space(.),'Nội dung')"
                            + " or starts-with(normalize-space(.),'Loại đơn')"
                            + " or starts-with(normalize-space(.),'Tài liệu')]"));
            for (WebElement span : spans) {
                String t = span.getText();
                if (t != null && !t.isBlank()) {
                    return t.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** Tìm nút Chỉnh sửa thật sự (hiển thị) theo danh sách tên mục. */
    private WebElement findVisibleChinhSuaButton(List<String> tenMucCandidates) {
        By allEdit = By.xpath("(" + REVIEW_CARD + ")//button[contains(normalize-space(.),'Chỉnh sửa')]");
        for (WebElement el : driver.findElements(allEdit)) {
            try {
                if (!el.isDisplayed() || !el.isEnabled()) {
                    continue;
                }
                if (!"button".equalsIgnoreCase(el.getTagName())) {
                    continue;
                }
                String title = readNearestCardTitle(el);
                for (String muc : tenMucCandidates) {
                    if (title.startsWith(muc) || title.contains(muc)) {
                        return el;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        // XPath cũ theo mục (chỉ khi fallback trên không khớp)
        for (String tenMuc : tenMucCandidates) {
            By by = chinhSuaTrongMuc(tenMuc);
            for (WebElement el : driver.findElements(by)) {
                try {
                    if (el.isDisplayed() && el.isEnabled()
                            && "button".equalsIgnoreCase(el.getTagName())) {
                        return el;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }


    /** ESC + đóng overlay — dùng existsNow (không isElementVisible 5s — treo trên card có iframe PDF). */
    private void dismissUiOverlaysFast() {
        try {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {
        }
        webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        List<By> closeButtons = List.of(
                By.xpath("//button[@aria-label='Close' or @aria-label='Đóng']"),
                By.xpath("//div[@role='dialog']//button[.//*[contains(@class,'lucide-x')]]")
        );
        for (By close : closeButtons) {
            if (webUI.existsNow(close)) {
                try {
                    webUI.clickElement(close, "Nút [Đóng overlay/dialog]");
                    webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
                } catch (RuntimeException ignored) {
                }
                break;
            }
        }
    }

    private void clickChinhSuaElement(WebElement btn, String logName) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", btn);
            webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            // Click đúng nút button — tránh click nhầm khối card cha.
            if (!"button".equalsIgnoreCase(btn.getTagName())) {
                List<WebElement> nested = btn.findElements(By.xpath(".//button[contains(normalize-space(.),'Chỉnh sửa')]"));
                if (!nested.isEmpty()) {
                    btn = nested.get(0);
                }
            }
            btn.click();
        } catch (Exception e) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", btn);
        }
        System.out.println(" ➔ Click vào: [" + logName + "]");
        TestActionLog.click(logName);
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
    }


    private boolean isStillOnReviewPageFast() {
        By guiDonOnReview = By.xpath(REVIEW_CARD + "//button[contains(normalize-space(.), 'Gửi đơn')]");
        return webUI.existsNow(guiDonOnReview);
    }

    /**
     * Quay lại bước từ màn Xem lại (UI 2026: 2 nút Chỉnh sửa chính).
     *
     * @param buoc 1 = Xem trước đơn → bước 1; 5 = Danh sách tài liệu → bước 5
     */
    public void quayLaiBuoc(int buoc) {
        switch (buoc) {
            case 1, 4 -> clickChinhSuaDon(); // 4 giữ tương thích — UI mới về bước 1
            case 2 -> clickChinhSuaNguyenDon();
            case 3 -> clickChinhSuaBiDon();
            case 5 -> clickChinhSuaTaiLieu();
            default -> throw new IllegalArgumentException(
                    "Chỉ hỗ trợ quay lại bước 1/2/3/5 từ Xem lại. Nhận: " + buoc);
        }
    }

    public void xacNhanThongTin() {
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
        choGuiDonThanhCong(timeoutSeconds, Set.of());
    }

    public void choGuiDonThanhCong(int timeoutSeconds, Set<String> baselineFeedback) {
        GuiDonKetQua kq = choKetQuaSauGuiDon(timeoutSeconds, baselineFeedback);
        if (!kq.isSuccess()) {
            throw new RuntimeException("❌ " + kq.message());
        }
    }

    public void xemLaiVaGuiDon() {
        waitStepReady();
        xacNhanThongTin();
        Set<String> baseline = snapshotFeedbackBaseline();
        clickGuiDon();
        choGuiDonThanhCong(submitTimeoutSec(), baseline);
    }

    /**
     * Tick xác nhận + bấm Gửi đơn + chờ toast + chụp ngay khi có phản hồi.
     *
     * @return {@link GuiDonKetQua} gồm trạng thái, message hệ thống, ảnh screenshot
     */
    public GuiDonKetQua thuGuiDonVaChoKetQua() {
        waitStepReady();
        xacNhanThongTin();
        Set<String> baseline = snapshotFeedbackBaseline();
        clickGuiDon();
        return choKetQuaSauGuiDon(submitTimeoutSec(), baseline);
    }

    private Set<String> snapshotFeedbackBaseline() {
        Set<String> baseline = new LinkedHashSet<>();
        for (String msg : webUI.collectSystemFeedbackMessages()) {
            String n = normalizeMessage(msg);
            if (!n.isBlank()) {
                baseline.add(n);
            }
        }
        return baseline;
    }

    /**
     * Chờ toast success/error (thoát sớm), lấy message, chụp giữ toast.
     * Hết trần → TIMEOUT + chụp màn hình hiện tại.
     */
    public GuiDonKetQua choKetQuaSauGuiDon(int timeoutSeconds) {
        return choKetQuaSauGuiDon(timeoutSeconds, Set.of());
    }

    public GuiDonKetQua choKetQuaSauGuiDon(int timeoutSeconds, Set<String> baselineFeedback) {
        Set<String> baseline = baselineFeedback == null ? Set.of() : baselineFeedback;
        System.out.println(" ⏳ Chờ hệ thống xử lý gửi đơn (tối đa " + timeoutSeconds + "s)...");
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int lastLogged = -1;
        long nextHintScan = 0;
        while (System.currentTimeMillis() < deadline) {
            int elapsed = (int) ((timeoutSeconds * 1000L
                    - (deadline - System.currentTimeMillis()) + 999) / 1000);
            elapsed = Math.min(Math.max(elapsed, 1), timeoutSeconds);

            GuiDonKetQua fromNewFeedback = tryReadNewSubmitFeedback(baseline);
            if (fromNewFeedback != null) {
                System.out.println(" " + (fromNewFeedback.isSuccess() ? "✅" : "❌")
                        + " Phản hồi sau Gửi đơn (" + elapsed + "s): " + fromNewFeedback.message());
                TestActionLog.validation("Sau Gửi đơn — bước 6", fromNewFeedback.message());
                return fromNewFeedback;
            }

            GuiDonKetQua fromToast = tryReadToastKetQua();
            if (fromToast != null) {
                System.out.println(" " + (fromToast.isSuccess() ? "✅" : "❌")
                        + " Toast sau Gửi đơn (" + elapsed + "s): " + fromToast.message());
                TestActionLog.validation("Sau Gửi đơn — bước 6", fromToast.message());
                return fromToast;
            }

            // successHint/errorHint là XPath //*[contains(...)] — quét TOÀN BỘ tài liệu, rất đắt.
            // Toast đã được 2 bộ ở trên bắt mỗi 250ms rồi; đây chỉ là lưới an toàn cho trường hợp
            // hệ thống in thẳng ra trang mà không qua toast → soi mỗi ~1s là đủ.
            if (System.currentTimeMillis() >= nextHintScan) {
                nextHintScan = System.currentTimeMillis() + 1000;
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
        GuiDonKetQua toast = tryReadToastKetQua(false);
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
    /**
     * Phản hồi mới sau Gửi đơn — so với baseline trước click, bỏ qua toast eform bước 4 cũ.
     */
    private GuiDonKetQua tryReadNewSubmitFeedback(Set<String> baseline) {
        List<String> fresh = new ArrayList<>();
        for (String msg : webUI.collectSystemFeedbackMessages()) {
            String n = normalizeMessage(msg);
            if (n.isBlank() || baseline.contains(n) || isEformBridgeToast(n)) {
                continue;
            }
            fresh.add(n);
        }
        if (fresh.isEmpty()) {
            return null;
        }
        String joined = String.join(" | ", fresh);
        String lower = joined.toLowerCase(Locale.ROOT);
        String shot = webUI.takeScreenshotPreserveToast();
        System.out.println(" 📸 Phát hiện phản hồi mới sau Gửi đơn — chụp ảnh ngay.");
        if (looksLikeSuccess(lower)) {
            return new GuiDonKetQua(GuiDonKetQua.TrangThai.SUCCESS, joined, shot);
        }
        return new GuiDonKetQua(GuiDonKetQua.TrangThai.ERROR, joined, shot);
    }

    private GuiDonKetQua tryReadToastKetQua() {
        return tryReadToastKetQua(true);
    }

    /**
     * @param chupAnh {@code false} khi chỉ cần biết trạng thái (vd. {@link #isGuiDonThanhCong()}) —
     *                chụp ảnh ở đó là tốn công vô ích vì kết quả bị vứt cùng đối tượng trả về.
     */
    private GuiDonKetQua tryReadToastKetQua(boolean chupAnh) {
        for (WebElement toast : findAllVisibleToasts()) {
            String text = normalizeMessage(toast.getText());
            if (text.isBlank() || isEformBridgeToast(text)) {
                continue;
            }
            GuiDonKetQua.TrangThai st = classifyToast(toast, text);
            if (st == null) {
                continue;
            }
            return new GuiDonKetQua(st, text, chupAnh ? webUI.takeScreenshotPreserveToast() : null);
        }
        return null;
    }

    private List<WebElement> findAllVisibleToasts() {
        // Giữ sẵn text đã đọc ở lượt quét đầu: getText() bắt Chrome tính lại layout, mà hàm so
        // sánh của sort gọi nó cho MỌI cặp phần tử (O(n log n) lượt) — trước đây mỗi toast bị
        // đọc text 2 + O(log n) lần chỉ để sắp theo độ dài.
        List<Map.Entry<WebElement, Integer>> found = new ArrayList<>();
        for (By by : TOAST_SELECTORS) {
            try {
                for (WebElement el : driver.findElements(by)) {
                    try {
                        if (!el.isDisplayed()) {
                            continue;
                        }
                        String t = normalizeMessage(el.getText());
                        if (!t.isBlank() && t.length() <= 500) {
                            found.add(Map.entry(el, t.length()));
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }
        found.sort(Map.Entry.comparingByValue());
        List<WebElement> result = new ArrayList<>(found.size());
        for (Map.Entry<WebElement, Integer> e : found) {
            result.add(e.getKey());
        }
        return result;
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

    /** Toast bridge iframe bước 4 — không phải kết quả Gửi đơn bước 6. */
    private static boolean isEformBridgeToast(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("biểu mẫu chưa phản hồi")
                || lower.contains("gửi ngay trong biểu mẫu")
                || lower.contains("đã ghi nhận nội dung");
    }

    /**
     * Đọc text ngắn nhất khớp locator.
     * <p>
     * Locator gọi vào đây là {@code //*[contains(., '…')]} nên khớp cả <b>mọi tổ tiên</b> của node
     * thật — kể cả {@code <body>} và card chứa iframe xem trước PDF. Dùng {@code innerText} thay
     * {@code getText()}: getText() bắt Chrome tính lại layout của cả cây con (chính là thứ làm treo
     * trên card PDF), trong khi ở đây chỉ cần chuỗi để chọn node nhỏ nhất.
     */
    private String firstVisibleText(By by, String fallback) {
        try {
            String best = null;
            for (WebElement el : driver.findElements(by)) {
                try {
                    if (!el.isDisplayed()) {
                        continue;
                    }
                    String t = normalizeMessage(el.getAttribute("innerText"));
                    if (t.isBlank() || t.length() > 400) {
                        continue;
                    }
                    // Ưu tiên node nhỏ (gần toast) hơn ancestor chứa cả trang
                    if (best == null || t.length() < best.length()) {
                        best = t;
                    }
                } catch (Exception ignored) {
                }
            }
            if (best != null) {
                return best;
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

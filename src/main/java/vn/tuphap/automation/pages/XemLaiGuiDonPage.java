package vn.tuphap.automation.pages;

import vn.tuphap.automation.report.TestActionLog;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.List;
import java.util.Locale;

public class XemLaiGuiDonPage {
    private final WebDriver driver;
    private final WebUI webUI;

    private static final String REVIEW_SECTION =
            "//h2[contains(., 'Xem lại') and contains(., 'Gửi đơn')]/parent::div";

    private final By stepReadyMarker = By.xpath(REVIEW_SECTION
            + "//span[contains(., 'Loại đơn') or contains(., 'Nguyên đơn')]");
    private final By chkXacNhan = By.xpath(REVIEW_SECTION
            + "//label[contains(., 'Tôi xác nhận')]//input[@type='checkbox']");
    private final By lblXacNhan = By.xpath(REVIEW_SECTION
            + "//label[contains(., 'Tôi xác nhận')]");
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
    public static final String MUC_TAI_LIEU = "Tài liệu";

    public XemLaiGuiDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void waitStepReady() {
        webUI.waitUntilVisible(stepReadyMarker, WaitConfig.STEP, "Bước 6 [Xem lại & Gửi đơn]");
    }

    private By chinhSuaTrongMuc(String tenMuc) {
        return By.xpath(REVIEW_SECTION
                + "//div[contains(@class, 'border') and contains(@class, 'rounded')"
                + " and .//span[contains(@class, 'font-bold') and contains(normalize-space(.), '"
                + tenMuc + "')]]"
                + "//button[contains(normalize-space(.), 'Chỉnh sửa')]");
    }

    /**
     * Bấm [Chỉnh sửa] tại một mục trên màn Xem lại và chờ biểu mẫu bước tương ứng hiển thị.
     */
    public void clickChinhSua(String tenMuc, By stepMarker, String moTaBuoc) {
        waitStepReady();
        By btnChinhSua = chinhSuaTrongMuc(tenMuc);
        if (!webUI.isElementVisible(btnChinhSua)) {
            throw new RuntimeException("❌ Không thấy nút [Chỉnh sửa] tại mục [" + tenMuc + "].");
        }
        webUI.clickElement(btnChinhSua, "Nút [Chỉnh sửa] — " + tenMuc);
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

    /** Quay lại bước 4 — Nội dung đơn. */
    public void clickChinhSuaNoiDung() {
        clickChinhSua(MUC_NOI_DUNG,
                By.xpath("//h2[contains(., 'Nội dung đơn')]"
                        + "/parent::div//label[contains(., 'Thời điểm phát sinh')]"),
                "Bước 4 [Nội dung đơn]");
    }

    /** Quay lại bước 5 — Tài liệu & chứng cứ. */
    public void clickChinhSuaTaiLieu() {
        clickChinhSua(MUC_TAI_LIEU,
                By.xpath("//h2[contains(., 'Tài liệu') and contains(., 'chứng')]"
                        + "/parent::div//div[contains(., 'Tài liệu bắt buộc')]"),
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
        if (!webUI.isElementVisible(chkXacNhan)) {
            throw new RuntimeException("❌ Không thấy Checkbox xác nhận ở Bước 6.");
        }
        WebElement checkbox = driver.findElement(chkXacNhan);
        if (!checkbox.isSelected()) {
            webUI.clickElement(lblXacNhan, "Checkbox [Xác nhận thông tin đơn]");
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        } else {
            System.out.println(" ⏩ Checkbox [Xác nhận thông tin] đã được chọn sẵn.");
            TestActionLog.chon("Checkbox [Xác nhận thông tin đơn]", "Đã chọn sẵn");
        }
    }

    public void clickGuiDon() {
        webUI.waitUntilVisible(btnGuiDon, WaitConfig.FIELD, "Nút [Gửi đơn]");
        WebElement button = driver.findElement(btnGuiDon);
        if (!button.isEnabled()) {
            throw new RuntimeException("❌ Nút [Gửi đơn] vẫn bị khóa — hãy tick Checkbox xác nhận trước.");
        }
        webUI.clickElement(btnGuiDon, "Nút [Gửi đơn]");
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

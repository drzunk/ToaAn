package vn.tuphap.automation.pages;

import vn.tuphap.automation.report.TestActionLog;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

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
    private final By successAny = By.xpath(
            "//*[contains(., 'thành công') or contains(., 'Thành công')"
                    + " or contains(., 'Gửi đơn thành công') or contains(., 'Nộp đơn thành công')]");
    /** Toast/banner lỗi thường gặp — dùng để dừng chờ sớm, không đợi đủ timeout. */
    private final By errorAny = By.xpath(
            "//*[contains(., 'thất bại') or contains(., 'Thất bại')"
                    + " or contains(., 'không thành công') or contains(., 'Có lỗi')"
                    + " or contains(., 'lỗi hệ thống') or contains(., 'Lỗi hệ thống')"
                    + " or contains(., 'vui lòng thử lại') or contains(., 'Vui lòng thử lại')]");

    /** Thời gian chờ toast thành công sau khi bấm Gửi đơn. Override: -Dtaodon.submit.timeoutSec */
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
        System.out.println(" ⏳ Chờ hệ thống xử lý gửi đơn (tối đa " + timeoutSeconds + "s)...");
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int lastLogged = -1;
        while (System.currentTimeMillis() < deadline) {
            int elapsed = (int) ((timeoutSeconds * 1000L - (deadline - System.currentTimeMillis()) + 999) / 1000);
            elapsed = Math.min(Math.max(elapsed, 1), timeoutSeconds);
            if (isGuiDonThanhCong()) {
                System.out.println(" ✅ Gửi đơn thành công (" + elapsed + "s).");
                return;
            }
            if (isGuiDonThatBai()) {
                throw new RuntimeException(
                        "❌ Hệ thống báo lỗi sau khi Gửi đơn (phát hiện sớm sau " + elapsed + "s).");
            }
            if (elapsed != lastLogged && (elapsed == 1 || elapsed % 4 == 0 || elapsed == timeoutSeconds)) {
                lastLogged = elapsed;
                System.out.println(" ⏳ Chờ phản hồi gửi đơn... (" + elapsed + "/" + timeoutSeconds + "s)");
            }
            webUI.sleepMillis(250);
        }
        throw new RuntimeException(
                "❌ Hết thời gian chờ: Không nhận được thông báo thành công sau khi Gửi đơn ("
                        + timeoutSeconds + "s).");
    }

    /** Chỉ coi là thành công khi thấy text thông báo thành công (không suy luận từ nút biến mất). */
    public boolean isGuiDonThanhCong() {
        return webUI.existsNow(successAny);
    }

    public boolean isGuiDonThatBai() {
        return webUI.existsNow(errorAny);
    }

    public void xemLaiVaGuiDon() {
        waitStepReady();
        xacNhanThongTin();
        clickGuiDon();
        choGuiDonThanhCong();
    }

    /**
     * Tick xác nhận + bấm Gửi đơn + chờ kết quả.
     *
     * @return {@code true} nếu thấy thông báo thành công; {@code false} nếu timeout/lỗi app
     *         (không ném exception — dùng cho soft-fail).
     */
    public boolean thuGuiDonVaChoKetQua() {
        waitStepReady();
        xacNhanThongTin();
        clickGuiDon();
        try {
            choGuiDonThanhCong();
            return true;
        } catch (RuntimeException ex) {
            System.out.println(" ⚠ Gửi đơn chưa thành công: " + ex.getMessage());
            return false;
        }
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

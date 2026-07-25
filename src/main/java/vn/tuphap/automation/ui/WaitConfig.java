package vn.tuphap.automation.ui;

/**
 * Timeout thống nhất cho toàn bộ luồng tạo đơn.
 * <p>
 * Nguyên tắc: giá trị là <b>tối đa</b> — WebDriverWait trả về ngay khi element xuất hiện,
 * nên happy-path vẫn nhanh; chỉ case chậm/SPA nặng mới chạm trần.
 * Tránh số magic rải rác (10/12/15/20) gây chỗ quá gắt, chỗ quá dài.
 */
public final class WaitConfig {

    private WaitConfig() {
    }

    /** Field / control đã nằm trên form hiện tại. */
    public static final int FIELD = 8;

    /**
     * Chuyển bước wizard (bấm Tiếp theo → marker bước sau).
     * SPA demo đôi khi chậm — 15s ổn định hơn 10s, vẫn nhanh hơn 20s cũ.
     */
    public static final int STEP = 15;

    /** Form trong card sau khi chọn Cá nhân / Tổ chức. */
    public static final int FORM = 10;

    /** Dashboard sau đăng nhập / đăng nhập lại. */
    public static final int DASHBOARD = 12;

    /**
     * Dashboard sau khi đã {@code driver.get(home)} — trang đang load,
     * không chờ trước khi navigate.
     */
    public static final int DASHBOARD_AFTER_NAV = 8;

    /** Dropdown mở / có option. */
    public static final int DROPDOWN = 10;

    /** Chờ toast sau Gửi đơn (soft-fail). Override: {@code -Dtaodon.submit.timeoutSec}. */
    public static final int SUBMIT = 10;

    /** Bước 3 Hôn nhân — UI đặc thù, đôi khi render chậm. */
    public static final int HON_NHAN = 15;

    /** Nghỉ ngắn sau click / đóng nháp (ms). */
    public static final long SETTLE_MS = 400;

    /** Nghỉ sau thao tác nặng hơn (thêm bị đơn, đổi tab). */
    public static final long SETTLE_LONG_MS = 700;

    public static int submitTimeoutSec() {
        String raw = System.getProperty("taodon.submit.timeoutSec");
        if (raw == null || raw.isBlank()) {
            return SUBMIT;
        }
        try {
            return Math.max(3, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return SUBMIT;
        }
    }
}

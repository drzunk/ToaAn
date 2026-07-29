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

    /**
     * Lỗi chặn luồng: chờ thêm trước khi dừng case (để toast/API kịp hiện đủ).
     */
    public static final int BLOCKING_GRACE_SEC = 10;

    /** Form trong card sau khi chọn Cá nhân / Tổ chức. */
    public static final int FORM = 10;

    /** Dashboard sau đăng nhập / đăng nhập lại. */
    public static final int DASHBOARD = 12;

    /** Chờ Dashboard sau bấm [Đăng nhập] — môi trường dev đôi khi >12s. */
    public static final int DASHBOARD_LOGIN = 25;

    /**
     * Dashboard sau khi đã {@code driver.get(home)} — trang đang load,
     * không chờ trước khi navigate.
     */
    public static final int DASHBOARD_AFTER_NAV = 8;

    /** Dropdown mở / có option. */
    public static final int DROPDOWN = 10;

    /**
     * Trần tối đa chờ toast sau Gửi đơn (soft-fail).
     * Happy-path vẫn thoát sớm khi toast hiện; chỉ case chậm/treo mới chạm trần.
     * Override: {@code -Dtaodon.submit.timeoutSec}.
     */
    public static final int SUBMIT = 60;

    /** Bước 3 Hôn nhân — UI đặc thù, đôi khi render chậm. */
    public static final int HON_NHAN = 15;

    /** Chờ phường/xã load sau chọn tỉnh — thoát ngay khi dropdown hiện + enabled. */
    public static final int WARD_READY = 4;

    /** Thử nhanh sau bấm Chỉnh sửa trên màn Xem lại (tránh treo 15s/nút). */
    public static final int REVIEW_EDIT_PROBE = 4;

    /** Nghỉ sau khối địa chỉ (tỉnh → chờ phường → chi tiết). */
    public static final long SETTLE_ADDRESS_MS = 280;

    /** Nghỉ giữa hai khối địa chỉ liên tiếp (thường trú / liên lạc, bị đơn #1 / #2…). */
    public static final long ADDRESS_BLOCK_GAP_MS = 350;

    /** Nghỉ ngắn sau scroll / click nhẹ (ms). */
    public static final long SETTLE_SHORT_MS = 120;

    /** Nghỉ ngắn sau click / đóng nháp (ms). */
    public static final long SETTLE_MS = 280;

    /** Chờ catalog/API dropdown sau khi chọn loại đơn (tránh crash danhSach). */
    public static final int CATALOG_READY = 12;

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

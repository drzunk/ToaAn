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

    /**
     * Chờ toast sau tick "Đồng ý lưu Thông tin định danh" (VNeID) — chỉ để LOG kết quả,
     * không quyết định nhánh nào của luồng (bước sau luôn tự soát lại field bất kể toast nói gì).
     * Môi trường dev hiện luôn trả lỗi cho lệnh lưu này — trần thấp để không cõng {@link #FIELD}
     * (8s) vào mọi case chỉ để đọc 1 dòng log chẩn đoán.
     */
    public static final int IDENTITY_SAVE_FEEDBACK_SEC = 3;

    /**
     * Trần chờ ngầm của {@code WebUI.isElementVisible} — hàm này bị dùng như một <b>phép kiểm tra</b>
     * ("field tuỳ chọn này có trên form không?") ở ~38 chỗ, mà mỗi lần trả {@code false} nó phải
     * chờ hết trần mới trả lời. Trần cũ 5s khiến một khối chỉ toàn field vắng mặt (vd. người đại
     * diện Tổ chức không hiện) đốt 15s+ chỉ để kết luận "không có gì".
     * <p>
     * 1.2s vẫn đủ phủ thời gian React render lại (thường &lt; 300ms). Chờ thật sự phải dùng
     * {@code waitUntilVisible(...)} với timeout khai báo rõ, không dựa vào trần ngầm ở đây.
     */
    public static final long PROBE_MS = resolveProbeMs();

    /**
     * 1200 → 400ms. Đây là <b>hạ thời gian chờ</b>, không phải bỏ chờ: mọi call site vẫn chờ y như
     * cũ, chỉ ngắn hơn. Chính javadoc trên ghi React render lại "thường &lt; 300ms", nên 400ms còn
     * dư biên; 1200ms là mức thừa được chọn hú hoạ.
     * <p>
     * Revert không cần build: {@code -Dtaodon.probeMs=1200}.
     */
    private static long resolveProbeMs() {
        String raw = System.getProperty("taodon.probeMs");
        if (raw == null || raw.isBlank()) {
            return 400;
        }
        try {
            return Math.max(100, Long.parseLong(raw.trim()));
        } catch (NumberFormatException e) {
            return 400;
        }
    }

    /**
     * Chờ toast host ghi nhận nội dung eform sau khi commit iframe (bước 4).
     * <p>
     * Thuần chẩn đoán: hết giờ thì chỉ in log rồi vẫn bấm Tiếp theo như thường. Đo trên 39 case
     * (mid) toast này <b>chưa bao giờ xuất hiện</b> — 4/4 case iframe đều đốt trọn 8s cũ rồi đi
     * tiếp, và chính 4 case đó là nhóm bước 4 chậm nhất (33–40s so với trung vị 8.5s).
     * Giữ lại một khoảng ngắn để host kịp xử lý, thay vì chờ mòn mỏi thứ không tới.
     */
    public static final int EFORM_ACK_MS = 2500;
    // Đã thử hạ xuống 800ms: chỉ tiết kiệm ~0.2s/case (4/39 case dùng iframe) nhưng lượt đo sau đó
    // xuất hiện thêm một case hỏng ở bước 4 với "Vui lòng điền đầy đủ thông tin vào biểu mẫu" —
    // đúng dạng lỗi khi host chưa kịp nhận nội dung iframe. Không đáng đổi. Giữ 2500.

    /**
     * Hệ số nhân cho các khoảng <b>nghỉ cố định</b> ({@code SETTLE_*}, {@code ADDRESS_BLOCK_GAP_MS}) —
     * chỉnh bằng {@code -Dtaodon.wait.scale=0.5} mà không phải sửa code hay build lại.
     * <p>
     * <b>Chỉ áp cho sleep, không áp cho trần chờ.</b> Trần ({@link #FIELD}, {@link #STEP},
     * {@link #WARD_READY}, {@link #PROBE_MS}, {@link #SUBMIT}…) tồn tại để đỡ case chậm; hạ trần
     * biến "chậm nhưng đậu" thành flaky, còn hạ sleep chỉ lấy lại thời gian đang ngồi không.
     * <p>
     * Đi kèm: các hằng dưới đây <b>không còn là compile-time constant</b>. Trước đây javac nhúng
     * thẳng con số vào cả trăm call site, nên sửa mỗi file này rồi build tăng dần sẽ để lại giá trị
     * cũ trong các page class. Chuyển sang lời gọi hàm là hết hẳn chuyện đó.
     */
    private static final double SCALE = resolveScale();

    private static double resolveScale() {
        String raw = System.getProperty("taodon.wait.scale");
        if (raw == null || raw.isBlank()) {
            return 1.0;
        }
        try {
            // Chặn dưới 0.25 để một lần gõ nhầm (vd. 0.05) không biến cả bộ thành flaky.
            return Math.max(0.25, Math.min(3.0, Double.parseDouble(raw.trim())));
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    private static long scaled(long baseMs) {
        return Math.max(0, Math.round(baseMs * SCALE));
    }

    public static double waitScale() {
        return SCALE;
    }

    // Các giá trị dưới đây là mức đã chạy 0.6 và đo sạch 3 lượt MID liên tiếp (StepBlocked=1,
    // lỗi eform=4, ô lệch giá trị=40 — đều bằng baseline). Trước đây phải bật bằng
    // -Dtaodon.wait.scale=0.6 mới có tác dụng; đưa thẳng vào đây để chạy bình thường cũng được hưởng.
    // Muốn quay lại mức thận trọng: -Dtaodon.wait.scale=1.7

    /** Nghỉ sau khối địa chỉ (tỉnh → chờ phường → chi tiết). */
    public static final long SETTLE_ADDRESS_MS = scaled(72);

    /** Nghỉ giữa hai khối địa chỉ liên tiếp (thường trú / liên lạc, bị đơn #1 / #2…). */
    public static final long ADDRESS_BLOCK_GAP_MS = scaled(90);

    /** Nghỉ ngắn sau scroll / click nhẹ (ms). */
    public static final long SETTLE_SHORT_MS = scaled(36);

    /** Nghỉ ngắn sau click / đóng nháp (ms). */
    public static final long SETTLE_MS = scaled(60);

    /** Chờ catalog/API dropdown sau khi chọn loại đơn (tránh crash danhSach). */
    public static final int CATALOG_READY = 12;

    /**
     * Nghỉ sau thao tác nặng hơn (thêm bị đơn, đổi tab).
     * <p>
     * Các chỗ dùng giá trị này để che một race bất đồng bộ thật đã chuyển sang
     * {@link #SETTLE_ASYNC_MS}; những gì còn lại đều có wait thật đi ngay sau.
     */
    public static final long SETTLE_LONG_MS = scaled(150);

    /**
     * Nghỉ <b>chịu lực</b> — che một race bất đồng bộ thật, không phải nghỉ cho đẹp.
     * Cố ý <b>không</b> nhân {@link #SCALE}: hạ giá trị này sinh lỗi dữ liệu (vd. chọn trúng phường
     * của tỉnh cũ vì nút phường còn giữ giá trị cũ thêm một nhịp sau khi đổi tỉnh).
     * <p>
     * Dùng ở: sau khi chọn tỉnh trước khi chờ phường, sau {@code navigate().refresh()} khi khôi phục
     * crash frontend, và ngay sau khi mở dropdown phường trước lúc gõ tìm kiếm.
     */
    public static final long SETTLE_ASYNC_MS = 700;

    /**
     * Nghỉ trong luồng eform iframe (bước 4) — <b>không</b> nhân {@link #SCALE}.
     * <p>
     * Đây là hệ thống con mong manh nhất của cả luồng: nội dung nằm trong iframe, việc chọn xong
     * phải được host bên ngoài ghi nhận qua {@code postMessage}. Đo thực tế khi bật
     * {@code taodon.wait.scale=0.6}: lỗi "Biểu mẫu chưa phản hồi" <b>tăng gấp đôi (4 → 8)</b> trong
     * khi mọi phần khác của luồng không hề xấu đi (số ô lệch giá trị giữ nguyên 40, không lỗi mới
     * ở bước 2/3). Tách riêng để hạ tốc phần còn lại mà không đụng vào đây.
     */
    public static final long SETTLE_EFORM_MS = 120;

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

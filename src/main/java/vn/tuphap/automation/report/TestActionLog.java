package vn.tuphap.automation.report;

import vn.tuphap.automation.pages.TaoDonPage;

import java.util.ArrayList;
import java.util.List;

/**
 * Ghi nhận thao tác nhập liệu thực tế (cùng nội dung in ra console) để xuất báo cáo Excel.
 */
public final class TestActionLog {

    public record Action(String thaoTac, String truong, String giaTri, String ghiChu) {
    }

    private static final ThreadLocal<List<Action>> ACTIONS =
            ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> Boolean.TRUE);

    private TestActionLog() {
    }

    /** Bắt đầu testcase mới — xóa buffer thao tác. */
    public static void beginTest() {
        ACTIONS.get().clear();
        ENABLED.set(Boolean.TRUE);
    }

    /** Tắt ghi (vd. đăng nhập session BeforeClass) để không lẫn vào testcase. */
    public static void pause() {
        ENABLED.set(Boolean.FALSE);
    }

    public static void resume() {
        ENABLED.set(Boolean.TRUE);
        ACTIONS.get().clear();
    }

    public static void clear() {
        ACTIONS.get().clear();
    }

    public static List<Action> snapshot() {
        return List.copyOf(ACTIONS.get());
    }

    public static List<Action> drain() {
        List<Action> copy = List.copyOf(ACTIONS.get());
        ACTIONS.get().clear();
        return copy;
    }

    public static void dien(String truong, String giaTri) {
        add("Điền", cleanField(truong), nz(giaTri), "");
    }

    public static void dienMask(String truong, String giaTri) {
        add("Điền", cleanField(truong), nz(giaTri), "Ô có định dạng đặc biệt");
    }

    public static void chon(String truong, String giaTri) {
        add("Chọn", cleanField(truong), nz(giaTri), "");
    }

    public static void timKiemDropdown(String giaTri) {
        add("Tìm kiếm dropdown", "Ô tìm kiếm trong Dropdown", nz(giaTri), "");
    }

    public static void taiLen(String truong, String tenFile) {
        add("Tải lên", cleanField(truong), nz(tenFile), "");
    }

    public static void click(String truong) {
        add("Click", cleanField(truong), "", "");
    }

    public static void boQua(String truong, String lyDo) {
        add("Bỏ qua", cleanField(truong), "", lyDo == null ? "" : lyDo);
    }

    public static void ghiChu(String noiDung) {
        add("Ghi chú", "", "", noiDung == null ? "" : noiDung);
    }

    /** Thông báo validate / server trên biểu mẫu. */
    public static void validation(String buoc, String message) {
        add("Validation", buoc == null ? "" : buoc.trim(), message == null ? "" : message.trim(),
                "Thông báo từ biểu mẫu / server");
    }

    /**
     * Đánh dấu đầu một khối bước/page trong báo cáo Excel.
     *
     * @param soBuoc   số bước (0 = mở biểu mẫu / đăng nhập / khác)
     * @param tenBuoc  tên bước đầy đủ
     * @param page     tên màn hình tiếng Việt (vd. Trang tạo đơn)
     */
    public static void buoc(int soBuoc, String tenBuoc, String page) {
        String title = soBuoc > 0
                ? ("Bước " + soBuoc + " — " + (tenBuoc == null ? "" : tenBuoc.trim()))
                : (tenBuoc == null ? "Thao tác" : tenBuoc.trim());
        add("KHỐI", title, page == null ? "" : page.trim(), "");
    }

    /**
     * Ghi trạng thái kết thúc bước hiện tại (Đạt / Thất bại / …) — hiện trên banner bước trong Excel.
     */
    public static void trangThaiBuoc(String trangThai) {
        add("TRẠNG_THÁI", "Trạng thái bước",
                trangThai == null || trangThai.isBlank() ? "Đạt" : trangThai.trim(), "");
    }

    public static boolean isKhoi(Action a) {
        return a != null && "KHỐI".equals(a.thaoTac());
    }

    public static boolean isTrangThaiBuoc(Action a) {
        return a != null && "TRẠNG_THÁI".equals(a.thaoTac());
    }

    private static void add(String thaoTac, String truong, String giaTri, String ghiChu) {
        if (!Boolean.TRUE.equals(ENABLED.get())) {
            return;
        }
        ACTIONS.get().add(new Action(thaoTac, truong, giaTri, ghiChu));
    }

    private static String cleanField(String name) {
        if (name == null) {
            return "";
        }
        String t = name.trim();
        // Bỏ khung ngoặc vuông thường dùng trong log console: Ô nhập [Họ và tên]
        if (t.contains("[") && t.contains("]")) {
            return t;
        }
        return t;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}

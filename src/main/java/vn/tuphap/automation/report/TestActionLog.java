package vn.tuphap.automation.report;

import java.util.HashSet;
import java.util.Set;

/**
 * Ghi lại <b>những gì automation thực sự làm</b> trên giao diện — điền gì, chọn gì, tải lên gì.
 * <p>
 * Đây là lớp từ vựng: các lớp trang gọi {@code dien}/{@code chon}/{@code taiLen}… bằng đúng cách
 * người dùng mô tả thao tác, còn việc nó hiện ra sao trên báo cáo là chuyện của
 * {@link BaoCaoData}. Nhờ vậy thêm một loại thao tác mới chỉ phải sửa ở đây.
 * <p>
 * Trước đây lớp này còn giữ một bộ đệm {@code ThreadLocal<List<Action>>} để bộ xuất Excel rút ra
 * cuối mỗi case, kèm hai loại đánh dấu cấu trúc ({@code KHỐI} / {@code TRẠNG_THÁI}) chỉ có ý nghĩa
 * với bố cục bảng tính. Bỏ Excel thì không còn ai rút, nên bộ đệm và hai đánh dấu đó đã gỡ —
 * dữ liệu chảy thẳng vào bộ thu.
 */
public final class TestActionLog {

    private static final ThreadLocal<Boolean> BAT = ThreadLocal.withInitial(() -> Boolean.TRUE);
    private static final ThreadLocal<Set<String>> DA_LECH = ThreadLocal.withInitial(HashSet::new);

    private TestActionLog() {
    }

    /** Bắt đầu testcase mới. */
    public static void beginTest() {
        DA_LECH.get().clear();
        BAT.set(Boolean.TRUE);
    }

    /** Tắt ghi (vd. lúc đăng nhập ở {@code @BeforeClass}) để không lẫn vào kịch bản. */
    public static void pause() {
        BAT.set(Boolean.FALSE);
    }

    public static void resume() {
        BAT.set(Boolean.TRUE);
        DA_LECH.get().clear();
    }

    public static void dien(String truong, String giaTri) {
        add("Điền", truong, giaTri, "");
    }

    public static void dienMask(String truong, String giaTri) {
        add("Điền", truong, giaTri, "Ô có định dạng đặc biệt");
    }

    /** Field điền xong nhưng giá trị thật trên UI khác giá trị đã gõ (nghi UI lọc/mask). */
    public static void dienLechGiaTri(String truong, String daGo, String thucTe) {
        add("Điền", truong, thucTe, "⚠ Lệch giá trị — đã gõ: '" + nz(daGo) + "'");
    }

    /**
     * True nếu cặp (field, giá trị đã gõ) này <b>chưa từng</b> lệch trong kịch bản hiện tại.
     * <p>
     * Dùng để chỉ cảnh báo và chụp ảnh lệch giá trị một lần mỗi case: vòng soát lại sau VNeID gõ
     * lại đúng field đó và ra đúng kết quả cũ, báo lần nữa chỉ làm loãng báo cáo.
     */
    public static boolean firstTimeMismatch(String truong, String daGo) {
        return DA_LECH.get().add(nz(truong).trim() + " " + nz(daGo));
    }

    public static void chon(String truong, String giaTri) {
        add("Chọn", truong, giaTri, "");
    }

    public static void timKiemDropdown(String giaTri) {
        add("Tìm kiếm dropdown", "Ô tìm kiếm trong Dropdown", giaTri, "");
    }

    public static void taiLen(String truong, String tenFile) {
        add("Tải lên", truong, tenFile, "");
    }

    public static void click(String truong) {
        add("Click", truong, "", "");
    }

    public static void boQua(String truong, String lyDo) {
        add("Bỏ qua", truong, "", lyDo);
    }

    public static void ghiChu(String noiDung) {
        add("Ghi chú", "", "", noiDung);
    }

    /** Thông báo validate / server trên biểu mẫu. */
    public static void validation(String buoc, String message) {
        add("Validation", nz(buoc).trim(), nz(message).trim(), "Thông báo từ biểu mẫu / server");
    }

    private static void add(String thaoTac, String truong, String giaTri, String ghiChu) {
        if (!Boolean.TRUE.equals(BAT.get())) {
            return;
        }
        BaoCaoData.hanhDong(thaoTac, nz(truong).trim(), nz(giaTri), nz(ghiChu));
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}

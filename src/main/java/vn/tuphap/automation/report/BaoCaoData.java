package vn.tuphap.automation.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thu thập toàn bộ nội dung báo cáo <b>độc lập với Extent</b>.
 * <p>
 * Extent chỉ còn đóng vai bộ vẽ: mọi dữ liệu (kịch bản, bước, sự kiện log, ảnh, stack trace) đều
 * do dự án tự sinh ra. Gom lại đây để có thể dựng báo cáo riêng — tự chứa trong một file, bố cục
 * do mình quyết định — thay vì tiếp tục đi vòng qua các đặc tính của thư viện:
 * {@code assignCategory()} nuốt khoảng trắng, {@code warning} đè lên {@code pass}, chế độ ngoại
 * tuyến vẫn cần thư mục tài nguyên 1.7 MB kèm theo, bảng điều khiển đếm cả mục thiết lập phiên.
 * <p>
 * Điểm chèn nằm gọn trong {@link BaoCao}: mọi lời gọi ghi log đã đi qua đó, nên không
 * phải sửa một dòng nào ở {@code WebUI}, {@code TaoDonFlow} hay các lớp trang.
 */
public final class BaoCaoData {

    /** Mức của một sự kiện — quyết định màu hiển thị. */
    public static final String MUC_PASS = "pass";
    public static final String MUC_FAIL = "fail";
    public static final String MUC_INFO = "info";
    public static final String MUC_NOTE = "note";
    public static final String MUC_WARN = "warn";
    public static final String MUC_SKIP = "skip";

    /**
     * @param muc     một trong các hằng {@code MUC_*}
     * @param noiDung nội dung <b>thô</b>, chưa escape — bộ vẽ tự lo
     * @param anh     đường dẫn ảnh tương đối so với {@code test-output/}, có thể {@code null}
     */
    public record SuKien(String muc, String thoiDiem, String noiDung, String anh) {
    }

    /**
     * Một thao tác trên giao diện kèm dữ liệu đã nhập — {@code Họ và tên → Điền → Nguyễn Văn A}.
     * <p>
     * Trước đây dữ liệu này chỉ chảy vào file Excel, nên xem một case hỏng mà muốn biết nó đã nhập
     * gì thì phải mở thêm bảng tính. Đưa vào đây để báo cáo tự trả lời được câu hỏi đó.
     */
    public record HanhDong(String thaoTac, String truong, String giaTri, String ghiChu) {
    }

    public record BuocBaoCao(int soBuoc, String ten, String trangThai, long thoiGianMs,
                             List<SuKien> suKien, List<HanhDong> hanhDong) {
    }

    /**
     * Một dòng của bảng tóm tắt 6 bước ở cuối kịch bản.
     * <p>
     * Khác {@link BuocBaoCao} ở chỗ liệt kê <b>đủ cả 6 bước</b>, kể cả bước luồng chưa chạy tới —
     * thứ mà danh sách bước đã mở không tự trả lời được. Đây là phần chống báo cáo suy đoán: bước
     * không có mặt trong {@link StepOutcome} thì phải hiện "Chưa chạy tới", không được hiện là đạt.
     *
     * @param thoiGianMs {@code -1} nghĩa là không đo được (bước chưa từng chạy)
     */
    public record TomTatBuoc(int soBuoc, String ten, String ketQua, long thoiGianMs) {
    }

    /**
     * @param suKienDau sự kiện xảy ra <b>trước</b> bước đầu tiên (vd. dòng giới thiệu kịch bản)
     * @param suKienCuoi sự kiện xảy ra <b>sau</b> khi các bước đã đóng (vd. thông báo kịch bản
     *                   thất bại). Tách hai danh sách để báo cáo giữ đúng thứ tự thời gian —
     *                   gộp làm một thì dòng lỗi cuối cùng bị đẩy lên trên các bước chạy trước nó.
     */
    /**
     * @param ketQuaMongDoi điều kịch bản <b>đáng lẽ</b> phải cho ra
     * @param ketQuaThucTe  điều nó thực sự cho ra
     * @param ghiChuKetQua  giải thích thêm khi hai thứ trên chưa đủ rõ
     */
    public record CaseBaoCao(String maCase, String tieuDe, String moTa, List<String> nhan,
                             String trangThai, long thoiGianMs, List<BuocBaoCao> buoc,
                             List<SuKien> suKienDau, List<SuKien> suKienCuoi, String stackTrace,
                             List<TomTatBuoc> tomTatBuoc,
                             String ketQuaMongDoi, String ketQuaThucTe, String ghiChuKetQua,
                             List<HanhDong> hanhDongNgoaiBuoc) {

        /** Dạng gọn cho kịch bản không có thao tác nào ngoài bước. */
        public CaseBaoCao(String maCase, String tieuDe, String moTa, List<String> nhan,
                          String trangThai, long thoiGianMs, List<BuocBaoCao> buoc,
                          List<SuKien> suKienDau, List<SuKien> suKienCuoi, String stackTrace,
                          List<TomTatBuoc> tomTatBuoc,
                          String ketQuaMongDoi, String ketQuaThucTe, String ghiChuKetQua) {
            this(maCase, tieuDe, moTa, nhan, trangThai, thoiGianMs, buoc, suKienDau, suKienCuoi,
                    stackTrace, tomTatBuoc, ketQuaMongDoi, ketQuaThucTe, ghiChuKetQua, List.of());
        }

        /** Các lượt chạy cũ chưa có trường này — Gson trả {@code null}, đừng để nổ khi dựng lại trang. */
        public List<TomTatBuoc> tomTatBuocAnToan() {
            return tomTatBuoc == null ? List.of() : tomTatBuoc;
        }

        public List<HanhDong> hanhDongNgoaiBuocAnToan() {
            return hanhDongNgoaiBuoc == null ? List.of() : hanhDongNgoaiBuoc;
        }
    }

    // ── Trạng thái đang thu thập, theo thread ──────────────────────────────

    private static final ThreadLocal<CaseDangGhi> HIEN_TAI = new ThreadLocal<>();

    /**
     * Có case nào đang mở trên thread này không.
     * <p>
     * {@code TestListener} cần biết điều này để tự tạo mục cho case bị bỏ qua — TestNG không gọi
     * {@code onTestStart} khi skip do lỗi cấu hình, nên nếu không kiểm tra thì case biến mất khỏi
     * báo cáo. Trước đây câu hỏi này hỏi đối tượng test của Extent.
     */
    public static boolean dangMoCase() {
        return HIEN_TAI.get() != null;
    }

    /**
     * Bước đang mở dở trên thread này, hoặc {@code null} nếu không có bước nào.
     * <p>
     * Nhánh lỗi cần biết điều này để ghi nhận bước đó là <b>không hoàn thành</b>. Không có nó thì
     * {@link StepOutcome} không biết bước đã chạy, và bảng tóm tắt in "Chưa chạy tới" cho đúng
     * cái bước vừa làm hỏng kịch bản.
     *
     * @param mocMoBuoc mốc thời gian mở bước, để tính thời lượng thật
     */
    public record BuocDangMo(int soBuoc, String ten, long mocMoBuoc) {
    }

    public static BuocDangMo buocDangMo() {
        CaseDangGhi c = HIEN_TAI.get();
        if (c == null || c.suKienBuoc == null || c.soBuocHienTai <= 0) {
            return null;
        }
        return new BuocDangMo(c.soBuocHienTai, c.tenBuocHienTai, c.mocMoBuoc);
    }

    /** Lỗi của case hiện tại đã kèm ảnh chưa — để listener khỏi chụp lần thứ hai. */
    public static boolean daCoAnhLoi() {
        CaseDangGhi c = HIEN_TAI.get();
        return c != null && c.daCoAnhLoi;
    }
    private static final List<CaseBaoCao> DA_XONG = Collections.synchronizedList(new ArrayList<>());

    private static final class CaseDangGhi {
        String maCase = "";
        String tieuDe = "";
        String moTa = "";
        final List<String> nhan = new ArrayList<>();
        final List<SuKien> suKienDau = new ArrayList<>();
        final List<SuKien> suKienCuoi = new ArrayList<>();
        /** Đã từng mở bước nào chưa — quyết định sự kiện cấp case rơi vào đầu hay cuối. */
        boolean daMoBuoc;
        /** Sự kiện của bước đang mở; {@code null} nghĩa là chưa mở bước nào. */
        List<SuKien> suKienBuoc;
        /** Thao tác giao diện của bước đang mở. */
        List<HanhDong> hanhDongBuoc;
        /** Thao tác xảy ra khi chưa mở bước nào — suite login nằm trọn trong nhóm này. */
        final List<HanhDong> hanhDongNgoaiBuoc = new ArrayList<>();
        int soBuocHienTai;
        String tenBuocHienTai = "";
        /** Mốc mở bước đang chạy — để đóng hộ một bước dở dang vẫn có thời gian thật. */
        long mocMoBuoc;
        final List<BuocBaoCao> buoc = new ArrayList<>();
        String stackTrace;
        /** Đã gắn được ảnh vào một sự kiện lỗi chưa. */
        boolean daCoAnhLoi;
        final List<TomTatBuoc> tomTat = new ArrayList<>();
        String ketQuaMongDoi = "";
        String ketQuaThucTe = "";
        String ghiChuKetQua = "";
    }

    private BaoCaoData() {
    }

    public static void batDauCase(String maCase, String tieuDe, String moTa, String... nhan) {
        CaseDangGhi c = new CaseDangGhi();
        c.maCase = maCase == null ? "" : maCase;
        c.tieuDe = tieuDe == null ? "" : tieuDe;
        c.moTa = moTa == null ? "" : moTa;
        if (nhan != null) {
            for (String n : nhan) {
                if (n != null && !n.isBlank()) {
                    c.nhan.add(n.trim());
                }
            }
        }
        HIEN_TAI.set(c);
    }

    public static void moBuoc(int soBuoc, String ten) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c == null) {
            return;
        }
        dongBuocNeuDangMo(c, null, TU_TINH);
        c.daMoBuoc = true;
        c.soBuocHienTai = soBuoc;
        c.tenBuocHienTai = ten == null ? "" : ten;
        c.mocMoBuoc = System.currentTimeMillis();
        c.suKienBuoc = new ArrayList<>();
        c.hanhDongBuoc = new ArrayList<>();
    }

    public static void dongBuoc(String trangThai, long thoiGianMs) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c != null) {
            dongBuocNeuDangMo(c, trangThai, thoiGianMs);
        }
    }

    /**
     * Truyền vào {@code thoiGianMs} để yêu cầu tự đo từ lúc mở bước.
     * <p>
     * Cần đến vì các bước <b>dở dang</b> không đi qua {@code logStepDone} — nhánh lỗi ném ngoại lệ
     * rồi {@link #ketThucCase} đóng hộ. Trước đây chỗ đó truyền thẳng 0, nên báo cáo in
     * "Thất bại — 0 mili giây" cho đúng cái bước tốn thời gian nhất.
     */
    private static final long TU_TINH = -1L;

    private static void dongBuocNeuDangMo(CaseDangGhi c, String trangThai, long thoiGianMs) {
        if (c.suKienBuoc == null) {
            return;
        }
        long thucTe = thoiGianMs >= 0 ? thoiGianMs
                : Math.max(0L, System.currentTimeMillis() - c.mocMoBuoc);
        c.buoc.add(new BuocBaoCao(c.soBuocHienTai, c.tenBuocHienTai,
                trangThai == null ? "—" : trangThai, thucTe, List.copyOf(c.suKienBuoc),
                c.hanhDongBuoc == null ? List.of() : List.copyOf(c.hanhDongBuoc)));
        c.suKienBuoc = null;
        c.hanhDongBuoc = null;
    }

    /** Ghi một sự kiện vào bước đang mở, hoặc vào thân case nếu chưa mở bước nào. */
    public static void suKien(String muc, String noiDung, String anh) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c == null) {
            return;
        }
        SuKien sk = new SuKien(muc, gio(), noiDung == null ? "" : noiDung, anh);
        if (MUC_FAIL.equals(muc) && anh != null && !anh.isBlank()) {
            c.daCoAnhLoi = true;
        }
        if (c.suKienBuoc != null) {
            c.suKienBuoc.add(sk);
        } else if (c.daMoBuoc) {
            c.suKienCuoi.add(sk);
        } else {
            c.suKienDau.add(sk);
        }
    }

    static final String MAT_KHAU_AN = "•••••• (đã ẩn)";

    /**
     * Ô này có phải ô mật khẩu không. Giá trị của nó không được rơi vào báo cáo hay
     * {@code bao-cao.json}: báo cáo là thứ đem đi chia sẻ, còn mật khẩu là bí mật của môi trường.
     */
    private static boolean laOMatKhau(String truong) {
        String t = truong.toLowerCase(java.util.Locale.ROOT);
        return t.contains("mật khẩu") || t.contains("password");
    }

    /**
     * Ghi một thao tác giao diện vào bước đang mở, hoặc vào phần thân case nếu chưa mở bước nào.
     * <p>
     * Trước đây thao tác ngoài bước bị bỏ hẳn, nên suite login — vốn không đi qua 6 bước nộp đơn —
     * ra báo cáo trắng trơn phần "dữ liệu đã nhập", đúng lúc người đọc cần biết nó đã gõ tài khoản
     * và captcha nào. Thao tác lúc đăng nhập chuẩn bị cho kịch bản khác vẫn không lẫn vào vì
     * {@code TestActionLog.pause()} đã tắt ghi ở đó.
     */
    public static void hanhDong(String thaoTac, String truong, String giaTri, String ghiChu) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c == null) {
            return;
        }
        String ten = truong == null ? "" : truong;
        HanhDong hd = new HanhDong(
                thaoTac == null ? "" : thaoTac,
                ten,
                laOMatKhau(ten) ? MAT_KHAU_AN : giaTri == null ? "" : giaTri,
                ghiChu == null ? "" : ghiChu);
        if (c.hanhDongBuoc != null) {
            c.hanhDongBuoc.add(hd);
        } else {
            c.hanhDongNgoaiBuoc.add(hd);
        }
    }

    /**
     * Ba trường kết luận của một kịch bản. Gọi lại thì ghi đè — lần gọi cuối thắng, vì luồng chạy
     * có thể tinh chỉnh lại kết luận khi biết thêm (vd. có toast lỗi từ server sau khi Gửi đơn).
     * <p>
     * Chuỗi rỗng nghĩa là "xoá đi", không phải "giữ nguyên": {@code setGhiChuKetQua("")} là cách
     * nhánh thành công dọn ghi chú của lần thử trước.
     */
    public static void ketQuaMongDoi(String v) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c != null) {
            c.ketQuaMongDoi = v == null ? "" : v.trim();
        }
    }

    public static void ketQuaThucTe(String v) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c != null) {
            c.ketQuaThucTe = v == null ? "" : v.trim();
        }
    }

    public static void ghiChuKetQua(String v) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c != null) {
            c.ghiChuKetQua = v == null ? "" : v.trim();
        }
    }

    /** Ghi một dòng của bảng tóm tắt 6 bước. Gọi lại cùng số bước thì ghi đè dòng cũ. */
    public static void tomTatBuoc(int soBuoc, String ten, String ketQua, long thoiGianMs) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c == null) {
            return;
        }
        c.tomTat.removeIf(x -> x.soBuoc() == soBuoc);
        c.tomTat.add(new TomTatBuoc(soBuoc, ten == null ? "" : ten,
                ketQua == null ? "—" : ketQua, thoiGianMs));
        c.tomTat.sort(java.util.Comparator.comparingInt(TomTatBuoc::soBuoc));
    }

    public static void stackTrace(Throwable t) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c == null || t == null) {
            return;
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        c.stackTrace = sw.toString();
    }

    /** Kết thúc case và đưa vào danh sách của lượt chạy. */
    public static void ketThucCase(String trangThai, long thoiGianMs) {
        CaseDangGhi c = HIEN_TAI.get();
        if (c == null) {
            return;
        }
        dongBuocNeuDangMo(c, trangThai, TU_TINH);
        DA_XONG.add(new CaseBaoCao(c.maCase, c.tieuDe, c.moTa, List.copyOf(c.nhan),
                trangThai == null ? "—" : trangThai, thoiGianMs,
                List.copyOf(c.buoc), List.copyOf(c.suKienDau), List.copyOf(c.suKienCuoi),
                c.stackTrace, List.copyOf(c.tomTat),
                c.ketQuaMongDoi, c.ketQuaThucTe, c.ghiChuKetQua,
                List.copyOf(c.hanhDongNgoaiBuoc)));
        HIEN_TAI.remove();
    }

    /** Danh sách case của lượt chạy, sắp theo mã case để hai lượt đối chiếu được với nhau. */
    public static List<CaseBaoCao> cases() {
        synchronized (DA_XONG) {
            List<CaseBaoCao> out = new ArrayList<>(DA_XONG);
            out.sort((a, b) -> a.maCase().compareToIgnoreCase(b.maCase()));
            return out;
        }
    }

    /**
     * Bỏ case đang mở mà <b>không</b> đưa vào lượt chạy.
     * <p>
     * Dùng cho mục "Thiết lập session — đăng nhập": nó gọi {@code createTest} để có chỗ ghi log,
     * nhưng nó không phải kịch bản kiểm thử và không được đếm. Bảng điều khiển Extent đếm nhầm
     * đúng chỗ này, khiến "3 đạt" trên bảng đá với "0 đạt" ở dòng tổng kết.
     * <p>
     * Quan trọng hơn: không dọn thì case rác đọng lại trên thread, {@link #dangMoCase()} trả về
     * {@code true}, và kịch bản bị TestNG bỏ qua sẽ bị ghi đè lên mục thiết lập session thay vì có
     * mục riêng.
     */
    public static void huyCaseDangMo() {
        HIEN_TAI.remove();
    }

    public static void xoaHet() {
        DA_XONG.clear();
        HIEN_TAI.remove();
    }

    private static String gio() {
        return LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}

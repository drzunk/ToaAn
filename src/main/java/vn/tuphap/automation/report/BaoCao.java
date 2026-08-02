package vn.tuphap.automation.report;

import java.util.List;

/**
 * Mặt tiền ghi báo cáo — mọi lời gọi log của cả dự án đi qua đây.
 * <p>
 * Đây là lớp {@code ExtentReportManager} cũ sau khi rút hết phần ExtentReports. Dữ liệu chảy vào
 * {@link BaoCaoData}, rồi {@link BaoCaoHtml} dựng ra {@code test-output/index.html}. Giữ nguyên
 * bề mặt hàm nên các lớp gọi không phải đổi gì ngoài cái tên.
 * <p>
 * Lý do bỏ Extent, ghi lại để khỏi có người mang về:
 * <ul>
 *   <li>Mỗi lượt chạy sinh thêm ~2.5 MB ({@code ExtentReport.html} 829 KB + thư mục
 *       {@code spark/} 1.7 MB tài nguyên giao diện) mà không ai mở nữa.</li>
 *   <li>{@code assignCategory()} nuốt khoảng trắng — "Dân sự" hiện thành "Dânsự"; cách chữa duy
 *       nhất là nhét ký tự trắng không ngắt, đổi lại ô tìm kiếm không khớp nữa.</li>
 *   <li>{@code warning} xếp cao hơn {@code pass}, nên một case đúng mà có cảnh báo chẩn đoán lại
 *       đeo huy hiệu cam.</li>
 *   <li>Bảng điều khiển đếm cả mục "Thiết lập phiên đăng nhập" là kịch bản, lệch với dòng tổng
 *       kết ngay trên cùng một màn hình.</li>
 * </ul>
 * Các hàm chỉ phục vụ Extent — {@code initReport}, {@code flushReport}, {@code archiveReport},
 * {@code logSuiteSummary}, {@code logTable} — đã bỏ hẳn cùng lời gọi của chúng.
 */
public class BaoCao {

    /** Mốc bắt đầu case, để tính thời lượng khi listener đóng case. */
    private static final ThreadLocal<Long> TEST_START_MS = new ThreadLocal<>();

    /** Mã case của thread hiện tại — do listener đặt trước khi mở case. */
    private static final ThreadLocal<String> CASE_CODE = ThreadLocal.withInitial(() -> "");

    // ── Vòng đời một kịch bản ──────────────────────────────────────────────

    public static void setCaseCode(String code) {
        CASE_CODE.set(code == null ? "" : code);
    }

    public static void createTest(String testName, String description, String... categories) {
        BaoCaoData.batDauCase(CASE_CODE.get(), testName, description, categories);
        TEST_START_MS.set(System.currentTimeMillis());
    }

    public static boolean hasCurrentTest() {
        return BaoCaoData.dangMoCase();
    }

    public static long getTestElapsedMs() {
        Long start = TEST_START_MS.get();
        return start == null ? 0 : System.currentTimeMillis() - start;
    }

    /**
     * Dọn ngữ cảnh case của thread. Bỏ luôn case còn đang mở — sau {@code ketThucCase} thì không
     * còn gì để bỏ, nhưng mục "Thiết lập session" không đi qua đó nên phải dọn ở đây, nếu không nó
     * đọng lại và nuốt mất kịch bản bị bỏ qua kế tiếp.
     */
    public static void clearTestContext() {
        BaoCaoData.huyCaseDangMo();
        TEST_START_MS.remove();
        // PHẢI xoá mã case. Không xoá thì kịch bản bị TestNG bỏ qua mà không gọi onTestStart (skip
        // do @BeforeMethod hỏng — chính là ca đăng nhập lỗi) sẽ nhặt mã của kịch bản chạy trước
        // trên cùng thread: hai mục cùng mã, hai thẻ HTML trùng id, nhãn so-với-lượt-trước tính
        // sai, và ảnh nằm ở thư mục mang mã khác.
        CASE_CODE.remove();
    }

    /**
     * True nếu lỗi <b>đã có ảnh</b> kèm theo. Chỉ khi đó {@code TestListener} mới được bỏ chụp bổ
     * sung — lỗi mới ghi bằng chữ thì vẫn phải chụp.
     * <p>
     * Hồi "đã ghi fail" và "fail đã có ảnh" chung một cờ, mọi {@code logFail} bằng chữ (vd. lỗi
     * chung {@code RuntimeException}) đều chặn luôn việc chụp — đúng loại lỗi cần bằng chứng nhất
     * lại thành loại không có ảnh.
     */
    public static boolean wasFailScreenshotAttached() {
        return BaoCaoData.daCoAnhLoi();
    }

    // ── Kết luận của một kịch bản ──────────────────────────────────────────

    /**
     * Ba trường kết luận từng là đặc sản của file Excel — <i>Kết quả mong đợi</i>,
     * <i>Kết quả thực tế</i>, <i>Ghi chú</i>. Đây là thứ người đọc báo cáo hỏi đầu tiên khi một
     * kịch bản hỏng, nên chúng thuộc về báo cáo chính chứ không phải một file phải mở kèm.
     */
    public static void ketQuaMongDoi(String noiDung) {
        BaoCaoData.ketQuaMongDoi(noiDung);
    }

    public static void ketQuaThucTe(String noiDung) {
        BaoCaoData.ketQuaThucTe(noiDung);
    }

    public static void ghiChuKetQua(String noiDung) {
        BaoCaoData.ghiChuKetQua(noiDung);
    }

    // ── Bước ───────────────────────────────────────────────────────────────

    public static void beginStepNode(int step, int total, String name) {
        BaoCaoData.moBuoc(step, tenBuoc(step, name));
    }

    /**
     * Kết thúc phần ghi của bước mà <b>chưa</b> chốt kết quả — bước vẫn ở trạng thái đang mở cho
     * tới khi có {@code beginStepNode} kế tiếp hoặc case đóng lại.
     */
    public static void endStepNode() {
        // Không còn node nào để đóng; giữ hàm để các lời gọi hiện có không phải đổi.
    }

    /**
     * Đóng bước kèm kết quả thật — dùng cho các mốc không đi qua {@link #logStepDone}, ví dụ bước
     * mở biểu mẫu. Thiếu nó thì bước hiện trạng thái "—" và 0 mili giây trên báo cáo.
     */
    public static void endStepNode(String trangThai, long thoiGianMs) {
        BaoCaoData.dongBuoc(trangThai, thoiGianMs);
    }

    /**
     * Ghi nhận một bước <b>không hoàn thành</b> — dùng ở nhánh lỗi.
     * <p>
     * Thiếu lời gọi này thì {@link StepOutcome} không hề biết bước đó đã chạy, và bảng tóm tắt in
     * <b>"Chưa chạy tới"</b> cho đúng cái bước vừa làm hỏng kịch bản: đảo chiều của chính kiểu báo
     * cáo suy đoán mà bảng này sinh ra để diệt.
     *
     * Tự lấy bước đang mở từ bộ thu nên nhánh lỗi không phải nhớ mình đang ở bước nào — đó chính
     * là lý do hàm tương tự trước đây có 0 điểm gọi suốt cả dự án.
     */
    public static void ghiBuocDangMoLaHong() {
        BaoCaoData.BuocDangMo b = BaoCaoData.buocDangMo();
        if (b == null) {
            return;
        }
        StepOutcome.record(b.soBuoc(), tenBuoc(b.soBuoc(), b.ten()),
                Math.max(0L, System.currentTimeMillis() - b.mocMoBuoc()), false);
    }

    public static long markStepStart() {
        return System.currentTimeMillis();
    }

    public static void logStepDone(int step, int total, String name, long startMs) {
        logStepDone(step, total, name, startMs, null);
    }

    public static void logStepDone(int step, int total, String name, long startMs,
                                   List<String> screenshots) {
        long elapsed = System.currentTimeMillis() - startMs;
        String ten = tenBuoc(step, name);
        // Cố ý không in ra console: luồng đã tự in mốc hoàn thành bước, in thêm là nhân đôi.
        BaoCaoData.suKien(BaoCaoData.MUC_PASS, String.format(
                "Hoàn thành bước %d/%d: %s — thời gian thực hiện %s",
                step, total, ten, TaoDonReportBuilder.formatDuration(elapsed)), null);
        logScreenshots("Ảnh ngữ cảnh — " + ten, screenshots);
        StepOutcome.record(step, ten, elapsed, true);
        BaoCaoData.dongBuoc("Đạt", elapsed);
    }

    /**
     * Bảng tóm tắt 6 bước ở cuối case — nói rõ bước nào chạy xong, bước nào <b>chưa chạy tới</b>.
     * <p>
     * Đây là chỗ chống lại kiểu báo cáo suy đoán: bước không có mặt trong {@link StepOutcome} nghĩa
     * là luồng chưa từng chạy tới nó, không được phép hiện là đạt. Danh sách bước trong
     * {@link BaoCaoData} chỉ chứa các bước <i>đã mở</i>, nên không tự trả lời được câu hỏi này —
     * phải dựng riêng.
     */
    public static void logStepSummary() {
        // KHÔNG thoát sớm khi chưa bước nào xong. Kịch bản hỏng ngay ở bước 1 là lúc người đọc cần
        // bảng này nhất, mà bản cũ lại giấu nó đi đúng lúc đó.
        int caoNhat = StepOutcome.highestReached();
        for (int i = 1; i <= 6; i++) {
            StepOutcome.Step s = StepOutcome.get(i);
            String ketQua;
            if (s == null) {
                ketQua = i <= caoNhat ? TrangThai.KHONG_HOAN_THANH : TrangThai.CHUA_CHAY_TOI;
            } else {
                ketQua = s.completed() ? TrangThai.DAT : TrangThai.KHONG_HOAN_THANH;
            }
            BaoCaoData.tomTatBuoc(i,
                    s != null ? s.name() : TaoDonReportBuilder.tenBuocDayDu(i),
                    ketQua,
                    s == null ? -1 : s.durationMs());
        }
    }

    // ── Ghi log ────────────────────────────────────────────────────────────

    public static void logInfo(String message) {
        BaoCaoData.suKien(BaoCaoData.MUC_INFO, message, null);
    }

    /**
     * Ghi chú chẩn đoán — hiện nền vàng nhưng <b>không</b> đổi trạng thái case. Để dành
     * {@link #logWarning} cho thứ thật sự đáng ngờ.
     */
    public static void logNoteWithScreenshot(String message, String base64Image) {
        BaoCaoData.suKien(BaoCaoData.MUC_NOTE, message, luuAnh(base64Image, "ghi-chu-" + message));
        System.out.println("ℹ " + message);
    }

    public static void logPass(String message) {
        BaoCaoData.suKien(BaoCaoData.MUC_PASS, message, null);
        System.out.println("🎉 " + message);
    }

    public static void logPassWithScreenshot(String message, String base64Image) {
        BaoCaoData.suKien(BaoCaoData.MUC_PASS, message, luuAnh(base64Image, message));
        System.out.println("🎉 " + message);
    }

    public static void logWarning(String message) {
        BaoCaoData.suKien(BaoCaoData.MUC_WARN, message, null);
        System.out.println("⚠️ [CẢNH BÁO] " + message);
    }

    public static void logWarningWithScreenshot(String message, String base64Image) {
        BaoCaoData.suKien(BaoCaoData.MUC_WARN, message, luuAnh(base64Image, "canh-bao-" + message));
        System.out.println("⚠️ [CẢNH BÁO] " + message);
    }

    public static void logFail(String message) {
        if (!BaoCaoData.dangMoCase()) {
            System.out.println("❌ [THẤT BẠI] (chưa gắn kịch bản báo cáo) " + message);
            return;
        }
        BaoCaoData.suKien(BaoCaoData.MUC_FAIL, message, null);
        System.out.println("❌ [THẤT BẠI] " + message);
    }

    public static void logFailWithScreenshot(String message, String base64Image) {
        if (!BaoCaoData.dangMoCase()) {
            System.out.println("❌ [THẤT BẠI] (chưa gắn kịch bản báo cáo) " + message);
            return;
        }
        // Không thêm tiền tố "Kịch bản thất bại." — huy hiệu trạng thái ngay cạnh đã nói điều đó,
        // lặp lại chỉ đẩy nội dung thật ra xa mắt người đọc.
        String detail = nullToEmpty(message).replace('\n', ' ').replaceAll("\\s+", " ").trim();
        BaoCaoData.suKien(BaoCaoData.MUC_FAIL, detail, luuAnh(base64Image, "loi-" + message));
        System.out.println("❌ " + message);
    }

    public static void logSkip(String message) {
        BaoCaoData.suKien(BaoCaoData.MUC_SKIP, message, null);
    }

    /** Đẩy nguyên {@link Throwable} vào báo cáo — không có nó thì phải mở file log mới biết dòng nào. */
    public static void logThrowable(Throwable t) {
        BaoCaoData.stackTrace(t);
    }

    // ── Ảnh ────────────────────────────────────────────────────────────────

    /**
     * Đính kèm 1–3 ảnh: Đầu / Giữa / Cuối biểu mẫu.
     * <p>
     * Đây từng là hàm duy nhất trong lớp không ghi sang bộ thu — ảnh ngữ cảnh theo bước vì thế chỉ
     * tồn tại trong Extent, và gỡ thư viện đi là chúng biến mất mà không báo lỗi gì. Chốt lại bằng
     * {@code BaoCaoTeeTest}.
     */
    public static void logScreenshots(String caption, List<String> screenshots) {
        if (screenshots == null || screenshots.isEmpty()) {
            return;
        }
        String[] phan = {"Đầu biểu mẫu (phía trên)", "Giữa biểu mẫu", "Cuối biểu mẫu (phía dưới)"};
        int n = screenshots.size();
        for (int i = 0; i < n; i++) {
            String anh = screenshots.get(i);
            if (anh == null || anh.isBlank()) {
                continue;
            }
            // Một ảnh thì chú thích gốc đã đủ — thêm "Toàn bộ khung nhìn hiện tại (1/1)" là nói
            // thừa. Chỉ đánh số khi thật sự có nhiều ảnh để người đọc biết đang xem đoạn nào.
            String moTa = n == 1 ? caption
                    : caption + " — " + (i < phan.length ? phan[i] : ("Đoạn " + (i + 1)))
                      + " (" + (i + 1) + "/" + n + ")";
            BaoCaoData.suKien(BaoCaoData.MUC_INFO, moTa, luuAnh(anh, caption));
        }
    }

    /**
     * Ghi ảnh ra đĩa và trả về đường dẫn tương đối so với {@code test-output/}.
     * <p>
     * Nhúng base64 thẳng vào HTML từng làm file báo cáo phình lên 16.8 MB cho một lượt 39 case, và
     * Excel không dẫn link tới ảnh được vì trên đĩa không có file nào. Ghi ra đĩa giải quyết cả hai.
     *
     * @return {@code null} khi không có ảnh hoặc không ghi được — nhánh gọi tự lùi về chỉ có chữ
     */
    static String luuAnh(String base64, String label) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        ScreenshotStore.Shot shot = ScreenshotStore.save(base64, label);
        return shot != null && shot.hasFile() ? shot.relPath() : null;
    }

    // ── Tiện ích ───────────────────────────────────────────────────────────

    private static String tenBuoc(int step, String name) {
        return (name == null || name.isBlank()) ? TaoDonReportBuilder.tenBuocDayDu(step) : name;
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private BaoCao() {
    }
}

package vn.tuphap.automation.report;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ghi ảnh chụp ra file PNG và trả về đường dẫn tương đối để báo cáo tham chiếu.
 * <p>
 * Trước đây mọi ảnh được nhúng thẳng vào HTML dạng base64: file báo cáo phình lên <b>16.8 MB</b>
 * cho một lượt 39 case (~122 ảnh), mở rất chậm, và <b>Excel không thể trỏ tới ảnh</b> vì trên đĩa
 * không hề có file nào. Ghi ra đĩa giải quyết cả ba: HTML chỉ còn thẻ {@code <img src>} trỏ file,
 * Excel gắn hyperlink được, và ảnh của từng lượt chạy được lưu lại thay vì bị ghi đè.
 * <p>
 * Bố cục: {@code test-output/runs/<runStamp>/screenshots/<maCase>/<NN>-<nhãn>.png} — cả ảnh, dữ
 * liệu báo cáo lẫn file Excel của một lượt dùng chung {@code runStamp}, nên nén cả thư mục lượt
 * chạy gửi đi là trọn vẹn.
 */
public final class ScreenshotStore {

    /** Thư mục gốc của mọi kết xuất — mọi đường dẫn tương đối đều tính từ đây. */
    private static final Path OUTPUT_ROOT = Paths.get("test-output");

    /** Bề rộng tối đa khi lưu; ảnh rộng hơn sẽ được thu nhỏ giữ nguyên tỉ lệ. */
    private static final int MAX_WIDTH = 1600;

    private static volatile LocalDateTime runStartedAt = LocalDateTime.now();
    private static volatile String runStamp =
            runStartedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    private static final ThreadLocal<String> CASE_DIR = ThreadLocal.withInitial(() -> "chung");
    private static final ThreadLocal<AtomicInteger> SEQ =
            ThreadLocal.withInitial(() -> new AtomicInteger(0));

    private ScreenshotStore() {
    }

    /**
     * Ảnh chụp đã lưu.
     *
     * @param base64  dữ liệu gốc — giữ lại cho các nhánh còn cần base64
     * @param relPath đường dẫn tương đối so với {@code test-output/}, vd.
     *                {@code screenshots/20260802_021118/TC_MID_003/01-loi-gui-don.png};
     *                {@code null} nếu không ghi được file (báo cáo sẽ tự lùi về chế độ chỉ có chữ)
     */
    public record Shot(String base64, String relPath) {
        public boolean hasFile() {
            return relPath != null && !relPath.isBlank();
        }
    }

    /**
     * Công tắc chung cho toàn bộ việc chụp ảnh: {@code -Dtaodon.screenshot=false}.
     * <p>
     * Trước đây cờ này nằm trong {@code WebUI} và chỉ chặn ba hàm của chính lớp đó, còn
     * {@code TestListener} gọi thẳng {@code getScreenshotAs} nên tắt cờ vẫn sinh ảnh cho mọi case
     * hỏng. Gom về đây để một chỗ quyết định.
     */
    public static boolean enabled() {
        return !"false".equalsIgnoreCase(System.getProperty("taodon.screenshot", "true"));
    }

    /** Đặt mốc thời gian dùng chung cho cả lượt chạy — gọi một lần ở đầu suite. */
    public static void initRun() {
        runStartedAt = LocalDateTime.now();
        runStamp = runStartedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    public static String runStamp() {
        return runStamp;
    }

    /**
     * Thời điểm bắt đầu lượt chạy — cùng một instant đã sinh ra {@link #runStamp()}.
     * <p>
     * Báo cáo phải ghi giờ <b>bắt đầu</b>, không phải giờ kết thúc: dòng tiêu đề và liên kết
     * {@code runs/<mốc>/} ngay dưới nó nói cùng một giờ thì mới ghép được với log và ảnh.
     */
    public static LocalDateTime runStartedAt() {
        return runStartedAt;
    }

    /** Mở một case mới trên thread hiện tại — ảnh sau đó gom vào thư mục riêng của case. */
    public static void beginCase(String caseLabel) {
        CASE_DIR.set(slug(caseLabel, "chung"));
        SEQ.get().set(0);
    }

    public static void clearCase() {
        CASE_DIR.remove();
        SEQ.remove();
    }

    /**
     * Lưu ảnh ra đĩa. Không bao giờ ném — hỏng thì trả {@link Shot} chỉ có base64 để nhánh gọi
     * vẫn báo cáo được, chỉ mất phần liên kết file.
     *
     * @param base64 ảnh gốc; {@code null}/rỗng trả về {@code null}
     * @param label  nhãn ngắn để đặt tên file (vd. "loi-gui-don", "buoc-2")
     */
    public static Shot save(String base64, String label) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            byte[] png = toPngBytes(base64);
            String name = String.format("%02d-%s.png", SEQ.get().incrementAndGet(), slug(label, "anh"));
            // Ảnh nằm TRONG thư mục lượt chạy, cạnh dữ liệu báo cáo của chính lượt đó —
            // nhờ vậy một thư mục runs/<mốc>/ là trọn vẹn: copy/zip đi đâu cũng còn ảnh.
            Path dir = OUTPUT_ROOT.resolve("runs").resolve(runStamp)
                    .resolve("screenshots").resolve(CASE_DIR.get());
            Files.createDirectories(dir);
            Files.write(dir.resolve(name), png);
            String rel = "runs/" + runStamp + "/screenshots/" + CASE_DIR.get() + "/" + name;
            return new Shot(base64, rel);
        } catch (Exception e) {
            System.out.println(" ⚠ Không ghi được ảnh ra đĩa (" + label + "): " + e.getMessage());
            return new Shot(base64, null);
        }
    }

    /**
     * Giải mã base64 và thu nhỏ nếu rộng hơn {@link #MAX_WIDTH}.
     * <p>
     * Ảnh gốc là PNG toàn khung nhìn ở kích thước cửa sổ thật; giữ nguyên thì một lượt 39 case tốn
     * hàng chục MB đĩa mà người đọc báo cáo không cần tới độ phân giải đó. Hỏng ở khâu thu nhỏ thì
     * ghi nguyên bản, không để mất ảnh.
     */
    private static byte[] toPngBytes(String base64) throws Exception {
        byte[] raw = Base64.getDecoder().decode(base64);
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(raw));
            if (src == null || src.getWidth() <= MAX_WIDTH) {
                return raw;
            }
            int w = MAX_WIDTH;
            int h = Math.max(1, (int) Math.round(src.getHeight() * (MAX_WIDTH / (double) src.getWidth())));
            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, w, h, null);
            } finally {
                g.dispose();
            }
            var out = new java.io.ByteArrayOutputStream();
            ImageIO.write(scaled, "png", out);
            byte[] small = out.toByteArray();
            return small.length > 0 && small.length < raw.length ? small : raw;
        } catch (Exception e) {
            return raw;
        }
    }

    /** Tên file/thư mục an toàn: bỏ dấu tiếng Việt, chỉ giữ chữ-số-gạch. */
    private static String slug(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String s = java.text.Normalizer.normalize(raw.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (s.isBlank()) {
            return fallback;
        }
        return s.length() > 60 ? s.substring(0, 60) : s;
    }
}

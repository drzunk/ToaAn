package vn.tuphap.automation.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đo thời gian theo <b>loại thao tác</b> trong một bước, để biết thời gian thực sự đi đâu
 * (chờ dropdown? khối địa chỉ? sleep cố định? chờ chuyển bước?).
 * <p>
 * Tính <b>self-time</b>: thời gian của thao tác con bị trừ khỏi thao tác cha, nên tổng các mục
 * không bị cộng trùng khi lồng nhau (vd. Địa chỉ → Dropdown → Click → Sleep).
 * <p>
 * Theo thread (mỗi Chrome một bộ đếm riêng), bật/tắt bằng {@code -Dtaodon.profile=false}.
 */
public final class UiProfiler {

    /** Nhãn nhóm — giữ ngắn để bảng log dễ đọc. */
    public static final String DIA_CHI = "Địa chỉ (tỉnh/phường)";
    public static final String DROPDOWN = "Dropdown khác";
    public static final String DIEN_FIELD = "Điền ô nhập";
    public static final String CLICK = "Click";
    public static final String CHO_DOI = "Chờ hiển thị/chuyển bước";
    public static final String TOAST = "Thu thập toast/validation";
    public static final String ANH = "Chụp ảnh";
    public static final String SLEEP = "Sleep cố định";
    /**
     * {@code isElementVisible} dùng như phép kiểm tra "field tuỳ chọn này có không?" — mỗi lần
     * trả {@code false} phải chờ hết {@link WaitConfig#PROBE_MS}. Tách riêng để thấy rõ bao nhiêu
     * thời gian đang trôi vào việc kết luận "không có gì".
     */
    public static final String DO_FIELD = "Dò field có/không";

    private static final boolean ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("taodon.profile", "true"));

    private record Frame(String category, long startMs, long[] childMs) {
    }

    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    /** category -> [selfMs, soLan] */
    private static final ThreadLocal<Map<String, long[]>> TOTALS =
            ThreadLocal.withInitial(LinkedHashMap::new);

    private UiProfiler() {
    }

    /** Xoá bộ đếm — gọi ở đầu mỗi bước. */
    public static void reset() {
        if (!ENABLED) {
            return;
        }
        STACK.get().clear();
        TOTALS.get().clear();
    }

    public static void enter(String category) {
        if (!ENABLED) {
            return;
        }
        STACK.get().push(new Frame(category, System.currentTimeMillis(), new long[]{0L}));
    }

    public static void exit() {
        if (!ENABLED) {
            return;
        }
        Deque<Frame> stack = STACK.get();
        Frame frame = stack.poll();
        if (frame == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - frame.startMs();
        long self = Math.max(0, elapsed - frame.childMs()[0]);
        long[] acc = TOTALS.get().computeIfAbsent(frame.category(), k -> new long[]{0L, 0L});
        acc[0] += self;
        acc[1]++;
        Frame parent = stack.peek();
        if (parent != null) {
            // Trừ trọn thời gian con khỏi cha để cha chỉ còn self-time.
            parent.childMs()[0] += elapsed;
        }
    }

    /** Cộng thẳng một khoảng đã biết (dùng cho sleep — không cần bọc enter/exit). */
    public static void addDirect(String category, long ms) {
        if (!ENABLED || ms <= 0) {
            return;
        }
        long[] acc = TOTALS.get().computeIfAbsent(category, k -> new long[]{0L, 0L});
        acc[0] += ms;
        acc[1]++;
        Frame current = STACK.get().peek();
        if (current != null) {
            current.childMs()[0] += ms;
        }
    }

    /**
     * Bảng phân tích 1 dòng cho bước vừa xong, sắp giảm dần theo thời gian.
     * Trả rỗng nếu tắt profiler hoặc không có dữ liệu.
     */
    public static String summary(long tongMs) {
        if (!ENABLED) {
            return "";
        }
        Map<String, long[]> totals = TOTALS.get();
        if (totals.isEmpty()) {
            return "";
        }
        List<Map.Entry<String, long[]>> rows = new ArrayList<>(totals.entrySet());
        rows.sort((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]));
        StringBuilder sb = new StringBuilder();
        long danhSachMs = 0;
        for (Map.Entry<String, long[]> e : rows) {
            long ms = e.getValue()[0];
            danhSachMs += ms;
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(e.getKey()).append(' ').append(String.format("%.1fs", ms / 1000.0))
                    .append('×').append(e.getValue()[1]);
            if (tongMs > 0) {
                sb.append(String.format(" (%.0f%%)", ms * 100.0 / tongMs));
            }
        }
        long khac = tongMs - danhSachMs;
        if (tongMs > 0 && khac > 0) {
            sb.append(String.format(" | Khác %.1fs (%.0f%%)", khac / 1000.0, khac * 100.0 / tongMs));
        }
        return sb.toString();
    }
}

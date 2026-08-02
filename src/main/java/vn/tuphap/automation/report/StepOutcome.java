package vn.tuphap.automation.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kết quả thật của từng bước trong một case — nguồn sự thật cho bảng tóm tắt 6 bước.
 * <p>
 * Bản báo cáo cũ <b>suy đoán</b> trạng thái bước: đóng dấu <b>Đạt</b> cho mọi bước đứng trước bước
 * cuối khi không tìm thấy đánh dấu trạng thái. Nhưng luồng chỉ ghi "Đạt" khi bước <i>hoàn thành</i>,
 * nên đúng những bước dở dang mới rơi vào nhánh đoán đó — và được tô xanh. Bước chưa từng chạy vẫn
 * hiện là đã đạt.
 * <p>
 * Lớp này ghi nhận theo sự kiện thật: một bước chỉ vào danh sách khi nó thực sự kết thúc, kèm cờ
 * cho biết có hoàn thành hay không. Bước không có mặt trong danh sách nghĩa là <b>chưa chạy tới</b>.
 */
public final class StepOutcome {

    /**
     * @param index      số thứ tự bước (1..6)
     * @param name       tên đầy đủ của bước
     * @param durationMs thời gian thực hiện
     * @param completed  {@code true} = chạy xong; {@code false} = bắt đầu nhưng bị chặn/lỗi giữa chừng
     */
    public record Step(int index, String name, long durationMs, boolean completed) {
    }

    private static final ThreadLocal<Map<Integer, Step>> STEPS =
            ThreadLocal.withInitial(LinkedHashMap::new);

    private StepOutcome() {
    }

    /** Xoá sạch — gọi ở đầu mỗi case. */
    public static void beginCase() {
        STEPS.get().clear();
    }

    public static void record(int index, String name, long durationMs, boolean completed) {
        STEPS.get().put(index, new Step(index, name, durationMs, completed));
    }

    /** Danh sách bước đã ghi nhận, theo thứ tự bước. */
    public static List<Step> snapshot() {
        List<Step> out = new ArrayList<>(STEPS.get().values());
        out.sort((a, b) -> Integer.compare(a.index(), b.index()));
        return out;
    }

    /** Trạng thái của một bước — {@code null} nghĩa là chưa chạy tới bước này. */
    public static Step get(int index) {
        return STEPS.get().get(index);
    }

    /** Bước cao nhất đã chạm tới (dù hoàn thành hay không); 0 nếu chưa bước nào. */
    public static int highestReached() {
        return STEPS.get().keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public static void clear() {
        STEPS.remove();
    }
}

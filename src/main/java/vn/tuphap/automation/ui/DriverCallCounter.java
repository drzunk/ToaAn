package vn.tuphap.automation.ui;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;

import java.lang.reflect.Method;

/**
 * Đếm số <b>lượt gọi sang chromedriver</b> và tổng thời gian chờ chúng, theo từng thread.
 * <p>
 * Vì sao cần: mỗi {@code element.getText()}, {@code isDisplayed()}, {@code findElements()} là một
 * lượt HTTP riêng sang chromedriver. Một dropdown tỉnh 63 mục quét kiểu "for từng option rồi
 * getText()" tốn ~127 lượt; Playwright đọc cả danh sách trong <b>1</b> lượt vì nó chạy ngay trong
 * trang. Bảng phân tích của {@link UiProfiler} chỉ cho biết thời gian trôi vào <i>loại thao tác</i>
 * nào, không cho biết nó trôi vào <i>số lượt gọi</i> — đây là mảnh còn thiếu để so với Playwright.
 * <p>
 * <b>Mặc định TẮT</b> ({@code -Dtaodon.countCalls=true} để bật). Decorator có chi phí riêng nên chỉ
 * bật ở lượt chạy đo, không bật ở lượt chạy lấy thời gian thật.
 */
public final class DriverCallCounter {

    private static final boolean ENABLED =
            "true".equalsIgnoreCase(System.getProperty("taodon.countCalls", "false"));

    /** [số lượt gọi, tổng ms] theo thread. */
    private static final ThreadLocal<long[]> STATS = ThreadLocal.withInitial(() -> new long[]{0L, 0L});
    /** Mốc bắt đầu của lượt gọi đang chạy — 0 nghĩa là không có lượt nào đang mở. */
    private static final ThreadLocal<long[]> PENDING = ThreadLocal.withInitial(() -> new long[]{0L});

    private DriverCallCounter() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    /** Bọc driver để đếm; trả nguyên driver khi tắt (không thêm lớp proxy nào). */
    public static WebDriver wrap(WebDriver driver) {
        if (!ENABLED) {
            return driver;
        }
        return new EventFiringDecorator<>(new WebDriverListener() {
            @Override
            public void beforeAnyCall(Object target, Method method, Object[] args) {
                if (isNoise(method)) {
                    return;
                }
                long[] pending = PENDING.get();
                // Không đo lồng nhau: chỉ tính lượt ngoài cùng để khỏi cộng trùng.
                if (pending[0] == 0L) {
                    pending[0] = System.nanoTime();
                }
            }

            @Override
            public void afterAnyCall(Object target, Method method, Object[] args, Object result) {
                if (isNoise(method)) {
                    return;
                }
                long[] pending = PENDING.get();
                if (pending[0] == 0L) {
                    return;
                }
                long elapsedMs = (System.nanoTime() - pending[0]) / 1_000_000L;
                pending[0] = 0L;
                long[] stats = STATS.get();
                stats[0]++;
                stats[1] += elapsedMs;
            }
        }).decorate(driver);
    }

    /** Các method của Object/proxy không sinh lượt gọi mạng nào. */
    private static boolean isNoise(Method method) {
        String name = method.getName();
        return "toString".equals(name) || "hashCode".equals(name) || "equals".equals(name)
                || "getWrappedDriver".equals(name) || "getWrappedElement".equals(name);
    }

    public static void reset() {
        if (!ENABLED) {
            return;
        }
        long[] stats = STATS.get();
        stats[0] = 0L;
        stats[1] = 0L;
        PENDING.get()[0] = 0L;
    }

    public static long calls() {
        return ENABLED ? STATS.get()[0] : 0L;
    }

    public static long totalMs() {
        return ENABLED ? STATS.get()[1] : 0L;
    }

    /** Một dòng tóm tắt, rỗng khi tắt hoặc chưa có lượt gọi nào. */
    public static String summary(long tongMs) {
        if (!ENABLED) {
            return "";
        }
        long calls = calls();
        if (calls == 0L) {
            return "";
        }
        long ms = totalMs();
        String phanTram = tongMs > 0 ? String.format(" = %.0f%% của bước", ms * 100.0 / tongMs) : "";
        return String.format("%d lượt gọi chromedriver, %.1fs%s (TB %.1fms/lượt)",
                calls, ms / 1000.0, phanTram, (double) ms / calls);
    }
}

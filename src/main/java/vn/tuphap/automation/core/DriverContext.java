package vn.tuphap.automation.core;

import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.ui.WebUI;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giữ WebDriver / WebUI theo từng thread — bắt buộc khi chạy parallel nhiều Chrome.
 * <p>
 * Đóng / lỗi 1 trình duyệt chỉ ảnh hưởng thread đó ({@link #quitCurrent()}),
 * không gọi {@link #quitAll()} trừ khi suite kết thúc.
 */
public final class DriverContext {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<WebUI> WEBUI = new ThreadLocal<>();
    /** Thread đã bị user đóng Chrome — không mở lại trên thread này; thread khác vẫn chạy. */
    private static final ThreadLocal<Boolean> THREAD_ABORTED =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Set<WebDriver> ALL_DRIVERS = ConcurrentHashMap.newKeySet();
    /** Map driver → thread id (debug / tránh nhầm lẫn). */
    private static final Map<WebDriver, Long> DRIVER_THREADS = new ConcurrentHashMap<>();

    private DriverContext() {
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static WebUI getWebUI() {
        return WEBUI.get();
    }

    public static boolean isCurrentThreadAborted() {
        return Boolean.TRUE.equals(THREAD_ABORTED.get());
    }

    /**
     * Đánh dấu thread hiện tại dừng (Chrome đã đóng) — không quit các Chrome khác.
     */
    public static void abortCurrentThread(String reason) {
        THREAD_ABORTED.set(Boolean.TRUE);
        System.out.println("⛔ Thread " + Thread.currentThread().getId()
                + " dừng riêng (các trình duyệt khác vẫn chạy): "
                + (reason == null ? "" : reason));
        quitCurrent();
    }

    public static void clearAbortFlag() {
        THREAD_ABORTED.set(Boolean.FALSE);
    }

    public static void bind(WebDriver driver, WebUI webUI) {
        DRIVER.set(driver);
        WEBUI.set(webUI);
        if (driver != null) {
            ALL_DRIVERS.add(driver);
            DRIVER_THREADS.put(driver, Thread.currentThread().getId());
        }
    }

    /** Chỉ đóng Chrome của thread hiện tại — không đụng 2 Chrome còn lại. */
    public static void quitCurrent() {
        WebDriver driver = DRIVER.get();
        try {
            if (driver != null) {
                Long owner = DRIVER_THREADS.get(driver);
                System.out.println("--- Đóng Chrome [thread " + Thread.currentThread().getId()
                        + (owner != null ? ", owner=" + owner : "")
                        + "] — còn lại "
                        + Math.max(0, ALL_DRIVERS.size() - 1) + " trình duyệt ---");
                driver.quit();
            }
        } catch (Exception e) {
            System.out.println("⚠ Lỗi khi đóng trình duyệt thread "
                    + Thread.currentThread().getId() + ": " + e.getMessage());
        } finally {
            if (driver != null) {
                ALL_DRIVERS.remove(driver);
                DRIVER_THREADS.remove(driver);
            }
            DRIVER.remove();
            WEBUI.remove();
            BrowserLayout.releaseCurrentSlot();
        }
    }

    /**
     * Chỉ gọi ở AfterSuite. Không gọi khi 1 browser bị đóng giữa chừng.
     */
    public static void quitAll() {
        WebDriver[] snapshot = ALL_DRIVERS.toArray(WebDriver[]::new);
        System.out.println("--- Suite kết thúc: đóng " + snapshot.length + " trình duyệt còn lại ---");
        for (WebDriver driver : snapshot) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.out.println("⚠ Lỗi khi đóng trình duyệt (suite cleanup): " + e.getMessage());
            } finally {
                ALL_DRIVERS.remove(driver);
                DRIVER_THREADS.remove(driver);
            }
        }
        DRIVER.remove();
        WEBUI.remove();
        THREAD_ABORTED.remove();
        BrowserLayout.releaseCurrentSlot();
    }

    public static int openDriverCount() {
        return ALL_DRIVERS.size();
    }
}

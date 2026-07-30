package vn.tuphap.automation.core;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import vn.tuphap.automation.config.RunFlowConfig;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.BitSet;

/**
 * Chia màn hình để xem đồng thời nhiều Chrome khi chạy parallel.
 * Kích thước lấy từ {@link vn.tuphap.automation.config.RunFlowConfig}
 * ({@code run-flow.properties}: run.window.width / height / scale).
 */
public final class BrowserLayout {

    private static final Object LOCK = new Object();
    private static final BitSet USED_SLOTS = new BitSet(16);

    /** Chiều cao mặc định vừa mắt trên 1080p (không full-height). */
    private static final int DEFAULT_HEIGHT_1080 = 640;
    private static final int DEFAULT_WIDTH_CAP = 580;
    private static final int GAP = 10;
    private static final int OUTER_MARGIN = 12;

    private BrowserLayout() {
    }

    /** Gắn cửa sổ theo slot thread hiện tại (parallel) hoặc maximize (1 browser). */
    public static void apply(WebDriver driver) {
        if (driver == null) {
            return;
        }
        if (!ParallelConfig.isParallel()) {
            if (RunFlowConfig.hasSlots() && BrowserSlot.get() == null) {
                BrowserSlot.set(0);
            }
            maximizeOrFallback(driver);
            return;
        }
        int total = Math.max(1, ParallelConfig.threadCount());
        int slot = claimSlot(total);
        tile(driver, slot, total);
    }

    public static void releaseCurrentSlot() {
        Integer slot = BrowserSlot.get();
        if (slot == null) {
            return;
        }
        synchronized (LOCK) {
            USED_SLOTS.clear(slot);
        }
        BrowserSlot.clear();
    }

    /** Nhãn tiếng Việt: trình duyệt số 1/2/3 (slot tile), fallback thread id. */
    public static String browserLabel() {
        Integer slot = currentSlot();
        if (slot != null) {
            return "trình duyệt số " + (slot + 1);
        }
        return "thread " + Thread.currentThread().getId();
    }

    public static Integer currentSlot() {
        return BrowserSlot.get();
    }

    private static int claimSlot(int total) {
        Integer existing = BrowserSlot.get();
        if (existing != null) {
            return existing;
        }
        synchronized (LOCK) {
            for (int i = 0; i < total; i++) {
                if (!USED_SLOTS.get(i)) {
                    USED_SLOTS.set(i);
                    BrowserSlot.set(i);
                    return i;
                }
            }
            int overflow = total + USED_SLOTS.cardinality();
            BrowserSlot.set(overflow);
            return overflow % Math.max(total, 1);
        }
    }

    private static void tile(WebDriver driver, int slot, int total) {
        Rectangle work = workArea();
        int cols = Math.min(total, 3);
        int rows = (int) Math.ceil(total / (double) cols);
        int col = slot % cols;
        int row = slot / cols;

        int usableW = work.width - OUTER_MARGIN * 2 - GAP * (cols - 1);
        int usableH = work.height - OUTER_MARGIN * 2 - GAP * (rows - 1);

        int cellW = resolveWidth(usableW / cols);
        int cellH = resolveHeight(usableH / rows);

        // Căn giữa trong ô cột (không ép sát mép / không full chiều cao)
        int colSpan = usableW / cols;
        int rowSpan = usableH / rows;
        int x = work.x + OUTER_MARGIN + col * (colSpan + GAP) + Math.max(0, (colSpan - cellW) / 2);
        int y = work.y + OUTER_MARGIN + row * (rowSpan + GAP) + Math.max(0, (rowSpan - cellH) / 2);

        try {
            driver.manage().window().setSize(new Dimension(cellW, cellH));
            driver.manage().window().setPosition(new Point(x, y));
            String label = switch (slot) {
                case 0 -> "TRÁI";
                case 1 -> "GIỮA";
                case 2 -> "PHẢI";
                default -> "SLOT-" + slot;
            };
            System.out.println("🖥 Chrome #" + (slot + 1) + "/" + total
                    + " [" + label + "] @ (" + x + "," + y + ") "
                    + cellW + "x" + cellH + " (màn ~" + work.width + "x" + work.height + ")");
        } catch (Exception e) {
            System.out.println("⚠ Không tile được cửa sổ Chrome slot " + slot
                    + ": " + e.getMessage() + " — fallback kích thước nhỏ.");
            try {
                driver.manage().window().setSize(new Dimension(DEFAULT_WIDTH_CAP, DEFAULT_HEIGHT_1080));
                driver.manage().window().setPosition(new Point(
                        work.x + OUTER_MARGIN + slot * (DEFAULT_WIDTH_CAP + GAP),
                        work.y + OUTER_MARGIN));
            } catch (Exception ignored) {
                maximizeOrFallback(driver);
            }
        }
    }

    private static int resolveWidth(int columnBudget) {
        int fixed = RunFlowConfig.windowWidth();
        if (fixed <= 0) {
            fixed = intProp("taodon.window.width", 0);
        }
        if (fixed > 200) {
            return Math.min(fixed, columnBudget);
        }
        int capped = Math.min(DEFAULT_WIDTH_CAP, columnBudget);
        double scale = RunFlowConfig.windowScale();
        if (scale > 0 && scale < 1.0) {
            capped = (int) Math.round(capped * scale);
        }
        return Math.max(420, capped);
    }

    private static int resolveHeight(int rowBudget) {
        int fixed = RunFlowConfig.windowHeight();
        if (fixed <= 0) {
            fixed = intProp("taodon.window.height", 0);
        }
        if (fixed > 200) {
            return Math.min(fixed, rowBudget);
        }
        double scale = RunFlowConfig.windowScale();
        int fromScale = (int) Math.round(rowBudget * clamp(scale, 0.45, 0.85));
        int target = Math.min(DEFAULT_HEIGHT_1080, fromScale);
        return Math.max(480, Math.min(target, rowBudget));
    }

    private static int intProp(String key, int def) {
        String raw = System.getProperty(key, "");
        if (raw.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static Rectangle workArea() {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            return new Rectangle(
                    bounds.x + insets.left,
                    bounds.y + insets.top,
                    Math.max(800, bounds.width - insets.left - insets.right),
                    Math.max(600, bounds.height - insets.top - insets.bottom));
        } catch (Exception e) {
            java.awt.Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            return new Rectangle(0, 0, screen.width, Math.max(600, screen.height - 48));
        }
    }

    private static void maximizeOrFallback(WebDriver driver) {
        try {
            driver.manage().window().maximize();
        } catch (Exception maximizeError) {
            System.out.println("⚠ maximize() thất bại (" + maximizeError.getMessage()
                    + ") — dùng setSize fallback.");
            try {
                driver.manage().window().setSize(new Dimension(1920, 1080));
            } catch (Exception sizeError) {
                System.out.println("⚠ setSize cũng thất bại: " + sizeError.getMessage());
            }
        }
    }
}

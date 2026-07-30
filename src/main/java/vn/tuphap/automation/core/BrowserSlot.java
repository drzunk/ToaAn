package vn.tuphap.automation.core;

/**
 * Slot trình duyệt theo thread (0-based) — dùng chung cho tile cửa sổ và {@code run.slots}.
 */
public final class BrowserSlot {

    private static final ThreadLocal<Integer> SLOT = new ThreadLocal<>();

    private BrowserSlot() {
    }

    public static void set(int slot) {
        SLOT.set(slot);
    }

    public static Integer get() {
        return SLOT.get();
    }

    public static void clear() {
        SLOT.remove();
    }
}

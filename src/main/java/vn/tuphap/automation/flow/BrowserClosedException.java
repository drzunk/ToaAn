package vn.tuphap.automation.flow;

/**
 * Trình duyệt/tab đã đóng — dừng ngay, không retry mở lại trang.
 */
public final class BrowserClosedException extends RuntimeException {

    public BrowserClosedException(String message) {
        super(message == null ? "Trình duyệt đã đóng — dừng kịch bản ngay." : message);
    }

    public BrowserClosedException(String message, Throwable cause) {
        super(message == null ? "Trình duyệt đã đóng — dừng kịch bản ngay." : message, cause);
    }
}

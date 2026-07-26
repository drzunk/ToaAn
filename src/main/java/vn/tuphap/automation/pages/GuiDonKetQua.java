package vn.tuphap.automation.pages;

/**
 * Kết quả sau khi bấm Gửi đơn: toast thành công / lỗi hệ thống / hết thời gian chờ.
 */
public final class GuiDonKetQua {

    public enum TrangThai {
        SUCCESS,
        ERROR,
        TIMEOUT
    }

    private final TrangThai trangThai;
    private final String message;
    private final String screenshotBase64;

    public GuiDonKetQua(TrangThai trangThai, String message, String screenshotBase64) {
        this.trangThai = trangThai;
        this.message = message == null ? "" : message.trim();
        this.screenshotBase64 = screenshotBase64;
    }

    public TrangThai trangThai() {
        return trangThai;
    }

    /** Message hệ thống từ toast, hoặc mô tả timeout. */
    public String message() {
        return message;
    }

    public String screenshotBase64() {
        return screenshotBase64;
    }

    public boolean isSuccess() {
        return trangThai == TrangThai.SUCCESS;
    }

    public boolean isError() {
        return trangThai == TrangThai.ERROR;
    }

    public boolean isTimeout() {
        return trangThai == TrangThai.TIMEOUT;
    }
}

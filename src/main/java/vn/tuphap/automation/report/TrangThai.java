package vn.tuphap.automation.report;

/**
 * Các chuỗi trạng thái hiện trên báo cáo — toàn bộ tiếng Việt, một chỗ duy nhất.
 * <p>
 * {@link BaoCaoHtml} nhận diện trạng thái bằng cách so đầu chuỗi để chọn màu, nên đây không chỉ là
 * hằng cho gọn: gõ tay "Thất bại " thừa dấu cách ở một chỗ nào đó là mất màu ở đúng chỗ đó.
 */
public final class TrangThai {

    public static final String DAT = "Đạt";
    public static final String THAT_BAI = "Thất bại";
    public static final String BO_QUA = "Bỏ qua";
    public static final String DAT_CANH_BAO = "Đạt — cảnh báo";

    /** Bước đã bắt đầu nhưng bị chặn/lỗi giữa chừng — khác hẳn "Thất bại" của cả kịch bản. */
    public static final String KHONG_HOAN_THANH = "Không hoàn thành";

    /** Luồng chưa chạy tới bước này — trước đây bị đóng dấu nhầm là "Đạt". */
    public static final String CHUA_CHAY_TOI = "Chưa chạy tới";

    private TrangThai() {
    }
}

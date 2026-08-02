package vn.tuphap.automation.report;

import vn.tuphap.automation.config.RunFlowConfig;
import vn.tuphap.automation.data.TaoDonScenario;

import vn.tuphap.automation.data.DataDictionary;

/**
 * Nội dung báo cáo bằng tiếng Việt đầy đủ, tránh viết tắt khó hiểu với người mới.
 */
public final class TaoDonReportBuilder {

    private TaoDonReportBuilder() {
    }

    public static TaoDonScenario asScenario(Object[] parameters) {
        if (parameters == null || parameters.length == 0 || !(parameters[0] instanceof TaoDonScenario scenario)) {
            return null;
        }
        return scenario;
    }

    /** {@link RunFlowConfig.CaseProfile} đi kèm scenario khi case đến từ Google Sheet — {@code null} nếu không. */
    public static RunFlowConfig.CaseProfile asCaseProfile(Object[] parameters) {
        if (parameters == null || parameters.length < 2
                || !(parameters[1] instanceof RunFlowConfig.CaseProfile caseProfile)) {
            return null;
        }
        return caseProfile;
    }

    public static String buildTestTitle(TaoDonScenario s) {
        if (s == null) {
            return "Kiểm thử tạo đơn dịch vụ tư pháp toà án";
        }
        return String.format("Kịch bản số %s — %s / %s", s.stt(), s.loaiDon(), s.loaiViec());
    }

    public static String buildTestDescription(TaoDonScenario s) {
        if (s == null) {
            return "Luồng nộp đơn khởi kiện trên hệ thống dịch vụ tư pháp";
        }
        String soBiDon = s.soLuongBiDon() <= 1
                ? "1 bị đơn (hoặc bên bị kiện)"
                : s.soLuongBiDon() + " bị đơn (đã thêm bằng nút Thêm bị đơn)";
        return String.format(
                "Nguyên đơn: %s. Bên bị đơn/bị kiện: %s. Tòa án nhận đơn: %s. %s.",
                moTaChuThe(s.loaiChuThe()),
                moTaChuThe(s.loaiBiDon()),
                s.toaAn(),
                soBiDon);
    }

    public static String getLoaiDonCategory(TaoDonScenario s) {
        return s != null ? s.loaiDon() : "Khác";
    }

    public static String getLoaiViecCategory(TaoDonScenario s) {
        return s != null ? s.loaiViec() : "Khác";
    }

    /** Dòng giới thiệu ngắn ở đầu kịch bản — dữ liệu từng trường nằm ngay trong các bước. */
    public static void logScenarioOverview(TaoDonScenario s) {
        if (s == null) {
            return;
        }
        BaoCao.logInfo("Kịch bản: " + buildTestTitle(s));
        BaoCao.ketQuaMongDoi("Nộp trọn đơn " + s.loaiDon() + " / " + s.loaiViec()
                + " qua 6 bước và được hệ thống tiếp nhận.");
    }

    public static String tenBuocDayDu(int step) {
        return switch (step) {
            case 1 -> "Chọn loại đơn, loại việc và tòa án nhận đơn";
            case 2 -> "Điền thông tin nguyên đơn";
            case 3 -> "Điền thông tin bị đơn / bên bị kiện";
            case 4 -> "Điền nội dung đơn";
            case 5 -> "Tải tài liệu và chứng cứ";
            case 6 -> "Xem lại thông tin và gửi đơn";
            default -> "Bước " + step;
        };
    }

    /**
     * Định dạng thời lượng cho người đọc.
     * <p>
     * Làm tròn <b>trước</b> rồi mới tách phút/giây. Bản cũ tách trước rồi mới làm tròn phần giây,
     * nên 179.5 giây in ra "2 phút 60 giây" — hiện ngay trên số dẫn của báo cáo. Cùng lỗi đó khiến
     * 59.95 giây thành "60.0 giây" thay vì "1 phút".
     */
    public static String formatDuration(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        if (millis < 1000) {
            return millis + " mili giây";
        }
        // Dưới 1 phút giữ một chữ số thập phân, nhưng phải kiểm tra sau khi làm tròn: 59.96 giây
        // làm tròn thành 60.0 thì phải chuyển sang cách đọc theo phút.
        if (millis < 60_000) {
            double giay = Math.round(millis / 100.0) / 10.0;
            if (giay < 60.0) {
                return String.format(java.util.Locale.ROOT, "%.1f giây", giay);
            }
        }
        long tongGiay = Math.round(millis / 1000.0);
        long phut = tongGiay / 60;
        long giay = tongGiay % 60;
        return giay == 0 ? phut + " phút" : phut + " phút " + giay + " giây";
    }

    private static String moTaChuThe(String loai) {
        if (loai == null || loai.isEmpty()) {
            return "chưa xác định";
        }
        return DataDictionary.isToChuc(loai) ? "Tổ chức / doanh nghiệp" : "Cá nhân";
    }
}

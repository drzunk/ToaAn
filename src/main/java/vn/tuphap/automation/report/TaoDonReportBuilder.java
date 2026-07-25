package vn.tuphap.automation.report;

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

    public static String buildTestTitle(TaoDonScenario s) {
        if (s == null) {
            return "Kiểm thử tạo đơn điện tử";
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

    /** Bảng tóm tắt ngắn trên Extent — chi tiết đầy đủ nằm ở TestLogs Excel. */
    public static void logScenarioOverview(TaoDonScenario s) {
        if (s == null) {
            return;
        }
        TaoDonExcelTestLog.bindScenario(s);
        ExtentReportManager.logInfo(
                "Kịch bản: " + buildTestTitle(s) + " — chi tiết dữ liệu xem file TestLogs Excel.");
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

    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " mili giây";
        }
        if (millis < 60_000) {
            return String.format("%.1f giây", millis / 1000.0);
        }
        long minutes = millis / 60_000;
        double seconds = (millis % 60_000) / 1000.0;
        return String.format("%d phút %.0f giây", minutes, seconds);
    }

    private static String moTaChuThe(String loai) {
        if (loai == null || loai.isEmpty()) {
            return "chưa xác định";
        }
        return DataDictionary.isToChuc(loai) ? "Tổ chức / doanh nghiệp" : "Cá nhân";
    }
}

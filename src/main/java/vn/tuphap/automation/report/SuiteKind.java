package vn.tuphap.automation.report;

import vn.tuphap.automation.config.RunFlowConfig;
import vn.tuphap.automation.data.TaoDonScenario;

/**
 * Bộ kiểm thử đang chạy, và cách đặt mã cho kịch bản trong bộ đó.
 * <p>
 * Trước đây nằm lồng trong lớp xuất Excel, nên gỡ Excel là kéo sập cả {@code TestListener},
 * {@code ScreenshotStore} và tên file kết xuất — dù không thứ nào trong đó liên quan bảng tính.
 * Tách ra để "đang chạy bộ nào" là một khái niệm độc lập.
 */
public enum SuiteKind {
    SMOKE("smoke", "SMOKE", "Mẫu nhanh"),
    MID("mid", "MID", "Trung bình"),
    FULL("full", "FULL", "Đầy đủ"),
    LOGIN("login", "LOGIN", "Đăng nhập"),
    LE("le", "LE", "Chạy lẻ"),
    VALIDATE("validate", "VALIDATE", "Kiểm tra ràng buộc");

    private final String folder;
    private final String fileTag;
    private final String tenVi;

    SuiteKind(String folder, String fileTag, String tenVi) {
        this.folder = folder;
        this.fileTag = fileTag;
        this.tenVi = tenVi;
    }

    public String folder() {
        return folder;
    }

    /** Nhãn ngắn viết hoa — hiện trên báo cáo và dùng làm tiền tố mã case. */
    public String fileTag() {
        return fileTag;
    }

    public String tenVi() {
        return tenVi;
    }

    public static SuiteKind fromSuiteName(String suiteName) {
        if (suiteName != null) {
            String n = suiteName.toLowerCase();
            if (n.contains("smoke")) {
                return SMOKE;
            }
            if (n.contains("login") || n.contains("đăng nhập")) {
                return LOGIN;
            }
            if (n.contains("mid")) {
                return MID;
            }
            if (n.contains("le")) {
                return LE;
            }
            if (n.contains("full")) {
                return FULL;
            }
        }
        // Tên suite không nói gì thì tin vào cấu hình chạy.
        String prop = System.getProperty("taodon.suite", "");
        if ("smoke".equalsIgnoreCase(prop)) {
            return SMOKE;
        }
        if ("mid".equalsIgnoreCase(prop)) {
            return MID;
        }
        if ("login".equalsIgnoreCase(prop)) {
            return LOGIN;
        }
        if ("le".equalsIgnoreCase(prop)) {
            return LE;
        }
        return FULL;
    }

    // ── Bộ đang chạy ───────────────────────────────────────────────────────

    private static volatile SuiteKind hienTai = FULL;

    /** Ghi nhận bộ của lượt chạy này — gọi một lần ở đầu suite. */
    public static void datHienTai(String suiteName) {
        hienTai = fromSuiteName(suiteName);
        System.setProperty("taodon.suite", hienTai.folder());
    }

    public static SuiteKind hienTai() {
        return hienTai;
    }

    /**
     * Mã case ổn định giữa các lượt chạy, dựng từ số thứ tự kịch bản — {@code TC_MID_007}.
     * <p>
     * Phải ổn định thì báo cáo mới đối chiếu được lượt này với lượt trước ("mới hỏng" / "đã sửa")
     * và liên kết sâu {@code #c-<mốc>-<mã case>} mới dán cho đồng nghiệp được.
     *
     * @return {@code null} khi không có kịch bản (vd. test đăng nhập) — người gọi tự chọn cách khác
     */
    public static String maCase(TaoDonScenario s) {
        return maCase(s, null);
    }

    /**
     * Như {@link #maCase(TaoDonScenario)}, nhưng case âm (kỳ vọng hệ thống chặn — cột "Trường lỗi"
     * trong sheet khác rỗng) luôn mang tag {@code VALIDATE} riêng, bất kể bộ đang chạy là gì. Case
     * đó về bản chất khác hẳn case thường trong cùng lượt, dù chạy chung một suite.
     */
    public static String maCase(TaoDonScenario s, RunFlowConfig.CaseProfile caseProfile) {
        if (s == null) {
            return null;
        }
        String tag = caseProfile != null && caseProfile.hasNegativeExpectation()
                ? VALIDATE.fileTag() : hienTai.fileTag();
        String stt = String.valueOf(s.stt()).trim();
        try {
            return "TC_" + tag + "_" + String.format("%03d", Integer.parseInt(stt));
        } catch (NumberFormatException e) {
            return stt.isBlank() ? null : "TC_" + tag + "_" + stt;
        }
    }
}

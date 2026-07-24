package utils;

import com.aventstack.extentreports.markuputils.MarkupHelper;

/**
 * Tạo nội dung báo cáo chi tiết cho kịch bản Tạo đơn.
 */
public final class TaoDonReportBuilder {

    private TaoDonReportBuilder() {
    }

    public static String buildTestTitle(Object[] p) {
        if (p == null || p.length < 6) {
            return "Tạo đơn";
        }
        return String.format("[STT %s] %s › %s",
                val(p, 0), val(p, 1), val(p, 2));
    }

    public static String buildTestDescription(Object[] p) {
        if (p == null || p.length < 26) {
            return "Luồng tạo đơn điện tử end-to-end";
        }
        return String.format(
                "Nguyên đơn: %s · Bị đơn/Bên kia: %s · Tòa án: %s",
                shortChuThe(val(p, 5)),
                shortChuThe(val(p, 25)),
                val(p, 3));
    }

    public static String getLoaiDonCategory(Object[] p) {
        return p != null && p.length > 1 ? val(p, 1) : "Khác";
    }

    public static void logScenarioOverview(Object[] p) {
        if (p == null || p.length < 50) {
            ExtentReportManager.logWarning("Không đủ tham số để hiển thị bảng scenario (cần 50 cột).");
            return;
        }

        ExtentReportManager.logSection("📋 Tổng quan kịch bản");
        ExtentReportManager.logTable("Bước 1 — Loại đơn & Tòa án", new String[][]{
                {"STT", val(p, 0)},
                {"Loại đơn", val(p, 1)},
                {"Loại việc cụ thể", val(p, 2)},
                {"Tòa án nhận đơn", val(p, 3)},
                {"Tóm tắt sơ bộ (B1)", truncate(val(p, 4), 80)}
        });

        if (DataDictionary.isToChuc(val(p, 5))) {
            ExtentReportManager.logTable("Bước 2 — Nguyên đơn [Tổ chức]", new String[][]{
                    {"Loại chủ thể", val(p, 5)},
                    {"Tên tổ chức", val(p, 14)},
                    {"Loại hình", val(p, 15)},
                    {"MST", val(p, 16)},
                    {"Người đại diện PL", val(p, 18)},
                    {"SĐT / Email", val(p, 20) + " · " + val(p, 21)},
                    {"Người đại diện pháp lý", yesNo(val(p, 22))}
            });
        } else {
            ExtentReportManager.logTable("Bước 2 — Nguyên đơn [Cá nhân]", new String[][]{
                    {"Loại chủ thể", val(p, 5)},
                    {"Họ và tên", val(p, 6)},
                    {"Ngày sinh / Giới tính", val(p, 7) + " · " + val(p, 8)},
                    {"CCCD", val(p, 9)},
                    {"SĐT / Email", val(p, 20) + " · " + val(p, 21)},
                    {"Người đại diện pháp lý", yesNo(val(p, 22))}
            });
        }

        logBiDonSection(p);
        logNoiDungSection(p);
        logTaiLieuSection(p);
    }

    private static void logBiDonSection(Object[] p) {
        String loaiDon = val(p, 1);
        if (DataDictionary.isHanhChinh(loaiDon)) {
            ExtentReportManager.logTable("Bước 3 — Bên bị kiện [Hành chính]", new String[][]{
                    {"Tên cơ quan", val(p, 41)},
                    {"Chức danh", val(p, 42)},
                    {"Người có thẩm quyền", val(p, 43)},
                    {"Người liên quan", yesNo(val(p, 37))}
            });
            return;
        }

        if (DataDictionary.isToChuc(val(p, 25))) {
            ExtentReportManager.logTable("Bước 3 — Bị đơn [Tổ chức]", new String[][]{
                    {"Loại bị đơn", val(p, 25)},
                    {"Tên tổ chức", val(p, 30)},
                    {"Loại hình", val(p, 31)},
                    {"MST", val(p, 32)},
                    {"Người liên quan", yesNo(val(p, 37))}
            });
        } else {
            ExtentReportManager.logTable("Bước 3 — Bị đơn [Cá nhân]", new String[][]{
                    {"Loại bị đơn", val(p, 25)},
                    {"Họ và tên", val(p, 26)},
                    {"CCCD / Năm sinh", val(p, 27) + " · " + val(p, 28)},
                    {"Người liên quan", yesNo(val(p, 37))}
            });
        }
    }

    private static void logNoiDungSection(Object[] p) {
        String loaiDon = val(p, 1);
        if (DataDictionary.hasGiaTriTranhChap(loaiDon)) {
            ExtentReportManager.logTable("Bước 4 — Nội dung đơn", new String[][]{
                    {"Thời điểm phát sinh", val(p, 44)},
                    {"Giá trị tranh chấp", emptyAsDash(val(p, 45)) + " VNĐ"},
                    {"Tóm tắt quá trình", truncate(val(p, 46), 100)},
                    {"Yêu cầu cụ thể", truncate(val(p, 47), 100)},
                    {"Căn cứ pháp lý", emptyAsDash(val(p, 48))}
            });
        } else {
            ExtentReportManager.logTable("Bước 4 — Nội dung đơn", new String[][]{
                    {"Thời điểm phát sinh", val(p, 44)},
                    {"Tóm tắt quá trình", truncate(val(p, 46), 100)},
                    {"Yêu cầu cụ thể", truncate(val(p, 47), 100)},
                    {"Căn cứ pháp lý", emptyAsDash(val(p, 48))}
            });
        }
    }

    private static void logTaiLieuSection(Object[] p) {
        ExtentReportManager.logTable("Bước 5 — Tài liệu", new String[][]{
                {"Tài liệu bắt buộc", "Upload sample.pdf (theo loại đơn)"},
                {"Tài liệu bổ sung", yesNo(val(p, 49))}
        });
    }

    public static void logExecutionPlan() {
        ExtentReportManager.logCodeBlock(
                "Luồng thực thi (6 bước)",
                "1. Loại đơn → 2. Nguyên đơn → 3. Bị đơn/Bên kia → "
                        + "4. Nội dung → 5. Tài liệu → 6. Xem lại & Gửi đơn");
    }

    public static void logBranchStrategy(String loaiDon, String loaiChuThe, String loaiBiDon) {
        String buoc3;
        if (DataDictionary.isHanhChinh(loaiDon)) {
            buoc3 = "Form cơ quan hành chính (không chọn loại bị đơn)";
        } else if (DataDictionary.isPhaSan(loaiDon)) {
            buoc3 = "Tổ chức bị yêu cầu phá sản";
        } else if (DataDictionary.isHonNhanGiaDinh(loaiDon)) {
            buoc3 = "UI hôn nhân (Người bị yêu cầu / Vợ-chồng)";
        } else {
            buoc3 = DataDictionary.isToChuc(loaiBiDon) ? "Bị đơn tổ chức" : "Bị đơn cá nhân";
        }

        ExtentReportManager.logTable("🧭 Chiến lược rẽ nhánh", new String[][]{
                {"Loại đơn", loaiDon},
                {"Nguyên đơn", DataDictionary.isToChuc(loaiChuThe) ? "Tổ chức" : "Cá nhân"},
                {"Bước 3", buoc3},
                {"Giá trị tranh chấp", DataDictionary.hasGiaTriTranhChap(loaiDon) ? "Có" : "Không"}
        });
    }

    public static String getLoaiViecCategory(Object[] p) {
        return p != null && p.length > 2 ? val(p, 2) : "Khác";
    }

    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        }
        return String.format("%.1f giây", millis / 1000.0);
    }

    public static String formatSuiteSummary(int passed, int failed, int skipped, long totalMs) {
        return MarkupHelper.createTable(new String[][]{
                {"✅ Passed", String.valueOf(passed)},
                {"❌ Failed", String.valueOf(failed)},
                {"⏭ Skipped", String.valueOf(skipped)},
                {"⏱ Tổng thời gian", formatDuration(totalMs)}
        }).getMarkup();
    }

    private static String val(Object[] p, int index) {
        if (p[index] == null) {
            return "";
        }
        return p[index].toString().trim();
    }

    private static String shortChuThe(String loai) {
        if (loai == null || loai.isEmpty()) {
            return "—";
        }
        return DataDictionary.isToChuc(loai) ? "Tổ chức" : "Cá nhân";
    }

    private static String yesNo(String value) {
        if (value == null || value.isBlank()) {
            return "Không";
        }
        return value.trim().equalsIgnoreCase("có") ? "Có" : "Không";
    }

    private static String emptyAsDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String truncate(String text, int max) {
        if (text == null || text.isBlank()) {
            return "—";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 3) + "...";
    }
}

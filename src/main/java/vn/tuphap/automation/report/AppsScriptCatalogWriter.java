package vn.tuphap.automation.report;

import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.data.MasterDataCatalog;
import vn.tuphap.automation.data.ToaAnCatalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sinh sẵn file {@code 1-Catalog.gs} (Apps Script, sheet Google) từ dữ liệu vừa quét được bởi
 * {@code MasterDataSyncTest} — để dán thẳng vào Google Sheet thay cho việc tay chuyển đổi
 * property → mảng JavaScript.
 * <p>
 * Chỉ 3 phần thật sự "lấy từ UI" được sinh động: {@code Loại đơn}, {@code Loại việc theo Loại đơn},
 * {@code Tòa án}. Các hằng số còn lại ({@code Tư cách Phá sản}, {@code Trường lỗi}, header cột) là
 * cấu trúc cố định của bộ ca âm — không đổi theo UI — nên giữ nguyên literal khớp với
 * {@code 1-Catalog.gs} đang dùng.
 * <p>
 * Đọc thẳng từ {@code Map<String, List<String>>} đã quét trong bộ nhớ (không qua
 * {@code MasterDataCatalog.reload()}), vì lớp đó nạp catalog qua classloader resource — trong
 * cùng 1 lần chạy test, classpath vẫn trỏ tới bản {@code target/classes} cũ, không phản ánh file
 * vừa ghi ở {@code src/main/resources}.
 */
public final class AppsScriptCatalogWriter {

    private AppsScriptCatalogWriter() {
    }

    /**
     * @param outputFile nơi ghi file .gs (thường {@code test-output/1-Catalog.gs})
     * @param merged     dữ liệu đã quét — bắt buộc có khoá {@code loaiDon}, {@code toaAn},
     *                   và {@code loaiViec.<Loại đơn>} cho từng loại đơn có dropdown loại việc
     */
    public static Path writeCatalogGs(Path outputFile, Map<String, List<String>> merged) throws IOException {
        List<String> loaiDonList = filterJunk(requireNonEmpty(merged, "loaiDon"));
        List<String> toaAnList = ToaAnCatalog.filterForAutomation(
                filterJunk(requireNonEmpty(merged, "toaAn")));

        StringBuilder sb = new StringBuilder();
        appendHeader(sb);
        appendLoaiVienByLoaiDon(sb, merged, loaiDonList);
        sb.append("var LOAI_DON_LIST = Object.keys(LOAI_VIEC_BY_LOAI_DON);\n\n");
        appendList(sb, "TOA_AN_LIST", toaAnList);
        appendFixedConstants(sb);

        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(outputFile, sb.toString(), StandardCharsets.UTF_8);
        return outputFile;
    }

    private static void appendHeader(StringBuilder sb) {
        sb.append("/**\n")
                .append(" * FILE 1/5 — Danh mục dữ liệu + hằng số cột.\n")
                .append(" * TỰ SINH bởi MasterDataSyncTest từ UI thật (Loại đơn / Loại việc / Tòa án).\n")
                .append(" * Dán ĐÈ lên file \"1-Catalog\" trong Apps Script — giữ nguyên file 2-5.\n")
                .append(" */\n\n");
    }

    private static void appendLoaiVienByLoaiDon(StringBuilder sb, Map<String, List<String>> merged,
                                                 List<String> loaiDonList) {
        sb.append("var LOAI_VIEC_BY_LOAI_DON = {};\n\n");
        for (String loaiDon : loaiDonList) {
            List<String> viecs = filterJunk(merged.get("loaiViec." + loaiDon));
            if (viecs.isEmpty()) {
                viecs = DataDictionary.hasLoaiViecDropdown(loaiDon)
                        ? List.of()
                        : List.of(DataDictionary.PHA_SAN_LOAI_VIEC_MAC_DINH);
            }
            sb.append("LOAI_VIEC_BY_LOAI_DON[").append(jsString(loaiDon)).append("] = [\n");
            for (int i = 0; i < viecs.size(); i++) {
                sb.append("  ").append(jsString(viecs.get(i)));
                sb.append(i < viecs.size() - 1 ? ",\n" : "\n");
            }
            sb.append("];\n\n");
        }
    }

    private static void appendList(StringBuilder sb, String varName, List<String> values) {
        sb.append("var ").append(varName).append(" = [\n");
        for (int i = 0; i < values.size(); i++) {
            sb.append("  ").append(jsString(values.get(i)));
            sb.append(i < values.size() - 1 ? ",\n" : "\n");
        }
        sb.append("];\n\n");
    }

    /** Phần không phải "từ UI" — hằng số cấu trúc cố định, khớp với 1-Catalog.gs hiện tại. */
    private static void appendFixedConstants(StringBuilder sb) {
        appendList(sb, "TU_CACH_PHA_SAN_LIST", List.of(
                "Chủ nợ", "Người lao động", "DN / HTX tự nộp", "Cổ đông – thành viên HTX"));

        appendList(sb, "TRUONG_LOI_LIST", List.of(
                "", "Số điện thoại (Nguyên đơn)", "Email (Nguyên đơn)", "CCCD (Nguyên đơn)",
                "Họ tên (Nguyên đơn)", "Ngày sinh (Nguyên đơn)", "Ngày cấp CCCD (Nguyên đơn)",
                "Mã số thuế (Nguyên đơn)", "Số điện thoại (Bị đơn)", "Email (Bị đơn)",
                "CCCD (Bị đơn)", "Họ tên (Bị đơn)", "Mã số thuế (Bị đơn)", "Giá trị tranh chấp"));

        appendList(sb, "HEADER", List.of(
                "Chạy", "Loại đơn", "Loại việc", "Chủ thể", "Tư cách", "Tòa án", "Số bị đơn",
                "Đồng NĐ", "Đại diện", "Liên quan", "TL bổ sung", "Đến bước", "Gửi đơn",
                "Trường lỗi", "Giá trị lỗi", "Thông báo mong đợi", "Ghi chú"));

        sb.append("var COL = {};\n")
                .append("COL.CHAY = 1;\n")
                .append("COL.LOAI_DON = 2;\n")
                .append("COL.LOAI_VIEC = 3;\n")
                .append("COL.CHU_THE = 4;\n")
                .append("COL.TU_CACH = 5;\n")
                .append("COL.TOA_AN = 6;\n")
                .append("COL.SO_BI_DON = 7;\n")
                .append("COL.DONG_ND = 8;\n")
                .append("COL.DAI_DIEN = 9;\n")
                .append("COL.LIEN_QUAN = 10;\n")
                .append("COL.TL_BO_SUNG = 11;\n")
                .append("COL.DEN_BUOC = 12;\n")
                .append("COL.GUI_DON = 13;\n")
                .append("COL.TRUONG_LOI = 14;\n")
                .append("COL.GIA_TRI_LOI = 15;\n")
                .append("COL.THONG_BAO = 16;\n")
                .append("COL.GHI_CHU = 17;\n\n");

        sb.append("var MAX_ROW = 300;\n");
    }

    /** Lọc dữ liệu rác/nháp còn sót trên UI dev/UAT — dùng chung logic với {@code MasterDataCatalog}. */
    private static List<String> filterJunk(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(MasterDataCatalog::isProductionOption)
                .collect(Collectors.toList());
    }

    private static List<String> requireNonEmpty(Map<String, List<String>> merged, String key) {
        List<String> values = merged.get(key);
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("Thiếu khoá '" + key + "' — không sinh được 1-Catalog.gs.");
        }
        return values;
    }

    /** Bọc chuỗi thành literal JS an toàn (escape backslash / nháy đơn). */
    private static String jsString(String raw) {
        String escaped = raw.replace("\\", "\\\\").replace("'", "\\'");
        return "'" + escaped + "'";
    }
}

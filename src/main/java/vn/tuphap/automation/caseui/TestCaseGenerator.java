package vn.tuphap.automation.caseui;

import vn.tuphap.automation.config.CaseFileSource.CaseRow;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.MasterDataCatalog;
import vn.tuphap.automation.report.TaoDonReportBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Sinh đề xuất test case theo từng màn hình luồng nộp đơn — nguồn catalog
 * ({@link MasterDataCatalog}) + ma trận ca âm field ({@link DataGenerator#TRUONG_LOI_HOP_LE}).
 * <p>
 * Không gọi LLM / không mở Chrome: chỉ dựng {@link CaseRow} hợp lệ để người dùng chọn trên
 * Dashboard rồi đưa vào {@code local-cases.json}. Nếu đã chạy {@code -Pdiscovery}, đọc CSV mới
 * nhất trong {@code test-output/discovery-sweep/} để điền {@code thongBaoMongDoi} khi hệ thống
 * từng chặn được.
 */
public final class TestCaseGenerator {

    private static final Path DISCOVERY_DIR = Paths.get("test-output", "discovery-sweep");

    private TestCaseGenerator() {
    }

    /** Một đề xuất gắn với 1 màn — {@code caseRow} sẵn sàng merge vào file case. */
    public record DeXuat(
            String id,
            String loai,
            String lyDo,
            boolean chonMacDinh,
            CaseRow caseRow
    ) {
    }

    /** Nhóm đề xuất theo 1 màn hình (Login hoặc bước 1–6). */
    public record ManHinh(
            String id,
            int buoc,
            String ten,
            String moTa,
            List<DeXuat> cases
    ) {
    }

    public record KetQua(
            List<ManHinh> screens,
            int tongDeXuat,
            String discoveryCsv,
            String ghiChu
    ) {
    }

    public static KetQua generate() {
        Map<String, String> thongBaoTuDiscovery = docThongBaoTuDiscoveryMoiNhat();
        String csvPath = thongBaoTuDiscovery.isEmpty() ? "" : thongBaoTuDiscovery.getOrDefault("_file", "");
        thongBaoTuDiscovery.remove("_file");

        String loaiDonMacDinh = first(MasterDataCatalog.getLoaiDon(), "Dân sự");
        String loaiViecMacDinh = firstLoaiViec(loaiDonMacDinh, "Hợp đồng dân sự");
        String toaAnMacDinh = first(MasterDataCatalog.getToaAn(), "");
        String chuTheCn = chonChuThe(false);
        String chuTheTc = chonChuThe(true);

        List<ManHinh> screens = new ArrayList<>();
        screens.add(manLogin());
        screens.add(manDashboard());
        screens.add(manBuoc1(loaiDonMacDinh, toaAnMacDinh, chuTheCn));
        screens.add(manBuoc2(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn, chuTheTc,
                thongBaoTuDiscovery));
        screens.add(manBuoc3(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn,
                thongBaoTuDiscovery));
        screens.add(manBuoc4(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn,
                thongBaoTuDiscovery));
        screens.add(manBuoc5(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn));
        screens.add(manBuoc6(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn, chuTheTc));

        int tong = 0;
        for (ManHinh m : screens) {
            tong += m.cases().size();
        }
        String ghiChu = csvPath.isBlank()
                ? "Chưa có CSV discovery — thông báo mong đợi của ca âm để trống (chấp nhận mọi thông báo khi chạy)."
                : "Đã gắn thông báo mong đợi từ " + csvPath + " khi discovery từng ghi nhận hệ thống chặn.";
        return new KetQua(screens, tong, csvPath, ghiChu);
    }

    private static ManHinh manLogin() {
        List<DeXuat> cases = new ArrayList<>();
        cases.add(deXuat("login-smoke", "duong",
                "Chỉ đăng nhập (untilStep=0) — smoke session trước khi vào wizard",
                true,
                row(true, "Dân sự", "", chuTheCnFallback(), "", "", 0,
                        null, null, null, null,
                        "GEN_Login_Smoke", "", "", "", 0, false)));
        return new ManHinh("login", 0, "Đăng nhập",
                "Kiểm tra vào được hệ thống. Ca âm sai mật khẩu / captcha nằm ở LoginTest (suite -Plogin), không đưa vào local-cases.",
                cases);
    }

    private static ManHinh manDashboard() {
        List<DeXuat> cases = new ArrayList<>();
        cases.add(deXuat("dash-nop-moi", "duong",
                "Đến được form Nộp đơn mới (untilStep=1 tối thiểu sau đăng nhập) — kiểm tra nút Dashboard",
                false,
                row(true, "Dân sự", firstLoaiViec("Dân sự", ""), "", "",
                        first(MasterDataCatalog.getToaAn(), ""), 1,
                        null, null, null, null,
                        "GEN_Dashboard_NopDonMoi", "", "", "", 1, false)));
        return new ManHinh("dashboard", 0, "Trang chủ (Dashboard)",
                "Cầu nối sau đăng nhập: bấm Nộp đơn mới. Thường gộp với bước 1.",
                cases);
    }

    private static ManHinh manBuoc1(String loaiDonMacDinh, String toaAn, String chuTheCn) {
        List<DeXuat> cases = new ArrayList<>();
        String[] loaiDons = MasterDataCatalog.getLoaiDon();
        int i = 0;
        for (String ld : loaiDons) {
            String lv = firstLoaiViec(ld, "");
            boolean chon = i < 2; // mặc định tick 2 loại đơn đầu
            cases.add(deXuat("b1-" + slug(ld), "duong",
                    "Bước 1: chọn loại đơn «" + ld + "»" + (lv.isBlank() ? "" : " / " + lv),
                    chon,
                    row(true, ld, lv, chuTheCn, "", toaAn, 1,
                            null, null, null, null,
                            "GEN_B1_" + slug(ld), "", "", "", 1, false)));
            i++;
        }
        return new ManHinh("buoc1", 1,
                "Bước 1 — " + TaoDonReportBuilder.tenBuocDayDu(1),
                "Một case dương cho mỗi loại đơn trong catalog (dừng sau bước 1).",
                cases);
    }

    private static ManHinh manBuoc2(String loaiDon, String loaiViec, String toaAn,
                                    String chuTheCn, String chuTheTc,
                                    Map<String, String> thongBao) {
        List<DeXuat> cases = new ArrayList<>();
        cases.add(deXuat("b2-cn", "duong", "Nguyên đơn Cá nhân — điền đủ đến hết bước 2", true,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 1,
                        false, false, false, null,
                        "GEN_B2_CaNhan", "", "", "", 2, false)));
        cases.add(deXuat("b2-tc", "duong", "Nguyên đơn Tổ chức — cần MST / loại hình", true,
                row(true, loaiDon, loaiViec, chuTheTc, "", toaAn, 1,
                        false, true, false, null,
                        "GEN_B2_ToChuc", "", "", "", 2, false)));
        if (containsLoaiDon("Phá sản")) {
            for (String tuCach : MasterDataCatalog.getTuCachNopDonPhaSan()) {
                cases.add(deXuat("b2-ps-" + slug(tuCach), "duong",
                        "Phá sản — tư cách «" + tuCach + "»", false,
                        row(true, "Phá sản", "", chuTheCn, tuCach, toaAn, 1,
                                false, false, false, null,
                                "GEN_B2_PS_" + slug(tuCach), "", "", "", 2, false)));
            }
        }
        themCaAm(cases, 2, loaiDon, loaiViec, toaAn, chuTheCn, chuTheTc, thongBao,
                List.of("Số điện thoại", "Email", "CCCD", "Họ tên", "Ngày sinh", "Ngày cấp", "Mã số thuế"));
        return new ManHinh("buoc2", 2,
                "Bước 2 — " + TaoDonReportBuilder.tenBuocDayDu(2),
                "Ca dương CN/TC (+ tư cách Phá sản) và ca âm field nguyên đơn.",
                cases);
    }

    private static ManHinh manBuoc3(String loaiDon, String loaiViec, String toaAn, String chuTheCn,
                                    Map<String, String> thongBao) {
        List<DeXuat> cases = new ArrayList<>();
        cases.add(deXuat("b3-1bd", "duong", "1 bị đơn — dừng sau bước 3", true,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 1,
                        false, false, false, null,
                        "GEN_B3_1BiDon", "", "", "", 3, false)));
        cases.add(deXuat("b3-2bd", "duong", "2 bị đơn — nhánh thêm card", true,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 2,
                        false, false, false, null,
                        "GEN_B3_2BiDon", "", "", "", 3, false)));
        cases.add(deXuat("b3-nlq", "duong", "Có người liên quan", false,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 1,
                        false, false, true, null,
                        "GEN_B3_NguoiLienQuan", "", "", "", 3, false)));
        themCaAm(cases, 3, loaiDon, loaiViec, toaAn, chuTheCn, chonChuThe(true), thongBao,
                List.of("Số điện thoại (Bị đơn)", "Email (Bị đơn)", "CCCD (Bị đơn)",
                        "Họ tên (Bị đơn)", "Mã số thuế (Bị đơn)"));
        return new ManHinh("buoc3", 3,
                "Bước 3 — " + TaoDonReportBuilder.tenBuocDayDu(3),
                "Số bị đơn, NLQ, và ca âm field bị đơn.",
                cases);
    }

    private static ManHinh manBuoc4(String loaiDon, String loaiViec, String toaAn, String chuTheCn,
                                    Map<String, String> thongBao) {
        List<DeXuat> cases = new ArrayList<>();
        // Eform Dân sự / Bồi thường — khớp smoke DataGenerator.
        String eformViec = firstLoaiViec("Dân sự", "Bồi thường thiệt hại ngoài hợp đồng");
        cases.add(deXuat("b4-eform", "duong",
                "Nội dung eform (Dân sự / " + eformViec + ") — untilStep=4", true,
                row(true, "Dân sự", eformViec, chuTheCn, "", toaAn, 1,
                        false, false, false, null,
                        "GEN_B4_Eform", "", "", "", 4, false)));
        cases.add(deXuat("b4-textarea", "duong",
                "Loại việc khác — form textarea / upload tùy UI", false,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 1,
                        false, false, false, null,
                        "GEN_B4_NoiDung", "", "", "", 4, false)));
        themCaAm(cases, 4, loaiDon, loaiViec, toaAn, chuTheCn, chuTheCn, thongBao,
                List.of("Giá trị tranh chấp", "Tóm tắt quá trình", "Yêu cầu cụ thể", "Căn cứ pháp lý"));
        return new ManHinh("buoc4", 4,
                "Bước 4 — " + TaoDonReportBuilder.tenBuocDayDu(4),
                "3 mode UI (eform / upload / textarea) — đề xuất eform smoke + ca âm nội dung.",
                cases);
    }

    private static ManHinh manBuoc5(String loaiDon, String loaiViec, String toaAn, String chuTheCn) {
        List<DeXuat> cases = new ArrayList<>();
        cases.add(deXuat("b5-batbuoc", "duong", "Upload tài liệu bắt buộc — untilStep=5", true,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 1,
                        false, false, false, false,
                        "GEN_B5_TaiLieuBatBuoc", "", "", "", 5, false)));
        cases.add(deXuat("b5-bosung", "duong", "Có tài liệu bổ sung", false,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 1,
                        false, false, false, true,
                        "GEN_B5_TaiLieuBoSung", "", "", "", 5, false)));
        return new ManHinh("buoc5", 5,
                "Bước 5 — " + TaoDonReportBuilder.tenBuocDayDu(5),
                "Đính kèm PDF/PNG theo accept của từng dòng bắt buộc.",
                cases);
    }

    private static ManHinh manBuoc6(String loaiDon, String loaiViec, String toaAn,
                                    String chuTheCn, String chuTheTc) {
        List<DeXuat> cases = new ArrayList<>();
        cases.add(deXuat("b6-xemlai", "duong", "Đến Xem lại — không gửi đơn", true,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 1,
                        false, false, false, false,
                        "GEN_B6_XemLai", "", "", "", 6, false)));
        cases.add(deXuat("b6-gui", "duong", "Xem lại + Gửi đơn (tạo đơn thật trên UAT)", false,
                row(true, loaiDon, loaiViec, chuTheCn, "", toaAn, 1,
                        false, false, false, false,
                        "GEN_B6_GuiDon", "", "", "", 6, true)));
        cases.add(deXuat("b6-full-nhanh", "duong",
                "Nhánh đầy đủ: TC + 2 BD + ĐD + ĐND + NLQ + TLBS — xem lại không gửi", false,
                row(true, loaiDon, loaiViec, chuTheTc, "", toaAn, 2,
                        true, true, true, true,
                        "GEN_B6_FullNhanh", "", "", "", 6, false)));
        return new ManHinh("buoc6", 6,
                "Bước 6 — " + TaoDonReportBuilder.tenBuocDayDu(6),
                "Xem lại / gửi đơn. Chỉnh sửa từ Xem lại vẫn cover bởi TaoDonTest#testChinhSuaNoiDungTuXemLai.",
                cases);
    }

    private static void themCaAm(List<DeXuat> cases, int buoc, String loaiDon, String loaiViec,
                                  String toaAn, String chuTheCn, String chuTheTc,
                                  Map<String, String> thongBao, List<String> truongs) {
        for (String truong : truongs) {
            String tenHopLe = khopTruongLoi(truong);
            if (tenHopLe == null) {
                continue;
            }
            String lower = tenHopLe.toLowerCase(Locale.ROOT);
            boolean mst = lower.contains("mã số thuế") || lower.contains("ma so thue");
            boolean biDon = lower.contains("bị đơn") || lower.contains("bi don");
            String chuThe = mst && !biDon ? chuTheTc : chuTheCn;
            String ld = mst && biDon ? "Phá sản" : loaiDon;
            String lv = "Phá sản".equals(ld) ? "" : loaiViec;
            String giaTri = goiYGiaTriLoi(tenHopLe);
            String tb = thongBao.getOrDefault(khoaDiscovery(tenHopLe, giaTri),
                    thongBao.getOrDefault(tenHopLe, ""));
            cases.add(deXuat("am-b" + buoc + "-" + slug(tenHopLe), "am",
                    "Ca âm: ép «" + tenHopLe + "» = «" + (giaTri.isEmpty() ? "(trống)" : giaTri) + "»",
                    false,
                    row(false, ld, lv, chuThe, "", toaAn, 1,
                            false, false, false, null,
                            "GEN_AM_B" + buoc + "_" + slug(tenHopLe),
                            tenHopLe, giaTri, tb, buoc, false)));
        }
    }

    /** Khớp tên field với whitelist {@link DataGenerator#TRUONG_LOI_HOP_LE}; null nếu không có. */
    private static String khopTruongLoi(String truong) {
        for (String t : DataGenerator.TRUONG_LOI_HOP_LE) {
            if (t.equalsIgnoreCase(truong)) {
                return t;
            }
        }
        return null;
    }

    private static String goiYGiaTriLoi(String truong) {
        String n = truong.toLowerCase(Locale.ROOT);
        if (n.contains("email")) {
            return "khong-phai-email";
        }
        if (n.contains("điện thoại") || n.contains("dien thoai")) {
            return "abc";
        }
        if (n.contains("cccd")) {
            return "123";
        }
        if (n.contains("ngày") || n.contains("ngay")) {
            return "31/13/2024";
        }
        if (n.contains("giá trị") || n.contains("gia tri")) {
            return "-1000000";
        }
        if (n.contains("mã số") || n.contains("ma so")) {
            return "abc";
        }
        if (n.contains("họ tên") || n.contains("ho ten")) {
            return "";
        }
        return "";
    }

    private static DeXuat deXuat(String id, String loai, String lyDo, boolean chon, CaseRow row) {
        return new DeXuat(id, loai, lyDo, chon, row);
    }

    private static CaseRow row(boolean chay, String loaiDon, String loaiViec, String chuThe,
                               String tuCach, String toaAn, int soBd,
                               Boolean dongNd, Boolean dd, Boolean nlq, Boolean tlbs,
                               String ghiChu, String truongLoi, String giaTriLoi, String tb,
                               int until, boolean submit) {
        return new CaseRow(chay, nz(loaiDon), nz(loaiViec), nz(chuThe), nz(tuCach), nz(toaAn),
                soBd, dongNd, dd, nlq, tlbs, nz(ghiChu), nz(truongLoi), nz(giaTriLoi), nz(tb),
                until, submit);
    }

    private static boolean containsLoaiDon(String ten) {
        for (String ld : MasterDataCatalog.getLoaiDon()) {
            if (ld.equalsIgnoreCase(ten)) {
                return true;
            }
        }
        return false;
    }

    private static String chonChuThe(boolean toChuc) {
        for (String c : MasterDataCatalog.getLoaiChuTheNguyenDon()) {
            if (toChuc == DataDictionary.isToChuc(c)) {
                return c;
            }
        }
        return toChuc ? "Tổ chức / Doanh nghiệp" : "Cá nhân";
    }

    private static String chuTheCnFallback() {
        try {
            return chonChuThe(false);
        } catch (RuntimeException e) {
            return "Cá nhân";
        }
    }

    private static String first(String[] arr, String fallback) {
        return arr != null && arr.length > 0 ? arr[0] : fallback;
    }

    private static String firstLoaiViec(String loaiDon, String preferred) {
        try {
            String[] ds = MasterDataCatalog.getLoaiViecByLoaiDon(loaiDon);
            if (ds == null || ds.length == 0) {
                return "";
            }
            if (preferred != null && !preferred.isBlank()) {
                for (String v : ds) {
                    if (v.equalsIgnoreCase(preferred) || v.toLowerCase(Locale.ROOT)
                            .contains(preferred.toLowerCase(Locale.ROOT))) {
                        return v;
                    }
                }
            }
            return ds[0];
        } catch (RuntimeException e) {
            return preferred == null ? "" : preferred;
        }
    }

    private static String slug(String s) {
        String n = java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        return n.isEmpty() ? "x" : n;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    /**
     * Đọc CSV discovery mới nhất → map {@code trường|giá trị} hoặc {@code trường} → thông báo
     * khi cột "Bị chặn"=Có.
     */
    static Map<String, String> docThongBaoTuDiscoveryMoiNhat() {
        Map<String, String> out = new LinkedHashMap<>();
        Optional<Path> newest = timCsvDiscoveryMoiNhat();
        if (newest.isEmpty()) {
            return out;
        }
        Path file = newest.get();
        out.put("_file", file.toString().replace('\\', '/'));
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return out;
            }
            // Bỏ BOM
            String header = lines.get(0).replace("\uFEFF", "");
            for (int i = 1; i < lines.size(); i++) {
                List<String> cols = parseCsvLine(lines.get(i));
                if (cols.size() < 7) {
                    continue;
                }
                String truong = cols.get(0).trim();
                String giaTri = cols.get(2).trim();
                String biChan = cols.get(5).trim();
                String thongBao = cols.get(6).trim();
                if (!"Có".equalsIgnoreCase(biChan) || thongBao.isBlank()
                        || thongBao.toUpperCase(Locale.ROOT).startsWith("LỖI HẠ TẦNG")) {
                    continue;
                }
                out.putIfAbsent(khoaDiscovery(truong, giaTri), thongBao);
                out.putIfAbsent(truong, thongBao);
            }
        } catch (IOException ignored) {
            // Không có CSV / đọc lỗi — generator vẫn chạy, chỉ thiếu thongBaoMongDoi.
        }
        return out;
    }

    private static Optional<Path> timCsvDiscoveryMoiNhat() {
        if (!Files.isDirectory(DISCOVERY_DIR)) {
            return Optional.empty();
        }
        Path best = null;
        long bestMtime = Long.MIN_VALUE;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(DISCOVERY_DIR, "field-discovery_*.csv")) {
            for (Path p : ds) {
                long m = Files.getLastModifiedTime(p).toMillis();
                if (m > bestMtime) {
                    bestMtime = m;
                    best = p;
                }
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.ofNullable(best);
    }

    private static String khoaDiscovery(String truong, String giaTri) {
        return nz(truong) + "|" + nz(giaTri);
    }

    /** Parser CSV tối giản (RFC 4180 đủ dùng cho discovery export). */
    static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) {
            return out;
        }
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}

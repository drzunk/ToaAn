package vn.tuphap.automation.caseui;

import vn.tuphap.automation.config.CaseFileSource.CaseRow;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.MasterDataCatalog;
import vn.tuphap.automation.report.TaoDonReportBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    /**
     * Một đề xuất gắn với 1 màn. {@code caseRow} chỉ có khi {@code engine=master}; ca login âm dùng
     * {@code engine=login} và chạy qua suite {@code -Plogin}, không merge vào {@code local-cases}.
     */
    public record DeXuat(
            String id,
            String loai,
            String lyDo,
            boolean chonMacDinh,
            CaseRow caseRow,
            String engine
    ) {
        public DeXuat {
            if (engine == null || engine.isBlank()) {
                engine = "master";
            }
        }
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
            String ghiChu,
            FieldCoverageCatalog.BaoCao fieldCoverage
    ) {
    }

    public static KetQua generate() {
        return generate(FieldCoverageCatalog.loadLatest(DISCOVERY_DIR));
    }

    static KetQua generate(FieldCoverageCatalog fieldCatalog) {
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
                fieldCatalog));
        screens.add(manBuoc3(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn,
                fieldCatalog));
        screens.add(manBuoc4(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn,
                fieldCatalog));
        screens.add(manBuoc5(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn));
        screens.add(manBuoc6(loaiDonMacDinh, loaiViecMacDinh, toaAnMacDinh, chuTheCn, chuTheTc));

        int tong = 0;
        for (ManHinh m : screens) {
            tong += m.cases().size();
        }
        Set<String> generatedFields = new LinkedHashSet<>();
        for (ManHinh screen : screens) {
            for (DeXuat deXuat : screen.cases()) {
                if (deXuat.caseRow() != null && deXuat.caseRow().truongLoi() != null
                        && !deXuat.caseRow().truongLoi().isBlank()) {
                    generatedFields.add(deXuat.caseRow().truongLoi());
                }
            }
        }
        FieldCoverageCatalog.BaoCao coverage = fieldCatalog.baoCao(generatedFields);
        String ghiChu = fieldCatalog.discoveryCsv().isBlank()
                ? "Chưa có CSV discovery — dùng whitelist ∩ field override áp dụng được."
                : "Ưu tiên field/thông báo discovery từ " + fieldCatalog.discoveryCsv() + ".";
        if (!fieldCatalog.boQua().isEmpty()) {
            ghiChu += " Bỏ qua " + fieldCatalog.boQua().size()
                    + " field/ngữ cảnh không ép được.";
        }
        return new KetQua(screens, tong, fieldCatalog.discoveryCsv(), ghiChu, coverage);
    }

    private static ManHinh manLogin() {
        List<DeXuat> cases = new ArrayList<>();
        cases.add(deXuat("login-duong", "duong",
                "Đăng nhập thành công — thấy Dashboard (untilStep=0, chạy qua master / local-cases)",
                true,
                row(true, "Dân sự", "", chuTheCnFallback(), "", "", 0,
                        null, null, null, null,
                        "GEN_Login_Smoke", "", "", "", 0, false)));
        cases.add(deXuat("login-am-mk", "am",
                "Sai mật khẩu — không vào Dashboard, hệ thống phải báo lỗi rõ ràng",
                false, null, "login"));
        cases.add(deXuat("login-am-captcha", "am",
                "Sai captcha — không vào Dashboard, hệ thống phải báo lỗi rõ ràng",
                false, null, "login"));
        cases.add(deXuat("login-am-trong-mk", "am",
                "Bỏ trống mật khẩu — không vào Dashboard, hệ thống phải báo lỗi rõ ràng",
                false, null, "login"));
        return new ManHinh("login", 0, "Đăng nhập",
                "1 ca dương chạy qua master (untilStep=0). 3 ca âm chạy suite LoginTest riêng"
                        + " — bấm «Chạy suite login» (mvn -Plogin).",
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
                                    FieldCoverageCatalog fieldCatalog) {
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
        themCaAm(cases, fieldCatalog.fieldsForStep(2), toaAn);
        return new ManHinh("buoc2", 2,
                "Bước 2 — " + TaoDonReportBuilder.tenBuocDayDu(2),
                "Ca dương CN/TC (+ tư cách Phá sản); ca âm theo bản đồ field CN/TC.",
                cases);
    }

    private static ManHinh manBuoc3(String loaiDon, String loaiViec, String toaAn, String chuTheCn,
                                    FieldCoverageCatalog fieldCatalog) {
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
        if (containsLoaiDon("Phá sản")) {
            cases.add(deXuat("b3-ps-tochuc", "duong",
                    "Phá sản — bị đơn là Tổ chức/Doanh nghiệp theo UI", false,
                    row(true, "Phá sản", "", chuTheCn, "", toaAn, 1,
                            false, false, false, null,
                            "GEN_B3_PS_BiDonToChuc", "", "", "", 3, false)));
        }
        themCaAm(cases, fieldCatalog.fieldsForStep(3), toaAn);
        return new ManHinh("buoc3", 3,
                "Bước 3 — " + TaoDonReportBuilder.tenBuocDayDu(3),
                "1/2 bị đơn, NLQ; ca âm tách đúng bị đơn Cá nhân và Tổ chức/Phá sản.",
                cases);
    }

    private static ManHinh manBuoc4(String loaiDon, String loaiViec, String toaAn, String chuTheCn,
                                    FieldCoverageCatalog fieldCatalog) {
        List<DeXuat> cases = new ArrayList<>();
        // Hai hình form được tách rõ: Bồi thường là eform đã biết; Hợp đồng là textarea legacy.
        String eformViec = firstLoaiViec("Dân sự", "Bồi thường thiệt hại ngoài hợp đồng");
        String textareaViec = firstLoaiViecKhac("Dân sự", eformViec, "Hợp đồng dân sự");
        cases.add(deXuat("b4-eform", "duong",
                "Nội dung eform (Dân sự / " + eformViec + ") — untilStep=4", true,
                row(true, "Dân sự", eformViec, chuTheCn, "", toaAn, 1,
                        false, false, false, null,
                        "GEN_B4_Eform", "", "", "", 4, false)));
        cases.add(deXuat("b4-textarea", "duong",
                "Form textarea legacy (Dân sự / " + textareaViec + ")", true,
                row(true, "Dân sự", textareaViec, chuTheCn, "", toaAn, 1,
                        false, false, false, null,
                        "GEN_B4_Textarea", "", "", "", 4, false)));
        themCaAm(cases, fieldCatalog.fieldsForVariant(
                FieldCoverageCatalog.Variant.B4_TEXTAREA), toaAn);
        return new ManHinh("buoc4", 4,
                "Bước 4 — " + TaoDonReportBuilder.tenBuocDayDu(4),
                "Eform và textarea là 2 hình riêng; ca âm field cố định chỉ gắn textarea.",
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

    private static void themCaAm(List<DeXuat> cases,
                                 List<FieldCoverageCatalog.FieldCandidate> candidates,
                                 String toaAn) {
        for (FieldCoverageCatalog.FieldCandidate candidate : candidates) {
            FieldCoverageCatalog.Context context = candidate.context();
            String marker = candidate.discoveryDaChan()
                    ? " · discovery đã thấy hệ thống chặn" : "";
            cases.add(deXuat("am-b" + candidate.buoc() + "-" + slug(candidate.field()), "am",
                    "Ca âm [" + context.variant().moTa() + "]: ép «" + candidate.field()
                            + "» = «" + (candidate.giaTriLoi().isEmpty()
                            ? "(trống)" : candidate.giaTriLoi()) + "»" + marker,
                    false,
                    row(false, context.loaiDon(), context.loaiViec(), context.chuThe(),
                            context.tuCachNopDon(), toaAn, 1,
                            false, false, false, null,
                            "GEN_AM_B" + candidate.buoc() + "_" + slug(candidate.field()),
                            candidate.field(), candidate.giaTriLoi(),
                            candidate.thongBaoMongDoi(), candidate.buoc(), false)));
        }
    }

    private static DeXuat deXuat(String id, String loai, String lyDo, boolean chon, CaseRow row) {
        return deXuat(id, loai, lyDo, chon, row, "master");
    }

    private static DeXuat deXuat(String id, String loai, String lyDo, boolean chon, CaseRow row,
                                   String engine) {
        return new DeXuat(id, loai, lyDo, chon, row, engine);
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

    private static String firstLoaiViecKhac(String loaiDon, String excluded, String preferred) {
        try {
            String[] ds = MasterDataCatalog.getLoaiViecByLoaiDon(loaiDon);
            if (preferred != null && !preferred.isBlank()) {
                for (String value : ds) {
                    if (!value.equalsIgnoreCase(excluded)
                            && value.toLowerCase(Locale.ROOT)
                            .contains(preferred.toLowerCase(Locale.ROOT))) {
                        return value;
                    }
                }
            }
            for (String value : ds) {
                if (!value.equalsIgnoreCase(excluded)) {
                    return value;
                }
            }
        } catch (RuntimeException ignored) {
            // Catalog thiếu thì giữ preferred để generator vẫn trả đề xuất đọc được.
        }
        return preferred == null ? "" : preferred;
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

    /** Giữ API parser cũ cho test/caller nội bộ; implementation thuộc catalog field. */
    static List<String> parseCsvLine(String line) {
        return FieldCoverageCatalog.parseCsvLine(line);
    }
}

package vn.tuphap.automation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import vn.tuphap.automation.config.RunFlowConfig.CaseProfile;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.MasterDataCatalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Nạp/ghi danh sách case từ một file JSON cục bộ, git-tracked — nguồn thay thế cho Google Sheet
 * ({@code run.caseSource=file} + {@code run.casesFile} trỏ tới file {@code .json}).
 * <p>
 * Khác {@link CaseSheetSource} ở chỗ VALIDATE NGHIÊM: "Loại đơn"/"Loại việc"/"Tòa án"/"Chủ thể" sai
 * chính tả bị chặn ngay lúc nạp (ném {@link ValidationException} kèm danh sách giá trị hợp lệ),
 * không âm thầm bỏ qua dòng hay fallback về giá trị mặc định như sheet.
 * <p>
 * "Trường lỗi" (ca âm) cũng validate theo whitelist {@link DataGenerator#TRUONG_LOI_HOP_LE} — gõ
 * sai tên trường trước đây bị {@code DataGenerator} âm thầm bỏ qua override (case vẫn coi là ca âm
 * nên FAIL "hệ thống có lỗ hổng" — dương tính giả); giờ chặn ngay lúc lưu, không đợi tới lúc chạy.
 */
public final class CaseFileSource {

    private CaseFileSource() {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Lỗi nội dung file case — số dòng (1-based) + thông điệp đủ rõ để sửa ngay, không cần đoán. */
    public static final class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * Một dòng case trong file — thêm cờ {@code chay} (giống cột "Chạy" của sheet) so với
     * {@link CaseProfile} thuần, để tắt một case mà không phải xoá phần khai báo.
     */
    public record CaseRow(
            boolean chay,
            String loaiDon,
            String loaiViec,
            String chuThe,
            String tuCachNopDon,
            String toaAn,
            int soLuongBiDon,
            Boolean coDongNguyenDon,
            Boolean coNguoiDaiDien,
            Boolean coNguoiLienQuan,
            Boolean coTaiLieuBoSung,
            String ghiChu,
            String truongLoi,
            String giaTriLoi,
            String thongBaoMongDoi,
            int untilStep,
            boolean submit
    ) {
        CaseProfile toCaseProfile() {
            return new CaseProfile(
                    loaiDon, loaiViec, RunFlowConfig.resolveChuThe(chuThe),
                    nz(tuCachNopDon), nz(toaAn), soLuongBiDon,
                    coDongNguyenDon, coNguoiDaiDien, coNguoiLienQuan, coTaiLieuBoSung,
                    nz(ghiChu), nz(truongLoi), nz(giaTriLoi), nz(thongBaoMongDoi),
                    untilStep, submit);
        }

        static CaseRow fromCaseProfile(CaseProfile p) {
            return new CaseRow(true, p.loaiDon(), p.loaiViec(), p.chuThe(), p.tuCachNopDon(), p.toaAn(),
                    p.soLuongBiDon(), p.coDongNguyenDon(), p.coNguoiDaiDien(), p.coNguoiLienQuan(),
                    p.coTaiLieuBoSung(), p.ghiChu(), p.truongLoi(), p.giaTriLoi(), p.thongBaoMongDoi(),
                    p.untilStep(), p.submit());
        }

        private static String nz(String v) {
            return v == null ? "" : v;
        }
    }

    /** Đọc mọi dòng thô (kể cả {@code chay=false}) — dùng để hiển thị/sửa trên trình web. */
    public static List<CaseRow> readAll(Path file) {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ValidationException("Không đọc được " + file + ": " + e.getMessage());
        }
        if (json.isBlank()) {
            return new ArrayList<>();
        }
        CaseRow[] rows;
        try {
            rows = GSON.fromJson(json, CaseRow[].class);
        } catch (JsonSyntaxException e) {
            throw new ValidationException(file + " không phải JSON hợp lệ: " + e.getMessage());
        }
        List<CaseRow> out = new ArrayList<>();
        if (rows != null) {
            for (CaseRow r : rows) {
                if (r != null) {
                    out.add(r);
                }
            }
        }
        return out;
    }

    /**
     * Nạp case sẽ CHẠY ({@code chay=true}) từ file, validate nghiêm từng dòng.
     *
     * @throws ValidationException dòng nào sai — dừng ngay ở dòng đó, không chạy nửa vời.
     */
    public static List<CaseProfile> load(Path file) {
        List<CaseRow> rows = readAll(file);
        List<CaseProfile> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CaseRow r = rows.get(i);
            if (!r.chay()) {
                continue;
            }
            validate(r, i + 1);
            out.add(r.toCaseProfile());
        }
        return out;
    }

    /** Ghi đè toàn bộ danh sách — validate hết trước, không lưu nếu có dòng sai (tất cả hoặc không gì cả). */
    public static void save(Path file, List<CaseRow> rows) {
        for (int i = 0; i < rows.size(); i++) {
            CaseRow r = rows.get(i);
            if (r.chay()) {
                validate(r, i + 1);
            }
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, GSON.toJson(rows), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ValidationException("Không ghi được " + file + ": " + e.getMessage());
        }
    }

    /** Chuyển case đã nạp từ Google Sheet sang định dạng file này — dùng cho nút "Nhập từ Sheet". */
    public static List<CaseRow> fromSheetCases(List<CaseProfile> sheetCases) {
        List<CaseRow> out = new ArrayList<>();
        for (CaseProfile p : sheetCases) {
            out.add(CaseRow.fromCaseProfile(p));
        }
        return out;
    }

    private static void validate(CaseRow r, int soDong) {
        String where = "Dòng " + soDong;
        if (r.loaiDon() == null || r.loaiDon().isBlank()) {
            throw new ValidationException(where + ": thiếu \"Loại đơn\".");
        }
        assertInCatalog(where, r.loaiDon(), "Loại đơn", MasterDataCatalog.getLoaiDon());
        if (r.loaiViec() != null && !r.loaiViec().isBlank()) {
            String[] allowed;
            try {
                allowed = MasterDataCatalog.getLoaiViecByLoaiDon(r.loaiDon());
            } catch (IllegalStateException e) {
                allowed = new String[0];
            }
            if (allowed.length > 0) {
                assertInCatalog(where, r.loaiViec(), "Loại việc (của \"" + r.loaiDon() + "\")", allowed);
            }
        }
        if (r.toaAn() != null && !r.toaAn().isBlank()) {
            assertInCatalog(where, r.toaAn(), "Tòa án", MasterDataCatalog.getToaAn());
        }
        assertInCatalog(where, RunFlowConfig.resolveChuThe(r.chuThe()), "Chủ thể",
                MasterDataCatalog.getLoaiChuTheNguyenDon());
        if (r.soLuongBiDon() < 0 || r.soLuongBiDon() > 2) {
            throw new ValidationException(where + ": \"Số bị đơn\" = " + r.soLuongBiDon()
                    + " không hợp lệ — chỉ nhận 0 (tự chọn), 1 hoặc 2.");
        }
        if (r.untilStep() < 0 || r.untilStep() > 6) {
            throw new ValidationException(where + ": \"Đến bước\" = " + r.untilStep()
                    + " không hợp lệ — chỉ nhận 0-6.");
        }
        if (r.submit() && r.untilStep() < 6) {
            throw new ValidationException(where + ": tick \"Gửi đơn\" nhưng \"Đến bước\" = "
                    + r.untilStep() + " (chưa tới bước 6 — Xem lại). Gửi đơn chỉ có nghĩa khi Đến bước = 6"
                    + " — bỏ tick Gửi đơn hoặc đặt Đến bước = 6.");
        }
        if (r.truongLoi() != null && !r.truongLoi().isBlank()
                && !DataGenerator.isKnownNegativeField(r.truongLoi())) {
            throw new ValidationException(where + ": \"Trường lỗi\" = \"" + r.truongLoi()
                    + "\" không khớp tên trường nào hệ thống hỗ trợ ép sai (gõ sai tên trường sẽ khiến"
                    + " case bị bỏ qua override nhưng vẫn tính là ca âm — báo FAIL giả). Giá trị cho phép: "
                    + String.join(", ", DataGenerator.TRUONG_LOI_HOP_LE));
        }
    }

    private static void assertInCatalog(String where, String value, String field, String[] allowed) {
        try {
            MasterDataCatalog.assertInCatalog(value, field, allowed);
        } catch (IllegalStateException e) {
            throw new ValidationException(where + ": " + e.getMessage());
        }
    }
}

package vn.tuphap.automation.data;

/**
 * Facade giữ tên cũ; mọi giá trị rời rạc lấy từ {@link MasterDataCatalog}.
 */
public class DataDictionary {

    public static String[] getToaAn() { return MasterDataCatalog.getToaAn(); }
    public static String[] getLoaiDon() { return MasterDataCatalog.getLoaiDon(); }
    public static String[] getLoaiChuTheNguyenDon() { return MasterDataCatalog.getLoaiChuTheNguyenDon(); }
    public static String[] getLoaiChuTheBiDon() { return MasterDataCatalog.getLoaiChuTheBiDon(); }
    public static String[] getLoaiHinhToChuc() { return MasterDataCatalog.getLoaiHinhToChuc(); }
    public static String[] getGioiTinh() { return MasterDataCatalog.getGioiTinh(); }
    public static String[] getNoiCapCccd() { return MasterDataCatalog.getNoiCapCccd(); }
    public static String[] getCoKhong() { return MasterDataCatalog.getCoKhong(); }
    public static String[] getQuanHeDaiDien() { return MasterDataCatalog.getQuanHeDaiDien(); }

    public static String[] getTuCachNopDonPhaSan() { return MasterDataCatalog.getTuCachNopDonPhaSan(); }

    public static String[] getLoaiViecByLoaiDon(String loaiDon) {
        return MasterDataCatalog.getLoaiViecByLoaiDon(loaiDon);
    }

    public static String pick(String[] options, int index) {
        return MasterDataCatalog.pick(options, index);
    }

    public static boolean isToChuc(String loaiChuThe) {
        if (loaiChuThe == null) {
            return false;
        }
        String normalized = loaiChuThe.trim().toLowerCase();
        return normalized.contains("tổ chức") || normalized.contains("doanh nghiệp");
    }

    // --- CÁC HÀM XÁC ĐỊNH LUỒNG (THÊM MỚI) ---
    public static boolean isHanhChinh(String loaiDon) {
        return loaiDon != null && loaiDon.trim().toLowerCase().contains("hành chính");
    }

    public static boolean isPhaSan(String loaiDon) {
        return loaiDon != null && loaiDon.trim().toLowerCase().contains("phá sản");
    }

    /**
     * Phá sản trên UI không có dropdown loại việc — chỉ chọn thẻ loại đơn rồi chọn tòa án.
     * Giá trị này dùng trong catalog/báo cáo để vẫn có cặp (loại đơn, loại việc).
     */
    public static final String PHA_SAN_LOAI_VIEC_MAC_DINH = "Yêu cầu mở thủ tục phá sản";

    /** Loại đơn có dropdown "Loại việc cụ thể" ở bước 1. */
    public static boolean hasLoaiViecDropdown(String loaiDon) {
        return !isPhaSan(loaiDon);
    }

    public static boolean isHonNhanGiaDinh(String loaiDon) {
        if (loaiDon == null) {
            return false;
        }
        String normalized = loaiDon.trim().toLowerCase();
        return normalized.contains("hôn nhân") && normalized.contains("gia đình");
    }

    /** Thuận tình ly hôn — UI "Người yêu cầu 2 (vợ/chồng)", không nút Thêm. */
    public static boolean isThuanTinhLyHon(String loaiViec) {
        return loaiViec != null && loaiViec.trim().toLowerCase().contains("thuận tình");
    }

    /** UI cho phép thêm bị đơn / người bị yêu cầu / cơ quan thứ 2. */
    public static boolean allowsThemBiDon(String loaiDon, String loaiViec) {
        return !isPhaSan(loaiDon) && !isThuanTinhLyHon(loaiViec);
    }

    /**
     * Bước 3 dùng form Bị đơn chuẩn (tab Cá nhân/Tổ chức + badge "Bị đơn N"):
     * Dân sự, Lao động, Kinh doanh thương mại, Sở hữu trí tuệ.
     */
    public static boolean isStandardBiDonUi(String loaiDon) {
        return !isHanhChinh(loaiDon) && !isPhaSan(loaiDon) && !isHonNhanGiaDinh(loaiDon);
    }

    /** Bước 4 — có ô Giá trị tranh chấp trên form. */
    public static boolean hasGiaTriTranhChap(String loaiDon) {
        if (loaiDon == null) {
            return false;
        }
        String normalized = loaiDon.trim().toLowerCase();
        return normalized.contains("dân sự")
                || normalized.contains("lao động")
                || (normalized.contains("kinh doanh") && normalized.contains("thương mại"))
                || normalized.contains("sở hữu trí tuệ");
    }

    /** Bước 2 — generator gán nhánh đồng nguyên đơn cho mọi loại đơn trong catalog (7 loại). */
    public static boolean allowsDongNguyenDon(String loaiDon) {
        if (loaiDon == null || loaiDon.isBlank()) {
            return false;
        }
        String trimmed = loaiDon.trim();
        for (String option : getLoaiDon()) {
            if (option.equals(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /** Bước 4 — Giá trị tranh chấp bắt buộc (có dấu *). */
    public static boolean isGiaTriTranhChapRequired(String loaiDon) {
        if (loaiDon == null) {
            return false;
        }
        String normalized = loaiDon.trim().toLowerCase();
        return normalized.contains("dân sự")
                || (normalized.contains("kinh doanh") && normalized.contains("thương mại"));
    }
}
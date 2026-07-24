package utils;

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

    public static boolean isHonNhanGiaDinh(String loaiDon) {
        if (loaiDon == null) {
            return false;
        }
        String normalized = loaiDon.trim().toLowerCase();
        return normalized.contains("hôn nhân") && normalized.contains("gia đình");
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
package vn.tuphap.automation.data;

/**
 * Dữ liệu một bên bị đơn / người bị kiện (dùng cho bị đơn #1, #2, …).
 */
public final class BiDonData {
    private final String loai;
    private final String hoTen;
    private final String cccd;
    private final String namSinh;
    private final String diaChiCaNhan;
    private final String tenToChuc;
    private final String loaiHinh;
    private final String mst;
    private final String diaChiTruSo;
    private final String nguoiDaiDien;
    private final String sdt;
    private final String email;
    private final String tenCoQuanHC;
    private final String chucDanhHC;
    private final String nguoiThamQuyenHC;

    private BiDonData(Builder b) {
        this.loai = b.loai;
        this.hoTen = b.hoTen;
        this.cccd = b.cccd;
        this.namSinh = b.namSinh;
        this.diaChiCaNhan = b.diaChiCaNhan;
        this.tenToChuc = b.tenToChuc;
        this.loaiHinh = b.loaiHinh;
        this.mst = b.mst;
        this.diaChiTruSo = b.diaChiTruSo;
        this.nguoiDaiDien = b.nguoiDaiDien;
        this.sdt = b.sdt;
        this.email = b.email;
        this.tenCoQuanHC = b.tenCoQuanHC;
        this.chucDanhHC = b.chucDanhHC;
        this.nguoiThamQuyenHC = b.nguoiThamQuyenHC;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String loai() { return loai; }
    public String hoTen() { return hoTen; }
    public String cccd() { return cccd; }
    public String namSinh() { return namSinh; }
    public String diaChiCaNhan() { return diaChiCaNhan; }
    public String tenToChuc() { return tenToChuc; }
    public String loaiHinh() { return loaiHinh; }
    public String mst() { return mst; }
    public String diaChiTruSo() { return diaChiTruSo; }
    public String nguoiDaiDien() { return nguoiDaiDien; }
    public String sdt() { return sdt; }
    public String email() { return email; }
    public String tenCoQuanHC() { return tenCoQuanHC; }
    public String chucDanhHC() { return chucDanhHC; }
    public String nguoiThamQuyenHC() { return nguoiThamQuyenHC; }

    public static final class Builder {
        private String loai = "";
        private String hoTen = "";
        private String cccd = "";
        private String namSinh = "";
        private String diaChiCaNhan = "";
        private String tenToChuc = "";
        private String loaiHinh = "";
        private String mst = "";
        private String diaChiTruSo = "";
        private String nguoiDaiDien = "";
        private String sdt = "";
        private String email = "";
        private String tenCoQuanHC = "";
        private String chucDanhHC = "";
        private String nguoiThamQuyenHC = "";

        public Builder loai(String v) { this.loai = nz(v); return this; }
        public Builder hoTen(String v) { this.hoTen = nz(v); return this; }
        public Builder cccd(String v) { this.cccd = nz(v); return this; }
        public Builder namSinh(String v) { this.namSinh = nz(v); return this; }
        public Builder diaChiCaNhan(String v) { this.diaChiCaNhan = nz(v); return this; }
        public Builder tenToChuc(String v) { this.tenToChuc = nz(v); return this; }
        public Builder loaiHinh(String v) { this.loaiHinh = nz(v); return this; }
        public Builder mst(String v) { this.mst = nz(v); return this; }
        public Builder diaChiTruSo(String v) { this.diaChiTruSo = nz(v); return this; }
        public Builder nguoiDaiDien(String v) { this.nguoiDaiDien = nz(v); return this; }
        public Builder sdt(String v) { this.sdt = nz(v); return this; }
        public Builder email(String v) { this.email = nz(v); return this; }
        public Builder tenCoQuanHC(String v) { this.tenCoQuanHC = nz(v); return this; }
        public Builder chucDanhHC(String v) { this.chucDanhHC = nz(v); return this; }
        public Builder nguoiThamQuyenHC(String v) { this.nguoiThamQuyenHC = nz(v); return this; }

        public BiDonData build() {
            return new BiDonData(this);
        }

        private static String nz(String v) {
            return v == null ? "" : v;
        }
    }
}

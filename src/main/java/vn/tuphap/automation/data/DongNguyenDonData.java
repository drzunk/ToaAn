package vn.tuphap.automation.data;

/**
 * Dữ liệu một đồng nguyên đơn / đồng người khởi kiện (form con sau nút Thêm).
 */
public final class DongNguyenDonData {
    private final String loai;
    private final String hoTen;
    private final String ngaySinh;
    private final String cccd;
    private final String diaChiCuTru;
    private final String noiOHienTai;
    private final String ngheNghiep;
    private final String sdt;
    private final String email;
    private final String gioiTinh;
    private final String tenToChuc;
    private final String loaiHinh;
    private final String mst;
    private final String diaChiTruSo;
    private final String nguoiDaiDien;
    private final String chucVu;

    private DongNguyenDonData(Builder b) {
        this.loai = b.loai;
        this.hoTen = b.hoTen;
        this.ngaySinh = b.ngaySinh;
        this.cccd = b.cccd;
        this.diaChiCuTru = b.diaChiCuTru;
        this.noiOHienTai = b.noiOHienTai;
        this.ngheNghiep = b.ngheNghiep;
        this.sdt = b.sdt;
        this.email = b.email;
        this.gioiTinh = b.gioiTinh;
        this.tenToChuc = b.tenToChuc;
        this.loaiHinh = b.loaiHinh;
        this.mst = b.mst;
        this.diaChiTruSo = b.diaChiTruSo;
        this.nguoiDaiDien = b.nguoiDaiDien;
        this.chucVu = b.chucVu;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String loai() { return loai; }
    public String hoTen() { return hoTen; }
    public String ngaySinh() { return ngaySinh; }
    public String cccd() { return cccd; }
    public String diaChiCuTru() { return diaChiCuTru; }
    public String noiOHienTai() { return noiOHienTai; }
    public String ngheNghiep() { return ngheNghiep; }
    public String sdt() { return sdt; }
    public String email() { return email; }
    public String gioiTinh() { return gioiTinh; }
    public String tenToChuc() { return tenToChuc; }
    public String loaiHinh() { return loaiHinh; }
    public String mst() { return mst; }
    public String diaChiTruSo() { return diaChiTruSo; }
    public String nguoiDaiDien() { return nguoiDaiDien; }
    public String chucVu() { return chucVu; }

    public static final class Builder {
        private String loai = "Cá nhân";
        private String hoTen = "";
        private String ngaySinh = "";
        private String cccd = "";
        private String diaChiCuTru = "";
        private String noiOHienTai = "";
        private String ngheNghiep = "";
        private String sdt = "";
        private String email = "";
        private String gioiTinh = "Nam";
        private String tenToChuc = "";
        private String loaiHinh = "";
        private String mst = "";
        private String diaChiTruSo = "";
        private String nguoiDaiDien = "";
        private String chucVu = "";

        public Builder loai(String v) { this.loai = nz(v); return this; }
        public Builder hoTen(String v) { this.hoTen = nz(v); return this; }
        public Builder ngaySinh(String v) { this.ngaySinh = nz(v); return this; }
        public Builder cccd(String v) { this.cccd = nz(v); return this; }
        public Builder diaChiCuTru(String v) { this.diaChiCuTru = nz(v); return this; }
        public Builder noiOHienTai(String v) { this.noiOHienTai = nz(v); return this; }
        public Builder ngheNghiep(String v) { this.ngheNghiep = nz(v); return this; }
        public Builder sdt(String v) { this.sdt = nz(v); return this; }
        public Builder email(String v) { this.email = nz(v); return this; }
        public Builder gioiTinh(String v) { this.gioiTinh = nz(v); return this; }
        public Builder tenToChuc(String v) { this.tenToChuc = nz(v); return this; }
        public Builder loaiHinh(String v) { this.loaiHinh = nz(v); return this; }
        public Builder mst(String v) { this.mst = nz(v); return this; }
        public Builder diaChiTruSo(String v) { this.diaChiTruSo = nz(v); return this; }
        public Builder nguoiDaiDien(String v) { this.nguoiDaiDien = nz(v); return this; }
        public Builder chucVu(String v) { this.chucVu = nz(v); return this; }

        public DongNguyenDonData build() {
            return new DongNguyenDonData(this);
        }

        private static String nz(String v) {
            return v == null ? "" : v;
        }
    }
}

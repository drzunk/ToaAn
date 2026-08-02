package vn.tuphap.automation.data;

/**
 * Hợp đồng dữ liệu một kịch bản tạo đơn (thay cho Object[] 50 cột).
 */
public final class TaoDonScenario {

    private final String stt;
    private final String loaiDon;
    private final String loaiViec;
    private final String toaAn;
    private final String tomTat;

    private final String loaiChuThe;
    private final String hoTen;
    private final String ngaySinh;
    private final String gioiTinh;
    private final String cccd;
    private final String ngayCap;
    private final String noiCap;
    private final String thuongTru;
    private final String lienLac;
    private final String tenToChuc;
    private final String loaiHinhToChuc;
    private final String mst;
    private final String diaChiToChuc;
    private final String nguoiDaiDienToChuc;
    private final String chucVuToChuc;
    private final String sdt;
    private final String email;
    private final String coNguoiDaiDien;
    private final String tenNguoiDaiDien;
    private final String quanHeDaiDien;

    private final String loaiBiDon;
    private final String hoTenBD;
    private final String cccdBD;
    private final String namSinhBD;
    private final String gioiTinhBD;
    private final String diaChiCaNhanBD;
    private final String noiOHienTaiBD;
    private final String ngheNghiepBD;
    private final String tenToChucBD;
    private final String loaiHinhBD;
    private final String mstBD;
    private final String diaChiTruSoBD;
    private final String nguoiDaiDienBD;
    private final String chucVuBD;
    private final String sdtBD;
    private final String emailBD;

    private final String coNguoiLienQuan;
    private final String hoTenNLQ;
    private final String lyDoNLQ;
    private final String thongTinLienLacNLQ;

    private final String tenCoQuanHC;
    private final String chucDanhHC;
    private final String nguoiThamQuyenHC;

    private final String thoiDiemPhatSinh;
    private final String giaTriTranhChap;
    private final String tomTatQuaTrinh;
    private final String yeuCauCuThe;
    private final String canCuPhapLy;

    private final String coTaiLieuBoSung;

    /** Tư cách người nộp đơn (chỉ Phá sản). */
    private final String tuCachNopDon;

    /** Số bị đơn muốn điền (1 hoặc 2). UI không hỗ trợ thì framework tự giữ 1. */
    private final int soLuongBiDon;
    /** Bị đơn / bên bị kiện thứ 2 (null nếu chỉ 1). */
    private final BiDonData biDonThem;

    /** Có đồng nguyên đơn / đồng người khởi kiện (Có = bấm Thêm và điền form con). */
    private final String coDongNguyenDon;
    /** Dữ liệu form con đồng nguyên đơn (null nếu Không). */
    private final DongNguyenDonData dongNguyenDon;

    private TaoDonScenario(Builder b) {
        this.stt = b.stt;
        this.loaiDon = b.loaiDon;
        this.loaiViec = b.loaiViec;
        this.toaAn = b.toaAn;
        this.tomTat = b.tomTat;
        this.loaiChuThe = b.loaiChuThe;
        this.hoTen = b.hoTen;
        this.ngaySinh = b.ngaySinh;
        this.gioiTinh = b.gioiTinh;
        this.cccd = b.cccd;
        this.ngayCap = b.ngayCap;
        this.noiCap = b.noiCap;
        this.thuongTru = b.thuongTru;
        this.lienLac = b.lienLac;
        this.tenToChuc = b.tenToChuc;
        this.loaiHinhToChuc = b.loaiHinhToChuc;
        this.mst = b.mst;
        this.diaChiToChuc = b.diaChiToChuc;
        this.nguoiDaiDienToChuc = b.nguoiDaiDienToChuc;
        this.chucVuToChuc = b.chucVuToChuc;
        this.sdt = b.sdt;
        this.email = b.email;
        this.coNguoiDaiDien = b.coNguoiDaiDien;
        this.tenNguoiDaiDien = b.tenNguoiDaiDien;
        this.quanHeDaiDien = b.quanHeDaiDien;
        this.loaiBiDon = b.loaiBiDon;
        this.hoTenBD = b.hoTenBD;
        this.cccdBD = b.cccdBD;
        this.namSinhBD = b.namSinhBD;
        this.gioiTinhBD = b.gioiTinhBD;
        this.diaChiCaNhanBD = b.diaChiCaNhanBD;
        this.noiOHienTaiBD = b.noiOHienTaiBD;
        this.ngheNghiepBD = b.ngheNghiepBD;
        this.tenToChucBD = b.tenToChucBD;
        this.loaiHinhBD = b.loaiHinhBD;
        this.mstBD = b.mstBD;
        this.diaChiTruSoBD = b.diaChiTruSoBD;
        this.nguoiDaiDienBD = b.nguoiDaiDienBD;
        this.chucVuBD = b.chucVuBD;
        this.sdtBD = b.sdtBD;
        this.emailBD = b.emailBD;
        this.coNguoiLienQuan = b.coNguoiLienQuan;
        this.hoTenNLQ = b.hoTenNLQ;
        this.lyDoNLQ = b.lyDoNLQ;
        this.thongTinLienLacNLQ = b.thongTinLienLacNLQ;
        this.tenCoQuanHC = b.tenCoQuanHC;
        this.chucDanhHC = b.chucDanhHC;
        this.nguoiThamQuyenHC = b.nguoiThamQuyenHC;
        this.thoiDiemPhatSinh = b.thoiDiemPhatSinh;
        this.giaTriTranhChap = b.giaTriTranhChap;
        this.tomTatQuaTrinh = b.tomTatQuaTrinh;
        this.yeuCauCuThe = b.yeuCauCuThe;
        this.canCuPhapLy = b.canCuPhapLy;
        this.coTaiLieuBoSung = b.coTaiLieuBoSung;
        this.tuCachNopDon = b.tuCachNopDon;
        this.soLuongBiDon = Math.max(1, b.soLuongBiDon);
        this.biDonThem = b.biDonThem;
        this.coDongNguyenDon = b.coDongNguyenDon;
        this.dongNguyenDon = b.dongNguyenDon;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Bản sao có thể sửa từng field (dùng cho ca âm — tiêm 1 giá trị sai vào scenario hợp lệ). */
    public Builder toBuilder() {
        return new Builder()
                .stt(stt).loaiDon(loaiDon).loaiViec(loaiViec).toaAn(toaAn).tomTat(tomTat)
                .loaiChuThe(loaiChuThe).hoTen(hoTen).ngaySinh(ngaySinh).gioiTinh(gioiTinh)
                .cccd(cccd).ngayCap(ngayCap).noiCap(noiCap).thuongTru(thuongTru).lienLac(lienLac)
                .tenToChuc(tenToChuc).loaiHinhToChuc(loaiHinhToChuc).mst(mst)
                .diaChiToChuc(diaChiToChuc).nguoiDaiDienToChuc(nguoiDaiDienToChuc)
                .chucVuToChuc(chucVuToChuc).sdt(sdt).email(email).coNguoiDaiDien(coNguoiDaiDien)
                .tenNguoiDaiDien(tenNguoiDaiDien).quanHeDaiDien(quanHeDaiDien)
                .loaiBiDon(loaiBiDon).hoTenBD(hoTenBD).cccdBD(cccdBD).namSinhBD(namSinhBD)
                .gioiTinhBD(gioiTinhBD).diaChiCaNhanBD(diaChiCaNhanBD).noiOHienTaiBD(noiOHienTaiBD)
                .ngheNghiepBD(ngheNghiepBD).tenToChucBD(tenToChucBD).loaiHinhBD(loaiHinhBD)
                .mstBD(mstBD).diaChiTruSoBD(diaChiTruSoBD).nguoiDaiDienBD(nguoiDaiDienBD)
                .chucVuBD(chucVuBD).sdtBD(sdtBD).emailBD(emailBD)
                .coNguoiLienQuan(coNguoiLienQuan).hoTenNLQ(hoTenNLQ).lyDoNLQ(lyDoNLQ)
                .thongTinLienLacNLQ(thongTinLienLacNLQ)
                .tenCoQuanHC(tenCoQuanHC).chucDanhHC(chucDanhHC).nguoiThamQuyenHC(nguoiThamQuyenHC)
                .thoiDiemPhatSinh(thoiDiemPhatSinh).giaTriTranhChap(giaTriTranhChap)
                .tomTatQuaTrinh(tomTatQuaTrinh).yeuCauCuThe(yeuCauCuThe).canCuPhapLy(canCuPhapLy)
                .coTaiLieuBoSung(coTaiLieuBoSung).tuCachNopDon(tuCachNopDon)
                .soLuongBiDon(soLuongBiDon).biDonThem(biDonThem)
                .coDongNguyenDon(coDongNguyenDon).dongNguyenDon(dongNguyenDon);
    }

    public String stt() { return stt; }
    public String loaiDon() { return loaiDon; }
    public String loaiViec() { return loaiViec; }
    public String toaAn() { return toaAn; }
    public String tomTat() { return tomTat; }
    public String loaiChuThe() { return loaiChuThe; }
    public String hoTen() { return hoTen; }
    public String ngaySinh() { return ngaySinh; }
    public String gioiTinh() { return gioiTinh; }
    public String cccd() { return cccd; }
    public String ngayCap() { return ngayCap; }
    public String noiCap() { return noiCap; }
    public String thuongTru() { return thuongTru; }
    public String lienLac() { return lienLac; }
    public String tenToChuc() { return tenToChuc; }
    public String loaiHinhToChuc() { return loaiHinhToChuc; }
    public String mst() { return mst; }
    public String diaChiToChuc() { return diaChiToChuc; }
    public String nguoiDaiDienToChuc() { return nguoiDaiDienToChuc; }
    public String chucVuToChuc() { return chucVuToChuc; }
    public String sdt() { return sdt; }
    public String email() { return email; }
    public String coNguoiDaiDien() { return coNguoiDaiDien; }
    public String tenNguoiDaiDien() { return tenNguoiDaiDien; }
    public String quanHeDaiDien() { return quanHeDaiDien; }
    public String loaiBiDon() { return loaiBiDon; }
    public String hoTenBD() { return hoTenBD; }
    public String cccdBD() { return cccdBD; }
    public String namSinhBD() { return namSinhBD; }
    public String gioiTinhBD() { return gioiTinhBD; }
    public String diaChiCaNhanBD() { return diaChiCaNhanBD; }
    public String noiOHienTaiBD() { return noiOHienTaiBD; }
    public String ngheNghiepBD() { return ngheNghiepBD; }
    public String tenToChucBD() { return tenToChucBD; }
    public String loaiHinhBD() { return loaiHinhBD; }
    public String mstBD() { return mstBD; }
    public String diaChiTruSoBD() { return diaChiTruSoBD; }
    public String nguoiDaiDienBD() { return nguoiDaiDienBD; }
    public String chucVuBD() { return chucVuBD; }
    public String sdtBD() { return sdtBD; }
    public String emailBD() { return emailBD; }
    public String coNguoiLienQuan() { return coNguoiLienQuan; }
    public String hoTenNLQ() { return hoTenNLQ; }
    public String lyDoNLQ() { return lyDoNLQ; }
    public String thongTinLienLacNLQ() { return thongTinLienLacNLQ; }
    public String tenCoQuanHC() { return tenCoQuanHC; }
    public String chucDanhHC() { return chucDanhHC; }
    public String nguoiThamQuyenHC() { return nguoiThamQuyenHC; }
    public String thoiDiemPhatSinh() { return thoiDiemPhatSinh; }
    public String giaTriTranhChap() { return giaTriTranhChap; }
    public String tomTatQuaTrinh() { return tomTatQuaTrinh; }
    public String yeuCauCuThe() { return yeuCauCuThe; }
    public String canCuPhapLy() { return canCuPhapLy; }
    public String coTaiLieuBoSung() { return coTaiLieuBoSung; }
    public String tuCachNopDon() { return tuCachNopDon; }
    public int soLuongBiDon() { return soLuongBiDon; }
    public BiDonData biDonThem() { return biDonThem; }
    public String coDongNguyenDon() { return coDongNguyenDon; }
    public DongNguyenDonData dongNguyenDon() { return dongNguyenDon; }

    /** Bị đơn #1 gom từ các field BD hiện có. */
    public BiDonData biDonChinh() {
        return BiDonData.builder()
                .loai(loaiBiDon)
                .hoTen(hoTenBD)
                .cccd(cccdBD)
                .namSinh(namSinhBD)
                .gioiTinh(gioiTinhBD)
                .diaChiCaNhan(diaChiCaNhanBD)
                .noiOHienTai(noiOHienTaiBD)
                .ngheNghiep(ngheNghiepBD)
                .tenToChuc(tenToChucBD)
                .loaiHinh(loaiHinhBD)
                .mst(mstBD)
                .diaChiTruSo(diaChiTruSoBD)
                .nguoiDaiDien(nguoiDaiDienBD)
                .chucVu(chucVuBD)
                .sdt(sdtBD)
                .email(emailBD)
                .tenCoQuanHC(tenCoQuanHC)
                .chucDanhHC(chucDanhHC)
                .nguoiThamQuyenHC(nguoiThamQuyenHC)
                .build();
    }

    public static final class Builder {
        private String stt = "";
        private String loaiDon = "";
        private String loaiViec = "";
        private String toaAn = "";
        private String tomTat = "";
        private String loaiChuThe = "";
        private String hoTen = "";
        private String ngaySinh = "";
        private String gioiTinh = "";
        private String cccd = "";
        private String ngayCap = "";
        private String noiCap = "";
        private String thuongTru = "";
        private String lienLac = "";
        private String tenToChuc = "";
        private String loaiHinhToChuc = "";
        private String mst = "";
        private String diaChiToChuc = "";
        private String nguoiDaiDienToChuc = "";
        private String chucVuToChuc = "";
        private String sdt = "";
        private String email = "";
        private String coNguoiDaiDien = "";
        private String tenNguoiDaiDien = "";
        private String quanHeDaiDien = "";
        private String loaiBiDon = "";
        private String hoTenBD = "";
        private String cccdBD = "";
        private String namSinhBD = "";
        private String gioiTinhBD = "";
        private String diaChiCaNhanBD = "";
        private String noiOHienTaiBD = "";
        private String ngheNghiepBD = "";
        private String tenToChucBD = "";
        private String loaiHinhBD = "";
        private String mstBD = "";
        private String diaChiTruSoBD = "";
        private String nguoiDaiDienBD = "";
        private String chucVuBD = "";
        private String sdtBD = "";
        private String emailBD = "";
        private String coNguoiLienQuan = "";
        private String hoTenNLQ = "";
        private String lyDoNLQ = "";
        private String thongTinLienLacNLQ = "";
        private String tenCoQuanHC = "";
        private String chucDanhHC = "";
        private String nguoiThamQuyenHC = "";
        private String thoiDiemPhatSinh = "";
        private String giaTriTranhChap = "";
        private String tomTatQuaTrinh = "";
        private String yeuCauCuThe = "";
        private String canCuPhapLy = "";
        private String coTaiLieuBoSung = "";
        private String tuCachNopDon = "";
        private int soLuongBiDon = 1;
        private BiDonData biDonThem;
        private String coDongNguyenDon = "Không";
        private DongNguyenDonData dongNguyenDon;

        public Builder stt(String v) { this.stt = nullToEmpty(v); return this; }
        public Builder loaiDon(String v) { this.loaiDon = nullToEmpty(v); return this; }
        public Builder loaiViec(String v) { this.loaiViec = nullToEmpty(v); return this; }
        public Builder toaAn(String v) { this.toaAn = nullToEmpty(v); return this; }
        public Builder tomTat(String v) { this.tomTat = nullToEmpty(v); return this; }
        public Builder loaiChuThe(String v) { this.loaiChuThe = nullToEmpty(v); return this; }
        public Builder hoTen(String v) { this.hoTen = nullToEmpty(v); return this; }
        public Builder ngaySinh(String v) { this.ngaySinh = nullToEmpty(v); return this; }
        public Builder gioiTinh(String v) { this.gioiTinh = nullToEmpty(v); return this; }
        public Builder cccd(String v) { this.cccd = nullToEmpty(v); return this; }
        public Builder ngayCap(String v) { this.ngayCap = nullToEmpty(v); return this; }
        public Builder noiCap(String v) { this.noiCap = nullToEmpty(v); return this; }
        public Builder thuongTru(String v) { this.thuongTru = nullToEmpty(v); return this; }
        public Builder lienLac(String v) { this.lienLac = nullToEmpty(v); return this; }
        public Builder tenToChuc(String v) { this.tenToChuc = nullToEmpty(v); return this; }
        public Builder loaiHinhToChuc(String v) { this.loaiHinhToChuc = nullToEmpty(v); return this; }
        public Builder mst(String v) { this.mst = nullToEmpty(v); return this; }
        public Builder diaChiToChuc(String v) { this.diaChiToChuc = nullToEmpty(v); return this; }
        public Builder nguoiDaiDienToChuc(String v) { this.nguoiDaiDienToChuc = nullToEmpty(v); return this; }
        public Builder chucVuToChuc(String v) { this.chucVuToChuc = nullToEmpty(v); return this; }
        public Builder sdt(String v) { this.sdt = nullToEmpty(v); return this; }
        public Builder email(String v) { this.email = nullToEmpty(v); return this; }
        public Builder coNguoiDaiDien(String v) { this.coNguoiDaiDien = nullToEmpty(v); return this; }
        public Builder tenNguoiDaiDien(String v) { this.tenNguoiDaiDien = nullToEmpty(v); return this; }
        public Builder quanHeDaiDien(String v) { this.quanHeDaiDien = nullToEmpty(v); return this; }
        public Builder loaiBiDon(String v) { this.loaiBiDon = nullToEmpty(v); return this; }
        public Builder hoTenBD(String v) { this.hoTenBD = nullToEmpty(v); return this; }
        public Builder cccdBD(String v) { this.cccdBD = nullToEmpty(v); return this; }
        public Builder namSinhBD(String v) { this.namSinhBD = nullToEmpty(v); return this; }
        public Builder gioiTinhBD(String v) { this.gioiTinhBD = nullToEmpty(v); return this; }
        public Builder diaChiCaNhanBD(String v) { this.diaChiCaNhanBD = nullToEmpty(v); return this; }
        public Builder noiOHienTaiBD(String v) { this.noiOHienTaiBD = nullToEmpty(v); return this; }
        public Builder ngheNghiepBD(String v) { this.ngheNghiepBD = nullToEmpty(v); return this; }
        public Builder tenToChucBD(String v) { this.tenToChucBD = nullToEmpty(v); return this; }
        public Builder loaiHinhBD(String v) { this.loaiHinhBD = nullToEmpty(v); return this; }
        public Builder mstBD(String v) { this.mstBD = nullToEmpty(v); return this; }
        public Builder diaChiTruSoBD(String v) { this.diaChiTruSoBD = nullToEmpty(v); return this; }
        public Builder nguoiDaiDienBD(String v) { this.nguoiDaiDienBD = nullToEmpty(v); return this; }
        public Builder chucVuBD(String v) { this.chucVuBD = nullToEmpty(v); return this; }
        public Builder sdtBD(String v) { this.sdtBD = nullToEmpty(v); return this; }
        public Builder emailBD(String v) { this.emailBD = nullToEmpty(v); return this; }
        public Builder coNguoiLienQuan(String v) { this.coNguoiLienQuan = nullToEmpty(v); return this; }
        public Builder hoTenNLQ(String v) { this.hoTenNLQ = nullToEmpty(v); return this; }
        public Builder lyDoNLQ(String v) { this.lyDoNLQ = nullToEmpty(v); return this; }
        public Builder thongTinLienLacNLQ(String v) { this.thongTinLienLacNLQ = nullToEmpty(v); return this; }
        public Builder tenCoQuanHC(String v) { this.tenCoQuanHC = nullToEmpty(v); return this; }
        public Builder chucDanhHC(String v) { this.chucDanhHC = nullToEmpty(v); return this; }
        public Builder nguoiThamQuyenHC(String v) { this.nguoiThamQuyenHC = nullToEmpty(v); return this; }
        public Builder thoiDiemPhatSinh(String v) { this.thoiDiemPhatSinh = nullToEmpty(v); return this; }
        public Builder giaTriTranhChap(String v) { this.giaTriTranhChap = nullToEmpty(v); return this; }
        public Builder tomTatQuaTrinh(String v) { this.tomTatQuaTrinh = nullToEmpty(v); return this; }
        public Builder yeuCauCuThe(String v) { this.yeuCauCuThe = nullToEmpty(v); return this; }
        public Builder canCuPhapLy(String v) { this.canCuPhapLy = nullToEmpty(v); return this; }
        public Builder coTaiLieuBoSung(String v) { this.coTaiLieuBoSung = nullToEmpty(v); return this; }
        public Builder tuCachNopDon(String v) { this.tuCachNopDon = nullToEmpty(v); return this; }
        public Builder soLuongBiDon(int v) { this.soLuongBiDon = Math.max(1, v); return this; }
        public Builder biDonThem(BiDonData v) { this.biDonThem = v; return this; }
        public Builder coDongNguyenDon(String v) { this.coDongNguyenDon = nullToEmpty(v); return this; }
        public Builder dongNguyenDon(DongNguyenDonData v) { this.dongNguyenDon = v; return this; }

        public TaoDonScenario build() {
            return new TaoDonScenario(this);
        }

        private static String nullToEmpty(String v) {
            return v == null ? "" : v;
        }
    }
}

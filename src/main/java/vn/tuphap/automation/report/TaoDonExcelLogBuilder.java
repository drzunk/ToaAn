package vn.tuphap.automation.report;

import vn.tuphap.automation.data.BiDonData;

import vn.tuphap.automation.data.TaoDonScenario;

import vn.tuphap.automation.data.DataDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * Sinh chi tiết dữ liệu báo cáo Excel theo từng bước — đầy đủ cho tester.
 */
public final class TaoDonExcelLogBuilder {

    private TaoDonExcelLogBuilder() {
    }

    public record Muc(String buoc, String tenTruong, String giaTri, String ghiChu) {
    }

    public static List<Muc> build(TaoDonScenario s) {
        List<Muc> rows = new ArrayList<>();
        if (s == null) {
            return rows;
        }

        // —— Tổng quan (hiển thị dạng thẻ trên Excel, không liệt kê phẳng) ——
        String b0 = "Tổng quan kịch bản";
        add(rows, b0, "Số thứ tự kịch bản", s.stt(), "");
        add(rows, b0, "Loại đơn", s.loaiDon(), "");
        add(rows, b0, "Loại việc cụ thể",
                blank(s.loaiViec()) ? "(Không có trên biểu mẫu)" : s.loaiViec(),
                DataDictionary.isPhaSan(s.loaiDon()) ? "Phá sản: không dùng Dropdown loại việc" : "");
        add(rows, b0, "Tòa án nhận đơn", s.toaAn(), "");
        add(rows, b0, "Nguyên đơn — loại chủ thể", moTaChuThe(s.loaiChuThe()), s.loaiChuThe());
        add(rows, b0, "Bị đơn / bên bị kiện — loại", moTaChuThe(s.loaiBiDon()), s.loaiBiDon());
        add(rows, b0, "Số lượng bị đơn", String.valueOf(s.soLuongBiDon()),
                s.soLuongBiDon() > 1 ? "Có dùng nút Thêm bị đơn" : "Chỉ 1 bên");
        add(rows, b0, "Có người liên quan?", yesNo(s.coNguoiLienQuan()), "");
        add(rows, b0, "Có tài liệu bổ sung?", yesNo(s.coTaiLieuBoSung()), "");
        if (DataDictionary.isPhaSan(s.loaiDon())) {
            add(rows, b0, "Tư cách người nộp đơn (Phá sản)",
                    blank(s.tuCachNopDon()) ? "—" : s.tuCachNopDon(), "");
        }

        // —— Bước 1 ——
        String b1 = "Bước 1 — Chọn loại đơn, loại việc và tòa án nhận đơn";
        add(rows, b1, "Loại đơn (thẻ chọn)", s.loaiDon(), "");
        add(rows, b1, "Loại việc cụ thể",
                blank(s.loaiViec()) ? "(Bỏ qua — không có Dropdown)" : s.loaiViec(), "");
        add(rows, b1, "Tòa án nhận đơn", s.toaAn(), "Tìm kiếm và chọn trong Dropdown");
        add(rows, b1, "Tóm tắt sơ bộ yêu cầu", s.tomTat(), "Ô văn bản dài bước 1");

        // —— Bước 2 ——
        String b2 = "Bước 2 — Thông tin nguyên đơn (người / tổ chức nộp đơn)";
        add(rows, b2, "Loại chủ thể đã chọn", moTaChuThe(s.loaiChuThe()), s.loaiChuThe());
        if (DataDictionary.isToChuc(s.loaiChuThe())) {
            add(rows, b2, "Tên tổ chức / doanh nghiệp", s.tenToChuc(), "");
            add(rows, b2, "Loại hình tổ chức", s.loaiHinhToChuc(), "");
            add(rows, b2, "Mã số thuế / MSDN", s.mst(), "");
            add(rows, b2, "Địa chỉ trụ sở", s.diaChiToChuc(), "");
            add(rows, b2, "Người đại diện pháp luật", s.nguoiDaiDienToChuc(), "");
            add(rows, b2, "Chức vụ", s.chucVuToChuc(), "");
            add(rows, b2, "Số điện thoại tổ chức", s.sdt(), "");
            add(rows, b2, "Email tổ chức", s.email(), "");
            add(rows, b2, "Checkbox người đại diện pháp lý", "Không áp dụng", "Biểu mẫu tổ chức không có checkbox này");
        } else {
            add(rows, b2, "Họ và tên", s.hoTen(), "");
            add(rows, b2, "Ngày sinh", s.ngaySinh(), "Định dạng dd/MM/yyyy");
            add(rows, b2, "Giới tính", s.gioiTinh(), "");
            add(rows, b2, "Số CCCD / CMND", s.cccd(), "");
            add(rows, b2, "Ngày cấp CCCD", s.ngayCap(), "Định dạng dd/MM/yyyy");
            add(rows, b2, "Nơi cấp CCCD", s.noiCap(), "");
            add(rows, b2, "Địa chỉ thường trú", s.thuongTru(), "");
            boolean giongThuongTru = "Giống thường trú".equalsIgnoreCase(trim(s.lienLac()));
            add(rows, b2, "Địa chỉ liên lạc giống thường trú?", giongThuongTru ? "Có" : "Không", "");
            add(rows, b2, "Địa chỉ liên lạc",
                    blank(s.lienLac()) ? "—" : s.lienLac(),
                    giongThuongTru ? "Đã tích checkbox giống thường trú" : "Nhập địa chỉ riêng");
            add(rows, b2, "Số điện thoại", s.sdt(), "");
            add(rows, b2, "Email", s.email(), "");
            add(rows, b2, "Có người đại diện pháp lý?", yesNo(s.coNguoiDaiDien()), "");
            if (isCo(s.coNguoiDaiDien())) {
                add(rows, b2, "Họ tên người đại diện pháp lý", s.tenNguoiDaiDien(), "");
                add(rows, b2, "Quan hệ đại diện", s.quanHeDaiDien(), "");
            } else {
                add(rows, b2, "Họ tên người đại diện pháp lý", "(Không nhập)", "Vì chọn Không");
                add(rows, b2, "Quan hệ đại diện", "(Không nhập)", "Vì chọn Không");
            }
        }
        if (DataDictionary.isPhaSan(s.loaiDon())) {
            add(rows, b2, "Tư cách người nộp đơn",
                    blank(s.tuCachNopDon()) ? "(Không nhập / không có trên biểu mẫu)" : s.tuCachNopDon(),
                    "Chỉ loại đơn Phá sản");
        }

        // —— Bước 3 ——
        String b3 = "Bước 3 — Thông tin bị đơn / bên bị kiện";
        add(rows, b3, "Số lượng bị đơn cần điền", String.valueOf(s.soLuongBiDon()), "");
        appendBiDon(rows, b3, 1, s);
        if (s.soLuongBiDon() >= 2 && s.biDonThem() != null) {
            appendBiDonExtra(rows, b3, 2, s.loaiDon(), s.biDonThem());
        } else if (s.soLuongBiDon() >= 2) {
            add(rows, b3, "Bị đơn #2", "(Thiếu dữ liệu bị đơn thêm)", "Cần kiểm tra generator");
        }
        add(rows, b3, "Có người có quyền lợi liên quan?", yesNo(s.coNguoiLienQuan()), "");
        if (isCo(s.coNguoiLienQuan())) {
            add(rows, b3, "Họ tên người liên quan", s.hoTenNLQ(), "");
            add(rows, b3, "Lý do liên quan", s.lyDoNLQ(), "");
            add(rows, b3, "Thông tin liên lạc người liên quan", s.thongTinLienLacNLQ(), "");
        } else {
            add(rows, b3, "Họ tên người liên quan", "(Không nhập)", "Nút chuyển Không");
            add(rows, b3, "Lý do liên quan", "(Không nhập)", "Nút chuyển Không");
            add(rows, b3, "Thông tin liên lạc người liên quan", "(Không nhập)", "Nút chuyển Không");
        }

        // —— Bước 4 ——
        String b4 = "Bước 4 — Nội dung đơn";
        add(rows, b4, "Thời điểm phát sinh vụ việc", s.thoiDiemPhatSinh(), "Định dạng dd/MM/yyyy");
        if (DataDictionary.hasGiaTriTranhChap(s.loaiDon())) {
            add(rows, b4, "Giá trị tranh chấp (VNĐ)",
                    blank(s.giaTriTranhChap()) ? "(Để trống)" : s.giaTriTranhChap(),
                    DataDictionary.isGiaTriTranhChapRequired(s.loaiDon())
                            ? "Trường bắt buộc với loại đơn này" : "Có trên biểu mẫu");
        } else {
            add(rows, b4, "Giá trị tranh chấp (VNĐ)", "Không áp dụng", "Loại đơn không có trường này");
        }
        add(rows, b4, "Tóm tắt quá trình sự việc", s.tomTatQuaTrinh(), "");
        add(rows, b4, "Yêu cầu cụ thể", s.yeuCauCuThe(), "");
        add(rows, b4, "Căn cứ pháp lý",
                blank(s.canCuPhapLy()) ? "(Để trống)" : s.canCuPhapLy(),
                blank(s.canCuPhapLy()) ? "Không bắt buộc" : "");

        // —— Bước 5 ——
        String b5 = "Bước 5 — Tài liệu và chứng cứ";
        add(rows, b5, "Tệp dùng để tải lên", "tệp mẫu.pdf (tệp mẫu trong dự án)",
                "Áp dụng cho mọi mục tài liệu bắt buộc trên biểu mẫu");
        add(rows, b5, "Tài liệu bắt buộc", "Đã tải đủ các mục bắt buộc hiển thị trên biểu mẫu",
                "Số lượng mục phụ thuộc loại đơn / loại việc");
        add(rows, b5, "Có tải tài liệu bổ sung?", yesNo(s.coTaiLieuBoSung()),
                isCo(s.coTaiLieuBoSung())
                        ? "Có tải nếu ô tài liệu bổ sung hiện trên biểu mẫu"
                        : "Không yêu cầu / biểu mẫu ẩn mục bổ sung");

        // —— Bước 6 ——
        String b6 = "Bước 6 — Xem lại và gửi đơn";
        add(rows, b6, "Đối chiếu trên màn Xem lại", "Có mở màn Xem lại sau bước tài liệu", "");
        add(rows, b6, "Checkbox xác nhận thông tin", "Có tích (khi thực hiện gửi đơn)",
                "Kịch bản chỉnh sửa có thể không gửi đơn");
        add(rows, b6, "Nút Gửi đơn", "Theo kết quả thực tế ở sheet Tổng hợp",
                "Demo có thể không trả thông báo thành công");

        return rows;
    }

    private static void appendBiDon(List<Muc> rows, String buoc, int index, TaoDonScenario s) {
        String prefix = "Bị đơn / bên bị kiện #" + index + " — ";
        if (DataDictionary.isHanhChinh(s.loaiDon())) {
            add(rows, buoc, prefix + "Loại", "Cơ quan hành chính", "");
            add(rows, buoc, prefix + "Tên cơ quan", s.tenCoQuanHC(), "");
            add(rows, buoc, prefix + "Chức danh", s.chucDanhHC(), "");
            add(rows, buoc, prefix + "Người có thẩm quyền", s.nguoiThamQuyenHC(), "");
            add(rows, buoc, prefix + "Địa chỉ trụ sở", s.diaChiTruSoBD(), "");
            add(rows, buoc, prefix + "Số điện thoại", s.sdtBD(), "");
            add(rows, buoc, prefix + "Email", blank(s.emailBD()) ? "(Không nhập)" : s.emailBD(), "");
            return;
        }
        if (DataDictionary.isPhaSan(s.loaiDon())) {
            add(rows, buoc, prefix + "Loại", "Doanh nghiệp / HTX bị yêu cầu (Tổ chức)", "Phá sản — cố định tổ chức");
            add(rows, buoc, prefix + "Tên tổ chức", s.tenToChucBD(), "");
            add(rows, buoc, prefix + "Loại hình", s.loaiHinhBD(), "");
            add(rows, buoc, prefix + "Mã số thuế", s.mstBD(), "");
            add(rows, buoc, prefix + "Địa chỉ trụ sở", s.diaChiTruSoBD(), "");
            add(rows, buoc, prefix + "Người đại diện", s.nguoiDaiDienBD(), "");
            add(rows, buoc, prefix + "Số điện thoại", s.sdtBD(), "");
            add(rows, buoc, prefix + "Email", blank(s.emailBD()) ? "(Không nhập)" : s.emailBD(), "");
            return;
        }
        add(rows, buoc, prefix + "Loại chủ thể", moTaChuThe(s.loaiBiDon()), s.loaiBiDon());
        if (DataDictionary.isToChuc(s.loaiBiDon())) {
            add(rows, buoc, prefix + "Tên tổ chức", s.tenToChucBD(), "");
            add(rows, buoc, prefix + "Loại hình", s.loaiHinhBD(), "");
            add(rows, buoc, prefix + "Mã số thuế", s.mstBD(), "");
            add(rows, buoc, prefix + "Địa chỉ trụ sở", s.diaChiTruSoBD(), "");
            add(rows, buoc, prefix + "Người đại diện", s.nguoiDaiDienBD(), "");
            add(rows, buoc, prefix + "Số điện thoại", s.sdtBD(), "");
            add(rows, buoc, prefix + "Email", blank(s.emailBD()) ? "(Không nhập)" : s.emailBD(), "");
        } else {
            add(rows, buoc, prefix + "Họ và tên", s.hoTenBD(), "");
            add(rows, buoc, prefix + "Số CCCD / CMND", s.cccdBD(), "");
            add(rows, buoc, prefix + "Năm sinh", s.namSinhBD(), "");
            add(rows, buoc, prefix + "Địa chỉ", s.diaChiCaNhanBD(), "");
            add(rows, buoc, prefix + "Số điện thoại", s.sdtBD(), "");
            add(rows, buoc, prefix + "Email", s.emailBD(), "");
        }
    }

    private static void appendBiDonExtra(List<Muc> rows, String buoc, int index, String loaiDon, BiDonData d) {
        String prefix = "Bị đơn / bên bị kiện #" + index + " — ";
        if (DataDictionary.isHanhChinh(loaiDon)) {
            add(rows, buoc, prefix + "Loại", "Cơ quan hành chính", "");
            add(rows, buoc, prefix + "Tên cơ quan", d.tenCoQuanHC(), "");
            add(rows, buoc, prefix + "Chức danh", d.chucDanhHC(), "");
            add(rows, buoc, prefix + "Người có thẩm quyền", d.nguoiThamQuyenHC(), "");
            add(rows, buoc, prefix + "Địa chỉ trụ sở", d.diaChiTruSo(), "");
            add(rows, buoc, prefix + "Số điện thoại", d.sdt(), "");
            add(rows, buoc, prefix + "Email", blank(d.email()) ? "(Không nhập)" : d.email(), "");
            return;
        }
        add(rows, buoc, prefix + "Loại chủ thể", moTaChuThe(d.loai()), d.loai());
        if (DataDictionary.isToChuc(d.loai())) {
            add(rows, buoc, prefix + "Tên tổ chức", d.tenToChuc(), "");
            add(rows, buoc, prefix + "Loại hình", d.loaiHinh(), "");
            add(rows, buoc, prefix + "Mã số thuế", d.mst(), "");
            add(rows, buoc, prefix + "Địa chỉ trụ sở", d.diaChiTruSo(), "");
            add(rows, buoc, prefix + "Người đại diện", d.nguoiDaiDien(), "");
            add(rows, buoc, prefix + "Số điện thoại", d.sdt(), "");
            add(rows, buoc, prefix + "Email", blank(d.email()) ? "(Không nhập)" : d.email(), "");
        } else {
            add(rows, buoc, prefix + "Họ và tên", d.hoTen(), "");
            add(rows, buoc, prefix + "Số CCCD / CMND", d.cccd(), "");
            add(rows, buoc, prefix + "Năm sinh", d.namSinh(), "");
            add(rows, buoc, prefix + "Địa chỉ", d.diaChiCaNhan(), "");
            add(rows, buoc, prefix + "Số điện thoại", d.sdt(), "");
            add(rows, buoc, prefix + "Email", d.email(), "");
        }
    }

    private static void add(List<Muc> rows, String buoc, String ten, String gt, String gc) {
        rows.add(new Muc(buoc, ten, gt == null ? "" : gt, gc == null ? "" : gc));
    }

    private static String moTaChuThe(String loai) {
        if (blank(loai)) {
            return "Chưa xác định";
        }
        return DataDictionary.isToChuc(loai) ? "Tổ chức / doanh nghiệp" : "Cá nhân";
    }

    private static String yesNo(String v) {
        return isCo(v) ? "Có" : "Không";
    }

    private static boolean isCo(String v) {
        return v != null && v.trim().equalsIgnoreCase("có");
    }

    private static boolean blank(String v) {
        return v == null || v.isBlank();
    }

    private static String trim(String v) {
        return v == null ? "" : v.trim();
    }
}

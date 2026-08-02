package vn.tuphap.automation.config;

import org.testng.Assert;
import org.testng.annotations.Test;
import vn.tuphap.automation.config.RunFlowConfig.CaseProfile;

import java.util.List;

/**
 * Unit test cho bộ đọc Google Sheet — chỉ kiểm tra parse CSV + map cột, KHÔNG gọi mạng.
 * Nằm cùng package {@code config} để dùng được các method package-private.
 */
public class CaseSheetSourceTest {

    private static final String HEADER = row(
            "Chạy", "Loại đơn", "Loại việc", "Chủ thể", "Tư cách", "Tòa án", "Số bị đơn", "Đồng NĐ",
            "Đại diện", "Liên quan", "TL bổ sung", "Đến bước", "Gửi đơn",
            "Trường lỗi", "Giá trị lỗi", "Thông báo mong đợi", "Ghi chú");

    /** Ghép field thành 1 dòng CSV — tránh đếm dấu phẩy thủ công khi có 17 cột. */
    private static String row(String... fields) {
        return String.join(",", fields) + "\n";
    }

    @Test(groups = "unit", description = "Đọc đúng các trường của một dòng sheet đầy đủ")
    public void testDongDayDu() {
        String csv = HEADER + row(
                "x", "Dân sự", "Hợp đồng dân sự", "TC", "", "Sơn La", "2", "Có", "Không", "Có", "Có",
                "6", "x", "", "", "", "case chính");
        List<CaseProfile> cases = CaseSheetSource.parseCases(csv, "test");

        Assert.assertEquals(cases.size(), 1, "Phải đọc được 1 case");
        CaseProfile c = cases.get(0);
        Assert.assertEquals(c.loaiDon(), "Dân sự");
        Assert.assertEquals(c.loaiViec(), "Hợp đồng dân sự");
        Assert.assertEquals(c.chuThe(), "Tổ chức / Doanh nghiệp", "TC phải map sang chủ thể tổ chức");
        Assert.assertEquals(c.toaAn(), "Sơn La");
        Assert.assertEquals(c.soLuongBiDon(), 2);
        Assert.assertEquals(c.coDongNguyenDon(), Boolean.TRUE);
        Assert.assertEquals(c.coNguoiDaiDien(), Boolean.FALSE);
        Assert.assertEquals(c.coNguoiLienQuan(), Boolean.TRUE);
        Assert.assertEquals(c.coTaiLieuBoSung(), Boolean.TRUE);
        Assert.assertEquals(c.untilStep(), 6);
        Assert.assertTrue(c.submit(), "Cột Gửi đơn = x thì submit phải bật");
        Assert.assertFalse(c.hasNegativeExpectation(), "Không có Trường lỗi = case bình thường");
        Assert.assertEquals(c.ghiChu(), "case chính");
    }

    @Test(groups = "unit", description = "Ô trống ở cột nhánh = automation tự chọn")
    public void testOTrongLaTuChon() {
        String csv = HEADER + row(
                "x", "Phá sản", "", "CN", "Chủ nợ", "", "", "", "", "", "", "3", "", "", "", "", "");
        CaseProfile c = CaseSheetSource.parseCases(csv, "test").get(0);

        Assert.assertEquals(c.chuThe(), "Cá nhân");
        Assert.assertEquals(c.tuCachNopDon(), "Chủ nợ");
        Assert.assertEquals(c.toaAn(), "", "Tòa án trống = tự chọn");
        Assert.assertEquals(c.soLuongBiDon(), 0, "0 = tự chọn");
        Assert.assertNull(c.coDongNguyenDon());
        Assert.assertNull(c.coNguoiDaiDien());
        Assert.assertNull(c.coNguoiLienQuan());
        Assert.assertNull(c.coTaiLieuBoSung());
        Assert.assertEquals(c.untilStep(), 3);
        Assert.assertFalse(c.submit());
        Assert.assertFalse(c.hasNegativeExpectation());
    }

    @Test(groups = "unit", description = "Cột Chạy trống / Không → bỏ dòng đó")
    public void testCotChayTatDong() {
        String csv = HEADER
                + row("x", "Dân sự", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "", "6", "", "", "", "", "chạy")
                + row("", "Hành chính", "", "CN", "", "", "", "", "", "", "", "6", "", "", "", "", "tạm tắt")
                + row("Không", "Phá sản", "", "TC", "Chủ nợ", "", "", "", "", "", "", "6", "", "", "", "", "tắt");
        List<CaseProfile> cases = CaseSheetSource.parseCases(csv, "test");

        Assert.assertEquals(cases.size(), 1, "Chỉ dòng có x mới được chạy");
        Assert.assertEquals(cases.get(0).loaiDon(), "Dân sự");
    }

    @Test(groups = "unit", description = "Không có cột Chạy thì mọi dòng đều chạy")
    public void testKhongCoCotChay() {
        String csv = row("Loại đơn", "Loại việc", "Đến bước")
                + row("Dân sự", "Hợp đồng dân sự", "3")
                + row("Hành chính", "", "2");
        List<CaseProfile> cases = CaseSheetSource.parseCases(csv, "test");

        Assert.assertEquals(cases.size(), 2);
        Assert.assertEquals(cases.get(0).untilStep(), 3);
        Assert.assertEquals(cases.get(1).untilStep(), 2);
    }

    @Test(groups = "unit", description = "Tên cột lệch dấu / hoa thường / thứ tự vẫn nhận ra")
    public void testTenCotLinhHoat() {
        String csv = row("GUI DON", "DEN BUOC", "loai viec", "LOAI DON", "chu the (CN/TC)")
                + row("x", "6", "Hợp đồng dân sự", "Dân sự", "TC");
        CaseProfile c = CaseSheetSource.parseCases(csv, "test").get(0);

        Assert.assertEquals(c.loaiDon(), "Dân sự");
        Assert.assertEquals(c.loaiViec(), "Hợp đồng dân sự");
        Assert.assertEquals(c.chuThe(), "Tổ chức / Doanh nghiệp");
        Assert.assertEquals(c.untilStep(), 6);
        Assert.assertTrue(c.submit());
    }

    @Test(groups = "unit", description = "Ô có dấu phẩy / dấu nháy / xuống dòng vẫn parse đúng")
    public void testOCoDauPhayVaXuongDong() {
        String csv = HEADER + row(
                "x", "Dân sự", "\"Bồi thường thiệt hại ngoài hợp đồng\"", "CN", "", "", "1", "", "", "", "",
                "6", "", "", "", "", "\"ghi chú có dấu phẩy, và \"\"nháy\"\"\nxuống dòng\"");
        List<CaseProfile> cases = CaseSheetSource.parseCases(csv, "test");

        Assert.assertEquals(cases.size(), 1);
        CaseProfile c = cases.get(0);
        Assert.assertEquals(c.loaiViec(), "Bồi thường thiệt hại ngoài hợp đồng");
        Assert.assertTrue(c.ghiChu().contains("dấu phẩy, và \"nháy\""), "Ghi chú: " + c.ghiChu());
        Assert.assertTrue(c.ghiChu().contains("xuống dòng"), "Ghi chú: " + c.ghiChu());
    }

    @Test(groups = "unit", description = "submit bị ép false khi dừng trước bước 6")
    public void testSubmitBiEpFalseKhiChuaDenBuoc6() {
        String csv = HEADER + row(
                "x", "Dân sự", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "", "4", "x", "", "", "", "");
        CaseProfile c = CaseSheetSource.parseCases(csv, "test").get(0);

        Assert.assertEquals(c.untilStep(), 4);
        Assert.assertFalse(c.submit(), "Chưa tới bước 6 thì không được gửi đơn");
    }

    @Test(groups = "unit", description = "Cột Đến bước cho phép ghi gộp 6:submit")
    public void testDenBuocGopSubmit() {
        String csv = HEADER + row(
                "x", "Dân sự", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "", "6:submit", "", "", "", "", "");
        CaseProfile c = CaseSheetSource.parseCases(csv, "test").get(0);

        Assert.assertEquals(c.untilStep(), 6);
        Assert.assertTrue(c.submit());
    }

    @Test(groups = "unit", description = "Dòng trống và dòng thiếu Loại đơn bị bỏ qua")
    public void testDongTrongBiBoQua() {
        String csv = HEADER
                + row("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
                + row("x", "", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "", "6", "", "", "", "", "thiếu loại đơn")
                + row("x", "Dân sự", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "", "6", "", "", "", "", "ok");
        List<CaseProfile> cases = CaseSheetSource.parseCases(csv, "test");

        Assert.assertEquals(cases.size(), 1);
        Assert.assertEquals(cases.get(0).ghiChu(), "ok");
    }

    @Test(groups = "unit", description = "Sheet không có dòng tiêu đề nhận ra được → trả rỗng")
    public void testKhongCoTieuDe() {
        String csv = row("abc", "def") + row("1", "2");
        Assert.assertTrue(CaseSheetSource.parseCases(csv, "test").isEmpty());
    }

    @Test(groups = "unit", description = "Bỏ qua dòng tiêu đề phụ phía trên bảng")
    public void testBoQuaDongTieuDePhu() {
        String csv = row("DANH SÁCH TEST CASE — cổng dịch vụ tư pháp", "", "", "")
                + row("", "", "", "")
                + HEADER
                + row("x", "Dân sự", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "", "6", "", "", "", "", "");
        List<CaseProfile> cases = CaseSheetSource.parseCases(csv, "test");

        Assert.assertEquals(cases.size(), 1);
        Assert.assertEquals(cases.get(0).loaiDon(), "Dân sự");
    }

    @Test(groups = "unit", description = "Link chia sẻ / link có gid → URL export CSV")
    public void testDoiLinkThanhCsvUrl() {
        String share = "https://docs.google.com/spreadsheets/d/ABC123_-xyz/edit?usp=sharing";
        Assert.assertEquals(CaseSheetSource.toCsvExportUrl(share, ""),
                "https://docs.google.com/spreadsheets/d/ABC123_-xyz/export?format=csv");

        String withGid = "https://docs.google.com/spreadsheets/d/ABC123_-xyz/edit#gid=771";
        Assert.assertEquals(CaseSheetSource.toCsvExportUrl(withGid, ""),
                "https://docs.google.com/spreadsheets/d/ABC123_-xyz/export?format=csv&gid=771");

        Assert.assertEquals(CaseSheetSource.toCsvExportUrl(withGid, "42"),
                "https://docs.google.com/spreadsheets/d/ABC123_-xyz/export?format=csv&gid=42",
                "run.casesSheetGid phải thắng gid trên URL");

        Assert.assertNull(CaseSheetSource.toCsvExportUrl("https://example.com/abc", ""));
        Assert.assertNull(CaseSheetSource.toCsvExportUrl("", ""));
    }

    // ------------------------------------------------------- ca âm (negative case)

    @Test(groups = "unit", description = "Đọc đúng Trường lỗi / Giá trị lỗi / Thông báo mong đợi")
    public void testCaAmDayDu() {
        String csv = HEADER + row(
                "x", "Dân sự", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "",
                "2", "", "Số điện thoại (Nguyên đơn)", "0123", "Số điện thoại không hợp lệ", "ca âm SĐT");
        CaseProfile c = CaseSheetSource.parseCases(csv, "test").get(0);

        Assert.assertTrue(c.hasNegativeExpectation());
        Assert.assertEquals(c.truongLoi(), "Số điện thoại (Nguyên đơn)");
        Assert.assertEquals(c.giaTriLoi(), "0123");
        Assert.assertEquals(c.thongBaoMongDoi(), "Số điện thoại không hợp lệ");
        Assert.assertEquals(c.ghiChu(), "ca âm SĐT");
    }

    @Test(groups = "unit", description = "Trường lỗi rỗng = case bình thường, không đọc Giá trị lỗi/Thông báo")
    public void testKhongPhaiCaAmKhiTruongLoiTrong() {
        String csv = HEADER + row(
                "x", "Dân sự", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "",
                "6", "", "", "giá trị này bị bỏ qua", "", "");
        CaseProfile c = CaseSheetSource.parseCases(csv, "test").get(0);

        Assert.assertFalse(c.hasNegativeExpectation(), "Trường lỗi trống thì không phải ca âm");
        Assert.assertEquals(c.truongLoi(), "");
    }

    @Test(groups = "unit", description = "Trường lỗi có nhưng Giá trị lỗi trống = cố tình để trống field đó")
    public void testCaAmDeTrongGiaTri() {
        String csv = HEADER + row(
                "x", "Dân sự", "Hợp đồng dân sự", "CN", "", "", "", "", "", "", "",
                "2", "", "Email (Nguyên đơn)", "", "", "");
        CaseProfile c = CaseSheetSource.parseCases(csv, "test").get(0);

        Assert.assertTrue(c.hasNegativeExpectation());
        Assert.assertEquals(c.truongLoi(), "Email (Nguyên đơn)");
        Assert.assertEquals(c.giaTriLoi(), "", "Giá trị lỗi trống = cố tình bỏ trống trường");
        Assert.assertEquals(c.thongBaoMongDoi(), "", "Trống = chấp nhận mọi thông báo chặn");
    }

    @Test(groups = "unit", description = "Alias tên cột ca âm linh hoạt (dấu/hoa thường/thứ tự)")
    public void testAliasCotCaAmLinhHoat() {
        String csv = row("LOAI DON", "GIA TRI LOI", "truong loi", "thong bao mong doi", "DEN BUOC")
                + row("Dân sự", "abc", "CCCD (Bị đơn)", "CCCD không hợp lệ", "3");
        CaseProfile c = CaseSheetSource.parseCases(csv, "test").get(0);

        Assert.assertEquals(c.truongLoi(), "CCCD (Bị đơn)");
        Assert.assertEquals(c.giaTriLoi(), "abc");
        Assert.assertEquals(c.thongBaoMongDoi(), "CCCD không hợp lệ");
    }
}

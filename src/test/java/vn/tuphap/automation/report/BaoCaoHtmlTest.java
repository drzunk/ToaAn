package vn.tuphap.automation.report;

import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Khoá cấu trúc của {@code index.html}.
 * <p>
 * Mọi lỗi báo cáo tìm được trong đợt làm này đều thuộc một loại: <b>không có ngoại lệ nào bắn ra</b>
 * — trang vẫn dựng, chỉ là sai. Lớp phóng ảnh mở sẵn khi tải trang, thời gian in ra "2 phút 60
 * giây", bước hiện "—", dòng lỗi sai thứ tự. Chỉ mở ra nhìn mới thấy.
 * <p>
 * Test này dựng trang từ dữ liệu giả và soi thẳng chuỗi HTML — không chạy trình duyệt, không ghi
 * đĩa, không đụng {@code index.html} thật.
 */
public class BaoCaoHtmlTest {

    private static BaoCaoData.SuKien sk(String muc, String noiDung, String anh) {
        return new BaoCaoData.SuKien(muc, "09:30:00", noiDung, anh);
    }

    private static BaoCaoHtml.LuotChay luot(String moc, String bo, BaoCaoData.CaseBaoCao... cases) {
        int dat = 0;
        int loi = 0;
        for (BaoCaoData.CaseBaoCao c : cases) {
            if (TrangThai.DAT.equals(c.trangThai())) {
                dat++;
            } else {
                loi++;
            }
        }
        return new BaoCaoHtml.LuotChay(moc, LocalDateTime.of(2026, 8, 2, 9, 30).toString(), bo,
                dat, loi, 0, 120_000, 300_000, List.of(cases));
    }

    private static BaoCaoData.CaseBaoCao ca(String ma, String trangThai, List<BaoCaoData.SuKien> cuoi) {
        return new BaoCaoData.CaseBaoCao(ma, "Kịch bản " + ma, "Mô tả", List.of("Dân sự", "Thừa kế"),
                trangThai, 61_000,
                List.of(new BaoCaoData.BuocBaoCao(1, "Chọn loại đơn", TrangThai.DAT, 9_000,
                        List.of(sk(BaoCaoData.MUC_PASS, "Xong bước 1", null)),
                        List.of(new BaoCaoData.HanhDong("Điền", "Họ và tên", "Nguyễn Văn A", "")))),
                List.of(), cuoi, null,
                List.of(new BaoCaoData.TomTatBuoc(1, "Chọn loại đơn", TrangThai.DAT, 9_000),
                        new BaoCaoData.TomTatBuoc(2, "Điền nguyên đơn", TrangThai.CHUA_CHAY_TOI, -1)),
                "Mong đợi X", "Thực tế Y", "Ghi chú Z");
    }

    /**
     * Nội dung do hệ thống trả về phải được escape. Thông báo lỗi của ứng dụng đã từng chứa
     * {@code <} và làm vỡ bố cục; và bộ ca âm có sẵn hai dòng {@code <script>alert(1)</script>}.
     */
    @Test(groups = {"unit"})
    public void noiDungDuocEscapeKhongLamVoTrang() {
        String doc = "<script>alert(1)</script> & \"nháy\" <b>đậm</b>";
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                luot("20260802_093000", "MID", ca("TC_MID_001", TrangThai.THAT_BAI,
                        List.of(sk(BaoCaoData.MUC_FAIL, doc, null))))));

        assertFalse(html.contains("<script>alert(1)</script>"),
                "Nội dung từ hệ thống lọt thẳng vào HTML — trang có thể bị vỡ hoặc chạy mã lạ");
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"), "Chưa escape thẻ");
        assertTrue(html.contains("&amp;"), "Chưa escape dấu &");
        // Đúng một khối script: khối của chính báo cáo.
        assertEquals(html.split("<script>", -1).length - 1, 1, "Số khối <script> bất thường");
    }

    /**
     * Lớp phóng ảnh phải đóng khi tải trang. Từng có lúc luật {@code .den-anh{display:flex}} thắng
     * thuộc tính {@code hidden}, nên mở báo cáo ra là thấy một tấm ảnh đen che kín màn hình.
     */
    @Test(groups = {"unit"})
    public void lopPhongAnhDongKhiTaiTrang() {
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                luot("20260802_093000", "MID", ca("TC_MID_001", TrangThai.DAT, List.of()))));
        assertTrue(html.contains("id=\"den-anh\" class=\"den-anh\" hidden"),
                "Thẻ lớp phóng ảnh phải có hidden");
        assertTrue(html.contains(".den-anh[hidden]{display:none}"),
                "Thiếu luật CSS này thì hidden bị display:flex đè, lớp phóng ảnh mở sẵn");
    }

    /** Ba trường kết luận và dải 6 bước phải hiện ra, kể cả bước chưa chạy tới. */
    @Test(groups = {"unit"})
    public void hienKetLuanVaBuocChuaChayToi() {
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                luot("20260802_093000", "MID", ca("TC_MID_001", TrangThai.THAT_BAI, List.of()))));
        assertTrue(html.contains("Kết quả mong đợi"), "Mất trường Kết quả mong đợi");
        assertTrue(html.contains("Thực tế Y") && html.contains("Ghi chú Z"), "Mất kết luận");
        assertTrue(html.contains(TrangThai.CHUA_CHAY_TOI),
                "Bước chưa chạy tới không hiện — báo cáo lại quay về suy đoán là đã đạt");
    }

    /** Neo liên kết sâu phải gắn mốc lượt chạy, nếu không mọi lượt đều nhảy về lượt đầu. */
    @Test(groups = {"unit"})
    public void neoLienKetSauPhanBietDuocCacLuot() {
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                luot("20260802_093000", "MID", ca("TC_MID_001", TrangThai.DAT, List.of())),
                luot("20260802_101500", "MID", ca("TC_MID_001", TrangThai.THAT_BAI, List.of()))));
        assertTrue(html.contains("id=\"c-20260802-093000-TC-MID-001\""), "Thiếu neo lượt thứ nhất");
        assertTrue(html.contains("id=\"c-20260802-101500-TC-MID-001\""), "Thiếu neo lượt thứ hai");
    }

    /**
     * Đối chiếu với lượt trước phải gắn đúng nhãn — đây là câu hỏi thật của một bộ hồi quy:
     * không phải "bao nhiêu case hỏng" mà "cái gì vừa mới hỏng".
     */
    @Test(groups = {"unit"})
    public void ganDungNhanMoiHongVaDaSua() {
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                luot("20260802_093000", "MID",
                        ca("TC_MID_001", TrangThai.DAT, List.of()),
                        ca("TC_MID_002", TrangThai.THAT_BAI, List.of())),
                luot("20260802_101500", "MID",
                        ca("TC_MID_001", TrangThai.THAT_BAI, List.of()),
                        ca("TC_MID_002", TrangThai.DAT, List.of()))));
        assertTrue(html.contains("Mới hỏng"), "Case lượt trước đạt, lượt này hỏng mà không gắn nhãn");
        assertTrue(html.contains("Đã sửa"), "Case lượt trước hỏng, lượt này đạt mà không gắn nhãn");
    }

    /**
     * Gom nhóm lỗi phải gộp các thông báo chỉ khác nhau ở mã lỗi GUID. Không gộp thì 39 case hỏng
     * cùng một lý do hiện thành 39 dòng, và bảng này mất sạch tác dụng.
     */
    @Test(groups = {"unit"})
    public void gomNhomLoiBoQuaMaLoiGuid() {
        String a = "Đã xảy ra lỗi khi gọi đến hệ thống Quản Lý Án Mã lỗi: "
                + "f315278d-f63d-432b-97ee-b29e5621a74a";
        String b = "Đã xảy ra lỗi khi gọi đến hệ thống Quản Lý Án Mã lỗi: "
                + "be887d90-71d8-4757-95dd-60791368e295";
        assertEquals(BaoCaoHtml.chuanHoaLoi(a), BaoCaoHtml.chuanHoaLoi(b),
                "Hai lỗi giống hệt nhau trừ mã GUID mà không gộp được");

        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                luot("20260802_093000", "MID",
                        ca("TC_MID_001", TrangThai.THAT_BAI, List.of(sk(BaoCaoData.MUC_FAIL, a, null))),
                        ca("TC_MID_002", TrangThai.THAT_BAI, List.of(sk(BaoCaoData.MUC_FAIL, b, null))))));
        assertTrue(html.contains("Các lỗi thường gặp"), "Thiếu bảng gom nhóm lỗi");
        // Đếm THẺ chứ không đếm chuỗi: "gomloi-dong" còn xuất hiện trong ba luật CSS.
        assertEquals(html.split("<div class=\"gomloi-dong\">", -1).length - 1, 1,
                "Hai case cùng một lý do phải gộp thành đúng một dòng");
    }

    /**
     * Kịch bản <b>bỏ qua</b> không được gộp vào "Các lỗi thường gặp".
     * <p>
     * Một lượt hỏng ngay khâu đăng nhập khiến toàn bộ kịch bản bị TestNG bỏ qua. Gộp chung thì báo
     * cáo nói "39 lỗi" — người đọc kết luận hệ thống hỏng 39 chỗ, trong khi sự thật là chưa kiểm
     * được chỗ nào. Hai câu chuyện hoàn toàn khác nhau.
     */
    @Test(groups = {"unit"})
    public void boQuaTachKhoiLoi() {
        BaoCaoData.CaseBaoCao boQua = new BaoCaoData.CaseBaoCao("TC_MID_001", "Kịch bản 1", "—",
                List.of("Dân sự", "Thừa kế"), TrangThai.BO_QUA, 0, List.of(), List.of(),
                List.of(sk(BaoCaoData.MUC_SKIP, "Không nhận được phản hồi từ trang web", null)),
                null, List.of(), "", "", "");
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                new BaoCaoHtml.LuotChay("20260802_093000", LocalDateTime.of(2026, 8, 2, 9, 30).toString(),
                        "MID", 0, 0, 1, 1000, 1000, List.of(boQua))));

        assertFalse(html.contains("Các lỗi thường gặp"),
                "Không có kịch bản nào thất bại mà vẫn hiện bảng lỗi");
        assertTrue(html.contains("Vì sao các kịch bản bị bỏ qua"), "Thiếu khối lý do bỏ qua");
        assertTrue(html.contains("Không nhận được phản hồi từ trang web"),
                "Lý do bỏ qua bị nuốt — báo cáo hiện 'Không ghi nhận thông báo cụ thể'");
        assertFalse(html.contains("Vẫn hỏng"), "Kịch bản bỏ qua bị gắn nhãn Vẫn hỏng");
        assertTrue(html.contains("Không chạy được"), "Thẻ chỉ số phải nói rõ cả lượt không chạy được");
    }

    /**
     * Thông báo lỗi phải sạch chữ của lập trình viên và chữ trên nút bấm của chính ứng dụng —
     * nhưng <b>giữ mã lỗi</b>, vì đó là thứ duy nhất tra được khi báo sang đội ứng dụng.
     */
    @Test(groups = {"unit"})
    public void thongBaoLoiSachChoNguoiDoc() {
        String tho = "Kịch bản thất bại. Chi tiết lỗi: Hệ thống báo lỗi sau Gửi đơn: "
                + "Đã xảy ra lỗi khi gọi đến hệ thống Quản Lý Án Mã lỗi: "
                + "f315278d-f63d-432b-97ee-b29e5621a74a Sao chép mã";
        String sach = BaoCaoHtml.loiChoNguoiDoc(tho);

        assertFalse(sach.contains("Kịch bản thất bại"), "Còn tiền tố lặp lại huy hiệu trạng thái");
        assertFalse(sach.contains("Sao chép mã"), "Còn chữ trên nút bấm của ứng dụng");
        assertTrue(sach.contains("f315278d-f63d-432b-97ee-b29e5621a74a"),
                "Mất mã lỗi — không còn tra được khi báo sang đội ứng dụng");
        assertTrue(sach.startsWith("Hệ thống báo lỗi sau Gửi đơn"), "Nội dung thật bị cắt: " + sach);

        // Gỡ URL phải gỡ luôn dấu phân cách của nó, không để lại "·" mồ côi cuối câu.
        assertEquals(BaoCaoHtml.loiChoNguoiDoc(
                        "Ảnh tổng quan — màn Xem lại · https://a.b/c-d — Toàn bộ khung nhìn hiện tại (1/1)"),
                "Ảnh tổng quan — màn Xem lại", "Chú thích ảnh chưa sạch");
    }

    /**
     * Ghi chú chỉ chép lại Kết quả thực tế thì không được hiện — chiếm một dòng mà không nói thêm
     * gì, và bắt người đọc đọc hai lần mới nhận ra là cùng một câu.
     */
    @Test(groups = {"unit"})
    public void ghiChuTrungKetQuaThucTeThiAnDi() {
        BaoCaoData.CaseBaoCao c = new BaoCaoData.CaseBaoCao("TC_MID_001", "Kịch bản 1", "—",
                List.of("Dân sự", "Thừa kế"), TrangThai.THAT_BAI, 1000, List.of(), List.of(),
                List.of(), null, List.of(),
                "Nộp trọn đơn qua 6 bước.",
                "Hệ thống báo lỗi sau Gửi đơn: Đã xảy ra lỗi khi gọi đến hệ thống Quản Lý Án "
                        + "Mã lỗi: f315278d-f63d-432b-97ee-b29e5621a74a",
                "Message lỗi hệ thống sau Gửi đơn: Đã xảy ra lỗi khi gọi đến hệ thống Quản Lý Án "
                        + "Mã lỗi: be887d90-71d8-4757-95dd-60791368e295");
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                new BaoCaoHtml.LuotChay("20260802_093000", LocalDateTime.of(2026, 8, 2, 9, 30).toString(),
                        "MID", 0, 1, 0, 1000, 1000, List.of(c))));

        assertTrue(html.contains("Kết quả thực tế"), "Mất Kết quả thực tế");
        assertFalse(html.contains("<dt>Ghi chú</dt>"),
                "Ghi chú chép lại Kết quả thực tế mà vẫn hiện");

        // Ghi chú nói thêm điều gì đó thì phải giữ.
        BaoCaoData.CaseBaoCao d = new BaoCaoData.CaseBaoCao("TC_MID_002", "Kịch bản 2", "—",
                List.of("Dân sự", "Thừa kế"), TrangThai.THAT_BAI, 1000, List.of(), List.of(),
                List.of(), null, List.of(), "Mong đợi", "Thực tế A",
                "Đã điền xong 6 bước — lỗi ở phía hệ thống nhận đơn.");
        String html2 = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                new BaoCaoHtml.LuotChay("20260802_093000", LocalDateTime.of(2026, 8, 2, 9, 30).toString(),
                        "MID", 0, 1, 0, 1000, 1000, List.of(d))));
        assertTrue(html2.contains("<dt>Ghi chú</dt>"), "Ghi chú có nội dung riêng mà bị nuốt");
    }

    /**
     * Giờ hiện trên dòng tiêu đề phải là giờ <b>bắt đầu</b> — cùng giờ với thư mục {@code runs/}
     * mà liên kết ngay dưới nó trỏ tới.
     * <p>
     * {@code iso} từng được ghi lúc kết thúc suite, lệch tới 18 phút so với tên thư mục: người đọc
     * thấy một giờ trên tiêu đề, một giờ khác trong đường dẫn ảnh, và không ghép được với log.
     */
    @Test(groups = {"unit"})
    public void gioHienThiLayTuMocThuMuc() {
        // iso cố ý đặt lệch 18 phút, đúng như dữ liệu đã lưu trên đĩa.
        BaoCaoHtml.LuotChay l = new BaoCaoHtml.LuotChay("20260802_092345",
                LocalDateTime.of(2026, 8, 2, 9, 41, 21).toString(), "MID",
                0, 1, 0, 1_000_000, 1_000_000,
                List.of(ca("TC_MID_001", TrangThai.THAT_BAI, List.of())));
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(l));

        assertTrue(html.contains("02/08/2026 09:23"),
                "Tiêu đề phải hiện giờ bắt đầu 09:23 (khớp thư mục), không phải giờ kết thúc");
        assertFalse(html.contains("02/08/2026 09:41"), "Vẫn còn hiện giờ kết thúc");
        assertTrue(html.contains("runs/20260802_092345/screenshots")
                        || html.contains("runs/20260802_092345/"),
                "Liên kết phải trỏ đúng thư mục của lượt");
    }

    /**
     * Sắp xếp phải theo thời điểm đã phân tích, không so chuỗi {@code iso} — chỉ cần một file lưu
     * khuôn khác là mọi bản ghi đó dồn lên đầu bất kể ngày, và "lượt trước" bị chọn sai, kéo theo
     * nhãn <i>Mới hỏng / Đã sửa</i> đảo ngược.
     */
    @Test(groups = {"unit"})
    public void sapXepTheoThoiDiemKhongPhaiChuoi() {
        BaoCaoHtml.LuotChay cu = new BaoCaoHtml.LuotChay("20260801_080000",
                "2026-08-01 08:00:00", "MID", 1, 0, 0, 1000, 1000,
                List.of(ca("TC_MID_001", TrangThai.DAT, List.of())));
        BaoCaoHtml.LuotChay moi = new BaoCaoHtml.LuotChay("20260802_090000",
                LocalDateTime.of(2026, 8, 2, 9, 0).toString(), "MID", 0, 1, 0, 1000, 1000,
                List.of(ca("TC_MID_001", TrangThai.THAT_BAI, List.of())));
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(cu, moi));

        // Lượt mới nhất đứng đầu danh sách và được đối chiếu với lượt cũ.
        assertTrue(html.indexOf("02/08/2026 09:00") < html.indexOf("01/08/2026 08:00"),
                "Lượt mới nhất phải đứng trên");
        assertTrue(html.contains("Mới hỏng"),
                "Lượt cũ đạt, lượt mới hỏng — phải nhận ra là 'Mới hỏng'");
    }

    /**
     * Phễu 6 bước phải nói <b>bước N là gì</b> và bao nhiêu kịch bản rụng ở đâu.
     * <p>
     * Trước đây tên bước chỉ nằm trong tooltip của dải tiến độ — người đọc thấy "Bước 4" mà không
     * biết đó là khâu nào, và không có chỗ nào trên trang giải thích.
     */
    @Test(groups = {"unit"})
    public void phieuLuongNoiRoTenBuocVaChoRung() {
        // Hai kịch bản qua hết; một kịch bản dừng ở bước 4.
        BaoCaoData.CaseBaoCao dat = caVoiTomTat("TC_A", TrangThai.DAT, 7);
        BaoCaoData.CaseBaoCao hong = caVoiTomTat("TC_B", TrangThai.THAT_BAI, 4);
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of(
                new BaoCaoHtml.LuotChay("20260802_093000",
                        LocalDateTime.of(2026, 8, 2, 9, 30).toString(), "FULL",
                        1, 1, 0, 1000, 1000, List.of(dat, hong))));

        assertTrue(html.contains("Luồng nộp đơn"), "Thiếu khối tổng quan luồng");
        // Tên chuẩn của cả 6 bước phải có mặt.
        for (int i = 1; i <= 6; i++) {
            assertTrue(html.contains(TaoDonReportBuilder.tenBuocDayDu(i)),
                    "Không nói bước " + i + " là gì");
        }
        assertTrue(html.contains("1 dừng ở đây"), "Không chỉ ra bước làm rụng kịch bản");
    }

    /** @param dungO bước bị "Không hoàn thành"; {@code > 6} nghĩa là qua hết */
    private static BaoCaoData.CaseBaoCao caVoiTomTat(String ma, String tt, int dungO) {
        java.util.List<BaoCaoData.TomTatBuoc> tom = new java.util.ArrayList<>();
        for (int b = 1; b <= 6; b++) {
            String kq = b < dungO ? TrangThai.DAT
                    : b == dungO ? TrangThai.KHONG_HOAN_THANH : TrangThai.CHUA_CHAY_TOI;
            tom.add(new BaoCaoData.TomTatBuoc(b, "Bước " + b, kq, b < dungO ? 1000L * b : -1));
        }
        return new BaoCaoData.CaseBaoCao(ma, "Kịch bản " + ma, "—", List.of("Dân sự", "Thừa kế"),
                tt, 5000, List.of(), List.of(), List.of(), null, tom, "", "", "");
    }

    /** Chưa có lượt nào thì phải nói rõ phải làm gì, không để trang trắng. */
    @Test(groups = {"unit"})
    public void trangRongCoHuongDan() {
        String html = BaoCaoHtml.dungTrangDeKiemTra(List.of());
        assertTrue(html.contains("Chưa có lượt chạy nào được lưu"), "Thiếu trạng thái rỗng");
        assertTrue(html.contains("chay.cmd"), "Trạng thái rỗng phải chỉ cách chạy");
    }
}

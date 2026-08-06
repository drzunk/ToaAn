package vn.tuphap.automation.report;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Khoá các đường dẫn dữ liệu chảy từ {@link BaoCao} sang {@link BaoCaoData}.
 * <p>
 * Đây là những chỗ mà hỏng thì <b>không có lỗi nào bắn ra</b>: báo cáo vẫn dựng được, chỉ là thiếu
 * ảnh, thiếu dữ liệu đã nhập, hoặc bước hỏng hiện 0 mili giây. Một lượt chạy thật không phát hiện
 * được — phải mở báo cáo, đếm bằng mắt, và đối chiếu với bản Extent. Đợt gỡ Extent phát hiện đúng
 * bốn chỗ như vậy, nên chốt lại bằng test thay vì bằng trí nhớ.
 * <p>
 * Chạy hoàn toàn ngoại tuyến — không cần trình duyệt, không cần VPN.
 */
public class BaoCaoTeeTest {

    /** Ảnh PNG hợp lệ nhỏ nhất có thể, đủ để {@code ImageIO} đọc được. */
    private static String anhGia() throws Exception {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private String mocLuot;
    private Path thuMucLuot;
    private boolean tuTao;

    @BeforeClass(alwaysRun = true)
    public void moLuotRieng() {
        ScreenshotStore.initRun();
        mocLuot = ScreenshotStore.runStamp();
        thuMucLuot = Paths.get("test-output", "runs", mocLuot);
        // Chỉ dọn thư mục do CHÍNH test này tạo ra. Lần trước tôi dọn bằng bộ lọc so tên và xoá
        // nhầm cả những lượt chạy thật — nên ở đây ghi nhớ đúng một đường dẫn, không đoán.
        tuTao = !Files.exists(thuMucLuot);
        BaoCaoData.xoaHet();
    }

    @AfterClass(alwaysRun = true)
    public void donThuMucTest() throws Exception {
        BaoCaoData.xoaHet();
        ScreenshotStore.clearCase();
        if (!tuTao || thuMucLuot == null || !Files.exists(thuMucLuot)) {
            return;
        }
        try (var duyet = Files.walk(thuMucLuot)) {
            duyet.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                    // Không để việc dọn dẹp làm hỏng kết quả test.
                }
            });
        }
    }

    /**
     * Ảnh ngữ cảnh theo bước phải có mặt trong bộ thu, <b>kể cả khi không có đối tượng Extent nào</b>.
     * <p>
     * Trước khi sửa, {@code logScreenshots} là hàm duy nhất trong lớp không tee: 4 ảnh mỗi lượt chỉ
     * tồn tại trong {@code ExtentReport.html}. Gỡ thư viện đi là chúng biến mất, lặng lẽ.
     */
    @Test(groups = {"unit"})
    public void anhNguCanhTheoBuocChayVaoBoThu() throws Exception {
        BaoCaoData.xoaHet();
        ScreenshotStore.beginCase("TC_TEE_ANH");
        BaoCaoData.batDauCase("TC_TEE_ANH", "Kiểm thử tee ảnh", "—", "Nhóm A");
        BaoCaoData.moBuoc(2, "Điền thông tin nguyên đơn");

        String anh = anhGia();
        BaoCao.logScreenshots("Ảnh ngữ cảnh — bước 2", List.of(anh, anh, anh));

        BaoCaoData.dongBuoc("Đạt", 1234);
        BaoCaoData.ketThucCase("Đạt", 5678);

        List<BaoCaoData.CaseBaoCao> cases = BaoCaoData.cases();
        assertEquals(cases.size(), 1, "Phải thu được đúng 1 kịch bản");
        List<BaoCaoData.SuKien> suKien = cases.get(0).buoc().get(0).suKien();
        assertEquals(suKien.size(), 3, "Cả 3 ảnh ngữ cảnh phải thành sự kiện trong bước");
        for (BaoCaoData.SuKien sk : suKien) {
            assertNotNull(sk.anh(), "Sự kiện ảnh mà không có đường dẫn ảnh: " + sk.noiDung());
            assertTrue(Files.exists(Paths.get("test-output").resolve(sk.anh())),
                    "Đường dẫn ảnh trỏ vào file không tồn tại: " + sk.anh());
        }
    }

    /**
     * Hai cờ mà {@code TestListener} dựa vào phải trả lời từ bộ thu, không phải từ Extent —
     * nếu không, gỡ thư viện xong là case bị bỏ qua biến mất và mọi lỗi bị chụp ảnh hai lần.
     */
    @Test(groups = {"unit"})
    public void coDangMoCaseVaCoAnhLoiTraLoiDungTrangThai() throws Exception {
        BaoCaoData.xoaHet();
        assertFalse(BaoCao.hasCurrentTest(), "Chưa mở case nào mà đã báo là có");

        ScreenshotStore.beginCase("TC_TEE_CO");
        BaoCaoData.batDauCase("TC_TEE_CO", "Kiểm thử hai cờ", "—");
        assertTrue(BaoCao.hasCurrentTest(), "Đã mở case mà vẫn báo là không");
        assertFalse(BaoCao.wasFailScreenshotAttached(), "Chưa có lỗi nào mà đã báo có ảnh lỗi");

        // Lỗi ghi bằng chữ: vẫn phải để listener chụp bổ sung.
        BaoCaoData.suKien(BaoCaoData.MUC_FAIL, "Lỗi chung không kèm ảnh", null);
        assertFalse(BaoCao.wasFailScreenshotAttached(),
                "Lỗi chỉ có chữ mà lại chặn việc chụp ảnh — đúng loại lỗi cần bằng chứng nhất");

        BaoCaoData.suKien(BaoCaoData.MUC_FAIL, "Lỗi kèm ảnh", "runs/x/screenshots/y/01-loi.png");
        assertTrue(BaoCao.wasFailScreenshotAttached(), "Lỗi đã có ảnh mà vẫn báo là chưa");

        BaoCaoData.ketThucCase("Thất bại", 100);
        assertFalse(BaoCao.hasCurrentTest(), "Đóng case rồi mà vẫn báo là đang mở");
    }

    /**
     * Bước dở dang — nhánh lỗi ném ngoại lệ, không ai gọi {@code logStepDone} — vẫn phải có thời
     * gian thật. Trước khi sửa, {@code ketThucCase} đóng hộ với 0 nên báo cáo in
     * "Thất bại — 0 mili giây" cho đúng cái bước tốn thời gian nhất.
     */
    @Test(groups = {"unit"})
    public void buocDoDangGiuDuocThoiGianThat() throws Exception {
        BaoCaoData.xoaHet();
        BaoCaoData.batDauCase("TC_TEE_DODANG", "Kiểm thử bước dở dang", "—");
        BaoCaoData.moBuoc(4, "Điền nội dung đơn");
        Thread.sleep(30);
        BaoCaoData.ketThucCase("Thất bại", 900);

        List<BaoCaoData.BuocBaoCao> buoc = BaoCaoData.cases().get(0).buoc();
        assertEquals(buoc.size(), 1, "Bước đang mở phải được đóng hộ, không bị nuốt");
        assertEquals(buoc.get(0).trangThai(), "Thất bại");
        assertTrue(buoc.get(0).thoiGianMs() >= 30,
                "Bước dở dang phải giữ thời gian thật, nhận được " + buoc.get(0).thoiGianMs() + " ms");
    }

    /**
     * Mục "Thiết lập session — đăng nhập" không phải kịch bản: nó gọi {@code createTest} để có chỗ
     * ghi log rồi {@code clearTestContext}, và sau đó thread phải sạch.
     * <p>
     * Không dọn thì case rác đọng lại, {@code hasCurrentTest()} trả về {@code true}, và kịch bản bị
     * TestNG bỏ qua sẽ bị ghi đè lên mục thiết lập session thay vì có mục riêng — đúng kiểu hỏng
     * không bắn lỗi mà chỉ thấy khi mở báo cáo ra đếm.
     */
    @Test(groups = {"unit"})
    public void mucThietLapSessionKhongDongLaiTrenThread() {
        BaoCaoData.xoaHet();
        BaoCao.createTest("Thiết lập session — đăng nhập (Chrome 1)", "Setup trước khi chạy");
        BaoCao.logPass("Session sẵn sàng");
        BaoCao.clearTestContext();

        assertFalse(BaoCao.hasCurrentTest(), "Mục thiết lập session còn đọng lại trên thread");
        assertEquals(BaoCaoData.cases().size(), 0, "Mục thiết lập session bị đếm thành kịch bản");

        // Kịch bản bị bỏ qua ngay sau đó vẫn phải có mục riêng.
        BaoCao.createTest("Kịch bản số 7 — Dân sự / Thừa kế", "—");
        BaoCao.logSkip("Đăng nhập hỏng ở @BeforeMethod");
        BaoCaoData.ketThucCase("Bỏ qua", 5);
        assertEquals(BaoCaoData.cases().size(), 1);
        assertEquals(BaoCaoData.cases().get(0).tieuDe(), "Kịch bản số 7 — Dân sự / Thừa kế");
    }

    /** Bảng tóm tắt phải nói được bước nào <b>chưa chạy tới</b>, không suy đoán là đạt. */
    @Test(groups = {"unit"})
    public void tomTatBuocPhanBietChuaChayToiVoiKhongHoanThanh() {
        BaoCaoData.xoaHet();
        StepOutcome.beginCase();
        BaoCao.createTest("TC_TOMTAT", "—");
        StepOutcome.record(1, "Chọn loại đơn", 1200, true);
        StepOutcome.record(2, "Điền nguyên đơn", 3400, true);
        StepOutcome.record(3, "Điền bị đơn", 800, false);
        BaoCao.logStepSummary();
        BaoCaoData.ketThucCase("Thất bại", 5400);
        StepOutcome.clear();

        List<BaoCaoData.TomTatBuoc> tt = BaoCaoData.cases().get(0).tomTatBuocAnToan();
        assertEquals(tt.size(), 6, "Phải liệt kê đủ 6 bước, kể cả bước chưa chạy");
        assertEquals(tt.get(0).ketQua(), "Đạt");
        assertEquals(tt.get(2).ketQua(), "Không hoàn thành");
        assertEquals(tt.get(3).ketQua(), "Chưa chạy tới", "Bước 4 chưa chạy tới mà lại báo khác");
        assertEquals(tt.get(5).ketQua(), "Chưa chạy tới");
        assertEquals(tt.get(3).thoiGianMs(), -1, "Bước chưa chạy thì không được có thời gian");
    }

    /**
     * Dữ liệu từng trường phải chảy vào đúng bước đang mở, và <b>không</b> ghi khi đang tạm dừng.
     * <p>
     * {@code pause()} là thứ giữ cho phần đăng nhập ở {@code @BeforeClass} không lẫn vào kịch bản
     * đầu tiên — hỏng cái này thì case số 1 tự nhiên có thêm mấy trường của màn đăng nhập.
     */
    @Test(groups = {"unit"})
    public void duLieuDaNhapChaySangBaoCaoHtml() {
        BaoCaoData.xoaHet();
        TestActionLog.beginTest();
        BaoCaoData.batDauCase("TC_TEE_NHAP", "Kiểm thử tee dữ liệu", "—");
        BaoCaoData.moBuoc(2, "Điền thông tin nguyên đơn");

        TestActionLog.dien("Họ và tên", "Nguyễn Văn A");
        TestActionLog.chon("Tỉnh/Thành phố", "Hà Nội");
        TestActionLog.pause();
        TestActionLog.dien("Tên đăng nhập", "khong-duoc-ghi");
        TestActionLog.resume();

        BaoCaoData.dongBuoc("Đạt", 10);
        BaoCaoData.ketThucCase("Đạt", 20);

        List<BaoCaoData.HanhDong> hd = BaoCaoData.cases().get(0).buoc().get(0).hanhDong();
        assertEquals(hd.size(), 2, "Thao tác lúc đang tạm dừng vẫn lọt vào báo cáo");
        assertEquals(hd.get(0).truong(), "Họ và tên");
        assertEquals(hd.get(0).giaTri(), "Nguyễn Văn A");
        assertEquals(hd.get(1).thaoTac(), "Chọn");
    }

    /**
     * Kịch bản chưa mở bước nào — suite login — vẫn phải giữ được dữ liệu đã nhập. Trước đây thao
     * tác ngoài bước bị bỏ hẳn, nên báo cáo lượt login không có phần dữ liệu nào.
     */
    @Test(groups = {"unit"})
    public void thaoTacNgoaiBuocDuocGiuOThanCase() {
        BaoCaoData.xoaHet();
        TestActionLog.beginTest();
        BaoCaoData.batDauCase("testDangNhapSaiMatKhau", "Ca âm — sai mật khẩu", "—");

        TestActionLog.dien("Ô nhập [CCCD/Tên đăng nhập]", "0123456789");
        TestActionLog.dien("Ô nhập [Captcha]", "abcd");

        BaoCaoData.ketThucCase("Đạt", 20);

        List<BaoCaoData.HanhDong> hd = BaoCaoData.cases().get(0).hanhDongNgoaiBuocAnToan();
        assertEquals(hd.size(), 2, "Thao tác lúc chưa mở bước nào bị mất khỏi báo cáo");
        assertEquals(hd.get(0).giaTri(), "0123456789");
    }

    /** Mật khẩu là bí mật môi trường — không được nằm trong báo cáo hay {@code bao-cao.json}. */
    @Test(groups = {"unit"})
    public void giaTriOMatKhauKhongLotVaoBaoCao() {
        BaoCaoData.xoaHet();
        TestActionLog.beginTest();
        BaoCaoData.batDauCase("testDangNhapSaiMatKhau", "Ca âm — sai mật khẩu", "—");

        TestActionLog.dien("Ô nhập [Mật khẩu]", "MatKhauThat_123");

        BaoCaoData.ketThucCase("Đạt", 20);

        List<BaoCaoData.HanhDong> hd = BaoCaoData.cases().get(0).hanhDongNgoaiBuocAnToan();
        assertEquals(hd.size(), 1);
        assertFalse(hd.get(0).giaTri().contains("MatKhauThat_123"),
                "Mật khẩu thật bị ghi vào báo cáo");
    }

    /**
     * Bước đang chạy dở khi kịch bản hỏng phải hiện <b>"Không hoàn thành"</b>, không phải
     * "Chưa chạy tới".
     * <p>
     * Trước đây {@code StepOutcome.record(..., false)} không có điểm gọi nào trong mã sản phẩm —
     * chỉ có nhánh {@code true}. Nên bước làm hỏng kịch bản không hề vào {@code StepOutcome}, và
     * bảng tóm tắt đóng dấu nó là "Chưa chạy tới": đảo chiều của chính kiểu báo cáo suy đoán mà
     * bảng này sinh ra để diệt — thay vì tô xanh nhầm thì giấu nhầm bước lỗi.
     */
    @Test(groups = {"unit"})
    public void buocDangChayDoBiGhiLaKhongHoanThanh() throws Exception {
        BaoCaoData.xoaHet();
        StepOutcome.beginCase();
        BaoCao.createTest("TC_HONG", "—");
        StepOutcome.record(1, "Chọn loại đơn", 1000, true);
        BaoCaoData.moBuoc(2, "Điền thông tin nguyên đơn");
        Thread.sleep(20);

        BaoCao.ghiBuocDangMoLaHong();
        BaoCao.logStepSummary();
        BaoCaoData.ketThucCase(TrangThai.THAT_BAI, 5000);
        StepOutcome.clear();

        List<BaoCaoData.TomTatBuoc> tt = BaoCaoData.cases().get(0).tomTatBuocAnToan();
        assertEquals(tt.get(0).ketQua(), TrangThai.DAT);
        assertEquals(tt.get(1).ketQua(), TrangThai.KHONG_HOAN_THANH,
                "Bước làm hỏng kịch bản bị đóng dấu sai");
        assertTrue(tt.get(1).thoiGianMs() >= 20, "Bước dở dang mất thời gian thật");
        assertEquals(tt.get(2).ketQua(), TrangThai.CHUA_CHAY_TOI, "Bước 3 thật sự chưa chạy tới");
    }

    /**
     * Kịch bản hỏng ngay ở bước 1 — chưa bước nào hoàn thành — vẫn phải có bảng 6 bước.
     * Bản cũ thoát sớm khi {@code StepOutcome} rỗng, tức giấu bảng đúng lúc cần nhất.
     */
    @Test(groups = {"unit"})
    public void hongNgayBuocDauVanCoBangSauBuoc() {
        BaoCaoData.xoaHet();
        StepOutcome.beginCase();
        BaoCao.createTest("TC_HONG_SOM", "—");
        BaoCao.logStepSummary();
        BaoCaoData.ketThucCase(TrangThai.THAT_BAI, 100);
        StepOutcome.clear();

        assertEquals(BaoCaoData.cases().get(0).tomTatBuocAnToan().size(), 6,
                "Mất bảng 6 bước khi kịch bản hỏng trước khi hoàn thành bước nào");
    }

    /**
     * {@code clearTestContext} phải xoá mã case, nếu không kịch bản bị bỏ qua mà không đi qua
     * {@code onTestStart} sẽ nhặt mã của kịch bản trước trên cùng thread — hai mục cùng mã, hai
     * thẻ HTML trùng id, và ảnh nằm ở thư mục mang mã khác.
     */
    @Test(groups = {"unit"})
    public void maCaseKhongRoSangKichBanKeTiep() {
        BaoCaoData.xoaHet();
        BaoCao.setCaseCode("TC_MID_006");
        BaoCao.createTest("Kịch bản 6", "—");
        BaoCaoData.ketThucCase(TrangThai.DAT, 10);
        BaoCao.clearTestContext();

        // Kịch bản kế tiếp bị bỏ qua, KHÔNG ai gọi setCaseCode.
        BaoCao.createTest("Kịch bản 7 bị bỏ qua", "—");
        BaoCaoData.ketThucCase(TrangThai.BO_QUA, 0);

        List<BaoCaoData.CaseBaoCao> ds = BaoCaoData.cases();
        assertEquals(ds.size(), 2);
        assertFalse("TC_MID_006".equals(ds.get(0).maCase()) && "TC_MID_006".equals(ds.get(1).maCase()),
                "Hai kịch bản mang cùng một mã — mã case đã rò rỉ qua thread");
    }

    /** Ba trường kết luận từng là đặc sản của Excel phải theo được vào dữ liệu kịch bản. */
    @Test(groups = {"unit"})
    public void baTruongKetLuanVaoDuocBaoCao() {
        BaoCaoData.xoaHet();
        BaoCao.createTest("TC_TEE_KL", "—");
        BaoCao.ketQuaMongDoi("Nộp trọn đơn qua 6 bước.");
        BaoCao.ketQuaThucTe("Hệ thống báo lỗi sau Gửi đơn.");
        BaoCao.ghiChuKetQua("Lỗi phía Quản Lý Án, không phải script.");
        BaoCaoData.ketThucCase(TrangThai.THAT_BAI, 1000);

        BaoCaoData.CaseBaoCao c = BaoCaoData.cases().get(0);
        assertEquals(c.ketQuaMongDoi(), "Nộp trọn đơn qua 6 bước.");
        assertEquals(c.ketQuaThucTe(), "Hệ thống báo lỗi sau Gửi đơn.");
        assertEquals(c.ghiChuKetQua(), "Lỗi phía Quản Lý Án, không phải script.");
        assertEquals(c.trangThai(), TrangThai.THAT_BAI);
    }
}

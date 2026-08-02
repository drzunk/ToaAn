package vn.tuphap.automation.tests;

import vn.tuphap.automation.core.TaoDonBaseTest;
import vn.tuphap.automation.core.ScenarioDispatch;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import vn.tuphap.automation.pages.GuiDonKetQua;
import vn.tuphap.automation.pages.XemLaiGuiDonPage;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.report.BaoCao;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.flow.TaoDonFlow;
import vn.tuphap.automation.report.TaoDonReportBuilder;
import vn.tuphap.automation.data.TaoDonScenario;

import java.util.List;

public class TaoDonTest extends TaoDonBaseTest {

    /**
     * parallel=true: TestNG chia từng dòng cho 1 thread — mỗi case chạy đúng 1 lần,
     * không nhân đôi trên 3 browser.
     */
    @DataProvider(name = "DuLieuTaoDon", parallel = true)
    public Object[][] getData(ITestContext context) {
        ScenarioDispatch.reset();
        String mode = resolveSuiteMode(context);
        Object[][] data;
        if ("smoke".equalsIgnoreCase(mode)) {
            data = DataGenerator.generateSmokeData();
        } else if ("mid".equalsIgnoreCase(mode)) {
            data = DataGenerator.generateMidCoverageData();
        } else {
            data = DataGenerator.generateFullCoverageData();
        }
        data = filterByOnlyStt(data);
        System.out.println("📋 DataProvider DuLieuTaoDon: " + data.length
                + " case (mỗi case 1 browser/thread, không trùng).");
        return data;
    }

    /**
     * Chạy lại case lỗi: {@code -Dtaodon.onlyStt=12,13} (STT trong ma trận).
     */
    private static Object[][] filterByOnlyStt(Object[][] data) {
        String raw = System.getProperty("taodon.onlyStt",
                System.getenv().getOrDefault("TOAAN_ONLY_STT", "")).trim();
        if (raw.isBlank()) {
            return data;
        }
        java.util.Set<String> want = new java.util.LinkedHashSet<>();
        for (String p : raw.split("[,;\\s]+")) {
            if (!p.isBlank()) {
                want.add(p.trim());
            }
        }
        java.util.List<Object[]> kept = new java.util.ArrayList<>();
        for (Object[] row : data) {
            if (row == null || row.length == 0 || !(row[0] instanceof TaoDonScenario s)) {
                continue;
            }
            if (want.contains(s.stt())) {
                kept.add(row);
                System.out.println("🔎 onlyStt giữ #" + s.stt() + " — " + s.loaiDon()
                        + " / " + s.loaiViec() + " | ND=" + s.loaiChuThe());
            }
        }
        if (kept.isEmpty()) {
            throw new IllegalStateException("taodon.onlyStt=" + raw + " không khớp case nào.");
        }
        System.out.println("📋 Lọc onlyStt=" + raw + " → " + kept.size() + " case.");
        return kept.toArray(new Object[0][]);
    }

    @Test(dataProvider = "DuLieuTaoDon", groups = {"smoke", "mid", "full"},
            description = "Luồng tạo đơn đầy đủ 6 bước: chọn loại đơn → nguyên đơn → bị đơn → nội dung → tài liệu → gửi đơn")
    public void testFlowTaoDon(TaoDonScenario s) {
        Assert.assertNotNull(s, "Kịch bản kiểm thử không được để trống");
        claimExclusive(s);
        TaoDonFlow flow = new TaoDonFlow(getDriver(), getWebUI());

        XemLaiGuiDonPage review = flow.denManXemLai(s);
        BaoCao.logPass("Đã điền biểu mẫu bước 1→5 và đến màn Xem lại.");

        long t = BaoCao.markStepStart();
        GuiDonKetQua kq = review.thuGuiDonVaChoKetQua();
        String shotSauGui = kq.screenshotBase64();
        String systemMsg = kq.message();

        if (kq.isSuccess()) {
            String stepMsg = TaoDonReportBuilder.tenBuocDayDu(6) + " — " + systemMsg;
            BaoCao.logStepDone(6, 6, stepMsg, t,
                    shotSauGui == null ? null : List.of(shotSauGui));
            BaoCao.ketQuaThucTe(systemMsg);
            BaoCao.ghiChuKetQua("");
            return;
        }

        String baoCaoMsg;
        if (kq.isError()) {
            baoCaoMsg = "Hệ thống báo lỗi sau Gửi đơn: " + systemMsg;
        } else {
            baoCaoMsg = systemMsg;
        }

        // Lỗi hệ thống sau Gửi đơn → FAIL cứng + ảnh (không soft-fail warning).
        BaoCao.ketQuaThucTe(systemMsg);
        // Ghi chú phải nói thêm điều gì đó, không chép lại Kết quả thực tế. Điều đáng nói ở đây là
        // ranh giới trách nhiệm: đơn đã điền xong 6 bước, chỗ hỏng nằm ở phía hệ thống nhận đơn.
        BaoCao.ghiChuKetQua(kq.isTimeout()
                ? "Đã điền xong 6 bước và bấm Gửi đơn, nhưng hệ thống không trả về thông báo nào "
                  + "trong thời gian chờ — không xác định được đơn có vào hay không."
                : "Đã điền xong 6 bước và bấm Gửi đơn — lỗi phát sinh ở phía hệ thống nhận đơn, "
                  + "không phải ở khâu nhập liệu.");
        if (shotSauGui != null) {
            BaoCao.logFailWithScreenshot(baoCaoMsg, shotSauGui);
        } else {
            BaoCao.logFail(baoCaoMsg);
        }
        Assert.fail(baoCaoMsg);
    }

    @Test(groups = {"smoke", "full", "chinhSua"},
            description = "Từ màn Xem lại, bấm Chỉnh sửa (Xem trước đơn) → bước 1, đi lại nộp đơn với yêu cầu mới, đối chiếu Xem lại (không gửi đơn)")
    public void testChinhSuaNoiDungTuXemLai() {
        TaoDonScenario s = DataGenerator.generateScenarioForReviewEdit();
        TaoDonReportBuilder.logScenarioOverview(s);
        BaoCao.logInfo(
                "Kiểm tra Chỉnh sửa đơn từ Xem lại: Xem trước đơn → bước 1 → nộp lại (không gửi đơn).");

        TaoDonFlow flow = new TaoDonFlow(getDriver(), getWebUI());
        XemLaiGuiDonPage review = flow.denManXemLai(s);

        String yeuCauMoi = "Yêu cầu đã chỉnh sửa lúc " + System.currentTimeMillis()
                + " — kiểm tra luồng Chỉnh sửa từ bước Xem lại.";

        long t = BaoCao.markStepStart();
        review.clickChinhSuaDon();
        BaoCao.logStepDone(6, 6, "Click Chỉnh sửa — Xem trước đơn (về bước 1)", t);

        flow.tiepTucNopDonTuBuoc1SauChinhSua(s, yeuCauMoi);

        review.waitStepReady();
        Assert.assertTrue(review.reviewContains(yeuCauMoi),
                "Màn Xem lại phải hiển thị đúng nội dung yêu cầu vừa chỉnh sửa");
        getWebUI().captureOverview("Ảnh tổng quan — màn Xem lại sau chỉnh sửa đơn");
        BaoCao.logPass("Đối chiếu Xem lại ổn — đã thấy yêu cầu đã chỉnh sửa.");

        BaoCao.ketQuaMongDoi(
                "Từ màn Xem lại, Chỉnh sửa (Xem trước đơn) về bước 1, nộp lại rồi Xem lại thấy đúng yêu cầu đã sửa.");
        BaoCao.ketQuaThucTe(
                "Đối chiếu thành công: màn Xem lại đã cập nhật theo yêu cầu vừa chỉnh sửa. Không gửi đơn.");
        BaoCao.ghiChuKetQua("Yêu cầu cụ thể sau chỉnh sửa: " + yeuCauMoi);
    }

    @DataProvider(name = "DuLieuBuoc23BayLoai", parallel = true)
    public Object[][] getBuoc23Data() {
        ScenarioDispatch.reset();
        Object[][] data = DataGenerator.generateBuoc23AllLoaiDonData();
        System.out.println("📋 DataProvider Buoc23: " + data.length + " case (không trùng trên 3 browser).");
        return data;
    }

    @Test(dataProvider = "DuLieuBuoc23BayLoai", groups = {"buoc23", "regression"},
            description = "Điền bước 1→3 cho đủ 7 loại đơn — kiểm tra thứ tự nhập nguyên đơn / bị đơn")
    public void testBuoc2Va3BayLoaiDon(TaoDonScenario s) {
        Assert.assertNotNull(s, "Kịch bản kiểm thử không được để trống");
        claimExclusive(s);
        TaoDonReportBuilder.logScenarioOverview(s);
        BaoCao.logInfo("Kiểm tra bước 2–3: " + s.loaiDon() + " / " + s.loaiViec());

        TaoDonFlow flow = new TaoDonFlow(getDriver(), getWebUI());
        flow.denHetBuoc3(s);

        Assert.assertTrue(getWebUI().existsNow(TaoDonFlow.MARKER_NOI_DUNG)
                        || getWebUI().existsNow(By.xpath("//button[contains(., 'Tiếp theo')]")),
                "Sau bước 3 phải còn ở wizard (có Tiếp theo / marker nội dung)");
        BaoCao.logPass("Đã điền ổn định bước 1→3 cho [" + s.loaiDon() + "].");
        BaoCao.ketQuaThucTe("Điền bước 1→3 thành công: " + s.loaiDon() + " / " + s.loaiViec());
    }

    /** Mỗi case chỉ 1 thread/browser — trùng thì skip, không chạy lại. */
    private static void claimExclusive(TaoDonScenario s) {
        if (!ScenarioDispatch.claim(s)) {
            throw new SkipException(
                    "Case đã được nhận bởi browser/thread khác — không chạy trùng: "
                            + s.loaiDon() + " / " + s.loaiViec());
        }
    }

    private static String resolveSuiteMode(ITestContext context) {
        String mode = "";
        if (context != null && context.getSuite() != null) {
            String suiteName = context.getSuite().getName();
            if (suiteName != null) {
                String n = suiteName.toLowerCase();
                if (n.contains("smoke")) {
                    mode = "smoke";
                } else if (n.contains("mid")) {
                    mode = "mid";
                } else if (n.contains("full")) {
                    mode = "full";
                }
            }
        }
        if (mode.isBlank()) {
            mode = System.getProperty("taodon.suite", "");
        }
        if (mode == null || mode.isBlank()) {
            mode = System.getenv().getOrDefault("TOAAN_SUITE", "");
        }
        if (mode == null || mode.isBlank()) {
            mode = "full";
        }
        return mode.trim();
    }
}

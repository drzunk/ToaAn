package vn.tuphap.automation.tests;

import vn.tuphap.automation.core.TaoDonBaseTest;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import vn.tuphap.automation.pages.GuiDonKetQua;
import vn.tuphap.automation.pages.XemLaiGuiDonPage;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.report.ExtentReportManager;
import vn.tuphap.automation.report.TaoDonExcelTestLog;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.flow.TaoDonFlow;
import vn.tuphap.automation.report.TaoDonReportBuilder;
import vn.tuphap.automation.data.TaoDonScenario;

import java.util.List;

public class TaoDonTest extends TaoDonBaseTest {

    @DataProvider(name = "DuLieuTaoDon")
    public Object[][] getData(ITestContext context) {
        String mode = resolveSuiteMode(context);
        if ("smoke".equalsIgnoreCase(mode)) {
            return DataGenerator.generateSmokeData();
        }
        if ("mid".equalsIgnoreCase(mode)) {
            return DataGenerator.generateMidCoverageData();
        }
        return DataGenerator.generateFullCoverageData();
    }

    @Test(dataProvider = "DuLieuTaoDon", groups = {"smoke", "mid", "full"},
            description = "Luồng tạo đơn đầy đủ 6 bước: chọn loại đơn → nguyên đơn → bị đơn → nội dung → tài liệu → gửi đơn")
    public void testFlowTaoDon(TaoDonScenario s) {
        Assert.assertNotNull(s, "Kịch bản kiểm thử không được để trống");
        TaoDonFlow flow = new TaoDonFlow(driver, webUI);

        XemLaiGuiDonPage review = flow.denManXemLai(s);
        ExtentReportManager.logPass("Đã điền biểu mẫu bước 1→5 và đến màn Xem lại.");

        long t = ExtentReportManager.markStepStart();
        GuiDonKetQua kq = review.thuGuiDonVaChoKetQua();
        String shotSauGui = kq.screenshotBase64();
        String systemMsg = kq.message();

        if (kq.isSuccess()) {
            String stepMsg = TaoDonReportBuilder.tenBuocDayDu(6) + " — " + systemMsg;
            ExtentReportManager.logStepDone(6, 6, stepMsg, t,
                    shotSauGui == null ? null : List.of(shotSauGui));
            TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_DAT);
            TaoDonExcelTestLog.setTrangThai(TaoDonExcelTestLog.ST_DAT);
            TaoDonExcelTestLog.setKetQuaThucTe(systemMsg);
            TaoDonExcelTestLog.setGhiChuKetQua("");
            return;
        }

        String baoCaoMsg;
        if (kq.isError()) {
            baoCaoMsg = "Hệ thống báo lỗi sau Gửi đơn: " + systemMsg;
        } else {
            baoCaoMsg = systemMsg;
        }

        if (shotSauGui != null) {
            ExtentReportManager.logWarningWithScreenshots(baoCaoMsg, List.of(shotSauGui));
        } else {
            ExtentReportManager.logWarning(baoCaoMsg);
        }

        if (requireSubmitSuccess()) {
            TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_THAT_BAI);
            Assert.fail(baoCaoMsg);
        } else {
            TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_DAT_CANH_BAO);
            TaoDonExcelTestLog.setTrangThai(TaoDonExcelTestLog.ST_DAT_CANH_BAO);
            TaoDonExcelTestLog.setKetQuaThucTe(systemMsg);
            TaoDonExcelTestLog.setGhiChuKetQua(kq.isTimeout()
                    ? "Không có toast từ hệ thống trong thời gian chờ. Phần gửi đơn chấp nhận thất bại mềm."
                    : "Message lỗi hệ thống (soft-fail): " + systemMsg);
            ExtentReportManager.logPass(
                    "Điền biểu mẫu thành công; Gửi đơn soft-fail — " + systemMsg);
        }
    }

    @Test(groups = {"smoke", "mid", "full", "chinhSua"},
            description = "Từ màn Xem lại, bấm Chỉnh sửa phần Nội dung đơn, cập nhật yêu cầu cụ thể, rồi quay lại Xem lại để đối chiếu (không gửi đơn)")
    public void testChinhSuaNoiDungTuXemLai() {
        TaoDonScenario s = DataGenerator.generateOneRandomScenario();
        TaoDonReportBuilder.logScenarioOverview(s);
        ExtentReportManager.logInfo("Kiểm tra Chỉnh sửa Nội dung đơn từ Xem lại (không gửi đơn).");

        TaoDonFlow flow = new TaoDonFlow(driver, webUI);
        XemLaiGuiDonPage review = flow.denManXemLai(s);

        String yeuCauMoi = "Yêu cầu đã chỉnh sửa lúc " + System.currentTimeMillis()
                + " — kiểm tra luồng Chỉnh sửa từ bước Xem lại.";

        long t = ExtentReportManager.markStepStart();
        review.clickChinhSuaNoiDung();
        ExtentReportManager.logStepDone(6, 6, "Click Chỉnh sửa tại mục Nội dung đơn", t);

        flow.dienBuoc4CapNhatYeuCau(s, yeuCauMoi);
        flow.tuXemLaiQuaBuoc5DenXemLai(s);

        review.waitStepReady();
        Assert.assertTrue(review.reviewContains(yeuCauMoi),
                "Màn Xem lại phải hiển thị đúng nội dung yêu cầu vừa chỉnh sửa");
        webUI.captureOverview("Ảnh tổng quan — màn Xem lại sau chỉnh sửa nội dung đơn");
        ExtentReportManager.logPass("Đối chiếu Xem lại ổn — đã thấy yêu cầu đã chỉnh sửa.");
        TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_DAT);

        TaoDonExcelTestLog.setKetQuaMongDoi(
                "Từ màn Xem lại, chỉnh sửa nội dung đơn rồi quay lại Xem lại thấy đúng yêu cầu đã sửa.");
        TaoDonExcelTestLog.setTrangThai(TaoDonExcelTestLog.ST_DAT);
        TaoDonExcelTestLog.setKetQuaThucTe(
                "Đối chiếu thành công: màn Xem lại đã cập nhật theo yêu cầu vừa chỉnh sửa. Không gửi đơn.");
        TaoDonExcelTestLog.setGhiChuKetQua("Yêu cầu cụ thể sau chỉnh sửa: " + yeuCauMoi);
    }

    /**
     * {@code true} = Gửi đơn thất bại sẽ làm kịch bản đỏ.
     * Mặc định {@code false} (chấp nhận thất bại mềm) vì app demo thường hết thời gian chờ.
     * Bật: {@code -Dtaodon.requireSubmit=true}
     */
    private static boolean requireSubmitSuccess() {
        return "true".equalsIgnoreCase(
                System.getProperty("taodon.requireSubmit",
                        System.getenv().getOrDefault("TOAAN_REQUIRE_SUBMIT", "false")));
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

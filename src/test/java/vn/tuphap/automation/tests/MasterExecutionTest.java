package vn.tuphap.automation.tests;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import vn.tuphap.automation.config.RunFlowConfig;
import vn.tuphap.automation.core.ScenarioDispatch;
import vn.tuphap.automation.core.TaoDonBaseTest;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.TaoDonScenario;
import vn.tuphap.automation.flow.TaoDonFlow;
import vn.tuphap.automation.pages.GuiDonKetQua;
import vn.tuphap.automation.pages.XemLaiGuiDonPage;
import vn.tuphap.automation.report.ExtentReportManager;
import vn.tuphap.automation.report.TaoDonExcelTestLog;
import vn.tuphap.automation.report.TaoDonReportBuilder;
import vn.tuphap.automation.report.TestActionLog;

import java.util.List;

/**
 * Điểm vào trung tâm — độ sâu bước điều khiển bởi {@code run-flow.properties}
 * ({@code run.untilStep}, {@code run.submit}). Menu dễ: {@code .\\scripts\\chay.ps1}.
 * <p>
 * Không gọi Selenium trực tiếp; chỉ gọi {@link TaoDonFlow}.
 */
public class MasterExecutionTest extends TaoDonBaseTest {

    @DataProvider(name = "DuLieuMaster", parallel = true)
    public Object[][] getData(ITestContext context) {
        ScenarioDispatch.reset();
        if (RunFlowConfig.maxUntilStep() <= 0) {
            System.out.println(" untilStep=login — không cần DataProvider scenario.");
            return new Object[][]{ { null } };
        }
        if (RunFlowConfig.hasCases()) {
            Object[][] custom = DataGenerator.generateConfiguredCases(RunFlowConfig.cases());
            System.out.println(" MasterExecution: " + custom.length + " case từ run.cases | Chrome="
                    + RunFlowConfig.browsers() + " | maxUntilStep="
                    + RunFlowConfig.maxUntilStep());
            return custom;
        }
        String mode = resolveSuiteMode(context);
        Object[][] data;
        if ("smoke".equalsIgnoreCase(mode) || "master".equalsIgnoreCase(mode) || mode.isBlank()) {
            data = DataGenerator.generateSmokeData();
        } else if ("mid".equalsIgnoreCase(mode)) {
            data = DataGenerator.generateMidCoverageData();
        } else {
            data = DataGenerator.generateFullCoverageData();
        }
        int limit = RunFlowConfig.browsers();
        if (RunFlowConfig.hasSlots() && data.length > RunFlowConfig.slotCount()) {
            limit = RunFlowConfig.slotCount();
        }
        if (data.length > limit && limit > 0) {
            Object[][] limited = new Object[limit][];
            System.arraycopy(data, 0, limited, 0, limit);
            data = limited;
            System.out.println(" Giới hạn " + data.length + " case (= số Chrome).");
        }
        System.out.println(" MasterExecution DataProvider: " + data.length + " case | maxUntilStep="
                + RunFlowConfig.maxUntilStep()
                + " | submit(chung)=" + RunFlowConfig.bool("run.submit", false));
        return data;
    }

    @Test(dataProvider = "DuLieuMaster", groups = {"master", "smoke", "mid", "full"},
            description = "Master fall-through theo run.untilStep / run.submit")
    public void testMasterExecution(TaoDonScenario s) {
        int until;
        try {
            bindCaseDepthFromScenario(s);
            until = RunFlowConfig.untilStep();
            if (until <= 0) {
                TestActionLog.ghiChu("Chạy an toàn: chỉ đăng nhập (run.untilStep=login).");
                ExtentReportManager.logPass("Session đăng nhập sẵn sàng — dừng theo run.untilStep.");
                return;
            }

            Assert.assertNotNull(s, "Kịch bản không được null khi untilStep >= 1");
            claimExclusive(s);

            TaoDonFlow flow = new TaoDonFlow(getDriver(), getWebUI());
            XemLaiGuiDonPage review;
            try {
                review = flow.chayTheoMasterConfig(s);
            } catch (RuntimeException ex) {
                TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_THAT_BAI);
                ExtentReportManager.logFail(ex.getMessage());
                throw ex;
            }

            if (until < 6) {
                ExtentReportManager.logPass("Đã chạy đến bước " + until + " rồi dừng (run.submit bỏ qua).");
                TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_DAT);
                return;
            }

            Assert.assertNotNull(review, "untilStep=6 phải đến màn Xem lại");
            ExtentReportManager.logPass("Đã điền bước 1→5 và đến màn Xem lại.");

            if (!RunFlowConfig.submit()) {
                TestActionLog.ghiChu("Chạy an toàn, bỏ qua gửi đơn");
                ExtentReportManager.logPass("Dừng an toàn tại Xem lại (run.submit=false).");
                TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_DAT);
                return;
            }

            TestActionLog.ghiChu("run.submit=true — gọi Gửi đơn.");
            long t = ExtentReportManager.markStepStart();
            GuiDonKetQua kq = review.thuGuiDonVaChoKetQua();
            String shot = kq.screenshotBase64();
            String msg = kq.message();

            if (kq.isSuccess()) {
                ExtentReportManager.logStepDone(6, 6, TaoDonReportBuilder.tenBuocDayDu(6) + " — " + msg, t,
                        shot == null ? null : List.of(shot));
                TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_DAT);
                TaoDonExcelTestLog.setTrangThai(TaoDonExcelTestLog.ST_DAT);
                TaoDonExcelTestLog.setKetQuaThucTe(msg);
                return;
            }

            String failMsg = kq.isError()
                    ? "Hệ thống báo lỗi sau Gửi đơn: " + msg
                    : msg;
            TestActionLog.trangThaiBuoc(TaoDonExcelTestLog.ST_THAT_BAI);
            TaoDonExcelTestLog.setTrangThai(TaoDonExcelTestLog.ST_THAT_BAI);
            TaoDonExcelTestLog.setKetQuaThucTe(msg);
            if (shot != null) {
                ExtentReportManager.logFailWithScreenshot(failMsg, shot);
            } else {
                ExtentReportManager.logFail(failMsg);
            }
            Assert.fail(failMsg);
        } finally {
            RunFlowConfig.clearBoundCase();
        }
    }

    /** Gắn until/submit theo đúng case menu (khi số case > số Chrome). */
    private static void bindCaseDepthFromScenario(TaoDonScenario s) {
        if (!RunFlowConfig.hasCases() || s == null || s.stt() == null) {
            return;
        }
        try {
            int idx = Integer.parseInt(s.stt().trim()) - 1;
            List<RunFlowConfig.CaseProfile> list = RunFlowConfig.cases();
            if (idx >= 0 && idx < list.size()) {
                RunFlowConfig.bindCaseProfile(list.get(idx));
            }
        } catch (NumberFormatException ignored) {
            // smoke/mid không dùng STT số → giữ untilStep chung
        }
    }

    private void claimExclusive(TaoDonScenario s) {
        if (!ScenarioDispatch.claim(s)) {
            throw new org.testng.SkipException(
                    "Case đã được nhận bởi trình duyệt khác — không chạy trùng: "
                            + ScenarioDispatch.keyOf(s));
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
            mode = System.getProperty("taodon.suite", RunFlowConfig.suite());
        }
        if (mode == null || mode.isBlank()) {
            mode = "smoke";
        }
        return mode.trim();
    }
}

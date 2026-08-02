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
import vn.tuphap.automation.flow.StepBlockedException;
import vn.tuphap.automation.flow.TaoDonFlow;
import vn.tuphap.automation.pages.GuiDonKetQua;
import vn.tuphap.automation.pages.XemLaiGuiDonPage;
import vn.tuphap.automation.report.BaoCao;
import vn.tuphap.automation.report.TaoDonReportBuilder;
import vn.tuphap.automation.report.TestActionLog;

import java.util.List;
import java.util.Locale;

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
            return new Object[][]{ { null, null } };
        }
        if (RunFlowConfig.hasCases()) {
            List<RunFlowConfig.CaseProfile> profiles = RunFlowConfig.cases();
            // Đã gồm sẵn 2 cột [scenario, CaseProfile] — ghép tại DataGenerator để dòng lỗi bị bỏ
            // không làm lệch cặp (xem javadoc generateConfiguredCases).
            Object[][] custom = DataGenerator.generateConfiguredCases(profiles);
            System.out.println(" MasterExecution: " + custom.length + " case từ "
                    + RunFlowConfig.caseSourceLabel() + " | Chrome="
                    + RunFlowConfig.browsers() + " | maxUntilStep="
                    + RunFlowConfig.maxUntilStep());
            return custom;
        }
        if (RunFlowConfig.useSheet()) {
            // Sheet là nguồn case đang bật nhưng không lấy được dòng nào → dừng rõ ràng,
            // tránh âm thầm chạy bộ smoke mặc định trong khi tester tưởng đang chạy theo sheet.
            throw new IllegalStateException(
                    "Không có case nào để chạy. Nguồn: " + RunFlowConfig.caseSourceLabel()
                            + " (" + RunFlowConfig.casesSheetUrl() + ")."
                            + " Kiểm tra: sheet có dòng dữ liệu chưa, dòng đầu có đúng tên cột"
                            + " (Loại đơn, Loại việc, ...) chưa, cột \"Chạy\" đã tick x chưa,"
                            + " và sheet đã chia sẻ \"Bất kỳ ai có đường liên kết — Người xem\" chưa."
                            + " Muốn chạy bộ mẫu thì đặt run.caseSource=file trong run-flow.properties.");
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
        return widenWithNullCaseProfile(data);
    }

    /**
     * Bộ smoke/mid/full không đi từ sheet nên không có {@link RunFlowConfig.CaseProfile} —
     * thêm cột thứ 2 = {@code null} cho khớp chữ ký {@link #testMasterExecution}
     * (case thường, không phải ca âm).
     */
    private static Object[][] widenWithNullCaseProfile(Object[][] data) {
        Object[][] out = new Object[data.length][2];
        for (int i = 0; i < data.length; i++) {
            out[i][0] = data[i][0];
            out[i][1] = null;
        }
        return out;
    }

    @Test(dataProvider = "DuLieuMaster", groups = {"master", "smoke", "mid", "full"},
            description = "Master fall-through theo run.untilStep / run.submit")
    public void testMasterExecution(TaoDonScenario s, RunFlowConfig.CaseProfile caseProfile) {
        int until;
        try {
            if (caseProfile != null) {
                RunFlowConfig.bindCaseProfile(caseProfile);
            }
            until = RunFlowConfig.untilStep();
            if (until <= 0) {
                TestActionLog.ghiChu("Chạy an toàn: chỉ đăng nhập (run.untilStep=login).");
                BaoCao.logPass("Session đăng nhập sẵn sàng — dừng theo run.untilStep.");
                return;
            }

            Assert.assertNotNull(s, "Kịch bản không được null khi untilStep >= 1");
            claimExclusive(s);

            boolean isNegative = caseProfile != null && caseProfile.hasNegativeExpectation();
            TaoDonFlow flow = new TaoDonFlow(getDriver(), getWebUI());
            XemLaiGuiDonPage review;
            try {
                review = flow.chayTheoMasterConfig(s);
                if (isNegative) {
                    // Ca âm nhưng đi hết flow mà không bị chặn — nghĩa là hệ thống có lỗ hổng
                    // validation (chấp nhận dữ liệu lẽ ra phải bị từ chối).
                    String msg = "Kỳ vọng hệ thống chặn (" + caseProfile.truongLoi() + " = \""
                            + caseProfile.giaTriLoi() + "\") nhưng KHÔNG bị chặn — có thể là lỗ hổng validation.";
                    BaoCao.ketQuaThucTe(msg);
                    BaoCao.logFail(msg);
                    Assert.fail(msg);
                }
            } catch (StepBlockedException ex) {
                if (isNegative) {
                    ghiNhanChanDungKyVong(ex, caseProfile);
                    return;
                }
                BaoCao.logFail(ex.getMessage());
                throw ex;
            } catch (RuntimeException ex) {
                BaoCao.logFail(ex.getMessage());
                throw ex;
            }

            if (until < 6) {
                BaoCao.logPass("Đã chạy đến bước " + until + " rồi dừng (run.submit bỏ qua).");
                return;
            }

            Assert.assertNotNull(review, "untilStep=6 phải đến màn Xem lại");
            BaoCao.logPass("Đã điền bước 1→5 và đến màn Xem lại.");

            if (!RunFlowConfig.submit()) {
                TestActionLog.ghiChu("Chạy an toàn, bỏ qua gửi đơn");
                BaoCao.logPass("Dừng an toàn tại Xem lại (run.submit=false).");
                return;
            }

            TestActionLog.ghiChu("run.submit=true — gọi Gửi đơn.");
            long t = BaoCao.markStepStart();
            GuiDonKetQua kq = review.thuGuiDonVaChoKetQua();
            String shot = kq.screenshotBase64();
            String msg = kq.message();

            if (kq.isSuccess()) {
                BaoCao.logStepDone(6, 6, TaoDonReportBuilder.tenBuocDayDu(6) + " — " + msg, t,
                        shot == null ? null : List.of(shot));
                BaoCao.ketQuaThucTe(msg);
                return;
            }

            String failMsg = kq.isError()
                    ? "Hệ thống báo lỗi sau Gửi đơn: " + msg
                    : msg;
            BaoCao.ketQuaThucTe(msg);
            if (shot != null) {
                BaoCao.logFailWithScreenshot(failMsg, shot);
            } else {
                BaoCao.logFail(failMsg);
            }
            Assert.fail(failMsg);
        } finally {
            RunFlowConfig.clearBoundCase();
        }
    }

    /**
     * Ca âm bị chặn — so thông báo hệ thống với {@code caseProfile.thongBaoMongDoi()}.
     * Trống = chấp nhận mọi thông báo chặn; có giá trị = phải chứa chuỗi đó (không phân biệt hoa thường).
     */
    private static void ghiNhanChanDungKyVong(StepBlockedException ex, RunFlowConfig.CaseProfile caseProfile) {
        String expected = caseProfile.thongBaoMongDoi();
        String actual = ex.systemMessage() == null ? "" : ex.systemMessage();
        boolean matched = expected == null || expected.isBlank()
                || actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));

        if (matched) {
            String note = "Hệ thống chặn đúng như kỳ vọng ở " + ex.stepName() + ": " + actual;
            BaoCao.logPass(note);
            BaoCao.ketQuaThucTe(note);
            return;
        }

        String msg = "Hệ thống có chặn nhưng thông báo khác kỳ vọng.\n"
                + "  Kỳ vọng chứa : " + expected + "\n"
                + "  Thực tế      : " + actual;
        BaoCao.ketQuaThucTe(msg);
        BaoCao.logFail(msg);
        Assert.fail(msg);
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

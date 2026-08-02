package vn.tuphap.automation.report;

import vn.tuphap.automation.data.TaoDonScenario;

import vn.tuphap.automation.config.ConfigReader;

import vn.tuphap.automation.core.BaseTest;
import vn.tuphap.automation.flow.StepBlockedException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import vn.tuphap.automation.ui.WebUI;

public class TestListener implements ITestListener {

    /** Mốc bắt đầu suite — thời gian THỰC TẾ, không phải tổng cộng dồn của các case. */
    private static volatile long batDauSuiteMs;

    @Override
    public void onStart(ITestContext context) {
        String suiteName = context.getSuite().getName();
        // Đặt mốc lượt chạy TRƯỚC mọi thứ: ảnh và dữ liệu báo cáo đều dùng chung mốc này nên kết
        // xuất của một lượt nằm gọn trong một thư mục thay vì rải theo giờ tạo từng file.
        batDauSuiteMs = System.currentTimeMillis();
        ScreenshotStore.initRun();
        vn.tuphap.automation.config.RunFlowConfig.applyKnownSystemAliases();
        vn.tuphap.automation.config.RunFlowConfig.printSummary(suiteName);
        vn.tuphap.automation.core.ScenarioDispatch.reset();
        SuiteKind.datHienTai(suiteName);
        // Dọn danh sách kịch bản của lượt TRƯỚC. DA_XONG là static, sống suốt vòng đời JVM — chạy
        // hai lượt trong cùng JVM (re-run trong IDE, surefire reuseForks) thì danh sách cộng dồn
        // trong khi bộ đếm passed/failed lấy từ TestNG chỉ của lượt mới: đầu trang và danh sách
        // bên dưới nói hai sự thật khác nhau, không chỗ nào đối chiếu.
        BaoCaoData.xoaHet();
        if (vn.tuphap.automation.config.RunFlowConfig.parallel()
                || (suiteName != null && suiteName.toLowerCase().contains("parallel"))) {
            System.setProperty("taodon.parallel", "true");
            System.setProperty("taodon.threads",
                    String.valueOf(vn.tuphap.automation.config.RunFlowConfig.browsers()));
        }
    }

    @Override
    public void onTestStart(ITestResult result) {
        TestActionLog.beginTest();
        StepOutcome.beginCase();
        TaoDonScenario scenario = TaoDonReportBuilder.asScenario(result.getParameters());
        String testName;
        String description;
        String category = "Kiểm thử chung";

        if (scenario != null) {
            testName = TaoDonReportBuilder.buildTestTitle(scenario);
            description = TaoDonReportBuilder.buildTestDescription(scenario);
            String maCase = SuiteKind.maCase(scenario);
            ScreenshotStore.beginCase(maCase);
            BaoCao.setCaseCode(maCase);
            // Gắn cả loại đơn lẫn loại việc: trước đây chỉ có loại đơn, mà getLoaiViecCategory()
            // vẫn nằm đó không ai gọi — lọc theo loại việc là thứ tester hay cần nhất.
            BaoCao.createTest(testName, description,
                    TaoDonReportBuilder.getLoaiDonCategory(scenario),
                    TaoDonReportBuilder.getLoaiViecCategory(scenario));
            TaoDonReportBuilder.logScenarioOverview(scenario);
        } else {
            ScreenshotStore.beginCase(tenTestDeHieu(result));
            BaoCao.setCaseCode(tenTestDeHieu(result));
            testName = tenTestDeHieu(result);
            description = result.getMethod().getDescription();
            if (description == null || description.isBlank()) {
                description = "Kịch bản kiểm thử không dùng dữ liệu tạo đơn động.";
            }
            BaoCao.createTest(testName, description, category);
            BaoCao.logInfo("Bắt đầu: " + description);
            if (isLoginTest(result)) {
                BaoCao.ketQuaMongDoi("Đăng nhập vào "
                        + ConfigReader.getValue("baseUrl", "https://demo-dichvutuphap.gsfpt.com/")
                        + " bằng tài khoản " + ConfigReader.getValue("username", "")
                        + " và thấy bảng điều khiển.");
            }
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        long elapsed = BaoCao.getTestElapsedMs();
        BaoCao.logPass(
                "Kịch bản đạt — tổng thời gian "
                        + TaoDonReportBuilder.formatDuration(elapsed));
        if (isLoginTest(result)) {
            BaoCao.ketQuaThucTe("Đăng nhập thành công, bảng điều khiển hiển thị Nộp đơn mới.");
        }
        try {
            BaoCao.logStepSummary();
            BaoCaoData.ketThucCase(TrangThai.DAT, elapsed);
        } finally {
            dongCase();
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String errorMsg = extractErrorMessage(result);
        if (WebUI.isBrowserClosed(result.getThrowable())
                || result.getThrowable() instanceof vn.tuphap.automation.flow.BrowserClosedException) {
            // Chỉ dừng thread/browser này — không quitAll / không kéo sập suite.
            vn.tuphap.automation.core.DriverContext.abortCurrentThread(errorMsg);
        }
        // Chỉ bỏ chụp khi lỗi ĐÃ CÓ ẢNH. Trước đây điều kiện là "đã ghi fail" nói chung, nên một
        // logFail bằng chữ (vd. lỗi chung RuntimeException) cũng chặn luôn việc chụp — đúng loại
        // lỗi cần bằng chứng nhất lại thành loại không có ảnh.
        if (!isAlreadyReportedStepBlock(result.getThrowable())
                && !BaoCao.wasFailScreenshotAttached()) {
            attachScreenshotOnFailure(result, errorMsg);
        }
        // Stack trace: không đẩy Throwable vào báo cáo thì muốn biết lỗi ở dòng nào phải mở file
        // log 600KB. Báo cáo hiện nó trong mục "Chi tiết kỹ thuật cho lập trình viên" của case.
        BaoCao.logThrowable(result.getThrowable());
        String shortMsg = rutGonLoiChoTester(errorMsg);
        try {
            // Bước đang chạy dở chính là bước làm hỏng kịch bản — ghi nhận nó là "không hoàn
            // thành" TRƯỚC khi dựng bảng, nếu không bảng sẽ in "Chưa chạy tới" cho đúng bước đó.
            BaoCao.ghiBuocDangMoLaHong();
            BaoCao.logStepSummary();
            BaoCao.ketQuaThucTe(shortMsg);
            BaoCaoData.ketThucCase(TrangThai.THAT_BAI, BaoCao.getTestElapsedMs());
        } finally {
            dongCase();
        }
    }

    /**
     * Dọn mọi trạng thái theo thread của một kịch bản.
     * <p>
     * Gọi trong {@code finally}: nếu khâu ghi báo cáo ném ngoại lệ mà không dọn, kịch bản vừa biến
     * mất khỏi báo cáo vừa để lại trạng thái đọng trên thread — kịch bản kế tiếp chạy trên cùng
     * thread đó sẽ mang mã case, ảnh và các bước của kịch bản này.
     */
    private static void dongCase() {
        BaoCao.clearTestContext();
        StepOutcome.clear();
        ScreenshotStore.clearCase();
    }

    /** Fail bước đã được báo cáo (log + ảnh) trước khi ném exception. */
    private static boolean isAlreadyReportedStepBlock(Throwable t) {
        return t instanceof StepBlockedException;
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        String skipMessage = result.getThrowable() != null
                ? WebUI.friendlyBrowserMessage(result.getThrowable())
                : "Không có lý do chi tiết";
        // TestNG KHÔNG gọi onTestStart cho case bị bỏ qua do lỗi cấu hình (vd. đăng nhập hỏng ở
        // @BeforeMethod), nên chưa có mục nào trên báo cáo và logSkip trước đây lặng lẽ không ghi
        // gì — case biến mất hoàn toàn khỏi HTML. Tự tạo mục ở đây để không mất dấu.
        if (!BaoCao.hasCurrentTest()) {
            TaoDonScenario s = TaoDonReportBuilder.asScenario(result.getParameters());
            if (s != null) {
                // Đặt mã case TRƯỚC createTest. Nhánh này không đi qua onTestStart nên nếu không
                // đặt, createTest sẽ dùng mã còn sót của kịch bản trước trên cùng thread.
                String maCase = SuiteKind.maCase(s);
                ScreenshotStore.beginCase(maCase);
                BaoCao.setCaseCode(maCase);
                BaoCao.createTest(
                        TaoDonReportBuilder.buildTestTitle(s),
                        TaoDonReportBuilder.buildTestDescription(s),
                        TaoDonReportBuilder.getLoaiDonCategory(s),
                        TaoDonReportBuilder.getLoaiViecCategory(s));
            } else {
                ScreenshotStore.beginCase(tenTestDeHieu(result));
                BaoCao.setCaseCode(tenTestDeHieu(result));
                BaoCao.createTest(tenTestDeHieu(result),
                        "Kịch bản bị bỏ qua trước khi bắt đầu chạy.", "Kiểm thử chung");
            }
        }
        try {
            BaoCao.ghiBuocDangMoLaHong();
            BaoCao.logSkip(skipMessage);
            BaoCao.ketQuaThucTe(rutGonLoiChoTester(skipMessage));
            // Kịch bản bị bỏ qua GIỮA CHỪNG vẫn đã chạy được vài bước — thiếu lời gọi này thì
            // bảng 6 bước biến mất đúng lúc người đọc cần biết nó dừng ở đâu.
            BaoCao.logStepSummary();
            BaoCaoData.ketThucCase(TrangThai.BO_QUA, BaoCao.getTestElapsedMs());
        } finally {
            dongCase();
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        int passed = context.getPassedTests().size();
        int failed = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();
        // wallMs là thời gian THỰC TẾ của cả suite. Cộng dồn thời gian từng case ra con số lớn gấp
        // mấy lần khi chạy song song — hai đại lượng này không được lẫn.
        long wallMs = System.currentTimeMillis() - batDauSuiteMs;
        BaoCaoHtml.ghi(ScreenshotStore.runStamp(), ScreenshotStore.runStartedAt(),
                SuiteKind.fromSuiteName(context.getSuite().getName()).fileTag(),
                passed, failed, skipped, wallMs, BaoCaoData.cases());
    }

    private static boolean isLoginTest(ITestResult result) {
        if (result.getMethod() == null) {
            return false;
        }
        String method = result.getMethod().getMethodName();
        return "testDangNhapThanhCong".equals(method) || SuiteKind.hienTai() == SuiteKind.LOGIN;
    }

    private static String rutGonLoiChoTester(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        if (raw.contains("dừng kịch bản ngay")) {
            return raw.replace("❌ ", "").trim();
        }
        if (WebUI.isBrowserClosed(new RuntimeException(raw))
                || raw.toLowerCase().contains("no such window")
                || raw.toLowerCase().contains("target window already closed")
                || raw.toLowerCase().contains("web view not found")) {
            return "Trình duyệt đã đóng hoặc mất kết nối — không nhận được phản hồi từ trang web.";
        }
        String msg = raw.replace("❌ ", "").trim();
        String lower = msg.toLowerCase();
        if (lower.contains("timeout") || lower.contains("hết thời gian chờ")) {
            return "Không nhận được phản hồi từ trang web trong thời gian chờ.";
        }
        if (msg.length() > 300) {
            return msg.substring(0, 300) + "…";
        }
        return msg;
    }

    private static String tenTestDeHieu(ITestResult result) {
        String method = result.getMethod().getMethodName();
        return switch (method) {
            case "testDangNhapThanhCong" -> "Đăng nhập thành công vào hệ thống";
            case "testChinhSuaNoiDungTuXemLai" ->
                    "Chỉnh sửa đơn từ Xem lại (Xem trước đơn → bước 1), nộp lại rồi đối chiếu Xem lại";
            case "testFlowTaoDon" -> "Luồng tạo đơn đầy đủ 6 bước";
            case "syncMasterDataFromUi" -> "Đồng bộ danh mục dữ liệu từ giao diện";
            default -> method;
        };
    }

    private static String extractErrorMessage(ITestResult result) {
        if (result.getThrowable() == null) {
            return "Lỗi không xác định";
        }
        return WebUI.friendlyBrowserMessage(result.getThrowable());
    }

    private static void attachScreenshotOnFailure(ITestResult result, String errorMsg) {
        try {
            WebDriver driver = getDriver(result);
            if (driver != null) {
                String base64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                BaoCao.logFailWithScreenshot(errorMsg, base64);
            } else {
                BaoCao.logFail(errorMsg);
            }
        } catch (Exception e) {
            System.out.println("❌ [THẤT BẠI] " + errorMsg + " (ảnh chụp: " + e.getMessage() + ")");
            try {
                BaoCao.logFail(errorMsg);
            } catch (Exception ignored) {
            }
        }
    }

    private static WebDriver getDriver(ITestResult result) {
        Object instance = result.getInstance();
        if (instance instanceof BaseTest baseTest) {
            return baseTest.getDriver();
        }
        return null;
    }
}

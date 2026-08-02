package vn.tuphap.automation.core;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import vn.tuphap.automation.flow.BrowserClosedException;
import vn.tuphap.automation.pages.DashboardPage;
import vn.tuphap.automation.pages.LoginPage;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.report.BaoCao;
import vn.tuphap.automation.report.BaoCaoData;
import vn.tuphap.automation.report.TrangThai;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.lang.reflect.Method;

/**
 * Mỗi thread giữ 1 Chrome + login riêng. Parallel 3 → tối đa 3 browser.
 * <ul>
 *   <li>Mỗi dòng DataProvider chỉ chạy 1 lần trên 1 thread (không trùng case).</li>
 *   <li>Đóng 1 Chrome → chỉ abort thread đó; 2 Chrome còn lại tiếp tục case của chúng.</li>
 * </ul>
 */
public abstract class TaoDonBaseTest extends BaseTest {

    private static final ThreadLocal<Boolean> THREAD_LOGGED_IN =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    protected boolean reuseBrowserSession() {
        return true;
    }

    @Override
    @BeforeMethod(alwaysRun = true)
    public void setupTestCase(Method method, Object[] args) {
        // no-op — session do ensureThreadSessionAndDashboard
    }

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setupTestCase")
    public void ensureThreadSessionAndDashboard() {
        if (DriverContext.isCurrentThreadAborted()) {
            throw new SkipException(
                    "Bỏ qua case trên trình duyệt đã đóng Chrome — các trình duyệt khác vẫn chạy case còn lại (không trùng).");
        }
        try {
            if (getDriver() == null || !Boolean.TRUE.equals(THREAD_LOGGED_IN.get())
                    || getWebUI() == null || !getWebUI().isBrowserAlive()) {
                if (getDriver() != null && (getWebUI() == null || !getWebUI().isBrowserAlive())) {
                    // Chrome thread này đã chết — abort thread, không quitAll.
                    THREAD_LOGGED_IN.set(Boolean.FALSE);
                    DriverContext.abortCurrentThread("Chrome không còn sống trước khi chạy case");
                    throw new SkipException(
                            "Trình duyệt này đã đóng — chỉ bỏ case còn lại trên " + BrowserLayout.browserLabel() + ".");
                }
                loginForCurrentThread();
            }
            ensureDashboard();
        } catch (BrowserClosedException ex) {
            THREAD_LOGGED_IN.set(Boolean.FALSE);
            DriverContext.abortCurrentThread(ex.getMessage());
            throw new SkipException(
                    BrowserLayout.browserLabel() + " đã đóng — các trình duyệt khác vẫn chạy. " + ex.getMessage(), ex);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void resetToDashboardAfterTest() {
        if (DriverContext.isCurrentThreadAborted()) {
            return;
        }
        try {
            if (getDriver() == null || getWebUI() == null || !getWebUI().isBrowserAlive()) {
                THREAD_LOGGED_IN.set(Boolean.FALSE);
                DriverContext.abortCurrentThread("Chrome đã đóng sau test — chỉ dừng " + BrowserLayout.browserLabel());
                return;
            }
            ensureDashboard();
        } catch (BrowserClosedException ex) {
            System.out.println(" ⚠ " + BrowserLayout.browserLabel() + ": " + ex.getMessage());
            THREAD_LOGGED_IN.set(Boolean.FALSE);
            DriverContext.abortCurrentThread(ex.getMessage());
        } catch (Exception e) {
            if (WebUI.isBrowserClosed(e)) {
                THREAD_LOGGED_IN.set(Boolean.FALSE);
                DriverContext.abortCurrentThread(e.getMessage());
                return;
            }
            System.out.println(" ⚠ Không reset được về Dashboard: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeThreadBrowser() {
        THREAD_LOGGED_IN.remove();
        // Chỉ đóng Chrome của thread chạy AfterClass — không quitAll.
        destroyDriver();
    }

    private void loginForCurrentThread() {
        createDriver();
        String browser = BrowserLayout.browserLabel();
        System.out.println("=== THIẾT LẬP SESSION [" + browser + "] — ĐĂNG NHẬP ===");
        TestActionLog.pause();
        BaoCao.setCaseCode("SETUP_" + browser);
        BaoCao.createTest(
                "Thiết lập session — đăng nhập (" + browser + ")",
                "Setup trước khi chạy kịch bản tạo đơn trên " + browser + " (mỗi case chỉ 1 trình duyệt).");
        // Đăng nhập hỏng là nguyên nhân gốc phổ biến nhất khiến cả một Chrome bị dừng và hàng chục
        // kịch bản bị bỏ qua. Trước đây khối finally huỷ luôn mục này nên toàn bộ log đó chỉ ra
        // console — người đọc báo cáo chỉ thấy N kịch bản "Bỏ qua" với lý do chung chung, không có
        // dấu vết nào của việc đăng nhập hỏng. Giờ: thành công thì vẫn huỷ (nó không phải kịch bản
        // kiểm thử, không được đếm), còn HỎNG thì giữ lại thành một mục để truy nguyên.
        boolean hong = false;
        try {
            performLogin();
            THREAD_LOGGED_IN.set(Boolean.TRUE);
            DriverContext.clearAbortFlag();
            BaoCao.logPass(
                    "Session " + browser + " sẵn sàng — reuse Chrome trên trình duyệt này.");
            System.out.println(" ✅ Session " + browser + " sẵn sàng (song song, case không trùng).");
        } catch (BrowserClosedException ex) {
            hong = true;
            THREAD_LOGGED_IN.set(Boolean.FALSE);
            BaoCao.logFail(ex.getMessage());
            DriverContext.abortCurrentThread(ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            hong = true;
            THREAD_LOGGED_IN.set(Boolean.FALSE);
            BaoCao.logFail("Đăng nhập thất bại (" + browser + "): " + ex.getMessage());
            if (WebUI.isBrowserClosed(ex)) {
                DriverContext.abortCurrentThread(ex.getMessage());
            } else {
                destroyDriver();
            }
            throw ex;
        } finally {
            if (hong) {
                BaoCao.ketQuaMongDoi("Đăng nhập được vào hệ thống để chạy các kịch bản trên "
                        + browser + ".");
                BaoCao.ghiChuKetQua("Mọi kịch bản xếp cho trình duyệt này sẽ bị bỏ qua.");
                BaoCaoData.ketThucCase(TrangThai.THAT_BAI, 0);
            }
            BaoCao.clearTestContext();
            TestActionLog.resume();
        }
    }

    protected void performLogin() {
        WebUI ui = getWebUI();
        ui.failIfBrowserClosed();
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.loginUntilDashboard(
                ConfigReader.getValue("username"),
                ConfigReader.getValue("password"),
                3,
                WaitConfig.DASHBOARD_LOGIN);
    }

    protected void ensureDashboard() {
        if (getDriver() == null || getWebUI() == null) {
            throw new BrowserClosedException(
                    "Trình duyệt chưa mở hoặc đã đóng — dừng kịch bản trên thread này.");
        }
        getWebUI().failIfBrowserClosed();
        LoginPage loginPage = new LoginPage(getDriver());
        new DashboardPage(getDriver()).ensureReady(loginPage, this::performLogin, WaitConfig.DASHBOARD);
    }
}

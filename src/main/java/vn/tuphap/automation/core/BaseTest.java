package vn.tuphap.automation.core;

import vn.tuphap.automation.report.TestListener;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.SkipException;
import org.testng.annotations.*;
import vn.tuphap.automation.ui.WebUI;

import java.lang.reflect.Method;
import java.time.Duration;

@Listeners(TestListener.class)
public class BaseTest {

    public WebDriver getDriver() {
        return DriverContext.getDriver();
    }

    public WebUI getWebUI() {
        return DriverContext.getWebUI();
    }

    public WebDriver driver() {
        return getDriver();
    }

    public WebUI webUI() {
        return getWebUI();
    }

    /** TaoDonTest bật reuse theo thread — LoginTest giữ mặc định false. */
    protected boolean reuseBrowserSession() {
        return false;
    }

    protected void createDriver() {
        // Chỉ đóng Chrome của thread này trước khi mở mới — không quitAll.
        DriverContext.quitCurrent();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--remote-allow-origins=*");
        // Parallel: không maximize — BrowserLayout chia 3 cột để xem cùng lúc.
        if (!ParallelConfig.isParallel()) {
            options.addArguments("--start-maximized");
        }
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);
        options.setCapability("goog:loggingPrefs", java.util.Map.of("browser", "ALL"));

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            BrowserLayout.apply(driver);
            WebUI webUI = new WebUI(driver);
            DriverContext.bind(driver, webUI);
            Integer slot = BrowserLayout.currentSlot();
            System.out.println("🌐 Chrome sẵn sàng [" + BrowserLayout.browserLabel()
                    + (slot != null ? " / cửa sổ #" + (slot + 1) : "")
                    + "] — đang mở " + DriverContext.openDriverCount() + " trình duyệt");
        } catch (RuntimeException e) {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                }
            }
            BrowserLayout.releaseCurrentSlot();
            DriverContext.quitCurrent();
            throw e;
        }
    }

    /** Chỉ đóng Chrome thread hiện tại. */
    protected void destroyDriver() {
        DriverContext.quitCurrent();
    }

    @BeforeMethod(alwaysRun = true)
    public void setupTestCase(Method method, Object[] args) {
        if (DriverContext.isCurrentThreadAborted()) {
            throw new SkipException(
                    "Bỏ qua — thread này đã đóng trình duyệt; các browser/thread khác vẫn chạy case còn lại.");
        }
        if (reuseBrowserSession() && getDriver() != null) {
            return;
        }
        createDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (reuseBrowserSession()) {
            return;
        }
        destroyDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        // Chỉ lúc này mới đóng hết — không gọi khi 1 browser bị đóng giữa chừng.
        DriverContext.quitAll();
        if (!vn.tuphap.automation.config.RunFlowConfig.openReport()) {
            return;
        }
        try {
            java.io.File reportFile = new java.io.File("test-output/ExtentReport.html");
            if (reportFile.exists()) {
                java.awt.Desktop.getDesktop().browse(reportFile.toURI());
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi mở báo cáo: " + e.getMessage());
        }
    }
}

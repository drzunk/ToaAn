package vn.tuphap.automation.core;

import vn.tuphap.automation.report.TestListener;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.*;
import vn.tuphap.automation.ui.WebUI;

import java.lang.reflect.Method;
import java.time.Duration;

@Listeners(TestListener.class)
public class BaseTest {
    public WebDriver driver;
    public WebUI webUI;

    public WebDriver getDriver() {
        return driver;
    }

    /** TaoDonTest bật reuse — LoginTest giữ mặc định false. */
    protected boolean reuseBrowserSession() {
        return false;
    }

    protected void createDriver() {
        destroyDriver();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.addArguments("--start-maximized");
        options.addArguments("--remote-allow-origins=*");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        try {
            driver = new ChromeDriver(options);
            // Explicit wait trong WebUI; implicit = 0 để existsNow không treo kép
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
            ensureWindowUsable();
            webUI = new WebUI(driver);
        } catch (RuntimeException e) {
            destroyDriver();
            throw e;
        }
    }

    /**
     * Tránh maximize qua CDP khi Selenium/Chrome lệch phiên bản
     * (lỗi Runtime.evaluate / maximizeCurrentWindow).
     */
    private void ensureWindowUsable() {
        try {
            driver.manage().window().maximize();
        } catch (Exception maximizeError) {
            System.out.println("⚠ maximize() thất bại (" + maximizeError.getMessage()
                    + ") — dùng setSize fallback.");
            try {
                driver.manage().window().setSize(new Dimension(1920, 1080));
            } catch (Exception sizeError) {
                System.out.println("⚠ setSize cũng thất bại: " + sizeError.getMessage());
            }
        }
    }

    protected void destroyDriver() {
        if (driver != null) {
            try {
                System.out.println("--- Đóng trình duyệt Chrome ---");
                driver.quit();
            } catch (Exception e) {
                System.out.println("⚠ Lỗi khi đóng trình duyệt: " + e.getMessage());
            } finally {
                driver = null;
                webUI = null;
            }
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setupTestCase(Method method, Object[] args) {
        if (reuseBrowserSession() && driver != null) {
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
        destroyDriver();
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

package core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.*;
import utils.WebUI;

import java.lang.reflect.Method;
import java.time.Duration;

@Listeners(utils.TestListener.class)
public class BaseTest {
    public WebDriver driver;
    public WebUI webUI;

    // HÀM QUAN TRỌNG: Cung cấp WebDriver cho TestListener để chụp ảnh lúc Fail
    public WebDriver getDriver() {
        return driver;
    }

    // ĐÃ XÓA SẠCH HÀM @BeforeSuite CHỨA initReport() Ở ĐÂY ĐỂ TRÁNH GHI ĐÈ!

    @BeforeMethod
    public void setupTestCase(Method method, Object[] args) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        webUI = new WebUI(driver);
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            System.out.println("--- Đóng trình duyệt Chrome ---");
            driver.quit();
        }
    }

    @AfterSuite
    public void afterSuite() {
        // Đã gỡ ExtentReportManager.flushReport() ở đây (Giao cho TestListener.onFinish)
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

package core;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.ITestResult;
import org.testng.annotations.*;
import utils.ExtentReportManager;
import utils.WebUI;

import java.lang.reflect.Method;
import java.time.Duration;

@Listeners(utils.TestListener.class)
public class BaseTest {
    public WebDriver driver;
    public WebUI webUI;

    @BeforeSuite
    public void beforeSuite() {
        ExtentReportManager.initReport();
    }

    // QUAY LẠI MỞ TRÌNH DUYỆT TỪNG LẦN (Cho từng dòng Excel)
    @BeforeMethod
    public void setupTestCase(Method method, Object[] args) {
        String testName = method.getName();
        if (args != null && args.length > 0) {
            testName = "Tạo đơn: " + args[0].toString();
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        // --- BỔ SUNG ĐOẠN NÀY ĐỂ TẮT POPUP LƯU MẬT KHẨU ---
        java.util.Map<String, Object> prefs = new java.util.HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);
        // -------------------------------------------------

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        webUI = new WebUI(driver);
    }
    // QUAY LẠI ĐÓNG TRÌNH DUYỆT TỪNG LẦN (Đã dọn sạch rác code lỗi)
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            // Thay vì dùng ExtentReportManager, chỉ in ra console thôi:
            System.out.println("--- Đóng trình duyệt Chrome ---");
            driver.quit();
        }
    }

    @AfterSuite
    public void afterSuite() {
        ExtentReportManager.flushReport();
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
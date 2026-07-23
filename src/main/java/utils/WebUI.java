package utils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.SkipException;

import java.io.File;
import java.time.Duration;
import java.util.List;

public class WebUI {
    private WebDriver driver;
    private WebDriverWait wait;

    public WebUI(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void scrollToElement(By by) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            Thread.sleep(300); // Chờ 1 nhịp rất nhỏ để cuộn xong
        } catch (Exception e) {
            // Không làm gì, bỏ qua lỗi cuộn
        }
    }

    // CLICK BẤT TỬ: Kết hợp Click chuẩn và Ép Click bằng JavaScript
    public void clickElement(By by, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            try {
                wait.until(ExpectedConditions.elementToBeClickable(by)).click();
            } catch (Exception e) {
            String msg = "❌ Lỗi dữ liệu/giao diện: Không tìm thấy hoặc không thể click: [" + elementName + "]";
            ExtentReportManager.logStep(msg); // GHI VÀO REPORT TRƯỚC
            throw new SkipException(msg);     // RỒI MỚI VĂNG LỖI
        }
            ExtentReportManager.logStep("Click vào <b>" + elementName + "</b>");
        } catch (Exception e) {
            throw new SkipException("❌ Lỗi dữ liệu/giao diện: Không tìm thấy hoặc không thể click: [" + elementName + "]");
        }
    }

    public void setText(By by, String value, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            element.clear();
            element.sendKeys(value);
            ExtentReportManager.logStep("Điền dữ liệu <b>'" + value + "'</b> vào <b>" + elementName + "</b>");
        } catch (Exception e) {
            throw new SkipException("❌ Lỗi dữ liệu/giao diện: Không tìm thấy ô [" + elementName + "] để điền '" + value + "'");
        }
    }

    // HÀM KIỂM TRA HIỂN THỊ (Linh hoạt như phiên bản cũ)
    public boolean isElementVisible(By by) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement element = shortWait.until(ExpectedConditions.presenceOfElementLocated(by));
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isElementEnabled(By by) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement element = shortWait.until(ExpectedConditions.presenceOfElementLocated(by));
            return element.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    // NHẬP CÓ ĐIỀU KIỆN (Kiểm tra Excel rỗng trước để tiết kiệm thời gian)
    public void setTextWithCheck(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do dữ liệu Excel trống.");
            return;
        }
        if (!isElementVisible(by)) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do form không hiển thị trường này.");
            return;
        }
        if (!isElementEnabled(by)) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do hệ thống đang khóa (Disabled).");
            return;
        }
        setText(by, value, elementName);
    }

    // NHẬP CÓ MASK (Ngày tháng/CCCD)
    public void setTextForMaskedInput(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do dữ liệu Excel trống.");
            return;
        }
        if (!isElementVisible(by)) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do form không hiển thị trường này.");
            return;
        }
        if (!isElementEnabled(by)) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do hệ thống đang khóa (Disabled).");
            return;
        }

        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            element.sendKeys(value);
            ExtentReportManager.logStep("Điền dữ liệu <b>'" + value + "'</b> vào <b>" + elementName + " (Có Mask)</b>");
        } catch (Exception e) {
            throw new SkipException("❌ Lỗi dữ liệu/giao diện: Không thể nhập dữ liệu vào ô [" + elementName + "]");
        }
    }

    public void selectCustomDropdown(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        clickElement(dropdownLocator, elementName);
        boolean isFound = false;
        try {
            List<WebElement> options = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(optionsLocator));
            for (WebElement option : options) {
                if (option.getText().trim().toLowerCase().contains(expectedText.trim().toLowerCase())) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", option);
                    option.click();
                    ExtentReportManager.logStep("Chọn giá trị <b>'" + expectedText + "'</b> từ danh sách thả xuống");
                    isFound = true;
                    break;
                }
            }
        } catch (Exception e) {
            // Chờ xử lý ở cờ isFound
        }
        if (!isFound) {
            throw new SkipException("❌ Dữ liệu Excel sai: Không tìm thấy giá trị ['" + expectedText + "'] trong danh sách [" + elementName + "]");
        }
    }

    public void selectDropdownWithSearch(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        clickElement(dropdownLocator, elementName);
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
            searchInput.clear();
            searchInput.sendKeys(expectedText);
            ExtentReportManager.logStep("Gõ tìm kiếm <b>'" + expectedText + "'</b> vào thanh Search");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("Không tìm thấy ô nhập tìm kiếm của Dropdown.");
        }

        boolean isFound = false;
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            List<WebElement> options = shortWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(optionsLocator));
            for (WebElement option : options) {
                if (option.getText().trim().toLowerCase().contains(expectedText.trim().toLowerCase())) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", option);
                    option.click();
                    ExtentReportManager.logStep("Chọn giá trị <b>'" + expectedText + "'</b> từ danh sách lọc");
                    isFound = true;
                    break;
                }
            }
        } catch (Exception e) {
        }

        if (!isFound) {
            throw new SkipException("❌ Dữ liệu Excel sai: Không tìm thấy giá trị ['" + expectedText + "'] trong Dropdown [" + elementName + "]");
        }
    }

    public void selectDropdownWithCheck(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do dữ liệu Excel trống.");
            return;
        }
        if (!isElementVisible(dropdownLocator)) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do form không hiển thị trường này.");
            return;
        }
        selectCustomDropdown(dropdownLocator, optionsLocator, expectedText, elementName);
    }

    public void selectToaAnWithCheck(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do dữ liệu Excel trống.");
            return;
        }
        if (!isElementVisible(dropdownLocator)) {
            ExtentReportManager.logStep("Bỏ qua <b>" + elementName + "</b> do form không hiển thị trường này.");
            return;
        }
        selectDropdownWithSearch(dropdownLocator, searchInputLocator, optionsLocator, expectedText, elementName);
    }

    public void zoomPage(String percentage) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("document.body.style.zoom = '" + percentage + "'");
            ExtentReportManager.logStep("Đã thu nhỏ màn hình xuống <b>" + percentage + "</b>");
        } catch (Exception e) {
            System.out.println("Không thể zoom màn hình: " + e.getMessage());
        }
    }

    public void captureScreen(String message) {
        try {
            org.openqa.selenium.TakesScreenshot ts = (org.openqa.selenium.TakesScreenshot) driver;
            String base64Image = ts.getScreenshotAs(org.openqa.selenium.OutputType.BASE64);
            ExtentReportManager.logPassWithScreenshot(message, base64Image);
        } catch (Exception e) {
            System.out.println("Lỗi khi chụp ảnh: " + e.getMessage());
        }
    }

    public void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String docCaptcha(By locatorAnhCaptcha) {
        String result = "";
        try {
            WebElement captchaImage = driver.findElement(locatorAnhCaptcha);
            File srcFile = captchaImage.getScreenshotAs(OutputType.FILE);

            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata");
            tesseract.setLanguage("eng");

            result = tesseract.doOCR(srcFile);
            result = result.replaceAll("[^a-zA-Z0-9]", "");

            ExtentReportManager.logStep("🤖 AI đã giải mã Captcha thành: <b>" + result + "</b>");
        } catch (Exception e) {
            ExtentReportManager.logStep("❌ Lỗi khi đọc Captcha: " + e.getMessage());
        }
        return result;
    }
}
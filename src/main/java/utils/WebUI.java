package utils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
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
            Thread.sleep(300);
        } catch (Exception e) {
        }
    }

    public void clickElement(By by, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            try {
                wait.until(ExpectedConditions.elementToBeClickable(by)).click();
            } catch (Exception ex) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click();", element);
            }
            System.out.println(" ➔ Click vào: [" + elementName + "]");
        } catch (Exception e) {
            // ĐÃ ĐỔI SANG RUNTIME EXCEPTION ĐỂ ĐÁNH FAILED REPORT
            throw new RuntimeException("❌ Lỗi: Không tìm thấy hoặc không thể click: [" + elementName + "]");
        }
    }

    public void setText(By by, String value, String elementName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            element.clear();
            element.sendKeys(value);
            System.out.println(" ➔ Điền: '" + value + "' vào [" + elementName + "]");
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi: Không tìm thấy ô [" + elementName + "] để điền '" + value + "'");
        }
    }

    public boolean isElementVisible(By by) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement element = shortWait.until(ExpectedConditions.presenceOfElementLocated(by));
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Kiểm tra nhanh trong vòng lặp chờ — timeout ngắn, không block lâu. */
    public boolean isElementPresent(By by) {
        return existsNow(by);
    }

    /**
     * Tìm element ngay lập tức (implicit wait = 0).
     * Tránh treo khi BaseTest đặt implicitlyWait 10s.
     */
    public boolean existsNow(By by) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            for (WebElement element : driver.findElements(by)) {
                try {
                    if (element.isDisplayed()) {
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }

    public int countNow(By by) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            return driver.findElements(by).size();
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }

    public void waitUntilVisible(By by, int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ hiển thị: " + description);
        try {
            WebDriverWait visibleWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            visibleWait.until(ExpectedConditions.visibilityOfElementLocated(by));
            System.out.println(" ✅ Đã hiển thị: " + description);
        } catch (TimeoutException e) {
            throw new RuntimeException("❌ Timeout: [" + description + "] không hiển thị sau " + timeoutSeconds + "s.");
        }
    }

    public void waitUntilExists(By by, int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ: " + description);
        for (int i = 1; i <= timeoutSeconds; i++) {
            if (existsNow(by)) {
                System.out.println(" ✅ Sẵn sàng: " + description);
                return;
            }
            if (i == 1 || i % 5 == 0 || i == timeoutSeconds) {
                System.out.println(" ⏳ Chờ: " + description + " (" + i + "/" + timeoutSeconds + "s)");
            }
            sleep(1);
        }
        throw new RuntimeException("❌ Lỗi: Timeout chờ [" + description + "] sau " + timeoutSeconds + "s.");
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

    public void setTextWithCheck(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do Excel trống.");
            return;
        }
        if (!isElementVisible(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do form ẩn.");
            return;
        }
        if (!isElementEnabled(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do hệ thống khóa.");
            return;
        }
        setText(by, value, elementName);
    }

    public void setTextForMaskedInput(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do Excel trống.");
            return;
        }
        if (!isElementVisible(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do form ẩn.");
            return;
        }
        if (!isElementEnabled(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do hệ thống khóa.");
            return;
        }

        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            element.sendKeys(value);
            System.out.println(" ➔ Điền (Mask): '" + value + "' vào [" + elementName + "]");
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi: Không thể nhập dữ liệu vào ô [" + elementName + "]");
        }
    }

    /**
     * Tải file lên input[type=file] (kể cả input ẩn trong label).
     */
    public void uploadFile(By by, String absolutePath, String elementName) {
        if (absolutePath == null || absolutePath.trim().isEmpty()) {
            throw new RuntimeException("❌ Thiếu đường dẫn file cho [" + elementName + "]");
        }
        File file = new File(absolutePath);
        if (!file.exists()) {
            throw new RuntimeException("❌ Không tìm thấy file: " + absolutePath);
        }
        try {
            WebElement input = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            input.sendKeys(file.getAbsolutePath());
            System.out.println(" ➔ Tải lên: '" + file.getName() + "' tại [" + elementName + "]");
            sleep(1);
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi: Không thể tải file lên [" + elementName + "]: " + e.getMessage());
        }
    }

    public void selectCustomDropdown(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        clickElement(dropdownLocator, elementName);
        sleep(1);
        boolean isFound = false;
        List<String> availableOptions = new ArrayList<>();
        try {
            List<WebElement> options = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(optionsLocator));
            for (WebElement option : options) {
                String textOnWeb = option.getText();
                if (textOnWeb == null || textOnWeb.trim().isEmpty()) {
                    textOnWeb = option.getAttribute("textContent");
                }
                if (textOnWeb != null && !textOnWeb.trim().isEmpty()) {
                    availableOptions.add(textOnWeb.trim());
                }
                if (textOnWeb != null && optionMatches(expectedText, textOnWeb)) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", option);
                    Thread.sleep(300);
                    option.click();
                    System.out.println(" ➔ Chọn Dropdown: '" + expectedText + "' tại [" + elementName + "]");
                    isFound = true;
                    break;
                }
            }
        } catch (Exception e) {
        }
        if (!isFound) {
            throw new RuntimeException("❌ Lỗi dữ liệu/Web lag: Không tìm thấy ['" + expectedText + "'] trong [" + elementName
                    + "]. Option UI hiện có: " + availableOptions
                    + ". Cập nhật master-data.properties hoặc chạy MasterDataSyncTest.");
        }
    }

    public void selectDropdownWithSearch(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        clickElement(dropdownLocator, elementName);
        sleep(1);
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
            searchInput.clear();
            searchInput.sendKeys(expectedText);
            System.out.println(" ➔ Gõ tìm kiếm Dropdown: '" + expectedText + "'");
            Thread.sleep(1000);
        } catch (Exception e) {
        }

        boolean isFound = false;
        List<String> availableOptions = new ArrayList<>();
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            List<WebElement> options = shortWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(optionsLocator));
            for (WebElement option : options) {
                String textOnWeb = option.getText();
                if (textOnWeb == null || textOnWeb.trim().isEmpty()) {
                    textOnWeb = option.getAttribute("textContent");
                }
                if (textOnWeb != null && !textOnWeb.trim().isEmpty()) {
                    availableOptions.add(textOnWeb.trim());
                }
                if (textOnWeb != null && optionMatches(expectedText, textOnWeb)) {
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", option);
                    option.click();
                    System.out.println(" ➔ Chọn Dropdown lọc: '" + expectedText + "' tại [" + elementName + "]");
                    isFound = true;
                    break;
                }
            }
        } catch (Exception e) {
        }

        if (!isFound) {
            throw new RuntimeException("❌ Lỗi dữ liệu: Không tìm thấy ['" + expectedText + "'] trong Dropdown [" + elementName
                    + "]. Option UI hiện có: " + availableOptions
                    + ". Cập nhật master-data.properties hoặc chạy MasterDataSyncTest.");
        }
    }

    public void selectDropdownWithCheck(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua Dropdown: [" + elementName + "] do Excel trống.");
            return;
        }
        waitUntilVisible(dropdownLocator, 15, elementName);
        selectCustomDropdown(dropdownLocator, optionsLocator, expectedText, elementName);
    }

    public void selectToaAnWithCheck(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua Tòa án: [" + elementName + "] do Excel trống.");
            return;
        }
        if (!isElementVisible(dropdownLocator)) {
            System.out.println(" ⏩ Bỏ qua Tòa án: [" + elementName + "] do form ẩn.");
            return;
        }
        selectDropdownWithSearch(dropdownLocator, searchInputLocator, optionsLocator, expectedText, elementName);
    }

    public void zoomPage(String percentage) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("document.body.style.zoom = '" + percentage + "'");
            System.out.println(" ➔ Thu nhỏ màn hình: " + percentage);
        } catch (Exception e) {
        }
    }

    public void captureScreen(String message) {
        try {
            org.openqa.selenium.TakesScreenshot ts = (org.openqa.selenium.TakesScreenshot) driver;
            String base64Image = ts.getScreenshotAs(org.openqa.selenium.OutputType.BASE64);
            String url = "";
            try {
                url = driver.getCurrentUrl();
            } catch (Exception ignored) {
            }
            String caption = message;
            if (!url.isBlank()) {
                caption = message + " · " + url;
            }
            ExtentReportManager.logInfoWithScreenshot(caption, base64Image);
        } catch (Exception e) {
        }
    }

    public void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
        }
    }

    public String docCaptcha(By locatorAnhCaptcha) {
        String result = "";
        try {
            WebElement captchaElement = driver.findElement(locatorAnhCaptcha);
            String rawText = captchaElement.getText();
            if (rawText != null && !rawText.trim().isEmpty()) {
                result = rawText.replaceAll("[^a-zA-Z0-9]", "");
                System.out.println(" 🤖 Bắt Captcha từ HTML: " + result);
                return result;
            }

            File srcFile = captchaElement.getScreenshotAs(OutputType.FILE);
            ITesseract tesseract = new Tesseract();
            String absoluteTessDataPath = System.getProperty("user.dir") + java.io.File.separator + "tessdata";
            tesseract.setDatapath(absoluteTessDataPath);
            tesseract.setLanguage("eng");

            result = tesseract.doOCR(srcFile);
            result = result.replaceAll("[^a-zA-Z0-9]", "");
            System.out.println(" 🤖 AI giải mã Captcha thành: " + result);

        } catch (Exception e) {
            System.out.println(" ❌ Lỗi đọc Captcha: " + e.getMessage());
        }
        return result;
    }

    private String normalizeOptionText(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().toLowerCase()
                .replace('–', '-')
                .replace('—', '-')
                .replaceAll("\\s*/\\s*", "/")
                .replaceAll("\\s+", " ");
    }

    private boolean optionMatches(String expected, String actual) {
        String normalizedExpected = normalizeOptionText(expected);
        String normalizedActual = normalizeOptionText(actual);
        return normalizedActual.equals(normalizedExpected)
                || normalizedActual.contains(normalizedExpected);
    }
}

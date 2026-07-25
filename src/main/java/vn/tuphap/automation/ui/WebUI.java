package vn.tuphap.automation.ui;

import vn.tuphap.automation.core.BaseTest;

import vn.tuphap.automation.report.TestActionLog;

import vn.tuphap.automation.report.ExtentReportManager;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebUI {
    private WebDriver driver;
    private WebDriverWait wait;

    public WebUI(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(WaitConfig.DROPDOWN));
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
            recordClickOrChon(elementName);
        } catch (Exception e) {
            // ĐÃ ĐỔI SANG RUNTIME EXCEPTION ĐỂ ĐÁNH FAILED REPORT
            throw new RuntimeException("❌ Lỗi: Không tìm thấy hoặc không thể click: [" + elementName + "]");
        }
    }

    private static void recordClickOrChon(String elementName) {
        if (elementName == null) {
            TestActionLog.click("");
            return;
        }
        String lower = elementName.toLowerCase();
        boolean isChoice = lower.startsWith("thẻ ")
                || lower.contains("nút chuyển ")
                || lower.contains("toggle ")
                || lower.contains("hộp kiểm ")
                || lower.contains("checkbox ");
        if (isChoice) {
            String value = extractBracketValue(elementName);
            TestActionLog.chon(elementName, value.isBlank() ? "Đã chọn" : value);
        } else {
            TestActionLog.click(elementName);
        }
    }

    private static String extractBracketValue(String name) {
        int a = name.lastIndexOf('[');
        int b = name.lastIndexOf(']');
        if (a >= 0 && b > a) {
            return name.substring(a + 1, b).trim();
        }
        return "";
    }

    /** Click không ghi TestLogs (mở danh sách thả xuống trước khi chọn). */
    public void clickElementQuiet(By by, String elementName) {
        try {
            scrollToElement(by);
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(by));
            try {
                element.click();
            } catch (Exception ex) {
                JavascriptExecutor js = (JavascriptExecutor) driver;
                js.executeScript("arguments[0].click();", element);
            }
            System.out.println(" ➔ Click vào: [" + elementName + "]");
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi: Không tìm thấy hoặc không thể click: [" + elementName + "]");
        }
    }

    public void setText(By by, String value, String elementName) {
        try {
            scrollToElement(by);
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(by));
            // Prefer an editable input/textarea if the locator matched a wrapper.
            String tag = element.getTagName() == null ? "" : element.getTagName().toLowerCase();
            if (!tag.equals("input") && !tag.equals("textarea")) {
                List<WebElement> nested = element.findElements(By.xpath(".//input|.//textarea"));
                if (!nested.isEmpty()) {
                    element = nested.get(0);
                }
            }
            element.click();
            element.clear();
            element.sendKeys(value);
            System.out.println(" ➔ Điền: '" + value + "' vào [" + elementName + "]");
            TestActionLog.dien(elementName, value);
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
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        }
    }

    public int countNow(By by) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            return driver.findElements(by).size();
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        }
    }

    public void waitUntilVisible(By by, int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ hiển thị: " + description);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(250))
                    .until(ExpectedConditions.visibilityOfElementLocated(by));
            System.out.println(" ✅ Đã hiển thị: " + description);
        } catch (TimeoutException e) {
            throw new RuntimeException("❌ Hết thời gian chờ: [" + description + "] không hiển thị sau " + timeoutSeconds + "s.");
        }
    }

    public void waitUntilExists(By by, int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ: " + description);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> existsNow(by));
            System.out.println(" ✅ Sẵn sàng: " + description);
        } catch (TimeoutException e) {
            throw new RuntimeException("❌ Lỗi: hết thời gian chờ [" + description + "] sau " + timeoutSeconds + "s.");
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

    public void setTextWithCheck(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không nhập");
            return;
        }
        if (!isElementVisible(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do biểu mẫu ẩn.");
            TestActionLog.boQua(elementName, "Biểu mẫu ẩn / không có trên giao diện");
            return;
        }
        if (!isElementEnabled(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do hệ thống khóa.");
            TestActionLog.boQua(elementName, "Ô bị khóa");
            return;
        }
        setText(by, value, elementName);
    }

    public void setTextForMaskedInput(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không nhập");
            return;
        }
        if (!isElementVisible(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do biểu mẫu ẩn.");
            TestActionLog.boQua(elementName, "Biểu mẫu ẩn / không có trên giao diện");
            return;
        }
        if (!isElementEnabled(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do hệ thống khóa.");
            TestActionLog.boQua(elementName, "Ô bị khóa");
            return;
        }

        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(by);
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            element.sendKeys(value);
            System.out.println(" ➔ Điền (định dạng đặc biệt): '" + value + "' vào [" + elementName + "]");
            TestActionLog.dienMask(elementName, value);
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
            String tenHienThi = tenTepHienThi(file.getName());
            System.out.println(" ➔ Tải lên: '" + tenHienThi + "' tại [" + elementName + "]");
            TestActionLog.taiLen(elementName, tenHienThi);
            sleep(1);
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi: Không thể tải file lên [" + elementName + "]: " + e.getMessage());
        }
    }

    public void selectCustomDropdown(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        clickElementQuiet(dropdownLocator, elementName);
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
                    TestActionLog.chon(elementName, expectedText);
                    isFound = true;
                    break;
                }
            }
        } catch (Exception e) {
        }
        if (!isFound) {
            throw new RuntimeException("❌ Lỗi dữ liệu/ứng dụng chậm: Không tìm thấy ['" + expectedText + "'] trong [" + elementName
                    + "]. Tuỳ chọn giao diện hiện có: " + availableOptions
                    + ". Cập nhật master-data.properties hoặc chạy đồng bộ dữ liệu gốc.");
        }
    }

    public void selectDropdownWithSearch(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        clickElementQuiet(dropdownLocator, elementName);
        sleep(1);
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
            searchInput.clear();
            searchInput.sendKeys(expectedText);
            System.out.println(" ➔ Gõ tìm kiếm Dropdown: '" + expectedText + "'");
            TestActionLog.timKiemDropdown(expectedText);
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
                    System.out.println(" ➔ Chọn Dropdown (lọc): '" + expectedText + "' tại [" + elementName + "]");
                    TestActionLog.chon(elementName, expectedText);
                    isFound = true;
                    break;
                }
            }
        } catch (Exception e) {
        }

        if (!isFound) {
            throw new RuntimeException("❌ Lỗi dữ liệu: Không tìm thấy ['" + expectedText + "'] trong Dropdown [" + elementName
                    + "]. Tuỳ chọn giao diện hiện có: " + availableOptions
                    + ". Cập nhật master-data.properties hoặc chạy đồng bộ dữ liệu gốc.");
        }
    }

    public void selectDropdownWithCheck(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua Dropdown: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không chọn Dropdown");
            return;
        }
        waitUntilVisible(dropdownLocator, WaitConfig.DROPDOWN, elementName);
        selectCustomDropdown(dropdownLocator, optionsLocator, expectedText, elementName);
    }

    public void selectToaAnWithCheck(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua Tòa án: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không chọn tòa án");
            return;
        }
        if (!isElementVisible(dropdownLocator)) {
            System.out.println(" ⏩ Bỏ qua Tòa án: [" + elementName + "] do biểu mẫu ẩn.");
            TestActionLog.boQua(elementName, "Biểu mẫu ẩn / không có trên giao diện");
            return;
        }
        selectDropdownWithSearch(dropdownLocator, searchInputLocator, optionsLocator, expectedText, elementName);
    }

    public void zoomPage(String percentage) {
        // Không dùng document.body.style.zoom — làm lệch layout SPA và ảnh chụp.
        // Giữ method để tương thích gọi cũ; chỉ log nhắc nhở.
        System.out.println(" ⏩ Bỏ qua zoom " + percentage
                + " (CSS zoom gây xáo layout — chạy ở tỉ lệ 100%).");
    }

    /**
     * Một ảnh tổng quan (viewport hiện tại, cuộn về đầu trang).
     * Dùng cho báo cáo Extent — không chụp nhiều khung theo bước.
     */
    public String takeOverviewScreenshot() {
        if (!screenshotsEnabled()) {
            return null;
        }
        try {
            dismissOverlaysForScreenshot();
            scrollWindowTo(0);
            sleepMillis(WaitConfig.SETTLE_MS);
            String shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            return (shot == null || shot.isBlank()) ? null : shot;
        } catch (Exception e) {
            System.out.println(" ⚠ Không chụp được ảnh tổng quan: " + e.getMessage());
            return null;
        }
    }

    /** Đính 1 ảnh tổng quan vào báo cáo Extent. */
    public void captureOverview(String message) {
        String shot = takeOverviewScreenshot();
        if (shot == null) {
            return;
        }
        String base = message;
        try {
            String url = driver.getCurrentUrl();
            if (url != null && !url.isBlank()) {
                base = message + " · " + url;
            }
        } catch (Exception ignored) {
        }
        ExtentReportManager.logScreenshots(base, List.of(shot));
    }

    /**
     * Chụp 1–3 ảnh theo chiều dọc biểu mẫu (đầu / giữa / cuối) — dùng khi cần chi tiết sâu.
     * Form ngắn chỉ trả 1 ảnh. Không đổi zoom, không sửa CSS.
     */
    public List<String> takeContextScreenshots() {
        List<String> shots = new ArrayList<>();
        if (!screenshotsEnabled()) {
            return shots;
        }
        try {
            dismissOverlaysForScreenshot();
            int[] positions = resolveScrollPositions();
            for (int pos : positions) {
                scrollWindowTo(pos);
                sleepMillis(WaitConfig.SETTLE_MS);
                String shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                if (shot != null && !shot.isBlank()) {
                    shots.add(shot);
                }
            }
            scrollWindowTo(0);
            sleepMillis(100);
        } catch (Exception e) {
            System.out.println(" ⚠ Không chụp được chuỗi ảnh ngữ cảnh: " + e.getMessage());
            if (shots.isEmpty()) {
                try {
                    shots.add(((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
                } catch (Exception ignored) {
                }
            }
        }
        return shots;
    }

    /** Tương thích cũ — lấy ảnh tổng quan 1 khung. */
    public String takeContextScreenshot() {
        return takeOverviewScreenshot();
    }

    /** Mặc định: ảnh tổng quan 1 khung (không multi-scroll). */
    public void captureScreen(String message) {
        captureOverview(message);
    }

    private static boolean screenshotsEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("taodon.screenshot", "true"));
    }

    private void dismissOverlaysForScreenshot() {
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {
        }
        sleepMillis(100);
    }

    /**
     * Vị trí cuộn cửa sổ: form ngắn → [0]; dài → [0, giữa, cuối].
     */
    private int[] resolveScrollPositions() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            @SuppressWarnings("unchecked")
            Map<String, Number> metrics = (Map<String, Number>) js.executeScript(
                    "var se = document.scrollingElement || document.documentElement;"
                            + "var view = window.innerHeight || se.clientHeight || 800;"
                            + "var max = Math.max(0, (se.scrollHeight || 0) - view);"
                            + "return { view: view, max: max };");
            int max = metrics == null || metrics.get("max") == null ? 0 : metrics.get("max").intValue();
            if (max < 120) {
                return new int[]{0};
            }
            int mid = max / 2;
            // Tránh trùng gần như nhau
            if (mid < 80 || Math.abs(max - mid) < 80) {
                return new int[]{0, max};
            }
            return new int[]{0, mid, max};
        } catch (Exception e) {
            return new int[]{0};
        }
    }

    private void scrollWindowTo(int y) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "window.scrollTo({top: arguments[0], left: 0, behavior: 'instant'});",
                    Math.max(0, y));
        } catch (Exception ignored) {
        }
    }

    public void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
        }
    }

    public void sleepMillis(long millis) {
        try {
            Thread.sleep(Math.max(0, millis));
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
            tesseract.setDatapath(resolveTessDataPath());
            tesseract.setLanguage("eng");

            result = tesseract.doOCR(srcFile);
            result = result.replaceAll("[^a-zA-Z0-9]", "");
            System.out.println(" 🤖 AI giải Captcha thành: " + result);

        } catch (Exception e) {
            System.out.println(" ❌ Lỗi đọc Captcha: " + e.getMessage());
        }
        return result;
    }

    /** Tên hiển thị trong nhật ký/Excel — file mẫu không dùng tên tiếng Anh. */
    private static String tenTepHienThi(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return fileName;
        }
        String lower = fileName.toLowerCase();
        if ("sample.pdf".equals(lower)) {
            return "tệp mẫu.pdf";
        }
        if ("sample.png".equals(lower) || "sample.jpg".equals(lower) || "sample.jpeg".equals(lower)) {
            int dot = fileName.lastIndexOf('.');
            return "tệp mẫu" + (dot >= 0 ? fileName.substring(dot) : "");
        }
        return fileName;
    }

    /** Thư mục chứa *.traineddata — ưu tiên classpath `tessdata/` (src/main/resources). */
    private static String resolveTessDataPath() {
        try {
            java.net.URL url = WebUI.class.getClassLoader().getResource("tessdata/eng.traineddata");
            if (url != null) {
                java.nio.file.Path file = java.nio.file.Paths.get(url.toURI());
                return file.getParent().toAbsolutePath().toString();
            }
        } catch (Exception ignored) {
            // fallback below
        }
        java.nio.file.Path fallback = java.nio.file.Paths.get(
                System.getProperty("user.dir"), "src", "main", "resources", "tessdata");
        return fallback.toAbsolutePath().toString();
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

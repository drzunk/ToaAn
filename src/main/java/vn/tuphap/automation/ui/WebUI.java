package vn.tuphap.automation.ui;

import vn.tuphap.automation.report.TestActionLog;

import vn.tuphap.automation.report.ExtentReportManager;

import vn.tuphap.automation.flow.StepBlockedException;

import vn.tuphap.automation.flow.BrowserClosedException;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WebUI {
    private WebDriver driver;
    private WebDriverWait wait;

    /** Toast/notify góc màn hình — đọc phản hồi lỗi từ server. */
    private static final List<By> TOAST_SELECTORS = List.of(
            By.cssSelector(".ant-notification-notice"),
            By.cssSelector(".ant-message-notice"),
            By.cssSelector(".ant-message-notice-content"),
            By.cssSelector(".Toastify__toast"),
            By.cssSelector("[data-sonner-toast]"),
            By.xpath("//div[(contains(@class,'toast') or contains(@class,'notification')"
                    + " or contains(@class,'Notification') or contains(@class,'notify'))"
                    + " and string-length(normalize-space(.)) > 0]")
    );

    public WebUI(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(WaitConfig.DROPDOWN));
    }

    public void scrollToElement(By by) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(by));
            scrollToElement(element);
        } catch (Exception ignored) {
        }
    }

    public void clickElement(By by, String elementName) {
        clickElement(by, elementName, WaitConfig.DROPDOWN);
    }

    public void clickElement(By by, String elementName, int timeoutSeconds) {
        try {
            failIfBrowserClosed();
            WebElement element = waitForDisplayedEnabled(by, timeoutSeconds);
            scrollToElement(element);
            clickWithFallback(element);
            System.out.println(" ➔ Click vào: [" + elementName + "]");
            recordClickOrChon(elementName);
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception e) {
            failIfBrowserClosed(e);
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new RuntimeException("❌ Lỗi: Không tìm thấy hoặc không thể click: ["
                    + elementName + "] — " + detail);
        }
    }

    /** Chờ có ít nhất một element khớp locator đang hiển thị và enabled. */
    public void waitUntilClickable(By by, int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ có thể click: " + description);
        try {
            waitForDisplayedEnabled(by, timeoutSeconds);
            System.out.println(" ✅ Có thể click: " + description);
        } catch (TimeoutException e) {
            throw new RuntimeException("❌ Hết thời gian chờ: [" + description
                    + "] không thể click sau " + timeoutSeconds + "s.");
        }
    }

    private WebElement waitForDisplayedEnabled(By by, int timeoutSeconds) {
        failIfBrowserClosed();
        WebDriverWait stepWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        stepWait.pollingEvery(Duration.ofMillis(250));
        try {
            return stepWait.until(d -> {
                failIfBrowserClosed();
                return findDisplayedEnabled(by);
            });
        } catch (TimeoutException e) {
            failIfBrowserClosed();
            throw e;
        }
    }

    private WebElement findDisplayedEnabled(By by) {
        try {
            for (WebElement element : driver.findElements(by)) {
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        return element;
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            return null;
        } catch (WebDriverException e) {
            failIfBrowserClosed(e);
            throw e;
        }
    }

    private void scrollToElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                    "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
            sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        } catch (Exception ignored) {
        }
    }

    private void clickWithFallback(WebElement element) {
        try {
            element.click();
        } catch (Exception ex) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", element);
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

    /** Tick checkbox — một click tự nhiên trên label, chờ {@code checked} (không sleep cố định). */
    public void clickCheckboxInLabel(By labelBy, String elementName) {
        try {
            WebElement label = waitForDisplayedEnabled(labelBy, WaitConfig.FIELD);
            scrollToElement(label);
            WebElement input = label.findElement(By.xpath(".//input[@type='checkbox']"));

            if (isCheckboxChecked(input)) {
                System.out.println(" ⏩ [" + elementName + "] đã được chọn sẵn.");
                TestActionLog.chon(elementName, "Đã chọn sẵn");
                return;
            }

            waitForCheckboxClickable(input, WaitConfig.FIELD);
            clickWithFallback(label);

            if (!waitUntilCheckboxChecked(input, 3)) {
                clickWithFallback(input);
                if (!waitUntilCheckboxChecked(input, 2)) {
                    throw new TimeoutException("Checkbox chưa chuyển sang trạng thái checked");
                }
            }

            System.out.println(" ➔ Click vào: [" + elementName + "]");
            recordClickOrChon(elementName);
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new RuntimeException("❌ Lỗi: Không thể tick checkbox [" + elementName + "] — " + detail);
        }
    }

    /** Đặt checkbox theo trạng thái mong muốn — không toggle nếu đã đúng. */
    public void ensureCheckboxState(By labelBy, boolean wantChecked, String elementName) {
        try {
            WebElement label = waitForDisplayedEnabled(labelBy, WaitConfig.FIELD);
            scrollToElement(label);
            WebElement input = label.findElement(By.xpath(".//input[@type='checkbox']"));
            if (isCheckboxChecked(input) == wantChecked) {
                System.out.println(" ⏩ [" + elementName + "] đã "
                        + (wantChecked ? "chọn" : "bỏ chọn") + " sẵn.");
                return;
            }
            waitForCheckboxClickable(input, WaitConfig.FIELD);
            clickWithFallback(label);
            boolean ok = wantChecked
                    ? waitUntilCheckboxChecked(input, 3)
                    : waitUntilCheckboxUnchecked(input, 3);
            if (!ok) {
                clickWithFallback(input);
                ok = wantChecked
                        ? waitUntilCheckboxChecked(input, 2)
                        : waitUntilCheckboxUnchecked(input, 2);
            }
            if (!ok) {
                throw new TimeoutException("Checkbox chưa chuyển sang trạng thái "
                        + (wantChecked ? "checked" : "unchecked"));
            }
            System.out.println(" ➔ " + (wantChecked ? "Chọn" : "Bỏ chọn") + ": [" + elementName + "]");
            recordClickOrChon(elementName);
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new RuntimeException("❌ Lỗi: Không thể đặt checkbox [" + elementName + "] — " + detail);
        }
    }

    public boolean waitUntilCheckboxUnchecked(WebElement checkboxInput, int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(100))
                    .until(d -> !isCheckboxChecked(checkboxInput));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Poll nhanh — thoát ngay khi React cập nhật {@code checked}. */
    public boolean waitUntilCheckboxChecked(WebElement checkboxInput, int timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(100))
                    .until(d -> isCheckboxChecked(checkboxInput));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** {@code input.checked} — tin cậy hơn {@code isSelected()} với React. */
    public boolean isCheckboxChecked(WebElement checkboxInput) {
        try {
            if (checkboxInput.isSelected()) {
                return true;
            }
            String aria = checkboxInput.getAttribute("aria-checked");
            if ("true".equalsIgnoreCase(aria)) {
                return true;
            }
            Object checked = ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].checked === true;", checkboxInput);
            return Boolean.TRUE.equals(checked);
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForCheckboxClickable(WebElement input, int timeoutSeconds) {
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        w.pollingEvery(Duration.ofMillis(100));
        w.until(d -> {
            try {
                return input.isDisplayed() && input.isEnabled();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    /** Click không ghi báo cáo Excel (mở danh sách thả xuống trước khi chọn). */
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
            failIfBrowserClosed();
            scrollToElement(by);
            WebElement element = wait.until(d -> {
                failIfBrowserClosed();
                return ExpectedConditions.visibilityOfElementLocated(by).apply(d);
            });
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
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception e) {
            failIfBrowserClosed(e);
            throw new RuntimeException("❌ Lỗi: Không tìm thấy ô [" + elementName + "] để điền '" + value + "'");
        }
    }

    public boolean isElementVisible(By by) {
        try {
            failIfBrowserClosed();
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
            shortWait.pollingEvery(Duration.ofMillis(250));
            WebElement element = shortWait.until(d -> {
                failIfBrowserClosed();
                return ExpectedConditions.presenceOfElementLocated(by).apply(d);
            });
            return element.isDisplayed();
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception e) {
            failIfBrowserClosed(e);
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
        failIfBrowserClosed();
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
        } catch (WebDriverException e) {
            failIfBrowserClosed(e);
            throw e;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        }
    }

    /** Chrome/tab đã đóng hoặc session WebDriver không còn hợp lệ. */
    public static boolean isBrowserClosed(Throwable t) {
        if (t == null) {
            return false;
        }
        String msg = t.getMessage();
        if (msg == null) {
            msg = "";
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("no such window")
                || lower.contains("target window already closed")
                || lower.contains("web view not found")
                || lower.contains("invalid session id")
                || lower.contains("session deleted")
                || lower.contains("disconnected")
                || lower.contains("not connected to devtools")) {
            return true;
        }
        return t.getCause() != null && isBrowserClosed(t.getCause());
    }

    public boolean isBrowserAlive() {
        try {
            driver.getWindowHandles();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Dừng ngay nếu Chrome/tab đã đóng — không chờ timeout, không mở lại trang. */
    public void failIfBrowserClosed() {
        if (driver == null || !isBrowserAlive()) {
            throw new BrowserClosedException(
                    "Trình duyệt đã đóng hoặc mất kết nối — dừng kịch bản ngay.");
        }
    }

    public void failIfBrowserClosed(Throwable cause) {
        if (cause instanceof BrowserClosedException) {
            throw (BrowserClosedException) cause;
        }
        if (isBrowserClosed(cause)) {
            throw new BrowserClosedException(
                    "Trình duyệt đã đóng hoặc mất kết nối — dừng kịch bản ngay.", cause);
        }
    }

    /** Message ngắn gọn cho báo cáo Excel/Extent. */
    public static String friendlyBrowserMessage(Throwable t) {
        if (t instanceof BrowserClosedException) {
            return t.getMessage();
        }
        if (t == null) {
            return "Không nhận được phản hồi từ trang web trong thời gian chờ.";
        }
        if (isBrowserClosed(t)) {
            return "Trình duyệt đã đóng hoặc mất kết nối — không nhận được phản hồi từ trang web.";
        }
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) {
            return t.getClass().getSimpleName();
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("timeout") || lower.contains("hết thời gian chờ")) {
            return "Không nhận được phản hồi từ trang web trong thời gian chờ.";
        }
        return msg.length() > 300 ? msg.substring(0, 300) + "…" : msg;
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
                    .until(d -> {
                        failIfBrowserClosed();
                        try {
                            return ExpectedConditions.visibilityOfElementLocated(by).apply(d);
                        } catch (WebDriverException e) {
                            failIfBrowserClosed(e);
                            throw e;
                        }
                    });
            System.out.println(" ✅ Đã hiển thị: " + description);
        } catch (BrowserClosedException e) {
            throw e;
        } catch (TimeoutException e) {
            failIfBrowserClosed();
            throw new RuntimeException("❌ Hết thời gian chờ: [" + description + "] không hiển thị sau "
                    + timeoutSeconds + "s.");
        }
    }

    /** Chờ mọi element khớp locator không còn hiển thị (wizard chuyển bước). */
    public void waitUntilInvisible(By by, int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ ẩn: " + description);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> {
                        failIfBrowserClosed();
                        return !existsDisplayed(by);
                    });
            System.out.println(" ✅ Đã ẩn: " + description);
        } catch (BrowserClosedException e) {
            throw e;
        } catch (TimeoutException e) {
            failIfBrowserClosed();
            List<String> hints = collectValidationMessages();
            String hint = hints.isEmpty()
                    ? " Kiểm tra validation trên biểu mẫu (UAT có thể chặt hơn dev)."
                    : " Lỗi hiển thị: " + String.join(" | ", hints);
            throw new RuntimeException("❌ Hết thời gian chờ: [" + description + "] vẫn hiển thị sau "
                    + timeoutSeconds + "s." + hint);
        }
    }

    private boolean existsDisplayed(By by) {
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
        } catch (WebDriverException e) {
            failIfBrowserClosed(e);
            throw e;
        }
    }

    /** Gợi ý lỗi validate — dùng khi debug sync / smoke. */
    public String peekValidationHints() {
        List<String> messages = collectValidationMessages();
        if (messages.isEmpty()) {
            return " (không đọc được thông báo lỗi trên biểu mẫu)";
        }
        return " " + String.join(" | ", messages);
    }

    public boolean hasValidationMessages() {
        return !collectValidationMessages().isEmpty();
    }

    /** Validate inline + toast đang hiển thị. */
    public boolean hasSystemFeedback() {
        return !collectSystemFeedbackMessages().isEmpty();
    }

    public List<String> collectToastMessages() {
        List<String> hints = new ArrayList<>();
        for (By selector : TOAST_SELECTORS) {
            collectTexts(driver.findElements(selector), hints);
            if (hints.size() >= 6) {
                break;
            }
        }
        return hints;
    }

    /** Gộp thông báo validate trên biểu mẫu và toast hệ thống. */
    public List<String> collectSystemFeedbackMessages() {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(collectValidationMessages());
        merged.addAll(collectToastMessages());
        return new ArrayList<>(merged);
    }

    /**
     * Chờ chuyển bước — nếu hệ thống báo lỗi thì dừng ngay, ghi log và chụp ảnh.
     */
    public void waitForStepTransition(int stepNumber, String stepName, By marker,
                                      int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ chuyển bước: " + description);
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            failIfBrowserClosed();
            if (existsDisplayed(marker)) {
                System.out.println(" ✅ " + description);
                return;
            }
            List<String> feedback = collectSystemFeedbackMessages();
            if (hasBlockingFeedback(feedback)) {
                failStepWithSystemFeedback(stepNumber, stepName, description, feedback);
            }
            sleepMillis(250);
        }
        List<String> feedback = collectSystemFeedbackMessages();
        if (hasBlockingFeedback(feedback)) {
            failStepWithSystemFeedback(stepNumber, stepName, description, feedback);
        }
        throw new RuntimeException("❌ Hết thời gian chờ: [" + description + "] không hiển thị sau "
                + timeoutSeconds + "s." + peekValidationHints());
    }

    /** Chỉ coi là chặn chuyển bước khi có lỗi bắt buộc — bỏ qua banner VNeID thông tin. */
    private static boolean hasBlockingFeedback(List<String> feedback) {
        if (feedback == null || feedback.isEmpty()) {
            return false;
        }
        for (String msg : feedback) {
            if (msg == null || msg.isBlank()) {
                continue;
            }
            String lower = msg.toLowerCase();
            if (lower.contains("bắt buộc")
                    || lower.contains("vui lòng điền")
                    || lower.contains("không hợp lệ")
                    || lower.contains("phải nhập")
                    || lower.contains("yêu cầu nhập")
                    || lower.contains("chọn tỉnh")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dừng testcase: log message hệ thống, chụp ảnh (giữ toast), báo cáo Extent/Excel.
     */
    public void failStepWithSystemFeedback(int stepNumber, String stepName, String context,
                                           List<String> messages) {
        List<String> feedback = messages == null || messages.isEmpty()
                ? collectSystemFeedbackMessages()
                : messages;
        if (feedback.isEmpty()) {
            feedback = List.of("(không đọc được nội dung thông báo)");
        }
        String stepLabel = "Bước " + stepNumber + " — " + (stepName == null ? "" : stepName.trim());
        String ctx = context == null || context.isBlank() ? "" : context.trim();
        String joined = String.join(" | ", feedback);

        System.out.println(" ❌ " + stepLabel + " — hệ thống trả về: " + joined);
        for (String msg : feedback) {
            String logCtx = ctx.isEmpty() ? stepLabel : stepLabel + " · " + ctx;
            TestActionLog.validation(logCtx, msg);
        }
        TestActionLog.trangThaiBuoc("Thất bại");

        String shot = takeScreenshotPreserveToast();
        if (shot != null) {
            System.out.println(" 📸 Đã chụp ảnh lỗi — " + stepLabel);
        }
        String reportBody = stepLabel
                + (ctx.isEmpty() ? "" : "\nNgữ cảnh: " + ctx)
                + "\nHệ thống trả về: " + joined;
        if (shot != null) {
            ExtentReportManager.logFailWithScreenshot(reportBody, shot);
        } else {
            ExtentReportManager.logFail(reportBody);
        }
        throw new StepBlockedException(stepNumber, stepName, joined, shot);
    }

    public List<String> collectValidationMessages() {
        return collectValidationMessagesInScope(null);
    }

    /** Thu thập thông báo validate/toast trên biểu mẫu (toàn trang hoặc trong scope). */
    public List<String> collectValidationMessagesInScope(String scopeXPath) {
        String scope = scopeXPath == null || scopeXPath.isBlank() ? "" : scopeXPath;
        List<String> hints = new ArrayList<>();
        collectTexts(driver.findElements(By.xpath(buildValidationXPath(scope, false))), hints);
        collectTexts(driver.findElements(By.xpath(buildValidationXPath(scope, true))), hints);
        return hints;
    }

    private static String buildValidationXPath(String scope, boolean keywordHints) {
        String root = scope.isBlank() ? "" : scope;
        if (keywordHints) {
            return root + "//*[self::p or self::span or self::div or self::li]"
                    + "[string-length(normalize-space(.)) > 2 and string-length(normalize-space(.)) < 300"
                    + " and (contains(normalize-space(.), 'Vui lòng')"
                    + " or contains(normalize-space(.), 'vui lòng')"
                    + " or contains(normalize-space(.), 'bắt buộc')"
                    + " or contains(normalize-space(.), 'Bắt buộc')"
                    + " or contains(normalize-space(.), 'không hợp lệ')"
                    + " or contains(normalize-space(.), 'Không hợp lệ')"
                    + " or contains(normalize-space(.), 'yêu cầu nhập')"
                    + " or contains(normalize-space(.), 'phải nhập'))]";
        }
        return root + "//*[contains(@class,'text-red') or contains(@class,'text-destructive')"
                + " or contains(@class,'border-red') or contains(@class,'ring-red')"
                + " or contains(@class,'text-error') or @role='alert' or @aria-invalid='true'"
                + " or contains(@class,'toast') or contains(@class,'Toaster') or contains(@class,'sonner')]"
                + "[string-length(normalize-space(.)) > 0 and string-length(normalize-space(.)) < 300]"
                + " | " + root + "//label/following-sibling::p[string-length(normalize-space(.)) > 0"
                + " and string-length(normalize-space(.)) < 300]"
                + " | " + root + "//label/following-sibling::span[string-length(normalize-space(.)) > 0"
                + " and string-length(normalize-space(.)) < 300]";
    }

    private static void collectTexts(List<WebElement> elements, List<String> hints) {
        for (WebElement el : elements) {
            try {
                if (!el.isDisplayed()) {
                    continue;
                }
                String text = el.getText();
                if (text == null || text.isBlank()) {
                    text = el.getAttribute("textContent");
                }
                if (text == null || text.isBlank()) {
                    continue;
                }
                String trimmed = text.trim().replaceAll("\\s+", " ");
                if (trimmed.length() < 3 || isNoiseValidationText(trimmed)) {
                    continue;
                }
                if (!hints.contains(trimmed)) {
                    hints.add(trimmed);
                }
            } catch (Exception ignored) {
            }
            if (hints.size() >= 8) {
                break;
            }
        }
    }

    private static boolean isNoiseValidationText(String text) {
        String lower = text.toLowerCase();
        return lower.equals("có") || lower.equals("không") || lower.contains("cursor-pointer");
    }

    /** In console + Excel log mọi thông báo validate đang hiển thị. */
    public void logValidationMessages(String context) {
        List<String> messages = collectValidationMessages();
        if (messages.isEmpty()) {
            return;
        }
        String prefix = (context == null || context.isBlank()) ? "Biểu mẫu" : context.trim();
        System.out.println(" ⚠ Validation [" + prefix + "]: " + String.join(" | ", messages));
        for (String msg : messages) {
            TestActionLog.validation(prefix, msg);
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
            WebElement element = waitForDisplayedEnabled(by, WaitConfig.FIELD);
            scrollToElement(element);
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
        List<String> availableOptions = new ArrayList<>();
        boolean isFound = trySelectFromDropdown(dropdownLocator, optionsLocator, expectedText, elementName, availableOptions);
        if (!isFound) {
            isFound = trySelectFromDropdown(dropdownLocator, GLOBAL_DROPDOWN_OPTIONS, expectedText, elementName, availableOptions);
        }
        if (!isFound) {
            throw new RuntimeException("❌ Lỗi dữ liệu/ứng dụng chậm: Không tìm thấy ['" + expectedText + "'] trong [" + elementName
                    + "]. Tuỳ chọn giao diện hiện có: " + availableOptions
                    + ". Cập nhật master-data.properties hoặc chạy đồng bộ dữ liệu gốc.");
        }
    }

    private boolean trySelectFromDropdown(By dropdownLocator, By optionsLocator, String expectedText,
                                          String elementName, List<String> availableOptions) {
        clickElementQuiet(dropdownLocator, elementName);
        sleepMillis(WaitConfig.SETTLE_MS);
        for (int attempt = 0; attempt < 3; attempt++) {
            for (WebElement option : driver.findElements(optionsLocator)) {
                try {
                    if (!option.isDisplayed()) {
                        continue;
                    }
                    String textOnWeb = readElementText(option);
                    if (!textOnWeb.isEmpty() && !availableOptions.contains(textOnWeb)) {
                        availableOptions.add(textOnWeb);
                    }
                    if (optionMatches(expectedText, textOnWeb)) {
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", option);
                        sleepMillis(300);
                        option.click();
                        System.out.println(" ➔ Chọn Dropdown: '" + expectedText + "' tại [" + elementName + "]");
                        TestActionLog.chon(elementName, expectedText);
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            sleepMillis(250);
        }
        dismissOpenDropdownsQuiet();
        return false;
    }

    /** Chờ ít nhất một option hiển thị sau khi mở dropdown (SPA load async). */
    private List<WebElement> waitForVisibleDropdownOptions(By optionsLocator, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> visible = new ArrayList<>();
            for (WebElement option : driver.findElements(optionsLocator)) {
                try {
                    if (!option.isDisplayed()) {
                        continue;
                    }
                    String text = option.getText();
                    if (text == null || text.isBlank()) {
                        text = option.getAttribute("textContent");
                    }
                    if (text != null && !text.trim().isEmpty()) {
                        visible.add(option);
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            if (!visible.isEmpty()) {
                return visible;
            }
            sleepMillis(250);
        }
        return List.of();
    }

    private void dismissOpenDropdownsQuiet() {
        try {
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
            sleepMillis(150);
        } catch (Exception ignored) {
        }
    }

    /** Đóng dropdown/listbox đang mở trước khi chuyển sang khối địa chỉ khác. */
    public void dismissOpenDropdowns() {
        dismissOpenDropdownsQuiet();
    }

    public void selectDropdownWithSearch(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (trySelectDropdownWithSearch(dropdownLocator, searchInputLocator, optionsLocator,
                        expectedText, elementName)) {
                    return;
                }
            } catch (RuntimeException ex) {
                lastError = ex;
            }
            if (attempt < 3) {
                System.out.println(" ⏳ Dropdown [" + elementName + "] chưa có kết quả — thử lại ("
                        + (attempt + 1) + "/3)...");
                dismissOpenDropdownsQuiet();
                sleepMillis(WaitConfig.SETTLE_LONG_MS);
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new RuntimeException("❌ Lỗi dữ liệu: Không tìm thấy ['" + expectedText + "'] trong Dropdown ["
                + elementName + "].");
    }

    private boolean trySelectDropdownWithSearch(By dropdownLocator, By searchInputLocator, By optionsLocator,
                                              String expectedText, String elementName) {
        clickElementQuiet(dropdownLocator, elementName);
        sleepMillis(WaitConfig.SETTLE_MS);
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
            searchInput.clear();
            searchInput.sendKeys(expectedText);
            System.out.println(" ➔ Gõ tìm kiếm Dropdown: '" + expectedText + "'");
            TestActionLog.timKiemDropdown(expectedText);
            sleepMillis(WaitConfig.SETTLE_LONG_MS);
        } catch (Exception ignored) {
        }

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
                    js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", option);
                    option.click();
                    System.out.println(" ➔ Chọn Dropdown (lọc): '" + expectedText + "' tại [" + elementName + "]");
                    TestActionLog.chon(elementName, expectedText);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        if (availableOptions.isEmpty()) {
            return false;
        }
        throw new RuntimeException("❌ Lỗi dữ liệu: Không tìm thấy ['" + expectedText + "'] trong Dropdown [" + elementName
                + "]. Tuỳ chọn giao diện hiện có: " + availableOptions
                + ". Cập nhật master-data.properties hoặc chạy đồng bộ dữ liệu gốc.");
    }

    private static final By GLOBAL_DROPDOWN_OPTIONS =
            By.xpath("//div[@role='listbox']//div[@role='option'] | //div[@role='option']");

    /**
     * Chọn mục đầu tiên có text trong dropdown (dùng khi không biết trước giá trị, vd. Phường/xã sau Tỉnh).
     */
    public void selectFirstDropdownOption(By dropdownLocator, By optionsLocator, String elementName) {
        clickElementQuiet(dropdownLocator, elementName);
        sleepMillis(WaitConfig.SETTLE_MS);
        if (clickFirstOptionInOpenDropdown(GLOBAL_DROPDOWN_OPTIONS, elementName)
                || clickFirstOptionInOpenDropdown(optionsLocator, elementName)) {
            return;
        }
        List<String> texts = collectValidDropdownOptionTexts(GLOBAL_DROPDOWN_OPTIONS);
        if (texts.isEmpty()) {
            texts = collectValidDropdownOptionTexts(optionsLocator);
        }
        throw new RuntimeException("❌ Không chọn được mục nào trong [" + elementName
                + "]. Tuỳ chọn hiện có: " + texts);
    }

    /**
     * Textarea chi tiết địa chỉ (Số nhà, tên đường…) — UAT thường không gắn label sibling.
     */
    public By addressDetailTextareaInScope(String scopeXPath) {
        String scope = scopeXPath == null || scopeXPath.isBlank() ? "" : scopeXPath;
        return By.xpath(scope + "//textarea[contains(@placeholder, 'Số nhà') or contains(@placeholder, 'thôn')"
                + " or contains(@placeholder, 'đường') or contains(@placeholder, 'xóm')]"
                + " | " + scope + "//label[contains(., 'Chi tiết') or contains(., 'số nhà')]/following-sibling::textarea"
                + " | " + scope + "//label[contains(., 'Chi tiết')]/parent::div//textarea"
                + " | " + scope + "//label[contains(., 'Chi tiết')]/following-sibling::div//textarea"
                + " | " + scope + "//label[contains(., 'Địa chỉ')]/following-sibling::textarea"
                + " | " + scope + "//label[contains(., 'Địa chỉ')]/parent::div//textarea"
                + " | " + scope + "//label[contains(., 'Địa chỉ')]/following-sibling::div//textarea"
                + " | " + scope + "//label[contains(., 'Phường') and contains(., 'xã')]/following::textarea[1]");
    }

    /** Textarea chi tiết theo khối địa chỉ (0 = thường trú, 1 = liên lạc…). */
    public By addressDetailTextareaAtBlock(String scopeXPath, int blockIndex) {
        String scope = scopeXPath == null || scopeXPath.isBlank() ? "" : scopeXPath;
        int idx = blockIndex + 1;
        return By.xpath("(" + scope + "//textarea[contains(@placeholder, 'Số nhà') or contains(@placeholder, 'thôn')"
                + " or contains(@placeholder, 'đường') or contains(@placeholder, 'xóm')"
                + " or contains(@placeholder, 'Chi tiết')]"
                + " | " + scope + "//label[contains(., 'Chi tiết') or contains(., 'số nhà')]/following-sibling::textarea"
                + " | " + scope + "//label[contains(., 'Chi tiết')]/parent::div//textarea"
                + " | " + scope + "//label[contains(., 'Chi tiết')]/following-sibling::div//textarea"
                + " | " + scope + "//label[contains(., 'Địa chỉ thường trú')]/following-sibling::textarea"
                + " | " + scope + "//label[contains(., 'Địa chỉ liên lạc')]/following-sibling::textarea)[" + idx + "]");
    }

    /**
     * UAT: dropdown Tỉnh/thành phố + Phường/xã (bỏ qua nếu form cũ không có).
     */
    public void selectAdministrativeAddress(String provinceHint, String wardHint) {
        selectAdministrativeAddressInScope("", provinceHint, wardHint);
    }

    /** Chọn ngẫu nhiên Tỉnh/TP + Phường/Xã trong scope (bỏ qua nếu đã chọn). */
    public void selectAdministrativeAddressInScope(String scopeXPath) {
        selectAdministrativeAddressInScope(scopeXPath, null, null);
    }

    /** Đếm số cặp dropdown Tỉnh/Phường hiển thị trong scope (form có thể có 2 khối địa chỉ). */
    public int countVisibleAddressBlocks(String scopeXPath) {
        String scope = scopeXPath == null ? "" : scopeXPath;
        int count = 0;
        By labels = By.xpath(scope + "//label[contains(., 'Tỉnh') and contains(., 'thành phố')]");
        for (WebElement el : driver.findElements(labels)) {
            try {
                if (el.isDisplayed()) {
                    count++;
                }
            } catch (StaleElementReferenceException ignored) {
            }
        }
        return count;
    }

    /** Chọn Tỉnh/Phường cho một khối địa chỉ theo thứ tự (0 = khối đầu, 1 = khối thứ hai…). */
    public void selectAdministrativeAddressBlockInScope(String scopeXPath, int blockIndex) {
        selectAdministrativeAddressBlockInScope(scopeXPath, blockIndex, null, null);
    }

    public void selectAdministrativeAddressBlockInScope(String scopeXPath, int blockIndex,
                                                        String provinceHint, String wardHint) {
        String scope = scopeXPath == null ? "" : scopeXPath;
        int idx = blockIndex + 1;
        int total = countVisibleAddressBlocks(scope);
        String blockSuffix = total > 1 ? " #" + idx : "";

        By btnTinh = adminDropdownButtonAt(scope, false, idx);
        if (!isElementVisible(btnTinh)) {
            return;
        }
        String tinhName = "Dropdown [Tỉnh / thành phố" + blockSuffix + "]";
        By searchTinh = adminDropdownSearchAt(scope, false, idx);
        By btnPhuong = adminDropdownButtonAt(scope, true, idx);
        String phuongName = "Dropdown [Phường / xã" + blockSuffix + "]";
        boolean provinceChanged = false;
        if (!isAdminDropdownFilledAt(btnTinh)) {
            if (provinceHint != null && !provinceHint.isBlank()) {
                if (!trySelectDropdownOption(btnTinh, searchTinh, GLOBAL_DROPDOWN_OPTIONS, provinceHint, tinhName)) {
                    selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
                }
            } else {
                selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
            }
            provinceChanged = true;
            waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
        } else {
            System.out.println(" ⏩ " + tinhName + " đã có giá trị — bỏ qua.");
        }

        if (!isElementVisible(btnPhuong)) {
            if (hasWardFieldInBlock(scope, idx)) {
                waitUntilVisible(btnPhuong, WaitConfig.FIELD, phuongName);
            } else {
                return;
            }
        }
        By searchPhuong = adminDropdownSearchAt(scope, true, idx);
        boolean needWard = provinceChanged || !isAdminDropdownFilledAt(btnPhuong);
        if (!needWard) {
            System.out.println(" ⏩ " + phuongName + " đã có giá trị — bỏ qua.");
            return;
        }
        if (!provinceChanged && isElementVisible(btnPhuong)) {
            waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
        }
        selectWardWithRetry(btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName,
                provinceHint, wardHint);
    }

    /** Chọn lại phường/xã cho mọi khối địa chỉ (khi VNeID prefill lệch tỉnh). */
    public void forceSelectAdministrativeWardsInScope(String scopeXPath) {
        String scope = scopeXPath == null ? "" : scopeXPath;
        int total = countVisibleAddressBlocks(scope);
        for (int i = 0; i < total; i++) {
            int idx = i + 1;
            String blockSuffix = total > 1 ? " #" + idx : "";
            By btnTinh = adminDropdownButtonAt(scope, false, idx);
            By searchTinh = adminDropdownSearchAt(scope, false, idx);
            String tinhName = "Dropdown [Tỉnh / thành phố" + blockSuffix + "]";
            By btnPhuong = adminDropdownButtonAt(scope, true, idx);
            if (!isElementVisible(btnPhuong)) {
                continue;
            }
            String phuongName = "Dropdown [Phường / xã" + blockSuffix + "]";
            By searchPhuong = adminDropdownSearchAt(scope, true, idx);
            if (!isAdminDropdownFilledAt(btnTinh)) {
                selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
                sleepMillis(WaitConfig.SETTLE_LONG_MS);
            }
            selectWardWithRetry(btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName, null, null);
            sleepMillis(WaitConfig.SETTLE_MS);
        }
    }

    private void selectWardWithRetry(By btnTinh, By searchTinh, String tinhName,
                                     By btnPhuong, By searchPhuong, String phuongName,
                                     String provinceHint, String wardHint) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                if (attempt > 1 || !isElementVisible(btnPhuong) || !isWardDropdownInteractive(btnPhuong)) {
                    waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
                }
                if (wardHint != null && !wardHint.isBlank()) {
                    if (trySelectWardInOpenDropdown(btnPhuong, searchPhuong, wardHint, phuongName)) {
                        return;
                    }
                    selectRandomWardOption(btnPhuong, searchPhuong, phuongName);
                    return;
                }
                selectRandomWardOption(btnPhuong, searchPhuong, phuongName);
                return;
            } catch (RuntimeException e) {
                last = e;
                dismissOpenDropdownsQuiet();
                if (attempt < 2) {
                    System.out.println(" ⏳ " + phuongName + " chưa chọn được — thử lại tỉnh/phường ("
                            + (attempt + 1) + "/2)...");
                    reselectProvinceForWard(btnTinh, searchTinh, tinhName, provinceHint);
                }
            }
        }
        if (last != null) {
            throw last;
        }
    }

    /** Chọn lại tỉnh ngẫu nhiên khác khi phường/xã chưa load (timing/API). */
    private void reselectProvinceForWard(By btnTinh, By searchTinh, String tinhName, String provinceHint) {
        if (!isElementVisible(btnTinh)) {
            return;
        }
        String current = "";
        try {
            current = readElementText(driver.findElement(btnTinh));
        } catch (Exception ignored) {
        }
        String exclude = current;
        if (provinceHint != null && !provinceHint.isBlank()) {
            exclude = provinceHint;
        }
        selectRandomFromCatalogExcluding(btnTinh, searchTinh, FALLBACK_PROVINCES, tinhName, exclude);
        sleepMillis(WaitConfig.SETTLE_LONG_MS);
    }

    /** Chờ dropdown phường/xã sẵn sàng sau khi chọn tỉnh (API async). Thoát ngay khi enabled. */
    private void waitForWardDropdownReady(By wardButton, int timeoutSec) {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (!isElementVisible(wardButton)) {
                    sleepMillis(100);
                    continue;
                }
                if (isAdminDropdownFilledAt(wardButton)) {
                    return;
                }
                WebElement el = driver.findElement(wardButton);
                if (el.isEnabled()) {
                    return;
                }
            } catch (Exception ignored) {
            }
            sleepMillis(100);
        }
    }

    /** Chọn Tỉnh/Phường cho mọi khối địa chỉ còn trống trong scope. */
    public void selectAllAdministrativeAddressBlocksInScope(String scopeXPath) {
        int total = countVisibleAddressBlocks(scopeXPath);
        for (int i = 0; i < total; i++) {
            selectAdministrativeAddressBlockInScope(scopeXPath, i);
            sleepMillis(WaitConfig.SETTLE_ADDRESS_MS);
        }
    }

    /**
     * Hoàn thiện một khối địa chỉ: tỉnh → phường (bắt buộc nếu có) → chi tiết.
     */
    public void ensureAdministrativeAddressBlockInScope(String scopeXPath, int blockIndex, String chiTietValue) {
        ensureAdministrativeAddressBlockInScope(scopeXPath, blockIndex, chiTietValue, null);
    }

    public void ensureAdministrativeAddressBlockInScope(String scopeXPath, int blockIndex,
                                                        String chiTietValue, String logContext) {
        String scope = scopeXPath == null ? "" : scopeXPath;
        int idx = blockIndex + 1;
        int total = effectiveAddressBlockCount(scope);
        if (blockIndex >= total && total > 0) {
            return;
        }
        String ctx = logContext == null || logContext.isBlank() ? "" : " (" + logContext + ")";
        String blockSuffix = total > 1 ? " #" + idx : "";

        focusAddressBlock(scope, blockIndex);

        By btnTinh = adminDropdownButtonAt(scope, false, idx);
        By btnPhuong = adminDropdownButtonAt(scope, true, idx);
        By searchTinh = adminDropdownSearchAt(scope, false, idx);
        By searchPhuong = adminDropdownSearchAt(scope, true, idx);
        String tinhName = "Dropdown [Tỉnh / thành phố" + blockSuffix + "]" + ctx;
        String phuongName = "Dropdown [Phường / xã" + blockSuffix + "]" + ctx;

        boolean hasProvince = existsNow(btnTinh) || isElementVisible(btnTinh);
        if (!hasProvince) {
            fillAddressDetailAtBlock(scope, blockIndex, chiTietValue,
                    "Chi tiết địa chỉ" + blockSuffix + ctx);
            finishAddressBlock();
            return;
        }

        if (!isAdminDropdownFilledAt(btnTinh)) {
            selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
            dismissOpenDropdownsQuiet();
            sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
        }

        ensureWardSelectedBeforeDetail(scope, idx, btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName);

        fillAddressDetailAtBlock(scope, blockIndex, chiTietValue,
                "Chi tiết địa chỉ" + blockSuffix + ctx);
        finishAddressBlock();
    }

    /**
     * Chọn lại tỉnh + phường + chi tiết — dùng sau Người liên quan (UI hay reset state).
     */
    public void forceEnsureAdministrativeAddressBlockInScope(String scopeXPath, int blockIndex,
                                                             String chiTietValue, String logContext) {
        String scope = scopeXPath == null ? "" : scopeXPath;
        int idx = blockIndex + 1;
        String ctx = logContext == null || logContext.isBlank() ? "" : " (" + logContext + ")";
        int total = effectiveAddressBlockCount(scope);
        String blockSuffix = total > 1 ? " #" + idx : "";

        focusAddressBlock(scope, blockIndex);

        By btnTinh = adminDropdownButtonAt(scope, false, idx);
        By btnPhuong = adminDropdownButtonAt(scope, true, idx);
        By searchTinh = adminDropdownSearchAt(scope, false, idx);
        By searchPhuong = adminDropdownSearchAt(scope, true, idx);
        String tinhName = "Dropdown [Tỉnh / thành phố" + blockSuffix + "]" + ctx;
        String phuongName = "Dropdown [Phường / xã" + blockSuffix + "]" + ctx;

        if (!existsNow(btnTinh) && !isElementVisible(btnTinh)) {
            fillAddressDetailAtBlock(scope, blockIndex, chiTietValue,
                    "Chi tiết địa chỉ" + blockSuffix + ctx);
            finishAddressBlock();
            return;
        }

        selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
        dismissOpenDropdownsQuiet();
        sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
        ensureWardSelectedBeforeDetail(scope, idx, btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName);

        fillAddressDetailAtBlock(scope, blockIndex, chiTietValue,
                "Chi tiết địa chỉ" + blockSuffix + ctx);
        finishAddressBlock();
    }

    /** Bắt buộc chọn phường/xã trước chi tiết — tránh bỏ qua khi dropdown render chậm. */
    private void ensureWardSelectedBeforeDetail(String scope, int idx, By btnTinh, By searchTinh, String tinhName,
                                                By btnPhuong, By searchPhuong, String phuongName) {
        waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
        waitForWardFieldInBlock(scope, idx, WaitConfig.WARD_READY);

        boolean wardPresent = isElementVisible(btnPhuong) || existsNow(btnPhuong) || hasWardFieldInBlock(scope, idx);
        if (!wardPresent) {
            return;
        }
        if (!isElementVisible(btnPhuong) && !existsNow(btnPhuong)) {
            try {
                waitUntilVisible(btnPhuong, WaitConfig.WARD_READY, phuongName);
            } catch (RuntimeException ignored) {
                return;
            }
        }
        if (!isAdminDropdownFilledAt(btnPhuong)) {
            selectWardWithRetry(btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName, null, null);
            pauseBetweenAddressSteps();
        }
        if (!isAdminDropdownFilledAt(btnPhuong)) {
            if (!isAdminDropdownFilledAt(btnTinh)) {
                selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
                dismissOpenDropdownsQuiet();
                sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            }
            waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
            selectWardWithRetry(btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName, null, null);
            pauseBetweenAddressSteps();
        }
    }

    /** Chọn phường/xã nếu dropdown có trong scope (sau khi đã chọn tỉnh). */
    private void selectWardIfNeeded(String scope, int idx, By btnTinh, By searchTinh, String tinhName,
                                    By btnPhuong, By searchPhuong, String phuongName) {
        selectWardIfNeeded(scope, idx, btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName, false);
    }

    private void selectWardIfNeeded(String scope, int idx, By btnTinh, By searchTinh, String tinhName,
                                    By btnPhuong, By searchPhuong, String phuongName, boolean force) {
        if (!force && isAdminDropdownFilledAt(btnPhuong)) {
            return;
        }
        ensureWardSelectedBeforeDetail(scope, idx, btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName);
    }

    /** Cuộn tới khối địa chỉ, đóng dropdown — tránh các khối dính nhau khi điền liên tiếp. */
    private void focusAddressBlock(String scope, int blockIndex) {
        dismissOpenDropdownsQuiet();
        sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        int idx = blockIndex + 1;
        By anchor = adminDropdownButtonAt(scope, false, idx);
        if (!existsNow(anchor)) {
            anchor = By.xpath("(" + scope + "//label[contains(., 'Tỉnh') and contains(., 'thành phố')])["
                    + idx + "]");
        }
        if (existsNow(anchor)) {
            scrollToElement(anchor);
        } else if (scope != null && !scope.isBlank()) {
            scrollToElement(By.xpath(scope));
        }
        sleepMillis(WaitConfig.SETTLE_ADDRESS_MS);
    }

    /** Đóng dropdown + nghỉ ngắn giữa tỉnh / phường / chi tiết. */
    private void pauseBetweenAddressSteps() {
        dismissOpenDropdownsQuiet();
        sleepMillis(WaitConfig.SETTLE_ADDRESS_MS);
    }

    /** Kết thúc một khối địa chỉ — tách khỏi khối kế tiếp. */
    private void finishAddressBlock() {
        dismissOpenDropdownsQuiet();
        sleepMillis(WaitConfig.ADDRESS_BLOCK_GAP_MS);
    }

    private int effectiveAddressBlockCount(String scope) {
        int visible = countVisibleAddressBlocks(scope);
        if (visible > 0) {
            return visible;
        }
        if (existsNow(By.xpath(scope + "//label[contains(., 'Tỉnh') and contains(., 'thành phố')]"))) {
            return 1;
        }
        return 0;
    }

    private boolean hasWardFieldInBlock(String scope, int oneBasedIndex) {
        return existsNow(adminDropdownButtonAt(scope, true, oneBasedIndex))
                || existsNow(By.xpath("(" + scope + "//label[contains(., 'Phường') and contains(., 'xã')])["
                + oneBasedIndex + "]"));
    }

    private boolean isWardDropdownInteractive(By wardButton) {
        try {
            if (!isElementVisible(wardButton)) {
                return false;
            }
            return driver.findElement(wardButton).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /** Chờ phường/xã xuất hiện sau khi chọn tỉnh (render/API async). */
    private boolean waitForWardFieldInBlock(String scope, int oneBasedIndex, int timeoutSec) {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasWardFieldInBlock(scope, oneBasedIndex)) {
                return true;
            }
            sleepMillis(200);
        }
        return hasWardFieldInBlock(scope, oneBasedIndex);
    }

    /** Điền chi tiết (số nhà, đường…) sau khi tỉnh/phường đã chọn. */
    public void fillAddressDetailAtBlock(String scopeXPath, int blockIndex, String value, String logLabel) {
        if (value == null || value.isBlank()) {
            return;
        }
        dismissOpenDropdownsQuiet();
        sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        String scope = scopeXPath == null ? "" : scopeXPath;
        By detail = addressDetailTextareaAtBlock(scope, blockIndex);
        if (!existsNow(detail)) {
            detail = addressDetailTextareaInScope(scope);
        }
        if (existsNow(detail)) {
            waitUntilVisible(detail, WaitConfig.FIELD, logLabel);
            scrollToElement(detail);
            sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            setTextWithCheck(detail, value, "Ô nhập [" + logLabel + "]");
            return;
        }
        By legacy = By.xpath(scope + "//label[contains(., 'Địa chỉ') and not(contains(., 'trụ sở'))]"
                + "/following-sibling::textarea | " + scope
                + "//label[contains(., 'Địa chỉ')]/parent::div//textarea");
        if (isElementVisible(legacy)) {
            setTextWithCheck(legacy, value, "Ô nhập [" + logLabel + "]");
        }
    }

    /** Khối địa chỉ đã có tỉnh, phường và chi tiết hợp lệ. */
    public boolean isAdministrativeAddressBlockComplete(String scopeXPath, int blockIndex) {
        String scope = scopeXPath == null ? "" : scopeXPath;
        int total = effectiveAddressBlockCount(scope);
        if (blockIndex < 0 || blockIndex >= total) {
            return true;
        }
        int idx = blockIndex + 1;
        By btnTinh = adminDropdownButtonAt(scope, false, idx);
        By btnPhuong = adminDropdownButtonAt(scope, true, idx);
        if ((existsNow(btnTinh) || isElementVisible(btnTinh)) && !isAdminDropdownFilledAt(btnTinh)) {
            return false;
        }
        if (hasWardFieldInBlock(scope, idx) && !isAdminDropdownFilledAt(btnPhuong)) {
            return false;
        }
        return isAddressDetailFilledInBlock(scope, blockIndex);
    }

    private boolean isAddressDetailFilledInBlock(String scope, int blockIndex) {
        By detail = addressDetailTextareaAtBlock(scope, blockIndex);
        if (!existsNow(detail)) {
            detail = addressDetailTextareaInScope(scope);
        }
        if (!existsNow(detail)) {
            return true;
        }
        try {
            WebElement el = driver.findElement(detail);
            if (!el.isDisplayed()) {
                return true;
            }
            String val = el.getAttribute("value");
            if (val == null || val.isBlank()) {
                val = readElementText(el);
            }
            return isMeaningfulAddressDetail(val);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isMeaningfulAddressDetail(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.length() < 3) {
            return false;
        }
        if (trimmed.chars().distinct().count() <= 2 && trimmed.length() > 6) {
            return false;
        }
        return true;
    }

    public void selectAdministrativeAddressInScope(String scopeXPath, String provinceHint, String wardHint) {
        selectAdministrativeAddressBlockInScope(scopeXPath, 0, provinceHint, wardHint);
    }

    private static By adminDropdownButtonAt(String scope, boolean phuong, int oneBasedIndex) {
        String label = phuong
                ? "contains(., 'Phường') and contains(., 'xã')"
                : "contains(., 'Tỉnh') and contains(., 'thành phố')";
        return By.xpath("(" + scope + "//label[" + label + "]/following-sibling::div//button"
                + " | " + scope + "//label[" + label + "]/following-sibling::button"
                + " | " + scope + "//label[" + label + "]/parent::div//button"
                + " | " + scope + "//label[" + label + "]/ancestor::div[1]//button)["
                + oneBasedIndex + "]");
    }

    private static By adminDropdownSearchAt(String scope, boolean phuong, int oneBasedIndex) {
        String label = phuong
                ? "contains(., 'Phường') and contains(., 'xã')"
                : "contains(., 'Tỉnh') and contains(., 'thành phố')";
        return By.xpath("(" + scope + "//label[" + label + "]/following-sibling::div//input"
                + " | " + scope + "//label[" + label + "]/following-sibling::input"
                + " | " + scope + "//label[" + label + "]/parent::div//input)["
                + oneBasedIndex + "]");
    }

    private boolean isAdminDropdownFilledAt(By button) {
        return !readAdminDropdownValue(button).isBlank();
    }

    private String readAdminDropdownValue(By button) {
        try {
            WebElement el = driver.findElement(button);
            if (!el.isDisplayed()) {
                return "";
            }
            for (WebElement span : el.findElements(By.xpath(".//span[normalize-space(.)!='']"))) {
                try {
                    if (!span.isDisplayed()) {
                        continue;
                    }
                    String t = readElementText(span).trim();
                    if (!t.isEmpty() && !isAdminPlaceholderText(t)) {
                        return t;
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            String text = readElementText(el).trim();
            return isAdminPlaceholderText(text) ? "" : text;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean isAdminPlaceholderText(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.equals("chọn")) {
            return true;
        }
        if (trimmed.startsWith("—") || trimmed.startsWith("-")) {
            return true;
        }
        if (trimmed.contains("— Chọn") || trimmed.contains("- Chọn")) {
            return true;
        }
        return lower.contains("chọn tỉnh") || lower.contains("chọn phường") || lower.contains("chọn xã")
                || lower.contains("chọn phường / xã")
                || lower.contains("chọn tỉnh / thành phố trước");
    }

    /**
     * Chọn ngẫu nhiên dropdown địa giới hành chính.
     * Tỉnh: chọn từ catalog + search (không scrape DOM — tránh stale).
     * Phường: đọc text listbox portal rồi chọn lại bằng tên.
     */
    public void selectRandomDropdownOption(By dropdownLocator, By searchInput, String elementName) {
        if (elementName.contains("Tỉnh")) {
            selectRandomFromCatalog(dropdownLocator, searchInput, FALLBACK_PROVINCES, elementName);
            return;
        }
        selectRandomWardOption(dropdownLocator, searchInput, elementName);
    }

    private void selectRandomFromCatalog(By dropdownLocator, By searchInput, String[] catalog, String elementName) {
        selectRandomFromCatalogExcluding(dropdownLocator, searchInput, catalog, elementName, null);
    }

    private void selectRandomFromCatalogExcluding(By dropdownLocator, By searchInput, String[] catalog,
                                                  String elementName, String exclude) {
        List<String> picks = new ArrayList<>(Arrays.asList(catalog));
        if (exclude != null && !exclude.isBlank()) {
            String ex = exclude.trim();
            picks.removeIf(p -> p.equalsIgnoreCase(ex) || ex.contains(p) || p.contains(ex));
        }
        if (picks.isEmpty()) {
            picks = new ArrayList<>(Arrays.asList(catalog));
        }
        Collections.shuffle(picks);
        RuntimeException last = null;
        for (String pick : picks) {
            try {
                if (searchInput != null && existsNow(searchInput)) {
                    selectDropdownWithSearch(dropdownLocator, searchInput, GLOBAL_DROPDOWN_OPTIONS, pick, elementName);
                } else {
                    selectCustomDropdown(dropdownLocator, GLOBAL_DROPDOWN_OPTIONS, pick, elementName);
                }
                return;
            } catch (RuntimeException e) {
                last = e;
            }
        }
        throw new RuntimeException("❌ Không chọn được tỉnh ngẫu nhiên trong [" + elementName + "].", last);
    }

    /** Mở dropdown phường/xã, chọn trực tiếp trong listbox (không đóng rồi mở lại). */
    private void selectRandomWardOption(By dropdownLocator, By searchInput, String elementName) {
        RuntimeException last = new RuntimeException("Không có option phường/xã khả dụng");
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                if (trySelectRandomWardOnce(dropdownLocator, searchInput, elementName)) {
                    return;
                }
            } catch (RuntimeException e) {
                last = e;
            }
            dismissOpenDropdownsQuiet();
            sleepMillis(WaitConfig.SETTLE_LONG_MS);
            if (attempt < 3) {
                System.out.println(" ⏳ " + elementName + " — thử chọn phường/xã lại (" + (attempt + 1) + "/3)...");
            }
        }
        dismissOpenDropdownsQuiet();
        throw new RuntimeException("❌ Không chọn được phường/xã trong [" + elementName + "].", last);
    }

    private boolean trySelectRandomWardOnce(By dropdownLocator, By searchInput, String elementName) {
        scrollToElement(dropdownLocator);
        clickElementQuiet(dropdownLocator, elementName);
        sleepMillis(WaitConfig.SETTLE_LONG_MS);
        List<String> texts = collectValidDropdownOptionTexts(GLOBAL_DROPDOWN_OPTIONS, WaitConfig.DROPDOWN);
        if (!texts.isEmpty()) {
            List<String> picks = new ArrayList<>(texts);
            Collections.shuffle(picks);
            for (String pick : picks) {
                if (clickOptionInOpenDropdown(GLOBAL_DROPDOWN_OPTIONS, pick, elementName)) {
                    return true;
                }
            }
        }
        String[] probes = {"Phường", "Xã", "Thị trấn", "P.", "X."};
        if (searchInput != null && existsNow(searchInput)) {
            for (String probe : probes) {
                try {
                    WebElement search = driver.findElement(searchInput);
                    search.clear();
                    search.sendKeys(probe);
                    sleepMillis(800);
                    if (clickFirstOptionInOpenDropdown(GLOBAL_DROPDOWN_OPTIONS, elementName)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (clickFirstOptionInOpenDropdown(GLOBAL_DROPDOWN_OPTIONS, elementName)) {
            return true;
        }
        return false;
    }

    private boolean trySelectWardInOpenDropdown(By dropdownLocator, By searchInput, String expected,
                                                String elementName) {
        scrollToElement(dropdownLocator);
        clickElementQuiet(dropdownLocator, elementName);
        sleepMillis(WaitConfig.SETTLE_LONG_MS);
        if (searchInput != null && existsNow(searchInput)) {
            try {
                WebElement search = driver.findElement(searchInput);
                search.clear();
                search.sendKeys(expected);
                sleepMillis(800);
            } catch (Exception ignored) {
            }
        }
        if (clickOptionInOpenDropdown(GLOBAL_DROPDOWN_OPTIONS, expected, elementName)) {
            return true;
        }
        dismissOpenDropdownsQuiet();
        return false;
    }

    private boolean clickOptionInOpenDropdown(By optionsLocator, String expectedText, String elementName) {
        for (int round = 0; round < 4; round++) {
            for (WebElement option : driver.findElements(optionsLocator)) {
                try {
                    if (!option.isDisplayed()) {
                        continue;
                    }
                    String text = readElementText(option);
                    if (!optionMatches(expectedText, text)) {
                        continue;
                    }
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
                    sleepMillis(150);
                    try {
                        option.click();
                    } catch (Exception e) {
                        js.executeScript("arguments[0].click();", option);
                    }
                    System.out.println(" ➔ Chọn Dropdown: '" + text + "' tại [" + elementName + "]");
                    TestActionLog.chon(elementName, text);
                    return true;
                } catch (StaleElementReferenceException ignored) {
                }
            }
            sleepMillis(300);
        }
        return false;
    }

    private boolean clickFirstOptionInOpenDropdown(By optionsLocator, String elementName) {
        for (int round = 0; round < 4; round++) {
            for (WebElement option : driver.findElements(optionsLocator)) {
                try {
                    if (!option.isDisplayed()) {
                        continue;
                    }
                    String text = readElementText(option);
                    if (!isSelectableDropdownText(text)) {
                        continue;
                    }
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("arguments[0].scrollIntoView({block:'center'});", option);
                    sleepMillis(150);
                    try {
                        option.click();
                    } catch (Exception e) {
                        js.executeScript("arguments[0].click();", option);
                    }
                    System.out.println(" ➔ Chọn Dropdown (mục đầu): '" + text + "' tại [" + elementName + "]");
                    TestActionLog.chon(elementName, text);
                    return true;
                } catch (StaleElementReferenceException ignored) {
                }
            }
            sleepMillis(300);
        }
        return false;
    }

    private static final String[] FALLBACK_PROVINCES = {
            "Hà Nội", "Thành phố Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "Thành phố Huế", "Bắc Ninh", "Sơn La", "Khánh Hòa", "Lâm Đồng", "Quảng Ninh"
    };

    private List<String> collectValidDropdownOptionTexts(By optionsLocator) {
        return collectValidDropdownOptionTexts(optionsLocator, 5);
    }

    private List<String> collectValidDropdownOptionTexts(By optionsLocator, int timeoutSec) {
        List<String> texts = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            texts.clear();
            for (WebElement option : driver.findElements(optionsLocator)) {
                try {
                    if (!option.isDisplayed()) {
                        continue;
                    }
                    String trimmed = readElementText(option);
                    if (isSelectableDropdownText(trimmed) && !texts.contains(trimmed)) {
                        texts.add(trimmed);
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            if (!texts.isEmpty()) {
                return texts;
            }
            sleepMillis(250);
        }
        return texts;
    }

    private static String readElementText(WebElement el) {
        String text = el.getText();
        if (text == null || text.isBlank()) {
            text = el.getAttribute("textContent");
        }
        if (text == null) {
            return "";
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    private static boolean isSelectableDropdownText(String text) {
        if (text == null || text.length() < 2) {
            return false;
        }
        if (text.equalsIgnoreCase("Chọn")) {
            return false;
        }
        return !text.startsWith("—") && !text.startsWith("-");
    }

    private boolean trySelectDropdownOption(By dropdown, By search, By options, String expected, String name) {
        try {
            selectDropdownWithSearch(dropdown, search, options, expected, name);
            return true;
        } catch (RuntimeException e) {
            try {
                selectCustomDropdown(dropdown, options, expected, name);
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
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

    /**
     * Chụp viewport giữ nguyên toast/notify (không ESC, không đóng overlay).
     * Dùng ngay sau khi toast Gửi đơn hiện.
     */
    public String takeScreenshotPreserveToast() {
        if (!screenshotsEnabled()) {
            return null;
        }
        try {
            scrollWindowTo(0);
            sleepMillis(150);
            String shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            return (shot == null || shot.isBlank()) ? null : shot;
        } catch (Exception e) {
            System.out.println(" ⚠ Không chụp được ảnh (giữ toast): " + e.getMessage());
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

    /** Tên hiển thị trong báo cáo Excel — file mẫu không dùng tên tiếng Anh. */
    private static String tenTepHienThi(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return fileName;
        }
        String lower = fileName.toLowerCase();
        if ("sample.pdf".equals(lower) || lower.endsWith(".pdf")) {
            return "tệp mẫu.pdf";
        }
        if ("sample.xlsx".equals(lower) || "sample.xls".equals(lower) || lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return "tệp mẫu.xlsx";
        }
        if ("sample.docx".equals(lower) || "sample.doc".equals(lower) || lower.endsWith(".docx") || lower.endsWith(".doc")) {
            return "tệp mẫu.docx";
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

    /** Về document gốc — gọi sau khi thao tác trong iframe. */
    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    /** Chuyển vào iframe đầu tiên khớp locator (URL nhúng /f/:code). */
    public void switchToIframe(By iframeLocator) {
        WebElement frame = waitForDisplayedEnabled(iframeLocator, WaitConfig.DROPDOWN);
        driver.switchTo().frame(frame);
    }

    public boolean existsIframe(By iframeLocator) {
        switchToDefaultContent();
        return existsNow(iframeLocator);
    }
}

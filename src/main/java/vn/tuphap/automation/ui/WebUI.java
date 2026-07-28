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
            // Chỉ fallback JS khi native click thật sự chưa tới (tránh click 2 lần nếu
            // event đã tới React rồi mới ném ElementClickInterceptedException).
            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
            boolean likelyAlreadyClicked = msg.contains("stale")
                    || msg.contains("not attached");
            if (likelyAlreadyClicked) {
                return;
            }
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", element);
        }
    }

    /**
     * Bấm một lần bằng JS — tránh native+JS double-submit (toast lỗi xuất hiện 2 lần).
     */
    public void clickElementOnceJs(By by, String elementName, int timeoutSeconds) {
        try {
            failIfBrowserClosed();
            WebElement element = waitForDisplayedEnabled(by, timeoutSeconds);
            scrollToElement(element);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
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

    /**
     * Toggle UAT dạng {@code div.cursor-pointer} + vòng tròn {@code rounded-full}
     * (không có {@code input[type=checkbox]} / {@code label}).
     */
    public void ensureCustomToggleSelected(By optionBy, boolean wantSelected, String elementName) {
        if (!existsNow(optionBy)) {
            System.out.println(" ⏩ Bỏ qua [" + elementName + "] — không có trên biểu mẫu.");
            TestActionLog.boQua(elementName, "Không có trên biểu mẫu");
            return;
        }
        boolean selected = isCustomToggleSelected(optionBy);
        if (selected == wantSelected) {
            System.out.println(" ⏩ [" + elementName + "] đã "
                    + (wantSelected ? "chọn" : "bỏ chọn") + " sẵn.");
            return;
        }
        clickElement(optionBy, (wantSelected ? "Chọn" : "Bỏ chọn") + " " + elementName);
        sleepMillis(WaitConfig.SETTLE_MS);
    }

    /** Đã chọn? — ưu tiên input/aria; fallback vòng tròn (có chấm trong / không còn border xám nhạt). */
    public boolean isCustomToggleSelected(By optionBy) {
        try {
            WebElement root = driver.findElement(optionBy);
            List<WebElement> inputs = root.findElements(By.xpath(
                    ".//input[@type='checkbox' or @type='radio']"));
            if (!inputs.isEmpty()) {
                return isCheckboxChecked(inputs.get(0));
            }
            String aria = root.getAttribute("aria-checked");
            if ("true".equalsIgnoreCase(aria)) {
                return true;
            }
            if ("false".equalsIgnoreCase(aria)) {
                return false;
            }
            String dataState = root.getAttribute("data-state");
            if (dataState != null) {
                String ds = dataState.trim().toLowerCase(Locale.ROOT);
                if (ds.equals("checked") || ds.equals("on") || ds.equals("true")) {
                    return true;
                }
                if (ds.equals("unchecked") || ds.equals("off") || ds.equals("false")) {
                    return false;
                }
            }
            List<WebElement> circles = root.findElements(By.xpath(
                    ".//span[contains(@class,'rounded-full') and (contains(@class,'w-[18px]') or contains(@class,'h-[18px]'))]"
                            + " | .//span[contains(@class,'rounded-full')][1]"));
            if (circles.isEmpty()) {
                return false;
            }
            WebElement circle = circles.get(0);
            if (!circle.findElements(By.xpath("./*")).isEmpty()) {
                return true;
            }
            String cls = circle.getAttribute("class");
            if (cls == null) {
                return false;
            }
            String lower = cls.toLowerCase(Locale.ROOT);
            // Unselected mẫu UAT: border-[#C9D4E1] bg-white, không có chấm trong
            boolean looksEmpty = lower.contains("c9d4e1") && lower.contains("bg-white");
            if (looksEmpty) {
                return false;
            }
            // Selected: viền/nền khác xám nhạt, hoặc class checked/selected/primary
            return true;
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
            clearEditable(element);
            typePreservingSpaces(element, value);
            System.out.println(" ➔ Điền: '" + value + "' vào [" + elementName + "]");
            TestActionLog.dien(elementName, value);
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception e) {
            failIfBrowserClosed(e);
            throw new RuntimeException("❌ Lỗi: Không tìm thấy ô [" + elementName + "] để điền '" + value + "'");
        }
    }

    /** Xóa nội dung ô — Ctrl+A + Delete ổn định hơn clear() với React/controlled input. */
    private void clearEditable(WebElement element) {
        try {
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        } catch (Exception ignored) {
            try {
                element.clear();
            } catch (Exception ignored2) {
                // ignore
            }
        }
    }

    /**
     * Điền text giữ khoảng trắng. Một số textarea UAT (địa chỉ chi tiết) nuốt space từ sendKeys
     * → hiện "127LêLợi" dù log vẫn có dấu cách. Fallback: JS native setter, rồi NBSP.
     */
    private void typePreservingSpaces(WebElement element, String value) {
        if (value == null) {
            return;
        }
        if (!value.contains(" ")) {
            element.sendKeys(value);
            return;
        }

        element.sendKeys(value);
        if (!spacesLost(value, readInputValue(element))) {
            return;
        }

        setNativeInputValue(element, value);
        if (!spacesLost(value, readInputValue(element))) {
            return;
        }

        // Ô/UAT đôi khi strip ASCII space — NBSP vẫn hiện khoảng trắng trên UI.
        setNativeInputValue(element, value.replace(' ', '\u00A0'));
    }

    private String readInputValue(WebElement el) {
        try {
            String val = el.getAttribute("value");
            if (val != null && !val.isEmpty()) {
                return val;
            }
        } catch (Exception ignored) {
            // fall through
        }
        try {
            String prop = el.getDomProperty("value");
            return prop == null ? "" : prop;
        } catch (Exception e) {
            return "";
        }
    }

    private static boolean spacesLost(String expected, String actual) {
        if (expected == null || !expected.contains(" ") || actual == null) {
            return false;
        }
        String actualNorm = actual.replace('\u00A0', ' ');
        if (actualNorm.contains(" ")) {
            return false;
        }
        return expected.replace(" ", "").equals(actualNorm.replace(" ", ""));
    }

    /** Gán value kiểu React-safe (native setter + input/change) — giữ nguyên khoảng trắng. */
    private void setNativeInputValue(WebElement element, String value) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(
                "var el = arguments[0], val = arguments[1];"
                        + "el.focus();"
                        + "var tag = (el.tagName || '').toLowerCase();"
                        + "var proto = tag === 'textarea'"
                        + "  ? window.HTMLTextAreaElement.prototype"
                        + "  : window.HTMLInputElement.prototype;"
                        + "var desc = Object.getOwnPropertyDescriptor(proto, 'value');"
                        + "var last = el.value;"
                        + "if (desc && desc.set) { desc.set.call(el, val); } else { el.value = val; }"
                        + "var tracker = el._valueTracker;"
                        + "if (tracker) { tracker.setValue(last); }"
                        + "el.dispatchEvent(new Event('input', { bubbles: true }));"
                        + "el.dispatchEvent(new Event('change', { bubbles: true }));",
                element, value);
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
     * Chờ chuyển bước.
     * <ul>
     *   <li>Lỗi chặn luồng → chờ thêm {@link WaitConfig#BLOCKING_GRACE_SEC}s, vẫn không được thì
     *       chụp ảnh và dừng case.</li>
     *   <li>Thông báo không chặn (vd. VNeID) → chụp ảnh, ghi log, đi tiếp.</li>
     * </ul>
     */
    public void waitForStepTransition(int stepNumber, String stepName, By marker,
                                      int timeoutSeconds, String description) {
        System.out.println(" ⏳ Chờ chuyển bước: " + description);
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        java.util.Set<String> softNoted = new java.util.LinkedHashSet<>();
        while (System.currentTimeMillis() < deadline) {
            failIfBrowserClosed();
            if (existsDisplayed(marker)) {
                System.out.println(" ✅ " + description);
                return;
            }
            List<String> feedback = collectSystemFeedbackMessages();
            if (hasBlockingFeedback(feedback)) {
                waitGraceThenFailIfStillBlocked(stepNumber, stepName, description, marker, feedback);
                return;
            }
            noteSoftFeedbackAndContinue(stepNumber, stepName, description, feedback, softNoted);
            sleepMillis(250);
        }
        List<String> feedback = collectSystemFeedbackMessages();
        if (hasBlockingFeedback(feedback)) {
            waitGraceThenFailIfStillBlocked(stepNumber, stepName, description, marker, feedback);
            return;
        }
        noteSoftFeedbackAndContinue(stepNumber, stepName, description, feedback, softNoted);
        // Hết thời gian chờ marker = không tiếp tục được → grace 10s rồi dừng + chụp ảnh.
        System.out.println(" ⚠ Chưa chuyển bước — chờ thêm " + WaitConfig.BLOCKING_GRACE_SEC
                + "s trước khi dừng case...");
        long graceDeadline = System.currentTimeMillis() + WaitConfig.BLOCKING_GRACE_SEC * 1000L;
        while (System.currentTimeMillis() < graceDeadline) {
            failIfBrowserClosed();
            if (existsDisplayed(marker)) {
                System.out.println(" ✅ " + description + " (sau khi chờ thêm)");
                return;
            }
            sleepMillis(250);
        }
        List<String> finalFeedback = collectSystemFeedbackMessages();
        if (finalFeedback.isEmpty()) {
            finalFeedback = List.of("Hết thời gian chờ: chưa chuyển sang bước tiếp theo sau "
                    + timeoutSeconds + "+" + WaitConfig.BLOCKING_GRACE_SEC + "s"
                    + peekValidationHints());
        }
        failStepWithSystemFeedback(stepNumber, stepName, description, finalFeedback);
    }

    /**
     * Lỗi chặn: chụp ảnh ngay (giữ toast), rồi grace ngắn — trừ lỗi eform chắc chắn thì fail ngay.
     */
    private void waitGraceThenFailIfStillBlocked(int stepNumber, String stepName, String description,
                                                 By marker, List<String> feedback) {
        // Chụp ngay khi toast còn trên màn — grace 10s sẽ làm toast biến mất.
        String earlyShot = takeScreenshotPreserveToast();
        if (earlyShot != null) {
            System.out.println(" 📸 Đã chụp ảnh lỗi (giữ toast) ngay khi phát hiện chặn luồng.");
        }

        if (isImmediateEformFailure(feedback)) {
            System.out.println(" ⚠ Lỗi eform — fail ngay (không chờ grace): "
                    + String.join(" | ", feedback));
            failStepWithSystemFeedback(stepNumber, stepName, description, feedback, earlyShot);
            return;
        }

        System.out.println(" ⚠ Lỗi chặn luồng — chờ thêm " + WaitConfig.BLOCKING_GRACE_SEC
                + "s: " + String.join(" | ", feedback));
        long graceDeadline = System.currentTimeMillis() + WaitConfig.BLOCKING_GRACE_SEC * 1000L;
        while (System.currentTimeMillis() < graceDeadline) {
            failIfBrowserClosed();
            if (existsDisplayed(marker)) {
                System.out.println(" ✅ " + description + " (hết lỗi sau khi chờ)");
                noteSoftFeedbackAndContinue(stepNumber, stepName, description, feedback,
                        new java.util.LinkedHashSet<>());
                return;
            }
            sleepMillis(250);
        }
        failStepWithSystemFeedback(stepNumber, stepName, description, feedback, earlyShot);
    }

    /** Eform/host báo chưa phản hồi / bắt bấm Gửi trong form — coi là eform lỗi, fail ngay. */
    private static boolean isImmediateEformFailure(List<String> feedback) {
        if (feedback == null) {
            return false;
        }
        for (String msg : feedback) {
            if (msg == null) {
                continue;
            }
            String lower = msg.toLowerCase(Locale.ROOT);
            if (lower.contains("chưa phản hồi")
                    || lower.contains("gửi ngay trong biểu mẫu")
                    || lower.contains("biểu mẫu chưa")) {
                return true;
            }
        }
        return false;
    }

    /** Thông báo không chặn → chụp ảnh + ghi log, không dừng case. */
    private void noteSoftFeedbackAndContinue(int stepNumber, String stepName, String context,
                                             List<String> feedback, java.util.Set<String> alreadyNoted) {
        if (feedback == null || feedback.isEmpty()) {
            return;
        }
        List<String> soft = new ArrayList<>();
        for (String msg : feedback) {
            if (msg == null || msg.isBlank() || isBlockingMessage(msg)) {
                continue;
            }
            if (alreadyNoted.add(msg)) {
                soft.add(msg);
            }
        }
        if (soft.isEmpty()) {
            return;
        }
        String stepLabel = "Bước " + stepNumber + " — " + (stepName == null ? "" : stepName.trim());
        String ctx = context == null || context.isBlank() ? "" : context.trim();
        String joined = String.join(" | ", soft);
        System.out.println(" ⚠ Thông báo (không chặn, đi tiếp) [" + stepLabel + "]: " + joined);
        for (String msg : soft) {
            TestActionLog.validation(ctx.isEmpty() ? stepLabel : stepLabel + " · " + ctx, msg);
        }
        String shot = takeScreenshotPreserveToast();
        String reportBody = stepLabel
                + (ctx.isEmpty() ? "" : "\nNgữ cảnh: " + ctx)
                + "\nThông báo (không chặn): " + joined;
        if (shot != null) {
            System.out.println(" 📸 Đã chụp ảnh thông báo — đi tiếp");
            ExtentReportManager.logWarningWithScreenshot(reportBody, shot);
        } else {
            ExtentReportManager.logWarning(reportBody);
        }
    }

    /** Chỉ coi là chặn chuyển bước khi có lỗi bắt buộc — bỏ qua banner VNeID thông tin. */
    private static boolean hasBlockingFeedback(List<String> feedback) {
        if (feedback == null || feedback.isEmpty()) {
            return false;
        }
        for (String msg : feedback) {
            if (isBlockingMessage(msg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockingMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        // Banner thông tin VNeID / lỗi lưu định danh (không chặn wizard).
        if (lower.contains("vneid")
                || (lower.contains("định danh") && lower.contains("điền sẵn"))
                || (lower.contains("định danh") && lower.contains("thất bại"))) {
            return false;
        }
        return lower.contains("bắt buộc")
                || lower.contains("vui lòng điền")
                || lower.contains("vui lòng chọn")
                || lower.contains("vui lòng bấm")
                || lower.contains("chưa phản hồi")
                || lower.contains("gửi ngay trong biểu mẫu")
                || lower.contains("không hợp lệ")
                || lower.contains("phải nhập")
                || lower.contains("yêu cầu nhập")
                || lower.contains("chọn tỉnh")
                || lower.contains("chọn phường")
                || lower.contains("có lỗi xảy ra")
                || lower.contains("thất bại")
                || lower.contains("không thành công")
                || lower.contains("server error")
                || lower.contains("internal server");
    }

    /**
     * Dừng testcase: log message hệ thống, chụp ảnh (giữ toast), báo cáo Extent/Excel.
     */
    public void failStepWithSystemFeedback(int stepNumber, String stepName, String context,
                                           List<String> messages) {
        failStepWithSystemFeedback(stepNumber, stepName, context, messages, null);
    }

    /**
     * @param preCapturedShot base64 đã chụp khi toast còn hiện (ưu tiên hơn chụp lại sau).
     */
    public void failStepWithSystemFeedback(int stepNumber, String stepName, String context,
                                           List<String> messages, String preCapturedShot) {
        List<String> feedback = messages == null || messages.isEmpty()
                ? collectSystemFeedbackMessages()
                : messages;
        if (feedback.isEmpty()) {
            feedback = List.of("(không đọc được nội dung thông báo)");
        }
        String stepLabel = "Bước " + stepNumber + " — " + (stepName == null ? "" : stepName.trim());
        String ctx = context == null || context.isBlank() ? "" : context.trim();
        String joined = String.join(" | ", feedback);

        // Console + Extent: chỉ qua logFail* (tránh in ❌ trùng với TestListener / logFail).
        for (String msg : feedback) {
            String logCtx = ctx.isEmpty() ? stepLabel : stepLabel + " · " + ctx;
            TestActionLog.validation(logCtx, msg);
        }
        TestActionLog.trangThaiBuoc("Thất bại");

        String shot = (preCapturedShot != null && !preCapturedShot.isBlank())
                ? preCapturedShot
                : takeScreenshotPreserveToast();
        if (shot != null) {
            System.out.println(" 📸 Đã chụp ảnh lỗi — " + stepLabel
                    + (preCapturedShot != null ? " (toast lúc phát hiện)" : ""));
        }
        String reportBody = stepLabel
                + (ctx.isEmpty() ? "" : "\nNgữ cảnh: " + ctx)
                + "\nHệ thống trả về: " + joined;
        // Extent một lần duy nhất — TestListener không logFail lại với StepBlockedException.
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

    /** In console + Excel; thông báo không chặn thì chụp ảnh và đi tiếp. */
    public void logValidationMessages(String context) {
        List<String> messages = collectSystemFeedbackMessages();
        if (messages.isEmpty()) {
            messages = collectValidationMessages();
        }
        if (messages.isEmpty()) {
            return;
        }
        String prefix = (context == null || context.isBlank()) ? "Biểu mẫu" : context.trim();
        List<String> blocking = new ArrayList<>();
        List<String> soft = new ArrayList<>();
        for (String msg : messages) {
            if (isBlockingMessage(msg)) {
                blocking.add(msg);
            } else {
                soft.add(msg);
            }
        }
        if (!soft.isEmpty()) {
            System.out.println(" ⚠ Thông báo (không chặn) [" + prefix + "]: " + String.join(" | ", soft));
            for (String msg : soft) {
                TestActionLog.validation(prefix, msg);
            }
            String shot = takeScreenshotPreserveToast();
            String body = prefix + "\nThông báo (không chặn): " + String.join(" | ", soft);
            if (shot != null) {
                System.out.println(" 📸 Đã chụp ảnh thông báo — đi tiếp");
                ExtentReportManager.logWarningWithScreenshot(body, shot);
            } else {
                ExtentReportManager.logWarning(body);
            }
        }
        if (!blocking.isEmpty()) {
            System.out.println(" ⚠ Validation chặn [" + prefix + "]: " + String.join(" | ", blocking));
            for (String msg : blocking) {
                TestActionLog.validation(prefix, msg);
            }
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
        return isElementEnabledNow(by);
    }

    /** Kiểm tra enabled ngay — không chờ 5s khi element không có. */
    public boolean isElementEnabledNow(By by) {
        failIfBrowserClosed();
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            for (WebElement element : driver.findElements(by)) {
                try {
                    if (element.isDisplayed()) {
                        return element.isEnabled();
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            return false;
        } catch (WebDriverException e) {
            failIfBrowserClosed(e);
            return false;
        }
    }

    public void setTextWithCheck(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không nhập");
            return;
        }
        // existsNow — không isElementVisible (chờ tới 5s) khi ô ẩn → tránh lúc nhanh lúc chậm.
        if (!existsNow(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do biểu mẫu ẩn.");
            TestActionLog.boQua(elementName, "Biểu mẫu ẩn / không có trên giao diện");
            return;
        }
        if (!isElementEnabledNow(by)) {
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
        if (!existsNow(by)) {
            System.out.println(" ⏩ Bỏ qua: [" + elementName + "] do biểu mẫu ẩn.");
            TestActionLog.boQua(elementName, "Biểu mẫu ẩn / không có trên giao diện");
            return;
        }
        if (!isElementEnabledNow(by)) {
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
        sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        for (int attempt = 0; attempt < 2; attempt++) {
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
                        js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", option);
                        option.click();
                        System.out.println(" ➔ Chọn Dropdown: '" + expectedText + "' tại [" + elementName + "]");
                        TestActionLog.chon(elementName, expectedText);
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            sleepMillis(WaitConfig.SETTLE_SHORT_MS);
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
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                if (trySelectDropdownWithSearch(dropdownLocator, searchInputLocator, optionsLocator,
                        expectedText, elementName)) {
                    return;
                }
            } catch (RuntimeException ex) {
                lastError = ex;
            }
            if (attempt < 2) {
                System.out.println(" ⏳ Dropdown [" + elementName + "] chưa có kết quả — thử lại ("
                        + (attempt + 1) + "/2)...");
                dismissOpenDropdownsQuiet();
                sleepMillis(WaitConfig.SETTLE_MS);
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
        sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
            searchInput.clear();
            searchInput.sendKeys(expectedText);
            System.out.println(" ➔ Gõ tìm kiếm Dropdown: '" + expectedText + "'");
            TestActionLog.timKiemDropdown(expectedText);
            sleepMillis(WaitConfig.SETTLE_MS);
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
     * Thẻ địa chỉ UAT (bước 2/3 mọi loại đơn): tiêu đề Địa chỉ… + Tỉnh/TP + Phường/xã + Chi tiết.
     * Markup: {@code rounded + border} card với font-semibold "Địa chỉ thường trú|liên lạc|…".
     */
    private static final String ADDRESS_CARD =
            "//div[contains(@class,'rounded') and contains(@class,'border')]"
                    + "[.//*[contains(@class,'font-semibold') and ("
                    + "contains(normalize-space(.), 'Địa chỉ thường trú')"
                    + " or contains(normalize-space(.), 'Địa chỉ liên lạc')"
                    + " or contains(normalize-space(.), 'Địa chỉ nơi cư trú')"
                    + " or contains(normalize-space(.), 'Địa chỉ trụ sở')"
                    + " or contains(normalize-space(.), 'Nơi ở hiện tại')"
                    + " or starts-with(normalize-space(.), 'Địa chỉ')"
                    + ")]"
                    + " and .//label[contains(., 'Tỉnh') and contains(., 'thành phố')]"
                    + " and (.//label[contains(., 'Chi tiết') or contains(., 'số nhà')]"
                    + " or .//textarea[contains(@placeholder, 'Số nhà') or contains(@placeholder, 'đường')"
                    + " or contains(@placeholder, 'Nguyễn Huệ')])]";

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

    /** Textarea chi tiết theo thẻ địa chỉ (0 = thường trú, 1 = liên lạc…). */
    public By addressDetailTextareaAtBlock(String scopeXPath, int blockIndex) {
        String card = addressCardScope(scopeXPath, blockIndex);
        if (card != null) {
            return By.xpath(card + "//textarea[contains(@placeholder, 'Số nhà') or contains(@placeholder, 'đường')"
                    + " or contains(@placeholder, 'Nguyễn Huệ') or contains(@placeholder, 'Chi tiết')]"
                    + " | " + card + "//label[contains(., 'Chi tiết') or contains(., 'số nhà')]/following-sibling::textarea"
                    + " | " + card + "//label[contains(., 'Chi tiết')]/parent::div//textarea");
        }
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

    /** XPath một thẻ địa chỉ trong parent; null nếu UI chưa dùng card này. */
    private String addressCardScope(String parentScope, int zeroBasedIndex) {
        String parent = parentScope == null ? "" : parentScope;
        String cardXpath = "(" + parent + ADDRESS_CARD + ")[" + (zeroBasedIndex + 1) + "]";
        try {
            if (existsNow(By.xpath(cardXpath))) {
                return cardXpath;
            }
        } catch (RuntimeException ignored) {
        }
        return null;
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

    /** Đếm thẻ địa chỉ (ưu tiên) hoặc cặp Tỉnh/Phường trong scope. */
    public int countVisibleAddressBlocks(String scopeXPath) {
        String scope = scopeXPath == null ? "" : scopeXPath;
        int cards = 0;
        for (WebElement el : driver.findElements(By.xpath(scope + ADDRESS_CARD))) {
            try {
                if (el.isDisplayed()) {
                    cards++;
                }
            } catch (StaleElementReferenceException ignored) {
            }
        }
        if (cards > 0) {
            return cards;
        }
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
        String parent = scopeXPath == null ? "" : scopeXPath;
        int total = countVisibleAddressBlocks(parent);
        String blockSuffix = total > 1 ? " #" + (blockIndex + 1) : "";
        String card = addressCardScope(parent, blockIndex);
        String scope = card != null ? card : parent;
        int idx = card != null ? 1 : blockIndex + 1;

        By btnTinh = adminDropdownButtonAt(scope, false, idx);
        if (!existsNow(btnTinh)) {
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

        if (!existsNow(btnPhuong)) {
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
        if (!provinceChanged && existsNow(btnPhuong)) {
            waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
        }
        // wardHint faker thường lệch tỉnh đã chọn → bỏ qua, chọn phường random 1 lần.
        selectWardWithRetry(btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName,
                provinceHint, null);
    }

    /** Chọn lại phường/xã cho mọi khối địa chỉ (khi VNeID prefill lệch tỉnh). */
    public void forceSelectAdministrativeWardsInScope(String scopeXPath) {
        String parent = scopeXPath == null ? "" : scopeXPath;
        int total = countVisibleAddressBlocks(parent);
        for (int i = 0; i < total; i++) {
            String blockSuffix = total > 1 ? " #" + (i + 1) : "";
            String card = addressCardScope(parent, i);
            String scope = card != null ? card : parent;
            int idx = card != null ? 1 : i + 1;
            By btnTinh = adminDropdownButtonAt(scope, false, idx);
            By searchTinh = adminDropdownSearchAt(scope, false, idx);
            String tinhName = "Dropdown [Tỉnh / thành phố" + blockSuffix + "]";
            By btnPhuong = adminDropdownButtonAt(scope, true, idx);
            if (!existsNow(btnPhuong)) {
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
                if (attempt > 1 || !existsNow(btnPhuong) || !isWardDropdownInteractive(btnPhuong)) {
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
        if (!existsNow(btnTinh)) {
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
        String parentScope = scopeXPath == null ? "" : scopeXPath;
        int total = effectiveAddressBlockCount(parentScope);
        if (blockIndex >= total && total > 0) {
            return;
        }
        String ctx = logContext == null || logContext.isBlank() ? "" : " (" + logContext + ")";
        String blockSuffix = total > 1 ? " #" + (blockIndex + 1) : "";

        // Ưu tiên neo đúng thẻ "Địa chỉ …" (Tỉnh + Phường + Chi tiết) — có ở hầu hết bước 2/3.
        String card = addressCardScope(parentScope, blockIndex);
        String scope = card != null ? card : parentScope;
        int idx = card != null ? 1 : blockIndex + 1;

        if (isAdministrativeAddressBlockComplete(parentScope, blockIndex)) {
            System.out.println(" ⏩ Chi tiết địa chỉ" + blockSuffix + ctx + " đã đủ — bỏ qua.");
            return;
        }

        focusAddressBlock(parentScope, blockIndex);

        By btnTinh = adminDropdownButtonAt(scope, false, idx);
        By btnPhuong = adminDropdownButtonAt(scope, true, idx);
        By searchTinh = adminDropdownSearchAt(scope, false, idx);
        By searchPhuong = adminDropdownSearchAt(scope, true, idx);
        String tinhName = "Dropdown [Tỉnh / thành phố" + blockSuffix + "]" + ctx;
        String phuongName = "Dropdown [Phường / xã" + blockSuffix + "]" + ctx;

        String provinceHint = extractProvinceHint(chiTietValue);
        // Không dùng ward hint từ faker — tên phường trong chuỗi chi tiết thường không có trong dropdown tỉnh đã chọn
        // → mở dropdown 2 lần (thử hint fail rồi chọn random).

        if (!existsNow(btnTinh)) {
            fillAddressDetailAtBlock(parentScope, blockIndex, chiTietValue,
                    "Chi tiết địa chỉ" + blockSuffix + ctx);
            finishAddressBlock();
            return;
        }

        if (!isAdminDropdownFilledAt(btnTinh)) {
            boolean selected = false;
            if (provinceHint != null && !provinceHint.isBlank()) {
                selected = trySelectDropdownOption(btnTinh, searchTinh, GLOBAL_DROPDOWN_OPTIONS,
                        provinceHint, tinhName);
            }
            if (!selected) {
                selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
            }
            dismissOpenDropdownsQuiet();
            sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
        } else {
            System.out.println(" ⏩ " + tinhName + " đã có giá trị — bỏ qua.");
        }

        ensureWardSelectedBeforeDetail(scope, idx, btnTinh, searchTinh, tinhName,
                btnPhuong, searchPhuong, phuongName, null);

        fillAddressDetailAtBlock(parentScope, blockIndex, chiTietValue,
                "Chi tiết địa chỉ" + blockSuffix + ctx);
        finishAddressBlock();
    }

    /** Lấy gợi ý tỉnh/TP từ đoạn cuối địa chỉ chi tiết (vd. "... , Hà Nội"). */
    static String extractProvinceHint(String diaChi) {
        if (diaChi == null || diaChi.isBlank()) {
            return null;
        }
        String[] parts = diaChi.split(",");
        if (parts.length == 0) {
            return null;
        }
        String last = parts[parts.length - 1].trim()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ");
        if (last.isBlank()) {
            return null;
        }
        return last.replaceFirst("(?i)^TP\\.\\s*", "")
                .replaceFirst("(?i)^Thành phố\\s+", "")
                .trim();
    }

    /** Lấy gợi ý phường/xã từ địa chỉ (đoạn chứa Phường/Xã). */
    static String extractWardHint(String diaChi) {
        if (diaChi == null || diaChi.isBlank()) {
            return null;
        }
        for (String part : diaChi.split(",")) {
            String p = part.trim();
            if (p.regionMatches(true, 0, "Phường", 0, "Phường".length())
                    || p.regionMatches(true, 0, "Xã", 0, "Xã".length())
                    || p.toLowerCase(Locale.ROOT).startsWith("phuong")
                    || p.toLowerCase(Locale.ROOT).startsWith("xa ")) {
                return p;
            }
        }
        return null;
    }

    /**
     * Hoàn thiện địa chỉ — chọn lại phần còn thiếu (không ép chọn lại tỉnh/phường đã có).
     * Tên force* giữ để tương thích gọi cũ; hành vi giống ensure (tránh chọn đi chọn lại).
     */
    public void forceEnsureAdministrativeAddressBlockInScope(String scopeXPath, int blockIndex,
                                                             String chiTietValue, String logContext) {
        if (isAdministrativeAddressBlockComplete(scopeXPath, blockIndex)) {
            String ctx = logContext == null || logContext.isBlank() ? "" : " (" + logContext + ")";
            System.out.println(" ⏩ Chi tiết địa chỉ" + ctx + " đã đủ — bỏ qua chọn lại.");
            return;
        }
        ensureAdministrativeAddressBlockInScope(scopeXPath, blockIndex, chiTietValue, logContext);
    }

    /** Bắt buộc chọn phường/xã trước chi tiết — tránh bỏ qua khi dropdown render chậm. */
    private void ensureWardSelectedBeforeDetail(String scope, int idx, By btnTinh, By searchTinh, String tinhName,
                                                By btnPhuong, By searchPhuong, String phuongName) {
        ensureWardSelectedBeforeDetail(scope, idx, btnTinh, searchTinh, tinhName,
                btnPhuong, searchPhuong, phuongName, null);
    }

    private void ensureWardSelectedBeforeDetail(String scope, int idx, By btnTinh, By searchTinh, String tinhName,
                                                By btnPhuong, By searchPhuong, String phuongName,
                                                String wardHint) {
        waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
        waitForWardFieldInBlock(scope, idx, WaitConfig.WARD_READY);

        boolean wardPresent = existsNow(btnPhuong) || hasWardFieldInBlock(scope, idx);
        if (!wardPresent) {
            return;
        }
        if (!existsNow(btnPhuong)) {
            try {
                waitUntilVisible(btnPhuong, WaitConfig.WARD_READY, phuongName);
            } catch (RuntimeException ignored) {
                return;
            }
        }
        if (isAdminDropdownFilledAt(btnPhuong)) {
            return;
        }
        selectWardWithRetry(btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName, null, wardHint);
        pauseBetweenAddressSteps();
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
        String card = addressCardScope(scope, blockIndex);
        String effective = card != null ? card : (scope == null ? "" : scope);
        int idx = card != null ? 1 : blockIndex + 1;
        By anchor = adminDropdownButtonAt(effective, false, idx);
        if (!existsNow(anchor)) {
            anchor = By.xpath("(" + effective + "//label[contains(., 'Tỉnh') and contains(., 'thành phố')])["
                    + idx + "]");
        }
        if (existsNow(anchor)) {
            scrollToElement(anchor);
        } else if (card != null) {
            scrollToElement(By.xpath(card));
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

    /** Điền chi tiết (số nhà, đường…) — không ghi lại Phường/Tỉnh (đã chọn ở dropdown). */
    public void fillAddressDetailAtBlock(String scopeXPath, int blockIndex, String value, String logLabel) {
        if (value == null || value.isBlank()) {
            return;
        }
        String streetOnly = toAddressStreetDetail(value);
        if (streetOnly == null || streetOnly.isBlank()) {
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
            if (isAddressDetailFilledInBlock(scope, blockIndex)) {
                System.out.println(" ⏩ Ô nhập [" + logLabel + "] đã có nội dung — bỏ qua.");
                return;
            }
            waitUntilVisible(detail, WaitConfig.FIELD, logLabel);
            scrollToElement(detail);
            sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            setTextWithCheck(detail, streetOnly, "Ô nhập [" + logLabel + "]");
            return;
        }
        By legacy = By.xpath(scope + "//label[contains(., 'Địa chỉ') and not(contains(., 'trụ sở'))]"
                + "/following-sibling::textarea | " + scope
                + "//label[contains(., 'Địa chỉ')]/parent::div//textarea");
        if (existsNow(legacy)) {
            setTextWithCheck(legacy, streetOnly, "Ô nhập [" + logLabel + "]");
        }
    }

    /**
     * Ô "Chi tiết (số nhà, đường…)" chỉ cần số nhà/đường — bỏ đoạn Phường/Xã và Tỉnh/TP
     * (vd. {@code 95 Nguyễn Huệ, Phường 1, Bắc Ninh} → {@code 95 Nguyễn Huệ}).
     */
    public static String toAddressStreetDetail(String diaChi) {
        if (diaChi == null || diaChi.isBlank()) {
            return null;
        }
        String trimmed = diaChi.replace('\u00A0', ' ').trim();
        int cut = -1;
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (String marker : List.of(", phường", ", xã", ", thị trấn", ", p.", ", x.")) {
            int i = lower.indexOf(marker);
            if (i > 0 && (cut < 0 || i < cut)) {
                cut = i;
            }
        }
        if (cut > 0) {
            return trimmed.substring(0, cut).trim();
        }
        if (trimmed.contains(",")) {
            return trimmed.substring(0, trimmed.indexOf(',')).trim();
        }
        return trimmed;
    }

    /** Khối địa chỉ đã có tỉnh, phường và chi tiết hợp lệ. */
    public boolean isAdministrativeAddressBlockComplete(String scopeXPath, int blockIndex) {
        String parent = scopeXPath == null ? "" : scopeXPath;
        int total = effectiveAddressBlockCount(parent);
        if (blockIndex < 0 || blockIndex >= total) {
            return true;
        }
        String card = addressCardScope(parent, blockIndex);
        String scope = card != null ? card : parent;
        int idx = card != null ? 1 : blockIndex + 1;
        By btnTinh = adminDropdownButtonAt(scope, false, idx);
        By btnPhuong = adminDropdownButtonAt(scope, true, idx);
        if ((existsNow(btnTinh)) && !isAdminDropdownFilledAt(btnTinh)) {
            return false;
        }
        if (hasWardFieldInBlock(scope, idx) && !isAdminDropdownFilledAt(btnPhuong)) {
            return false;
        }
        return isAddressDetailFilledInBlock(parent, blockIndex);
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
        String trimmed = text.replace('\u00A0', ' ').trim();
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
            for (String attr : List.of("aria-label", "title", "data-value", "data-state")) {
                try {
                    String a = el.getAttribute(attr);
                    if (a != null && !a.isBlank() && !isAdminPlaceholderText(a)
                            && !"open".equalsIgnoreCase(a) && !"closed".equalsIgnoreCase(a)) {
                        return a.trim();
                    }
                } catch (Exception ignored) {
                }
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
            if (!isAdminPlaceholderText(text)) {
                return text;
            }
            String raw = el.getAttribute("textContent");
            if (raw != null) {
                String cleaned = raw.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
                if (!isAdminPlaceholderText(cleaned)) {
                    return cleaned;
                }
            }
            return "";
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
        // Giới hạn thử — tránh click dropdown hàng chục lần khi catalog dài.
        int maxTries = Math.min(4, picks.size());
        RuntimeException last = null;
        for (int i = 0; i < maxTries; i++) {
            String pick = picks.get(i);
            try {
                selectCustomDropdown(dropdownLocator, GLOBAL_DROPDOWN_OPTIONS, pick, elementName);
                return;
            } catch (RuntimeException e) {
                last = e;
                dismissOpenDropdownsQuiet();
            }
        }
        if (searchInput != null) {
            try {
                selectDropdownWithSearch(dropdownLocator, searchInput, GLOBAL_DROPDOWN_OPTIONS, picks.get(0), elementName);
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
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                if (trySelectRandomWardOnce(dropdownLocator, searchInput, elementName)) {
                    return;
                }
            } catch (RuntimeException e) {
                last = e;
            }
            dismissOpenDropdownsQuiet();
            sleepMillis(WaitConfig.SETTLE_MS);
            if (attempt < 2) {
                System.out.println(" ⏳ " + elementName + " — thử chọn phường/xã lại (" + (attempt + 1) + "/2)...");
            }
        }
        dismissOpenDropdownsQuiet();
        throw new RuntimeException("❌ Không chọn được phường/xã trong [" + elementName + "].", last);
    }

    private boolean trySelectRandomWardOnce(By dropdownLocator, By searchInput, String elementName) {
        scrollToElement(dropdownLocator);
        clickElementQuiet(dropdownLocator, elementName);
        sleepMillis(WaitConfig.SETTLE_MS);
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
     * Chụp viewport giữ nguyên toast/notify.
     * Không scroll / không ESC — scroll dễ làm toast tắt hoặc host gửi lại request (toast x2).
     */
    public String takeScreenshotPreserveToast() {
        if (!screenshotsEnabled()) {
            return null;
        }
        try {
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

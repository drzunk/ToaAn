package vn.tuphap.automation.ui;

import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import vn.tuphap.automation.report.TestActionLog;

import vn.tuphap.automation.report.BaoCao;

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
import java.util.HashMap;
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

    /** Tiền tố log theo Chrome khi chạy song song — tránh tưởng 1 browser chọn/điền 2 lần. */
    private static String browserTag() {
        try {
            Integer slot = vn.tuphap.automation.core.BrowserLayout.currentSlot();
            if (slot != null) {
                return "[C" + (slot + 1) + "] ";
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private void logUi(String message) {
        System.out.println(browserTag() + message);
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
        UiProfiler.enter(UiProfiler.CLICK);
        try {
            clickElementInternal(by, elementName, timeoutSeconds);
        } finally {
            UiProfiler.exit();
        }
    }

    private void clickElementInternal(By by, String elementName, int timeoutSeconds) {
        try {
            failIfBrowserClosed();
            WebElement element = waitForDisplayedEnabled(by, timeoutSeconds);
            scrollToElement(element);
            clickWithFallback(element);
            logUi(" ➔ Click vào: [" + elementName + "]");
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
        logUi(" ⏳ Chờ có thể click: " + description);
        try {
            waitForDisplayedEnabled(by, timeoutSeconds);
            logUi(" ✅ Có thể click: " + description);
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
            List<WebElement> found = driver.findElements(by);
            if (found.isEmpty()) {
                return null;
            }
            // Lọc nới tay 1 lượt, rồi hỏi Selenium đúng như cũ trên số ít ứng viên còn lại.
            // KHÔNG tự suy ra "enabled" bằng JS: isEnabled() của Selenium chỉ xét thuộc tính
            // disabled của control, KHÔNG xét aria-disabled — tự thêm điều kiện đó là làm chặt
            // hơn bản gốc, khiến nút vốn bấm được bỗng thành không tìm thấy.
            boolean[] maybe = maybeDisplayed(found);
            for (int i = 0; i < found.size(); i++) {
                if (!maybe[i]) {
                    continue;
                }
                WebElement element = found.get(i);
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        return element;
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            return null;
        } catch (BrowserClosedException e) {
            throw e;
        } catch (WebDriverException e) {
            failIfBrowserClosed(e);
            throw e;
        }
    }

    /**
     * Cuộn element vào giữa khung nhìn, chỉ nghỉ khi trang <b>thật sự</b> có dịch chuyển.
     * <p>
     * Hàm này nằm trên đường đi của mọi {@code clickElement} / {@code setText} (~70-95 lần mỗi case),
     * nên khoản nghỉ vô điều kiện cũ tốn ~10s/case. Các field liên tiếp trong cùng một thẻ đã nằm
     * giữa màn hình thì cuộn 0px — không có gì để chờ ổn định.
     */
    private void scrollToElement(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object moved = js.executeScript(
                    "var x0 = window.scrollX, y0 = window.scrollY;"
                            + "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});"
                            + "return Math.abs(window.scrollX - x0) + Math.abs(window.scrollY - y0);",
                    element);
            if (moved instanceof Number n && n.doubleValue() > 0) {
                sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            }
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
            // Bị chắn = layout chưa yên. Nghỉ + thử lại native đúng ở đây, thay cho khoản nghỉ vô
            // điều kiện đã bỏ trong scrollToElement. JS click bỏ qua overlay nên "thành công" của
            // nó có thể là giả — chỉ dùng khi native đã thua lần hai.
            if (msg.contains("intercepted")) {
                sleepMillis(WaitConfig.SETTLE_SHORT_MS);
                try {
                    element.click();
                    return;
                } catch (Exception ignored) {
                }
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
            logUi(" ➔ Click vào: [" + elementName + "]");
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
                logUi(" ⏩ [" + elementName + "] đã được chọn sẵn.");
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

            logUi(" ➔ Click vào: [" + elementName + "]");
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
                logUi(" ⏩ [" + elementName + "] đã "
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
            logUi(" ➔ " + (wantChecked ? "Chọn" : "Bỏ chọn") + ": [" + elementName + "]");
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
            logUi(" ⏩ Bỏ qua [" + elementName + "] — không có trên biểu mẫu.");
            TestActionLog.boQua(elementName, "Không có trên biểu mẫu");
            return;
        }
        boolean selected = isCustomToggleSelected(optionBy);
        if (selected == wantSelected) {
            logUi(" ⏩ [" + elementName + "] đã "
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
            logUi(" ➔ Click vào: [" + elementName + "]");
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi: Không tìm thấy hoặc không thể click: [" + elementName + "]");
        }
    }

    public void setText(By by, String value, String elementName) {
        UiProfiler.enter(UiProfiler.DIEN_FIELD);
        try {
            setTextInternal(by, value, elementName);
        } finally {
            UiProfiler.exit();
        }
    }

    private void setTextInternal(By by, String value, String elementName) {
        try {
            failIfBrowserClosed();
            // Phân giải locator ĐÚNG MỘT LẦN. Trước đây scrollToElement(by) phân giải lần 1 rồi
            // wait.until phân giải lần 2 trên cùng locator — với các XPath union dài trong dự án
            // này thì lần thứ hai không rẻ. Cuộn bằng chính element vừa lấy được.
            WebElement element = wait.until(d -> {
                failIfBrowserClosed();
                return ExpectedConditions.visibilityOfElementLocated(by).apply(d);
            });
            scrollToElement(element);
            // Prefer an editable input/textarea if the locator matched a wrapper.
            // getTagName + findElements lồng nhau gộp thành 1 lượt JS.
            WebElement editable = resolveEditableTarget(element);
            if (editable != null) {
                element = editable;
            }
            element.click();
            clearEditable(element);
            typePreservingSpaces(element, value);
            logUi(" ➔ Điền: '" + value + "' vào [" + elementName + "]");
            TestActionLog.dien(elementName, value);
            verifyFilledValue(by, value, elementName);
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception e) {
            failIfBrowserClosed(e);
            throw new RuntimeException("❌ Lỗi: Không tìm thấy ô [" + elementName + "] để điền '" + value + "'");
        }
    }

    /**
     * Nếu locator trúng phần bọc thay vì ô nhập, trả về input/textarea bên trong — <b>một</b> lượt
     * gọi thay cho {@code getTagName()} + {@code findElements(".//input|.//textarea")}.
     * Trả {@code null} nghĩa là dùng chính element ban đầu (nó đã là ô nhập, hoặc không có ô lồng).
     */
    private WebElement resolveEditableTarget(WebElement element) {
        try {
            Object nested = ((JavascriptExecutor) driver).executeScript(
                    "var e = arguments[0];"
                            + "var t = (e.tagName || '').toLowerCase();"
                            + "if (t === 'input' || t === 'textarea') { return null; }"
                            + "return e.querySelector('input, textarea');",
                    element);
            return nested instanceof WebElement w ? w : null;
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception ignored) {
            return null;
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
    /**
     * Từ độ dài này trở lên thì đặt thẳng value bằng JS thay vì gõ từng phím.
     * <p>
     * Gõ phím làm React render lại sau <b>mỗi ký tự</b> — đo được 3.8 giây cho một ô ~50 ký tự
     * ("Tóm tắt sơ bộ yêu cầu" ở bước 1). Ngưỡng 40 giữ các ô ngắn (họ tên, SĐT, CCCD, MST…)
     * ở đường gõ phím thật, vì đó là nơi cần mask/validate phía client phản ứng đúng như người
     * dùng gõ — cũng là nơi bộ ca âm dựa vào để phát hiện UI lọc ký tự.
     */
    private static final int LONG_TEXT_CHARS = resolveLongTextChars();

    /**
     * 40 → 15, đã kiểm chứng bằng bộ ca âm.
     * <p>
     * Lo ngại ban đầu: đặt value bằng JS thay vì gõ phím sẽ bỏ qua bộ lọc ký tự phía client, khiến
     * ca âm không còn bắt được lỗi. Chạy đối chứng 6 ca âm ở hai ngưỡng (40 và 15) — <b>kết quả
     * giống hệt nhau từng ca</b>, kể cả hai ca duy nhất mà hệ thống thật sự chặn
     * ({@code CCCD = "abcdefghijklmnop"} 16 ký tự và {@code Giá trị tranh chấp} 25 ký tự) — đúng
     * hai ca nằm trong vùng chuyển sang JS. Số ô báo "lệch giá trị" giữ nguyên 2 ở cả hai lượt,
     * tức bộ lọc phía client <b>vẫn chạy</b> khi set value bằng JS + dispatch input/change.
     * <p>
     * Đổi lại: {@code Điền ô nhập} giảm từ 14.9s xuống 10.7s mỗi case.
     * Quay lại mức cũ: {@code -Dtaodon.longTextChars=40}.
     */
    private static int resolveLongTextChars() {
        String raw = System.getProperty("taodon.longTextChars");
        if (raw == null || raw.isBlank()) {
            return 15;
        }
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    private void typePreservingSpaces(WebElement element, String value) {
        if (value == null) {
            return;
        }
        if (!value.contains(" ")) {
            element.sendKeys(value);
            return;
        }

        if (value.length() >= LONG_TEXT_CHARS) {
            setNativeInputValue(element, value);
            String actual = readInputValue(element).replace('\u00A0', ' ').trim();
            if (actual.equals(value.replace('\u00A0', ' ').trim())) {
                return;
            }
            clearEditable(element);
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

    /**
     * Có thấy element không — chờ tối đa {@link WaitConfig#PROBE_MS} rồi trả lời.
     * Đây là <b>phép kiểm tra</b>, không phải chỗ để chờ: cần chờ thật thì dùng
     * {@link #waitUntilVisible(By, int, String)} với timeout khai báo rõ.
     */
    public boolean isElementVisible(By by) {
        UiProfiler.enter(UiProfiler.DO_FIELD);
        try {
            failIfBrowserClosed();
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofMillis(WaitConfig.PROBE_MS));
            shortWait.pollingEvery(Duration.ofMillis(100));
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
        } finally {
            UiProfiler.exit();
        }
    }

    /** Kiểm tra nhanh trong vòng lặp chờ — timeout ngắn, không block lâu. */
    public boolean isElementPresent(By by) {
        return existsNow(by);
    }

    /**
     * Tìm element ngay lập tức (không chờ).
     * <p>
     * Không tự đặt lại {@code implicitlyWait}: {@code BaseTest.createDriver()} đã đặt 0 ngay khi
     * tạo driver và không nơi nào đặt khác — mỗi lệnh set là 1 lượt gọi chromedriver, mà hàm này
     * bị gọi hàng trăm lần cho mỗi khối địa chỉ nên 2 lệnh set thừa/lần cộng dồn rất tốn.
     */
    public boolean existsNow(By by) {
        failIfBrowserClosed();
        try {
            List<WebElement> found = driver.findElements(by);
            if (found.isEmpty()) {
                return false;
            }
            boolean[] maybe = maybeDisplayed(found);
            for (int i = 0; i < found.size(); i++) {
                // Lọc nới tay bằng 1 lượt JS, rồi để isDisplayed() quyết trên số ít còn lại —
                // kết quả y hệt vòng lặp cũ, chỉ bớt lượt gọi.
                if (maybe[i] && reallyDisplayed(found.get(i))) {
                    return true;
                }
            }
            return false;
        } catch (WebDriverException e) {
            failIfBrowserClosed(e);
            throw e;
        }
    }

    /**
     * Hỏi "element nào đang hiển thị" cho <b>cả danh sách</b> trong một lượt gọi chromedriver.
     * <p>
     * {@code element.isDisplayed()} là một lượt HTTP riêng cho <i>từng</i> element. Với danh sách
     * option của dropdown tỉnh (63 mục) thì riêng khâu này đã là 63 lượt. Gộp lại còn 1 — đây chính
     * là cách Playwright nhanh hơn: nó chạy phép kiểm tra ngay trong trang thay vì hỏi qua mạng
     * từng cái một.
     * <p>
     * Trả về mảng cùng độ dài với {@code elements}; nếu JS lỗi thì lùi về cách hỏi từng cái để
     * không đổi hành vi.
     */
    /**
     * Phép kiểm tra "đang hiển thị" chạy trong trang — bám sát {@code isDisplayed()} của Selenium.
     * <p>
     * Riêng {@code opacity} là bắt buộc: React hay fade-in dropdown, và một option đang ở
     * {@code opacity:0} thì Selenium coi là <b>chưa</b> hiển thị. Bỏ sót điều kiện này sẽ click
     * trúng option chưa hiện xong — đúng kiểu flaky mà việc gộp lượt gọi không được phép đánh đổi.
     * Opacity phải soát cả tổ tiên vì nó kế thừa hiệu ứng thị giác; {@code getClientRects()} đã lo
     * {@code display:none} ở mọi cấp.
     */
    private static final String JS_MAYBE_VISIBLE_FN =
            "function maybeVisible(e) {"
                    + "  if (!e || !e.getClientRects || e.getClientRects().length === 0) { return false; }"
                    + "  for (var n = e; n && n.nodeType === 1; n = n.parentElement) {"
                    + "    var s = window.getComputedStyle(n);"
                    + "    if (!s) { break; }"
                    + "    if (s.visibility === 'hidden' || s.visibility === 'collapse') { return false; }"
                    + "    if (s.display === 'none') { return false; }"
                    + "  }"
                    + "  return true;"
                    + "}";

    /**
     * Bộ lọc <b>nới tay</b> chạy trong trang: chỉ loại những element <i>chắc chắn</i> không hiển thị
     * ({@code display:none}, {@code visibility:hidden}, không có hộp bao).
     * <p>
     * Cố ý <b>không</b> tự kết luận "đang hiển thị" — quy tắc của {@code isDisplayed()} còn xét
     * opacity, cắt theo overflow, transform… Chép lại bằng JS là mời flaky. Vai trò của hàm này chỉ
     * là cắt danh sách ứng viên trong <b>1</b> lượt gọi, rồi để chính Selenium phán quyết trên số ít
     * ứng viên còn lại. Nhờ vậy kết quả <b>giống hệt</b> code cũ, chỉ tốn ít lượt gọi hơn.
     * <p>
     * JS lỗi → trả về "tất cả đều có thể", tức lùi hẳn về hành vi cũ.
     */
    private boolean[] maybeDisplayed(List<WebElement> elements) {
        boolean[] out = new boolean[elements.size()];
        Arrays.fill(out, true);
        if (elements.isEmpty()) {
            return out;
        }
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(
                    JS_MAYBE_VISIBLE_FN + "return arguments[0].map(maybeVisible);", elements);
            if (raw instanceof List<?> list && list.size() == out.length) {
                for (int i = 0; i < out.length; i++) {
                    out[i] = Boolean.TRUE.equals(list.get(i));
                }
            }
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception ignored) {
            // Giữ nguyên "tất cả đều có thể" — Selenium sẽ soát từng cái như trước.
        }
        return out;
    }

    /** Lấy biểu thức XPath thô từ {@code By.xpath(...)}; {@code null} nếu không phải loại đó. */
    private static String rawXPath(By by) {
        if (by == null) {
            return null;
        }
        String s = by.toString();
        String prefix = "By.xpath: ";
        return s.startsWith(prefix) ? s.substring(prefix.length()) : null;
    }

    /**
     * Ứng viên đầu tiên có element hiển thị — <b>một</b> lượt gọi cho cả danh sách.
     * <p>
     * Các hàm tìm nút tỉnh/phường thử 3-4 locator <i>tuần tự</i>, mỗi cái một lượt
     * {@code findElements} kèm đánh giá XPath nhiều nhánh union trên DOM React nặng. Gộp lại thành
     * một lần {@code document.evaluate} chạy trong trang. Dùng bộ lọc nới tay giống
     * {@link #maybeDisplayed} rồi để {@link #existsNow} xác nhận lại ở phía người gọi, nên kết quả
     * không lỏng hơn cách cũ.
     *
     * @return chỉ số ứng viên khớp, hoặc {@code -1}; {@code -2} nghĩa là không gộp được (người gọi
     *         tự thử tuần tự như cũ).
     */
    private int firstMaybeExisting(List<By> candidates) {
        List<String> xps = new ArrayList<>(candidates.size());
        for (By b : candidates) {
            String x = rawXPath(b);
            if (x == null) {
                return -2;
            }
            xps.add(x);
        }
        if (xps.isEmpty()) {
            return -1;
        }
        try {
            Object idx = ((JavascriptExecutor) driver).executeScript(
                    JS_MAYBE_VISIBLE_FN
                            + "var xs = arguments[0];"
                            + "for (var i = 0; i < xs.length; i++) {"
                            + "  try {"
                            + "    var r = document.evaluate(xs[i], document, null, 7, null);"
                            + "    for (var j = 0; j < r.snapshotLength; j++) {"
                            + "      if (maybeVisible(r.snapshotItem(j))) { return i; }"
                            + "    }"
                            + "  } catch (e) { return -2; }"
                            + "}"
                            + "return -1;",
                    xps);
            if (idx instanceof Number n) {
                return n.intValue();
            }
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception ignored) {
        }
        return -2;
    }

    /** Selenium phán quyết (đúng ngữ nghĩa cũ), có bọc stale. */
    private boolean reallyDisplayed(WebElement el) {
        try {
            return el.isDisplayed();
        } catch (StaleElementReferenceException ignored) {
            return false;
        }
    }

    /**
     * Đọc text của <b>cả danh sách</b> element trong một lượt gọi (thay cho N lượt {@code getText()}
     * + N lượt {@code getAttribute("textContent")}). Xem giải thích ở {@link #displayedFlags}.
     */
    private List<String> textsOf(List<WebElement> elements) {
        if (elements.isEmpty()) {
            return List.of();
        }
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(
                    "return arguments[0].map(function (e) {"
                            + "  if (!e) { return ''; }"
                            + "  var t = e.innerText;"
                            + "  if (!t || !t.trim()) { t = e.textContent; }"
                            + "  return t == null ? '' : t;"
                            + "});",
                    elements);
            if (raw instanceof List<?> list && list.size() == elements.size()) {
                List<String> out = new ArrayList<>(list.size());
                for (Object o : list) {
                    out.add(o == null ? "" : o.toString().replace('\u00A0', ' ').trim().replaceAll("\\s+", " "));
                }
                return out;
            }
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception ignored) {
        }
        List<String> out = new ArrayList<>(elements.size());
        for (WebElement el : elements) {
            try {
                out.add(readElementText(el));
            } catch (StaleElementReferenceException ignored) {
                out.add("");
            }
        }
        return out;
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

    /** Message ngắn gọn cho báo cáo Excel/HTML. */
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
        return driver.findElements(by).size();
    }

    public void waitUntilVisible(By by, int timeoutSeconds, String description) {
        UiProfiler.enter(UiProfiler.CHO_DOI);
        try {
            waitUntilVisibleInternal(by, timeoutSeconds, description);
        } finally {
            UiProfiler.exit();
        }
    }

    private void waitUntilVisibleInternal(By by, int timeoutSeconds, String description) {
        logUi(" ⏳ Chờ hiển thị: " + description);
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
            logUi(" ✅ Đã hiển thị: " + description);
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
        logUi(" ⏳ Chờ ẩn: " + description);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> {
                        failIfBrowserClosed();
                        return !existsDisplayed(by);
                    });
            logUi(" ✅ Đã ẩn: " + description);
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
        UiProfiler.enter(UiProfiler.TOAST);
        try {
            return collectSystemFeedbackMessagesInternal();
        } finally {
            UiProfiler.exit();
        }
    }

    private List<String> collectSystemFeedbackMessagesInternal() {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(collectValidationMessages());
        merged.addAll(collectToastMessages());
        merged.addAll(collectFrontendCrashMessages());
        return new ArrayList<>(merged);
    }

    /** Lỗi JS frontend (API danhSach undefined) — thường crash cả trang. */
    public static boolean isFrontendCrashMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("cannot read properties of undefined")
                || lower.contains("reading 'danhsach'")
                || lower.contains("reading \"danhsach\"")
                || (lower.contains("danhsach") && lower.contains("undefined"))
                || lower.contains("something went wrong")
                || lower.contains("application error")
                || lower.contains("minified react error");
    }

    public boolean hasFrontendCrashVisible() {
        for (String msg : collectFrontendCrashMessages()) {
            if (isFrontendCrashMessage(msg)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldRetryAfterFrontendCrash(Throwable error) {
        if (hasFrontendCrashVisible()) {
            return true;
        }
        if (error != null && error.getMessage() != null && isFrontendCrashMessage(error.getMessage())) {
            return true;
        }
        return error != null && error.getMessage() != null && error.getMessage().contains("FRONTEND_CRASH");
    }

    /** F5 khi trang crash — trả về true nếu đã refresh. */
    public boolean recoverFromFrontendCrash() {
        if (!hasFrontendCrashVisible()) {
            return false;
        }
        logUi(" ⚠ Crash frontend (danhSach/API) — tải lại trang (F5)...");
        // Chụp ảnh crash để có bằng chứng — trước đây có chụp nhưng vứt kết quả đi, tốn thời gian
        // mà báo cáo không nhận được gì.
        BaoCao.logNoteWithScreenshot(
                "Trang bị lỗi hiển thị (danhSach/API) — tự tải lại trang để đi tiếp.",
                takeScreenshotPreserveToast());
        driver.navigate().refresh();
        // Chịu lực: không có wait nào giữa refresh và dismissOpenDropdownsQuiet bên dưới.
        sleepMillis(WaitConfig.SETTLE_ASYNC_MS);
        dismissOpenDropdownsQuiet();
        return true;
    }

    /**
     * Sau khi bấm thẻ Loại đơn — chờ dropdown loại việc enabled, API catalog ổn định.
     */
    public void waitForStableFormAfterLoaiDon(By loaiViecDropdown, int timeoutSec) {
        logUi(" ⏳ Chờ catalog loại việc sau chọn loại đơn...");
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasFrontendCrashVisible()) {
                throw new RuntimeException("FRONTEND_CRASH: danhSach/API khi load catalog loại việc");
            }
            if (existsNow(loaiViecDropdown) && isElementEnabledNow(loaiViecDropdown)) {
                // Chịu lực: khoảng cách có chủ đích giữa "dropdown đã enabled" và lần soát crash
                // danhSach thứ hai. Cố định 280ms, không theo taodon.wait.scale.
                sleepMillis(280);
                if (!hasFrontendCrashVisible()) {
                    logUi(" ✅ Catalog loại việc sẵn sàng.");
                    return;
                }
                throw new RuntimeException("FRONTEND_CRASH: trang crash ngay sau khi catalog hiện");
            }
            sleepMillis(300);
        }
        if (hasFrontendCrashVisible()) {
            throw new RuntimeException("FRONTEND_CRASH: danhSach/API khi load catalog loại việc");
        }
        waitUntilVisible(loaiViecDropdown, Math.min(WaitConfig.FIELD, timeoutSec),
                "Dropdown [Loại việc cụ thể]");
    }

    private List<String> collectFrontendCrashMessages() {
        List<String> hints = new ArrayList<>();
        collectTexts(driver.findElements(By.xpath(
                "//*[contains(., 'Cannot read properties')"
                        + " or contains(., 'reading') and contains(., 'danhSach')"
                        + " or contains(., 'Something went wrong')"
                        + " or contains(., 'Application error')]"
                        + "[string-length(normalize-space(.)) < 400]")), hints);
        for (String msg : collectToastMessages()) {
            if (isFrontendCrashMessage(msg)) {
                hints.add(msg);
            }
        }
        hints.addAll(readRecentBrowserCrashLogs());
        return hints;
    }

    private List<String> readRecentBrowserCrashLogs() {
        List<String> errors = new ArrayList<>();
        try {
            LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
            for (LogEntry entry : logs) {
                if (entry == null || entry.getMessage() == null) {
                    continue;
                }
                String msg = entry.getMessage();
                if (isFrontendCrashMessage(msg)) {
                    errors.add(msg.length() > 220 ? msg.substring(0, 219) + "…" : msg);
                }
            }
        } catch (Exception ignored) {
        }
        return errors;
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
        UiProfiler.enter(UiProfiler.CHO_DOI);
        try {
            waitForStepTransitionInternal(stepNumber, stepName, marker, timeoutSeconds, description);
        } finally {
            UiProfiler.exit();
        }
    }

    private void waitForStepTransitionInternal(int stepNumber, String stepName, By marker,
                                               int timeoutSeconds, String description) {
        logUi(" ⏳ Chờ chuyển bước: " + description);
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        java.util.Set<String> softNoted = new java.util.LinkedHashSet<>();
        while (System.currentTimeMillis() < deadline) {
            failIfBrowserClosed();
            if (existsDisplayed(marker)) {
                logUi(" ✅ " + description);
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
        logUi(" ⚠ Chưa chuyển bước — chờ thêm " + WaitConfig.BLOCKING_GRACE_SEC
                + "s trước khi dừng case...");
        long graceDeadline = System.currentTimeMillis() + WaitConfig.BLOCKING_GRACE_SEC * 1000L;
        while (System.currentTimeMillis() < graceDeadline) {
            failIfBrowserClosed();
            if (existsDisplayed(marker)) {
                logUi(" ✅ " + description + " (sau khi chờ thêm)");
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
        if (existsDisplayed(marker)) {
            logUi(" ✅ " + description + " (đã chuyển bước — bỏ qua toast tạm)");
            return;
        }
        // Chụp ngay khi toast còn trên màn — grace 10s sẽ làm toast biến mất.
        String earlyShot = takeScreenshotPreserveToast();
        if (earlyShot != null) {
            logUi(" 📸 Đã chụp ảnh lỗi (giữ toast) ngay khi phát hiện chặn luồng.");
        }

        logUi(" ⚠ Lỗi chặn luồng — chờ thêm " + WaitConfig.BLOCKING_GRACE_SEC
                + "s: " + String.join(" | ", feedback));
        long graceDeadline = System.currentTimeMillis() + WaitConfig.BLOCKING_GRACE_SEC * 1000L;
        while (System.currentTimeMillis() < graceDeadline) {
            failIfBrowserClosed();
            if (existsDisplayed(marker)) {
                logUi(" ✅ " + description + " (hết lỗi sau khi chờ)");
                noteSoftFeedbackAndContinue(stepNumber, stepName, description, feedback,
                        new java.util.LinkedHashSet<>());
                return;
            }
            sleepMillis(250);
        }
        failStepWithSystemFeedback(stepNumber, stepName, description, feedback, earlyShot);
    }

    public static boolean hasEformAckInFeedback(List<String> feedback) {
        return isEformBridgeSettledInFeedback(feedback);
    }

    /** Có toast "Đã ghi nhận…" trong cùng lượt feedback → bỏ qua cảnh báo bridge tạm. */
    private static boolean isEformBridgeSettledInFeedback(List<String> feedback) {
        if (feedback == null) {
            return false;
        }
        for (String msg : feedback) {
            if (isEformBridgeAcknowledged(msg)) {
                return true;
            }
        }
        return false;
    }

    /** Host đã ack nội dung iframe — toast "chưa phản hồi" kèm theo là cảnh báo tạm, không chặn wizard. */
    private static boolean isEformBridgeAcknowledged(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("đã ghi nhận nội dung")
                || lower.contains("da ghi nhan noi dung");
    }

    private static boolean isEformBridgePendingWarning(String msg) {
        if (msg == null || msg.isBlank() || isEformBridgeAcknowledged(msg)) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("chưa phản hồi")
                || lower.contains("gửi ngay trong biểu mẫu")
                || lower.contains("biểu mẫu chưa");
    }

    /** Thông báo không chặn → chụp ảnh + ghi log, không dừng case. */
    private void noteSoftFeedbackAndContinue(int stepNumber, String stepName, String context,
                                             List<String> feedback, java.util.Set<String> alreadyNoted) {
        if (feedback == null || feedback.isEmpty()) {
            return;
        }
        List<String> soft = new ArrayList<>();
        List<String> successAck = new ArrayList<>();
        for (String msg : filterFeedbackNoise(feedback)) {
            if (msg == null || msg.isBlank() || isBlockingMessage(msg)) {
                continue;
            }
            if (!alreadyNoted.add(msg)) {
                continue;
            }
            if (isEformBridgeAcknowledged(msg)) {
                successAck.add(msg);
            } else if (isVneidPrefillNotice(msg) || isIdentitySaveFailureMessage(msg)) {
                // VNeID / lưu định danh chỉ log tại tick checkbox — tránh banner cũ khi replay wizard.
            } else {
                soft.add(msg);
            }
        }
        String stepLabel = "Bước " + stepNumber + " — " + (stepName == null ? "" : stepName.trim());
        String ctx = context == null || context.isBlank() ? "" : context.trim();
        String prefix = ctx.isEmpty() ? stepLabel : stepLabel + " · " + ctx;
        emitSuccessAckReport(prefix, successAck);
        emitSoftWarningReport(prefix, soft);
    }

    /** Chỉ coi là chặn chuyển bước khi có lỗi bắt buộc — bỏ qua banner VNeID thông tin. */
    private static boolean hasBlockingFeedback(List<String> feedback) {
        if (feedback == null || feedback.isEmpty()) {
            return false;
        }
        boolean eformAcked = isEformBridgeSettledInFeedback(feedback);
        for (String msg : feedback) {
            if (msg == null || msg.isBlank()) {
                continue;
            }
            if (eformAcked && isEformBridgePendingWarning(msg)) {
                continue;
            }
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
        if (isFormFieldDumpMessage(msg)) {
            return false;
        }
        if (msg.length() > 120 && msg.contains("Họ và tên") && msg.contains("Phường / xã")) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        // Banner thông tin VNeID / lỗi lưu định danh (không chặn wizard).
        if (lower.contains("vneid")
                || (lower.contains("định danh") && lower.contains("điền sẵn"))
                || (lower.contains("định danh") && lower.contains("thất bại"))) {
            return false;
        }
        if (isEformBridgeAcknowledged(msg)) {
            return false;
        }
        if (isEformBridgePendingWarning(msg)) {
            return true;
        }
        if (isFrontendCrashMessage(msg)) {
            return true;
        }
        return lower.contains("bắt buộc")
                || lower.contains("vui lòng điền")
                || lower.contains("vui lòng chọn")
                || lower.contains("vui lòng bấm")
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
     * Dừng testcase: log message hệ thống, chụp ảnh (giữ toast), báo cáo HTML/Excel.
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
        List<String> clean = filterFeedbackNoise(feedback);
        if (clean.isEmpty() && feedback != null && !feedback.isEmpty()) {
            clean = new ArrayList<>();
            for (String msg : feedback) {
                String extracted = extractReportableMessage(msg);
                if (extracted != null && !clean.contains(extracted)) {
                    clean.add(extracted);
                }
            }
        }
        if (clean.isEmpty()) {
            clean = List.of("(không đọc được nội dung thông báo)");
        }
        String stepLabel = "Bước " + stepNumber + " — " + (stepName == null ? "" : stepName.trim());
        String ctx = context == null || context.isBlank() ? "" : context.trim();
        String joined = String.join(" | ", clean);
        if (hasEformPendingInList(clean)) {
            joined = formatEformFailMessage(joined);
        }

        for (String msg : clean) {
            String logCtx = ctx.isEmpty() ? stepLabel : stepLabel + " · " + ctx;
            TestActionLog.validation(logCtx, msg);
        }

        String shot = (preCapturedShot != null && !preCapturedShot.isBlank())
                ? preCapturedShot
                : takeScreenshotPreserveToast();
        if (shot != null) {
            logUi(" 📸 Đã chụp ảnh lỗi — " + stepLabel
                    + (preCapturedShot != null ? " (toast lúc phát hiện)" : ""));
        }
        String reportBody = stepLabel
                + (ctx.isEmpty() ? "" : " Ngữ cảnh: " + ctx)
                + " Hệ thống trả về: " + joined;
        if (shot != null) {
            BaoCao.logFailWithScreenshot(reportBody, shot);
        } else {
            BaoCao.logFail(reportBody);
        }
        throw new StepBlockedException(stepNumber, stepName, joined, shot);
    }

    private static boolean hasEformPendingInList(List<String> messages) {
        if (messages == null) {
            return false;
        }
        for (String msg : messages) {
            if (isEformBridgePendingWarning(msg)) {
                return true;
            }
        }
        return false;
    }

    private static String formatEformFailMessage(String joined) {
        String core = joined;
        int idx = joined.toLowerCase(Locale.ROOT).indexOf("biểu mẫu chưa phản hồi");
        if (idx >= 0) {
            core = joined.substring(idx);
            int dot = core.indexOf('.');
            if (dot > 0) {
                core = core.substring(0, dot).trim();
            } else {
                int vui = core.toLowerCase(Locale.ROOT).indexOf("vui lòng");
                if (vui > 0) {
                    core = core.substring(0, vui).trim();
                }
            }
        }
        return core + " -> lỗi eform";
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
                    + " or contains(normalize-space(.), 'phải nhập')"
                    + " or contains(normalize-space(.), 'VNeID')"
                    + " or contains(normalize-space(.), 'điền sẵn'))]";
        }
        return root + "//*[contains(@class,'text-red') or contains(@class,'text-destructive')"
                + " or contains(@class,'border-red') or contains(@class,'ring-red')"
                + " or contains(@class,'text-error') or @role='alert' or @aria-invalid='true'"
                + " or contains(@class,'toast') or contains(@class,'Toaster') or contains(@class,'sonner')]"
                + "[string-length(normalize-space(.)) > 0 and string-length(normalize-space(.)) < 300]"
                + " | " + root + "//label/following-sibling::p[string-length(normalize-space(.)) > 0"
                + " and string-length(normalize-space(.)) < 300]"
                + " | " + root + "//label/following-sibling::span[string-length(normalize-space(.)) > 0"
                + " and string-length(normalize-space(.)) < 300]"
                + " | " + root + "//*[@role='status' or @role='note'][string-length(normalize-space(.)) > 10"
                + " and string-length(normalize-space(.)) < 400]"
                + " | " + root + "//*[contains(@class,'bg-') and (contains(@class,'amber')"
                + " or contains(@class,'yellow') or contains(@class,'blue') or contains(@class,'sky')"
                + " or contains(@class,'info'))][string-length(normalize-space(.)) > 10"
                + " and string-length(normalize-space(.)) < 400]";
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
        if (isFormFieldDumpMessage(text)) {
            return true;
        }
        String lower = text.toLowerCase();
        return lower.equals("có") || lower.equals("không") || lower.contains("cursor-pointer");
    }

    /** Text scrape cả form (nhiều nhãn field) — không phải toast/banner thật. */
    static boolean isFormFieldDumpMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        if (msg.contains("Họ và tên *") && msg.contains("Email *")) {
            return true;
        }
        if (msg.length() < 60) {
            return false;
        }
        int markers = 0;
        if (msg.contains("Họ và tên")) {
            markers++;
        }
        if (msg.contains("Ngày sinh") || msg.contains("Giới tính")) {
            markers++;
        }
        if (msg.contains("Phường") && msg.contains("xã")) {
            markers++;
        }
        if (msg.contains("CCCD") || msg.contains("CMND")) {
            markers++;
        }
        if (msg.contains("Địa chỉ thường trú") || msg.contains("Tỉnh / thành phố")) {
            markers++;
        }
        return markers >= 3;
    }

    static boolean isVneidPrefillNotice(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("thất bại")) {
            return false;
        }
        return (lower.contains("vneid") && lower.contains("điền sẵn"))
                || (lower.contains("điền sẵn") && lower.contains("định danh"));
    }

    static boolean isIdentitySaveFailureMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return false;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("định danh") && lower.contains("thất bại");
    }

    private static boolean isIdentitySaveFeedbackContext(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return false;
        }
        return prefix.contains("Sau tick") || prefix.contains("Đồng ý lưu Thông tin định danh");
    }

    /** Rút gọn message hệ thống — bỏ scrape form, giữ 1 câu banner/toast. */
    static String extractReportableMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return null;
        }
        String trimmed = msg.trim().replaceAll("\\s+", " ");
        if (isFormFieldDumpMessage(trimmed)) {
            return null;
        }
        if (isVneidPrefillNotice(trimmed)) {
            int dot = trimmed.indexOf('.');
            if (dot > 20 && dot < 220) {
                return trimmed.substring(0, dot + 1).trim();
            }
        }
        if (isIdentitySaveFailureMessage(trimmed)) {
            return trimmed.replaceAll("\\s*Sao chép mã\\s*$", "").trim();
        }
        if (isEformBridgePendingWarning(trimmed)) {
            int dot = trimmed.indexOf('.');
            if (dot > 10) {
                return trimmed.substring(0, dot + 1).trim();
            }
            int vui = trimmed.toLowerCase(Locale.ROOT).indexOf("vui lòng");
            if (vui > 10) {
                return trimmed.substring(0, vui).trim().replaceAll("[.,;]+$", "") + ".";
            }
        }
        if (trimmed.length() > 160) {
            int dot = trimmed.indexOf('.');
            if (dot > 20 && dot < 160) {
                return trimmed.substring(0, dot + 1).trim();
            }
            return trimmed.substring(0, 157) + "…";
        }
        return trimmed;
    }

    private static List<String> splitFeedbackParts(String msg) {
        if (msg == null || msg.isBlank()) {
            return List.of();
        }
        if (!msg.contains(" | ")) {
            return List.of(msg);
        }
        return Arrays.asList(msg.split("\\s\\|\\s"));
    }

    /** Bỏ scrape form / text nhiễu trước khi log hoặc quyết định chặn bước. */
    public List<String> filterFeedbackNoise(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<String> clean = new ArrayList<>();
        for (String msg : messages) {
            for (String part : splitFeedbackParts(msg)) {
                String extracted = extractReportableMessage(part);
                if (extracted != null && !extracted.isBlank() && !clean.contains(extracted)) {
                    clean.add(extracted);
                }
            }
        }
        return clean;
    }

    /**
     * Chờ banner/toast sau thao tác (vd. tick Đồng ý lưu định danh) rồi chụp ảnh ngay.
     */
    public void logFeedbackAfterIdentitySave() {
        logFeedbackAfterAction("Sau tick Đồng ý lưu Thông tin định danh",
                WaitConfig.IDENTITY_SAVE_FEEDBACK_SEC * 1000L);
    }

    public void logFeedbackAfterAction(String context, long maxWaitMs) {
        long start = System.currentTimeMillis();
        long deadline = start + Math.max(500, maxWaitMs);
        while (System.currentTimeMillis() < deadline) {
            List<String> messages = filterFeedbackNoise(collectSystemFeedbackMessages());
            if (!messages.isEmpty()) {
                if (isIdentitySaveFeedbackContext(context) && !hasIdentitySaveFailure(messages)) {
                    sleepMillis(600);
                    List<String> later = filterFeedbackNoise(collectSystemFeedbackMessages());
                    if (!later.isEmpty()) {
                        messages = later;
                    }
                }
                emitValidationWarnings(context, messages);
                logUi(" ⏱ logFeedbackAfterAction[" + context + "]: "
                        + (System.currentTimeMillis() - start) + "ms (thấy feedback, thoát sớm)");
                return;
            }
            sleepMillis(250);
        }
        logUi(" ⏱ logFeedbackAfterAction[" + context + "]: "
                + (System.currentTimeMillis() - start) + "ms (hết trần, không thấy feedback)");
    }

    private static boolean hasIdentitySaveFailure(List<String> messages) {
        if (messages == null) {
            return false;
        }
        for (String msg : messages) {
            if (isIdentitySaveFailureMessage(msg)) {
                return true;
            }
        }
        return false;
    }

    /** In console + Excel; thông báo không chặn thì chụp ảnh và đi tiếp. */
    public void logValidationMessages(String context) {
        List<String> messages = filterFeedbackNoise(collectSystemFeedbackMessages());
        if (messages.isEmpty()) {
            return;
        }
        emitValidationWarnings(context, messages);
    }

    private void emitValidationWarnings(String context, List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        String prefix = (context == null || context.isBlank()) ? "Biểu mẫu" : context.trim();
        List<String> blocking = new ArrayList<>();
        List<String> soft = new ArrayList<>();
        List<String> successAck = new ArrayList<>();
        List<String> identityErrors = new ArrayList<>();
        for (String msg : messages) {
            if (isBlockingMessage(msg)) {
                blocking.add(msg);
            } else if (isEformBridgeAcknowledged(msg)) {
                successAck.add(msg);
            } else if (isIdentitySaveFailureMessage(msg)) {
                if (isIdentitySaveFeedbackContext(prefix)) {
                    identityErrors.add(msg);
                }
            } else if (isVneidPrefillNotice(msg)) {
                // Banner VNeID prefill — chỉ ghi console, không đưa vào báo cáo.
                if (isIdentitySaveFeedbackContext(prefix)) {
                    logUi(" ℹ VNeID prefill (không ghi báo cáo): " + msg);
                }
            } else {
                soft.add(msg);
            }
        }
        emitSuccessAckReport(prefix, successAck);
        emitSoftWarningReport(prefix, identityErrors);
        emitSoftWarningReport(prefix, soft);
        if (!blocking.isEmpty()) {
            logUi(" ⚠ Validation chặn [" + prefix + "]: " + String.join(" | ", blocking));
            for (String msg : blocking) {
                TestActionLog.validation(prefix, msg);
            }
        }
    }

    /** Eform host ack — coi là đạt, chụp ảnh, không ghi cảnh báo. */
    private void emitSuccessAckReport(String prefix, List<String> ack) {
        if (ack == null || ack.isEmpty()) {
            return;
        }
        String joined = String.join(" | ", ack);
        logUi(" ✅ [" + prefix + "]: " + joined);
        for (String msg : ack) {
            TestActionLog.validation(prefix, msg);
        }
        String shot = takeScreenshotForFeedback(ack);
        String body = prefix + " — " + joined;
        if (shot != null) {
            logUi(" 📸 Đã chụp ảnh xác nhận eform");
            BaoCao.logPassWithScreenshot(body, shot);
        } else {
            BaoCao.logPass(body);
        }
    }

    /** Chỉ log cảnh báo khi có thông báo mềm thật — không phải ack/pass hay banner VNeID. */
    private void emitSoftWarningReport(String prefix, List<String> soft) {
        if (soft == null || soft.isEmpty()) {
            return;
        }
        String joined = String.join(" | ", soft);
        logUi(" ⚠ Thông báo (không chặn) [" + prefix + "]: " + joined);
        for (String msg : soft) {
            TestActionLog.validation(prefix, msg);
        }
        String shot = takeScreenshotForFeedback(soft);
        String body = formatSoftWarningReport(prefix, soft);
        if (shot != null) {
            logUi(" 📸 Đã chụp ảnh thông báo — đi tiếp");
            BaoCao.logWarningWithScreenshot(body, shot);
        } else {
            BaoCao.logWarning(body);
        }
    }

    private static String formatSoftWarningReport(String prefix, List<String> soft) {
        String joined = String.join(" | ", soft);
        if (prefix.contains("Đồng ý lưu Thông tin định danh") || prefix.contains("Sau tick")) {
            return "Click [Đồng ý lưu Thông tin định danh] — hệ thống báo: " + joined;
        }
        return prefix + " — " + joined;
    }

    public void waitUntilExists(By by, int timeoutSeconds, String description) {
        logUi(" ⏳ Chờ: " + description);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> existsNow(by));
            logUi(" ✅ Sẵn sàng: " + description);
        } catch (TimeoutException e) {
            throw new RuntimeException("❌ Lỗi: hết thời gian chờ [" + description + "] sau " + timeoutSeconds + "s.");
        }
    }

    public boolean isElementEnabled(By by) {
        return isElementEnabledNow(by);
    }

    /** Kiểm tra enabled ngay — không chờ khi element không có (implicit wait đã là 0, xem existsNow). */
    public boolean isElementEnabledNow(By by) {
        failIfBrowserClosed();
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
            logUi(" ⏩ Bỏ qua: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không nhập");
            return;
        }
        // existsNow — không isElementVisible (chờ tới 5s) khi ô ẩn → tránh lúc nhanh lúc chậm.
        if (!existsNow(by)) {
            logUi(" ⏩ Bỏ qua: [" + elementName + "] do biểu mẫu ẩn.");
            TestActionLog.boQua(elementName, "Biểu mẫu ẩn / không có trên giao diện");
            return;
        }
        if (!isElementEnabledNow(by)) {
            logUi(" ⏩ Bỏ qua: [" + elementName + "] do hệ thống khóa.");
            TestActionLog.boQua(elementName, "Ô bị khóa");
            return;
        }
        if (inputValueMatches(by, value)) {
            logUi(" ⏩ [" + elementName + "] đã đúng — không điền lại.");
            return;
        }
        setText(by, value, elementName);
    }

    public void setTextForMaskedInput(By by, String value, String elementName) {
        if (value == null || value.trim().isEmpty()) {
            logUi(" ⏩ Bỏ qua: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không nhập");
            return;
        }
        if (!existsNow(by)) {
            logUi(" ⏩ Bỏ qua: [" + elementName + "] do biểu mẫu ẩn.");
            TestActionLog.boQua(elementName, "Biểu mẫu ẩn / không có trên giao diện");
            return;
        }
        if (!isElementEnabledNow(by)) {
            logUi(" ⏩ Bỏ qua: [" + elementName + "] do hệ thống khóa.");
            TestActionLog.boQua(elementName, "Ô bị khóa");
            return;
        }
        if (inputValueMatches(by, value)) {
            logUi(" ⏩ [" + elementName + "] đã đúng — không điền lại.");
            return;
        }

        try {
            WebElement element = waitForDisplayedEnabled(by, WaitConfig.FIELD);
            scrollToElement(element);
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            element.sendKeys(value);
            logUi(" ➔ Điền (định dạng đặc biệt): '" + value + "' vào [" + elementName + "]");
            TestActionLog.dienMask(elementName, value);
            verifyFilledValue(by, value, elementName);
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi: Không thể nhập dữ liệu vào ô [" + elementName + "]");
        }
    }

    /** Tìm lại element theo locator, chui vào input/textarea lồng nếu cần, đọc giá trị hiện tại. */
    private String resolveInputValue(By by) {
        WebElement element = driver.findElement(by);
        // Gộp getTagName + tìm ô lồng + đọc value thành 1 lượt gọi. Hàm này chạy sau MỖI lần điền
        // (verifyFilledValue), 854 lần mỗi lượt chạy — 3-4 lượt gọi mỗi lần là quá đắt cho một
        // phép đối chiếu thuần chẩn đoán.
        try {
            Object v = ((JavascriptExecutor) driver).executeScript(
                    "var e = arguments[0];"
                            + "var t = (e.tagName || '').toLowerCase();"
                            + "if (t !== 'input' && t !== 'textarea') { e = e.querySelector('input, textarea') || e; }"
                            + "return e.value != null ? e.value : '';",
                    element);
            if (v != null) {
                return v.toString();
            }
        } catch (BrowserClosedException e) {
            throw e;
        } catch (Exception ignored) {
            // JS lỗi → lùi về đường cũ bên dưới, giữ nguyên hành vi.
        }
        String tag = element.getTagName() == null ? "" : element.getTagName().toLowerCase();
        if (!tag.equals("input") && !tag.equals("textarea")) {
            List<WebElement> nested = element.findElements(By.xpath(".//input|.//textarea"));
            if (!nested.isEmpty()) {
                element = nested.get(0);
            }
        }
        return readInputValue(element);
    }

    /** So sánh giá trị ô với expected (bỏ qua khoảng trắng thừa / NBSP). */
    private boolean inputValueMatches(By by, String expected) {
        try {
            String actual = resolveInputValue(by).replace('\u00A0', ' ').trim();
            String want = expected.replace('\u00A0', ' ').trim();
            return !want.isEmpty() && actual.equalsIgnoreCase(want);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Đối chiếu giá trị thật trên UI sau khi điền với giá trị đã gõ — bắt các trường hợp UI tự
     * lọc/định dạng lại ký tự (mask phía client) khiến giá trị "sai" cố tình gõ vào (ca âm) chưa
     * từng thật sự tới được ô. Chỉ log + chụp ảnh khi thật sự lệch — không được phép làm gãy
     * luồng chính nên nuốt mọi lỗi.
     */
    private void verifyFilledValue(By by, String expectedValue, String elementName) {
        if (expectedValue == null || expectedValue.isBlank()) {
            return;
        }
        try {
            String actual = resolveInputValue(by).replace('\u00A0', ' ').trim();
            String want = expectedValue.replace('\u00A0', ' ').trim();
            if (actual.equalsIgnoreCase(want)) {
                return;
            }
            if (!TestActionLog.firstTimeMismatch(elementName, expectedValue)) {
                // Đã cảnh báo đúng field + đúng giá trị này rồi trong case hiện tại (thường do vòng
                // soát lại sau VNeID gõ lại) — khỏi lặp lại cảnh báo + ảnh giống hệt lần trước.
                return;
            }
            String msg = "Ô [" + elementName + "] sau khi điền có giá trị khác dữ liệu đã gõ — "
                    + "gõ: '" + expectedValue + "' | thực tế trên UI: '" + actual + "'.";
            logUi(" ⚠ " + msg);
            TestActionLog.dienLechGiaTri(elementName, expectedValue, actual);
            String shot = takeScreenshotPreserveToast();
            // Ghi chú chẩn đoán, KHÔNG phải cảnh báo: đây thường là UI tự lọc/định dạng lại ký tự,
            // không phải lỗi. Warning được xếp nặng hơn pass nên dùng warning ở đây sẽ khiến
            // case chạy đúng vẫn hiện huy hiệu cam — người đọc tưởng có vấn đề.
            BaoCao.logNoteWithScreenshot(msg, shot);
        } catch (Exception ignored) {
            // Không để bước đối chiếu (chẩn đoán) làm gãy luồng chính.
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
            logUi(" ➔ Tải lên: '" + tenHienThi + "' tại [" + elementName + "]");
            TestActionLog.taiLen(elementName, tenHienThi);
            waitUntilFileAttached(input);
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi: Không thể tải file lên [" + elementName + "]: " + e.getMessage());
        }
    }

    /**
     * Chờ trình duyệt gắn xong file vào input — thay cho {@code sleep(1)} cứng trước đây.
     * <p>
     * File mẫu chỉ vài trăm byte nên thực tế trả về sau ~100ms. Hết giờ thì đi tiếp: đây là bước
     * xác nhận, không phải điều kiện chặn (người gọi còn tự soát lại hàng tài liệu).
     */
    private void waitUntilFileAttached(WebElement input) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .pollingEvery(Duration.ofMillis(100))
                    .until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                            "return !!(arguments[0].files && arguments[0].files.length > 0);", input)));
        } catch (Exception ignored) {
        }
    }

    public void selectCustomDropdown(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
        UiProfiler.enter(UiProfiler.DROPDOWN);
        try {
            selectCustomDropdownInternal(dropdownLocator, optionsLocator, expectedText, elementName);
        } finally {
            UiProfiler.exit();
        }
    }

    private void selectCustomDropdownInternal(By dropdownLocator, By optionsLocator, String expectedText, String elementName) {
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
            // Đọc trạng thái hiển thị + text của TOÀN BỘ option trong 2 lượt gọi, thay vì
            // 2 lượt cho mỗi option. Dropdown tỉnh 63 mục: 127 lượt → 3 lượt.
            List<WebElement> options = driver.findElements(optionsLocator);
            boolean[] maybe = maybeDisplayed(options);
            List<String> texts = textsOf(options);
            for (int i = 0; i < options.size(); i++) {
                try {
                    if (!maybe[i]) {
                        continue;
                    }
                    String textOnWeb = texts.get(i);
                    if (!textOnWeb.isEmpty() && !availableOptions.contains(textOnWeb)) {
                        availableOptions.add(textOnWeb);
                    }
                    if (optionMatches(expectedText, textOnWeb)) {
                        WebElement option = options.get(i);
                        // Chỉ đến lúc sắp click mới hỏi Selenium — 1 lượt, đúng ngữ nghĩa cũ.
                        if (!reallyDisplayed(option)) {
                            continue;
                        }
                        JavascriptExecutor js = (JavascriptExecutor) driver;
                        js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", option);
                        option.click();
                        logUi(" ➔ Chọn Dropdown: '" + expectedText + "' tại [" + elementName + "]");
                        TestActionLog.chon(elementName, expectedText);
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                }
            }
            // Không nghỉ sau vòng cuối — sau nó là thoát vòng lặp, không quét lại nữa.
            if (attempt == 0) {
                sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            }
        }
        dismissOpenDropdownsQuiet();
        return false;
    }

    /** Chờ ít nhất một option hiển thị sau khi mở dropdown (SPA load async). */
    private List<WebElement> waitForVisibleDropdownOptions(By optionsLocator, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            // 2 lượt gọi cho cả danh sách, thay vì 2-3 lượt cho mỗi option — và vòng lặp này còn
            // chạy lại mỗi 250ms cho tới khi có option, nên chi phí cũ bị nhân lên theo số vòng.
            List<WebElement> options = driver.findElements(optionsLocator);
            List<WebElement> visible = new ArrayList<>();
            if (!options.isEmpty()) {
                boolean[] maybe = maybeDisplayed(options);
                List<String> texts = textsOf(options);
                for (int i = 0; i < options.size(); i++) {
                    if (maybe[i] && !texts.get(i).isBlank() && reallyDisplayed(options.get(i))) {
                        visible.add(options.get(i));
                    }
                }
            }
            if (!visible.isEmpty()) {
                return visible;
            }
            sleepMillis(250);
        }
        return List.of();
    }

    /**
     * Đóng dropdown/listbox đang mở — bấm ESC vô điều kiện.
     * <p>
     * ĐỪNG "tối ưu" bằng cách chỉ bấm ESC khi thấy {@code GLOBAL_DROPDOWN_OPTIONS}: đã thử và
     * hỏng nặng. Locator đó chỉ bắt {@code [role=option]}, không phủ hết các loại menu/overlay mà
     * ESC đang dọn — bỏ ESC khiến chúng ở lại chắn thao tác sau, sinh retry. Đo thực tế ở bước 3:
     * số lần sleep <b>tăng gấp đôi</b> (75 → 151 lượt, 15.1s → 35.8s) và bước 3 chậm thêm 37%.
     */
    private void dismissOpenDropdownsQuiet() {
        try {
            // ESC vẫn bấm VÔ ĐIỀU KIỆN như javadoc trên yêu cầu — chỉ rút ngắn khoảng nghỉ sau đó,
            // vốn là số 150 cứng nằm ngoài mọi hằng số. Hàm này chạy 15-25 lần mỗi case.
            driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
            sleepMillis(WaitConfig.SETTLE_SHORT_MS);
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
            if (hasFrontendCrashVisible()) {
                recoverFromFrontendCrash();
                sleepMillis(WaitConfig.SETTLE_ASYNC_MS);
            }
            try {
                if (trySelectDropdownWithSearch(dropdownLocator, searchInputLocator, optionsLocator,
                        expectedText, elementName)) {
                    return;
                }
            } catch (RuntimeException ex) {
                lastError = ex;
                if (shouldRetryAfterFrontendCrash(ex)) {
                    recoverFromFrontendCrash();
                    sleepMillis(WaitConfig.SETTLE_ASYNC_MS);
                    continue;
                }
            }
            if (attempt < 3) {
                logUi(" ⏳ Dropdown [" + elementName + "] chưa có kết quả — thử lại ("
                        + (attempt + 1) + "/3)...");
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
        if (hasFrontendCrashVisible()) {
            throw new RuntimeException("FRONTEND_CRASH trước khi chọn [" + elementName + "]");
        }
        clickElementQuiet(dropdownLocator, elementName);
        sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        waitForDropdownOptionsLoaded(optionsLocator, WaitConfig.DROPDOWN);
        try {
            WebElement searchInput = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInputLocator));
            searchInput.clear();
            searchInput.sendKeys(expectedText);
            logUi(" ➔ Gõ tìm kiếm Dropdown: '" + expectedText + "'");
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
                    logUi(" ➔ Chọn Dropdown (lọc): '" + expectedText + "' tại [" + elementName + "]");
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

    /** Chờ listbox có option — tránh gõ tìm kiếm khi API danhSach chưa về. */
    private void waitForDropdownOptionsLoaded(By optionsLocator, int timeoutSec) {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        // hasFrontendCrashVisible() quét XPath toàn tài liệu + đọc log trình duyệt — quá đắt để
        // chạy mỗi 250ms. Frontend crash không tự khỏi nên soi mỗi ~1s là đủ phát hiện.
        long nextCrashCheck = 0;
        while (System.currentTimeMillis() < deadline) {
            long now = System.currentTimeMillis();
            if (now >= nextCrashCheck) {
                nextCrashCheck = now + 1000;
                if (hasFrontendCrashVisible()) {
                    throw new RuntimeException("FRONTEND_CRASH: dropdown options chưa load");
                }
            }
            try {
                for (WebElement option : driver.findElements(optionsLocator)) {
                    if (!option.isDisplayed()) {
                        continue;
                    }
                    String text = readElementText(option);
                    if (text != null && !text.isBlank() && !isAdminPlaceholderText(text)) {
                        return;
                    }
                }
            } catch (Exception ignored) {
            }
            sleepMillis(250);
        }
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
    /**
     * Nhớ kết quả {@link #addressCardScope} trong lúc xử lý <b>một</b> khối địa chỉ.
     * <p>
     * {@code ADDRESS_CARD} là XPath nhiều tầng vị từ, mà 9 chỗ trong lớp này gọi
     * {@code addressCardScope(parent, index)} với cùng tham số trong lúc xử lý một khối
     * (resolveAdminDropdownButton, resolveAdminDropdownSearch, hasWardLabelVisible,
     * findWardButtonForBlock, addressDetailTextareaAtBlock, focusAddressBlock…).
     * <p>
     * Chỉ nhớ <b>thẻ chứa</b> — nó có mặt từ đầu tới cuối quá trình xử lý khối. Cố ý KHÔNG nhớ nút
     * tỉnh/phường: nút phường chưa tồn tại trước khi chọn tỉnh rồi mới hiện ra, nhớ lại là sai.
     * Cache tắt mặc định, chỉ bật trong cửa sổ xử lý một khối.
     */
    private static final ThreadLocal<Map<String, String>> CARD_SCOPE_CACHE =
            ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Boolean> CARD_SCOPE_CACHE_ON =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private void beginAddressCardCache() {
        CARD_SCOPE_CACHE.get().clear();
        CARD_SCOPE_CACHE_ON.set(Boolean.TRUE);
    }

    private void endAddressCardCache() {
        CARD_SCOPE_CACHE_ON.set(Boolean.FALSE);
        CARD_SCOPE_CACHE.get().clear();
    }

    private String addressCardScope(String parentScope, int zeroBasedIndex) {
        String parent = parentScope == null ? "" : parentScope;
        String key = parent + " " + zeroBasedIndex;
        boolean caching = Boolean.TRUE.equals(CARD_SCOPE_CACHE_ON.get());
        Map<String, String> cache = CARD_SCOPE_CACHE.get();
        if (caching && cache.containsKey(key)) {
            return cache.get(key);
        }
        String cardXpath = "(" + parent + ADDRESS_CARD + ")[" + (zeroBasedIndex + 1) + "]";
        String resolved = null;
        try {
            if (existsNow(By.xpath(cardXpath))) {
                resolved = cardXpath;
            }
        } catch (RuntimeException ignored) {
        }
        if (caching) {
            cache.put(key, resolved);
        }
        return resolved;
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
        int cards = countDisplayed(By.xpath(scope + ADDRESS_CARD));
        if (cards > 0) {
            return cards;
        }
        return countDisplayed(
                By.xpath(scope + "//label[contains(., 'Tỉnh') and contains(., 'thành phố')]"));
    }

    /** Đếm element đang hiển thị — 2 lượt gọi bất kể có bao nhiêu element khớp. */
    private int countDisplayed(By by) {
        List<WebElement> found = driver.findElements(by);
        if (found.isEmpty()) {
            return 0;
        }
        boolean[] maybe = maybeDisplayed(found);
        int n = 0;
        for (int i = 0; i < found.size(); i++) {
            // Số đếm này quyết định nhánh (1 hay 2 khối địa chỉ) — phải đúng tuyệt đối,
            // nên vẫn để Selenium chốt trên từng ứng viên qua được bộ lọc.
            if (maybe[i] && reallyDisplayed(found.get(i))) {
                n++;
            }
        }
        return n;
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
            logUi(" ⏩ " + tinhName + " đã có giá trị — bỏ qua.");
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
            logUi(" ⏩ " + phuongName + " đã có giá trị — bỏ qua.");
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
        forceSelectAdministrativeWardsInScope(scopeXPath, -1);
    }

    /**
     * Lưới an toàn "phường/xã còn trống thì chọn giúp".
     *
     * @param onlyBlockIndex chỉ xử lý khối này ({@code -1} = mọi khối trong scope).
     *                       <p>
     *                       Hãy truyền đúng khối vừa điền. Quét cả scope sẽ đụng vào những khối
     *                       <b>chưa tới lượt</b> và điền chúng bằng tỉnh/phường ngẫu nhiên. Đo
     *                       thực tế: sau khi điền xong khối Thường trú, nó chọn luôn tỉnh + phường
     *                       cho thẻ Liên lạc, rồi ngay sau đó {@code chonDiaChiLienLacGiongThuongTru}
     *                       tick "giống thường trú" làm thẻ ấy ẩn đi — toàn bộ công đó bị vứt, và
     *                       trên màn hình hiện đúng cảnh "tự nhiên nhảy ra một phường/xã rồi mất".
     */
    public void forceSelectAdministrativeWardsInScope(String scopeXPath, int onlyBlockIndex) {
        String parent = scopeXPath == null ? "" : scopeXPath;
        int total = Math.max(countVisibleAddressBlocks(parent), effectiveAddressBlockCount(parent));
        if (total <= 0) {
            total = 1;
        }
        for (int i = 0; i < total; i++) {
            if (onlyBlockIndex >= 0 && i != onlyBlockIndex) {
                continue;
            }
            beginAddressCardCache();
            try {
            String blockSuffix = total > 1 ? " #" + (i + 1) : "";
            String card = addressCardScope(parent, i);
            String scope = card != null ? card : parent;
            int idx = card != null ? 1 : i + 1;
            if (!hasWardFieldInBlock(scope, idx) && !hasWardLabelVisible(parent, i)) {
                continue;
            }
            // Khối đã có phường rồi thì KHÔNG đụng vào nữa.
            //
            // Đây là lưới an toàn cho các khối mà ensureAdministrativeAddressBlockInScope chưa
            // xử lý — nhưng nó đang quét cả khối vừa được xử lý xong, và nó phân giải nút bằng
            // đường khác (findWardButtonForBlock theo parent+index thay vì theo thẻ địa chỉ).
            // Khi hai đường trỏ vào hai element khác nhau, nó tưởng phường còn trống → chọn lại
            // tỉnh → phường bị reset → chọn lại phường. Đo trên 39 case: 139 lần chọn phường cho
            // 104 khối (dư 34%), và số lần chọn tỉnh dư đúng bằng số lần chọn phường dư.
            // Dùng đúng vị từ mà ensure... tin cậy để quyết định "đã xong hay chưa".
            if (!isWardRequiredAndUnfilled(parent, i)) {
                continue;
            }
            By btnTinh = resolveAdminDropdownButton(parent, i, false);
            By btnPhuong = findWardButtonForBlock(parent, i);
            if (btnPhuong == null || !existsNow(btnPhuong)) {
                waitForWardFieldInBlock(scope, idx, WaitConfig.WARD_READY);
                btnPhuong = findWardButtonForBlock(parent, i);
            }
            if (btnPhuong == null || !existsNow(btnPhuong)) {
                logUi(" ⚠ Không tìm thấy dropdown phường/xã" + blockSuffix + " — bỏ qua.");
                continue;
            }
            String tinhName = "Dropdown [Tỉnh / thành phố" + blockSuffix + "]";
            String phuongName = "Dropdown [Phường / xã" + blockSuffix + "]";
            By searchTinh = resolveAdminDropdownSearch(parent, i, false);
            By searchPhuong = resolveAdminDropdownSearch(parent, i, true);
            boolean daChonGiTri = false;
            // Bắt buộc existsNow trước: resolveAdminDropdownButton có thể trả về locator KHÔNG
            // tồn tại (nhánh cuối `return inParent` chạy cả khi existsNow(inParent) đã false).
            // Khi đó readAdminDropdownValue ném ở findElement rồi nuốt lỗi trả "" — tỉnh đang có
            // giá trị vẫn bị coi là trống, và ta chọn đè một tỉnh ngẫu nhiên khác.
            if (btnTinh != null && existsNow(btnTinh) && !isAdminDropdownFilledAt(btnTinh)) {
                selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
                // Chịu lực: waitForWardDropdownReady thoát sớm khi nút phường "đã có giá trị"
                // (đọc text CŨ của tỉnh trước) hoặc còn enabled — cả hai đều đúng thêm một nhịp
                // sau khi đổi tỉnh. Bỏ khoảng này là chọn trúng phường của tỉnh cũ.
                sleepMillis(WaitConfig.SETTLE_ASYNC_MS);
                waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
                daChonGiTri = true;
            }
            if (!isAdminDropdownFilledAt(btnPhuong)) {
                logUi(" ➔ Chọn phường/xã bắt buộc" + blockSuffix + "...");
                selectWardWithRetry(btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName, null, null);
                daChonGiTri = true;
            }
            // Khối đã đủ tỉnh/phường từ trước (vd. vừa được ensureAdministrativeAddressBlockInScope
            // điền xong) — không có gì để chọn thêm, khỏi cần nghỉ settle.
            if (daChonGiTri) {
                sleepMillis(WaitConfig.SETTLE_MS);
            }
            } finally {
                endAddressCardCache();
            }
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
                    // Rơi sang ngẫu nhiên thì phải NÓI: phường trên báo cáo sẽ không khớp phường
                    // trong dữ liệu test, trước đây im lặng nên không ai biết mà đối chiếu.
                    logUi(" ⚠ Không khớp phường '" + wardHint + "' trong " + phuongName
                            + " — chọn ngẫu nhiên thay thế (dữ liệu báo cáo sẽ khác dữ liệu sinh).");
                    TestActionLog.ghiChu("Không khớp phường '" + wardHint + "' — đã chọn ngẫu nhiên thay thế");
                    selectRandomWardOption(btnPhuong, searchPhuong, phuongName);
                    return;
                }
                selectRandomWardOption(btnPhuong, searchPhuong, phuongName);
                return;
            } catch (RuntimeException e) {
                last = e;
                dismissOpenDropdownsQuiet();
                if (attempt < 2) {
                    logUi(" ⏳ " + phuongName + " chưa chọn được — thử lại tỉnh/phường ("
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
        // Chịu lực: cùng race tỉnh→phường, ở nhánh retry.
        sleepMillis(WaitConfig.SETTLE_ASYNC_MS);
    }

    /** Chờ dropdown phường/xã sẵn sàng sau khi chọn tỉnh (API async). Thoát ngay khi enabled. */
    private void waitForWardDropdownReady(By wardButton, int timeoutSec) {
        long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                // existsNow (không chờ) — isElementVisible tự chờ tới 5s, dài hơn cả ngân sách
                // timeoutSec=4 của vòng lặp này nên chỉ 1 vòng đã vượt trần.
                if (!existsNow(wardButton)) {
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
        UiProfiler.enter(UiProfiler.DIA_CHI);
        beginAddressCardCache();
        try {
            ensureAddressBlockInternal(scopeXPath, blockIndex, chiTietValue, logContext);
        } finally {
            endAddressCardCache();
            UiProfiler.exit();
        }
    }

    private void ensureAddressBlockInternal(String scopeXPath, int blockIndex,
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

        if (isAdministrativeAddressBlockComplete(parentScope, blockIndex)
                && !isWardRequiredAndUnfilled(parentScope, blockIndex)) {
            logUi(" ⏩ Chi tiết địa chỉ" + blockSuffix + ctx + " đã đủ — bỏ qua.");
            return;
        }

        focusAddressBlock(parentScope, blockIndex);

        By btnTinh = resolveAdminDropdownButton(parentScope, blockIndex, false);
        By btnPhuong = resolveAdminDropdownButton(parentScope, blockIndex, true);
        By searchTinh = resolveAdminDropdownSearch(parentScope, blockIndex, false);
        By searchPhuong = resolveAdminDropdownSearch(parentScope, blockIndex, true);
        String tinhName = "Dropdown [Tỉnh / thành phố" + blockSuffix + "]" + ctx;
        String phuongName = "Dropdown [Phường / xã" + blockSuffix + "]" + ctx;

        String provinceHint = extractProvinceHint(chiTietValue);
        String wardHint = extractWardHint(chiTietValue);

        if (btnTinh == null || !existsNow(btnTinh)) {
            fillAddressDetailAtBlock(parentScope, blockIndex, chiTietValue,
                    "Chi tiết địa chỉ" + blockSuffix + ctx);
            finishAddressBlock();
            return;
        }

        // Tỉnh có đúng là tỉnh trong chuỗi địa chỉ không? Quyết định wardHint còn nghĩa lý gì không.
        boolean tinhTheoDuLieu = true;
        if (!isAdminDropdownFilledAt(btnTinh)) {
            boolean selected = false;
            if (provinceHint != null && !provinceHint.isBlank()) {
                selected = trySelectDropdownOption(btnTinh, searchTinh, GLOBAL_DROPDOWN_OPTIONS,
                        provinceHint, tinhName);
            }
            if (!selected) {
                selectRandomDropdownOption(btnTinh, searchTinh, tinhName);
            }
            tinhTheoDuLieu = selected;
            dismissOpenDropdownsQuiet();
            sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            if (btnPhuong != null) {
                waitForWardDropdownReady(btnPhuong, WaitConfig.WARD_READY);
            }
        } else {
            logUi(" ⏩ " + tinhName + " đã có giá trị — bỏ qua.");
        }
        if (!tinhTheoDuLieu && wardHint != null && !wardHint.isBlank()) {
            // Tỉnh đã bị chọn ngẫu nhiên → danh sách phường thuộc tỉnh KHÁC, nên phường trong
            // chuỗi địa chỉ chắc chắn không có trong đó. Thử nó chỉ tổ gõ vào ô tìm kiếm, chờ
            // 800ms, quét trượt 4 vòng × 300ms rồi mới chịu — mất ~1.7s và hiện ra trên UI cảnh
            // "ghi tên phường ra rồi xoá đi chọn lại". Bỏ thẳng sang chọn từ danh sách thật.
            logUi(" ⏩ Tỉnh chọn ngẫu nhiên (không khớp '" + provinceHint
                    + "') — bỏ qua gợi ý phường '" + wardHint + "', chọn theo danh sách của tỉnh này.");
            wardHint = null;
        }

        if (hasWardFieldInBlock(scope, idx) || hasWardLabelVisible(parentScope, blockIndex)) {
            waitForWardFieldInBlock(scope, idx, WaitConfig.WARD_READY);
            btnPhuong = findWardButtonForBlock(parentScope, blockIndex);
            searchPhuong = resolveAdminDropdownSearch(parentScope, blockIndex, true);
        }

        ensureWardSelectedBeforeDetail(parentScope, blockIndex, scope, idx, btnTinh, searchTinh, tinhName,
                btnPhuong, searchPhuong, phuongName, provinceHint, wardHint);

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
            logUi(" ⏩ Chi tiết địa chỉ" + ctx + " đã đủ — bỏ qua chọn lại.");
            return;
        }
        ensureAdministrativeAddressBlockInScope(scopeXPath, blockIndex, chiTietValue, logContext);
    }

    /** Bắt buộc chọn phường/xã trước chi tiết — tránh bỏ qua khi dropdown render chậm. */
    private void ensureWardSelectedBeforeDetail(String parentScope, int blockIndex, String scope, int idx,
                                                By btnTinh, By searchTinh, String tinhName,
                                                By btnPhuong, By searchPhuong, String phuongName,
                                                String provinceHint, String wardHint) {
        By wardBtn = findWardButtonForBlock(parentScope, blockIndex);
        if (wardBtn == null) {
            wardBtn = btnPhuong;
        }
        if ((wardBtn == null || !existsNow(wardBtn)) && hasWardLabelVisible(parentScope, blockIndex)) {
            waitForWardFieldInBlock(scope, idx, WaitConfig.WARD_READY);
            wardBtn = findWardButtonForBlock(parentScope, blockIndex);
        }
        if (wardBtn == null || !existsNow(wardBtn)) {
            if (hasWardLabelVisible(parentScope, blockIndex)) {
                logUi(" ⚠ Nhãn phường/xã hiển thị nhưng chưa tìm được dropdown — thử lại sau.");
            }
            return;
        }
        By wardSearch = resolveAdminDropdownSearch(parentScope, blockIndex, true);
        if (wardSearch == null || !existsNow(wardSearch)) {
            wardSearch = searchPhuong;
        }
        waitForWardDropdownReady(wardBtn, WaitConfig.WARD_READY);
        waitForWardFieldInBlock(scope, idx, WaitConfig.WARD_READY);
        if (!existsNow(wardBtn)) {
            try {
                waitUntilVisible(wardBtn, WaitConfig.WARD_READY, phuongName);
            } catch (RuntimeException ignored) {
                wardBtn = resolveAdminDropdownButton(parentScope, blockIndex, true);
                if (wardBtn == null || !existsNow(wardBtn)) {
                    logUi(" ⚠ Không tìm thấy dropdown phường/xã — thử chọn ngẫu nhiên sau khi có tỉnh.");
                    return;
                }
            }
        }
        if (isAdminDropdownFilledAt(wardBtn)) {
            return;
        }
        selectWardWithRetry(btnTinh, searchTinh, tinhName, wardBtn, wardSearch, phuongName,
                provinceHint, wardHint);
        pauseBetweenAddressSteps();
    }

    /** Chọn phường/xã nếu dropdown có trong scope (sau khi đã chọn tỉnh). */
    private void selectWardIfNeeded(String scope, int idx, By btnTinh, By searchTinh, String tinhName,
                                    By btnPhuong, By searchPhuong, String phuongName) {
        selectWardIfNeeded(scope, idx, btnTinh, searchTinh, tinhName, btnPhuong, searchPhuong, phuongName, false);
    }

    private void selectWardIfNeeded(String scope, int idx, By btnTinh, By searchTinh, String tinhName,
                                    By btnPhuong, By searchPhuong, String phuongName, boolean force) {
        if (!force && btnPhuong != null && isAdminDropdownFilledAt(btnPhuong)) {
            return;
        }
        ensureWardSelectedBeforeDetail(scope, Math.max(0, idx - 1), scope, idx, btnTinh, searchTinh, tinhName,
                btnPhuong, searchPhuong, phuongName, null, null);
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
            // Kiểm tra nhanh để quyết định có cần chờ lại hay không — không được tự chờ 5s.
            if (!existsNow(wardButton)) {
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
            if (blockIndex > 0) {
                detail = null;
            } else {
                detail = addressDetailTextareaInScope(scope);
            }
        }
        if (detail != null && existsNow(detail)) {
            if (isAddressDetailFilledInBlock(scope, blockIndex)) {
                logUi(" ⏩ Ô nhập [" + logLabel + "] đã có nội dung — bỏ qua.");
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
        if (isWardRequiredAndUnfilled(parent, blockIndex)) {
            return false;
        }
        By btnTinh = resolveAdminDropdownButton(parent, blockIndex, false);
        By btnPhuong = findWardButtonForBlock(parent, blockIndex);
        if (btnTinh != null && existsNow(btnTinh) && !isAdminDropdownFilledAt(btnTinh)) {
            return false;
        }
        if (btnPhuong != null && existsNow(btnPhuong) && !isAdminDropdownFilledAt(btnPhuong)) {
            return false;
        }
        return isAddressDetailFilledInBlock(parent, blockIndex);
    }

    private boolean isWardRequiredAndUnfilled(String parentScope, int blockIndex) {
        if (!hasWardLabelVisible(parentScope, blockIndex)) {
            return false;
        }
        By wardBtn = findWardButtonForBlock(parentScope, blockIndex);
        if (wardBtn == null || !existsNow(wardBtn)) {
            return true;
        }
        return !isAdminDropdownFilledAt(wardBtn);
    }

    private boolean hasWardLabelVisible(String parentScope, int blockIndex) {
        String parent = parentScope == null ? "" : parentScope;
        String card = addressCardScope(parent, blockIndex);
        String scope = card != null ? card : parent;
        int idx = card != null ? 1 : blockIndex + 1;
        return hasWardFieldInBlock(scope, idx);
    }

    /** Tìm nút dropdown phường/xã — thử card scope, parent scope, và neo theo nhãn. */
    private By findWardButtonForBlock(String parentScope, int blockIndex) {
        String parent = parentScope == null ? "" : parentScope;
        By resolved = resolveAdminDropdownButton(parent, blockIndex, true);
        if (resolved != null && existsNow(resolved)) {
            return resolved;
        }
        String card = addressCardScope(parent, blockIndex);
        String scope = card != null ? card : parent;
        int idx = card != null ? 1 : blockIndex + 1;
        List<By> candidates = List.of(
                By.xpath("(" + scope + "//label[contains(., 'Phường') and contains(., 'xã')]"
                        + "/following::button[1])[" + idx + "]"),
                By.xpath(scope + "//label[contains(., 'Phường') and contains(., 'xã')]/following::button[1]"),
                By.xpath("(" + parent + ADDRESS_CARD
                        + "//label[contains(., 'Phường') and contains(., 'xã')]/following::button[1])["
                        + (blockIndex + 1) + "]"));
        int hit = firstMaybeExisting(candidates);
        if (hit >= 0) {
            By picked = candidates.get(hit);
            if (existsNow(picked)) {
                return picked;
            }
        }
        if (hit == -2) {
            for (By c : candidates) {
                if (existsNow(c)) {
                    return c;
                }
            }
        }
        return resolved;
    }

    private boolean isAddressDetailFilledInBlock(String scope, int blockIndex) {
        By detail = addressDetailTextareaAtBlock(scope, blockIndex);
        if (!existsNow(detail)) {
            if (blockIndex > 0) {
                return false;
            }
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
                + " | " + scope + "//label[" + label + "]/ancestor::div[1]//button"
                + " | " + scope + "//label[" + label + "]/following::button[1])[" + oneBasedIndex + "]");
    }

    /** Tìm nút dropdown tỉnh/phường — thử card scope rồi parent scope. */
    private By resolveAdminDropdownButton(String parentScope, int blockIndex, boolean phuong) {
        String parent = parentScope == null ? "" : parentScope;
        String card = addressCardScope(parent, blockIndex);
        int idx = blockIndex + 1;
        By inParent = adminDropdownButtonAt(parent, phuong, idx);
        String label = phuong ? "contains(., 'Phường') and contains(., 'xã')"
                : "contains(., 'Tỉnh') and contains(., 'thành phố')";
        By fallback = By.xpath("(" + parent + ADDRESS_CARD + "//label[" + label + "]/following::button[1])[" + (blockIndex + 1) + "]");

        List<By> candidates = new ArrayList<>(3);
        if (card != null) {
            candidates.add(adminDropdownButtonAt(card, phuong, 1));
        }
        candidates.add(inParent);
        candidates.add(fallback);

        // Thử cả 3 ứng viên trong 1 lượt gọi; hit nào cũng được existsNow soát lại đúng như cũ.
        int hit = firstMaybeExisting(candidates);
        if (hit >= 0) {
            By picked = candidates.get(hit);
            if (existsNow(picked)) {
                return picked;
            }
        }
        if (hit != -2) {
            return inParent;
        }
        // Không gộp được (locator không phải XPath) — giữ nguyên đường tuần tự cũ.
        for (By c : candidates) {
            if (existsNow(c)) {
                return c;
            }
        }
        return inParent;
    }

    private By resolveAdminDropdownSearch(String parentScope, int blockIndex, boolean phuong) {
        String parent = parentScope == null ? "" : parentScope;
        String card = addressCardScope(parent, blockIndex);
        if (card != null) {
            By inCard = adminDropdownSearchAt(card, phuong, 1);
            if (existsNow(inCard)) {
                return inCard;
            }
        }
        return adminDropdownSearchAt(parent, phuong, blockIndex + 1);
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
        UiProfiler.enter(UiProfiler.DROPDOWN);
        try {
            selectRandomDropdownOptionInternal(dropdownLocator, searchInput, elementName);
        } finally {
            UiProfiler.exit();
        }
    }

    private void selectRandomDropdownOptionInternal(By dropdownLocator, By searchInput, String elementName) {
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
                logUi(" ⏳ " + elementName + " — thử chọn phường/xã lại (" + (attempt + 1) + "/2)...");
            }
        }
        dismissOpenDropdownsQuiet();
        throw new RuntimeException("❌ Không chọn được phường/xã trong [" + elementName + "].", last);
    }

    private boolean trySelectRandomWardOnce(By dropdownLocator, By searchInput, String elementName) {
        // clickElementQuiet đã tự scrollToElement(dropdownLocator) ngay đầu — gọi thêm ở đây là
        // cuộn 2 lần tới cùng một element, mất thêm 1 lượt tìm element + 120ms nghỉ vô ích.
        clickElementQuiet(dropdownLocator, elementName);
        sleepMillis(WaitConfig.SETTLE_MS);
        List<String> texts = collectValidDropdownOptionTexts(GLOBAL_DROPDOWN_OPTIONS, WaitConfig.DROPDOWN);
        if (!texts.isEmpty()) {
            // Danh sách này vừa đọc từ chính dropdown đang mở, nên mục đã chọn CHẮC CHẮN có ở đó:
            // chỉ cần 1 vòng quét, không phải 4 vòng × 300ms. Trước đây mỗi pick trượt tốn 900ms,
            // mà còn shuffle cả danh sách rồi thử lần lượt.
            List<String> picks = new ArrayList<>(texts);
            Collections.shuffle(picks);
            for (String pick : picks) {
                if (clickOptionInOpenDropdown(GLOBAL_DROPDOWN_OPTIONS, pick, elementName, 1)) {
                    return true;
                }
            }
        }
        // Chỉ tới đây khi dropdown không đọc được mục nào — lúc đó mới cần gõ dò.
        String[] probes = {"Phường", "Xã", "Thị trấn", "P.", "X."};
        if (searchInput != null && existsNow(searchInput)) {
            for (String probe : probes) {
                try {
                    WebElement search = driver.findElement(searchInput);
                    search.clear();
                    search.sendKeys(probe);
                    sleepMillis(WaitConfig.SETTLE_LONG_MS);
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
        // Chịu lực: chặn sendKeys khi option phường chưa render xong.
        sleepMillis(WaitConfig.SETTLE_ASYNC_MS);
        if (searchInput != null && existsNow(searchInput)) {
            try {
                WebElement search = driver.findElement(searchInput);
                search.clear();
                search.sendKeys(expected);
                sleepMillis(WaitConfig.SETTLE_LONG_MS);
            } catch (Exception ignored) {
            }
        }
        // 2 vòng đủ: đã lọc theo từ khoá nên danh sách ngắn và render nhanh. 4 vòng chỉ kéo dài
        // trường hợp "phường này không thuộc tỉnh đang chọn" — vốn là trường hợp phổ biến nhất.
        if (clickOptionInOpenDropdown(GLOBAL_DROPDOWN_OPTIONS, expected, elementName, 2)) {
            return true;
        }
        dismissOpenDropdownsQuiet();
        return false;
    }

    private boolean clickOptionInOpenDropdown(By optionsLocator, String expectedText, String elementName) {
        return clickOptionInOpenDropdown(optionsLocator, expectedText, elementName, 4);
    }

    /**
     * @param rounds số vòng quét lại. Dùng 4 khi chưa biết option đã render chưa; dùng 1 khi text
     *               vừa được đọc ra từ chính dropdown đang mở — quét lại không thêm thông tin gì,
     *               chỉ tốn 300ms mỗi vòng.
     */
    private boolean clickOptionInOpenDropdown(By optionsLocator, String expectedText, String elementName,
                                              int rounds) {
        int total = Math.max(1, rounds);
        for (int round = 0; round < total; round++) {
            List<WebElement> options = driver.findElements(optionsLocator);
            boolean[] maybe = maybeDisplayed(options);
            List<String> allTexts = textsOf(options);
            for (int i = 0; i < options.size(); i++) {
                try {
                    if (!maybe[i]) {
                        continue;
                    }
                    String text = allTexts.get(i);
                    if (!optionMatches(expectedText, text)) {
                        continue;
                    }
                    WebElement option = options.get(i);
                    if (!reallyDisplayed(option)) {
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
                    logUi(" ➔ Chọn Dropdown: '" + text + "' tại [" + elementName + "]");
                    TestActionLog.chon(elementName, text);
                    return true;
                } catch (StaleElementReferenceException ignored) {
                }
            }
            // Không nghỉ sau vòng cuối — nghỉ xong là thoát, chỉ tổ đốt thêm 300ms mỗi lần
            // dropdown không chứa option cần tìm.
            if (round < total - 1) {
                sleepMillis(300);
            }
        }
        return false;
    }

    private boolean clickFirstOptionInOpenDropdown(By optionsLocator, String elementName) {
        for (int round = 0; round < 4; round++) {
            List<WebElement> options = driver.findElements(optionsLocator);
            boolean[] maybe = maybeDisplayed(options);
            List<String> allTexts = textsOf(options);
            for (int i = 0; i < options.size(); i++) {
                try {
                    if (!maybe[i]) {
                        continue;
                    }
                    String text = allTexts.get(i);
                    if (!isSelectableDropdownText(text)) {
                        continue;
                    }
                    WebElement option = options.get(i);
                    if (!reallyDisplayed(option)) {
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
                    logUi(" ➔ Chọn Dropdown (mục đầu): '" + text + "' tại [" + elementName + "]");
                    TestActionLog.chon(elementName, text);
                    return true;
                } catch (StaleElementReferenceException ignored) {
                }
            }
            // Không ngủ sau vòng cuối — hàm sắp trả false (giống clickOptionInOpenDropdown ở trên).
            if (round < 3) {
                sleepMillis(300);
            }
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
            List<WebElement> options = driver.findElements(optionsLocator);
            if (!options.isEmpty()) {
                boolean[] maybe = maybeDisplayed(options);
                List<String> allTexts = textsOf(options);
                for (int i = 0; i < options.size(); i++) {
                    String trimmed = allTexts.get(i);
                    if (maybe[i] && isSelectableDropdownText(trimmed) && !texts.contains(trimmed)
                            && reallyDisplayed(options.get(i))) {
                        texts.add(trimmed);
                    }
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
            logUi(" ⏩ Bỏ qua Dropdown: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không chọn Dropdown");
            return;
        }
        waitUntilVisible(dropdownLocator, WaitConfig.DROPDOWN, elementName);
        if (dropdownAlreadyShows(dropdownLocator, expectedText)) {
            logUi(" ⏩ [" + elementName + "] đã chọn '" + expectedText + "' — không chọn lại.");
            return;
        }
        selectCustomDropdown(dropdownLocator, optionsLocator, expectedText, elementName);
    }

    /** Dropdown đã hiện đúng giá trị (không mở lại menu). */
    private boolean dropdownAlreadyShows(By dropdownLocator, String expectedText) {
        try {
            String current = readAdminDropdownValue(dropdownLocator);
            if (current.isBlank()) {
                current = readElementText(driver.findElement(dropdownLocator)).trim();
            }
            return !current.isBlank() && optionMatches(expectedText, current);
        } catch (Exception e) {
            return false;
        }
    }

    /** Chip/tab (vd. giới tính) đã ở trạng thái chọn. */
    public boolean isChoiceChipSelected(By chipBy) {
        try {
            if (!existsNow(chipBy)) {
                return false;
            }
            WebElement el = driver.findElement(chipBy);
            String aria = el.getAttribute("aria-pressed");
            if ("true".equalsIgnoreCase(aria)) {
                return true;
            }
            aria = el.getAttribute("aria-checked");
            if ("true".equalsIgnoreCase(aria)) {
                return true;
            }
            String cls = el.getAttribute("class");
            if (cls == null) {
                return false;
            }
            String lower = cls.toLowerCase(Locale.ROOT);
            return lower.contains("border-blue")
                    || lower.contains("bg-blue")
                    || lower.contains("ring-2")
                    || lower.contains("selected");
        } catch (Exception e) {
            return false;
        }
    }

    public void clickChoiceChipIfNeeded(By chipBy, String elementName) {
        if (isChoiceChipSelected(chipBy)) {
            logUi(" ⏩ [" + elementName + "] đã chọn — không click lại.");
            return;
        }
        clickElement(chipBy, elementName);
    }

    public void selectToaAnWithCheck(By dropdownLocator, By searchInputLocator, By optionsLocator, String expectedText, String elementName) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            logUi(" ⏩ Bỏ qua Tòa án: [" + elementName + "] do dữ liệu trống.");
            TestActionLog.boQua(elementName, "Giá trị trống — không chọn tòa án");
            return;
        }
        if (!isElementVisible(dropdownLocator)) {
            logUi(" ⏩ Bỏ qua Tòa án: [" + elementName + "] do biểu mẫu ẩn.");
            TestActionLog.boQua(elementName, "Biểu mẫu ẩn / không có trên giao diện");
            return;
        }
        selectDropdownWithSearch(dropdownLocator, searchInputLocator, optionsLocator, expectedText, elementName);
    }

    public void zoomPage(String percentage) {
        // Không dùng document.body.style.zoom — làm lệch layout SPA và ảnh chụp.
        // Giữ method để tương thích gọi cũ; chỉ log nhắc nhở.
        logUi(" ⏩ Bỏ qua zoom " + percentage
                + " (CSS zoom gây xáo layout — chạy ở tỉ lệ 100%).");
    }

    /**
     * Một ảnh tổng quan (viewport hiện tại, cuộn về đầu trang).
     * Dùng cho báo cáo — không chụp nhiều khung theo bước.
     */
    public String takeOverviewScreenshot() {
        if (!screenshotsEnabled()) {
            return null;
        }
        UiProfiler.enter(UiProfiler.ANH);
        try {
            return takeOverviewScreenshotInternal();
        } finally {
            UiProfiler.exit();
        }
    }

    private String takeOverviewScreenshotInternal() {
        try {
            dismissOverlaysForScreenshot();
            scrollWindowTo(0);
            sleepMillis(WaitConfig.SETTLE_MS);
            String shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            return (shot == null || shot.isBlank()) ? null : shot;
        } catch (Exception e) {
            logUi(" ⚠ Không chụp được ảnh tổng quan: " + e.getMessage());
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
            logUi(" ⚠ Không chụp được ảnh (giữ toast): " + e.getMessage());
            return null;
        }
    }

    /**
     * Chụp ảnh kèm thông báo mềm (vd. banner VNeID) — cuộn tới banner inline trước khi chụp;
     * toast góc màn thì giữ viewport (fixed position).
     */
    private String takeScreenshotForFeedback(List<String> messages) {
        if (!screenshotsEnabled()) {
            return null;
        }
        try {
            WebElement vneidBanner = hasVneidInfoMessage(messages) ? findVneidInfoBanner() : null;
            if (vneidBanner != null) {
                scrollToElement(vneidBanner);
                sleepMillis(WaitConfig.SETTLE_SHORT_MS);
            } else {
                WebElement anchor = findVisibleFeedbackElement(messages);
                if (anchor != null) {
                    if (isFloatingToastElement(anchor)) {
                        return takeScreenshotPreserveToast();
                    }
                    scrollToElement(anchor);
                    sleepMillis(WaitConfig.SETTLE_SHORT_MS);
                } else if (hasVneidInfoMessage(messages)) {
                    scrollWindowTo(0);
                    sleepMillis(WaitConfig.SETTLE_SHORT_MS);
                }
            }
            String shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            return (shot == null || shot.isBlank()) ? null : shot;
        } catch (Exception e) {
            logUi(" ⚠ Không chụp được ảnh thông báo: " + e.getMessage());
            return takeScreenshotPreserveToast();
        }
    }

    /** Banner info VNeID — text ngắn, thường ở đầu form bước 2. */
    private WebElement findVneidInfoBanner() {
        String xpath = "//*[contains(normalize-space(.), 'điền sẵn')"
                + " and contains(normalize-space(.), 'VNeID')"
                + " and string-length(normalize-space(.)) < 220]";
        WebElement best = null;
        int bestLen = Integer.MAX_VALUE;
        for (WebElement el : driver.findElements(By.xpath(xpath))) {
            try {
                if (!el.isDisplayed()) {
                    continue;
                }
                String text = readElementText(el);
                if (text == null || text.isBlank() || text.length() >= bestLen) {
                    continue;
                }
                best = el;
                bestLen = text.length();
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    private WebElement findVisibleFeedbackElement(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        WebElement best = null;
        int bestLen = Integer.MAX_VALUE;
        for (String msg : messages) {
            if (msg == null || msg.isBlank()) {
                continue;
            }
            String probe = feedbackSearchProbe(msg);
            if (probe.isBlank()) {
                continue;
            }
            String xpath = "//*[contains(normalize-space(.), '" + probe.replace("'", "") + "')]";
            for (WebElement el : driver.findElements(By.xpath(xpath))) {
                try {
                    if (!el.isDisplayed()) {
                        continue;
                    }
                    String text = readElementText(el);
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    if (text.length() >= bestLen) {
                        continue;
                    }
                    best = el;
                    bestLen = text.length();
                } catch (Exception ignored) {
                }
            }
        }
        return best;
    }

    private static String feedbackSearchProbe(String msg) {
        String trimmed = msg.trim().replaceAll("\\s+", " ");
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("vneid") && lower.contains("điền sẵn")) {
            return "điền sẵn";
        }
        if (trimmed.length() <= 48) {
            return trimmed;
        }
        return trimmed.substring(0, 48);
    }

    private static boolean hasVneidInfoMessage(List<String> messages) {
        if (messages == null) {
            return false;
        }
        for (String msg : messages) {
            if (msg == null || msg.isBlank()) {
                continue;
            }
            String lower = msg.toLowerCase(Locale.ROOT);
            if (lower.contains("vneid") || (lower.contains("định danh") && lower.contains("điền sẵn"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isFloatingToastElement(WebElement el) {
        try {
            WebElement toastHost = el.findElement(By.xpath(
                    "./ancestor-or-self::*[contains(@class,'toast') or contains(@class,'Toastify')"
                            + " or contains(@class,'notification') or contains(@class,'Notification')"
                            + " or contains(@class,'ant-message') or contains(@class,'ant-notification')"
                            + " or contains(@class,'sonner') or @data-sonner-toast][1]"));
            return toastHost != null && toastHost.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Đính 1 ảnh tổng quan vào báo cáo. */
    public void captureOverview(String message) {
        String shot = takeOverviewScreenshot();
        if (shot == null) {
            return;
        }
        // Cố ý KHÔNG kèm URL vào chú thích: cả lượt chạy chỉ có một địa chỉ, dán nó vào từng dòng
        // chỉ làm loãng câu mô tả mà không nói thêm được gì cho người đọc báo cáo.
        BaoCao.logScreenshots(message, List.of(shot));
    }

    /**
     * Công tắc chụp ảnh — nay do {@code ScreenshotStore} giữ.
     * <p>
     * Trước đây cờ nằm riêng ở đây và chỉ chặn ba hàm của lớp này, còn {@code TestListener} gọi
     * thẳng {@code getScreenshotAs} nên tắt cờ vẫn sinh ảnh cho mọi case hỏng.
     */
    private static boolean screenshotsEnabled() {
        return vn.tuphap.automation.report.ScreenshotStore.enabled();
    }

    private void dismissOverlaysForScreenshot() {
        try {
            driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
        } catch (Exception ignored) {
        }
        sleepMillis(100);
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
        sleepMillis(seconds * 1000L);
    }

    public void sleepMillis(long millis) {
        long ms = Math.max(0, millis);
        UiProfiler.addDirect(UiProfiler.SLEEP, ms);
        try {
            Thread.sleep(ms);
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
                logUi(" 🤖 Bắt Captcha từ HTML: " + result);
                return result;
            }

            File srcFile = captchaElement.getScreenshotAs(OutputType.FILE);
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(resolveTessDataPath());
            tesseract.setLanguage("eng");

            result = tesseract.doOCR(srcFile);
            result = result.replaceAll("[^a-zA-Z0-9]", "");
            logUi(" 🤖 AI giải Captcha thành: " + result);

        } catch (Exception e) {
            logUi(" ❌ Lỗi đọc Captcha: " + e.getMessage());
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

}

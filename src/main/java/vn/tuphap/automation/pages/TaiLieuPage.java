package vn.tuphap.automation.pages;

import vn.tuphap.automation.report.TestActionLog;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import vn.tuphap.automation.ui.TestFileHelper;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TaiLieuPage {
    private final WebDriver driver;
    private final WebUI webUI;

    /** UAT mới: khối upload trong card bo góc, mỗi dòng flex + label Tải lên. */
    public static final String UPLOAD_PANEL =
            "//div[contains(@class,'rounded') and contains(@class,'border')"
                    + " and .//input[@type='file'] and .//svg[contains(@class,'lucide-upload')]]";

    /** Marker bước 5 — dùng chung TaoDonFlow / XemLaiGuiDonPage. */
    public static final By MARKER_STEP_READY = By.xpath(
            "//h2[contains(., 'Tài liệu') or contains(., 'chứng cứ') or contains(., 'Chứng cứ')]"
                    + " | " + UPLOAD_PANEL
                    + " | //label[contains(., 'Tải lên') and .//input[@type='file']]"
                    + " | //input[@type='file']/ancestor::div[contains(@class,'rounded') and contains(@class,'border')][1]");

    private final By stepReadyMarker = MARKER_STEP_READY;

    /** Dòng tài liệu bắt buộc — có dấu * đỏ (text-danger). */
    private final By requiredRows = By.xpath(
            UPLOAD_PANEL + "//div[contains(@class,'flex') and contains(@class,'items-center')"
                    + " and .//input[@type='file'] and .//span[contains(@class,'text-danger')]]"
                    + " | //div[contains(@class,'flex') and contains(@class,'items-center')"
                    + " and .//input[@type='file'] and .//*[contains(@class,'text-danger')]]");

    private final By optionalFileInput = By.xpath(
            "//div[contains(., 'Tài liệu bổ sung') or contains(., 'tài liệu bổ sung')]"
                    + "//input[@type='file']"
                    + " | //label[contains(., 'Tài liệu bổ sung')]//input[@type='file']"
                    + " | " + UPLOAD_PANEL + "//div[contains(., 'bổ sung') or contains(., 'Bổ sung')]"
                    + "//input[@type='file']"
                    + " | //div[contains(@class,'rounded') and contains(@class,'border')]"
                    + "[.//input[@type='file'] and (contains(., 'bổ sung') or contains(., 'Bổ sung'))]"
                    + "//input[@type='file']");

    private final By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    private boolean uploadedAnyFile = false;

    public TaiLieuPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void waitStepReady() {
        webUI.waitUntilVisible(stepReadyMarker, WaitConfig.STEP, "Bước 5 [Tài liệu & chứng cứ]");
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
    }

    /**
     * Đính kèm đủ mọi tài liệu bắt buộc (*) trên bước 5.
     * Upload theo tiêu đề (không theo index — DOM đổi sau mỗi lần tải).
     * Ưu tiên PDF (UAT hay từ chối xlsx/docx với giấy tờ tùy thân).
     */
    public void uploadTaiLieuBatBuoc() {
        waitStepReady();

        TestFileHelper.assertExists(TestFileHelper.getSamplePdf());
        TestFileHelper.assertExists(TestFileHelper.getSamplePng());

        List<String> titles = collectRequiredTitles();
        if (titles.isEmpty()) {
            String msg = "Không có thành phần hồ sơ bắt buộc — tải file vào Tài liệu bổ sung";
            System.out.println(" ⚠ " + msg);
            TestActionLog.ghiChu(msg);
            uploadFileToBoSung(TestFileHelper.getSamplePdf());
            return;
        }

        System.out.println(" ⏳ Tải " + titles.size() + " tài liệu bắt buộc (mỗi mục một file)...");
        for (String title : titles) {
            uploadRequiredByTitle(title, 3);
        }

        List<String> missing = findStillMissingTitles();
        for (String title : missing) {
            System.out.println(" ⚠ Còn thiếu [" + title + "] — tải lại PDF...");
            uploadRequiredByTitle(title, 3);
        }

        missing = findStillMissingTitles();
        if (!missing.isEmpty()) {
            throw new RuntimeException(
                    "❌ Chưa đính kèm đủ tài liệu bắt buộc sau khi tải: " + missing);
        }
        System.out.println(" ✅ Đã đính kèm đủ " + titles.size() + " tài liệu bắt buộc.");
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
    }

    /** Thu thập tiêu đề các dòng bắt buộc (ổn định trước khi DOM đổi). */
    private List<String> collectRequiredTitles() {
        Set<String> titles = new LinkedHashSet<>();
        for (WebElement row : findRequiredUploadRows()) {
            try {
                String title = extractDocumentTitle(row);
                if (title != null && !title.isBlank()) {
                    titles.add(title);
                }
            } catch (Exception ignored) {
            }
        }
        return new ArrayList<>(titles);
    }

    private void uploadRequiredByTitle(String title, int maxAttempts) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                WebElement row = findRequiredRowByTitle(title);
                if (row == null) {
                    // Có thể đã đính kèm xong — hàng biến mất khỏi danh sách * đỏ
                    if (!isTitleStillMissing(title)) {
                        System.out.println(" ⏩ [" + title + "] đã có file — bỏ qua.");
                        return;
                    }
                    throw new RuntimeException("Không tìm thấy dòng upload: " + title);
                }
                if (isRowAttached(row)) {
                    // Không tin skip nếu tiêu đề này vẫn còn trong danh sách thiếu
                    // (tránh khớp nhầm sang dòng "Hợp đồng lao động" khi tìm "Quyết định… / …").
                    if (!isTitleStillMissing(title)) {
                        System.out.println(" ⏩ [" + title + "] đã đính kèm — bỏ qua.");
                        return;
                    }
                    System.out.println(" ⚠ [" + title + "] hàng đang mở có vẻ đã có file nhưng tiêu đề vẫn thiếu — tải lại...");
                }

                WebElement input = findFileInputInRow(row);
                if (input == null) {
                    throw new RuntimeException("Không thấy input file trong dòng: " + title);
                }
                String filePath = pickFileForInput(input, title);
                String tenTep = TestFileHelper.displayName(filePath);

                scrollRowIntoView(row);
                ensureFileInputReady(input);
                input.sendKeys(filePath);
                System.out.println(" ➔ Tải lên: '" + tenTep + "' tại [Tài liệu bắt buộc: " + title + "]"
                        + (attempt > 1 ? " (lần " + attempt + ")" : ""));
                TestActionLog.taiLen("Tài liệu bắt buộc: " + title, tenTep);
                uploadedAnyFile = true;

                if (waitUntilRowAttached(title)) {
                    return;
                }
                last = new RuntimeException("Upload chưa được UAT chấp nhận: " + title + " (" + tenTep + ")");
                System.out.println(" ⚠ [" + title + "] chưa thấy xác nhận đính kèm — thử lại...");
            } catch (RuntimeException e) {
                last = e;
            }
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        }
        throw new RuntimeException("❌ Không đính kèm được [" + title + "].", last);
    }

    private WebElement findFileInputInRow(WebElement row) {
        try {
            List<WebElement> inputs = row.findElements(By.xpath(".//input[@type='file']"));
            for (WebElement input : inputs) {
                if (input != null) {
                    return input;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void ensureFileInputReady(WebElement input) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.display='block';"
                            + "arguments[0].style.visibility='visible';"
                            + "arguments[0].style.opacity='1';"
                            + "arguments[0].removeAttribute('disabled');",
                    input);
        } catch (Exception ignored) {
        }
    }

    /** PDF mặc định; nếu accept chỉ ảnh thì dùng PNG. */
    private String pickFileForInput(WebElement input, String title) {
        String accept = "";
        try {
            accept = input.getAttribute("accept");
        } catch (Exception ignored) {
        }
        String a = accept == null ? "" : accept.toLowerCase(Locale.ROOT);
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);

        boolean wantsImage = a.contains("image") || a.contains(".png") || a.contains(".jpg")
                || a.contains(".jpeg") || a.contains("image/");
        boolean rejectsPdf = !a.isBlank() && !a.contains("pdf") && !a.contains("*")
                && (a.contains("image") || a.contains(".png") || a.contains(".jpg"));

        if (wantsImage || rejectsPdf) {
            return TestFileHelper.getSamplePng();
        }
        // Giấy tờ tùy thân / CCCD / ảnh — ưu tiên PDF (rộng nhất trên UAT)
        if (t.contains("cccd") || t.contains("cmnd") || t.contains("hộ chiếu")
                || t.contains("tuỳ thân") || t.contains("tùy thân") || t.contains("giấy tờ tùy thân")) {
            return TestFileHelper.getSamplePdf();
        }
        return TestFileHelper.getSamplePdf();
    }

    private boolean waitUntilRowAttached(String title) {
        long deadline = System.currentTimeMillis() + WaitConfig.FIELD * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (!isTitleStillMissing(title)) {
                return true;
            }
            WebElement row = findRequiredRowByTitle(title);
            if (row != null && isRowAttached(row)) {
                return true;
            }
            webUI.sleepMillis(300);
        }
        return !isTitleStillMissing(title);
    }

    private boolean isTitleStillMissing(String title) {
        for (String missing : findStillMissingTitles()) {
            if (titlesMatch(missing, title)) {
                return true;
            }
        }
        // Còn thấy dòng * đỏ cùng tiêu đề và chưa attached
        WebElement row = findRequiredRowByTitle(title);
        return row != null && !isRowAttached(row);
    }

    private List<String> findStillMissingTitles() {
        List<String> missing = new ArrayList<>();
        for (WebElement row : findRequiredUploadRows()) {
            try {
                if (isRowAttached(row)) {
                    continue;
                }
                String title = extractDocumentTitle(row);
                String text = safeText(row);
                if (text.toLowerCase(Locale.ROOT).contains("chưa đính kèm")
                        || !isRowAttached(row)) {
                    if (title != null && !title.isBlank()) {
                        missing.add(title);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return missing;
    }

    private boolean isRowAttached(WebElement row) {
        try {
            String text = safeText(row).toLowerCase(Locale.ROOT);
            // UAT vẫn hiện "Chưa đính kèm" → chắc chắn chưa xong (không tin hint ".pdf" trên dòng).
            if (text.contains("chưa đính kèm")) {
                return false;
            }
            if (text.contains("đã đính kèm") || text.contains("đã tải lên") || text.contains("đã tải")) {
                return true;
            }
            // Icon check sau upload thành công
            if (!row.findElements(By.xpath(
                    ".//svg[contains(@class,'lucide-check')]"
                            + " | .//*[contains(@class,'text-success') or contains(@class,'text-green')]")).isEmpty()) {
                return true;
            }
            // Chip tên file thật (không dùng hint accept ".pdf/.docx" trên UI)
            if (!row.findElements(By.xpath(
                    ".//*[contains(@class,'truncate')]"
                            + "[contains(., '.pdf') or contains(., '.png') or contains(., '.jpg')"
                            + " or contains(., '.jpeg') or contains(., '.docx') or contains(., '.xlsx')]"
                            + " | .//button[contains(@aria-label,'Xóa') or contains(@aria-label,'xóa')"
                            + " or contains(., 'Xóa') or contains(., 'Gỡ')]")).isEmpty()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement findRequiredRowByTitle(String title) {
        WebElement fuzzy = null;
        String want = normalizeTitle(title);
        for (WebElement row : findRequiredUploadRows()) {
            try {
                String got = normalizeTitle(extractDocumentTitle(row));
                if (got.equals(want)) {
                    return row;
                }
                if (titlesMatch(got, want) && fuzzy == null) {
                    fuzzy = row;
                }
            } catch (Exception ignored) {
            }
        }
        if (fuzzy != null) {
            return fuzzy;
        }
        // Fallback rộng hơn: mọi dòng có file input chứa tiêu đề
        List<WebElement> allRows = driver.findElements(By.xpath(
                "//div[contains(@class,'flex') and .//input[@type='file']]"));
        for (WebElement row : allRows) {
            try {
                if (!row.isDisplayed()) {
                    continue;
                }
                String got = normalizeTitle(extractDocumentTitle(row));
                if (got.equals(want) || titlesMatch(got, want)) {
                    return row;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static boolean titlesMatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String x = normalizeTitle(a);
        String y = normalizeTitle(b);
        if (x.equals(y)) {
            return true;
        }
        // Tiêu đề có "/": chỉ khớp khi MỘT nhánh == toàn bộ tiêu đề kia (không dùng contains —
        // tránh "…/ chấm dứt HĐLĐ" khớp nhầm "Hợp đồng lao động").
        if (x.contains("/")) {
            for (String part : x.split("/")) {
                if (normalizeTitle(part).equals(y)) {
                    return true;
                }
            }
        }
        if (y.contains("/")) {
            for (String part : y.split("/")) {
                if (normalizeTitle(part).equals(x)) {
                    return true;
                }
            }
        }
        String shorter = x.length() <= y.length() ? x : y;
        String longer = x.length() <= y.length() ? y : x;
        return shorter.length() >= 16
                && longer.contains(shorter)
                && shorter.length() * 10 >= longer.length() * 7;
    }

    private static String normalizeTitle(String t) {
        return t.replace("*", "")
                .replace('\u00a0', ' ')
                .replaceAll("\\s*/\\s*", " / ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private void scrollRowIntoView(WebElement row) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});", row);
            webUI.sleepMillis(WaitConfig.SETTLE_SHORT_MS);
        } catch (Exception ignored) {
        }
    }

    private void uploadFileToBoSung(String filePath) {
        if (tryUploadToFirstFileInput(filePath, "Tài liệu bổ sung (fallback — không có hồ sơ bắt buộc)")) {
            uploadedAnyFile = true;
            return;
        }
        throw new RuntimeException(
                "❌ Không có thành phần hồ sơ bắt buộc và không tìm thấy input upload nào trên Bước 5.");
    }

    private boolean tryUploadToFirstFileInput(String filePath, String logLabel) {
        List<WebElement> inputs = driver.findElements(By.xpath("//input[@type='file']"));
        if (inputs.isEmpty()) {
            inputs = driver.findElements(By.xpath(
                    "//label[contains(., 'Tải lên')]//input[@type='file']"
                            + " | //label[.//input[@type='file']]//input[@type='file']"));
        }
        for (WebElement input : inputs) {
            try {
                input.sendKeys(filePath);
                String tenTep = TestFileHelper.displayName(filePath);
                System.out.println(" ➔ Tải lên: '" + tenTep + "' tại [" + logLabel + "]");
                TestActionLog.taiLen(logLabel, tenTep);
                webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
                uploadedAnyFile = true;
                return true;
            } catch (Exception ignored) {
            }
        }
        if (webUI.isElementVisible(optionalFileInput)) {
            webUI.uploadFile(optionalFileInput, filePath, logLabel);
            uploadedAnyFile = true;
            return true;
        }
        return false;
    }

    private List<WebElement> findRequiredUploadRows() {
        List<WebElement> rows = new ArrayList<>();
        for (WebElement row : driver.findElements(requiredRows)) {
            try {
                if (row.isDisplayed()) {
                    rows.add(row);
                }
            } catch (Exception ignored) {
            }
        }
        if (!rows.isEmpty()) {
            return rows;
        }
        List<WebElement> allRows = driver.findElements(By.xpath(
                UPLOAD_PANEL + "//div[contains(@class,'flex') and .//input[@type='file']]"));
        if (allRows.isEmpty()) {
            allRows = driver.findElements(By.xpath(
                    "//div[contains(@class,'flex') and contains(@class,'items-center')"
                            + " and .//input[@type='file'] and .//div[contains(@class,'font-medium')]]"));
        }
        for (WebElement row : allRows) {
            try {
                if (!row.isDisplayed()) {
                    continue;
                }
                String title = extractDocumentTitle(row);
                String text = safeText(row);
                if (title.contains("*") || text.contains("*")
                        || !row.findElements(By.xpath(".//span[contains(@class,'text-danger')]")).isEmpty()
                        || text.toLowerCase(Locale.ROOT).contains("bắt buộc")) {
                    rows.add(row);
                }
            } catch (Exception ignored) {
            }
        }
        return rows;
    }

    private static String extractDocumentTitle(WebElement row) {
        try {
            WebElement titleEl = row.findElement(By.xpath(".//div[contains(@class,'font-medium')]"));
            return titleEl.getText().replace("*", "").trim();
        } catch (Exception e) {
            String text = safeText(row);
            int star = text.indexOf('*');
            if (star > 0) {
                return text.substring(0, star).trim();
            }
            return text.lines().findFirst().orElse("Tài liệu bắt buộc").trim();
        }
    }

    private static String safeText(WebElement el) {
        try {
            String t = el.getText();
            return t == null ? "" : t.replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            return "";
        }
    }

    public void uploadTaiLieuBoSung(String coTaiLieuBoSung) {
        if (coTaiLieuBoSung == null || !coTaiLieuBoSung.trim().equalsIgnoreCase("có")) {
            System.out.println(" ⏩ Bỏ qua [Tài liệu bổ sung] — không yêu cầu.");
            TestActionLog.boQua("Tài liệu bổ sung", "Không yêu cầu");
            return;
        }
        if (tryUploadOptionalBoSung()) {
            return;
        }
        System.out.println(" ⏩ Bỏ qua [Tài liệu bổ sung] — không thấy ô tải lên riêng trên giao diện.");
        TestActionLog.boQua("Tài liệu bổ sung", "Không thấy ô tải lên riêng");
    }

    private boolean tryUploadOptionalBoSung() {
        if (webUI.isElementVisible(optionalFileInput)) {
            webUI.uploadFile(optionalFileInput, TestFileHelper.getSamplePng(), "Tài liệu bổ sung (tùy chọn)");
            uploadedAnyFile = true;
            return true;
        }
        // Ô bổ sung đôi khi không có label rõ — bỏ qua, không đụng input bắt buộc đã điền
        return false;
    }

    public void clickTiepTheo() {
        List<String> missing = findStillMissingTitles();
        if (!missing.isEmpty()) {
            System.out.println(" ⚠ Trước Tiếp theo còn thiếu: " + missing + " — tải bổ sung...");
            for (String title : missing) {
                uploadRequiredByTitle(title, 2);
            }
            missing = findStillMissingTitles();
            if (!missing.isEmpty()) {
                throw new RuntimeException(
                        "❌ Không bấm Tiếp theo được — còn thiếu tài liệu bắt buộc: " + missing);
            }
        }
        if (uploadedAnyFile) {
            webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
        }
        webUI.waitUntilClickable(btnTiepTheo, WaitConfig.STEP, "Nút [Tiếp theo] ở Bước 5");
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước 5", WaitConfig.FIELD);
    }
}

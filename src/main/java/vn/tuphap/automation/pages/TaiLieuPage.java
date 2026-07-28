package vn.tuphap.automation.pages;

import vn.tuphap.automation.report.TestActionLog;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import vn.tuphap.automation.ui.TestFileHelper;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.ArrayList;
import java.util.List;

public class TaiLieuPage {
    private final WebDriver driver;
    private final WebUI webUI;

    /** UAT mới: khối upload trong card bo góc, mỗi dòng flex + label Tải lên. */
    public static final String UPLOAD_PANEL =
            "//div[contains(@class,'rounded') and contains(@class,'border')"
                    + " and .//input[@type='file'] and .//svg[contains(@class,'lucide-upload')]]";

    /** Marker bước 5 — dùng chung TaoDonFlow / XemLaiGuiDonPage (không còn text "Tài liệu bắt buộc"). */
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

    private void uploadFileToBoSung(String filePath) {
        if (tryUploadToFirstFileInput(filePath, "Tài liệu bổ sung (fallback — không có hồ sơ bắt buộc)")) {
            return;
        }
        throw new RuntimeException(
                "❌ Không có thành phần hồ sơ bắt buộc và không tìm thấy input upload nào trên Bước 5.");
    }

    /** File input thường ẩn — thử mọi input[type=file] trên màn Tài liệu. */
    private boolean tryUploadToFirstFileInput(String filePath, String logLabel) {
        List<WebElement> inputs = driver.findElements(By.xpath("//input[@type='file']"));
        if (inputs.isEmpty()) {
            inputs = driver.findElements(By.xpath(
                    "//label[contains(., 'Tải lên')]//input[@type='file']"
                            + " | //label[.//input[@type='file']]//input[@type='file']"));
        }
        for (int i = inputs.size() - 1; i >= 0; i--) {
            try {
                WebElement input = inputs.get(i);
                input.sendKeys(filePath);
                String tenTep = TestFileHelper.displayName(filePath);
                System.out.println(" ➔ Tải lên: '" + tenTep + "' tại [" + logLabel + "]");
                TestActionLog.taiLen(logLabel, tenTep);
                webUI.sleepMillis(WaitConfig.SETTLE_MS);
                return true;
            } catch (Exception ignored) {
            }
        }
        if (webUI.isElementVisible(optionalFileInput)) {
            webUI.uploadFile(optionalFileInput, filePath, logLabel);
            return true;
        }
        return false;
    }

    private final By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    public TaiLieuPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void waitStepReady() {
        webUI.waitUntilVisible(stepReadyMarker, WaitConfig.STEP, "Bước 5 [Tài liệu & chứng cứ]");
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
    }

    public void uploadTaiLieuBatBuoc() {
        waitStepReady();

        TestFileHelper.assertExists(TestFileHelper.getSamplePdf());
        TestFileHelper.assertExists(TestFileHelper.getSampleXlsx());
        TestFileHelper.assertExists(TestFileHelper.getSampleDocx());

        List<WebElement> rows = findRequiredUploadRows();
        if (rows.isEmpty()) {
            String msg = "Không có thành phần hồ sơ bắt buộc — tải file vào Tài liệu bổ sung";
            System.out.println(" ⚠ " + msg);
            TestActionLog.ghiChu(msg);
            uploadFileToBoSung(TestFileHelper.pickRandomUploadFile());
            return;
        }

        System.out.println(" ⏳ Tải " + rows.size() + " tài liệu bắt buộc (PDF/Excel/Word ngẫu nhiên)...");
        for (int i = 0; i < rows.size(); i++) {
            List<WebElement> currentRows = findRequiredUploadRows();
            if (i >= currentRows.size()) {
                throw new RuntimeException("❌ Danh sách tài liệu bắt buộc thay đổi sau khi upload (index " + i + ").");
            }
            WebElement row = currentRows.get(i);
            String tenTaiLieu = extractDocumentTitle(row);
            WebElement input = row.findElement(By.xpath(".//input[@type='file']"));
            String filePath = TestFileHelper.pickRandomUploadFile();
            String tenTep = TestFileHelper.displayName(filePath);
            input.sendKeys(filePath);
            System.out.println(" ➔ Tải lên: '" + tenTep + "' tại [Tài liệu bắt buộc: " + tenTaiLieu + "]");
            TestActionLog.taiLen("Tài liệu bắt buộc: " + tenTaiLieu, tenTep);
            choUploadRowOnDinh(row);
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        }
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
    }

    /** Chờ dòng upload xử lý xong (icon check / tên file) trước khi bấm Tiếp theo. */
    private void choUploadRowOnDinh(WebElement row) {
        long deadline = System.currentTimeMillis() + WaitConfig.FIELD * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                String text = row.getText();
                if (text != null && (text.contains("tệp mẫu") || text.contains(".pdf")
                        || text.contains(".docx") || text.contains(".xlsx"))) {
                    return;
                }
                if (!row.findElements(By.xpath(".//svg[contains(@class,'lucide-check')]")).isEmpty()) {
                    return;
                }
            } catch (Exception ignored) {
            }
            webUI.sleepMillis(300);
        }
    }

    private List<WebElement> findRequiredUploadRows() {
        List<WebElement> rows = new ArrayList<>(driver.findElements(requiredRows));
        if (!rows.isEmpty()) {
            return rows;
        }
        // Fallback: mọi dòng upload trong panel có tiêu đề * (legacy / biến thể markup)
        List<WebElement> allRows = driver.findElements(By.xpath(
                UPLOAD_PANEL + "//div[contains(@class,'flex') and .//input[@type='file']]"));
        if (allRows.isEmpty()) {
            allRows = driver.findElements(By.xpath(
                    "//div[contains(@class,'flex') and contains(@class,'items-center')"
                            + " and .//input[@type='file'] and .//div[contains(@class,'font-medium')]]"));
        }
        for (WebElement row : allRows) {
            try {
                String title = extractDocumentTitle(row);
                if (title.contains("*") || row.findElements(By.xpath(".//span[contains(@class,'text-danger')]")).size() > 0) {
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
            return "Tài liệu bắt buộc";
        }
    }

    public void uploadTaiLieuBoSung(String coTaiLieuBoSung) {
        if (coTaiLieuBoSung == null || !coTaiLieuBoSung.trim().equalsIgnoreCase("có")) {
            System.out.println(" ⏩ Bỏ qua [Tài liệu bổ sung] — không yêu cầu.");
            TestActionLog.boQua("Tài liệu bổ sung", "Không yêu cầu");
            return;
        }
        if (!webUI.isElementVisible(optionalFileInput)) {
            System.out.println(" ⏩ Bỏ qua [Tài liệu bổ sung] — biểu mẫu ẩn.");
            TestActionLog.boQua("Tài liệu bổ sung", "Biểu mẫu ẩn");
            return;
        }
        String samplePng = TestFileHelper.getSamplePng();
        webUI.uploadFile(optionalFileInput, samplePng, "Tài liệu bổ sung (tùy chọn)");
    }

    public void clickTiepTheo() {
        webUI.waitUntilClickable(btnTiepTheo, WaitConfig.STEP, "Nút [Tiếp theo] ở Bước 5");
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước 5", WaitConfig.FIELD);
    }
}

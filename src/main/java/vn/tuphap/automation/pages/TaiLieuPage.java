package vn.tuphap.automation.pages;

import vn.tuphap.automation.report.TestActionLog;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import vn.tuphap.automation.ui.TestFileHelper;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.List;

public class TaiLieuPage {
    private final WebDriver driver;
    private final WebUI webUI;

    private static final String TAILIEU_SECTION =
            "//h2[contains(., 'Tài liệu') and contains(., 'chứng')]/parent::div";

    private final By stepReadyMarker = By.xpath(TAILIEU_SECTION
            + "//div[contains(., 'Tài liệu bắt buộc')]");
    private final By requiredRows = By.xpath(TAILIEU_SECTION
            + "//div[contains(., 'Tài liệu bắt buộc')]/following-sibling::div[1]"
            + "//div[contains(@class, 'flex') and .//input[@type='file']]");
    private final By optionalFileInput = By.xpath(TAILIEU_SECTION
            + "//div[contains(., 'Tài liệu bổ sung')]/following-sibling::label//input[@type='file']");
    private final By btnTiepTheo = By.xpath("//button[contains(., 'Tiếp theo')]");

    public TaiLieuPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public void waitStepReady() {
        webUI.waitUntilVisible(stepReadyMarker, WaitConfig.STEP, "Bước 5 [Tài liệu & chứng cứ]");
    }

    public void uploadTaiLieuBatBuoc() {
        waitStepReady();

        String samplePdf = TestFileHelper.getSamplePdf();
        TestFileHelper.assertExists(samplePdf);

        List<WebElement> rows = driver.findElements(requiredRows);
        if (rows.isEmpty()) {
            throw new RuntimeException("❌ Không tìm thấy tài liệu bắt buộc nào trên Bước 5.");
        }

        System.out.println(" ⏳ Tải " + rows.size() + " tài liệu bắt buộc...");
        int total = rows.size();
        for (int i = 0; i < total; i++) {
            List<WebElement> currentRows = driver.findElements(requiredRows);
            if (i >= currentRows.size()) {
                throw new RuntimeException("❌ Danh sách tài liệu bắt buộc thay đổi sau khi upload (index " + i + ").");
            }
            WebElement row = currentRows.get(i);
            String tenTaiLieu = row.findElement(By.xpath(".//div[contains(@class, 'font-medium')]"))
                    .getText().trim();
            WebElement input = row.findElement(By.xpath(".//input[@type='file']"));
            input.sendKeys(samplePdf);
            System.out.println(" ➔ Tải lên: 'tệp mẫu.pdf' tại [Tài liệu bắt buộc: " + tenTaiLieu + "]");
            TestActionLog.taiLen("Tài liệu bắt buộc: " + tenTaiLieu, "tệp mẫu.pdf");
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
        webUI.clickElement(btnTiepTheo, "Nút [Tiếp theo] ở Bước 5");
    }
}

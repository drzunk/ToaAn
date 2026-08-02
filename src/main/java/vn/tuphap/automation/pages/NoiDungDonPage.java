package vn.tuphap.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.report.TaoDonReportBuilder;
import vn.tuphap.automation.report.TestActionLog;
import vn.tuphap.automation.ui.TestFileHelper;
import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NoiDungDonPage {
    private final WebDriver driver;
    private final WebUI webUI;

    /** Loại biểu mẫu bước 4 — iframe nhúng, tải file, hoặc form textarea legacy. */
    public enum Step4Mode {
        IFRAME, UPLOAD, LEGACY
    }

    private static final String NOIDUNG_SECTION =
            "//h2[contains(., 'Nội dung đơn')]/parent::div";

    /** Wizard bước 4 — loại trừ iframe/text xem trước trên màn Xem lại. */
    private static final String WIZARD_NOIDUNG_SECTION =
            "//h2[contains(., 'Nội dung đơn')]/parent::div"
                    + "[not(ancestor::div[.//h2[contains(., 'Xem lại')]])]";

    /** Iframe biểu mẫu động bước 4 — chỉ trong wizard, không lấy iframe PDF trên Xem lại. */
    public static final By IFRAME_NOI_DUNG = By.xpath(
            WIZARD_NOIDUNG_SECTION + "//iframe[contains(@src,'/f/')]"
                    + " | //iframe[@data-testid='noi-dung-don-iframe']");

    public static final By MARKER_IFRAME_READY = By.cssSelector(".pf-wrap .pf-card");

    private static final By PF_UNPUBLISHED = By.xpath(
            "//*[contains(@class,'pf-wrap')]//h2[contains(., 'Biểu mẫu không tồn tại')"
                    + " or contains(., 'chưa được xuất bản')]");

    private static final By FILE_INPUT_NOI_DUNG = By.xpath(
            NOIDUNG_SECTION + "//input[@type='file']"
                    + " | //h2[contains(., 'Nội dung đơn')]/parent::div//input[@type='file']"
                    + " | //label[contains(., 'Tải lên') and ancestor::div[.//h2[contains(., 'Nội dung')]]]"
                    + "//input[@type='file']"
                    + " | //h2[contains(., 'Nội dung đơn')]/following::input[@type='file'][1]");

    public static final By MARKER_UPLOAD_READY = FILE_INPUT_NOI_DUNG;

    public static final By MARKER_STEP_READY = By.xpath(WIZARD_NOIDUNG_SECTION
            + "//label[contains(., 'Thời điểm phát sinh')]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//label[contains(., 'Thời điểm phát sinh vụ việc')]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//label[contains(., 'Yêu cầu cụ thể')]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//textarea[ancestor::div[.//label[contains(., 'Yêu cầu cụ thể')]]]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//iframe[contains(@src,'/f/')]"
            + " | " + WIZARD_NOIDUNG_SECTION + "//input[@type='file']");

    private final By stepReadyMarker = MARKER_STEP_READY;
    private final By btnTiepTheoWizard = By.xpath(
            "(" + WIZARD_NOIDUNG_SECTION + "//button[contains(., 'Tiếp theo')]"
                    + " | //button[contains(., 'Tiếp theo') and not(ancestor::iframe)])[last()]");
    private final By btnTiepTheoIframe = By.xpath(
            "//div[contains(@class,'pf-nav')]//button[contains(., 'Tiếp theo')]"
                    + " | //div[contains(@class,'pf-wrap')]//button[contains(., 'Tiếp theo')]");

    private Step4Mode step4Mode = Step4Mode.LEGACY;

    /** Chế độ bước 4 vừa resolve — dùng bước 6 quyết định có chờ preview eform hay không. */
    private static volatile Step4Mode lastResolvedStep4Mode = Step4Mode.LEGACY;

    public NoiDungDonPage(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public static boolean wasLastStep4Iframe() {
        return lastResolvedStep4Mode == Step4Mode.IFRAME;
    }

    public boolean isIframeMode() {
        return step4Mode == Step4Mode.IFRAME;
    }

    public boolean isUploadMode() {
        return step4Mode == Step4Mode.UPLOAD;
    }

    public void waitStepReady() {
        step4Mode = resolveStep4Mode();
        lastResolvedStep4Mode = step4Mode;
        logStep4Mode();
        webUI.switchToDefaultContent();
        switch (step4Mode) {
            case IFRAME -> waitIframeFormReady();
            case UPLOAD -> webUI.waitUntilVisible(MARKER_UPLOAD_READY, WaitConfig.STEP,
                    "Bước 4 [Nội dung đơn — tải file]");
            case LEGACY -> webUI.waitUntilVisible(stepReadyMarker, WaitConfig.STEP,
                    "Bước 4 [Nội dung đơn]");
            default -> throw new IllegalStateException("Step4Mode không xác định");
        }
    }

    /**
     * Phân loại bước 4:
     * 1. Có iframe /f/ → nếu báo lỗi chưa xuất bản thì dừng ngay; ngược lại điền trong iframe.
     * 2. Không có iframe hợp lệ nhưng có input file → tải file lên.
     * 3. Còn lại → form legacy (textarea).
     */
    private Step4Mode resolveStep4Mode() {
        webUI.switchToDefaultContent();
        if (webUI.existsNow(IFRAME_NOI_DUNG)) {
            webUI.switchToIframe(IFRAME_NOI_DUNG);
            try {
                if (webUI.existsNow(PF_UNPUBLISHED)) {
                    String msg = readUnpublishedMessage();
                    webUI.switchToDefaultContent();
                    webUI.failStepWithSystemFeedback(4, TaoDonReportBuilder.tenBuocDayDu(4),
                            "Iframe biểu mẫu nhúng báo lỗi", List.of(msg));
                }
                return Step4Mode.IFRAME;
            } finally {
                webUI.switchToDefaultContent();
            }
        }
        if (webUI.existsNow(FILE_INPUT_NOI_DUNG)) {
            return Step4Mode.UPLOAD;
        }
        return Step4Mode.LEGACY;
    }

    /**
     * Chờ form trong iframe sẵn sàng.
     * <p>
     * Không kiểm tra lại {@code PF_UNPUBLISHED}: {@link #resolveStep4Mode()} vừa chạy ngay trước đó
     * đã kiểm tra và dừng case nếu biểu mẫu chưa xuất bản — lặp lại chỉ tốn thêm 1 lần
     * {@code switchToIframe} (mỗi lần là một {@code waitForDisplayedEnabled} trên XPath dài).
     */
    private void waitIframeFormReady() {
        webUI.switchToIframe(IFRAME_NOI_DUNG);
        try {
            webUI.waitUntilVisible(MARKER_IFRAME_READY, WaitConfig.STEP,
                    "Bước 4 [Nội dung đơn — iframe biểu mẫu]");
        } finally {
            webUI.switchToDefaultContent();
        }
    }

    private String readUnpublishedMessage() {
        try {
            WebElement el = driver.findElement(PF_UNPUBLISHED);
            String text = el.getText();
            if (text != null && !text.isBlank()) {
                return text.trim().replaceAll("\\s+", " ");
            }
        } catch (Exception ignored) {
        }
        return "Biểu mẫu không tồn tại hoặc chưa được xuất bản";
    }

    private void logStep4Mode() {
        String label = switch (step4Mode) {
            case IFRAME -> "Biểu mẫu nhúng (iframe)";
            case UPLOAD -> "Tải file nội dung đơn";
            case LEGACY -> "Form nhập liệu (legacy)";
        };
        System.out.println(" ℹ Bước 4 — chế độ: " + label);
        TestActionLog.chon("Bước 4 — loại biểu mẫu", label);
    }

    public void dienForm(String loaiDon,
                         String thoiDiemPhatSinh,
                         String giaTriTranhChap,
                         String tomTatQuaTrinh,
                         String yeuCauCuThe,
                         String canCuPhapLy) {
        waitStepReady();
        if (step4Mode == Step4Mode.UPLOAD) {
            uploadNoiDungFile();
            return;
        }
        if (step4Mode == Step4Mode.IFRAME) {
            fillEmbeddedIframeForm(loaiDon, thoiDiemPhatSinh, giaTriTranhChap,
                    tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy);
            return;
        }
        dienFormLegacyOrIframe(loaiDon, thoiDiemPhatSinh, giaTriTranhChap,
                tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy);
    }

    /**
     * Điền biểu mẫu nhúng trong iframe (pf-wrap) — không phụ thuộc từng loại eform.
     * Ưu tiên map theo nhãn quen thuộc; còn lại quét DOM điền ô trống / bắt buộc.
     */
    private void fillEmbeddedIframeForm(String loaiDon,
                                        String thoiDiemPhatSinh,
                                        String giaTriTranhChap,
                                        String tomTatQuaTrinh,
                                        String yeuCauCuThe,
                                        String canCuPhapLy) {
        webUI.switchToIframe(IFRAME_NOI_DUNG);
        try {
            webUI.waitUntilVisible(MARKER_IFRAME_READY, WaitConfig.FIELD, "Iframe .pf-wrap sẵn sàng");
            logIframeFieldInventory();
            fillIframeByLabelHints(loaiDon, thoiDiemPhatSinh, giaTriTranhChap,
                    tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy);
            int filled = fillGenericIframeInputs(tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy,
                    thoiDiemPhatSinh, giaTriTranhChap);
            System.out.println(" ℹ Iframe eform — đã điền generic thêm " + filled + " ô trống.");
            int dd = fillIframeCustomDropdowns();
            System.out.println(" ℹ Iframe eform — đã chọn " + dd + " dropdown tùy chỉnh.");
            int pendingDd = countPendingDropdowns();
            if (pendingDd > 0) {
                System.out.println(" ⚠ Còn " + pendingDd + " dropdown chưa chọn — thử lần 2.");
                dd += fillIframeCustomDropdowns();
                pendingDd = countPendingDropdowns();
            }
            ensureTableHasRow();
            uploadIframeFilesIfPresent();
            int stillEmpty = countEmptyRequiredLikeFields();
            if (stillEmpty > 0) {
                System.out.println(" ⚠ Iframe còn ~" + stillEmpty + " ô trống sau lần điền 1 — điền lại.");
                fillGenericIframeInputs(tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy,
                        thoiDiemPhatSinh, giaTriTranhChap);
                fillIframeCustomDropdowns();
                ensureTableHasRow();
                uploadIframeFilesIfPresent();
            }
            // Lần cuối: mọi ô trống còn hiện (không chỉ nhãn *) — tránh block wizard.
            int forced = forceFillRemainingEmptyInputs(tomTatQuaTrinh);
            if (forced > 0) {
                System.out.println(" ℹ Iframe — force điền thêm " + forced + " ô trống còn lại.");
                fillIframeCustomDropdowns();
            }
            pendingDd = countPendingDropdowns();
            if (pendingDd > 0) {
                System.out.println(" ⚠ Iframe vẫn còn " + pendingDd
                        + " dropdown 'Chọn' — luồng có thể bị chặn ở Tiếp theo.");
            }
            logEmptyRequiredFields();
            // Commit 1 lần ngay trước Tiếp theo (trong clickTiepTheo) — tránh kích hoạt host 2 lần.
            advanceIframeInternalSteps(tomTatQuaTrinh, yeuCauCuThe, canCuPhapLy,
                    thoiDiemPhatSinh, giaTriTranhChap);
        } finally {
            webUI.switchToDefaultContent();
        }
    }

    /**
     * In danh sách control trong iframe — chỉ để debug khi eform đổi cấu trúc, bật bằng
     * {@code -Dtaodon.debug.eform=true}.
     * <p>
     * Mặc định TẮT vì rất đắt mà không ảnh hưởng kết quả test: mỗi ô tốn ~8 lượt gọi WebDriver
     * (tag/type/name/id/placeholder/class/value/label/isDisplayed), rồi còn quét <b>mọi nút</b>
     * trên trang kèm 1 lệnh JS đọc outerHTML cho từng nút. Đo thực tế: 16 ô + 29 nút cho một
     * eform — vài trăm lượt gọi chỉ để sinh log.
     */
    public void logIframeFieldInventory() {
        if (!"true".equalsIgnoreCase(System.getProperty("taodon.debug.eform", "false"))) {
            return;
        }
        List<WebElement> fields = findIframeEditableFields();
        System.out.println(" 📋 Iframe eform — " + fields.size() + " ô có thể nhập:");
        int i = 0;
        for (WebElement field : fields) {
            i++;
            try {
                String tag = safe(field.getTagName());
                String type = safe(field.getAttribute("type"));
                String name = safe(field.getAttribute("name"));
                String id = safe(field.getAttribute("id"));
                String ph = safe(field.getAttribute("placeholder"));
                String cls = safe(field.getAttribute("class"));
                String val = readFieldValue(field);
                String label = nearestLabelText(field);
                boolean shown = field.isDisplayed();
                System.out.println("   #" + i
                        + " tag=" + tag
                        + (type.isBlank() ? "" : " type=" + type)
                        + " shown=" + shown
                        + " label=[" + abbreviate(label, 60) + "]"
                        + " name=[" + abbreviate(name, 40) + "]"
                        + " id=[" + abbreviate(id, 40) + "]"
                        + " ph=[" + abbreviate(ph, 40) + "]"
                        + " class=[" + abbreviate(cls, 50) + "]"
                        + " value=[" + abbreviate(val, 40) + "]");
            } catch (Exception e) {
                System.out.println("   #" + i + " (không đọc được: " + e.getMessage() + ")");
            }
        }
        List<WebElement> buttons = driver.findElements(By.cssSelector(
                ".pf-wrap button, .pf-nav button, button"));
        System.out.println(" 📋 Iframe eform — nút (" + buttons.size() + "):");
        int b = 0;
        for (WebElement btn : buttons) {
            try {
                if (!btn.isDisplayed()) {
                    continue;
                }
                b++;
                String parentHtml = "";
                try {
                    parentHtml = abbreviate(safe((String) ((org.openqa.selenium.JavascriptExecutor) driver)
                            .executeScript("return arguments[0].parentElement"
                                    + "?arguments[0].parentElement.outerHTML:''", btn)), 200);
                } catch (Exception ignored) {
                }
                System.out.println("   [B" + b + "] '" + abbreviate(safe(btn.getText()), 60)
                        + "' enabled=" + btn.isEnabled()
                        + " class=[" + abbreviate(safe(btn.getAttribute("class")), 40) + "]"
                        + " parent=[" + parentHtml + "]");
            } catch (Exception ignored) {
            }
        }
    }

    /** Điền theo gợi ý nhãn — khớp mềm, không bắt buộc đủ 5 field legacy. */
    private void fillIframeByLabelHints(String loaiDon,
                                        String thoiDiemPhatSinh,
                                        String giaTriTranhChap,
                                        String tomTatQuaTrinh,
                                        String yeuCauCuThe,
                                        String canCuPhapLy) {
        // Đọc nhãn/placeholder/name/aria-label/value/trạng thái của MỌI ô trong 1 lệnh JS, dùng lại
        // cho cả 5 nhóm gợi ý bên dưới. Trước đây mỗi nhóm lại duyệt hết các ô và hỏi WebDriver
        // từng thuộc tính một (~9 lượt/ô × 16 ô × 5 nhóm ≈ 700 lượt gọi cho một bước 4).
        List<WebElement> fields = findIframeEditableFields();
        List<FieldMeta> meta = readFieldMetaBulk(fields);

        fillFirstMatchingField(fields, meta, List.of("thời điểm", "phát sinh", "ngày xảy", "ngày tháng"),
                thoiDiemPhatSinh, true, "Thời điểm / ngày vụ việc (iframe)");
        if (DataDictionary.hasGiaTriTranhChap(loaiDon)) {
            fillFirstMatchingField(fields, meta, List.of("giá trị tranh chấp", "giá trị", "số tiền", "thiệt hại"),
                    giaTriTranhChap, false, "Giá trị / số tiền (iframe)");
        }
        fillFirstMatchingField(fields, meta, List.of("tóm tắt", "quá trình", "sự việc", "diễn biến", "nội dung vụ"),
                tomTatQuaTrinh, false, "Tóm tắt sự việc (iframe)");
        fillFirstMatchingField(fields, meta, List.of("yêu cầu cụ thể", "yêu cầu", "đề nghị"),
                yeuCauCuThe, false, "Yêu cầu (iframe)");
        fillFirstMatchingField(fields, meta, List.of("căn cứ pháp lý", "căn cứ", "điều luật", "pháp lý"),
                canCuPhapLy, false, "Căn cứ pháp lý (iframe)");
    }

    /** Thông tin nhận dạng của một ô, đọc sẵn bằng JS để khỏi hỏi WebDriver từng thuộc tính. */
    private record FieldMeta(String blob, String value, boolean shown, boolean enabled) {
    }

    private static final String FIELD_META_JS =
            "function lab(el){"
                    + "  if(el.id){var q;try{q=document.querySelector('label[for=\"'+el.id.replace(/\"/g,'\\\\\"')+'\"]');}catch(e){q=null;}"
                    + "    if(q) return q.innerText||'';}"
                    + "  var p=el.parentElement;"
                    + "  while(p && p!==document.body){var l=p.querySelector('label');"
                    + "    if(l) return l.innerText||''; p=p.parentElement;}"
                    + "  return '';}"
                    + "return arguments[0].map(function(el){"
                    + "  return [lab(el),"
                    + "          el.getAttribute('placeholder')||'',"
                    + "          el.getAttribute('name')||'',"
                    + "          el.getAttribute('aria-label')||'',"
                    + "          (el.value==null?'':String(el.value)),"
                    + "          (el.offsetParent!==null||el.getClientRects().length>0)?'1':'0',"
                    + "          el.disabled?'0':'1'];});";

    @SuppressWarnings("unchecked")
    private List<FieldMeta> readFieldMetaBulk(List<WebElement> fields) {
        List<FieldMeta> out = new ArrayList<>();
        if (fields.isEmpty()) {
            return out;
        }
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(FIELD_META_JS, fields);
            for (Object row : (List<Object>) raw) {
                List<Object> c = (List<Object>) row;
                String blob = (safe((String) c.get(0)) + " " + safe((String) c.get(1))
                        + " " + safe((String) c.get(2)) + " " + safe((String) c.get(3)))
                        .toLowerCase(Locale.ROOT);
                out.add(new FieldMeta(blob, safe((String) c.get(4)),
                        "1".equals(c.get(5)), "1".equals(c.get(6))));
            }
        } catch (Exception e) {
            System.out.println(" ⚠ Không đọc gộp được thuộc tính ô iframe (" + e.getMessage()
                    + ") — quay lại đọc từng ô.");
            out.clear();
        }
        return out;
    }

    private void fillFirstMatchingField(List<WebElement> fields, List<FieldMeta> meta,
                                        List<String> labelHints, String value,
                                        boolean maskedDate, String logName) {
        if (value == null || value.isBlank()) {
            return;
        }
        boolean coMeta = meta.size() == fields.size();
        for (int i = 0; i < fields.size(); i++) {
            WebElement field = fields.get(i);
            try {
                String blob;
                String hienTai;
                if (coMeta) {
                    FieldMeta m = meta.get(i);
                    if (!m.shown() || !m.enabled()) {
                        continue;
                    }
                    blob = m.blob();
                    hienTai = m.value();
                } else {
                    // Đường lùi khi lệnh JS gộp lỗi — giữ nguyên cách cũ để không mất khả năng điền.
                    if (!field.isDisplayed() || !field.isEnabled()) {
                        continue;
                    }
                    blob = (nearestLabelText(field) + " " + safe(field.getAttribute("placeholder"))
                            + " " + safe(field.getAttribute("name")) + " " + safe(field.getAttribute("aria-label")))
                            .toLowerCase(Locale.ROOT);
                    hienTai = readFieldValue(field);
                }
                boolean match = false;
                for (String hint : labelHints) {
                    if (blob.contains(hint.toLowerCase(Locale.ROOT))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                if (!hienTai.isBlank()) {
                    return;
                }
                typeIntoField(field, value, maskedDate, logName);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Fallback: điền mọi ô trống còn lại trong iframe theo type/nhãn.
     * @return số ô đã điền
     */
    private int fillGenericIframeInputs(String tomTat, String yeuCau, String canCu,
                                        String thoiDiem, String giaTri) {
        fillIframeRadiosAndCheckboxes();
        int filled = 0;
        List<WebElement> fields = findIframeEditableFields();
        int textAreaIdx = 0;
        for (WebElement field : fields) {
            try {
                if (!field.isDisplayed() || !field.isEnabled()) {
                    continue;
                }
                if (!readFieldValue(field).isBlank()) {
                    continue;
                }
                String tag = safe(field.getTagName()).toLowerCase(Locale.ROOT);
                String type = safe(field.getAttribute("type")).toLowerCase(Locale.ROOT);
                String blob = (nearestLabelText(field) + " " + safe(field.getAttribute("placeholder"))
                        + " " + safe(field.getAttribute("name")) + " " + safe(field.getAttribute("aria-label")))
                        .toLowerCase(Locale.ROOT);

                if ("select".equals(tag)) {
                    selectFirstEnabledOption(field);
                    filled++;
                    continue;
                }
                if ("time".equals(type) || blob.contains("giờ") || blob.contains("phút")) {
                    typeIntoField(field, "09:00", false, "Ô giờ (generic iframe)");
                    filled++;
                    continue;
                }
                if ("date".equals(type) || blob.contains("ngày") || blob.contains("thời điểm")
                        || blob.contains("dd/mm") || blob.contains("dd-mm")) {
                    String v = toHtmlDate(thoiDiem);
                    typeIntoField(field, v, false, "Ô ngày (generic iframe)");
                    filled++;
                    continue;
                }
                if ("email".equals(type) || blob.contains("email") || blob.contains("thư điện tử")) {
                    typeIntoField(field, "eform.probe@test.example.com", false, "Ô email (generic iframe)");
                    filled++;
                    continue;
                }
                if ("tel".equals(type) || blob.contains("điện thoại") || blob.contains("sđt")
                        || blob.contains("so dien thoai")) {
                    typeIntoField(field, "0912345678", false, "Ô SĐT (generic iframe)");
                    filled++;
                    continue;
                }
                if (blob.contains("cccd") || blob.contains("định danh") || blob.contains("cmnd")
                        || blob.contains("hộ chiếu")) {
                    typeIntoField(field, "001099012345", false, "Ô CCCD (generic iframe)");
                    filled++;
                    continue;
                }
                if (blob.contains("họ tên") || blob.contains("họ và tên") || blob.contains("ho ten")) {
                    typeIntoField(field, "Nguyễn Văn Probe", false, "Ô họ tên (generic iframe)");
                    filled++;
                    continue;
                }
                if ("number".equals(type) || blob.contains("số lượng") || blob.contains("số tiền")
                        || blob.contains("giá trị") || blob.contains("tiền")) {
                    String v = blob.contains("số lượng") ? "2"
                            : ((giaTri != null && !giaTri.isBlank()) ? giaTri.replaceAll("\\D", "") : "1000000");
                    if (v.isBlank()) {
                        v = "2";
                    }
                    typeIntoField(field, v, false, "Ô số (generic iframe)");
                    filled++;
                    continue;
                }
                if ("textarea".equals(tag) || "true".equalsIgnoreCase(safe(field.getAttribute("contenteditable")))
                        || "textbox".equalsIgnoreCase(safe(field.getAttribute("role")))) {
                    String v;
                    if (textAreaIdx == 0 && tomTat != null && !tomTat.isBlank()) {
                        v = tomTat;
                    } else if (textAreaIdx == 1 && yeuCau != null && !yeuCau.isBlank()) {
                        v = yeuCau;
                    } else if (textAreaIdx >= 2 && canCu != null && !canCu.isBlank()) {
                        v = canCu;
                    } else {
                        v = "Nội dung kiểm thử tự động.";
                    }
                    textAreaIdx++;
                    typeIntoField(field, v, false, "Ô văn bản (generic iframe)");
                    filled++;
                    continue;
                }
                // input text mặc định — bảng dữ liệu / nơi đăng ký…
                typeIntoField(field, blob.contains("bảng") ? "Dòng 1" : "Kiểm thử eform",
                        false, "Ô nhập (generic iframe)");
                filled++;
            } catch (Exception ignored) {
            }
        }
        return filled;
    }

    /** Dropdown custom eform: {@code button.inp} — "— Chọn —". */
    private int fillIframeCustomDropdowns() {
        return EformDropdownHelper.fillPending(driver, webUI);
    }
    private void fillIframeRadiosAndCheckboxes() {
        // Chọn radio/checkbox bắt buộc còn trống — ưu tiên Có / Đồng ý / mục đầu.
        List<WebElement> radios = driver.findElements(By.cssSelector(
                ".pf-wrap input[type='radio'], input[type='radio']"));
        java.util.Set<String> doneNames = new java.util.HashSet<>();
        for (WebElement radio : radios) {
            try {
                if (!radio.isDisplayed() || !radio.isEnabled()) {
                    continue;
                }
                String name = safe(radio.getAttribute("name"));
                if (!name.isBlank() && !doneNames.add(name)) {
                    continue;
                }
                if (radio.isSelected()) {
                    continue;
                }
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", radio);
                System.out.println(" ➔ Chọn radio (generic iframe): " + abbreviate(nearestLabelText(radio), 40));
            } catch (Exception ignored) {
            }
        }
        List<WebElement> checks = driver.findElements(By.cssSelector(
                ".pf-wrap input[type='checkbox'], input[type='checkbox']"));
        for (WebElement check : checks) {
            try {
                if (!check.isDisplayed() || !check.isEnabled() || check.isSelected()) {
                    continue;
                }
                String blob = nearestLabelText(check).toLowerCase(Locale.ROOT);
                if (blob.contains("bắt buộc") || blob.contains("*") || blob.contains("đồng ý")
                        || blob.contains("xác nhận") || blob.isBlank()) {
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", check);
                    System.out.println(" ➔ Chọn checkbox (generic iframe): " + abbreviate(blob, 40));
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Ép React/controlled inputs commit giá trị trước khi wizard hỏi iframe (postMessage).
     * Tránh toast "Biểu mẫu chưa phản hồi" khi ô đã có value DOM nhưng state chưa sync.
     */
    private void commitIframeFieldState() {
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var els=[...document.querySelectorAll("
                            + "'.pf-wrap input,.pf-wrap textarea,.pf-wrap select,"
                            + "input,textarea,select')];"
                            + "els.forEach(function(el){try{"
                            + "  el.dispatchEvent(new Event('input',{bubbles:true}));"
                            + "  el.dispatchEvent(new Event('change',{bubbles:true}));"
                            + "  el.dispatchEvent(new Event('blur',{bubbles:true}));"
                            + "}catch(e){}});"
                            + "if(document.activeElement&&document.activeElement.blur){"
                            + "  document.activeElement.blur();}");
            webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
            System.out.println(" ℹ Iframe — đã commit state các ô trước khi bấm Tiếp theo.");
        } catch (Exception e) {
            System.out.println(" ⚠ Không commit được state iframe: " + e.getMessage());
        }
    }

    /** Bấm [Gửi ngay] trong iframe nếu có — host yêu cầu trước khi wizard nhận postMessage. */
    private boolean clickGuiNgayInIframe() {
        By btnGuiNgay = By.xpath(
                "//button[contains(normalize-space(.), 'Gửi ngay') or contains(normalize-space(.), 'Gửi Ngay')"
                        + " or contains(normalize-space(.), 'GUI NGAY')]"
                        + " | //div[contains(@class,'pf-nav')]//button[contains(., 'Gửi')]"
                        + " | //div[contains(@class,'pf-wrap')]//button[contains(., 'Gửi ngay')]"
                        + " | //*[@role='button' and contains(., 'Gửi ngay')]");
        if (webUI.existsNow(btnGuiNgay)) {
            webUI.clickElementOnceJs(btnGuiNgay, "Nút [Gửi ngay] trong iframe eform", WaitConfig.FIELD);
            webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
            System.out.println(" ℹ Iframe — đã bấm [Gửi ngay] đồng bộ nội dung với host.");
            return true;
        }
        try {
            Object clicked = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var btns=[...document.querySelectorAll('button,[role=button]')];"
                            + "for (var b of btns){"
                            + " if(!b.offsetParent&&b.getBoundingClientRect().height===0) continue;"
                            + " var t=(b.textContent||'').replace(/\\s+/g,' ').trim();"
                            + " if(/gửi\\s*ngay/i.test(t)||(/^gửi$/i.test(t)&&!/đơn/i.test(t))){"
                            + "   b.click(); return t;"
                            + " }"
                            + "}"
                            + "return null;");
            if (clicked != null && !String.valueOf(clicked).isBlank()) {
                webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
                System.out.println(" ℹ Iframe — đã bấm [" + clicked + "] (JS) đồng bộ host.");
                return true;
            }
        } catch (Exception e) {
            System.out.println(" ⚠ Không bấm được Gửi ngay trong iframe: " + e.getMessage());
        }
        return false;
    }

    /** Kích hoạt dirty state iframe — host cần trước khi nhận postMessage. */
    private void nudgeIframeDirtyState() {
        try {
            List<WebElement> fields = findIframeEditableFields();
            for (WebElement field : fields) {
                try {
                    if (!field.isDisplayed() || !field.isEnabled()) {
                        continue;
                    }
                    if ("text".equalsIgnoreCase(safe(field.getAttribute("type")))
                            || "textarea".equalsIgnoreCase(safe(field.getTagName()))) {
                        field.click();
                        field.sendKeys(Keys.END);
                        field.sendKeys(Keys.BACK_SPACE);
                        field.sendKeys(" ");
                        field.sendKeys(Keys.BACK_SPACE);
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            commitIframeFieldState();
        } catch (Exception e) {
            System.out.println(" ⚠ Không nudge được iframe: " + e.getMessage());
        }
    }

    /** Chờ toast host ghi nhận nội dung eform sau Gửi ngay / commit. */
    private void waitForEformHostAck(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (WebUI.hasEformAckInFeedback(webUI.collectSystemFeedbackMessages())) {
                System.out.println(" ✅ Host đã ghi nhận nội dung eform.");
                return;
            }
            webUI.sleepMillis(300);
        }
        System.out.println(" ℹ Chưa thấy toast ghi nhận eform — vẫn thử Tiếp theo wizard.");
    }

    /** Commit + Gửi ngay (nếu có) + chờ ack trước khi bấm Tiếp theo wizard. */
    private void syncIframeEformWithHost() {
        // PHẢI giữ commit này: commit bên trong nudgeIframeDirtyState() nằm trong khối try, nếu
        // vòng tìm ô ném lỗi thì nó không chạy → host không nhận nội dung → bước 4 kẹt 15+10s rồi
        // fail. Đã dính đúng lỗi đó khi thử bỏ. (Lần commit thứ 3 ở clickTiepTheo mới là lần thừa.)
        commitIframeFieldState();
        nudgeIframeDirtyState();
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (clickGuiNgayInIframe()) {
                break;
            }
            // Không nghỉ sau lần thử cuối — sau nó là thoát vòng lặp, không thử lại nữa.
            if (attempt < 3) {
                webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
            }
        }
        webUI.switchToDefaultContent();
        waitForEformHostAck(WaitConfig.EFORM_ACK_MS);
    }

    /**
     * Sau Chỉnh sửa từ Xem lại: ghi đè ít nhất một ô iframe để host nhận thay đổi.
     */
    public void prepareIframeResubmitAfterEdit(String yeuCauMoi) {
        waitStepReady();
        webUI.switchToIframe(IFRAME_NOI_DUNG);
        try {
            webUI.waitUntilVisible(MARKER_IFRAME_READY, WaitConfig.FIELD, "Iframe .pf-wrap sẵn sàng");
            String text = (yeuCauMoi == null || yeuCauMoi.isBlank())
                    ? "Kiểm thử eform — cập nhật sau chỉnh sửa." : yeuCauMoi;
            overwriteFirstMatchingField(
                    List.of("chi tiết hộ tịch", "chi tiết", "yêu cầu", "hộ tịch", "nội dung"),
                    text, "Cập nhật nội dung (iframe sau chỉnh sửa)");
            nudgeIframeDirtyState();
        } finally {
            webUI.switchToDefaultContent();
        }
    }

    private void overwriteFirstMatchingField(List<String> labelHints, String value, String logName) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (WebElement field : findIframeEditableFields()) {
            try {
                if (!field.isDisplayed() || !field.isEnabled()) {
                    continue;
                }
                String blob = (nearestLabelText(field) + " " + safe(field.getAttribute("placeholder"))
                        + " " + safe(field.getAttribute("name")) + " " + safe(field.getAttribute("aria-label")))
                        .toLowerCase(Locale.ROOT);
                boolean match = false;
                for (String hint : labelHints) {
                    if (blob.contains(hint.toLowerCase(Locale.ROOT))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                typeIntoField(field, value, false, logName);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    /** Bảng dữ liệu eform — bấm Thêm dòng nếu chưa có hàng. */
    private void ensureTableHasRow() {
        By themDong = By.xpath(
                "//button[contains(., 'Thêm dòng') or contains(., 'Thêm hàng') or contains(., 'Thêm mới')]");
        if (!webUI.existsNow(themDong)) {
            return;
        }
        try {
            Object rowCount = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "return document.querySelectorAll('table tbody tr, .pf-wrap [role=row]').length;");
            int rows = rowCount == null ? 0 : ((Number) rowCount).intValue();
            if (rows > 0) {
                return;
            }
            webUI.clickElement(themDong, "Nút [Thêm dòng] trong eform", WaitConfig.FIELD);
            webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
            System.out.println(" ➔ Thêm dòng bảng dữ liệu eform");
        } catch (Exception e) {
            System.out.println(" ⚠ Không thêm dòng bảng eform: " + e.getMessage());
        }
    }

    private void uploadIframeFilesIfPresent() {
        List<WebElement> files = driver.findElements(By.cssSelector(
                ".pf-wrap input[type='file'], input[type='file']"));
        for (WebElement file : files) {
            try {
                String existing = safe(file.getAttribute("value"));
                if (!existing.isBlank()) {
                    continue;
                }
                String path = TestFileHelper.pickRandomUploadFile();
                file.sendKeys(path);
                System.out.println(" ➔ Upload file eform: " + abbreviate(path, 80));
                TestActionLog.dien("File eform (iframe)", path);
                webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
            } catch (Exception e) {
                System.out.println(" ⚠ Không upload file trong eform: " + e.getMessage());
            }
        }
    }

    /** Điền mọi ô trống còn hiện — kể cả không có dấu *. */
    private int forceFillRemainingEmptyInputs(String fallbackText) {
        int filled = 0;
        String fallback = (fallbackText == null || fallbackText.isBlank())
                ? "Nội dung kiểm thử tự động." : fallbackText;
        for (WebElement field : findIframeEditableFields()) {
            try {
                if (!field.isDisplayed() || !field.isEnabled()) {
                    continue;
                }
                if (!readFieldValue(field).isBlank()) {
                    continue;
                }
                String tag = safe(field.getTagName()).toLowerCase(Locale.ROOT);
                String type = safe(field.getAttribute("type")).toLowerCase(Locale.ROOT);
                String label = nearestLabelText(field);
                if ("select".equals(tag)) {
                    selectFirstEnabledOption(field);
                    filled++;
                    continue;
                }
                if ("date".equals(type)) {
                    typeIntoField(field, "2024-01-15", false, "Force ngày (iframe)");
                } else if ("time".equals(type)) {
                    typeIntoField(field, "09:00", false, "Force giờ (iframe)");
                } else if ("number".equals(type)) {
                    typeIntoField(field, "1", false, "Force số (iframe)");
                } else if ("email".equals(type)) {
                    typeIntoField(field, "eform.probe@test.example.com", false, "Force email (iframe)");
                } else {
                    typeIntoField(field, fallback, false,
                            "Force ô trống [" + abbreviate(label, 40) + "]");
                }
                if (!readFieldValue(field).isBlank()) {
                    filled++;
                } else {
                    System.out.println(" ⚠ Force điền thất bại: [" + abbreviate(label, 60)
                            + "] tag=" + tag + " type=" + type);
                }
            } catch (Exception ignored) {
            }
        }
        return filled;
    }

    private void logEmptyRequiredFields() {
        int emptyAll = 0;
        for (WebElement field : findIframeEditableFields()) {
            try {
                if (!field.isDisplayed() || !field.isEnabled()) {
                    continue;
                }
                if (!readFieldValue(field).isBlank()) {
                    continue;
                }
                emptyAll++;
                String label = nearestLabelText(field);
                boolean required = label.contains("*")
                        || label.toLowerCase(Locale.ROOT).contains("bắt buộc")
                        || "true".equalsIgnoreCase(safe(field.getAttribute("required")))
                        || "true".equalsIgnoreCase(safe(field.getAttribute("aria-required")));
                System.out.println((required ? " ⚠ Ô bắt buộc còn trống: " : " ⚠ Ô trống còn lại: ")
                        + "[" + abbreviate(label, 80) + "]"
                        + " tag=" + field.getTagName()
                        + " type=" + safe(field.getAttribute("type"))
                        + " name=[" + abbreviate(safe(field.getAttribute("name")), 40) + "]"
                        + " id=[" + abbreviate(safe(field.getAttribute("id")), 40) + "]");
            } catch (Exception ignored) {
            }
        }
        if (emptyAll == 0) {
            System.out.println(" ✅ Iframe — không còn ô nhập trống hiển thị.");
        }
        int pending = countPendingDropdowns();
        if (pending > 0) {
            System.out.println(" ⚠ Dropdown bắt buộc/placeholder còn: " + pending);
        }
    }

    private int countPendingDropdowns() {
        try {
            Object n = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var isPlaceholder=function(t){"
                            + "t=(t||'').replace(/\\u00a0/g,' ').trim().toLowerCase();"
                            + "return !t || t.indexOf('chọn')>=0 || t==='▾' || t.charAt(0)==='—' || t.charAt(0)==='-';"
                            + "};"
                            + "return [...document.querySelectorAll('button.inp')].filter(b=>"
                            + "!b.disabled && b.getAttribute('data-skip-dd')!=='1'"
                            + " && isPlaceholder(b.innerText||b.textContent)).length;");
            return n == null ? 0 : ((Number) n).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private int countEmptyRequiredLikeFields() {
        int n = 0;
        for (WebElement field : findIframeEditableFields()) {
            try {
                if (!field.isDisplayed() || !field.isEnabled()) {
                    continue;
                }
                if (readFieldValue(field).isBlank()) {
                    n++;
                }
            } catch (Exception ignored) {
            }
        }
        return n;
    }

    /** Chuẩn hóa ngày → yyyy-MM-dd cho input[type=date]. */
    private static String toHtmlDate(String ddMMyyyy) {
        if (ddMMyyyy == null || ddMMyyyy.isBlank()) {
            return "2024-01-15";
        }
        String t = ddMMyyyy.trim();
        if (t.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return t;
        }
        if (t.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) {
            String[] p = t.split("/");
            return String.format("%s-%02d-%02d", p[2], Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        }
        return "2024-01-15";
    }

    private List<WebElement> findIframeEditableFields() {
        return driver.findElements(By.cssSelector(
                ".pf-wrap input:not([type='hidden']):not([type='file']):not([type='checkbox'])"
                        + ":not([type='radio']):not([type='button']):not([type='submit']):not([type='image']),"
                        + " .pf-wrap textarea,"
                        + " .pf-wrap select,"
                        + " .pf-wrap [contenteditable='true'],"
                        + " .pf-wrap [role='textbox'],"
                        + " input:not([type='hidden']):not([type='file']):not([type='checkbox'])"
                        + ":not([type='radio']):not([type='button']):not([type='submit']),"
                        + " textarea, select, [contenteditable='true'], [role='textbox']"));
    }

    private void typeIntoField(WebElement field, String value, boolean maskedDate, String logName) {
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});", field);
        } catch (Exception ignored) {
        }
        String type = safe(field.getAttribute("type")).toLowerCase(Locale.ROOT);
        // input[type=date|time|number] — set value bằng JS (sendKeys định dạng UI hay fail).
        if ("date".equals(type) || "time".equals(type) || "number".equals(type) || "email".equals(type)) {
            try {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "var el=arguments[0], v=arguments[1];"
                                + "el.focus();"
                                + "var proto=window.HTMLInputElement.prototype;"
                                + "var setter=Object.getOwnPropertyDescriptor(proto,'value');"
                                + "if(setter&&setter.set){setter.set.call(el,v);}else{el.value=v;}"
                                + "el.dispatchEvent(new Event('input',{bubbles:true}));"
                                + "el.dispatchEvent(new Event('change',{bubbles:true}));"
                                + "el.blur();",
                        field, value);
                System.out.println(" ➔ Điền: '" + abbreviate(value, 80) + "' vào [" + logName + "]");
                TestActionLog.dien(logName, value);
                return;
            } catch (Exception ignored) {
            }
        }
        try {
            field.click();
        } catch (Exception ignored) {
        }
        try {
            field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        } catch (Exception ignored) {
        }
        field.sendKeys(value);
        if (readFieldValue(field).isBlank()) {
            try {
                ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "var el=arguments[0], v=arguments[1];"
                                + "el.focus();"
                                + "var proto=/textarea/i.test(el.tagName)"
                                + "?window.HTMLTextAreaElement.prototype:window.HTMLInputElement.prototype;"
                                + "var setter=Object.getOwnPropertyDescriptor(proto,'value');"
                                + "if(setter&&setter.set){setter.set.call(el,v);}else{el.textContent=v;}"
                                + "el.dispatchEvent(new Event('input',{bubbles:true}));"
                                + "el.dispatchEvent(new Event('change',{bubbles:true}));",
                        field, value);
            } catch (Exception ignored) {
            }
        }
        System.out.println(" ➔ Điền: '" + abbreviate(value, 80) + "' vào [" + logName + "]");
        TestActionLog.dien(logName, value);
    }

    private void selectFirstEnabledOption(WebElement select) {
        List<WebElement> options = select.findElements(By.tagName("option"));
        for (WebElement opt : options) {
            String v = safe(opt.getAttribute("value"));
            String t = safe(opt.getText()).trim();
            if (v.isBlank() && t.isBlank()) {
                continue;
            }
            if ("--".equals(t) || t.toLowerCase(Locale.ROOT).contains("chọn")) {
                continue;
            }
            opt.click();
            System.out.println(" ➔ Chọn select: '" + abbreviate(t.isBlank() ? v : t, 60) + "' (generic iframe)");
            TestActionLog.chon("Select (iframe)", t.isBlank() ? v : t);
            return;
        }
    }

    private String nearestLabelText(WebElement field) {
        try {
            String id = field.getAttribute("id");
            if (id != null && !id.isBlank()) {
                List<WebElement> byFor = driver.findElements(By.cssSelector("label[for='" + id + "']"));
                if (!byFor.isEmpty()) {
                    return safe(byFor.get(0).getText());
                }
            }
            WebElement labeled = field.findElement(By.xpath(
                    "./ancestor::*[.//label][1]//label | ./preceding::label[1]"));
            return safe(labeled.getText());
        } catch (Exception e) {
            return "";
        }
    }

    private String readFieldValue(WebElement field) {
        try {
            String tag = safe(field.getTagName()).toLowerCase(Locale.ROOT);
            if ("textarea".equals(tag) || "input".equals(tag) || "select".equals(tag)) {
                String v = field.getAttribute("value");
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
            String text = field.getText();
            return text == null ? "" : text.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    /** Bấm Tiếp theo bên trong iframe nếu biểu mẫu nhiều bước. */
    private void advanceIframeInternalSteps(String tomTat, String yeuCau, String canCu,
                                            String thoiDiem, String giaTri) {
        for (int step = 0; step < 6; step++) {
            // existsNow thay isElementVisible: eform 1 trang không có nút [Tiếp theo] nội bộ —
            // trường hợp phổ biến — nên đây gần như luôn là "không thấy", mà isElementVisible
            // phải chờ hết PROBE_MS mới trả lời được là không thấy.
            if (!webUI.existsNow(btnTiepTheoIframe) || !webUI.isElementEnabled(btnTiepTheoIframe)) {
                break;
            }
            fillGenericIframeInputs(tomTat, yeuCau, canCu, thoiDiem, giaTri);
            fillIframeCustomDropdowns();
            webUI.clickElement(btnTiepTheoIframe, "Nút [Tiếp theo] trong iframe bước 4", WaitConfig.FIELD);
            webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
        }
    }

    /** Tải file nội dung đơn (PDF / Excel / Word ngẫu nhiên). */
    public void uploadNoiDungFile() {
        if (step4Mode != Step4Mode.UPLOAD) {
            step4Mode = resolveStep4Mode();
            if (step4Mode != Step4Mode.UPLOAD) {
                throw new IllegalStateException("Bước 4 không ở chế độ tải file.");
            }
        }
        webUI.switchToDefaultContent();
        webUI.waitUntilVisible(MARKER_UPLOAD_READY, WaitConfig.FIELD, "Input [Tải nội dung đơn]");
        String filePath = TestFileHelper.pickRandomUploadFile();
        webUI.uploadFile(FILE_INPUT_NOI_DUNG, filePath, "Tải lên [Nội dung đơn]");
        webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
    }

    private void dienFormLegacyOrIframe(String loaiDon,
                                        String thoiDiemPhatSinh,
                                        String giaTriTranhChap,
                                        String tomTatQuaTrinh,
                                        String yeuCauCuThe,
                                        String canCuPhapLy) {
        webUI.setTextForMaskedInput(
                txtThoiDiemPhatSinh(), thoiDiemPhatSinh, "Ô nhập [Thời điểm phát sinh vụ việc]");

        if (DataDictionary.hasGiaTriTranhChap(loaiDon) && webUI.isElementVisible(txtGiaTriTranhChap())) {
            boolean required = DataDictionary.isGiaTriTranhChapRequired(loaiDon);
            if (required) {
                webUI.setTextWithCheck(txtGiaTriTranhChap(), giaTriTranhChap,
                        "Ô nhập [Giá trị tranh chấp (VNĐ)]");
            } else if (giaTriTranhChap != null && !giaTriTranhChap.trim().isEmpty()) {
                webUI.setTextWithCheck(txtGiaTriTranhChap(), giaTriTranhChap,
                        "Ô nhập [Giá trị tranh chấp (VNĐ)]");
            } else {
                System.out.println(" ⏩ Bỏ qua [Giá trị tranh chấp] — không bắt buộc với loại đơn này.");
                TestActionLog.boQua("Giá trị tranh chấp (VNĐ)", "Không bắt buộc với loại đơn này");
            }
        }

        webUI.setTextWithCheck(txtTomTatQuaTrinh(), tomTatQuaTrinh, "Ô nhập [Tóm tắt quá trình sự việc]");
        webUI.setTextWithCheck(txtYeuCauCuThe(), yeuCauCuThe, "Ô nhập [Yêu cầu cụ thể]");
        webUI.setTextWithCheck(txtCanCuPhapLy(), canCuPhapLy, "Ô nhập [Căn cứ pháp lý]");
    }

    /**
     * Bước 4 iframe: commit state rồi bấm [Tiếp theo] wizard đúng một lần (JS click).
     */
    public void clickTiepTheo() {
        if (step4Mode == Step4Mode.IFRAME) {
            webUI.switchToIframe(IFRAME_NOI_DUNG);
            try {
                By iframeNextOnly = By.xpath(
                        "//div[contains(@class,'pf-nav')]//button[contains(., 'Tiếp theo')]"
                                + " | //div[contains(@class,'pf-wrap')]//button[contains(., 'Tiếp theo')]");
                if (webUI.existsNow(iframeNextOnly)) {
                    webUI.clickElementOnceJs(iframeNextOnly, "Nút [Tiếp theo] nội bộ iframe", WaitConfig.FIELD);
                    webUI.sleepMillis(WaitConfig.SETTLE_EFORM_MS);
                    fillGenericIframeInputs("Nội dung kiểm thử tự động.", "Nội dung kiểm thử tự động.",
                            "Nội dung kiểm thử tự động.", "15/01/2024", "1000000");
                    fillIframeCustomDropdowns();
                }
                // syncIframeEformWithHost() tự commit ở cuối (qua nudgeIframeDirtyState) — commit
                // thêm ở đây là lần thứ 3 liên tiếp, mỗi lần đều bắn input/change/blur lên MỌI ô.
                syncIframeEformWithHost();
            } finally {
                webUI.switchToDefaultContent();
            }
            // Một lần duy nhất — tránh native+JS / scroll-chụp ảnh kích hoạt toast x2.
            webUI.clickElementOnceJs(btnTiepTheoWizard, "Nút [Tiếp theo] ở Bước 4", WaitConfig.STEP);
            return;
        }
        webUI.clickElement(btnTiepTheoWizard, "Nút [Tiếp theo] ở Bước 4", WaitConfig.STEP);
    }

    /**
     * Kiểm tra đã điền đủ ô nhập + dropdown trong iframe.
     * @return mô tả chỗ trống; chuỗi rỗng nếu đủ
     */
    public String describeIframeFillGaps() {
        webUI.switchToIframe(IFRAME_NOI_DUNG);
        try {
            StringBuilder gaps = new StringBuilder();
            int empty = 0;
            for (WebElement field : findIframeEditableFields()) {
                try {
                    if (!field.isDisplayed() || !field.isEnabled()) {
                        continue;
                    }
                    if (!readFieldValue(field).isBlank()) {
                        continue;
                    }
                    empty++;
                    String label = nearestLabelText(field);
                    gaps.append("\n  - ô trống: [").append(abbreviate(label, 60)).append("]")
                            .append(" type=").append(safe(field.getAttribute("type")));
                } catch (Exception ignored) {
                }
            }
            int pendingDd = countPendingDropdowns();
            if (pendingDd > 0) {
                gaps.append("\n  - dropdown còn 'Chọn': ").append(pendingDd);
            }
            if (empty == 0 && pendingDd == 0) {
                return "";
            }
            return "Eform chưa điền đủ (" + empty + " ô trống, " + pendingDd + " dropdown):" + gaps;
        } finally {
            webUI.switchToDefaultContent();
        }
    }

    private String scope() {
        if (step4Mode == Step4Mode.IFRAME) {
            return "//div[contains(@class,'pf-wrap')]";
        }
        return "(" + WIZARD_NOIDUNG_SECTION + " | " + NOIDUNG_SECTION + ")";
    }

    private By fieldInScope(String relativeXPath) {
        return By.xpath(scope() + relativeXPath);
    }

    private By txtThoiDiemPhatSinh() {
        return By.xpath(scope()
                + "//label[contains(., 'Thời điểm phát sinh')]/following-sibling::div//input"
                + " | " + scope() + "//label[contains(., 'Thời điểm phát sinh')]/following-sibling::input"
                + " | " + scope() + "//input[@placeholder and (contains(@placeholder, 'Thời điểm')"
                + " or contains(@placeholder, 'dd/mm') or contains(@placeholder, 'DD/MM'))]");
    }

    private By txtGiaTriTranhChap() {
        return fieldInScope("//label[contains(., 'Giá trị tranh chấp')]/following-sibling::input"
                + " | //label[contains(., 'Giá trị tranh chấp')]/parent::div//input");
    }

    private By txtTomTatQuaTrinh() {
        return fieldInScope("//label[contains(., 'Tóm tắt quá trình sự việc')]/following-sibling::textarea"
                + " | //label[contains(., 'Tóm tắt quá trình sự việc')]/parent::div//textarea");
    }

    private By txtYeuCauCuThe() {
        return fieldInScope("//label[contains(., 'Yêu cầu cụ thể')]/following-sibling::textarea"
                + " | //label[contains(., 'Yêu cầu cụ thể')]/parent::div//textarea");
    }

    private By txtCanCuPhapLy() {
        return fieldInScope("//label[contains(., 'Căn cứ pháp lý')]/following-sibling::textarea"
                + " | //label[contains(., 'Căn cứ pháp lý')]/parent::div//textarea");
    }
}

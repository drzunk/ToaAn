package vn.tuphap.automation.data;

import vn.tuphap.automation.pages.BiDonPage;
import vn.tuphap.automation.pages.NguyenDonPage;
import vn.tuphap.automation.ui.LoaiDonLocator;
import vn.tuphap.automation.ui.UiSynonyms;

import vn.tuphap.automation.ui.WaitConfig;
import vn.tuphap.automation.ui.WebUI;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc trực tiếp option/card đang hiển thị trên UI để đồng bộ vùng dữ liệu hợp lệ.
 */
public class UiMasterDataReader {

    private final WebDriver driver;
    private final WebUI webUI;

    public UiMasterDataReader(WebDriver driver) {
        this.driver = driver;
        this.webUI = new WebUI(driver);
    }

    public Map<String, List<String>> scrapeTaoDonStep1() {
        Map<String, List<String>> data = new LinkedHashMap<>();

        List<String> loaiDonCards = scrapeLoaiDonCards();
        data.put("loaiDon", loaiDonCards);

        Map<String, List<String>> loaiViecByDon = new LinkedHashMap<>();
        for (String loaiDon : data.get("loaiDon")) {
            clickLoaiDonCard(loaiDon);
            webUI.sleep(1);
            loaiViecByDon.put(loaiDon, scrapeDropdownOptions(
                    By.xpath("//button[contains(., 'Chọn loại việc cụ thể') or contains(., 'loại việc cụ thể')]"),
                    LOAI_VIEC_OPTIONS
            ));
        }
        for (Map.Entry<String, List<String>> entry : loaiViecByDon.entrySet()) {
            List<String> values = resolveLoaiViecValues(entry.getKey(), entry.getValue());
            loaiViecByDon.put(entry.getKey(), values);
            data.put("loaiViec." + entry.getKey(), values);
        }

        // Tòa án chỉ enable sau khi đã chọn loại đơn + loại việc cụ thể
        List<String> toaAnOptions = ToaAnCatalog.filterForAutomation(
                scrapeToaAnAfterStep1Selection(loaiDonCards, loaiViecByDon));
        data.put("toaAn", toaAnOptions);

        return data;
    }

    private static final By LOAI_VIEC_DROPDOWN = By.xpath("//button[contains(., 'Chọn loại việc cụ thể')]");
    private static final By LOAI_VIEC_OPTIONS = By.xpath(
            "//button[contains(., 'Chọn loại việc cụ thể')]/following-sibling::div//div[@role='option']"
                    + " | //div[@role='listbox']//div[@role='option']");
    private static final By TOA_AN_DROPDOWN = By.xpath("//button[contains(., 'Chọn tòa án nhận đơn')]");
    private static final By TOA_AN_SEARCH = By.xpath(
            "//button[contains(., 'Chọn tòa án nhận đơn')]/following-sibling::div//input[contains(@placeholder, 'Tìm kiếm')]");
    private static final By TOA_AN_OPTIONS = By.xpath(
            "//button[contains(., 'Chọn tòa án nhận đơn')]/following-sibling::div//div[@role='option']");

    /**
     * Thử lần lượt từng loại đơn (kèm loại việc đầu tiên) cho đến khi danh sách thả xuống tòa án có option.
     */
    private List<String> scrapeToaAnAfterStep1Selection(
            List<String> loaiDonCards, Map<String, List<String>> loaiViecByDon) {
        if (loaiDonCards == null || loaiDonCards.isEmpty()) {
            return List.of();
        }

        for (String loaiDon : loaiDonCards) {
            List<String> loaiViecList = loaiViecByDon.get(loaiDon);
            if (loaiViecList == null || loaiViecList.isEmpty()) {
                continue;
            }

            dismissOpenDropdowns();
            clickLoaiDonCard(loaiDon);
            webUI.sleep(1);
            selectLoaiViec(loaiDon, loaiViecList.get(0));
            webUI.sleep(2);

            if (!isToaAnDropdownReady()) {
                System.out.println(" ⏩ Dropdown tòa án chưa sẵn sàng sau [" + loaiDon + " / "
                        + loaiViecList.get(0) + "], thử loại đơn khác...");
                continue;
            }

            List<String> options = scrapeToaAnOptions();
            if (!options.isEmpty()) {
                System.out.println(" ✅ Đã đọc danh sách tòa án sau [" + loaiDon + " / " + loaiViecList.get(0)
                        + "]: " + options);
                return options;
            }
        }
        return List.of();
    }

    private void dismissOpenDropdowns() {
        try {
            driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
            webUI.sleep(1);
        } catch (Exception ignored) {
        }
    }

    private boolean isToaAnDropdownReady() {
        webUI.scrollToElement(TOA_AN_DROPDOWN);
        return webUI.isElementVisible(TOA_AN_DROPDOWN) && webUI.isElementEnabled(TOA_AN_DROPDOWN);
    }

    private void selectLoaiViec(String loaiDon, String loaiViec) {
        if (DataDictionary.isPhaSan(loaiDon)) {
            return;
        }
        webUI.selectDropdownWithCheck(LOAI_VIEC_DROPDOWN, LOAI_VIEC_OPTIONS, loaiViec,
                "Loại việc (chuẩn bị scrape tòa án)");
    }

    private List<String> resolveLoaiViecValues(String loaiDon, List<String> scraped) {
        if (scraped != null && !scraped.isEmpty()) {
            return scraped;
        }
        if (DataDictionary.isPhaSan(loaiDon)) {
            return List.of(DataDictionary.PHA_SAN_LOAI_VIEC_MAC_DINH);
        }
        return List.of();
    }

    private List<String> scrapeToaAnOptions() {
        webUI.scrollToElement(TOA_AN_DROPDOWN);
        if (!isToaAnDropdownReady()) {
            return List.of();
        }

        webUI.clickElement(TOA_AN_DROPDOWN, "Dropdown tòa án (đọc catalog)");
        webUI.sleep(1);

        List<String> merged = new ArrayList<>();
        mergeUnique(merged, scrapeVisibleTexts(TOA_AN_OPTIONS));

        for (String keyword : new String[]{"", "Tòa án", "Tòa", "nhân dân", "Sơn La", "Sơn", "Hà Nội"}) {
            mergeUnique(merged, searchAndScrapeToaAn(keyword));
            if (!merged.isEmpty() && keyword.isEmpty()) {
                break;
            }
        }

        try {
            webUI.clickElement(TOA_AN_DROPDOWN, "Đóng Dropdown tòa án");
        } catch (Exception ignored) {
        }
        return merged;
    }

    private List<String> searchAndScrapeToaAn(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return scrapeVisibleTexts(TOA_AN_OPTIONS);
        }
        try {
            WebElement input = driver.findElement(TOA_AN_SEARCH);
            input.clear();
            input.sendKeys(keyword);
            webUI.sleep(2);
        } catch (Exception ignored) {
        }
        return scrapeVisibleTexts(TOA_AN_OPTIONS);
    }

    private static void mergeUnique(List<String> target, List<String> source) {
        for (String value : source) {
            if (!target.contains(value)) {
                target.add(value);
            }
        }
    }

    public Map<String, List<String>> scrapeNguyenDonStep() {
        Map<String, List<String>> data = new LinkedHashMap<>();

        data.put("loaiChuTheNguyenDon", scrapeVisibleTexts(
                By.xpath("//div[contains(@class, 'cursor-pointer') and (contains(., 'Cá nhân') or contains(., 'Tổ chức') or contains(., 'Doanh nghiệp'))]")
        ));

        clickFirstMatchingCard("Tổ chức");
        webUI.sleep(1);
        data.put("loaiHinhToChuc", scrapeDropdownOptions(
                By.xpath("//label[contains(text(), 'Loại hình tổ chức')]/following-sibling::div//button"),
                By.xpath("//label[contains(text(), 'Loại hình tổ chức')]/following-sibling::div//div[@role='option']")
        ));

        clickFirstMatchingCard("Cá nhân");
        webUI.sleep(1);
        if (webUI.existsNow(By.xpath("//span[contains(text(), 'Tôi có người đại diện pháp lý')]"))) {
            webUI.clickElement(
                    By.xpath("//span[contains(text(), 'Tôi có người đại diện pháp lý')]"),
                    "Checkbox người đại diện"
            );
            webUI.sleep(1);
            data.put("quanHeDaiDien", scrapeDropdownOptions(
                    By.xpath("//label[contains(text(),'Quan hệ')]/following-sibling::div//button"),
                    By.xpath("//label[contains(text(),'Quan hệ')]/following-sibling::div//div[@role='option']")
            ));
        }

        if (webUI.existsNow(NguyenDonPage.BTN_THEM_DONG_NGUYEN_DON)) {
            data.putAll(scrapeDongNguyenDonExpanded());
        }

        return data;
    }

    /**
     * Bấm [Thêm nguyên đơn] / [Thêm người khởi kiện] để đọc label field bên trong form đồng nguyên đơn.
     * Chỉ dùng khi sync catalog — không gọi trong luồng điền tối thiểu.
     */
    public Map<String, List<String>> scrapeDongNguyenDonExpanded() {
        Map<String, List<String>> data = new LinkedHashMap<>();
        if (!webUI.existsNow(NguyenDonPage.BTN_THEM_DONG_NGUYEN_DON)) {
            return data;
        }

        webUI.scrollToElement(NguyenDonPage.BTN_THEM_DONG_NGUYEN_DON);
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        List<String> labelsBefore = scrapeVisibleFormLabels();

        webUI.clickElement(NguyenDonPage.BTN_THEM_DONG_NGUYEN_DON, "Nút [Thêm nguyên đơn] (scrape catalog)");
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);

        List<String> labelsAfter = scrapeVisibleFormLabels();
        List<String> delta = labelDelta(labelsBefore, labelsAfter);
        if (delta.isEmpty()) {
            delta = scrapeLabelsInScope(NguyenDonPage.DONG_NGUYEN_DON_BLOCK);
        }
        if (!delta.isEmpty()) {
            data.put("labels.dongNguyenDon", delta);
            System.out.println(" ✅ Labels form đồng nguyên đơn (sau Thêm): " + delta);
        } else {
            System.out.println(" ⚠ Không đọc được label mới sau [Thêm nguyên đơn] — kiểm tra scope/locator.");
        }

        new NguyenDonPage(driver).dongBoSauScrapeDongNguyenDon();

        List<String> tabs = scrapeVisibleTexts(By.xpath(
                NguyenDonPage.DONG_NGUYEN_DON_BLOCK
                        + "//div[contains(@class, 'cursor-pointer') and (contains(., 'Cá nhân') or contains(., 'Tổ chức'))]"));
        if (!tabs.isEmpty()) {
            data.put("loaiChuTheDongNguyenDon", tabs);
        }

        return data;
    }

    /**
     * Bấm mọi nút Thêm (border-dashed) đang hiện trên form — ghi nhãn field sau khi mở rộng.
     * Trả về map khóa catalog → danh sách label (vd. {@code labels.themBiDon}).
     */
    public Map<String, List<String>> scrapeAllVisibleThemButtons() {
        Map<String, List<String>> data = new LinkedHashMap<>();
        List<WebElement> buttons = driver.findElements(UiSynonyms.anyThemButton());
        for (int i = 0; i < buttons.size(); i++) {
            WebElement btn = buttons.get(i);
            if (!btn.isDisplayed()) {
                continue;
            }
            String text = normalizeButtonText(btn);
            if (text.isEmpty()) {
                continue;
            }
            String catalogKey = catalogKeyForThemButton(text);
            if (catalogKey == null || data.containsKey(catalogKey)) {
                continue;
            }

            String scope = scopeXpathForThemButton(text);
            List<String> labelsBefore = scrapeVisibleFormLabels();
            By btnBy = By.xpath("(//button[(contains(@class,'border-dashed') or .//svg[contains(@class,'lucide-plus')])"
                    + " and starts-with(normalize-space(.), 'Thêm')])[" + (i + 1) + "]");
            try {
                webUI.scrollToElement(btnBy);
                webUI.clickElement(btnBy, "Nút [" + text + "] (scrape catalog)");
                webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
            } catch (RuntimeException e) {
                System.out.println(" ⚠ Không scrape được sau nút [" + text + "]: " + e.getMessage());
                continue;
            }

            List<String> delta = labelDelta(labelsBefore, scrapeVisibleFormLabels());
            if (delta.isEmpty()) {
                delta = scrapeLabelsInScope(scope);
            }
            if (!delta.isEmpty()) {
                data.put(catalogKey, delta);
                System.out.println(" ✅ Labels sau [" + text + "]: " + delta);
            }
        }
        return data;
    }

    /** Phá sản — dropdown Tư cách người nộp đơn (UAT có thể thêm option mới). */
    public List<String> scrapeTuCachNopDonPhaSan() {
        By btn = By.xpath(
                "//label[contains(., 'Tư cách người nộp đơn') or contains(., 'Tư cách')]"
                        + "/following-sibling::div//button"
                        + " | //label[contains(., 'Tư cách người nộp đơn') or contains(., 'Tư cách')]"
                        + "/ancestor::div[contains(@class,'space') or contains(@class,'grid') or contains(@class,'flex')][1]"
                        + "//button[contains(., 'Chọn') or contains(., '—')]");
        return scrapeDropdownOptions(btn, By.xpath("//div[@role='option']"));
    }

    /** In ra label/input đang hiển thị — hỗ trợ phát hiện field mới trên UAT. */
    public List<String> scrapeVisibleFormLabels() {
        return scrapeVisibleTexts(
                By.xpath("//label[normalize-space(.)!=''][not(ancestor::*[contains(@style,'display: none')])]"));
    }

    public Map<String, List<String>> scrapeBiDonStep() {
        Map<String, List<String>> data = new LinkedHashMap<>();

        By chuTheCards = By.xpath(
                BiDonPage.BIDON_SECTION
                        + "//div[contains(@class, 'cursor-pointer') and (contains(., 'Cá nhân') or contains(., 'Tổ chức'))]");
        if (webUI.existsNow(chuTheCards)) {
            data.put("loaiChuTheBiDon", scrapeVisibleTexts(chuTheCards));
            By toChucCard = By.xpath(
                    BiDonPage.BIDON_SECTION
                            + "//div[contains(@class, 'cursor-pointer') and contains(., 'Tổ chức')]");
            if (webUI.existsNow(toChucCard)) {
                webUI.clickElement(toChucCard, "Bị đơn [Tổ chức] (scrape catalog)");
                webUI.sleep(1);
                data.put("loaiHinhToChucBiDon", scrapeDropdownOptions(
                        By.xpath("(//label[contains(., 'Loại hình')]/parent::div//button)[1]"),
                        By.xpath("(//label[contains(., 'Loại hình')]/parent::div)[1]//div[@role='option']")
                ));
            }
        } else if (webUI.existsNow(By.xpath(BiDonPage.BIDON_SECTION + "//label[contains(., 'Tên cơ quan')]"))) {
            data.put("loaiChuTheBiDon", List.of("Cơ quan"));
        }

        if (webUI.existsNow(BiDonPage.BTN_THEM_BI_DON)) {
            data.putAll(scrapeThemBiDonExpanded());
        }

        return data;
    }

    /** Bấm [Thêm bị đơn] (và biến thể) để đọc label slot bị đơn thứ 2. */
    public Map<String, List<String>> scrapeThemBiDonExpanded() {
        Map<String, List<String>> data = new LinkedHashMap<>();
        if (!webUI.existsNow(BiDonPage.BTN_THEM_BI_DON)) {
            return data;
        }

        webUI.scrollToElement(BiDonPage.BTN_THEM_BI_DON);
        List<String> labelsBefore = scrapeVisibleFormLabels();

        webUI.clickElement(BiDonPage.BTN_THEM_BI_DON, "Nút Thêm bị đơn (scrape catalog)");
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);

        List<String> labelsAfter = scrapeVisibleFormLabels();
        List<String> delta = labelDelta(labelsBefore, labelsAfter);
        if (delta.isEmpty()) {
            delta = scrapeLabelsInScope(BiDonPage.BIDON_SECTION);
        }
        if (!delta.isEmpty()) {
            data.put("labels.themBiDon", delta);
            System.out.println(" ✅ Labels form bị đơn (sau Thêm): " + delta);
        }
        return data;
    }

    public Map<String, List<String>> scrapeAll() {
        Map<String, List<String>> all = new LinkedHashMap<>(scrapeTaoDonStep1());
        all.putAll(scrapeNguyenDonStep());
        all.putAll(scrapeBiDonStep());

        all.put("gioiTinh", List.of("Nam", "Nữ", "Khác"));
        all.put("coKhong", List.of("Có", "Không"));
        all.put("noiCapCccd", List.of(
                "Cục Cảnh sát QLHC và TTXH",
                "Cục CS QLHC và TTXH",
                "Công an TP Hà Nội",
                "Công an TP Đà Nẵng"
        ));
        return all;
    }

    private List<String> scrapeDropdownOptions(By dropdown, By optionsLocator) {
        if (!webUI.isElementVisible(dropdown)) {
            return List.of();
        }
        webUI.clickElement(dropdown, "Dropdown để đọc catalog");
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
        return scrapeVisibleTexts(optionsLocator);
    }

    private List<String> scrapeVisibleTexts(By locator) {
        List<String> values = new ArrayList<>();
        for (WebElement element : driver.findElements(locator)) {
            if (!element.isDisplayed()) {
                continue;
            }
            String text = element.getText();
            if (text == null || text.trim().isEmpty()) {
                text = element.getAttribute("textContent");
            }
            if (text != null) {
                text = text.trim().replaceAll("\\s+", " ");
                if (!text.isEmpty() && isProductionOption(text) && !values.contains(text)) {
                    values.add(text);
                }
            }
        }
        return values;
    }

    private List<String> scrapeLoaiDonCards() {
        List<String> cards = new ArrayList<>();
        By cardLocator = By.xpath("//div[contains(@class, 'cursor-pointer')][.//span[normalize-space()!='']]");
        for (WebElement card : driver.findElements(cardLocator)) {
            if (!card.isDisplayed()) {
                continue;
            }
            String text = card.getText();
            if (text == null || text.isBlank()) {
                text = card.getAttribute("textContent");
            }
            String canonical = LoaiDonLocator.canonicalName(text);
            if (canonical != null && !cards.contains(canonical)) {
                cards.add(canonical);
            }
        }
        return cards;
    }

    private void clickLoaiDonCard(String loaiDon) {
        webUI.clickElement(LoaiDonLocator.card(loaiDon), "Thẻ loại đơn [" + loaiDon + "]");
    }

    private List<String> labelDelta(List<String> before, List<String> after) {
        List<String> delta = new ArrayList<>();
        for (String label : after) {
            if (!before.contains(label)) {
                delta.add(label);
            }
        }
        return delta;
    }

    private List<String> scrapeLabelsInScope(String scopeXpath) {
        return scrapeVisibleTexts(By.xpath(
                scopeXpath + "//label[normalize-space(.)!='' and not(ancestor::*[contains(@style,'display: none')])]"));
    }

    private int countLabelsInScope(String scopeXpath) {
        int count = 0;
        for (WebElement el : driver.findElements(By.xpath(scopeXpath + "//label"))) {
            if (el.isDisplayed()) {
                count++;
            }
        }
        return count;
    }

    private void waitForExpandedLabels(String scopeXpath, int labelsBefore) {
        try {
            webUI.waitUntilExists(
                    By.xpath(scopeXpath + "//label[normalize-space(.)!='']"),
                    WaitConfig.STEP,
                    "Label form sau nút Thêm");
        } catch (RuntimeException ignored) {
        }
        if (countLabelsInScope(scopeXpath) <= labelsBefore) {
            webUI.sleepMillis(WaitConfig.SETTLE_MS);
        }
    }

    private static String normalizeButtonText(WebElement btn) {
        String text = btn.getText();
        if (text == null || text.isBlank()) {
            text = btn.getAttribute("textContent");
        }
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private static String catalogKeyForThemButton(String buttonText) {
        String lower = buttonText.toLowerCase();
        if (lower.contains("nguyên đơn") || lower.contains("khởi kiện")) {
            return "labels.dongNguyenDon";
        }
        if (lower.contains("bị đơn") || lower.contains("bị kiện")
                || lower.contains("bị yêu cầu") || lower.contains("được yêu cầu")) {
            return "labels.themBiDon";
        }
        return "labels.them." + buttonText.replaceAll("[^a-zA-Z0-9\\u00C0-\\u1EF9]+", "_");
    }

    private static String scopeXpathForThemButton(String buttonText) {
        String lower = buttonText.toLowerCase();
        if (lower.contains("nguyên đơn") || lower.contains("khởi kiện")) {
            return NguyenDonPage.DONG_NGUYEN_DON_BLOCK;
        }
        return BiDonPage.BIDON_SECTION;
    }

    private void clickFirstMatchingCard(String keyword) {
        webUI.clickElement(
                By.xpath("//div[contains(@class, 'cursor-pointer') and contains(., '" + keyword + "')]"),
                "Thẻ [" + keyword + "]"
        );
    }

    private void clickBiDonCard(int index, String keyword) {
        webUI.clickElement(
                By.xpath("(//div[contains(@class, 'cursor-pointer') and contains(., '" + keyword + "')])[" + index + "]"),
                "Bị đơn [" + keyword + "]"
        );
    }

    private boolean isProductionOption(String text) {
        String lower = text.toLowerCase();
        return !lower.contains("test in don")
                && !lower.contains("fpt test")
                && !lower.contains("tam, xoa")
                && !lower.contains("xoa sau");
    }
}

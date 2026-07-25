package vn.tuphap.automation.data;

import vn.tuphap.automation.ui.LoaiDonLocator;

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
                    By.xpath("//button[contains(., 'Chọn loại việc cụ thể')]"),
                    By.xpath("//button[contains(., 'Chọn loại việc cụ thể')]/following-sibling::div//div[@role='option']")
            ));
        }
        for (Map.Entry<String, List<String>> entry : loaiViecByDon.entrySet()) {
            List<String> values = entry.getValue();
            // Phá sản trên UI không có danh sách thả xuống loại việc — giữ nhãn catalog để automation/cover được.
            if ((values == null || values.isEmpty())
                    && entry.getKey() != null
                    && entry.getKey().toLowerCase().contains("phá sản")) {
                values = List.of(DataDictionary.PHA_SAN_LOAI_VIEC_MAC_DINH);
            }
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
            "//button[contains(., 'Chọn loại việc cụ thể')]/following-sibling::div//div[@role='option']");
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
            selectLoaiViec(loaiViecList.get(0));
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

    private void selectLoaiViec(String loaiViec) {
        webUI.selectDropdownWithCheck(LOAI_VIEC_DROPDOWN, LOAI_VIEC_OPTIONS, loaiViec,
                "Loại việc (chuẩn bị scrape tòa án)");
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

        webUI.clickElement(
                By.xpath("//span[contains(text(), 'Tôi có người đại diện pháp lý')]"),
                "Checkbox người đại diện"
        );
        webUI.sleep(1);
        data.put("quanHeDaiDien", scrapeDropdownOptions(
                By.xpath("//label[contains(text(),'Quan hệ')]/following-sibling::div//button"),
                By.xpath("//label[contains(text(),'Quan hệ')]/following-sibling::div//div[@role='option']")
        ));

        return data;
    }

    public Map<String, List<String>> scrapeBiDonStep() {
        Map<String, List<String>> data = new LinkedHashMap<>();

        data.put("loaiChuTheBiDon", scrapeVisibleTexts(
                By.xpath("(//div[contains(@class, 'cursor-pointer') and (contains(., 'Cá nhân') or contains(., 'Tổ chức'))])[1] | (//div[contains(@class, 'cursor-pointer') and (contains(., 'Cá nhân') or contains(., 'Tổ chức'))])[2]")
        ));

        clickBiDonCard(1, "Tổ chức");
        webUI.sleep(1);
        data.put("loaiHinhToChucBiDon", scrapeDropdownOptions(
                By.xpath("(//label[contains(., 'Loại hình')]/parent::div//button)[1]"),
                By.xpath("(//label[contains(., 'Loại hình')]/parent::div)[1]//div[@role='option']")
        ));

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
        webUI.sleep(1);
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

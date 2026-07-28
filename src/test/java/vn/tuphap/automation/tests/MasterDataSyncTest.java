package vn.tuphap.automation.tests;

import vn.tuphap.automation.ui.WebUI;

import vn.tuphap.automation.core.BaseTest;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.testng.annotations.Test;
import vn.tuphap.automation.flow.TaoDonFlow;
import vn.tuphap.automation.pages.BiDonPage;
import vn.tuphap.automation.pages.DashboardPage;
import vn.tuphap.automation.pages.LoginPage;
import vn.tuphap.automation.pages.NguyenDonPage;
import vn.tuphap.automation.pages.TaoDonPage;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.data.MasterDataCatalog;
import vn.tuphap.automation.data.ToaAnCatalog;
import vn.tuphap.automation.data.UiMasterDataReader;
import vn.tuphap.automation.ui.WaitConfig;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đồng bộ {@code master-data.properties} từ UI thật (dev/UAT).
 * Chạy sau mỗi lần môi trường đổi loại đơn, loại việc, dropdown, label form.
 */
public class MasterDataSyncTest extends BaseTest {

    @Test
    public void syncMasterDataFromUi() throws Exception {
        LoginPage loginPage = new LoginPage(driver);
        loginPage.openPage();
        loginPage.chonDangNhapBangTaiKhoan();
        loginPage.thucHienDangNhap(
                ConfigReader.getValue("username"),
                ConfigReader.getValue("password"),
                ""
        );

        new DashboardPage(driver).clickNopDonMoi();

        UiMasterDataReader reader = new UiMasterDataReader(driver);

        Map<String, List<String>> merged = new LinkedHashMap<>(defaultStaticValues());
        merged.putAll(reader.scrapeTaoDonStep1());

        if (merged.get("loaiDon") == null || merged.get("loaiDon").isEmpty()) {
            throw new IllegalStateException("Không scrape được loại đơn từ UI.");
        }
        if (merged.get("toaAn") == null || merged.get("toaAn").isEmpty()) {
            throw new IllegalStateException(
                    "Không scrape được Tòa án từ UI. Kiểm tra: đã chọn loại đơn + loại việc trước dropdown tòa án; "
                            + "tài khoản có quyền chọn tòa án.");
        }

        // —— Bước 2: Nguyên đơn (Dân sự) — tiếp tục từ form bước 1 vừa scrape ——
        advanceFromScrapedStep1ToNguyenDon(merged);
        merged.putAll(reader.scrapeNguyenDonStep());
        logDiscoveryLabels("Bước 2 [Nguyên đơn — Dân sự]", reader.scrapeVisibleFormLabels());
        if (merged.containsKey("labels.dongNguyenDon")) {
            logDiscoveryLabels("Form đồng nguyên đơn (sau Thêm)", merged.get("labels.dongNguyenDon"));
        }

        // —— Bước 2: Phá sản — Tư cách người nộp đơn ——
        if (merged.get("loaiDon").contains("Phá sản")) {
            navigateToNguyenDonStep(merged, "Phá sản");
            List<String> tuCach = reader.scrapeTuCachNopDonPhaSan();
            if (!tuCach.isEmpty()) {
                merged.put("tuCachNopDonPhaSan", tuCach);
            }
            logDiscoveryLabels("Bước 2 [Nguyên đơn — Phá sản]", reader.scrapeVisibleFormLabels());
        }

        // —— Bước 3: Bị đơn (Dân sự) — bỏ qua nếu UAT chặn chuyển bước ——
        try {
            navigateToNguyenDonStep(merged, pickLoaiDon(merged, "Dân sự"));
            if (advanceToBiDonStep("Dân sự")) {
                merged.putAll(reader.scrapeBiDonStep());
                merged.putAll(reader.scrapeAllVisibleThemButtons());
                logDiscoveryLabels("Bước 3 [Bị đơn — Dân sự]", reader.scrapeVisibleFormLabels());
                if (merged.containsKey("labels.themBiDon")) {
                    logDiscoveryLabels("Form bị đơn (sau Thêm)", merged.get("labels.themBiDon"));
                }
            } else {
                System.out.println(" ⚠ Bỏ qua scrape bước 3 [Dân sự] — không chuyển được sau điền tối thiểu.");
            }
        } catch (Exception e) {
            System.out.println(" ⚠ Bỏ qua scrape bước 3 [Dân sự]: " + e.getMessage());
        }

        // —— Bước 3: Hành chính ——
        if (merged.get("loaiDon").contains("Hành chính")) {
            try {
                navigateToNguyenDonStep(merged, "Hành chính");
                if (advanceToBiDonStep("Hành chính")) {
                    merged.putAll(reader.scrapeBiDonStep());
                    merged.putAll(reader.scrapeAllVisibleThemButtons());
                    logDiscoveryLabels("Bước 3 [Bị kiện — Hành chính]", reader.scrapeVisibleFormLabels());
                    if (merged.containsKey("labels.themBiDon")) {
                        logDiscoveryLabels("Form bị kiện (sau Thêm)", merged.get("labels.themBiDon"));
                    }
                } else {
                    System.out.println(" ⚠ Bỏ qua scrape bước 3 [Hành chính] — không chuyển được sau điền tối thiểu.");
                }
            } catch (Exception e) {
                System.out.println(" ⚠ Bỏ qua scrape bước 3 [Hành chính]: " + e.getMessage());
            }
        }

        MasterDataCatalog.saveToWorkspace(MasterDataCatalog.getWorkspaceCatalogPath(), merged);
        MasterDataCatalog.reload();

        System.out.println("✅ Đã cập nhật master-data.properties từ UI (" + ConfigReader.getValue("baseUrl") + ")");
        merged.forEach((key, values) -> System.out.println(" - " + key + ": " + values));
    }

    private static void logDiscoveryLabels(String step, List<String> labels) {
        System.out.println(" 📋 Labels hiện có — " + step + ":");
        if (labels == null || labels.isEmpty()) {
            System.out.println("    (không đọc được label nào)");
            return;
        }
        labels.stream().limit(40).forEach(l -> System.out.println("    · " + l));
        if (labels.size() > 40) {
            System.out.println("    … và " + (labels.size() - 40) + " label khác");
        }
    }

    private static String pickLoaiDon(Map<String, List<String>> step1, String preferred) {
        List<String> loaiDon = step1.get("loaiDon");
        if (loaiDon.contains(preferred)) {
            return preferred;
        }
        return loaiDon.get(0);
    }

    private void returnToDashboard() {
        try {
            driver.get(ConfigReader.getValue("baseUrl"));
        } catch (Exception ignored) {
        }
        webUI.sleepMillis(WaitConfig.SETTLE_LONG_MS);
        new DashboardPage(driver).waitForDashboard(WaitConfig.DASHBOARD_AFTER_NAV);
    }

    private void advanceFromScrapedStep1ToNguyenDon(Map<String, List<String>> step1) {
        String toaAn = step1.get("toaAn").get(0);
        TaoDonPage taoDonPage = new TaoDonPage(driver);
        taoDonPage.waitForBuoc1Ready();
        taoDonPage.dienToaAnVaTomTat(toaAn, "Đồng bộ catalog tự động");
        taoDonPage.clickTiepTheo();
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        webUI.waitUntilInvisible(TaoDonPage.MARKER_BUOC1, WaitConfig.STEP, "Đã rời bước 1 (sync)");
        webUI.waitUntilVisible(TaoDonFlow.MARKER_NGUYEN_DON, WaitConfig.STEP, "Nguyên đơn (sync)");
    }

    private void navigateToNguyenDonStep(Map<String, List<String>> step1, String loaiDon) {
        List<String> loaiViecList = step1.get("loaiViec." + loaiDon);
        if (loaiViecList == null || loaiViecList.isEmpty()) {
            throw new IllegalStateException("Không có loại việc cho [" + loaiDon + "] sau khi scrape.");
        }
        String loaiViec = loaiViecList.get(0);
        String toaAn = step1.get("toaAn").get(0);

        returnToDashboard();
        new DashboardPage(driver).clickNopDonMoi();
        TaoDonPage taoDonPage = new TaoDonPage(driver);
        taoDonPage.dienFormBuoc1(loaiDon, loaiViec, toaAn, "Đồng bộ catalog tự động");
        taoDonPage.clickTiepTheo();
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        webUI.waitUntilInvisible(TaoDonPage.MARKER_BUOC1, WaitConfig.STEP, "Đã rời bước 1 (sync)");
        webUI.waitUntilVisible(TaoDonFlow.MARKER_NGUYEN_DON, WaitConfig.STEP, "Nguyên đơn (sync)");
    }

    private void fillMinimalNguyenDon(String loaiDon) {
        NguyenDonPage page = new NguyenDonPage(driver);
        page.chonLoaiChuThe("Cá nhân");
        page.dienThongTinCaNhan(
                "Nguyễn Văn Sync",
                "01/01/1990",
                "Nam",
                "079090012345",
                "01/01/2020",
                "Cục Cảnh sát QLHC về TTXH");
        page.dienThongTinLienHe(
                "123 Nguyễn Huệ",
                "Giống thường trú",
                "0912345678",
                "sync.test@gmail.com");
        page.chonNguoiDaiDien("Có", "Nguyễn Văn Đại Diện", "Luật sư");
        page.chonDongNguyenDon("Không");
    }

    private boolean advanceToBiDonStep(String loaiDon) {
        fillMinimalNguyenDon(loaiDon);
        driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        webUI.scrollToElement(By.xpath("//button[contains(., 'Tiếp theo')]"));
        new NguyenDonPage(driver).clickTiepTheo();
        webUI.sleepMillis(WaitConfig.SETTLE_MS);
        if (waitForBiDonStepVisible()) {
            return true;
        }
        webUI.logValidationMessages("Sync — không chuyển bước 3 sau điền tối thiểu");
        return false;
    }

    private boolean waitForBiDonStepVisible() {
        By[] markers = {
                TaoDonFlow.MARKER_BI_DON,
                BiDonPage.BTN_THEM_BI_DON,
                By.xpath("//h2[contains(., 'Bị đơn') or contains(., 'bị kiện')]")
        };
        for (By marker : markers) {
            try {
                webUI.waitUntilVisible(marker, WaitConfig.STEP, "Bước 3 sau điền tối thiểu");
                return true;
            } catch (RuntimeException ignored) {
            }
        }
        System.out.println(" ⚠ Không chuyển sang bước 3 — không thấy marker Bị đơn / Thêm bị đơn.");
        return false;
    }

    private Map<String, List<String>> defaultStaticValues() {
        Map<String, List<String>> defaults = new LinkedHashMap<>();
        defaults.put("loaiChuTheBiDon", List.of("Cá nhân", "Tổ chức"));
        defaults.put("loaiChuTheNguyenDon", List.of("Cá nhân", "Tổ chức / Doanh nghiệp"));
        defaults.put("loaiHinhToChuc", List.of("Công ty TNHH", "Công ty cổ phần", "Doanh nghiệp tư nhân", "Hợp tác xã", "Khác"));
        defaults.put("gioiTinh", List.of("Nam", "Nữ", "Khác"));
        defaults.put("noiCapCccd", List.of("Cục Cảnh sát QLHC về TTXH", "Công an TP Hà Nội"));
        defaults.put("coKhong", List.of("Có", "Không"));
        defaults.put("quanHeDaiDien", List.of("Luật sư", "Người thân", "Khác"));
        defaults.put("toaAn", Arrays.asList(ToaAnCatalog.getAutomationDefaults()));
        return defaults;
    }
}

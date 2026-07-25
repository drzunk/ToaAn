package vn.tuphap.automation.tests;

import vn.tuphap.automation.ui.WebUI;

import vn.tuphap.automation.core.BaseTest;
import org.testng.annotations.Test;
import vn.tuphap.automation.pages.DashboardPage;
import vn.tuphap.automation.pages.LoginPage;
import vn.tuphap.automation.pages.TaoDonPage;
import vn.tuphap.automation.config.ConfigReader;
import vn.tuphap.automation.data.MasterDataCatalog;
import vn.tuphap.automation.data.ToaAnCatalog;
import vn.tuphap.automation.data.UiMasterDataReader;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        // Fix G: Đưa Default vào TRƯỚC, sau đó lấy dữ liệu scrape đè lên để ưu tiên dữ liệu sống
        Map<String, List<String>> merged = new LinkedHashMap<>(defaultStaticValues());
        merged.putAll(reader.scrapeTaoDonStep1());

        if (merged.get("loaiDon") == null || merged.get("loaiDon").isEmpty()) {
            throw new IllegalStateException("Không scrape được loại đơn từ UI.");
        }
        if (merged.get("toaAn") == null || merged.get("toaAn").isEmpty()) {
            throw new IllegalStateException(
                    "Không scrape được Tòa án từ UI. Kiểm tra: đã chọn loại đơn + loại việc trước dropdown tòa án; "
                            + "tài khoản demo có quyền chọn tòa án; chạy Rebuild Project trước khi test.");
        }

        MasterDataCatalog.saveToWorkspace(MasterDataCatalog.getWorkspaceCatalogPath(), merged);
        MasterDataCatalog.reload();

        System.out.println("✅ Đã cập nhật master-data.properties thành công!");
        merged.forEach((key, values) -> System.out.println(" - " + key + ": " + values));
    }

    private void navigateToNguyenDonStep(Map<String, List<String>> step1) {
        String loaiDon = step1.get("loaiDon").get(0);
        List<String> loaiViecList = step1.get("loaiViec." + loaiDon);
        if (loaiViecList == null || loaiViecList.isEmpty()) {
            throw new IllegalStateException("Không có loại việc cho [" + loaiDon + "] sau khi scrape.");
        }
        String loaiViec = loaiViecList.get(0);
        String toaAn = step1.get("toaAn").get(0);

        TaoDonPage taoDonPage = new TaoDonPage(driver);
        taoDonPage.dienFormBuoc1(loaiDon, loaiViec, toaAn, "Đồng bộ catalog tự động");
        taoDonPage.clickTiepTheo();
        webUI.sleep(2);
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
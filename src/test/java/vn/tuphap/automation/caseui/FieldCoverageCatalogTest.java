package vn.tuphap.automation.caseui;

import org.testng.Assert;
import org.testng.annotations.Test;
import vn.tuphap.automation.config.CaseFileSource;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Khóa bản đồ field theo bước/biến thể và metric phủ của generator.
 * <p>
 * Mọi test ở đây nạp catalog tường minh (fallback hoặc fixture CSV trong test resources) chứ không
 * đọc {@code test-output/discovery-sweep/} — máy chưa chạy {@code -Pdiscovery} vẫn cho cùng kết quả.
 */
public class FieldCoverageCatalogTest {

    @Test(groups = "unit", description = "Fallback whitelist được lọc bằng tryFieldOverride")
    public void fallbackCoDuFieldTheoBuoc() {
        FieldCoverageCatalog catalog = FieldCoverageCatalog.fallback();

        Assert.assertEquals(catalog.fieldsForStep(2).size(), 9, "Bước 2 phải có 9 field applicable");
        Assert.assertEquals(catalog.fieldsForStep(3).size(), 7, "Bước 3 phải có 7 field applicable");
        Assert.assertEquals(catalog.fieldsForStep(4).size(), 5, "Bước 4 legacy phải có 5 field");
        Assert.assertTrue(catalog.boQua().isEmpty(), "Không nên bỏ field whitelist: " + catalog.boQua());
    }

    @Test(groups = "unit", description = "Field đặc trưng đi đúng ngữ cảnh CN/TC/Phá sản")
    public void fieldGanDungBienThe() {
        FieldCoverageCatalog catalog = FieldCoverageCatalog.fallback();

        Assert.assertEquals(find(catalog, "Mã số thuế").context().variant(),
                FieldCoverageCatalog.Variant.B2_TO_CHUC);
        Assert.assertEquals(find(catalog, "CCCD").context().variant(),
                FieldCoverageCatalog.Variant.B2_CA_NHAN);
        Assert.assertEquals(find(catalog, "Mã số thuế (Bị đơn)").context().variant(),
                FieldCoverageCatalog.Variant.B3_TO_CHUC_PHA_SAN);
        Assert.assertEquals(find(catalog, "CCCD (Bị đơn)").context().variant(),
                FieldCoverageCatalog.Variant.B3_CA_NHAN);
        Assert.assertTrue(catalog.fieldsForVariant(FieldCoverageCatalog.Variant.B4_EFORM).isEmpty(),
                "Field textarea cố định không được gắn vào eform");
    }

    @Test(groups = "unit", description = "CSV discovery map alias Nguyên đơn và giữ thông báo chặn")
    public void docFixtureDiscovery() {
        FieldCoverageCatalog catalog = FieldCoverageCatalog.fromCsv(fixtureCsv());

        FieldCoverageCatalog.FieldCandidate phone = find(catalog, "Số điện thoại");
        Assert.assertTrue(phone.discoveryDaThay());
        Assert.assertTrue(phone.discoveryDaChan());
        Assert.assertTrue(phone.thongBaoMongDoi().contains("không hợp lệ"));

        FieldCoverageCatalog.FieldCandidate cccdBd = find(catalog, "CCCD (Bị đơn)");
        Assert.assertTrue(cccdBd.discoveryDaThay());
        Assert.assertFalse(cccdBd.discoveryDaChan());
        Assert.assertTrue(cccdBd.thongBaoMongDoi().isBlank());
    }

    @Test(groups = "unit", description = "Generator phủ tối thiểu 90% field applicable")
    public void generatorKhongTutCoverage() {
        TestCaseGenerator.KetQua result =
                TestCaseGenerator.generate(FieldCoverageCatalog.fallback());
        FieldCoverageCatalog.BaoCao coverage = result.fieldCoverage();

        Assert.assertTrue(coverage.phanTramPhu() >= 90.0,
                "Độ phủ field ca âm tụt dưới 90%: " + coverage.fieldDaCoCaAm() + "/"
                        + coverage.tongFieldUngVien() + " — thiếu " + coverage.fieldChuaPhu());
        Assert.assertEquals(coverage.fieldDaCoCaAm(), coverage.tongFieldUngVien());

        TestCaseGenerator.ManHinh step4 = result.screens().stream()
                .filter(s -> "buoc4".equals(s.id())).findFirst().orElseThrow();
        Set<String> negativeStep4Jobs = step4.cases().stream()
                .filter(c -> "am".equals(c.loai()))
                .map(c -> c.caseRow().loaiViec())
                .collect(Collectors.toSet());
        Assert.assertEquals(negativeStep4Jobs, Set.of("Hợp đồng dân sự"),
                "Ca âm textarea không được gắn vào loại việc eform");
    }

    @Test(groups = "unit", description = "Thông báo mong đợi từ CSV chảy vào case sinh ra")
    public void generatorGanThongBaoTuFixture() {
        TestCaseGenerator.KetQua result =
                TestCaseGenerator.generate(FieldCoverageCatalog.fromCsv(fixtureCsv()));

        CaseFileSource.CaseRow phone = caAm(result, "Số điện thoại");
        Assert.assertTrue(phone.thongBaoMongDoi().contains("không hợp lệ"),
                "Ca âm SĐT phải mang thông báo từ CSV: " + phone.thongBaoMongDoi());

        CaseFileSource.CaseRow cccdBd = caAm(result, "CCCD (Bị đơn)");
        Assert.assertTrue(cccdBd.thongBaoMongDoi().isBlank(),
                "Discovery không chặn thì để trống thongBaoMongDoi, đang là: "
                        + cccdBd.thongBaoMongDoi());
        Assert.assertTrue(result.ghiChu().contains("field-discovery-fixture.csv"),
                "Ghi chú phải nêu CSV đang dùng: " + result.ghiChu());
    }

    private static CaseFileSource.CaseRow caAm(TestCaseGenerator.KetQua result, String field) {
        return result.screens().stream()
                .flatMap(screen -> screen.cases().stream())
                .filter(deXuat -> deXuat.caseRow() != null)
                .map(TestCaseGenerator.DeXuat::caseRow)
                .filter(row -> field.equals(row.truongLoi()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không sinh ca âm cho field " + field));
    }

    /** Fixture nạp từ classpath để không phụ thuộc thư mục làm việc lúc chạy Maven/IDE. */
    private static Path fixtureCsv() {
        URL url = FieldCoverageCatalogTest.class.getResource("/caseui/field-discovery-fixture.csv");
        Assert.assertNotNull(url, "Thiếu fixture caseui/field-discovery-fixture.csv trong test resources");
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError("Không đọc được fixture CSV: " + url, e);
        }
    }

    private static FieldCoverageCatalog.FieldCandidate find(FieldCoverageCatalog catalog,
                                                             String field) {
        return catalog.candidates().stream()
                .filter(candidate -> field.equals(candidate.field()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có field " + field));
    }
}

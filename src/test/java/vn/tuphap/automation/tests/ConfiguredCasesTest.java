package vn.tuphap.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import vn.tuphap.automation.config.RunFlowConfig.CaseProfile;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.TaoDonScenario;

import java.util.List;

/**
 * Kiểm tra {@code DataGenerator.generateConfiguredCases} — dựng case từ dòng sheet / run.cases
 * (không mở browser).
 * <p>
 * Trọng tâm: một dòng gõ sai danh mục chỉ được bỏ dòng đó, và cặp scenario↔CaseProfile
 * phải giữ đúng sau khi bỏ (nếu lệch, ca âm sẽ bị gán nhầm sang case khác mà không ai thấy).
 */
public class ConfiguredCasesTest {

    private static CaseProfile hopLe(String loaiDon, String loaiViec) {
        return new CaseProfile(loaiDon, loaiViec, "CN", "", 6, false);
    }

    /** Ca âm: cần {@code truongLoi} khác rỗng — dùng để soi cặp scenario↔profile có lệch không. */
    private static CaseProfile caAm(String loaiDon, String loaiViec, String truongLoi) {
        return new CaseProfile(loaiDon, loaiViec, "CN", "", "", 0,
                null, null, null, null, "", truongLoi, "x", "", 2, false);
    }

    @Test(groups = {"unit"})
    public void dongGoSaiLoaiDon_chiBoDongDo_khongLamHongCaLuot() {
        List<CaseProfile> profiles = List.of(
                hopLe("Dân sự", "Hợp đồng dân sự"),
                hopLe("KHONG-CO-LOAI-DON-NAY", ""),
                hopLe("Lao động", ""));

        Object[][] rows = DataGenerator.generateConfiguredCases(profiles);

        Assert.assertEquals(rows.length, 2, "Phải giữ 2 dòng hợp lệ, chỉ bỏ dòng gõ sai");
        Assert.assertEquals(((TaoDonScenario) rows[0][0]).loaiDon(), "Dân sự");
        Assert.assertEquals(((TaoDonScenario) rows[1][0]).loaiDon(), "Lao động");
    }

    @Test(groups = {"unit"})
    public void boDongLoi_capScenarioVaCaseProfileVanDungNhau() {
        // Dòng lỗi nằm giữa: nếu ghép lại theo chỉ số ở tầng test thì ca âm của "Lao động"
        // sẽ bị gán nhầm sang scenario khác.
        List<CaseProfile> profiles = List.of(
                hopLe("Dân sự", "Hợp đồng dân sự"),
                hopLe("KHONG-CO-LOAI-DON-NAY", ""),
                caAm("Lao động", "", "Email (Nguyên đơn)"));

        Object[][] rows = DataGenerator.generateConfiguredCases(profiles);

        Assert.assertEquals(rows.length, 2);
        for (Object[] row : rows) {
            Assert.assertEquals(row.length, 2, "Mỗi dòng phải gồm [scenario, CaseProfile]");
            TaoDonScenario scenario = (TaoDonScenario) row[0];
            CaseProfile profile = (CaseProfile) row[1];
            Assert.assertNotNull(profile, "CaseProfile phải đi kèm scenario");
            Assert.assertTrue(profile.loaiDon().startsWith(scenario.loaiDon()),
                    "Lệch cặp: scenario=" + scenario.loaiDon() + " nhưng profile=" + profile.loaiDon());
        }

        CaseProfile laoDong = (CaseProfile) rows[1][1];
        Assert.assertTrue(laoDong.hasNegativeExpectation(),
                "Ca âm phải bám đúng case Lao động sau khi dòng lỗi bị bỏ");
        Assert.assertEquals(laoDong.truongLoi(), "Email (Nguyên đơn)");
    }

    @Test(groups = {"unit"})
    public void moiDongDeuSai_baoLoiRoRangThayViTraVeRong() {
        List<CaseProfile> profiles = List.of(
                hopLe("KHONG-CO-1", ""),
                hopLe("KHONG-CO-2", ""));

        Assert.assertThrows(IllegalStateException.class,
                () -> DataGenerator.generateConfiguredCases(profiles));
    }

    /** Phá sản luôn bị ép về loại việc cố định, bất kể cột "Loại việc" ghi gì. */
    @Test(groups = {"unit"})
    public void phaSan_epLoaiViecMacDinh_vaEpMotBiDon() {
        Object[][] rows = DataGenerator.generateConfiguredCases(
                List.of(new CaseProfile("Phá sản", "loai viec nay se bi bo qua", "CN", "", "", 2,
                        null, null, null, null, "", "", "", "", 6, false)));

        TaoDonScenario s = (TaoDonScenario) rows[0][0];
        Assert.assertEquals(s.loaiViec(), "Yêu cầu mở thủ tục phá sản");
        Assert.assertEquals(s.soLuongBiDon(), 1, "Phá sản không cho thêm bị đơn thứ 2");
    }

    /** Nguyên đơn là Tổ chức thì không có người đại diện, dù cột "Đại diện" ghi Có. */
    @Test(groups = {"unit"})
    public void nguyenDonToChuc_luonKhongCoNguoiDaiDien() {
        Object[][] rows = DataGenerator.generateConfiguredCases(
                List.of(new CaseProfile("Dân sự", "Hợp đồng dân sự", "TC", "", "", 1,
                        null, Boolean.TRUE, null, null, "", "", "", "", 6, false)));

        TaoDonScenario s = (TaoDonScenario) rows[0][0];
        Assert.assertEquals(s.coNguoiDaiDien(), "Không");
    }
}

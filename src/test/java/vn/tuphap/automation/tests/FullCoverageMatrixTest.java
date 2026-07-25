package vn.tuphap.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.FullCoverageMatrix;
import vn.tuphap.automation.data.MasterDataCatalog;
import vn.tuphap.automation.data.TaoDonScenario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kiểm tra ma trận Full pairwise mức B (không mở browser).
 */
public class FullCoverageMatrixTest {

    @Test(groups = {"unit"})
    public void maTranPairwiseB_duCapVaNhanh() {
        List<FullCoverageMatrix.BranchSpec> specs = FullCoverageMatrix.build();
        List<String> gaps = FullCoverageMatrix.validateCoverage(specs);
        Assert.assertTrue(gaps.isEmpty(), "Thiếu nhánh: " + gaps);
        Assert.assertTrue(specs.size() >= MasterDataCatalog.getAllLoaiDonViecPairs().size() + 3,
                "Cần ≥ pairs + expand Phá sản; actual=" + specs.size());

        Object[][] data = DataGenerator.generateFullCoverageData();
        Assert.assertEquals(data.length, specs.size());

        Set<String> pairs = new HashSet<>();
        Set<String> tuCach = new HashSet<>();
        boolean hasNdCn = false, hasNdTc = false, hasBdCn = false, hasBdTc = false;
        boolean hasSo2 = false, hasDaiDienCo = false, hasNlqCo = false, hasNlqKhong = false;

        for (Object[] row : data) {
            TaoDonScenario s = (TaoDonScenario) row[0];
            Assert.assertNotNull(s);
            pairs.add(s.loaiDon() + ">" + s.loaiViec());
            if (DataDictionary.isPhaSan(s.loaiDon())) {
                Assert.assertEquals(s.soLuongBiDon(), 1);
                tuCach.add(s.tuCachNopDon());
            }
            if (DataDictionary.isThuanTinhLyHon(s.loaiViec())) {
                Assert.assertEquals(s.soLuongBiDon(), 1);
            }
            if (DataDictionary.isToChuc(s.loaiChuThe())) {
                hasNdTc = true;
            } else {
                hasNdCn = true;
                if ("Có".equals(s.coNguoiDaiDien())) {
                    hasDaiDienCo = true;
                }
            }
            if (!DataDictionary.isPhaSan(s.loaiDon()) && !DataDictionary.isHanhChinh(s.loaiDon())) {
                if (DataDictionary.isToChuc(s.loaiBiDon())) {
                    hasBdTc = true;
                } else {
                    hasBdCn = true;
                }
            }
            if (s.soLuongBiDon() >= 2) {
                hasSo2 = true;
            }
            if ("Có".equals(s.coNguoiLienQuan())) {
                hasNlqCo = true;
            } else {
                hasNlqKhong = true;
            }
        }

        Assert.assertEquals(pairs.size(), MasterDataCatalog.getAllLoaiDonViecPairs().size(),
                "Phải cover mọi cặp loại đơn–việc");
        Assert.assertEquals(tuCach.size(), MasterDataCatalog.getTuCachNopDonPhaSan().length,
                "Phải cover đủ tư cách Phá sản");
        Assert.assertTrue(hasNdCn && hasNdTc, "Thiếu ND CN/TC");
        Assert.assertTrue(hasBdCn && hasBdTc, "Thiếu BD CN/TC");
        Assert.assertTrue(hasSo2, "Thiếu nhánh 2 bị đơn");
        Assert.assertTrue(hasDaiDienCo, "Thiếu đại diện Có");
        Assert.assertTrue(hasNlqCo && hasNlqKhong, "Thiếu NLQ Có/Không");

        System.out.println(" ✅ " + FullCoverageMatrix.summarize(specs));
    }
}

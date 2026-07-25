package vn.tuphap.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;
import vn.tuphap.automation.data.DataDictionary;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.FullCoverageMatrix;
import vn.tuphap.automation.data.MasterDataCatalog;
import vn.tuphap.automation.data.MidCoverageMatrix;
import vn.tuphap.automation.data.TaoDonScenario;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kiểm tra ma trận Mid 35+4 (không mở browser).
 */
public class MidCoverageMatrixTest {

    @Test(groups = {"unit"})
    public void maTranMid_35ThuongVa4TuCachPhaSan() {
        List<FullCoverageMatrix.BranchSpec> specs = MidCoverageMatrix.build();
        List<String> gaps = MidCoverageMatrix.validateCoverage(specs);
        Assert.assertTrue(gaps.isEmpty(), "Thiếu nhánh mid: " + gaps);

        int catalogNonPs = 0;
        for (String[] p : MasterDataCatalog.getAllLoaiDonViecPairs()) {
            if (!DataDictionary.isPhaSan(p[0])) {
                catalogNonPs++;
            }
        }
        Assert.assertEquals(specs.size(), MidCoverageMatrix.TARGET_REGULAR
                + MasterDataCatalog.getTuCachNopDonPhaSan().length);

        Object[][] data = DataGenerator.generateMidCoverageData();
        Assert.assertEquals(data.length, specs.size());

        Set<String> nonPsPairs = new HashSet<>();
        Set<String> tuCach = new HashSet<>();
        int regular = 0;
        int phaSan = 0;

        for (Object[] row : data) {
            TaoDonScenario s = (TaoDonScenario) row[0];
            Assert.assertNotNull(s);
            if (DataDictionary.isPhaSan(s.loaiDon())) {
                phaSan++;
                Assert.assertEquals(s.soLuongBiDon(), 1);
                tuCach.add(s.tuCachNopDon());
            } else {
                regular++;
                nonPsPairs.add(s.loaiDon() + ">" + s.loaiViec());
            }
        }

        Assert.assertEquals(nonPsPairs.size(), catalogNonPs, "Phải cover mọi cặp loại việc thường");
        Assert.assertEquals(regular, MidCoverageMatrix.TARGET_REGULAR);
        Assert.assertEquals(phaSan, MasterDataCatalog.getTuCachNopDonPhaSan().length);
        Assert.assertEquals(tuCach.size(), MasterDataCatalog.getTuCachNopDonPhaSan().length,
                "Phải cover đủ tư cách Phá sản");

        System.out.println(" ✅ " + MidCoverageMatrix.summarize(specs));
    }
}

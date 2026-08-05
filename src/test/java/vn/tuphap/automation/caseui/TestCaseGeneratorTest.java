package vn.tuphap.automation.caseui;

import org.testng.Assert;
import org.testng.annotations.Test;
import vn.tuphap.automation.config.CaseFileSource;
import vn.tuphap.automation.data.DataGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Khóa hợp đồng đề xuất: đủ màn, id không trùng, ca âm nằm trong whitelist, parse CSV discovery.
 */
public class TestCaseGeneratorTest {

    @Test(groups = "unit", description = "Sinh đủ màn Login→Bước 6, không trùng id ghi chú")
    public void generateDuManVaKhongTrungId() {
        TestCaseGenerator.KetQua kq = TestCaseGenerator.generate();
        Assert.assertTrue(kq.tongDeXuat() >= 15, "Quá ít đề xuất: " + kq.tongDeXuat());
        Assert.assertEquals(kq.screens().size(), 8, "Cần 8 nhóm màn (login, dashboard, bước 1–6)");

        Set<String> ids = new HashSet<>();
        Set<String> ghiChu = new HashSet<>();
        for (TestCaseGenerator.ManHinh man : kq.screens()) {
            Assert.assertFalse(man.ten().isBlank(), "Thiếu tên màn");
            Assert.assertFalse(man.cases().isEmpty(), "Màn trống: " + man.id());
            for (TestCaseGenerator.DeXuat dx : man.cases()) {
                Assert.assertTrue(ids.add(dx.id()), "Trùng id đề xuất: " + dx.id());
                CaseFileSource.CaseRow row = dx.caseRow();
                Assert.assertNotNull(row);
                Assert.assertFalse(row.ghiChu().isBlank(), "Thiếu ghiChu: " + dx.id());
                Assert.assertTrue(ghiChu.add(row.ghiChu()), "Trùng ghiChu: " + row.ghiChu());
                if (row.truongLoi() != null && !row.truongLoi().isBlank()) {
                    Assert.assertTrue(DataGenerator.TRUONG_LOI_HOP_LE.contains(row.truongLoi()),
                            "Trường lỗi ngoài whitelist: " + row.truongLoi());
                    Assert.assertEquals(dx.loai(), "am");
                }
            }
        }
        List<String> idsMan = kq.screens().stream().map(TestCaseGenerator.ManHinh::id).toList();
        Assert.assertEquals(idsMan.get(0), "login");
        Assert.assertEquals(idsMan.get(idsMan.size() - 1), "buoc6");
    }

    @Test(groups = "unit", description = "Parser CSV discovery giữ nguyên ô có dấu phẩy trong ngoặc kép")
    public void parseCsvLineGiuNgoacKep() {
        List<String> cols = TestCaseGenerator.parseCsvLine(
                "Số điện thoại,\"abc,xyz\",abc,2,Có,Có,\"SĐT không hợp lệ, vui lòng nhập lại\",");
        Assert.assertEquals(cols.get(0), "Số điện thoại");
        Assert.assertEquals(cols.get(1), "abc,xyz");
        Assert.assertEquals(cols.get(6), "SĐT không hợp lệ, vui lòng nhập lại");
    }
}

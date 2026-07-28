package vn.tuphap.automation.tests;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import vn.tuphap.automation.core.TaoDonBaseTest;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.TaoDonScenario;
import vn.tuphap.automation.flow.TaoDonFlow;
import vn.tuphap.automation.pages.NoiDungDonPage;
import vn.tuphap.automation.pages.TaiLieuPage;
import vn.tuphap.automation.ui.WaitConfig;

/**
 * Probe eform bước 4: điền đủ theo DOM generic → assert đủ dữ liệu.
 * Bấm Tiếp theo một lần; nếu hệ thống/eform báo lỗi → log + chụp ảnh + fail (eform lỗi).
 */
public class Step4EformProbeTest extends TaoDonBaseTest {

    @DataProvider(name = "eformProbe")
    public Object[][] eformProbe() {
        return DataGenerator.generateDanSuBoiThuongEformProbeData();
    }

    @Test(dataProvider = "eformProbe", groups = {"probe"},
            description = "Điền đủ eform iframe bước 4; lỗi sau Tiếp theo = eform lỗi")
    public void probeFillIframeEformBuoc4(TaoDonScenario s) {
        Assert.assertNotNull(s);
        TaoDonFlow flow = new TaoDonFlow(driver, webUI);

        flow.moFormNopDonMoi();
        flow.dienBuoc1(s);
        flow.dienBuoc2(s);
        flow.dienBuoc3(s);

        NoiDungDonPage page = new NoiDungDonPage(driver);
        page.waitStepReady();
        Assert.assertEquals(page.getStep4Mode(), NoiDungDonPage.Step4Mode.IFRAME,
                "Kỳ vọng bước 4 là eform iframe cho loại việc này");

        webUI.switchToIframe(NoiDungDonPage.IFRAME_NOI_DUNG);
        try {
            page.logIframeFieldInventory();
        } finally {
            webUI.switchToDefaultContent();
        }

        page.dienForm(s.loaiDon(), s.thoiDiemPhatSinh(), s.giaTriTranhChap(),
                s.tomTatQuaTrinh(), s.yeuCauCuThe(), s.canCuPhapLy());

        String gaps = page.describeIframeFillGaps();
        Assert.assertTrue(gaps.isBlank(), "Chưa điền đủ eform:\n" + gaps);
        System.out.println("✅ Probe — đã điền đủ thông tin trong eform iframe.");

        page.clickTiepTheo();
        // Lỗi sau submit (kể cả "chưa phản hồi") → fail + log + ảnh = eform lỗi.
        webUI.waitForStepTransition(4, "Điền nội dung đơn",
                TaiLieuPage.MARKER_STEP_READY,
                WaitConfig.STEP, "Đã chuyển sang bước Tài liệu và chứng cứ");
        System.out.println("🎉 Probe eform OK — đã qua bước 4 sang Tài liệu.");
    }
}

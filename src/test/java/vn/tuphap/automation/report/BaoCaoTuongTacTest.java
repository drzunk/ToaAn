package vn.tuphap.automation.report;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Bấm thật vào trang báo cáo bằng trình duyệt.
 * <p>
 * Mọi kiểm chứng khác của báo cáo đều là <b>tĩnh</b>: soi chuỗi HTML, chạy JavaScript trên một DOM
 * giả, chụp ảnh. Không cái nào chứng minh được rằng bấm vào nút thì có chuyện gì xảy ra — mà phần
 * tương tác lại chính là chỗ vừa bị viết lại nhiều nhất: bốn cơ chế lọc gom về một hàm, nút chép
 * nằm trong {@code <summary>}, liên kết sâu tới kịch bản đang bị ẩn.
 * <p>
 * Dự án có sẵn Selenium để kiểm thử ứng dụng — dùng luôn nó để kiểm thử báo cáo của chính mình.
 * Test này <b>không</b> nằm trong bộ unit vì cần trình duyệt; chạy bằng
 * {@code suites/testng-baocao-ui.xml}.
 */
public class BaoCaoTuongTacTest {

    private ChromeDriver driver;
    private JavascriptExecutor js;

    @BeforeClass(alwaysRun = true)
    public void moTrang() {
        Path trang = Paths.get("test-output", "index.html").toAbsolutePath();
        if (!Files.isRegularFile(trang)) {
            throw new SkipException("Chưa có test-output/index.html — chạy một lượt kiểm thử trước.");
        }
        ChromeOptions o = new ChromeOptions();
        o.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1400,1200");
        driver = new ChromeDriver(o);
        js = driver;
        driver.get(trang.toUri().toString());
    }

    @AfterClass(alwaysRun = true)
    public void dong() {
        if (driver != null) {
            driver.quit();
        }
    }

    private void moLuotDauTien() {
        // Trang tu loc ve HOM NAY luc tai — bo loc di de moi test lam viec tren toan bo lich su.
        js.executeScript("document.getElementById('xoaloc').click();");
        // Mở lượt đầu và bỏ mọi bộ lọc, để mỗi test bắt đầu từ cùng một trạng thái.
        js.executeScript(
                "var l=document.querySelector('details.luot');l.open=true;"
                + "l.querySelectorAll('.gomloi-msg').forEach(function(x){x.classList.remove('dangloc');});"
                + "var o=l.querySelector('.tim-case'); if(o) o.value='';"
                + "var c=l.querySelector('.chi-loi'); if(c) c.checked=false;"
                + "l.querySelectorAll('details.case').forEach(function(x){x.hidden=false;x.open=false;});");
    }

    private WebElement luot() {
        return driver.findElement(By.cssSelector("details.luot"));
    }

    private long soCaseHien() {
        return (Long) js.executeScript(
                "return Array.prototype.filter.call("
                + "document.querySelector('details.luot').querySelectorAll('details.case'),"
                + "function(c){return !c.hidden;}).length;");
    }

    /** Nút đổi giao diện phải thật sự đổi, và nhãn phải khớp với thứ đang hiển thị. */
    @Test(groups = {"baocao-ui"})
    public void doiGiaoDienDoiThat() {
        WebElement nut = driver.findElement(By.id("doigiaodien"));
        String truoc = nut.getText();
        nut.click();
        String sau = nut.getText();
        assertFalse(truoc.equals(sau), "Bấm nút mà nhãn không đổi");
        String theme = driver.findElement(By.tagName("html")).getDomAttribute("data-theme");
        assertTrue("dark".equals(theme) || "light".equals(theme), "data-theme chưa được đặt");
        // Nhãn phải mời gọi chế độ NGƯỢC lại với chế độ đang xem.
        assertEquals(sau, "dark".equals(theme) ? "Giao diện sáng" : "Giao diện tối");
        nut.click();
    }

    /**
     * Lỗi từng có: bấm một nhóm lỗi rồi gõ vào ô tìm thì bộ lọc nhóm biến mất nhưng dòng nhóm lỗi
     * <b>vẫn sáng xanh</b> — hai bộ lọc trông như đang bật mà chỉ một cái có tác dụng.
     */
    @Test(groups = {"baocao-ui"})
    public void locTheoNhomLoiVaOTimKhongDaNhau() {
        moLuotDauTien();
        List<WebElement> nhom = luot().findElements(By.cssSelector(".gomloi-msg"));
        if (nhom.isEmpty()) {
            throw new SkipException("Lượt đầu không có nhóm lỗi nào để bấm.");
        }
        nhom.get(0).click();
        long chiNhom = soCaseHien();
        assertTrue(nhom.get(0).getAttribute("class").contains("dangloc"), "Dòng nhóm lỗi chưa sáng");
        // Không giả định danh sách phải NGẮN lại: một lượt có thể hỏng toàn bộ vì cùng một lý do,
        // khi đó nhóm đó chứa mọi kịch bản. Kiểm đúng bản chất — tập đang hiện phải TRÙNG KHỚP
        // tập mã case của nhóm vừa bấm.
        long khopNhom = (Long) js.executeScript(
                "var b=arguments[0], ds=(b.dataset.locMa||'').split('|').filter(Boolean);"
                + "return Array.prototype.filter.call("
                + "  b.closest('details.luot').querySelectorAll('details.case'),"
                + "  function(c){return !c.hidden && ds.indexOf((c.dataset.ma||''))>=0;}).length;",
                nhom.get(0));
        assertEquals(khopNhom, chiNhom,
                "Có kịch bản đang hiện mà không thuộc nhóm lỗi vừa bấm");
        assertTrue(chiNhom > 0, "Bấm nhóm lỗi mà không còn kịch bản nào");

        // Gõ vào ô tìm: nhóm lỗi PHẢI vẫn còn hiệu lực, và hai điều kiện chồng nhau.
        luot().findElement(By.cssSelector(".tim-case")).sendKeys("kich ban");
        assertTrue(nhom.get(0).getAttribute("class").contains("dangloc"),
                "Gõ vào ô tìm làm mất bộ lọc nhóm lỗi nhưng vẫn để dòng đó sáng");
        assertTrue(soCaseHien() <= chiNhom, "Thêm điều kiện tìm mà số case lại tăng");

        // Bấm lại đúng dòng đó phải TẮT lọc nhóm, không phải bật lần nữa.
        nhom.get(0).click();
        assertFalse(nhom.get(0).getAttribute("class").contains("dangloc"), "Bấm lần hai không tắt");
    }

    /** Nút "Bỏ lọc" phải gỡ sạch cả ba điều kiện, không chỉ một. */
    @Test(groups = {"baocao-ui"})
    public void boLocGoSachCaBaDieuKien() {
        moLuotDauTien();
        long tatCa = soCaseHien();
        luot().findElement(By.cssSelector(".tim-case")).sendKeys("khong-ton-tai-xyz");
        luot().findElement(By.cssSelector(".chi-loi")).click();
        List<WebElement> nhom = luot().findElements(By.cssSelector(".gomloi-msg"));
        if (!nhom.isEmpty()) {
            nhom.get(0).click();
        }
        assertTrue(soCaseHien() < tatCa, "Ba bộ lọc mà không lọc được gì");

        WebElement go = luot().findElement(By.cssSelector(".bo-loc-case"));
        assertFalse(go.getAttribute("hidden") != null, "Đang lọc mà nút Bỏ lọc vẫn ẩn");
        go.click();
        assertEquals(soCaseHien(), tatCa, "Bỏ lọc rồi mà danh sách vẫn bị lọc");
        assertEquals(luot().findElement(By.cssSelector(".tim-case")).getAttribute("value"), "");
        assertFalse(luot().findElement(By.cssSelector(".chi-loi")).isSelected());
    }

    /**
     * Nút chép nằm TRONG {@code <summary>} của kịch bản. Bấm nó không được làm kịch bản bung ra —
     * đó là hành vi mặc định của summary và phải bị chặn.
     */
    @Test(groups = {"baocao-ui"})
    public void bamNutChepKhongLamBungKichBan() {
        moLuotDauTien();
        WebElement caseDau = luot().findElement(By.cssSelector("details.case"));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", caseDau);
        boolean truoc = caseDau.getAttribute("open") != null;
        js.executeScript("arguments[0].querySelector('.chep').click();", caseDau);
        boolean sau = caseDau.getAttribute("open") != null;
        assertEquals(sau, truoc, "Bấm nút chép làm kịch bản bung/thu — preventDefault không ăn");
    }

    /**
     * Liên kết sâu tới một kịch bản <b>đang bị bộ lọc ẩn</b> phải gỡ ẩn và bung nó ra.
     * Trước đây chỉ mở {@code <details>} mà không gỡ {@code hidden}, nên phần tử không có hộp bố
     * cục và trang đứng yên — người nhận link tưởng link hỏng.
     */
    @Test(groups = {"baocao-ui"})
    public void lienKetSauMoDuocKichBanDangBiAn() {
        moLuotDauTien();
        WebElement caseCuoi = luot().findElements(By.cssSelector("details.case"))
                .stream().reduce((a, b) -> b).orElseThrow();
        String neo = caseCuoi.getAttribute("id");

        // Ẩn nó đi bằng bộ lọc tìm kiếm rồi mới nhảy tới.
        luot().findElement(By.cssSelector(".tim-case")).sendKeys("khong-ton-tai-xyz");
        assertTrue((Boolean) js.executeScript(
                "return document.getElementById(arguments[0]).hidden;", neo),
                "Kịch bản chưa bị ẩn — chưa dựng được tình huống cần kiểm");

        driver.get(driver.getCurrentUrl().split("#")[0] + "#" + neo);
        assertFalse((Boolean) js.executeScript(
                "return document.getElementById(arguments[0]).hidden;", neo),
                "Liên kết sâu không gỡ được trạng thái ẩn");
        assertTrue((Boolean) js.executeScript(
                "return document.getElementById(arguments[0]).open;", neo),
                "Liên kết sâu không bung kịch bản");
    }

    /** Mở tất cả / Thu gọn phải tác động lên các kịch bản đang hiện. */
    @Test(groups = {"baocao-ui"})
    public void moTatCaVaThuGon() {
        moLuotDauTien();
        luot().findElement(By.cssSelector(".bung[data-bung='1']")).click();
        long dangMo = (Long) js.executeScript(
                "return Array.prototype.filter.call("
                + "document.querySelector('details.luot').querySelectorAll('details.case'),"
                + "function(c){return c.open;}).length;");
        assertEquals(dangMo, soCaseHien(), "Mở tất cả mà còn kịch bản đóng");

        luot().findElement(By.cssSelector(".bung[data-bung='0']")).click();
        long conMo = (Long) js.executeScript(
                "return Array.prototype.filter.call("
                + "document.querySelector('details.luot').querySelectorAll('details.case'),"
                + "function(c){return c.open;}).length;");
        assertEquals(conMo, 0L, "Thu gọn mà còn kịch bản mở");
    }

    /** Phím tắt "/" phải đưa con trỏ vào ô tìm. */
    @Test(groups = {"baocao-ui"})
    public void phimTatGachCheo() {
        driver.findElement(By.tagName("body")).sendKeys("/");
        assertEquals(driver.switchTo().activeElement().getAttribute("id"), "tim",
                "Bấm / không nhảy vào ô tìm");
        driver.switchTo().activeElement().sendKeys(Keys.ESCAPE);
    }

    /** Lớp phóng ảnh phải đóng khi tải trang, mở khi bấm ảnh, và đi được bằng phím mũi tên. */
    @Test(groups = {"baocao-ui"})
    public void xemAnhPhongToVaDiChuyen() {
        moLuotDauTien();
        assertTrue((Boolean) js.executeScript("return document.getElementById('den-anh').hidden;"),
                "Lớp phóng ảnh mở sẵn ngay khi tải trang");

        // Tìm ảnh trên TOÀN TRANG, không chỉ lượt đầu — lượt mới nhất có thể không chụp ảnh nào.
        Boolean coAnh = (Boolean) js.executeScript(
                "var b=document.querySelector('.sk-anh');"
                + "if(!b) return false;"
                + "for(var n=b;n;n=n.parentElement){if(n.tagName==='DETAILS')n.open=true;}"
                + "b.click(); return true;");
        if (!Boolean.TRUE.equals(coAnh)) {
            throw new SkipException("Lượt này không có ảnh chụp nào để mở.");
        }
        assertFalse((Boolean) js.executeScript("return document.getElementById('den-anh').hidden;"),
                "Bấm ảnh mà lớp phóng to không mở");
        String dem = driver.findElement(By.id("anh-dem")).getText();
        assertTrue(dem.matches("\\d+ / \\d+"), "Bộ đếm ảnh sai định dạng: " + dem);

        driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
        assertTrue((Boolean) js.executeScript("return document.getElementById('den-anh').hidden;"),
                "Escape không đóng lớp phóng ảnh");
    }

    /**
     * Mở trang là hai ô ngày đã điền sẵn HÔM NAY.
     * <p>
     * Nếu hôm nay chưa chạy gì, trang không được để trống câm — phải có nút thoát ngay trong dòng
     * thông báo để xem lại toàn bộ lịch sử.
     */
    @Test(groups = {"baocao-ui"})
    public void oNgayTuDienHomNay() {
        driver.navigate().refresh();
        String homNay = java.time.LocalDate.now().toString();
        assertEquals(driver.findElement(By.id("tu")).getAttribute("value"), homNay,
                "Ô 'Từ' không tự điền hôm nay");
        assertEquals(driver.findElement(By.id("den")).getAttribute("value"), homNay,
                "Ô 'Đến' không tự điền hôm nay");

        long hien = (Long) js.executeScript(
                "return Array.prototype.filter.call(document.querySelectorAll('details.luot'),"
                + "function(d){return !d.hidden;}).length;");
        if (hien == 0) {
            WebElement bao = driver.findElement(By.id("khonghop"));
            assertFalse(bao.getAttribute("hidden") != null,
                    "Hôm nay không có lượt nào mà cũng không báo gì");
            driver.findElement(By.id("xemtatca")).click();
            assertTrue((Long) js.executeScript(
                    "return Array.prototype.filter.call(document.querySelectorAll('details.luot'),"
                    + "function(d){return !d.hidden;}).length;") > 0,
                    "Bấm 'Xem tất cả' mà vẫn không hiện lượt nào");
        }
    }

    /** Mỗi màn hình tối đa 10 lượt; nhiều hơn thì sang trang, và lọc lại phải quay về trang 1. */
    @Test(groups = {"baocao-ui"})
    public void phanTrangToiDaMuoiLuot() {
        driver.navigate().refresh();
        driver.findElement(By.id("xoaloc")).click();     // bỏ lọc ngày, xem toàn bộ lịch sử
        long tong = (Long) js.executeScript("return document.querySelectorAll('details.luot').length;");
        long hien = (Long) js.executeScript(
                "return Array.prototype.filter.call(document.querySelectorAll('details.luot'),"
                + "function(d){return !d.hidden;}).length;");
        assertTrue(hien <= 10, "Một màn hình hiện " + hien + " lượt, vượt quá 10");

        WebElement nav = driver.findElement(By.cssSelector(".phantrang"));
        if (tong <= 10) {
            assertTrue(nav.getAttribute("hidden") != null,
                    "Chỉ có " + tong + " lượt mà vẫn hiện thanh phân trang");
            return;
        }
        assertFalse(nav.getAttribute("hidden") != null, "Hơn 10 lượt mà không có phân trang");
        assertTrue(driver.findElement(By.id("tr-nhan")).getText().startsWith("Trang 1/"));
        assertTrue(driver.findElement(By.id("tr-truoc")).getAttribute("disabled") != null,
                "Đang ở trang 1 mà nút Trước vẫn bấm được");

        driver.findElement(By.id("tr-sau")).click();
        assertTrue(driver.findElement(By.id("tr-nhan")).getText().startsWith("Trang 2/"));
        assertTrue((Long) js.executeScript(
                "return Array.prototype.filter.call(document.querySelectorAll('details.luot'),"
                + "function(d){return !d.hidden;}).length;") <= 10);

        // Lọc lại phải kéo về trang 1, nếu không người dùng đứng ở trang 2 của một tập khác.
        driver.findElement(By.id("tim")).sendKeys("0");
        assertTrue(driver.findElement(By.id("tr-nhan")).getText().startsWith("Trang 1/")
                        || driver.findElement(By.cssSelector(".phantrang")).getAttribute("hidden") != null,
                "Lọc lại mà vẫn đứng ở trang cũ");
        driver.findElement(By.id("xoaloc")).click();
    }

    /** Lọc theo ngày ở cấp lượt phải ẩn/hiện đúng và báo khi không còn gì khớp. */
    @Test(groups = {"baocao-ui"})
    public void locCapLuotTheoTuKhoa() {
        WebElement tim = driver.findElement(By.id("tim"));
        tim.clear();
        long tatCa = (Long) js.executeScript(
                "return Array.prototype.filter.call(document.querySelectorAll('details.luot'),"
                + "function(d){return !d.hidden;}).length;");
        tim.sendKeys("khong-ton-tai-xyz");
        assertEquals(js.executeScript(
                "return Array.prototype.filter.call(document.querySelectorAll('details.luot'),"
                + "function(d){return !d.hidden;}).length;"), 0L, "Lọc không ẩn lượt nào");
        assertFalse((Boolean) js.executeScript(
                "return document.getElementById('khonghop').hidden;"),
                "Không còn lượt nào khớp mà không hiện thông báo");

        driver.findElement(By.id("xoaloc")).click();
        assertEquals(js.executeScript(
                "return Array.prototype.filter.call(document.querySelectorAll('details.luot'),"
                + "function(d){return !d.hidden;}).length;"), tatCa, "Xoá lọc không khôi phục");
    }
}

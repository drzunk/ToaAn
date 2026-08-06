package vn.tuphap.automation.caseui;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Khóa hợp đồng dò Maven: chọn bản IDE mới nhất, tôn trọng khai báo tay, và trả rỗng khi không có
 * gì (thay vì trả chuỗi trần "mvn.cmd" — thứ gây "CreateProcess error=2" trên Windows).
 */
public class MavenResolverTest {

    @Test(groups = "unit", description = "Nhiều bản IDE cùng cài thì lấy bản tên lớn nhất")
    public void chonBanIdeMoiNhat() throws IOException {
        Path goc = Files.createTempDirectory("jetbrains-gia");
        taoMvnGia(goc.resolve("IntelliJ IDEA 2026.1.3"));
        taoMvnGia(goc.resolve("IntelliJ IDEA 2026.1.4"));

        Optional<Path> found = MavenResolver.mvnBundledMoiNhat(goc);
        Assert.assertTrue(found.isPresent(), "Không thấy mvn trong " + goc);
        Assert.assertTrue(found.get().toString().contains("2026.1.4"),
                "Phải lấy bản mới nhất, đang lấy: " + found.get());
    }

    @Test(groups = "unit", description = "Thư mục không có Maven thì trả rỗng, không trả chuỗi trần")
    public void khongCoMavenThiTraRong() throws IOException {
        Path goc = Files.createTempDirectory("jetbrains-rong");
        Files.createDirectories(goc.resolve("IntelliJ IDEA 2026.1.4"));

        Assert.assertTrue(MavenResolver.mvnBundledMoiNhat(goc).isEmpty());
        Assert.assertTrue(MavenResolver.mvnTrongThuMuc(goc.resolve("khong-ton-tai")).isEmpty());
        Assert.assertTrue(MavenResolver.mvnTrenPath(null).isEmpty());
        Assert.assertTrue(MavenResolver.mvnTrenPath("").isEmpty());
    }

    @Test(groups = "unit", description = "Khai báo tay run.mavenCmd được ưu tiên trước mọi nguồn khác")
    public void uuTienKhaiBaoTay() throws IOException {
        Path bin = Files.createTempDirectory("mvn-khai-bao-tay").resolve("bin");
        Files.createDirectories(bin);
        Path mvn = Files.createFile(bin.resolve(MavenResolver.tenFileMvn()));

        System.setProperty("run.mavenCmd", mvn.toString());
        try {
            Assert.assertEquals(MavenResolver.timMaven().orElse(null), mvn);
        } finally {
            System.clearProperty("run.mavenCmd");
        }
    }

    @Test(groups = "unit", description = "JAVA_HOME luôn tìm được ít nhất JVM đang chạy test")
    public void luonTimDuocJavaHome() {
        Optional<Path> jdk = MavenResolver.timJavaHome();
        Assert.assertTrue(jdk.isPresent(), "Phải fallback về java.home của JVM hiện tại");
        Assert.assertTrue(MavenResolver.laJavaHome(jdk.get()), "Không phải JDK/JRE: " + jdk.get());
    }

    @Test(groups = "unit", description = "Thông báo lỗi liệt kê được nơi đã tìm")
    public void moTaNoiDaTimCoDuNguon() {
        String mota = MavenResolver.moTaNoiDaTim();
        Assert.assertTrue(mota.contains("run.mavenCmd"), mota);
        Assert.assertTrue(mota.contains("MAVEN_HOME"), mota);
        Assert.assertTrue(mota.contains("PATH"), mota);
    }

    private static void taoMvnGia(Path ide) throws IOException {
        Path bin = ide.resolve("plugins").resolve("maven").resolve("lib").resolve("maven3").resolve("bin");
        Files.createDirectories(bin);
        Files.createFile(bin.resolve(MavenResolver.tenFileMvn()));
    }
}

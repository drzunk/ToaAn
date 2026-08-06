package vn.tuphap.automation.caseui;

import vn.tuphap.automation.config.RunFlowConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Dò {@code mvn} và JDK để dashboard chạy được test trên máy chưa cài Maven/JDK "chuẩn".
 * <p>
 * Trả về đường dẫn đầy đủ tới {@code mvn.cmd}, không bao giờ trả chuỗi trần {@code "mvn"}:
 * {@code ProcessBuilder} trên Windows gọi thẳng {@code CreateProcess}, không tra PATHEXT như
 * {@code cmd.exe}/PowerShell — thư mục bin Maven có cả file {@code mvn} (script Unix, không đuôi)
 * lẫn {@code mvn.cmd}; trả {@code "mvn"} thì Windows báo "CreateProcess error=2".
 * <p>
 * Không hardcode phiên bản IntelliJ: quét {@code C:\Program Files\JetBrains\*} và Toolbox rồi lấy
 * bản tên lớn nhất, vì bản cũ bị gỡ sau mỗi lần IDE tự cập nhật.
 */
public final class MavenResolver {

    private static final boolean WINDOWS =
            System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    /** Đặt trong {@code run-flow.properties}, hoặc {@code -Drun.mavenCmd=...}, hoặc env {@code TOAAN_RUN_MAVENCMD}. */
    private static final String KHOA_OVERRIDE = "run.mavenCmd";

    private MavenResolver() {
    }

    /**
     * Đường dẫn {@code mvn} dùng được, theo thứ tự ưu tiên: khai báo tay → {@code MAVEN_HOME}/
     * {@code M2_HOME} → PATH → Maven đi kèm IntelliJ (cài đặt thường + Toolbox).
     */
    public static Optional<Path> timMaven() {
        String khaiBaoTay = RunFlowConfig.text(KHOA_OVERRIDE, "");
        if (!khaiBaoTay.isBlank()) {
            Path p = Paths.get(khaiBaoTay);
            if (Files.isRegularFile(p)) {
                return Optional.of(p);
            }
            Optional<Path> trongBin = mvnTrongThuMuc(p);
            if (trongBin.isPresent()) {
                return trongBin;
            }
        }
        for (String bien : new String[]{"MAVEN_HOME", "M2_HOME"}) {
            String home = System.getenv(bien);
            if (home != null && !home.isBlank()) {
                Optional<Path> found = mvnTrongThuMuc(Paths.get(home, "bin"));
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        Optional<Path> tuPath = mvnTrenPath(System.getenv("PATH"));
        if (tuPath.isPresent()) {
            return tuPath;
        }
        for (Path goc : thuMucJetBrains()) {
            Optional<Path> found = mvnBundledMoiNhat(goc);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /**
     * JDK để truyền {@code JAVA_HOME} cho tiến trình Maven con. Ưu tiên {@code JAVA_HOME} sẵn có nếu
     * thật sự trỏ vào một JDK; nếu không thì dùng chính JVM đang chạy (luôn hợp lệ vì server khởi
     * động được), rồi tới {@code ~/.jdks} và JBR của IntelliJ.
     */
    public static Optional<Path> timJavaHome() {
        String env = System.getenv("JAVA_HOME");
        if (env != null && !env.isBlank() && laJavaHome(Paths.get(env))) {
            return Optional.of(Paths.get(env));
        }
        String jvmHienTai = System.getProperty("java.home");
        if (jvmHienTai != null && laJavaHome(Paths.get(jvmHienTai))) {
            return Optional.of(Paths.get(jvmHienTai));
        }
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Optional<Path> tuJdks = thuMucConMoiNhat(Paths.get(userHome, ".jdks"), MavenResolver::laJavaHome);
            if (tuJdks.isPresent()) {
                return tuJdks;
            }
        }
        for (Path goc : thuMucJetBrains()) {
            Optional<Path> jbr = thuMucConMoiNhat(goc, ide -> laJavaHome(ide.resolve("jbr")));
            if (jbr.isPresent()) {
                return Optional.of(jbr.get().resolve("jbr"));
            }
        }
        return Optional.empty();
    }

    /** Danh sách nơi đã dò, để thông báo lỗi nói được "đã tìm ở đâu" thay vì chỉ "không thấy". */
    public static String moTaNoiDaTim() {
        List<String> noi = new ArrayList<>();
        noi.add("khóa " + KHOA_OVERRIDE + " trong src/test/resources/run-flow.properties");
        noi.add("biến môi trường MAVEN_HOME / M2_HOME");
        noi.add("PATH");
        for (Path goc : thuMucJetBrains()) {
            noi.add(goc + File.separator + "*" + File.separator + "plugins"
                    + File.separator + "maven" + File.separator + "lib" + File.separator + "maven3");
        }
        return String.join("; ", noi);
    }

    // ── Các hàm quét nhận thư mục gốc làm tham số để unit test được ───────────

    /** Tên file mvn của hệ điều hành hiện tại — test dựng file giả theo tên này. */
    static String tenFileMvn() {
        return WINDOWS ? "mvn.cmd" : "mvn";
    }

    static String tenFileJava() {
        return WINDOWS ? "java.exe" : "java";
    }

    /** {@code mvn.cmd}/{@code mvn.bat} (Windows) hoặc {@code mvn} trong đúng một thư mục bin. */
    static Optional<Path> mvnTrongThuMuc(Path bin) {
        if (bin == null) {
            return Optional.empty();
        }
        for (String ten : WINDOWS ? new String[]{"mvn.cmd", "mvn.bat"} : new String[]{"mvn"}) {
            Path ungVien = bin.resolve(ten);
            if (Files.isRegularFile(ungVien)) {
                return Optional.of(ungVien);
            }
        }
        return Optional.empty();
    }

    static Optional<Path> mvnTrenPath(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        for (String dir : path.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (dir.isBlank()) {
                continue;
            }
            Optional<Path> found = mvnTrongThuMuc(Paths.get(dir));
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /**
     * Maven đi kèm IDE: {@code <goc>/<ten IDE>/plugins/maven/lib/maven3/bin/mvn.cmd}. Nhiều bản IDE
     * cùng tồn tại thì lấy bản có tên lớn nhất (2026.1.3 &lt; 2026.1.4).
     */
    static Optional<Path> mvnBundledMoiNhat(Path goc) {
        return thuMucConMoiNhat(goc, ide -> mvnTrongThuMuc(binMaven(ide)).isPresent())
                .flatMap(ide -> mvnTrongThuMuc(binMaven(ide)));
    }

    private static Path binMaven(Path ide) {
        return ide.resolve("plugins").resolve("maven").resolve("lib").resolve("maven3").resolve("bin");
    }

    /** Thư mục con hợp lệ có tên lớn nhất — dùng cho cả IDE lẫn {@code ~/.jdks}. */
    static Optional<Path> thuMucConMoiNhat(Path goc, java.util.function.Predicate<Path> hopLe) {
        if (goc == null || !Files.isDirectory(goc)) {
            return Optional.empty();
        }
        try (Stream<Path> con = Files.list(goc)) {
            return con.filter(Files::isDirectory)
                    .filter(hopLe)
                    .max(Comparator.comparing(p -> p.getFileName().toString()));
        } catch (java.io.IOException e) {
            return Optional.empty();
        }
    }

    static boolean laJavaHome(Path home) {
        if (home == null) {
            return false;
        }
        return Files.isRegularFile(home.resolve("bin").resolve(tenFileJava()));
    }

    private static List<Path> thuMucJetBrains() {
        List<Path> goc = new ArrayList<>();
        String programFiles = System.getenv("ProgramFiles");
        goc.add(Paths.get(programFiles == null || programFiles.isBlank()
                ? "C:\\Program Files" : programFiles, "JetBrains"));
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            goc.add(Paths.get(localAppData, "JetBrains", "Toolbox", "apps"));
            goc.add(Paths.get(localAppData, "Programs"));
        }
        return goc;
    }
}

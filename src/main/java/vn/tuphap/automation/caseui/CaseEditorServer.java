package vn.tuphap.automation.caseui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import vn.tuphap.automation.config.CaseFileSource;
import vn.tuphap.automation.config.CaseSheetSource;
import vn.tuphap.automation.config.RunFlowConfig;
import vn.tuphap.automation.config.RunFlowConfig.CaseProfile;
import vn.tuphap.automation.data.MasterDataCatalog;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dashboard cục bộ — 1 trang, 4 tab: <b>Dashboard</b> (nhúng {@code test-output/index.html}),
 * <b>Test case</b> (form web sửa {@code local-cases.json} — thay Google Sheet, validate nghiêm qua
 * {@link CaseFileSource} — và nút "Chạy" gọi thẳng {@code mvn test -Pmaster}), <b>Sinh test case</b>
 * (đề xuất theo từng màn từ {@link TestCaseGenerator} + CSV discovery nếu có), <b>Config locator</b>
 * (xem — không sửa — mọi {@code By.*} quét từ Page Object + catalog UI).
 * <p>
 * Không stream log Maven lên trang (quyết định có chủ đích — bản đầu giữ đơn giản): nút Chạy kích
 * hoạt tiến trình rồi trả lời ngay; xem tiến độ ở {@code test-output/last-run.log}, xem kết quả ở
 * lại tab Dashboard khi xong.
 * <p>
 * Khởi động: {@code mvn exec:java -Dexec.mainClass=vn.tuphap.automation.caseui.CaseEditorServer}
 * (hoặc mục menu "Mở trình khai báo case (web)" trong {@code scripts/chay.cmd}).
 */
public final class CaseEditorServer {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Path RUN_FLOW_PROPERTIES = Paths.get("src", "test", "resources", "run-flow.properties");
    private static final Path DEFAULT_CASES_FILE = Paths.get("src", "test", "resources", "local-cases.json");
    private static final Path LOG_FILE = Paths.get("test-output", "last-run.log");
    private static final Path REPORT_FILE = Paths.get("test-output", "index.html");
    private static final Path TEST_OUTPUT_DIR = Paths.get("test-output");

    private CaseEditorServer() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.getInteger("caseui.port", 8787);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", CaseEditorServer::handleIndex);
        server.createContext("/api/catalog", CaseEditorServer::handleCatalog);
        server.createContext("/api/locators", CaseEditorServer::handleLocators);
        server.createContext("/api/cases", CaseEditorServer::handleCases);
        server.createContext("/api/generate-cases", CaseEditorServer::handleGenerateCases);
        server.createContext("/api/import-sheet", CaseEditorServer::handleImportSheet);
        server.createContext("/api/run", CaseEditorServer::handleRun);
        server.createContext("/api/run-login", CaseEditorServer::handleRunLogin);
        server.createContext("/report/", CaseEditorServer::handleReport);
        server.start();
        String url = "http://localhost:" + port + "/";
        System.out.println("Trình khai báo case đang chạy ở " + url);
        System.out.println("Nhấn Ctrl+C để dừng.");
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            System.out.println("(Không tự mở được trình duyệt — tự mở " + url + ")");
        }
    }

    // ── /  ───────────────────────────────────────────────────────────────

    private static void handleIndex(HttpExchange ex) throws IOException {
        try (InputStream in = CaseEditorServer.class.getClassLoader()
                .getResourceAsStream("case-editor/index.html")) {
            if (in == null) {
                sendText(ex, 500, "Không tìm thấy case-editor/index.html trong classpath.");
                return;
            }
            byte[] bytes = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ── /report/* — phục vụ tĩnh test-output/ để tab Dashboard nhúng iframe được ─

    private static void handleReport(HttpExchange ex) throws IOException {
        String rel = ex.getRequestURI().getPath().substring("/report".length());
        if (rel.isEmpty() || "/".equals(rel)) {
            rel = "/index.html";
        }
        Path file = TEST_OUTPUT_DIR.resolve(rel.substring(1)).normalize();
        if (!file.startsWith(TEST_OUTPUT_DIR) || !Files.isRegularFile(file)) {
            sendText(ex, 404, "Không thấy " + rel + " — chạy 1 lượt test trước để sinh báo cáo.");
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType(file));
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    // ── /api/catalog ─────────────────────────────────────────────────────

    private static void handleCatalog(HttpExchange ex) throws IOException {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("loaiDon", MasterDataCatalog.getLoaiDon());
        Map<String, String[]> loaiViecByLoaiDon = new LinkedHashMap<>();
        for (String loaiDon : MasterDataCatalog.getLoaiDon()) {
            try {
                loaiViecByLoaiDon.put(loaiDon, MasterDataCatalog.getLoaiViecByLoaiDon(loaiDon));
            } catch (IllegalStateException ignored) {
                // Danh mục thiếu loại việc cho loại đơn này — bỏ qua, form để trống dropdown đó.
            }
        }
        catalog.put("loaiViecByLoaiDon", loaiViecByLoaiDon);
        catalog.put("toaAn", MasterDataCatalog.getToaAn());
        catalog.put("chuThe", MasterDataCatalog.getLoaiChuTheNguyenDon());
        catalog.put("tuCachNopDonPhaSan", MasterDataCatalog.getTuCachNopDonPhaSan());
        catalog.put("truongLoiHopLe", vn.tuphap.automation.data.DataGenerator.TRUONG_LOI_HOP_LE);
        sendJson(ex, 200, catalog);
    }

    // ── /api/locators (chỉ đọc — quét toàn bộ By.* trong Page Object thật) ─

    private static final Path PAGES_DIR = Paths.get("src", "main", "java", "vn", "tuphap", "automation", "pages");
    private static final Path UI_DIR = Paths.get("src", "main", "java", "vn", "tuphap", "automation", "ui");
    private static final Pattern BY_CALL = Pattern.compile(
            "By\\.(id|xpath|cssSelector|className|name|tagName|linkText|partialLinkText)\\s*\\(");
    private static final Pattern FIELD_DECL = Pattern.compile("\\bBy\\s+(\\w+)\\s*=");
    private static final Pattern METHOD_SIG = Pattern.compile(
            "^(?:[\\w<>\\[\\],]+\\s+)+(\\w+)\\s*\\([^()]*\\)\\s*\\{?\\s*$");

    /**
     * Quét TOÀN BỘ locator (mọi {@code By.xpath/id/cssSelector/...}) trực tiếp từ source thật của
     * 9 Page Object + 2 lớp catalog dùng chung ({@code ui/UiSynonyms.java}, {@code ui/LoaiDonLocator.java}).
     * <p>
     * Chỉ đọc file — không nạp class, không dùng reflection: nhiều locator được dựng động theo
     * tham số (index bị đơn, loại đơn...) lúc chạy nên không có giá trị "sống" cố định để phản ánh;
     * hiển thị đúng dòng mã nguồn là cách trung thực nhất, và luôn khớp 100% với code đang chạy.
     * Chỉ để XEM (tab "Config locator" không sửa được) — quyết định có chủ đích cho bản đầu.
     */
    private static void handleLocators(HttpExchange ex) throws IOException {
        List<Path> targets = new ArrayList<>();
        if (Files.isDirectory(PAGES_DIR)) {
            try (var s = Files.list(PAGES_DIR)) {
                s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(targets::add);
            }
        }
        for (String f : List.of("UiSynonyms.java", "LoaiDonLocator.java")) {
            Path p = UI_DIR.resolve(f);
            if (Files.exists(p)) {
                targets.add(p);
            }
        }

        List<Map<String, Object>> files = new ArrayList<>();
        int tongSo = 0;
        for (Path p : targets) {
            List<Map<String, Object>> locs = scanLocators(p);
            if (locs.isEmpty()) {
                continue;
            }
            Map<String, Object> fileEntry = new LinkedHashMap<>();
            fileEntry.put("ten", p.getFileName().toString());
            fileEntry.put("locators", locs);
            files.add(fileEntry);
            tongSo += locs.size();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("files", files);
        body.put("tongSo", tongSo);
        sendJson(ex, 200, body);
    }

    private static List<Map<String, Object>> scanLocators(Path javaFile) throws IOException {
        List<String> lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
        List<Map<String, Object>> out = new ArrayList<>();
        String hamHienTai = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            Matcher msig = METHOD_SIG.matcher(trimmed);
            if (trimmed.endsWith("{") && msig.find()) {
                hamHienTai = msig.group(1);
            }
            Matcher m = BY_CALL.matcher(line);
            if (!m.find()) {
                continue;
            }
            String nhan;
            Matcher f = FIELD_DECL.matcher(line);
            if (f.find()) {
                nhan = f.group(1);
            } else if (hamHienTai != null) {
                nhan = "trong " + hamHienTai + "()";
            } else {
                nhan = "(dòng " + (i + 1) + ")";
            }
            int dongBatDau = i + 1;
            // Locator dài thường xuống dòng (XPath nhiều nhánh) — gộp tới khi ngoặc cân bằng VÀ câu
            // lệnh kết thúc bằng ';', không thì "Mã kỹ thuật" hiện cụt lủng "By.xpath(" trơ trọi.
            StringBuilder full = new StringBuilder(trimmed);
            int depth = demKyTu(trimmed, '(') - demKyTu(trimmed, ')');
            String dongCuoi = trimmed;
            int gioiHan = i + 20;
            while (depth > 0 && !dongCuoi.endsWith(";") && i + 1 < lines.size() && i < gioiHan) {
                i++;
                dongCuoi = lines.get(i).trim();
                full.append(' ').append(dongCuoi);
                depth += demKyTu(dongCuoi, '(') - demKyTu(dongCuoi, ')');
            }
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("dong", dongBatDau);
            e.put("nhan", nhan);
            e.put("loai", m.group(1));
            e.put("ma", full.toString());
            out.add(e);
        }
        return out;
    }

    private static int demKyTu(String s, char c) {
        int n = 0;
        for (int k = 0; k < s.length(); k++) {
            if (s.charAt(k) == c) {
                n++;
            }
        }
        return n;
    }

    // ── /api/cases (GET đọc, POST ghi đè) ────────────────────────────────

    private static void handleCases(HttpExchange ex) throws IOException {
        Path casesFile = casesFileInUse();
        if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 200, CaseFileSource.readAll(casesFile));
            return;
        }
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "Chỉ hỗ trợ GET/POST.");
            return;
        }
        String body = readBody(ex);
        CaseFileSource.CaseRow[] rows;
        try {
            rows = GSON.fromJson(body, CaseFileSource.CaseRow[].class);
        } catch (Exception e) {
            sendJson(ex, 400, Map.of("error", "JSON gửi lên không hợp lệ: " + e.getMessage()));
            return;
        }
        List<CaseFileSource.CaseRow> list = rows == null ? List.of() : List.of(rows);
        try {
            CaseFileSource.save(casesFile, list);
        } catch (CaseFileSource.ValidationException e) {
            sendJson(ex, 400, Map.of("error", e.getMessage()));
            return;
        }
        sendJson(ex, 200, Map.of("message", "Đã lưu " + list.size() + " dòng.", "savedCount", list.size()));
    }

    // ── /api/generate-cases — đề xuất theo từng màn (không ghi file) ──────

    private static void handleGenerateCases(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "Chỉ hỗ trợ GET.");
            return;
        }
        TestCaseGenerator.KetQua ketQua = TestCaseGenerator.generate();
        sendJson(ex, 200, ketQua);
    }

    // ── /api/import-sheet ────────────────────────────────────────────────

    private static void handleImportSheet(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "Chỉ hỗ trợ POST.");
            return;
        }
        String sheetUrl = RunFlowConfig.casesSheetUrl();
        if (sheetUrl.isBlank()) {
            sendJson(ex, 400, Map.of("error", "Chưa cấu hình run.casesSheet trong run-flow.properties."));
            return;
        }
        CaseSheetSource.Result r = CaseSheetSource.load(sheetUrl, RunFlowConfig.text("run.casesSheetGid", ""));
        List<CaseProfile> fromSheet = r.cases();
        if (fromSheet.isEmpty()) {
            sendJson(ex, 400, Map.of("error", "Không lấy được case nào từ Sheet (" + r.sourceLabel() + ")."));
            return;
        }
        List<CaseFileSource.CaseRow> rows = CaseFileSource.fromSheetCases(fromSheet);
        Path casesFile = casesFileInUse();
        CaseFileSource.save(casesFile, rows);
        sendJson(ex, 200, Map.of("message", "Đã nhập " + rows.size() + " case từ " + r.sourceLabel()
                + " vào " + casesFile + ".", "importedCount", rows.size()));
    }

    // ── /api/run ─────────────────────────────────────────────────────────

    private static void handleRun(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "Chỉ hỗ trợ POST.");
            return;
        }
        Path casesFile = casesFileInUse();
        // Kiểm tra TRƯỚC khi đụng vào run-flow.properties / khởi động Maven: không có case nào bật
        // (file rỗng, mọi dòng chay=false, hoặc dòng sai validate) thì trước đây rơi thẳng xuống bộ
        // smoke mặc định của MasterExecutionTest — tester tưởng "không chạy gì" nhưng Chrome vẫn mở
        // và tạo đơn thật. Giờ chặn hẳn, báo rõ lý do.
        List<CaseProfile> caseDangBat;
        try {
            caseDangBat = CaseFileSource.load(casesFile);
        } catch (CaseFileSource.ValidationException e) {
            sendJson(ex, 400, Map.of("error", "Có case sai chưa sửa được — sửa xong mới Chạy: "
                    + e.getMessage()));
            return;
        }
        if (caseDangBat.isEmpty()) {
            sendJson(ex, 400, Map.of("error", "Không có case nào đang bật (\"Chạy\" = tick) trong "
                    + casesFile + " — không chạy gì cả. Thêm case hoặc tick \"Chạy\" rồi Lưu tất cả trước."));
            return;
        }
        Path mvn = MavenResolver.timMaven().orElse(null);
        if (mvn == null) {
            sendJson(ex, 500, Map.of("error", "Không tìm thấy Maven. Đã tìm ở: "
                    + MavenResolver.moTaNoiDaTim()
                    + ". Cài Maven hoặc khai báo đường dẫn đầy đủ tới mvn.cmd bằng khóa"
                    + " run.mavenCmd trong " + RUN_FLOW_PROPERTIES + "."));
            return;
        }
        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("run.suite", "master");
        updates.put("run.caseSource", "file");
        updates.put("run.casesFile", casesFile.toString().replace('\\', '/'));
        setProperties(RUN_FLOW_PROPERTIES, updates);

        Process p;
        try {
            Files.createDirectories(LOG_FILE.getParent());
            String casesFileArg = casesFile.toString().replace('\\', '/');
            // Profile Maven "master" (pom.xml) có sẵn <taodon.suite>smoke</taodon.suite> làm mặc
            // định — không truyền -D đè lại thì báo cáo gắn nhầm tag SMOKE dù case đến từ file JSON.
            // -Drun.openReport=false CHỈ cho lượt này (không ghi xuống file): dashboard đã có tab
            // Dashboard nhúng sẵn báo cáo, khỏi cần bật thêm 1 tab trình duyệt riêng khi xong.
            ProcessBuilder pb = new ProcessBuilder(
                    mvn.toString(), "-Pmaster", "test",
                    "-Dtaodon.suite=master", "-Drun.suite=master",
                    "-Drun.caseSource=file", "-Drun.casesFile=" + casesFileArg,
                    "-Drun.openReport=false");
            // mvn.cmd chết ngay nếu JAVA_HOME trỏ sai/không có. Máy chỉ cài JDK qua IDE thì biến này
            // thường trống, và lỗi chỉ nằm trong log — trang web vẫn báo "đang chạy".
            MavenResolver.timJavaHome()
                    .ifPresent(jdk -> pb.environment().put("JAVA_HOME", jdk.toString()));
            pb.redirectOutput(LOG_FILE.toFile());
            pb.redirectErrorStream(true);
            p = pb.start();
        } catch (IOException e) {
            sendJson(ex, 500, Map.of("error", "Không khởi động được Maven (" + mvn + "): "
                    + e.getMessage()));
            return;
        }

        String chetSom = loiKhiMavenChetSom(p);
        if (chetSom != null) {
            sendJson(ex, 500, Map.of("error", chetSom));
            return;
        }

        sendJson(ex, 200, Map.of(
                "message", "Đang chạy — theo dõi " + LOG_FILE + ". Xong thì mở lại tab Dashboard để"
                        + " xem báo cáo cập nhật.",
                "log", LOG_FILE.toString(),
                "report", REPORT_FILE.toString()));
    }

    // ── /api/run-login — suite LoginTest (1 dương + 3 âm) ─────────────────

    private static void handleRunLogin(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "Chỉ hỗ trợ POST.");
            return;
        }
        Path mvn = MavenResolver.timMaven().orElse(null);
        if (mvn == null) {
            sendJson(ex, 500, Map.of("error", "Không tìm thấy Maven. Đã tìm ở: "
                    + MavenResolver.moTaNoiDaTim()
                    + ". Cài Maven hoặc khai báo đường dẫn đầy đủ tới mvn.cmd bằng khóa"
                    + " run.mavenCmd trong " + RUN_FLOW_PROPERTIES + "."));
            return;
        }
        Process p;
        try {
            Files.createDirectories(LOG_FILE.getParent());
            ProcessBuilder pb = new ProcessBuilder(
                    mvn.toString(), "-Plogin", "test", "-Drun.openReport=false");
            MavenResolver.timJavaHome()
                    .ifPresent(jdk -> pb.environment().put("JAVA_HOME", jdk.toString()));
            pb.redirectOutput(LOG_FILE.toFile());
            pb.redirectErrorStream(true);
            p = pb.start();
        } catch (IOException e) {
            sendJson(ex, 500, Map.of("error", "Không khởi động được Maven (" + mvn + "): "
                    + e.getMessage()));
            return;
        }
        String chetSom = loiKhiMavenChetSom(p);
        if (chetSom != null) {
            sendJson(ex, 500, Map.of("error", chetSom));
            return;
        }
        sendJson(ex, 200, Map.of(
                "message", "Đang chạy suite login (LoginTest: 1 dương + 3 âm) — theo dõi "
                        + LOG_FILE + ". Xong thì mở lại tab Dashboard để xem báo cáo.",
                "log", LOG_FILE.toString(),
                "report", REPORT_FILE.toString()));
    }

    /**
     * Maven hỏng cấu hình (thiếu JAVA_HOME, sai profile) thì thoát trong vòng 1–2 giây. Bắt sớm để
     * trả lỗi thật kèm cuối log, thay vì báo "đang chạy" cho tiến trình đã chết.
     * Trả {@code null} nếu tiến trình vẫn sống hoặc đã kết thúc bình thường.
     */
    private static String loiKhiMavenChetSom(Process p) {
        try {
            if (!p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) || p.exitValue() == 0) {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return "Maven thoát ngay với mã " + p.exitValue() + ". Cuối " + LOG_FILE + ":\n"
                + docCuoiLog(15);
    }

    private static String docCuoiLog(int soDong) {
        try {
            List<String> dong = Files.readAllLines(LOG_FILE, StandardCharsets.UTF_8);
            return String.join("\n", dong.subList(Math.max(0, dong.size() - soDong), dong.size()));
        } catch (IOException | RuntimeException e) {
            return "(không đọc được log)";
        }
    }

    private static Path casesFileInUse() {
        Path configured = RunFlowConfig.casesFilePath();
        return configured != null ? configured : DEFAULT_CASES_FILE;
    }

    /** Sửa {@code key=value} tại chỗ trong file properties, giữ nguyên comment/định dạng — thêm dòng mới nếu chưa có. */
    private static void setProperties(Path file, Map<String, String> updates) throws IOException {
        List<String> lines = Files.exists(file)
                ? Files.readAllLines(file, StandardCharsets.UTF_8) : new ArrayList<>();
        Set<String> remaining = new LinkedHashSet<>(updates.keySet());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            if (updates.containsKey(key)) {
                lines.set(i, key + "=" + updates.get(key));
                remaining.remove(key);
            }
        }
        for (String key : remaining) {
            lines.add(key + "=" + updates.get(key));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    // ── tiện ích HTTP ────────────────────────────────────────────────────

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange ex, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}

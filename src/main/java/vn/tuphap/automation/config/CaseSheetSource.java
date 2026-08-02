package vn.tuphap.automation.config;

import vn.tuphap.automation.config.RunFlowConfig.CaseProfile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nạp danh sách test case từ một <b>Google Sheet</b> (thay cho {@code run.cases} trong file).
 * <p>
 * Sheet phải được chia sẻ <i>"Bất kỳ ai có đường liên kết — Người xem"</i>; lớp này tải bản CSV
 * qua endpoint export công khai nên không cần API key hay OAuth.
 * <p>
 * Thứ tự chống lỗi mạng (quyết định trong {@link #load(String, String)}):
 * <ol>
 *   <li>Tải sheet OK → dùng sheet, ghi cache {@code test-output/cases-sheet-cache.csv}.</li>
 *   <li>Lỗi mạng / mất quyền, có cache → dùng cache, in cảnh báo kèm thời điểm tải.</li>
 *   <li>Lỗi mạng, không cache → trả rỗng để {@link RunFlowConfig} quay về {@code run.cases}.</li>
 * </ol>
 * Kết quả được cache trong bộ nhớ theo (url, gid) — {@code RunFlowConfig.cases()} bị gọi rất nhiều
 * lần trong một lần chạy (browsers(), caseCount(), printSummary()...) nên chỉ tải mạng đúng một lần.
 *
 * <h2>Bố cục sheet</h2>
 * Dòng đầu chứa tên cột (tự nhận diện, không phân biệt hoa thường / dấu / thứ tự cột; cột lạ bị bỏ qua):
 * <pre>
 * Chạy | Loại đơn | Loại việc | Chủ thể | Tư cách | Tòa án | Số bị đơn | Đồng NĐ
 *      | Đại diện | Liên quan | TL bổ sung | Đến bước | Gửi đơn
 *      | Trường lỗi | Giá trị lỗi | Thông báo mong đợi | Ghi chú
 * </pre>
 * Ô trống ở các cột nhánh = "để automation tự chọn" (giữ nguyên hành vi sinh dữ liệu theo seed).
 * <p>
 * 3 cột cuối (trước "Ghi chú") khai báo <b>ca âm</b> — cố tình nhập sai 1 trường để kiểm tra hệ
 * thống có chặn đúng hay không (xem {@code RunFlowConfig.CaseProfile#hasNegativeExpectation()} và
 * {@code DataGenerator#applyNegativeFieldOverride}): "Trường lỗi" rỗng = case bình thường như cũ;
 * có giá trị (một trong 10 tên trường được hỗ trợ) thì "Giá trị lỗi" (có thể để trống = cố tình bỏ
 * trống trường đó) sẽ được tiêm vào scenario, và "Thông báo mong đợi" (rỗng = chấp nhận mọi thông
 * báo chặn) phải khớp một phần với thông báo hệ thống trả về để tính là PASS.
 */
public final class CaseSheetSource {

    /** Cache CSV thô của lần tải thành công gần nhất. */
    private static final Path CACHE_FILE = Paths.get("test-output", "cases-sheet-cache.csv");

    private static final Pattern DOC_ID = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
    private static final Pattern GID_IN_URL = Pattern.compile("[#?&]gid=([0-9]+)");
    private static final Duration TIMEOUT = Duration.ofSeconds(25);

    /** Kết quả nạp — {@code cases} rỗng nghĩa là caller nên fallback về {@code run.cases}. */
    public record Result(List<CaseProfile> cases, String sourceLabel) {
    }

    private static final Object LOCK = new Object();
    private static String cachedKey;
    private static Result cachedResult;

    private CaseSheetSource() {
    }

    /** Nạp (và cache trong bộ nhớ) danh sách case từ sheet. Không bao giờ ném exception. */
    public static Result load(String sheetUrl, String gid) {
        String key = (sheetUrl == null ? "" : sheetUrl.trim()) + "#" + (gid == null ? "" : gid.trim());
        synchronized (LOCK) {
            if (cachedResult != null && key.equals(cachedKey)) {
                return cachedResult;
            }
            Result r = loadUncached(sheetUrl, gid);
            cachedKey = key;
            cachedResult = r;
            return r;
        }
    }

    /** Xoá cache trong bộ nhớ (test / gọi lại sau khi sheet đổi). */
    public static void invalidate() {
        synchronized (LOCK) {
            cachedKey = null;
            cachedResult = null;
        }
    }

    private static Result loadUncached(String sheetUrl, String gid) {
        String csvUrl = toCsvExportUrl(sheetUrl, gid);
        if (csvUrl == null) {
            System.out.println("⚠ run.casesSheet='" + sheetUrl + "' không phải link Google Sheet "
                    + "(cần dạng https://docs.google.com/spreadsheets/d/<ID>/edit...).");
            return new Result(List.of(), "Google Sheet (URL không hợp lệ)");
        }

        String csv = null;
        String fetchError = null;
        try {
            csv = fetchCsv(csvUrl);
        } catch (Exception e) {
            fetchError = e.getMessage() == null ? e.toString() : e.getMessage();
        }

        if (csv != null) {
            List<CaseProfile> parsed = parseCases(csv, "Google Sheet");
            System.out.println("📗 Test case lấy từ Google Sheet: " + parsed.size() + " case  ("
                    + csvUrl + ")");
            // Chỉ ghi cache khi thật sự có case — sheet trống / sai cột không được xoá bản cache tốt.
            if (!parsed.isEmpty()) {
                writeCache(csv);
            }
            return new Result(parsed, "Google Sheet (" + parsed.size() + " case)");
        }

        String cachedCsv = readCache();
        if (cachedCsv != null) {
            String when = cacheTimestamp();
            System.out.println("⚠ Không tải được Google Sheet (" + fetchError
                    + ") — dùng cache tải lúc " + when + ".");
            List<CaseProfile> parsed = parseCases(cachedCsv, "cache sheet");
            return new Result(parsed, "Cache sheet (" + parsed.size() + " case, tải lúc " + when + ")");
        }

        System.out.println("⚠ Không tải được Google Sheet (" + fetchError + ") và chưa có cache — "
                + "quay về run.cases trong run-flow.properties.");
        return new Result(List.of(), "Google Sheet (lỗi tải)");
    }

    // ---------------------------------------------------------------- tải CSV

    /**
     * Đổi link chia sẻ / link edit / ID thuần thành endpoint export CSV.
     * {@code gid} truyền vào thắng {@code gid} trong URL; không có gid nào thì lấy tab đầu tiên.
     */
    static String toCsvExportUrl(String sheetUrl, String gidOverride) {
        if (sheetUrl == null || sheetUrl.isBlank()) {
            return null;
        }
        String raw = sheetUrl.trim();
        String docId;
        Matcher m = DOC_ID.matcher(raw);
        if (m.find()) {
            docId = m.group(1);
        } else if (raw.matches("[a-zA-Z0-9-_]{20,}")) {
            docId = raw; // người dùng dán thẳng ID
        } else {
            return null;
        }

        String gid = gidOverride == null ? "" : gidOverride.trim();
        if (gid.isBlank()) {
            Matcher g = GID_IN_URL.matcher(raw);
            if (g.find()) {
                gid = g.group(1);
            }
        }
        String url = "https://docs.google.com/spreadsheets/d/" + docId + "/export?format=csv";
        if (!gid.isBlank()) {
            url = url + "&gid=" + gid;
        }
        return url;
    }

    private static String fetchCsv(String csvUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(TIMEOUT)
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(csvUrl))
                .timeout(TIMEOUT)
                .header("User-Agent", "ToaAn-Automation/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        int code = res.statusCode();
        if (code != 200) {
            throw new IllegalStateException("HTTP " + code + " — kiểm tra sheet đã chia sẻ "
                    + "\"Bất kỳ ai có đường liên kết — Người xem\" chưa");
        }
        String contentType = res.headers().firstValue("content-type").orElse("");
        String body = new String(res.body(), StandardCharsets.UTF_8);
        if (contentType.toLowerCase(Locale.ROOT).contains("text/html")
                || body.stripLeading().startsWith("<")) {
            throw new IllegalStateException("Google trả về trang HTML (thường là trang đăng nhập) — "
                    + "sheet chưa được chia sẻ công khai dạng \"Người xem\"");
        }
        return body;
    }

    private static void writeCache(String csv) {
        try {
            Path parent = CACHE_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(CACHE_FILE, csv, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("⚠ Không ghi được cache sheet (" + CACHE_FILE + "): " + e.getMessage());
        }
    }

    private static String readCache() {
        try {
            if (!Files.exists(CACHE_FILE)) {
                return null;
            }
            String csv = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
            return csv.isBlank() ? null : csv;
        } catch (Exception e) {
            return null;
        }
    }

    private static String cacheTimestamp() {
        try {
            Instant t = Files.getLastModifiedTime(CACHE_FILE).toInstant();
            return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(t);
        } catch (Exception e) {
            return "(không rõ thời điểm)";
        }
    }

    // ------------------------------------------------------------- parse CSV

    /** Cột nhận diện được trên sheet. */
    private enum Col {
        CHAY, LOAI_DON, LOAI_VIEC, CHU_THE, TU_CACH, TOA_AN, SO_BI_DON,
        DONG_NGUYEN_DON, DAI_DIEN, LIEN_QUAN, TAI_LIEU_BO_SUNG, DEN_BUOC, GUI_DON,
        TRUONG_LOI, GIA_TRI_LOI, THONG_BAO_MONG_DOI, GHI_CHU
    }

    /** Tên cột chấp nhận được (đã chuẩn hoá: bỏ dấu, bỏ khoảng trắng, chữ thường). */
    private static final Map<Col, String[]> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put(Col.LOAI_DON, new String[]{"loaidon", "loaidonkhoikien", "donkhoikien", "loaiho so"});
        ALIASES.put(Col.LOAI_VIEC, new String[]{"loaiviec", "viec", "quanhephapluat"});
        ALIASES.put(Col.CHU_THE, new String[]{"chuthe", "chuthenguyendon", "nguyendon", "cntc", "cn tc"});
        ALIASES.put(Col.TU_CACH, new String[]{"tucach", "tucachnopdon", "tucachnguoinopdon"});
        ALIASES.put(Col.TOA_AN, new String[]{"toaan", "toaannhandon", "toa"});
        ALIASES.put(Col.SO_BI_DON, new String[]{"sobidon", "soluongbidon", "sobd", "bidon"});
        ALIASES.put(Col.DONG_NGUYEN_DON, new String[]{"dongnguyendon", "dongnd", "codongnguyendon", "dongkhoikien"});
        ALIASES.put(Col.DAI_DIEN, new String[]{"daidien", "nguoidaidien", "codaidien", "nguoidaidientheouyquyen"});
        ALIASES.put(Col.LIEN_QUAN, new String[]{"lienquan", "nguoilienquan", "colienquan", "nguoicoquyenloilienquan"});
        ALIASES.put(Col.TAI_LIEU_BO_SUNG, new String[]{"tailieubosung", "tlbosung", "tlbs", "tailieu", "hosobosung"});
        ALIASES.put(Col.DEN_BUOC, new String[]{"denbuoc", "buoc", "dungobuoc", "dosau", "untilstep", "until", "step"});
        ALIASES.put(Col.GUI_DON, new String[]{"guidon", "gui", "submit", "nopdon"});
        ALIASES.put(Col.CHAY, new String[]{"chay", "run", "enable", "bat", "kichhoat", "chayko", "active"});
        ALIASES.put(Col.TRUONG_LOI, new String[]{"truongloi", "truongsailoi", "truongcansai", "invalidfield", "fieldloi"});
        ALIASES.put(Col.GIA_TRI_LOI, new String[]{"giatriloi", "giatrisai", "invalidvalue", "gtloi"});
        ALIASES.put(Col.THONG_BAO_MONG_DOI, new String[]{"thongbaomongdoi", "thongbaoloimongdoi", "expectedmessage", "thongbaokyvong", "mongdoi"});
        ALIASES.put(Col.GHI_CHU, new String[]{"ghichu", "note", "mota", "tencase", "tentestcase", "matc", "machaycase"});
    }

    /** Parse CSV → danh sách case (package-private để unit test khỏi cần mạng). */
    static List<CaseProfile> parseCases(String csv, String sourceLabel) {
        List<List<String>> rows = parseCsv(csv);
        int headerIdx = -1;
        Map<Col, Integer> cols = Map.of();
        for (int i = 0; i < rows.size() && i < 20; i++) {
            Map<Col, Integer> candidate = mapHeader(rows.get(i));
            if (candidate.containsKey(Col.LOAI_DON)) {
                headerIdx = i;
                cols = candidate;
                break;
            }
        }
        if (headerIdx < 0) {
            System.out.println("⚠ " + sourceLabel + ": không thấy dòng tiêu đề có cột \"Loại đơn\" — "
                    + "không đọc được case nào. Xem mẫu cột ở README mục 6.4.");
            return List.of();
        }

        List<CaseProfile> out = new ArrayList<>();
        int skippedOff = 0;
        int skippedBad = 0;
        for (int r = headerIdx + 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (isBlankRow(row)) {
                continue;
            }
            String loaiDon = cell(row, cols.get(Col.LOAI_DON));
            if (loaiDon.isBlank()) {
                skippedBad++;
                continue;
            }
            if (cols.containsKey(Col.CHAY) && !flagOn(cell(row, cols.get(Col.CHAY)))) {
                skippedOff++;
                continue;
            }
            out.add(toProfile(row, cols, loaiDon));
        }

        if (skippedOff > 0) {
            System.out.println("   ↳ " + skippedOff + " dòng bị bỏ qua (cột \"Chạy\" trống / = Không).");
        }
        if (skippedBad > 0) {
            System.out.println("   ↳ " + skippedBad + " dòng bị bỏ qua (thiếu Loại đơn).");
        }
        return out;
    }

    private static CaseProfile toProfile(List<String> row, Map<Col, Integer> cols, String loaiDon) {
        String untilRaw = cell(row, cols.get(Col.DEN_BUOC));
        boolean submit = flagOn(cell(row, cols.get(Col.GUI_DON)));
        // "Đến bước" cho phép ghi gộp "6:submit" như run.cases
        int colon = untilRaw.indexOf(':');
        if (colon >= 0) {
            submit = submit || flagOn(untilRaw.substring(colon + 1));
            untilRaw = untilRaw.substring(0, colon).trim();
        }
        int until = RunFlowConfig.parseUntilStep(untilRaw.isBlank() ? "6" : untilRaw);
        if (until < 6) {
            submit = false;
        }

        return new CaseProfile(
                loaiDon,
                cell(row, cols.get(Col.LOAI_VIEC)),
                RunFlowConfig.resolveChuThe(cell(row, cols.get(Col.CHU_THE))),
                blankToEmpty(cell(row, cols.get(Col.TU_CACH))),
                blankToEmpty(cell(row, cols.get(Col.TOA_AN))),
                parseSoBiDon(cell(row, cols.get(Col.SO_BI_DON))),
                triState(cell(row, cols.get(Col.DONG_NGUYEN_DON))),
                triState(cell(row, cols.get(Col.DAI_DIEN))),
                triState(cell(row, cols.get(Col.LIEN_QUAN))),
                triState(cell(row, cols.get(Col.TAI_LIEU_BO_SUNG))),
                cell(row, cols.get(Col.GHI_CHU)),
                cell(row, cols.get(Col.TRUONG_LOI)),
                cell(row, cols.get(Col.GIA_TRI_LOI)),
                cell(row, cols.get(Col.THONG_BAO_MONG_DOI)),
                until,
                submit);
    }

    private static Map<Col, Integer> mapHeader(List<String> header) {
        Map<Col, Integer> cols = new LinkedHashMap<>();
        for (int c = 0; c < header.size(); c++) {
            String key = norm(header.get(c));
            if (key.isEmpty()) {
                continue;
            }
            Col col = matchColumn(key);
            if (col != null && !cols.containsKey(col)) {
                cols.put(col, c);
            }
        }
        return cols;
    }

    private static Col matchColumn(String normalizedHeader) {
        for (Map.Entry<Col, String[]> e : ALIASES.entrySet()) {
            for (String alias : e.getValue()) {
                if (normalizedHeader.equals(norm(alias))) {
                    return e.getKey();
                }
            }
        }
        // Khớp lỏng theo thứ tự ưu tiên (tránh "gửi đơn" bị hiểu là "loại đơn")
        for (Map.Entry<Col, String[]> e : ALIASES.entrySet()) {
            for (String alias : e.getValue()) {
                String a = norm(alias);
                if (a.length() >= 4 && normalizedHeader.contains(a)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    /** CSV theo RFC 4180: hỗ trợ ô có dấu phẩy, dấu nháy kép nhân đôi và xuống dòng trong ô. */
    static List<List<String>> parseCsv(String csvIn) {
        String csv = csvIn == null ? "" : csvIn;
        if (csv.startsWith("﻿")) {
            csv = csv.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(c);
                }
                continue;
            }
            if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (c != '\r') {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    // ------------------------------------------------------------- tiện ích ô

    private static String cell(List<String> row, Integer index) {
        if (index == null || index < 0 || index >= row.size()) {
            return "";
        }
        String v = row.get(index);
        return v == null ? "" : v.trim();
    }

    private static boolean isBlankRow(List<String> row) {
        for (String c : row) {
            if (c != null && !c.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Ô bật: x / v / ✓ / có / yes / true / 1 / on. Trống hoặc "-" = tắt. */
    static boolean flagOn(String raw) {
        String v = norm(raw);
        if (v.isEmpty()) {
            return false;
        }
        return switch (v) {
            case "x", "v", "co", "yes", "y", "true", "1", "on", "submit", "gui", "bat", "run" -> true;
            default -> "✓".equals(raw.trim()) || "✔".equals(raw.trim());
        };
    }

    /** Tri-state: trống / "-" = null (automation tự chọn); còn lại theo {@link #flagOn}. */
    static Boolean triState(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.isEmpty() || "-".equals(t)) {
            return null;
        }
        return flagOn(t);
    }

    /** Số bị đơn: 0 = tự chọn; ngoài 1–2 thì kẹp lại. */
    static int parseSoBiDon(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.isEmpty() || "-".equals(t)) {
            return 0;
        }
        try {
            int n = Integer.parseInt(t);
            return Math.max(0, Math.min(2, n));
        } catch (NumberFormatException e) {
            System.out.println("⚠ Sheet: số bị đơn='" + t + "' không phải số — để automation tự chọn.");
            return 0;
        }
    }

    private static String blankToEmpty(String raw) {
        String t = raw == null ? "" : raw.trim();
        return "-".equals(t) ? "" : t;
    }

    /** Chuẩn hoá để so tên cột / giá trị: chữ thường, bỏ dấu, bỏ mọi ký tự không phải chữ-số. */
    static String norm(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return t.replaceAll("[^a-z0-9]", "");
    }
}

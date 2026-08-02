package vn.tuphap.automation.config;

import vn.tuphap.automation.core.BrowserSlot;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Đọc {@code run-flow.properties} — bảng điều khiển duy nhất.
 * Ưu tiên: {@code -Dkey} → env {@code TOAAN_*} → run-flow.properties → mặc định.
 * <p>
 * {@code run.slots} (tuỳ chọn): mỗi Chrome một độ sâu / submit riêng.
 * Ví dụ {@code 3|6:submit} → Chrome 1 dừng bước 3; Chrome 2 điền đủ + gửi đơn.
 */
public final class RunFlowConfig {

    private static final Properties FILE = new Properties();
    private static final boolean LOADED;

    static {
        boolean ok = loadClasspathProperties("run-flow.properties");
        if (!ok) {
            System.out.println("⚠ Không thấy run-flow.properties — dùng mặc định / -D / env.");
        }
        LOADED = ok;
        applyKnownSystemAliases();
    }

    private static boolean loadClasspathProperties(String name) {
        try (InputStream in = RunFlowConfig.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                return false;
            }
            FILE.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            System.out.println("⚠ Không nạp được " + name + ": " + e.getMessage());
            return false;
        }
    }

    private RunFlowConfig() {
    }

    /** smoke | mid | full | login | buoc23 | unit | master */
    public static String suite() {
        return text("run.suite", "smoke").toLowerCase(Locale.ROOT);
    }

    public static boolean parallel() {
        if (browsers() <= 1) {
            return false;
        }
        if (hasCases() || hasSlotsOnly()) {
            return true;
        }
        return bool("run.parallel", false);
    }

    /**
     * Số Chrome mở song song (tối thiểu 1, tối đa 8).
     * Có {@code run.cases}: lấy {@code run.browsers}, không vượt quá số case
     * (vd. 2 Chrome chạy lần lượt 5 case).
     */
    public static int browsers() {
        int configured = integer("run.browsers", 1);
        configured = Math.max(1, Math.min(8, configured));
        if (hasCases()) {
            return Math.max(1, Math.min(configured, caseCount()));
        }
        if (hasSlotsOnly()) {
            return Math.max(1, Math.min(8, slotCount()));
        }
        return configured;
    }

    public static int windowWidth() {
        return integer("run.window.width", 520);
    }

    public static int windowHeight() {
        return integer("run.window.height", 580);
    }

    public static double windowScale() {
        return decimal("run.window.scale", 0.55);
    }

    public static boolean requireSubmit() {
        return bool("run.requireSubmit", false);
    }

    public static boolean openReport() {
        return bool("run.openReport", true);
    }

    /** Case tùy chọn — từ Google Sheet ({@code run.casesSheet}) hoặc từ menu ({@code run.cases}). */
    public static boolean hasCases() {
        return !cases().isEmpty();
    }

    public static int caseCount() {
        return cases().size();
    }

    /**
     * Danh sách case sẽ chạy.
     * <p>
     * Nguồn theo {@code run.caseSource}: {@code sheet} (mặc định khi có {@code run.casesSheet})
     * đọc Google Sheet — mất mạng thì dùng cache, không có cache thì quay về {@code run.cases};
     * {@code file} đọc thẳng {@code run.cases} trong {@code run-flow.properties}.
     */
    public static List<CaseProfile> cases() {
        if (useSheet()) {
            List<CaseProfile> fromSheet = CaseSheetSource
                    .load(casesSheetUrl(), text("run.casesSheetGid", "")).cases();
            if (!fromSheet.isEmpty()) {
                return fromSheet;
            }
        }
        return parseCases(raw("run.cases"));
    }

    /** URL Google Sheet chứa danh sách case ({@code run.casesSheet}); rỗng = không dùng sheet. */
    public static String casesSheetUrl() {
        return text("run.casesSheet", "");
    }

    /** Có lấy case từ Google Sheet hay không ({@code run.caseSource=sheet} + có URL). */
    public static boolean useSheet() {
        if (!hasText(casesSheetUrl())) {
            return false;
        }
        String src = text("run.caseSource", "sheet").trim().toLowerCase(Locale.ROOT);
        return !("file".equals(src) || "properties".equals(src) || "run.cases".equals(src));
    }

    /** Nhãn nguồn case để in ra log / báo cáo. */
    public static String caseSourceLabel() {
        String fromFile = hasText(raw("run.cases"))
                ? "run.cases (" + parseCases(raw("run.cases")).size() + " case)"
                : "(không có case)";
        if (!useSheet()) {
            return fromFile;
        }
        CaseSheetSource.Result r = CaseSheetSource.load(casesSheetUrl(), text("run.casesSheetGid", ""));
        return r.cases().isEmpty() ? r.sourceLabel() + " → " + fromFile : r.sourceLabel();
    }

    /** Có cấu hình từng Chrome ({@code run.slots} thuần — không suy từ run.cases). */
    public static boolean hasSlots() {
        return hasSlotsOnly();
    }

    /** Chỉ {@code run.slots} thuần (không tính cases). */
    public static boolean hasSlotsOnly() {
        return !parseSlots(raw("run.slots")).isEmpty();
    }

    public static int slotCount() {
        return slots().size();
    }

    public static List<SlotProfile> slots() {
        return parseSlots(raw("run.slots"));
    }

    /** Độ sâu / submit gắn theo case đang chạy trên thread (khi cases > Chrome). */
    private static final ThreadLocal<SlotProfile> BOUND_CASE_DEPTH = new ThreadLocal<>();

    public static void bindCaseProfile(CaseProfile profile) {
        if (profile == null) {
            BOUND_CASE_DEPTH.remove();
            return;
        }
        BOUND_CASE_DEPTH.set(new SlotProfile(profile.untilStep(), profile.submit()));
    }

    public static void clearBoundCase() {
        BOUND_CASE_DEPTH.remove();
    }

    private static SlotProfile boundCaseOrNull() {
        return BOUND_CASE_DEPTH.get();
    }

    /**
     * Độ sâu fall-through từ {@code run-flow.properties}.
     * {@code login}/{@code 0} = chỉ đăng nhập; {@code 1}…{@code 6} = dừng sau bước đó.
     * Ưu tiên: case đang chạy (menu) → {@code run.slots} theo Chrome → {@code run.untilStep}.
     */
    public static String untilStepRaw() {
        SlotProfile bound = boundCaseOrNull();
        if (bound != null) {
            return String.valueOf(bound.untilStep());
        }
        SlotProfile slot = currentSlotOrNull();
        if (slot != null) {
            return String.valueOf(slot.untilStep());
        }
        return text("run.untilStep", "6");
    }

    /**
     * Số bước tối đa sẽ chạy (0 = chỉ login). Giá trị không hợp lệ → 6.
     */
    public static int untilStep() {
        SlotProfile bound = boundCaseOrNull();
        if (bound != null) {
            return bound.untilStep();
        }
        SlotProfile slot = currentSlotOrNull();
        if (slot != null) {
            return slot.untilStep();
        }
        return parseUntilStep(untilStepRaw());
    }

    /**
     * Số thư mục lượt chạy giữ lại trong {@code test-output/runs/}; {@code 0} = không bao giờ xoá.
     * <p>
     * Một lượt FULL chiếm ~36 MB trên đĩa và toàn bộ JSON của mọi lượt được phân tích lại sau mỗi
     * lần chạy, nên để tích luỹ vô hạn là có ngày báo cáo ngừng dựng được.
     */
    public static String keepRuns() {
        return text("run.keepRuns", "30");
    }

    /** Max untilStep trên mọi case/slot (hoặc cấu hình chung) — dùng cho DataProvider. */
    public static int maxUntilStep() {
        if (hasCases()) {
            int max = 0;
            for (CaseProfile c : cases()) {
                max = Math.max(max, c.untilStep());
            }
            return max;
        }
        List<SlotProfile> list = slots();
        if (!list.isEmpty()) {
            int max = 0;
            for (SlotProfile s : list) {
                max = Math.max(max, s.untilStep());
            }
            return max;
        }
        return parseUntilStep(text("run.untilStep", "6"));
    }

    /** Chỉ gửi đơn khi {@link #untilStep()} ≥ 6 và submit=true (theo case / slot / chung). */
    public static boolean submit() {
        SlotProfile bound = boundCaseOrNull();
        if (bound != null) {
            return bound.submit() && bound.untilStep() >= 6;
        }
        SlotProfile slot = currentSlotOrNull();
        if (slot != null) {
            return slot.submit() && slot.untilStep() >= 6;
        }
        return bool("run.submit", false) && untilStep() >= 6;
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    /**
     * Đẩy giá trị sang system property quen thuộc ({@code taodon.*})
     * để TestNG / code cũ đọc thống nhất.
     */
    public static void applyKnownSystemAliases() {
        putIfAbsentSys("taodon.suite", suite());
        putIfAbsentSys("taodon.parallel", String.valueOf(parallel()));
        putIfAbsentSys("taodon.threads", String.valueOf(browsers()));
        putIfAbsentSys("taodon.requireSubmit", String.valueOf(requireSubmit()));
        putIfAbsentSys("taodon.window.width", String.valueOf(windowWidth()));
        putIfAbsentSys("taodon.window.height", String.valueOf(windowHeight()));
        putIfAbsentSys("taodon.window.scale", String.valueOf(windowScale()));
        putIfAbsentSys("taodon.untilStep", text("run.untilStep", "6"));
        putIfAbsentSys("taodon.submit", String.valueOf(bool("run.submit", false)));
        String slotsRaw = text("run.slots", "");
        if (hasText(slotsRaw)) {
            putIfAbsentSys("taodon.slots", slotsRaw);
            putIfAbsentSys("run.slots", slotsRaw);
        }
        String casesRaw = text("run.cases", "");
        // Không đẩy run.cases sang -D (dấu > làm vỡ shell). Đọc từ file / env.
        if (hasText(casesRaw)) {
            putIfAbsentSys("taodon.cases", casesRaw);
        }
        String sheetUrl = text("run.casesSheet", "");
        if (hasText(sheetUrl)) {
            putIfAbsentSys("taodon.casesSheet", sheetUrl);
        }
    }

    /** In bảng tóm tắt dễ đọc lúc bắt đầu suite. */
    public static void printSummary(String suiteNameFromTestNg) {
        List<CaseProfile> caseListForDetail = List.of();
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           CẤU HÌNH LUỒNG (run-flow.properties)               ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  File nạp được     : " + pad(LOADED ? "Có" : "Không (dùng mặc định)"));
        System.out.println("║  Gói (run.suite)   : " + pad(suite()));
        System.out.println("║  Suite TestNG      : " + pad(suiteNameFromTestNg == null ? "(chưa có)" : suiteNameFromTestNg));
        System.out.println("║  Song song         : " + pad(parallel() ? "BẬT" : "TẮT (1 Chrome)"));
        System.out.println("║  Số trình duyệt    : " + pad(String.valueOf(browsers())));
        if (parallel()) {
            System.out.println("║  Cửa sổ (rxc)      : " + pad(windowWidth() + " x " + windowHeight() + " px"));
        }
        if (hasCases()) {
            List<CaseProfile> list = cases();
            System.out.println("║  Số case           : " + pad(String.valueOf(list.size())
                    + (list.size() > browsers()
                    ? " (xếp hàng trên " + browsers() + " Chrome)" : "")));
            System.out.println("║  Nguồn case        : " + pad(caseSourceLabel()));
            if (list.size() > 2) {
                System.out.println("║  Chi tiết case     : " + pad("xem STT bên dưới khung"));
                caseListForDetail = list;
            } else {
                for (int i = 0; i < list.size(); i++) {
                    System.out.println("║  Case #" + (i + 1) + ": "
                            + pad(list.get(i).shortLabel()));
                }
            }
        } else if (hasSlotsOnly()) {
            List<SlotProfile> list = parseSlots(raw("run.slots"));
            System.out.println("║  run.slots         : " + pad(formatSlotsShort(list)));
            for (int i = 0; i < list.size(); i++) {
                System.out.println("║  Case #" + (i + 1) + " (Chrome " + (i + 1) + "): "
                        + pad(list.get(i).label()));
            }
        } else {
            if (useSheet()) {
                System.out.println("║  Nguồn case        : " + pad(caseSourceLabel()));
            }
            System.out.println("║  untilStep         : " + pad(untilStepLabel(parseUntilStep(text("run.untilStep", "6")),
                    bool("run.submit", false))));
            System.out.println("║  submit (bước 6)   : " + pad(bool("run.submit", false)
                    ? "Có — bấm Gửi đơn" : "Không — dừng an toàn"));
        }
        System.out.println("║  Mở báo cáo hết run: " + pad(openReport() ? "Có" : "Không"));
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  File cấu hình     : src/test/resources/run-flow.properties ║");
        System.out.println("║  Menu dễ dùng      : .\\scripts\\chay.cmd                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        if (!caseListForDetail.isEmpty()) {
            for (int i = 0; i < caseListForDetail.size(); i++) {
                CaseProfile c = caseListForDetail.get(i);
                System.out.println("  Case #" + (i + 1) + ": "
                        + c.loaiDon() + " > " + c.loaiViec()
                        + " > " + c.chuThe()
                        + (hasText(c.tuCachNopDon()) ? " > " + c.tuCachNopDon() : "")
                        + " > " + c.untilStep() + (c.submit() ? ":submit" : "")
                        + "  —  " + c.shortLabel()
                        + (hasText(c.ghiChu()) ? "  [" + c.ghiChu() + "]" : ""));
            }
            System.out.println();
        } else {
            System.out.println();
        }
    }

    private static String formatSlotsShort(List<SlotProfile> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(list.get(i).toConfigToken());
        }
        return sb.toString();
    }

    private static String untilStepLabel(int n, boolean submit) {
        return switch (n) {
            case 0 -> "login (0) — chỉ đăng nhập";
            case 1 -> "1 — Loại đơn / Tòa án";
            case 2 -> "2 — Nguyên đơn";
            case 3 -> "3 — Bị đơn";
            case 4 -> "4 — Nội dung đơn";
            case 5 -> "5 — Tài liệu";
            default -> "6 — Xem lại" + (submit ? " + Gửi đơn" : "");
        };
    }

    private static SlotProfile currentSlotOrNull() {
        List<SlotProfile> list = slots();
        if (list.isEmpty()) {
            return null;
        }
        Integer idx = BrowserSlot.get();
        if (idx == null) {
            // Chưa gán slot: 1 profile → dùng luôn; nhiều profile → chờ BrowserLayout
            return list.size() == 1 ? list.get(0) : null;
        }
        if (idx < 0) {
            return list.get(0);
        }
        return list.get(idx % list.size());
    }

    static int parseUntilStep(String rawIn) {
        String raw = rawIn == null ? "" : rawIn.trim().toLowerCase(Locale.ROOT);
        if (raw.isBlank() || "login".equals(raw) || "0".equals(raw)) {
            return 0;
        }
        try {
            int n = Integer.parseInt(raw);
            return Math.max(0, Math.min(6, n));
        } catch (NumberFormatException e) {
            System.out.println("⚠ run.untilStep='" + raw + "' không hợp lệ — dùng 6.");
            return 6;
        }
    }

    /**
     * Parse {@code run.slots}: {@code 3|6:submit|6} — mỗi phần một Chrome.
     * Token: {@code <bước>} hoặc {@code <bước>:submit|true|gui|yes}.
     */
    static List<SlotProfile> parseSlots(String raw) {
        if (!hasText(raw)) {
            return List.of();
        }
        String[] parts = raw.trim().split("\\|");
        List<SlotProfile> out = new ArrayList<>();
        for (String part : parts) {
            if (!hasText(part)) {
                continue;
            }
            String token = part.trim();
            String stepPart = token;
            boolean submit = false;
            int colon = token.indexOf(':');
            if (colon >= 0) {
                stepPart = token.substring(0, colon).trim();
                String flag = token.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
                submit = "submit".equals(flag)
                        || "true".equals(flag)
                        || "yes".equals(flag)
                        || "gui".equals(flag)
                        || "1".equals(flag)
                        || "on".equals(flag);
            }
            int until = parseUntilStep(stepPart);
            if (until < 6) {
                submit = false;
            }
            out.add(new SlotProfile(until, submit));
        }
        return out;
    }

    /**
     * Parse {@code run.cases}: mỗi Chrome một case, ngăn bởi {@code |}.
     * Format: {@code Loại đơn>Loại việc>CN|TC>tư cách|->until[:submit]}
     * Ví dụ: {@code Dân sự>Hợp đồng dân sự>CN>->3|Phá sản>Yêu cầu mở thủ tục phá sản>TC>Chủ nợ>6:submit}
     */
    static List<CaseProfile> parseCases(String raw) {
        if (!hasText(raw)) {
            return List.of();
        }
        String[] parts = raw.trim().split("\\|");
        List<CaseProfile> out = new ArrayList<>();
        for (String part : parts) {
            if (!hasText(part)) {
                continue;
            }
            CaseProfile parsed = parseOneCase(part.trim());
            if (parsed != null) {
                out.add(parsed);
            }
        }
        return out;
    }

    private static CaseProfile parseOneCase(String token) {
        String[] bits = token.split(">", -1);
        if (bits.length < 5) {
            System.out.println("⚠ run.cases token thiếu trường (cần 5 phần): " + token);
            return null;
        }
        // until nằm ở phần cuối; tư cách có thể chứa dấu '>' hiếm — gộp phần giữa nếu >5
        String loaiDon = bits[0].trim();
        String loaiViec = bits[1].trim();
        String chuTheRaw = bits[2].trim();
        String untilToken = bits[bits.length - 1].trim();
        StringBuilder tuCachBuf = new StringBuilder();
        for (int i = 3; i < bits.length - 1; i++) {
            if (tuCachBuf.length() > 0) {
                tuCachBuf.append('>');
            }
            tuCachBuf.append(bits[i].trim());
        }
        String tuCach = tuCachBuf.toString();
        if ("-".equals(tuCach) || tuCach.isBlank()) {
            tuCach = "";
        }

        boolean submit = false;
        String stepPart = untilToken;
        int colon = untilToken.indexOf(':');
        if (colon >= 0) {
            stepPart = untilToken.substring(0, colon).trim();
            String flag = untilToken.substring(colon + 1).trim().toLowerCase(Locale.ROOT);
            submit = "submit".equals(flag) || "true".equals(flag) || "yes".equals(flag)
                    || "gui".equals(flag) || "1".equals(flag) || "on".equals(flag);
        }
        int until = parseUntilStep(stepPart);
        if (until < 6) {
            submit = false;
        }
        String chuThe = resolveChuThe(chuTheRaw);
        return new CaseProfile(loaiDon, loaiViec, chuThe, tuCach, until, submit);
    }

    static String resolveChuThe(String raw) {
        if (!hasText(raw)) {
            return "Cá nhân";
        }
        String n = raw.trim().toLowerCase(Locale.ROOT);
        if ("tc".equals(n) || n.contains("tổ chức") || n.contains("to chuc") || n.contains("doanh nghiệp")
                || n.contains("doanh nghiep")) {
            return "Tổ chức / Doanh nghiệp";
        }
        if ("cn".equals(n) || n.contains("cá nhân") || n.contains("ca nhan")) {
            return "Cá nhân";
        }
        return raw.trim();
    }

    public static String text(String key, String defaultValue) {
        String v = raw(key);
        return hasText(v) ? v.trim() : defaultValue;
    }

    public static boolean bool(String key, boolean defaultValue) {
        String v = raw(key);
        if (!hasText(v)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(v.trim())
                || "yes".equalsIgnoreCase(v.trim())
                || "1".equals(v.trim())
                || "on".equalsIgnoreCase(v.trim());
    }

    public static int integer(String key, int defaultValue) {
        String v = raw(key);
        if (!hasText(v)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double decimal(String key, double defaultValue) {
        String v = raw(key);
        if (!hasText(v)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String raw(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String fromSys = System.getProperty(key);
        if (hasText(fromSys)) {
            return fromSys;
        }
        String alias = aliasSys(key);
        if (hasText(alias)) {
            return alias;
        }
        String envKey = toEnvKey(key);
        String fromEnv = System.getenv(envKey);
        if (hasText(fromEnv)) {
            return fromEnv;
        }
        return FILE.getProperty(key);
    }

    private static String aliasSys(String key) {
        return switch (key) {
            case "run.suite" -> System.getProperty("taodon.suite");
            case "run.parallel" -> System.getProperty("taodon.parallel");
            case "run.browsers" -> firstNonBlank(
                    System.getProperty("taodon.threads"),
                    System.getProperty("taodon.browsers"));
            case "run.requireSubmit" -> System.getProperty("taodon.requireSubmit");
            case "run.untilStep" -> System.getProperty("taodon.untilStep");
            case "run.submit" -> System.getProperty("taodon.submit");
            case "run.slots" -> System.getProperty("taodon.slots");
            case "run.cases" -> System.getProperty("taodon.cases");
            case "run.casesSheet" -> System.getProperty("taodon.casesSheet");
            case "run.casesSheetGid" -> System.getProperty("taodon.casesSheetGid");
            case "run.caseSource" -> System.getProperty("taodon.caseSource");
            case "run.window.width" -> System.getProperty("taodon.window.width");
            case "run.window.height" -> System.getProperty("taodon.window.height");
            case "run.window.scale" -> System.getProperty("taodon.window.scale");
            default -> null;
        };
    }

    private static String firstNonBlank(String a, String b) {
        if (hasText(a)) {
            return a;
        }
        if (hasText(b)) {
            return b;
        }
        return null;
    }

    private static void putIfAbsentSys(String key, String value) {
        if (!hasText(System.getProperty(key)) && hasText(value)) {
            System.setProperty(key, value);
        }
    }

    private static String toEnvKey(String key) {
        return "TOAAN_" + key.trim()
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String pad(String s) {
        String t = s == null ? "" : s;
        if (t.length() >= 36) {
            return t.substring(0, 33) + "...";
        }
        return String.format("%-36s", t) + "║";
    }

    /** Cấu hình độ sâu / submit cho một Chrome (slot). */
    public record SlotProfile(int untilStep, boolean submit) {
        public String label() {
            return untilStepLabel(untilStep, submit);
        }

        public String toConfigToken() {
            if (submit && untilStep >= 6) {
                return untilStep + ":submit";
            }
            return String.valueOf(untilStep);
        }
    }

    /**
     * Một case cụ thể — từ menu ({@code run.cases}) hoặc từ Google Sheet ({@code run.casesSheet}).
     * <p>
     * Các trường nhánh chỉ có trong sheet dùng quy ước <b>"trống = automation tự chọn"</b>:
     * {@code toaAn} rỗng, {@code soLuongBiDon} = 0 và các {@code Boolean} = {@code null}
     * đều để {@code DataGenerator} giữ nguyên cách sinh theo seed như trước.
     */
    public record CaseProfile(
            String loaiDon,
            String loaiViec,
            String chuThe,
            String tuCachNopDon,
            String toaAn,
            int soLuongBiDon,
            Boolean coDongNguyenDon,
            Boolean coNguoiDaiDien,
            Boolean coNguoiLienQuan,
            Boolean coTaiLieuBoSung,
            String ghiChu,
            /** Tên trường muốn ép giá trị sai (rỗng = case bình thường, không phải ca âm). */
            String truongLoi,
            /** Giá trị sai sẽ điền vào {@code truongLoi} (rỗng = cố tình để trống trường đó). */
            String giaTriLoi,
            /** Chuỗi con bắt buộc có trong thông báo hệ thống trả về (rỗng = chấp nhận mọi thông báo chặn). */
            String thongBaoMongDoi,
            int untilStep,
            boolean submit
    ) {
        /** Dạng gọn của {@code run.cases} — 5 trường, mọi nhánh khác để automation tự chọn. */
        public CaseProfile(String loaiDon, String loaiViec, String chuThe, String tuCachNopDon,
                           int untilStep, boolean submit) {
            this(loaiDon, loaiViec, chuThe, tuCachNopDon, "", 0,
                    null, null, null, null, "", "", "", "", untilStep, submit);
        }

        /** Case này có phải ca âm (cố tình nhập sai để kiểm tra hệ thống chặn đúng) không. */
        public boolean hasNegativeExpectation() {
            return truongLoi != null && !truongLoi.isBlank();
        }

        public String shortLabel() {
            String depth = untilStepLabel(untilStep, submit);
            String label = loaiDon + " / " + (chuThe == null ? "?" : chuThe) + " @ " + depth;
            return hasNegativeExpectation() ? label + " [ca âm: " + truongLoi + "]" : label;
        }

        public String toConfigToken() {
            String tu = (tuCachNopDon == null || tuCachNopDon.isBlank()) ? "-" : tuCachNopDon;
            String chu = chuThe != null && chuThe.toLowerCase(Locale.ROOT).contains("tổ chức") ? "TC" : "CN";
            String until = submit && untilStep >= 6 ? untilStep + ":submit" : String.valueOf(untilStep);
            return loaiDon + ">" + loaiViec + ">" + chu + ">" + tu + ">" + until;
        }
    }
}

package vn.tuphap.automation.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class MasterDataCatalog {

    private static final String RESOURCE_PATH = "master-data.properties";
    private static final String LOAI_VIEC_PREFIX = "loaiViec.";

    private static volatile Map<String, String[]> catalog;
    private static volatile List<String[]> loaiDonViecPairs;

    private MasterDataCatalog() {}

    public static void reload() {
        synchronized (MasterDataCatalog.class) {
            catalog = loadCatalog();
            loaiDonViecPairs = null;
        }
    }

    private static Map<String, String[]> catalog() {
        Map<String, String[]> current = catalog;
        if (current == null) {
            synchronized (MasterDataCatalog.class) {
                current = catalog;
                if (current == null) {
                    catalog = current = loadCatalog();
                }
            }
        }
        return current;
    }

    public static String[] getLoaiDon() { return requireNonEmpty("loaiDon"); }

    // Fix E: Dùng chung lõi getAllLoaiDonViecPairs() để đảm bảo data được parse đồng nhất
    public static String[] getLoaiDonViecPairs() {
        List<String[]> pairs = getAllLoaiDonViecPairs();
        String[] result = new String[pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
            result[i] = pairs.get(i)[0] + ">" + pairs.get(i)[1];
        }
        return result;
    }

    public static String[] getLoaiViecByLoaiDon(String loaiDon) {
        List<String> values = new ArrayList<>();
        for (String[] pair : loaiDonViecPairs()) {
            if (pair[0].equals(loaiDon) && !values.contains(pair[1])) {
                values.add(pair[1]);
            }
        }
        if (values.isEmpty()) {
            throw new IllegalStateException("❌ Danh mục thiếu loại việc cho [" + loaiDon + "].");
        }
        return values.toArray(new String[0]);
    }

    public static String[] getToaAn() { return requireNonEmpty("toaAn"); }
    public static String[] getLoaiChuTheNguyenDon() { return requireNonEmpty("loaiChuTheNguyenDon"); }
    public static String[] getLoaiChuTheBiDon() { return requireNonEmpty("loaiChuTheBiDon"); }
    public static String[] getLoaiHinhToChuc() { return requireNonEmpty("loaiHinhToChuc"); }
    public static String[] getGioiTinh() { return requireNonEmpty("gioiTinh"); }
    public static String[] getNoiCapCccd() { return requireNonEmpty("noiCapCccd"); }
    public static String[] getCoKhong() { return requireNonEmpty("coKhong"); }
    public static String[] getQuanHeDaiDien() { return requireNonEmpty("quanHeDaiDien"); }

    /** Tư cách người nộp đơn — chỉ dùng cho loại đơn Phá sản. */
    public static String[] getTuCachNopDonPhaSan() {
        String[] values = catalog().get("tuCachNopDonPhaSan");
        if (values == null || values.length == 0) {
            return new String[]{"Chủ nợ", "Người lao động", "DN / HTX tự nộp", "Cổ đông – thành viên HTX"};
        }
        return values;
    }

    public static String pick(String[] options, int index) {
        if (options == null || options.length == 0) {
            throw new IllegalStateException("❌ Danh mục rỗng, không thể chọn giá trị.");
        }
        return options[Math.floorMod(index, options.length)];
    }

    public static void assertInCatalog(String value, String fieldName, String[] allowed) {
        if (value == null || value.trim().isEmpty()) return;
        for (String option : allowed) {
            if (option.equals(value)) return;
        }
        throw new IllegalStateException("❌ Dữ liệu [" + value + "] không thuộc vùng hợp lệ của [" + fieldName + "]. Giá trị cho phép: " + String.join(", ", allowed));
    }

    /**
     * Số kịch bản Full pairwise mức B (delegate {@link FullCoverageMatrix}).
     * Catalog phụ (tòa, giới tính…) chỉ xoay theo seed — không nhân tổ hợp.
     */
    public static int getMinimumCoverageRowCount() {
        return FullCoverageMatrix.expectedRowCount();
    }

    public static List<String[]> getAllLoaiDonViecPairs() {
        List<String[]> pairs = loaiDonViecPairs();
        if (pairs.isEmpty()) throw new IllegalStateException("❌ Không có cặp (loaiDon, loaiViec) hợp lệ.");
        return pairs;
    }

    public static String[] getLoaiDonViecPair(int index) {
        List<String[]> pairs = getAllLoaiDonViecPairs();
        return pairs.get(Math.floorMod(index, pairs.size()));
    }

    public static void saveToWorkspace(Path targetFile, Map<String, List<String>> data) throws IOException {
        Path parent = targetFile.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (var writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
            writer.write("# Auto-generated by MasterDataSyncTest\n");
            List<String> pairLines = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(LOAI_VIEC_PREFIX)) {
                    String loaiDon = key.substring(LOAI_VIEC_PREFIX.length());
                    for (String loaiViec : entry.getValue()) pairLines.add(loaiDon + ">" + loaiViec);
                    continue;
                }
                if ("loaiDonViecPairs".equals(key)) continue;
                if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
                List<String> values = entry.getValue();
                if ("toaAn".equals(key)) {
                    values = ToaAnCatalog.filterForAutomation(values);
                }
                writer.write(key + "=" + String.join("|", values) + "\n");
            }
            if (!pairLines.isEmpty()) writer.write("loaiDonViecPairs=" + String.join(";", pairLines) + "\n");
        }
    }

    private static List<String[]> loaiDonViecPairs() {
        List<String[]> cached = loaiDonViecPairs;
        if (cached == null) {
            synchronized (MasterDataCatalog.class) {
                cached = loaiDonViecPairs;
                if (cached == null) loaiDonViecPairs = cached = parseLoaiDonViecPairs(catalog());
            }
        }
        return cached;
    }

    private static List<String[]> parseLoaiDonViecPairs(Map<String, String[]> data) {
        List<String[]> pairs = new ArrayList<>();
        String[] rawPairs = data.get("loaiDonViecPairs");
        if (rawPairs != null && rawPairs.length == 1 && !rawPairs[0].isBlank()) {
            for (String segment : rawPairs[0].split(";")) addPair(pairs, segment);
        }
        if (pairs.isEmpty()) {
            for (String key : data.keySet()) {
                if (!key.startsWith(LOAI_VIEC_PREFIX)) continue;
                String loaiDon = key.substring(LOAI_VIEC_PREFIX.length()).trim();
                for (String loaiViec : data.get(key)) {
                    if (isProductionOption(loaiViec)) {
                        pairs.add(new String[]{loaiDon, loaiViec});
                    }
                }
            }
        }
        return Collections.unmodifiableList(pairs);
    }

    private static void addPair(List<String[]> pairs, String segment) {
        String[] parts = segment.split(">", 2);
        if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()
                && isProductionOption(parts[1].trim())) {
            pairs.add(new String[]{parts[0].trim(), parts[1].trim()});
        }
    }

    public static Path getWorkspaceCatalogPath() {
        return Paths.get("src", "main", "resources", RESOURCE_PATH);
    }

    private static String[] requireNonEmpty(String key) {
        String[] values = catalog().get(key);
        if (values == null || values.length == 0) throw new IllegalStateException("❌ Danh mục thiếu khóa [" + key + "].");
        return values;
    }

    private static Map<String, String[]> loadCatalog() {
        Properties props = new Properties();
        try (InputStream in = MasterDataCatalog.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) throw new IllegalStateException("Không tìm thấy " + RESOURCE_PATH);
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được " + RESOURCE_PATH, e);
        }

        Map<String, String[]> loaded = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            if ("loaiDonViecPairs".equals(key)) {
                loaded.put(key, new String[]{props.getProperty(key, "").trim()});
            } else if ("toaAn".equals(key)) {
                loaded.put(key, normalizeToaAn(splitValues(props.getProperty(key))));
            } else {
                loaded.put(key, splitValues(props.getProperty(key)));
            }
        }
        return Collections.unmodifiableMap(loaded);
    }

    private static String[] splitValues(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new String[0];
        String[] parts = raw.split("\\|");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && isProductionOption(trimmed)) values.add(trimmed);
        }
        return values.toArray(new String[0]);
    }

    private static String[] normalizeToaAn(String[] raw) {
        List<String> filtered = ToaAnCatalog.filterForAutomation(raw == null ? List.of() : List.of(raw));
        return filtered.toArray(new String[0]);
    }

    private static boolean isProductionOption(String value) {
        String lower = value.toLowerCase();
        return !lower.contains("test in don")
                && !lower.contains("fpt test")
                && !lower.contains("tam, xoa")
                && !lower.contains("xoa sau");
    }
}
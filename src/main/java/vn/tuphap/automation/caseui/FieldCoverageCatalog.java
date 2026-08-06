package vn.tuphap.automation.caseui;

import vn.tuphap.automation.config.RunFlowConfig.CaseProfile;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.TaoDonScenario;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bản đồ field ca âm theo bước và hình form.
 *
 * <p>Nguồn field duy nhất vẫn là {@link DataGenerator#TRUONG_LOI_HOP_LE}. Lớp này bổ sung nơi field
 * xuất hiện, ngữ cảnh CN/TC/Phá sản, và kiểm chứng bằng
 * {@link DataGenerator#tryFieldOverride(TaoDonScenario, String, String)} trước khi cho generator
 * dùng. CSV discovery chỉ làm giàu trạng thái/thông báo; không có CSV thì fallback vẫn hoạt động.
 */
public final class FieldCoverageCatalog {

    public enum Variant {
        B2_CA_NHAN("Nguyên đơn Cá nhân"),
        B2_TO_CHUC("Nguyên đơn Tổ chức"),
        B3_CA_NHAN("Bị đơn Cá nhân"),
        B3_TO_CHUC_PHA_SAN("Bị đơn Tổ chức (Phá sản)"),
        B4_EFORM("Eform iframe"),
        B4_TEXTAREA("Textarea legacy");

        private final String moTa;

        Variant(String moTa) {
            this.moTa = moTa;
        }

        public String moTa() {
            return moTa;
        }
    }

    public record Context(
            Variant variant,
            int buoc,
            String loaiDon,
            String loaiViec,
            String chuThe,
            String tuCachNopDon
    ) {
    }

    public record FieldCandidate(
            String field,
            int buoc,
            Context context,
            String giaTriLoi,
            boolean discoveryDaThay,
            boolean discoveryDaChan,
            String thongBaoMongDoi
    ) {
    }

    public record BaoCao(
            int tongFieldUngVien,
            int fieldDaCoCaAm,
            double phanTramPhu,
            int fieldDiscoveryDaThay,
            List<String> fieldChuaPhu
    ) {
    }

    private record FieldRule(String field, int buoc, Variant variant) {
    }

    private record DiscoveryObservation(boolean apDung, boolean biChan, String thongBao) {
    }

    private static final List<FieldRule> RULES = List.of(
            new FieldRule("Số điện thoại", 2, Variant.B2_CA_NHAN),
            new FieldRule("Email", 2, Variant.B2_CA_NHAN),
            new FieldRule("CCCD", 2, Variant.B2_CA_NHAN),
            new FieldRule("Ngày sinh", 2, Variant.B2_CA_NHAN),
            new FieldRule("Ngày cấp", 2, Variant.B2_CA_NHAN),
            new FieldRule("Họ tên", 2, Variant.B2_CA_NHAN),
            new FieldRule("Giới tính", 2, Variant.B2_CA_NHAN),
            new FieldRule("Địa chỉ thường trú", 2, Variant.B2_CA_NHAN),
            new FieldRule("Mã số thuế", 2, Variant.B2_TO_CHUC),

            new FieldRule("Số điện thoại (Bị đơn)", 3, Variant.B3_CA_NHAN),
            new FieldRule("Email (Bị đơn)", 3, Variant.B3_CA_NHAN),
            new FieldRule("CCCD (Bị đơn)", 3, Variant.B3_CA_NHAN),
            new FieldRule("Họ tên (Bị đơn)", 3, Variant.B3_CA_NHAN),
            new FieldRule("Nghề nghiệp (Bị đơn)", 3, Variant.B3_CA_NHAN),
            new FieldRule("Nơi ở hiện tại (Bị đơn)", 3, Variant.B3_CA_NHAN),
            // Schema CaseRow chưa chọn được loại bị đơn. Phá sản là luồng UI bảo đảm bị đơn là TC.
            new FieldRule("Mã số thuế (Bị đơn)", 3, Variant.B3_TO_CHUC_PHA_SAN),

            // Các field cố định này chỉ thuộc form legacy. Eform có schema động trong iframe nên
            // không được gắn nhầm override Java vào case eform.
            new FieldRule("Thời điểm phát sinh", 4, Variant.B4_TEXTAREA),
            new FieldRule("Giá trị tranh chấp", 4, Variant.B4_TEXTAREA),
            new FieldRule("Tóm tắt quá trình", 4, Variant.B4_TEXTAREA),
            new FieldRule("Yêu cầu cụ thể", 4, Variant.B4_TEXTAREA),
            new FieldRule("Căn cứ pháp lý", 4, Variant.B4_TEXTAREA)
    );

    private final List<FieldCandidate> candidates;
    private final String discoveryCsv;
    private final List<String> boQua;

    private FieldCoverageCatalog(List<FieldCandidate> candidates, String discoveryCsv,
                                 List<String> boQua) {
        this.candidates = List.copyOf(candidates);
        this.discoveryCsv = discoveryCsv == null ? "" : discoveryCsv;
        this.boQua = List.copyOf(boQua);
    }

    public static FieldCoverageCatalog loadLatest(Path discoveryDir) {
        Optional<Path> newest = timCsvMoiNhat(discoveryDir);
        return newest.map(FieldCoverageCatalog::fromCsv).orElseGet(FieldCoverageCatalog::fallback);
    }

    public static FieldCoverageCatalog fallback() {
        return build(Map.of(), "");
    }

    public static FieldCoverageCatalog fromCsv(Path csv) {
        if (csv == null || !Files.isRegularFile(csv)) {
            return fallback();
        }
        try {
            Map<String, DiscoveryObservation> observations = parseDiscovery(csv);
            return build(observations, csv.toString().replace('\\', '/'));
        } catch (IOException e) {
            return fallback();
        }
    }

    public List<FieldCandidate> fieldsForStep(int buoc) {
        return candidates.stream().filter(c -> c.buoc() == buoc).toList();
    }

    public List<FieldCandidate> fieldsForVariant(Variant variant) {
        return candidates.stream().filter(c -> c.context().variant() == variant).toList();
    }

    public List<FieldCandidate> candidates() {
        return candidates;
    }

    public String discoveryCsv() {
        return discoveryCsv;
    }

    public List<String> boQua() {
        return boQua;
    }

    public BaoCao baoCao(Set<String> generatedFields) {
        Set<String> ungVien = new LinkedHashSet<>();
        int discovery = 0;
        for (FieldCandidate candidate : candidates) {
            ungVien.add(candidate.field());
            if (candidate.discoveryDaThay()) {
                discovery++;
            }
        }
        Set<String> daPhu = new LinkedHashSet<>();
        for (String field : generatedFields == null ? Set.<String>of() : generatedFields) {
            String canonical = canonicalWhitelist(field);
            if (canonical != null && ungVien.contains(canonical)) {
                daPhu.add(canonical);
            }
        }
        List<String> chuaPhu = ungVien.stream().filter(f -> !daPhu.contains(f)).toList();
        double pct = ungVien.isEmpty() ? 100.0 : daPhu.size() * 100.0 / ungVien.size();
        return new BaoCao(ungVien.size(), daPhu.size(), Math.round(pct * 10.0) / 10.0,
                discovery, chuaPhu);
    }

    private static FieldCoverageCatalog build(Map<String, DiscoveryObservation> observations,
                                              String source) {
        List<FieldCandidate> out = new ArrayList<>();
        List<String> boQua = new ArrayList<>();
        Set<String> whitelist = new LinkedHashSet<>(DataGenerator.TRUONG_LOI_HOP_LE);
        for (FieldRule rule : RULES) {
            if (!whitelist.contains(rule.field())) {
                boQua.add(rule.field() + ": không còn trong whitelist");
                continue;
            }
            Context context = context(rule.variant());
            String value = giaTriLoi(rule.field());
            TaoDonScenario scenario = scenario(context);
            DataGenerator.FieldOverrideAttempt attempt =
                    DataGenerator.tryFieldOverride(scenario, rule.field(), value);
            if (!attempt.applicable()) {
                boQua.add(rule.field() + " / " + context.variant().moTa() + ": "
                        + attempt.skipReason());
                continue;
            }
            DiscoveryObservation observation = observations.get(normalize(rule.field()));
            out.add(new FieldCandidate(rule.field(), rule.buoc(), context, value,
                    observation != null && observation.apDung(),
                    observation != null && observation.biChan(),
                    observation == null ? "" : observation.thongBao()));
        }
        return new FieldCoverageCatalog(out, source, boQua);
    }

    private static Context context(Variant variant) {
        return switch (variant) {
            case B2_CA_NHAN -> new Context(variant, 2, "Dân sự", "Hợp đồng dân sự",
                    "Cá nhân", "");
            case B2_TO_CHUC -> new Context(variant, 2, "Dân sự", "Hợp đồng dân sự",
                    "Tổ chức / Doanh nghiệp", "");
            case B3_CA_NHAN -> new Context(variant, 3, "Dân sự", "Hợp đồng dân sự",
                    "Cá nhân", "");
            case B3_TO_CHUC_PHA_SAN -> new Context(variant, 3, "Phá sản", "",
                    "Cá nhân", "");
            case B4_EFORM -> new Context(variant, 4, "Dân sự",
                    "Bồi thường thiệt hại ngoài hợp đồng", "Cá nhân", "");
            case B4_TEXTAREA -> new Context(variant, 4, "Dân sự",
                    "Hợp đồng dân sự", "Cá nhân", "");
        };
    }

    private static TaoDonScenario scenario(Context context) {
        CaseProfile profile = new CaseProfile(context.loaiDon(), context.loaiViec(),
                context.chuThe(), "", 4, false);
        Object[][] generated = DataGenerator.generateConfiguredCases(List.of(profile));
        if (generated.length == 0) {
            throw new IllegalStateException("Không sinh được baseline cho " + context.variant());
        }
        TaoDonScenario scenario = (TaoDonScenario) generated[0][0];
        if (context.variant() == Variant.B3_CA_NHAN) {
            return scenario.toBuilder().loaiBiDon("Cá nhân").build();
        }
        return scenario;
    }

    private static Map<String, DiscoveryObservation> parseDiscovery(Path csv) throws IOException {
        Map<String, DiscoveryObservation> out = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            List<String> cols = parseCsvLine(lines.get(i));
            if (cols.size() < 7) {
                continue;
            }
            String canonical = canonicalWhitelist(cols.get(0));
            if (canonical == null) {
                continue;
            }
            boolean apDung = "Có".equalsIgnoreCase(cols.get(4).trim());
            boolean biChan = "Có".equalsIgnoreCase(cols.get(5).trim());
            String message = cols.get(6).trim();
            if (message.toUpperCase(Locale.ROOT).startsWith("LỖI HẠ TẦNG")) {
                message = "";
                biChan = false;
            }
            DiscoveryObservation current = out.get(normalize(canonical));
            DiscoveryObservation next = new DiscoveryObservation(apDung, biChan,
                    biChan ? message : "");
            if (current == null || (!current.biChan() && next.biChan())
                    || (!current.apDung() && next.apDung())) {
                out.put(normalize(canonical), next);
            }
        }
        return out;
    }

    static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) {
            return out;
        }
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(current.toString());
        return out;
    }

    private static String canonicalWhitelist(String raw) {
        String normalized = normalize(raw)
                .replace("nguyen don", "")
                .replaceAll("\\s+", " ")
                .trim();
        for (String allowed : DataGenerator.TRUONG_LOI_HOP_LE) {
            if (normalize(allowed).equals(normalized)) {
                return allowed;
            }
        }
        return null;
    }

    private static String normalize(String raw) {
        return Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[()]+", " ")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private static String giaTriLoi(String field) {
        String n = normalize(field);
        if (n.contains("email")) return "khong-phai-email";
        if (n.contains("dien thoai")) return "abc";
        if (n.contains("cccd")) return "123";
        if (n.contains("ngay") || n.contains("thoi diem")) return "31/13/2024";
        if (n.contains("gia tri")) return "-1000000";
        if (n.contains("ma so thue")) return "abc";
        return "";
    }

    private static Optional<Path> timCsvMoiNhat(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return Optional.empty();
        }
        Path best = null;
        long bestTime = Long.MIN_VALUE;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "field-discovery_*.csv")) {
            for (Path file : files) {
                long time = Files.getLastModifiedTime(file).toMillis();
                if (time > bestTime) {
                    best = file;
                    bestTime = time;
                }
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.ofNullable(best);
    }
}

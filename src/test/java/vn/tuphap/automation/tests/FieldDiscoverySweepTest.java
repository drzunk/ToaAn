package vn.tuphap.automation.tests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import vn.tuphap.automation.config.RunFlowConfig.CaseProfile;
import vn.tuphap.automation.core.TaoDonBaseTest;
import vn.tuphap.automation.data.DataGenerator;
import vn.tuphap.automation.data.DataGenerator.FieldOverrideAttempt;
import vn.tuphap.automation.data.TaoDonScenario;
import vn.tuphap.automation.flow.StepBlockedException;
import vn.tuphap.automation.flow.TaoDonFlow;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bộ quét dò (discovery sweep) — Giai đoạn 1 của việc mở rộng ca âm ra nhiều field.
 * <p>
 * Với mỗi (field, giá trị sai): dựng 1 scenario hợp lệ, ép đúng field đó sai qua
 * {@link DataGenerator#tryFieldOverride}, điền các bước hợp lệ trước bước chứa field, rồi thử
 * chuyển sang bước đó — ghi lại hệ thống có chặn hay không và thông báo gì.
 * <p>
 * <b>Không assert gì</b> — đây là công cụ quan sát, không phải test pass/fail. Mọi kết quả (kể cả
 * lỗi hạ tầng) được gom vào {@link #RESULTS} rồi ghi ra 1 file CSV ở {@code @AfterClass}. Người đọc
 * báo cáo tự quyết field nào cần trở thành ca âm thật (điền vào cột "Trường lỗi" trên Google Sheet).
 * <p>
 * Chạy riêng, không nằm trong smoke/mid/full/master: {@code mvn -Pdiscovery test}.
 */
public class FieldDiscoverySweepTest extends TaoDonBaseTest {

    private static final List<String[]> RESULTS = Collections.synchronizedList(new ArrayList<>());

    /** 1 field cần quét: nhãn hiển thị (khớp "Trường lỗi" trên sheet), bước chứa field, baseline dùng. */
    private record FieldSpec(String label, int homeStep, String baseline, List<String> bienThe) {
    }

    private static final String CN = "CN";
    private static final String TC = "TC";
    /**
     * Riêng cho "Mã số thuế (Bị đơn)": catalog {@code loaiChuTheBiDon} chỉ có 1 giá trị "Cơ quan"
     * (xem {@code master-data.properties}), nên {@code CaseProfile} không có cách nào ép bị đơn
     * thành Tổ chức cho loại đơn thường qua {@code generateConfiguredCases}. Chỉ Phá sản mới ép
     * bị đơn thành Tổ chức vô điều kiện ({@code DataDictionary.isPhaSan} trong
     * {@code DataGenerator.tryFieldOverride}) — dùng baseline này thay vì "TC".
     */
    private static final String PHA_SAN = "PHA_SAN";

    private static final List<FieldSpec> CATALOG = List.of(
            new FieldSpec("Số điện thoại (Nguyên đơn)", 2, CN, List.of("(để trống)", "abc", "0123")),
            new FieldSpec("Email (Nguyên đơn)", 2, CN, List.of("(để trống)", "khong-phai-email")),
            new FieldSpec("CCCD (Nguyên đơn)", 2, CN, List.of("(để trống)", "123")),
            new FieldSpec("Họ tên (Nguyên đơn)", 2, CN, List.of("(để trống)", "<script>alert(1)</script>")),
            new FieldSpec("Ngày sinh (Nguyên đơn)", 2, CN, List.of("(để trống)", "31/13/2024")),
            new FieldSpec("Ngày cấp CCCD (Nguyên đơn)", 2, CN, List.of("(để trống)", "khong-phai-ngay")),
            new FieldSpec("Mã số thuế (Nguyên đơn)", 2, TC, List.of("(để trống)", "abc")),
            new FieldSpec("Số điện thoại (Bị đơn)", 3, CN, List.of("(để trống)", "abc")),
            new FieldSpec("Email (Bị đơn)", 3, CN, List.of("(để trống)", "khong-phai-email")),
            new FieldSpec("CCCD (Bị đơn)", 3, CN, List.of("(để trống)", "123")),
            new FieldSpec("Họ tên (Bị đơn)", 3, CN, List.of("(để trống)", "<script>alert(1)</script>")),
            new FieldSpec("Mã số thuế (Bị đơn)", 3, PHA_SAN, List.of("(để trống)", "abc")),
            new FieldSpec("Giá trị tranh chấp", 4, CN, List.of("(để trống)", "-1000000", "khong-phai-so"))
    );

    @DataProvider(name = "FieldProbes")
    public Object[][] fieldProbes() {
        List<Object[]> rows = new ArrayList<>();
        for (FieldSpec spec : CATALOG) {
            for (String bienThe : spec.bienThe()) {
                String value = "(để trống)".equals(bienThe) ? "" : bienThe;
                rows.add(new Object[]{spec, bienThe, value});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "FieldProbes", groups = "discovery",
            description = "Dò 1 field x 1 biến thể sai — không assert, chỉ ghi nhận kết quả")
    public void probeField(FieldSpec spec, String bienThe, String value) {
        TaoDonScenario baseline = resolveBaseline(spec.baseline());
        FieldOverrideAttempt attempt = DataGenerator.tryFieldOverride(baseline, spec.label(), value);

        if (!attempt.applicable()) {
            ghiKetQua(spec, bienThe, value, "Không", "N/A", attempt.skipReason());
            return;
        }

        TaoDonScenario corrupted = attempt.result();
        try {
            ensureDashboard();
            TaoDonFlow flow = new TaoDonFlow(getDriver(), getWebUI());
            flow.moFormNopDonMoi();
            flow.dienBuoc1(corrupted);
            if (spec.homeStep() >= 3) {
                flow.dienBuoc2(corrupted);
            }
            if (spec.homeStep() >= 4) {
                flow.dienBuoc3(corrupted);
            }

            try {
                switch (spec.homeStep()) {
                    case 2 -> flow.dienBuoc2(corrupted);
                    case 3 -> flow.dienBuoc3(corrupted);
                    case 4 -> flow.dienBuoc4(corrupted);
                    default -> throw new IllegalStateException("Bước không hỗ trợ: " + spec.homeStep());
                }
                ghiKetQua(spec, bienThe, value, "Có", "Không", "");
            } catch (StepBlockedException ex) {
                ghiKetQua(spec, bienThe, value, "Có", "Có", ex.systemMessage());
            }
        } catch (Exception ex) {
            ghiKetQua(spec, bienThe, value, "Có", "N/A",
                    "LỖI HẠ TẦNG (không phải hệ thống chặn): " + ex.getMessage());
        }
    }

    private void ghiKetQua(FieldSpec spec, String bienThe, String value,
                            String apDung, String biChan, String thongBao) {
        String ghiChu = "N/A".equals(biChan) && "Không".equals(apDung) ? "bỏ qua, không mở trình duyệt" : "";
        RESULTS.add(new String[]{
                spec.label(), bienThe, value, String.valueOf(spec.homeStep()), apDung, biChan,
                thongBao == null ? "" : thongBao, ghiChu});
        System.out.println("🔎 [" + spec.label() + " | " + bienThe + "] áp dụng=" + apDung
                + " chặn=" + biChan + (thongBao == null || thongBao.isBlank() ? "" : " — " + thongBao));
    }

    private TaoDonScenario resolveBaseline(String key) {
        return switch (key) {
            case TC -> baselineTC();
            case PHA_SAN -> baselinePhaSan();
            default -> baselineCN();
        };
    }

    private TaoDonScenario baselineCN() {
        return (TaoDonScenario) DataGenerator.generateConfiguredCases(
                List.of(new CaseProfile("Dân sự", "Hợp đồng dân sự", "CN", "", 6, false)))[0][0];
    }

    private TaoDonScenario baselineTC() {
        return (TaoDonScenario) DataGenerator.generateConfiguredCases(
                List.of(new CaseProfile("Dân sự", "Hợp đồng dân sự", "TC", "", 6, false)))[0][0];
    }

    /** Duy nhất loại đơn ép bị đơn thành Tổ chức vô điều kiện — xem javadoc hằng số {@link #PHA_SAN}. */
    private TaoDonScenario baselinePhaSan() {
        return (TaoDonScenario) DataGenerator.generateConfiguredCases(
                List.of(new CaseProfile("Phá sản", "", "CN", "", 6, false)))[0][0];
    }

    @AfterClass(alwaysRun = true)
    public void writeReport() {
        if (RESULTS.isEmpty()) {
            return;
        }
        // Ưu tiên lên đầu: "Áp dụng=Có, Bị chặn=Không" — đáng ngờ nhất (có thể là lỗ hổng validation).
        // Kế đến "Bị chặn=Có" (bình thường/đã có validation), cuối cùng "Áp dụng=Không" (bỏ qua).
        List<String[]> sorted = new ArrayList<>(RESULTS);
        sorted.sort(java.util.Comparator.comparingInt(FieldDiscoverySweepTest::doUuTien));

        StringBuilder csv = new StringBuilder();
        csv.append(csvRow("Trường lỗi", "Biến thể", "Giá trị", "Bước", "Áp dụng", "Bị chặn", "Thông báo", "Ghi chú"));
        for (String[] row : sorted) {
            csv.append(csvRow(row));
        }

        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path out = Path.of("test-output", "discovery-sweep", "field-discovery_" + stamp + ".csv");
        try {
            Files.createDirectories(out.getParent());
            // BOM UTF-8 để Excel tự nhận đúng bảng mã khi double-click mở trực tiếp (không có BOM
            // thì tiếng Việt hiển thị lỗi font, tester phải biết cách "Nhập dữ liệu > UTF-8" mới đọc được).
            byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            try (var os = Files.newOutputStream(out)) {
                os.write(bom);
                os.write(csv.toString().getBytes(StandardCharsets.UTF_8));
            }
            System.out.println("📄 Báo cáo quét dò (" + sorted.size() + " dòng, đáng ngờ nhất ở đầu): "
                    + out.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("⚠ Không ghi được báo cáo quét dò: " + e.getMessage());
        }
    }

    /** Áp dụng=Có & Bị chặn=Không xếp trước nhất — đây là dấu hiệu lỗ hổng validation cần xem ngay. */
    private static int doUuTien(String[] row) {
        String apDung = row[4];
        String biChan = row[5];
        if ("Có".equals(apDung) && "Không".equals(biChan)) {
            return 0;
        }
        if ("Có".equals(apDung) && "N/A".equals(biChan)) {
            return 1; // lỗi hạ tầng khi probe — cũng đáng xem
        }
        if ("Có".equals(apDung) && "Có".equals(biChan)) {
            return 2; // bình thường, hệ thống đã chặn đúng
        }
        return 3; // Áp dụng=Không — bị bỏ qua, ít quan trọng nhất
    }

    private static String csvRow(String... cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(csvEscape(cols[i]));
        }
        return sb.append('\n').toString();
    }

    private static String csvEscape(String raw) {
        String v = raw == null ? "" : raw;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}

package vn.tuphap.automation.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

/**
 * Trang báo cáo {@code test-output/index.html} — tự chứa, tiếng Việt, tích luỹ qua các lượt chạy.
 * <p>
 * <b>Mỗi lượt chạy lưu dữ liệu riêng ra {@code runs/&lt;mốc&gt;/bao-cao.json}, và trang index được
 * dựng lại toàn bộ từ các file đó.</b> Bản đầu tiên bê nguyên khối HTML của lượt trước sang file
 * mới — cách đó khiến chỉ cần đổi tên lớp CSS là mọi lượt cũ mất định dạng, và không bao giờ sửa
 * ngược được lịch sử. Giữ dữ liệu thay vì giữ HTML thì đổi giao diện bao nhiêu lần cũng được.
 * <p>
 * Bố cục theo đúng việc mà số liệu phải làm: số dẫn + hàng thẻ chỉ số cho lượt mới nhất, đường xu
 * hướng một chuỗi (không chú giải, chỉ dán nhãn điểm cuối), thanh phần-trên-tổng-thể dùng bảng màu
 * trạng thái <b>luôn kèm nhãn chữ</b>, rồi danh sách ba tầng lượt → kịch bản → bước.
 */
public final class BaoCaoHtml {

    private static final Path OUT = Paths.get("test-output");
    private static final Path HTML = OUT.resolve("index.html");
    private static final Path RUNS = OUT.resolve("runs");
    private static final String TEN_JSON = "bao-cao.json";

    /** Số lượt vẽ trên biểu đồ xu hướng — nhiều hơn thì trục hoành thành cháo. */
    private static final int SO_LUOT_TREN_BIEU_DO = 20;

    /**
     * Số lượt gần nhất được dựng <b>đầy đủ</b> (kịch bản, bước, ảnh). Lượt cũ hơn chỉ còn một dòng
     * tóm tắt.
     * <p>
     * Không giới hạn thì trang phình tuyến tính mãi: 7 lượt đã là 1,6 MB HTML, 50 lượt là ~11 MB,
     * và trang được <b>dựng lại toàn bộ sau mỗi lượt chạy</b> nên chi phí tích luỹ theo n². Đến một
     * ngày nào đó việc dựng báo cáo ném {@code OutOfMemoryError}, bị nuốt thành một dòng cảnh báo,
     * và báo cáo lặng lẽ ngừng cập nhật vĩnh viễn. Dữ liệu gốc không mất — vẫn ở {@code runs/}.
     */
    private static final int SO_LUOT_DUNG_CHI_TIET = 20;

    /**
     * Số thư mục lượt chạy giữ lại trên đĩa; lượt cũ hơn bị xoá sau mỗi lần chạy.
     * <p>
     * Đo thật: một lượt FULL tốn <b>36 MB</b> (2,6 MB JSON + 323 ảnh), MID <b>13 MB</b>, SMOKE
     * 1,4 MB. Chạy FULL hằng ngày mà không dọn thì khoảng <b>1 GB mỗi tháng</b> — nhưng chỗ đau
     * trước tiên không phải đĩa mà là {@link #docTatCa()}: nó phân tích lại JSON của <i>mọi</i>
     * lượt sau <i>mỗi</i> lượt chạy.
     * <p>
     * Đổi bằng {@code -Dtaodon.giuLuot=N} hoặc {@code run.keepRuns} trong
     * {@code run-flow.properties}. Đặt <b>0 để không bao giờ xoá</b>.
     */
    private static int soLuotGiuLai() {
        String v = System.getProperty("taodon.giuLuot",
                vn.tuphap.automation.config.RunFlowConfig.keepRuns());
        try {
            return Math.max(0, Integer.parseInt(v.trim()));
        } catch (RuntimeException e) {
            return 30;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BaoCaoHtml() {
    }

    /**
     * Dữ liệu một lượt chạy, đúng như ghi ra đĩa.
     *
     * @param wallMs      thời gian thực tế từ đầu tới cuối suite
     * @param tongCaseMs  tổng thời gian cộng dồn của các case — với chạy song song thì lớn hơn
     *                    {@code wallMs} vài lần, nên hai con số này <b>không được lẫn</b>
     */
    public record LuotChay(String moc, String iso, String bo,
                           int dat, int thatBai, int boQua,
                           long wallMs, long tongCaseMs,
                           List<BaoCaoData.CaseBaoCao> cases) {
        int tong() {
            return dat + thatBai + boQua;
        }

        double tiLe() {
            return tong() > 0 ? dat * 100.0 / tong() : 0;
        }

        /**
         * Thời điểm <b>bắt đầu</b> lượt chạy.
         * <p>
         * Ưu tiên {@code moc} vì đó chính là tên thư mục {@code runs/<mốc>/} — dòng tiêu đề và
         * liên kết ảnh ngay dưới nó buộc phải nói cùng một giờ. {@code iso} từng được ghi bằng
         * {@code LocalDateTime.now()} lúc <i>kết thúc</i> suite, lệch tới 18 phút so với thư mục:
         * người đọc thấy "09:41" rồi bấm vào link trỏ {@code runs/20260802_092345} và không ghép
         * được với log. Đọc từ {@code moc} sửa luôn cả các lượt đã lưu, không phải sửa dữ liệu.
         */
        LocalDateTime luc() {
            if (moc != null) {
                try {
                    return LocalDateTime.parse(moc, MOC_FMT);
                } catch (RuntimeException ignored) {
                    // Mốc không đúng khuôn — rơi xuống iso.
                }
            }
            try {
                return LocalDateTime.parse(iso);
            } catch (RuntimeException e) {
                return LocalDateTime.now();
            }
        }
    }

    /** Khuôn của tên thư mục lượt chạy: {@code 20260802_092345}. */
    private static final DateTimeFormatter MOC_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** Ghi dữ liệu lượt vừa xong rồi dựng lại toàn bộ trang. Không bao giờ ném. */
    public static void ghi(String moc, LocalDateTime luc, String bo,
                           int dat, int thatBai, int boQua, long wallMs,
                           List<BaoCaoData.CaseBaoCao> cases) {
        try {
            Files.createDirectories(OUT);
            long tongCase = 0;
            List<BaoCaoData.CaseBaoCao> ds = cases == null ? List.of() : cases;
            for (BaoCaoData.CaseBaoCao c : ds) {
                tongCase += c.thoiGianMs();
            }
            LuotChay luot = new LuotChay(moc, luc.toString(), bo, dat, thatBai, boQua,
                    wallMs, tongCase, ds);
            Path thuMuc = RUNS.resolve(moc);
            Files.createDirectories(thuMuc);
            Files.writeString(thuMuc.resolve(TEN_JSON), GSON.toJson(luot), StandardCharsets.UTF_8);
            donLuotCu();
            dungLaiTrang();
        } catch (Exception e) {
            System.out.println(" ⚠ Không ghi được báo cáo index.html: " + e.getMessage());
        }
    }

    /**
     * Xoá các thư mục lượt chạy cũ, chỉ giữ {@link #soLuotGiuLai()} lượt gần nhất.
     * <p>
     * Đây là code <b>xoá dữ liệu</b> nên mọi điều kiện đều phải thoả, không có "hoặc":
     * <ol>
     *   <li>Chức năng được bật ({@code giu > 0}) — đặt 0 là không bao giờ xoá gì;</li>
     *   <li>đúng là thư mục con trực tiếp của {@code test-output/runs/};</li>
     *   <li>tên khớp <b>chính xác</b> khuôn mốc {@code yyyyMMdd_HHmmss} — nhờ vậy mọi thứ khác nằm
     *       lẫn trong đó (kể cả các file {@code run_*.log}) không bao giờ bị đụng tới;</li>
     *   <li>bên trong có {@code bao-cao.json} — tức nó thật sự là một lượt chạy đã ghi xong.</li>
     * </ol>
     * Và in ra từng thư mục bị xoá. Lần trước tôi dọn bằng cách so tên theo phỏng đoán và xoá nhầm
     * cả những lượt chạy thật — nên ở đây không đoán, chỉ khớp khuôn.
     */
    private static void donLuotCu() {
        int giu = soLuotGiuLai();
        if (giu <= 0 || !Files.isDirectory(RUNS)) {
            return;
        }
        List<Path> luot = new ArrayList<>();
        try (var ds = Files.list(RUNS)) {
            for (Path d : ds.toList()) {
                if (!Files.isDirectory(d)) {
                    continue;
                }
                String ten = d.getFileName().toString();
                if (!ten.matches("\\d{8}_\\d{6}") || !Files.isRegularFile(d.resolve(TEN_JSON))) {
                    continue;
                }
                luot.add(d);
            }
        } catch (Exception e) {
            return;
        }
        if (luot.size() <= giu) {
            return;
        }
        // Tên thư mục là yyyyMMdd_HHmmss nên so chuỗi chính là so thời gian.
        luot.sort(Comparator.comparing(p -> p.getFileName().toString()));
        for (Path cu : luot.subList(0, luot.size() - giu)) {
            try (var duyet = Files.walk(cu)) {
                duyet.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                        // Một file khoá không được làm hỏng cả lượt dọn.
                    }
                });
                System.out.println(" 🧹 Dọn lượt chạy cũ (giữ " + giu + " lượt gần nhất): "
                        + cu.getFileName());
            } catch (Exception e) {
                System.out.println(" ⚠ Không dọn được " + cu.getFileName() + ": " + e.getMessage());
            }
        }
    }

    /** Đọc lại mọi lượt đã lưu và vẽ lại {@code index.html} từ đầu. */
    public static void dungLaiTrang() {
        List<LuotChay> tatCa = docTatCa();
        try {
            Files.writeString(HTML, trang(tatCa), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println(" ⚠ Không ghi được index.html: " + e.getMessage());
        }
    }

    private static List<LuotChay> docTatCa() {
        List<LuotChay> out = new ArrayList<>();
        hong.clear();
        if (!Files.isDirectory(RUNS)) {
            return out;
        }
        try (var ds = Files.list(RUNS)) {
            for (Path d : ds.toList()) {
                Path f = d.resolve(TEN_JSON);
                if (!Files.isRegularFile(f)) {
                    continue;
                }
                try {
                    LuotChay l = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), LuotChay.class);
                    // `iso` cũng phải khác null: nó là khoá sắp xếp, và Comparator.comparing sẽ
                    // ném NPE ở tận dòng sort — nơi không ai bắt — làm cả trang không được ghi lại.
                    if (l != null && l.moc() != null && l.iso() != null) {
                        out.add(l);
                    } else {
                        hong.add(d.getFileName().toString());
                    }
                } catch (Exception e) {
                    // Một file hỏng không được làm chết cả trang báo cáo — nhưng cũng KHÔNG được
                    // biến mất im lặng: lượt chạy đó sẽ vắng khỏi danh sách, khỏi biểu đồ và khỏi
                    // phép so với lượt trước, mà không dòng nào trên trang nói là có thứ bị mất.
                    hong.add(d.getFileName().toString());
                }
            }
        } catch (Exception ignored) {
        }
        // Sắp theo THỜI ĐIỂM đã phân tích, không so chuỗi `iso`. So chuỗi đúng với mọi giá trị do
        // LocalDateTime.toString() sinh ra, nhưng chỉ cần một file lưu khuôn khác (khoảng trắng
        // thay chữ T) là mọi bản ghi đó dồn hết lên đầu bất kể ngày. Thêm `moc` làm khoá phụ để
        // hai lượt trùng giờ không bị xếp theo thứ tự hệ tập tin — thứ tự đó quyết định lượt nào
        // là "lượt trước", tức quyết định nhãn Mới hỏng / Đã sửa.
        out.sort(Comparator.comparing(LuotChay::luc).thenComparing(l -> nz(l.moc())));
        // Lượt cũ ngoài ngưỡng chi tiết chỉ được vẽ thành một dòng tóm tắt, nên không cần giữ đồ
        // thị kịch bản của chúng trong bộ nhớ. Một lượt FULL là 2,6 MB JSON; giữ cả 100 lượt là
        // vài trăm MB sống suốt lúc dựng trang, đủ để OutOfMemoryError nuốt cả báo cáo.
        for (int i = 0; i < out.size() - SO_LUOT_DUNG_CHI_TIET; i++) {
            LuotChay l = out.get(i);
            out.set(i, new LuotChay(l.moc(), l.iso(), l.bo(), l.dat(), l.thatBai(), l.boQua(),
                    l.wallMs(), l.tongCaseMs(), List.of()));
        }
        return out;
    }

    /** Tên các thư mục lượt chạy đọc không được — hiện thành một dòng cảnh báo trên trang. */
    private static final List<String> hong = Collections.synchronizedList(new ArrayList<>());

    // ── Phân tích: gom nhóm lỗi & so với lượt trước ────────────────────────

    /**
     * Chuẩn hoá thông báo lỗi để gom nhóm: bỏ mã lỗi GUID, số dài và khoảng trắng thừa.
     * <p>
     * 39 case cùng hỏng vì một nguyên nhân nhưng mỗi case một mã lỗi khác nhau — không chuẩn hoá
     * thì gom ra 39 nhóm, tức là không gom được gì.
     */
    static String chuanHoaLoi(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw
                .replaceAll("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b", "")
                // Neo vào cụm "Mã lỗi: <giá trị>" — bản cũ xoá cụm "mã lỗi" ở BẤT KỲ đâu, nên
                // "Hệ thống không trả về mã lỗi nào" thành "Hệ thống không trả về nào".
                .replaceAll("(?i)Mã lỗi:?\\s*\\S*", "")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*·\\s*$", "")
                .trim();
        // KHÔNG xoá mọi số ≥4 chữ số nữa. Bản cũ biến "Mã lỗi: 40012" thành chuỗi rỗng, và rỗng
        // thì bị gán nhãn "Không ghi nhận thông báo cụ thể" — nói dối hai lần: thông báo có đủ, và
        // hai mã khác nhau bị gộp làm một. Còn "Hết thời gian chờ sau 30000 ms" với "… 60000 ms"
        // là hai vấn đề khác nhau, không được gộp.
        return s;
    }

    /**
     * Dọn thông báo lỗi cho <b>người đọc báo cáo</b>, không phải cho lập trình viên.
     * <p>
     * Ba thứ rác bám vào thông báo trên đường đi:
     * <ul>
     *   <li>{@code "Kịch bản thất bại. Chi tiết lỗi: "} — huy hiệu "Thất bại" ngay bên cạnh đã nói
     *       điều đó rồi;</li>
     *   <li>{@code "Sao chép mã"} — <b>chữ trên nút bấm</b> của chính ứng dụng, bị cào vào cùng
     *       thông báo lỗi khi đọc toast. Nó khiến câu lỗi kết thúc bằng một mệnh lệnh vô nghĩa;</li>
     *   <li>đường dẫn URL đầy đủ nhét giữa câu.</li>
     * </ul>
     * <b>Giữ lại mã lỗi</b>: người đọc không cần nó, nhưng khi báo sang đội ứng dụng thì đó là thứ
     * duy nhất tra được. Dọn ở khâu hiển thị nên dữ liệu cũ cũng sạch theo.
     */
    static String loiChoNguoiDoc(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replaceAll("^\\s*Kịch bản thất bại\\.\\s*Chi tiết lỗi:\\s*", "")
                .replaceAll("\\s*Sao chép mã\\s*", " ")
                // Gỡ cả dấu phân cách đứng trước URL. Gỡ mỗi URL thì còn lại một dấu "·" mồ côi
                // giữa câu — trông như thiếu chữ, còn khó chịu hơn để nguyên.
                .replaceAll("\\s*[·—]?\\s*https?://\\S+", "")
                // Hai vệt của các lượt chạy lưu trước đây: một câu chỉ người đọc đi mở file Excel
                // không còn tồn tại, và một hậu tố đánh số thừa khi chỉ có đúng một ảnh. Gỡ ở khâu
                // HIỂN THỊ, không sửa file dữ liệu — lịch sử vẫn nguyên vẹn.
                .replaceAll("\\s*[—-]\\s*chi tiết dữ liệu xem file báo cáo Excel\\.?", "")
                .replaceAll("\\s*[—-]\\s*Toàn bộ khung nhìn hiện tại\\s*\\(1/1\\)", "")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*[·—-]\\s*$", "")
                .trim();
    }

    /**
     * Lý do một kịch bản không đạt — sự kiện lỗi cuối cùng, hoặc lý do bỏ qua.
     * <p>
     * Phải xét cả {@code MUC_SKIP}: một lượt hỏng từ khâu đăng nhập thì mọi kịch bản đều bị TestNG
     * bỏ qua và <i>không có</i> sự kiện lỗi nào. Bỏ sót nhánh này thì cả 39 kịch bản hiện chung một
     * dòng "Không ghi nhận thông báo cụ thể", trong khi lý do thật ("Không nhận được phản hồi từ
     * trang web trong thời gian chờ") nằm ngay đó.
     */
    private static String lyDoHong(BaoCaoData.CaseBaoCao c) {
        // Mọi trường của record đều có thể null: Gson đặt null cho trường thiếu, và file lưu bằng
        // phiên bản record cũ thì thiếu thật. Một NPE ở đây làm cả trang không được ghi lại — người
        // đọc mở báo cáo thấy dữ liệu hôm qua mà không có dấu hiệu gì.
        String cuoi = "";
        for (BaoCaoData.SuKien sk : noiDs(c.suKienDau(), c.suKienCuoi())) {
            if (sk != null
                    && (BaoCaoData.MUC_FAIL.equals(sk.muc()) || BaoCaoData.MUC_SKIP.equals(sk.muc()))) {
                cuoi = nz(sk.noiDung());
            }
        }
        if (cuoi.isEmpty() && c.buoc() != null) {
            for (BaoCaoData.BuocBaoCao b : c.buoc()) {
                if (b == null || b.suKien() == null) {
                    continue;
                }
                for (BaoCaoData.SuKien sk : b.suKien()) {
                    if (sk != null && (BaoCaoData.MUC_FAIL.equals(sk.muc())
                            || BaoCaoData.MUC_SKIP.equals(sk.muc()))) {
                        cuoi = nz(sk.noiDung());
                    }
                }
            }
        }
        return loiChoNguoiDoc(cuoi);
    }

    private static List<BaoCaoData.SuKien> noiDs(List<BaoCaoData.SuKien> a, List<BaoCaoData.SuKien> b) {
        List<BaoCaoData.SuKien> out = new ArrayList<>();
        if (a != null) {
            out.addAll(a);
        }
        if (b != null) {
            out.addAll(b);
        }
        return out;
    }

    /**
     * Gom các kịch bản không đạt theo lý do.
     * <p>
     * <b>Tách hẳn "thất bại" khỏi "bỏ qua".</b> Gộp chung là nói sai bản chất: một lượt hỏng ở khâu
     * đăng nhập khiến 39 kịch bản bị bỏ qua sẽ hiện thành "39 lỗi" dưới tiêu đề <i>Các lỗi thường
     * gặp</i> — người đọc kết luận hệ thống hỏng 39 chỗ, trong khi thực tế là chưa chạy được chỗ nào.
     */
    private static String bangGomLoi(LuotChay d) {
        return khoiGom(d, TrangThai.BO_QUA, "Vì sao các kịch bản bị bỏ qua")
                + khoiGom(d, TrangThai.THAT_BAI, "Các lỗi thường gặp");
    }

    /** Một nhóm lỗi: khoá gom nhóm, câu <b>nguyên văn</b> để hiện, và các mã case thuộc nhóm. */
    private record NhomLoi(String nguyenVan, List<String> maCase) {
    }

    /** @param chiTrangThai chỉ gom các kịch bản mang trạng thái này */
    private static String khoiGom(LuotChay d, String chiTrangThai, String tieuDe) {
        Map<String, NhomLoi> nhom = new LinkedHashMap<>();
        for (BaoCaoData.CaseBaoCao c : d.cases()) {
            if (c == null || !chiTrangThai.equalsIgnoreCase(nz(c.trangThai()).trim())) {
                continue;
            }
            String nguyenVan = lyDoHong(c);
            String key = chuanHoaLoi(nguyenVan);
            if (key.isBlank()) {
                key = "Không ghi nhận thông báo cụ thể";
                nguyenVan = nguyenVan.isBlank() ? key : nguyenVan;
            }
            // Gom bằng bản đã chuẩn hoá, nhưng HIỆN và CHÉP bản nguyên văn của case đầu nhóm.
            // Bản cũ hiện luôn bản chuẩn hoá — tức đã bị xoá mã lỗi — nên nút "Chép thông báo lỗi"
            // đưa cho đội ứng dụng một câu không tra được, đúng thứ mà javadoc bên trên hứa giữ.
            nhom.computeIfAbsent(key, k -> new NhomLoi(k, new ArrayList<>()));
            NhomLoi n = nhom.get(key);
            if (n.maCase().isEmpty() && !nguyenVan.isBlank()) {
                nhom.put(key, new NhomLoi(nguyenVan, n.maCase()));
            }
            nhom.get(key).maCase().add(nz(c.maCase()).isBlank() ? "—" : c.maCase());
        }
        if (nhom.isEmpty()) {
            return "";
        }
        List<Map.Entry<String, NhomLoi>> rows = new ArrayList<>(nhom.entrySet());
        rows.sort((x, y) -> Integer.compare(y.getValue().maCase().size(), x.getValue().maCase().size()));
        String lop = TrangThai.BO_QUA.equals(chiTrangThai) ? "gomloi gom-bo" : "gomloi";
        StringBuilder sb = new StringBuilder("<div class=\"").append(lop)
                .append("\"><div class=\"gomloi-dau\">").append(esc(tieuDe)).append("</div>");
        for (Map.Entry<String, NhomLoi> e : rows) {
            List<String> ma = e.getValue().maCase();
            String nguyenVan = e.getValue().nguyenVan();
            // Nút lọc: bấm một nhóm lỗi là chỉ còn các case của nhóm đó — với 39 case hỏng cùng một
            // lý do thì đây là khác biệt giữa đọc được và phải cuộn.
            sb.append("<div class=\"gomloi-dong\"><span class=\"gomloi-so\">").append(ma.size())
                    .append("</span><button class=\"gomloi-msg\" type=\"button\" data-loc-ma=\"")
                    .append(esc(String.join("|", ma).toLowerCase(Locale.ROOT)))
                    .append("\" title=\"Chỉ hiện các kịch bản hỏng vì lý do này\">")
                    .append(esc(rutGon(nguyenVan, 180)))
                    .append("</button><span class=\"gomloi-ma\">").append(esc(String.join(", ",
                            ma.size() > 6 ? ma.subList(0, 6) : ma)))
                    .append(ma.size() > 6 ? "…" : "")
                    .append("</span><button class=\"chep chep-loi\" type=\"button\" data-chep=\"")
                    .append(esc(nguyenVan)).append("\" title=\"Chép thông báo lỗi\" ")
                    .append("aria-label=\"Chép thông báo lỗi\">⧉</button></div>");
        }
        return sb.append("</div>").toString();
    }

    /**
     * So sánh với lượt liền trước theo mã case.
     * <p>
     * Câu hỏi thật của một bộ hồi quy không phải "bao nhiêu case hỏng" mà <b>"cái gì vừa mới hỏng"</b>.
     * Mã case đã ổn định theo số thứ tự kịch bản nên đối chiếu được.
     */
    private static Map<String, String> soVoiLuotTruoc(LuotChay nay, LuotChay truoc) {
        Map<String, String> nhan = new LinkedHashMap<>();
        if (truoc == null) {
            return nhan;
        }
        if (truoc.cases() == null || nay.cases() == null) {
            return nhan;
        }
        Map<String, String> cu = new LinkedHashMap<>();
        for (BaoCaoData.CaseBaoCao c : truoc.cases()) {
            if (c != null) {
                cu.put(nz(c.maCase()), nz(c.trangThai()));
            }
        }
        for (BaoCaoData.CaseBaoCao c : nay.cases()) {
            if (c == null) {
                continue;
            }
            String truocTt = cu.get(nz(c.maCase()));
            if (truocTt == null) {
                nhan.put(nz(c.maCase()), "moi");
                continue;
            }
            // Bỏ qua ≠ hỏng. Kịch bản chưa từng chạy thì không có gì để nói là "vẫn hỏng" — gắn
            // nhãn đó lên nó là khẳng định một điều mà lượt chạy này không hề kiểm chứng.
            if (TrangThai.BO_QUA.equalsIgnoreCase(nz(c.trangThai()).trim())) {
                nhan.put(c.maCase(), "boqua");
                continue;
            }
            // Lượt trước BỎ QUA nghĩa là lượt đó chưa hề kiểm case này. Không được nói "vẫn hỏng"
            // (ngụ ý lượt trước đã thấy nó hỏng) mà cũng không được nói "mới hỏng" (ngụ ý lượt
            // trước đã thấy nó đạt). Cả hai đều là khẳng định không có căn cứ.
            boolean truocChuaKiem = TrangThai.BO_QUA.equalsIgnoreCase(nz(truocTt).trim());
            boolean datNay = TrangThai.DAT.equalsIgnoreCase(nz(c.trangThai()).trim());
            boolean datTruoc = TrangThai.DAT.equalsIgnoreCase(nz(truocTt).trim());
            if (truocChuaKiem) {
                nhan.put(c.maCase(), datNay ? "" : "chuadoichieu");
            } else if (datNay && !datTruoc) {
                nhan.put(c.maCase(), "dasua");
            } else if (!datNay && datTruoc) {
                nhan.put(c.maCase(), "moihong");
            } else if (!datNay) {
                nhan.put(c.maCase(), "vanhong");
            }
        }
        return nhan;
    }

    /**
     * Lượt gần nhất trước đó <b>của cùng bộ kiểm thử</b>.
     * <p>
     * So một lượt MID với một lượt SMOKE là vô nghĩa — hai bộ có tập kịch bản khác nhau nên mọi
     * case đều bị coi là "kịch bản mới". Chỉ đối chiếu trong cùng một bộ.
     */
    private static LuotChay luotTruocCungBo(List<LuotChay> lichSu, int viTri) {
        for (int j = viTri - 1; j >= 0; j--) {
            if (nz(lichSu.get(j).bo()).trim()
                    .equalsIgnoreCase(nz(lichSu.get(viTri).bo()).trim())) {
                return lichSu.get(j);
            }
        }
        return null;
    }

    private static String dongSoSanh(Map<String, String> nhan, boolean coLuotTruoc) {
        if (!coLuotTruoc) {
            return "";
        }
        long moiHong = nhan.values().stream().filter("moihong"::equals).count();
        long daSua = nhan.values().stream().filter("dasua"::equals).count();
        long vanHong = nhan.values().stream().filter("vanhong"::equals).count();
        long moi = nhan.values().stream().filter("moi"::equals).count();
        long boQua = nhan.values().stream().filter("boqua"::equals).count();
        long chuaDoiChieu = nhan.values().stream().filter("chuadoichieu"::equals).count();
        StringBuilder sb = new StringBuilder("So với lượt trước: ");
        sb.append("<b class=\"").append(moiHong > 0 ? "xau" : "nhat").append("\">").append(moiHong)
                .append("</b> mới hỏng<span class=\"cach\">·</span><b class=\"ok\">").append(daSua)
                .append("</b> đã sửa<span class=\"cach\">·</span><b class=\"nhat\">").append(vanHong)
                .append("</b> vẫn hỏng");
        if (boQua > 0) {
            sb.append("<span class=\"cach\">·</span><b class=\"nhat\">").append(boQua)
                    .append("</b> không chạy được");
        }
        if (chuaDoiChieu > 0) {
            sb.append("<span class=\"cach\">·</span><b class=\"nhat\">").append(chuaDoiChieu)
                    .append("</b> lượt trước chưa kiểm");
        }
        if (moi > 0) {
            sb.append("<span class=\"cach\">·</span><b class=\"nhat\">").append(moi).append("</b> kịch bản mới");
        }
        return sb.toString();
    }

    private static String huyNhan(String loai) {
        return switch (loai == null ? "" : loai) {
            case "moihong" -> "<span class=\"nhandau nhandau-moihong\">Mới hỏng</span>";
            case "dasua" -> "<span class=\"nhandau nhandau-dasua\">Đã sửa</span>";
            case "vanhong" -> "<span class=\"nhandau nhandau-vanhong\">Vẫn hỏng</span>";
            case "boqua" -> "<span class=\"nhandau nhandau-vanhong\">Không chạy được</span>";
            case "chuadoichieu" -> "<span class=\"nhandau nhandau-vanhong\">Lượt trước chưa kiểm</span>";
            case "moi" -> "<span class=\"nhandau nhandau-moi\">Kịch bản mới</span>";
            default -> "";
        };
    }

    private static String rutGon(String s, int n) {
        if (s == null) {
            return "";
        }
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    // ── Khối một lượt chạy (ba tầng) ───────────────────────────────────────

    /**
     * Lượt cũ ngoài ngưỡng {@link #SO_LUOT_DUNG_CHI_TIET} — chỉ dòng tóm tắt, không dựng kịch bản.
     * <p>
     * Vẫn giữ nguyên ngày, bộ, thanh đạt/lỗi, tỉ lệ và thời gian, nên vẫn tra cứu và so sánh được;
     * chỉ phần nặng (kịch bản, bước, ảnh) là lược đi.
     */
    private static String mucLuotGon(LuotChay d) {
        String ngay = d.luc().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return "<details class=\"luot luot-gon\" data-ngay=\""
                + d.luc().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + "\" data-tim=\"" + esc(boDau(ngay + " " + nz(d.bo()))) + "\">"
                + "<summary class=\"luot-dau\"><span class=\"mui\" aria-hidden=\"true\"></span>"
                + "<span class=\"luot-ngay\">" + esc(ngay) + "</span>"
                + "<span class=\"chip\">" + esc(nhanBo(d)) + "</span>"
                + thanhPhanBo(d.dat(), d.thatBai(), d.boQua())
                + "<span class=\"luot-tile\">" + d.dat() + "/" + d.tong() + "</span>"
                + "<span class=\"luot-tg\">" + esc(TaoDonReportBuilder.formatDuration(d.wallMs()))
                + "</span></summary><div class=\"luot-than\"><p class=\"trong\">"
                + "Chi tiết của lượt này đã lược để trang nhẹ — dữ liệu gốc vẫn nằm ở "
                + "<code>runs/" + esc(nz(d.moc())) + "/</code>.</p></div></details>\n";
    }

    private static String mucLuotChay(LuotChay d, LuotChay truoc) {
        String ngay = d.luc().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String ngayLoc = d.luc().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        StringBuilder sb = new StringBuilder();
        sb.append("<details class=\"luot\" data-ngay=\"").append(ngayLoc)
                .append("\" data-tim=\"").append(esc((ngay + " " + d.bo()).toLowerCase(Locale.ROOT)))
                .append("\"><summary class=\"luot-dau\">")
                .append("<span class=\"mui\" aria-hidden=\"true\"></span>")
                .append("<span class=\"luot-ngay\">").append(esc(ngay)).append("</span>")
                .append("<span class=\"chip\">").append(esc(d.bo())).append("</span>")
                .append(thanhPhanBo(d.dat(), d.thatBai(), d.boQua()))
                .append("<span class=\"luot-tile\">").append(d.dat()).append('/').append(d.tong())
                .append("</span>")
                .append("<span class=\"luot-tg\">").append(esc(TaoDonReportBuilder.formatDuration(d.wallMs())))
                .append("</span></summary><div class=\"luot-than\">");

        // Đường dẫn dạng chữ, không phải liên kết: lượt hỏng ngay khâu đăng nhập không chụp được
        // ảnh nào, một liên kết dẫn tới trang lỗi làm hỏng lòng tin vào cả bản báo cáo. Mốc ở đây
        // phải trùng giờ trên dòng tiêu đề, nếu không người đọc không ghép được với log.
        sb.append("<p class=\"lienket mo\">Dữ liệu, ảnh và log của lượt này nằm ở <code>runs/")
                .append(esc(nz(d.moc()))).append("/</code></p>");

        if (d.cases() == null || d.cases().isEmpty()) {
            sb.append("<p class=\"trong\">Lượt chạy này không ghi nhận kịch bản nào.</p>");
        } else {
            java.util.Map<String, String> nhan = soVoiLuotTruoc(d, truoc);
            sb.append("<p class=\"sosanh\">").append(dongSoSanh(nhan, truoc != null)).append("</p>")
              .append(bangGomLoi(d))
              .append(phieuLuong(d))
              .append("<div class=\"loc-case\">")
              .append("<input class=\"tim-case\" type=\"search\""
                      + " aria-label=\"Tìm kịch bản trong lượt chạy này\""
                      + " placeholder=\"Tìm mã case, tên kịch bản, nội dung lỗi…\">")
              .append("<label class=\"chon\"><input type=\"checkbox\" class=\"chi-loi\"> Chỉ hiện case lỗi</label>")
              .append("<button class=\"nut nho bung\" type=\"button\" data-bung=\"1\">Mở tất cả</button>")
              .append("<button class=\"nut nho bung\" type=\"button\" data-bung=\"0\">Thu gọn</button>")
              .append("<button class=\"nut nho bo-loc-case\" type=\"button\" hidden>Bỏ lọc</button>")
              .append("<span class=\"dem-case mo\" role=\"status\"></span></div>");
            sb.append(bangDoPhu(d));
            for (BaoCaoData.CaseBaoCao c : d.cases()) {
                sb.append(mucCase(c, nhan.get(c.maCase()), d.moc()));
            }
        }
        return sb.append("</div></details>\n").toString();
    }

    /**
     * Thanh phần-trên-tổng-thể, khe 2px giữa các đoạn.
     * <p>
     * Luôn kèm nhãn chữ bên cạnh: màu vàng "bỏ qua" có tương phản dưới 3:1 trên nền sáng, nên theo
     * đúng quy tắc của bảng màu trạng thái, màu không bao giờ được đứng một mình.
     */
    private static String thanhPhanBo(int dat, int loi, int bo) {
        int tong = dat + loi + bo;
        if (tong <= 0) {
            return "<span class=\"pb-nhan\">—</span>";
        }
        StringBuilder sb = new StringBuilder("<span class=\"pb\" role=\"img\" aria-label=\""
                + dat + " đạt, " + loi + " thất bại, " + bo + " bỏ qua\">");
        if (dat > 0) {
            sb.append("<i class=\"seg dat\" style=\"width:").append(fmt(dat * 100.0 / tong)).append("%\"></i>");
        }
        if (loi > 0) {
            sb.append("<i class=\"seg loi\" style=\"width:").append(fmt(loi * 100.0 / tong)).append("%\"></i>");
        }
        if (bo > 0) {
            sb.append("<i class=\"seg bo\" style=\"width:").append(fmt(bo * 100.0 / tong)).append("%\"></i>");
        }
        sb.append("</span><span class=\"pb-nhan\"><b class=\"ok\">").append(dat).append("</b> đạt");
        if (loi > 0) {
            sb.append("<span class=\"cach\">·</span><b class=\"xau\">").append(loi).append("</b> lỗi");
        }
        if (bo > 0) {
            sb.append("<span class=\"cach\">·</span><b class=\"nhat\">").append(bo).append("</b> bỏ qua");
        }
        return sb.append("</span>").toString();
    }

    /**
     * Tổng quan luồng nộp đơn: sáu bước là gì, và bao nhiêu kịch bản đi qua được từng bước.
     * <p>
     * Trước đây báo cáo <b>không hề nói bước 1, bước 2 là gì</b> — tên bước chỉ nằm trong tooltip
     * của dải tiến độ, nên người đọc thấy "Bước 4" mà không biết đó là khâu nào. Ở đây vừa là chú
     * giải cho toàn bộ con số "Bước N" trên trang, vừa là một cái phễu: nhìn một cái là thấy kịch
     * bản rụng ở khâu nào.
     * <p>
     * Tên bước lấy từ {@link TaoDonReportBuilder#tenBuocDayDu(int)} — tên chuẩn — chứ không lấy tên
     * trong dữ liệu case, vì tên đó có phần biến thiên theo kịch bản ("… (đã điền 2 bị đơn)").
     */
    private static String phieuLuong(LuotChay d) {
        int[] qua = new int[7];
        int[] dung = new int[7];
        int coDuLieu = 0;
        for (BaoCaoData.CaseBaoCao c : d.cases()) {
            if (c == null || c.tomTatBuocAnToan().isEmpty()) {
                continue;
            }
            coDuLieu++;
            for (BaoCaoData.TomTatBuoc t : c.tomTatBuocAnToan()) {
                if (t == null || t.soBuoc() < 1 || t.soBuoc() > 6) {
                    continue;
                }
                if (TrangThai.DAT.equalsIgnoreCase(nz(t.ketQua()).trim())) {
                    qua[t.soBuoc()]++;
                } else if (TrangThai.KHONG_HOAN_THANH.equalsIgnoreCase(nz(t.ketQua()).trim())) {
                    dung[t.soBuoc()]++;
                }
            }
        }
        if (coDuLieu == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<div class=\"luong\"><div class=\"luong-dau\">")
                .append("Luồng nộp đơn — ").append(coDuLieu)
                .append(" kịch bản đi qua 6 bước</div><ol class=\"luong-ds\">");
        for (int i = 1; i <= 6; i++) {
            double phan = coDuLieu > 0 ? qua[i] * 100.0 / coDuLieu : 0;
            sb.append("<li><span class=\"luong-so\">").append(i).append("</span>")
                    .append("<span class=\"luong-ten\">")
                    .append(esc(TaoDonReportBuilder.tenBuocDayDu(i))).append("</span>")
                    .append("<span class=\"luong-thanh\" aria-hidden=\"true\"><i style=\"width:")
                    .append(fmt(phan)).append("%\"></i></span>")
                    .append("<span class=\"luong-qua\">").append(qua[i]).append(" qua</span>")
                    .append(dung[i] > 0
                            ? "<span class=\"luong-rot\">" + dung[i] + " dừng ở đây</span>"
                            : "<span class=\"luong-rot mo\">—</span>")
                    .append("</li>");
        }
        return sb.append("</ol></div>").toString();
    }

    /**
     * Độ phủ của một lượt: mọi cặp <i>loại đơn × loại việc</i> trong danh mục, đối chiếu với những
     * cặp đã thực sự chạy. Trả lời câu hỏi <b>"bộ test này còn sót gì"</b>.
     * <p>
     * Trước đây chỉ có trong sheet {@code Độ phủ} của file Excel (và chỉ với bộ FULL/MID). Đưa vào
     * đây để không phải mở hai file mới biết mình đã kiểm hết chưa.
     * <p>
     * Nguồn dữ liệu là <b>nhãn</b> của kịch bản — {@code nhan[0]} là loại đơn, {@code nhan[1]} là
     * loại việc, do listener gắn từ chính scenario. Không phân tích tiêu đề, vì tiêu đề là chuỗi
     * cho người đọc và sẽ đổi.
     */
    private static String bangDoPhu(LuotChay d) {
        java.util.Map<String, Integer> dem = new java.util.HashMap<>();
        for (BaoCaoData.CaseBaoCao c : d.cases()) {
            List<String> nhan = c == null ? null : c.nhan();
            if (nhan != null && nhan.size() >= 2 && nhan.get(0) != null && nhan.get(1) != null) {
                dem.merge(nhan.get(0).trim() + " › " + nhan.get(1).trim(), 1, Integer::sum);
            }
        }
        if (dem.isEmpty()) {
            return "";
        }
        List<String[]> tatCa;
        try {
            tatCa = vn.tuphap.automation.data.MasterDataCatalog.getAllLoaiDonViecPairs();
        } catch (RuntimeException e) {
            // Danh mục hỏng thì bỏ hẳn khối này — không được để nó làm chết cả trang báo cáo.
            return "";
        }
        List<String> thieu = new java.util.ArrayList<>();
        int phu = 0;
        for (String[] cap : tatCa) {
            // trim CẢ HAI phía: danh mục đọc từ file, thừa một khoảng trắng là cặp đã chạy vẫn bị
            // đếm thành "còn thiếu" và báo cáo nói sai về độ phủ.
            String khoa = nz(cap[0]).trim() + " › " + nz(cap[1]).trim();
            if (dem.getOrDefault(khoa, 0) > 0) {
                phu++;
            } else {
                thieu.add(khoa);
            }
        }
        int tong = tatCa.size();
        boolean du = thieu.isEmpty();
        StringBuilder sb = new StringBuilder("<details class=\"phu-khoi\"><summary>")
                .append("<span class=\"cham ").append(du ? "dat" : "bo").append("\" aria-hidden=\"true\"></span>")
                .append("Độ phủ loại đơn × loại việc: <b>").append(phu).append("/").append(tong)
                .append("</b> cặp đã chạy");
        if (!du) {
            sb.append(" — còn thiếu ").append(thieu.size());
        }
        sb.append("</summary>");
        if (du) {
            sb.append("<p class=\"trong\">Không sót cặp nào trong danh mục.</p>");
        } else {
            sb.append("<p class=\"trong\">Các cặp chưa có kịch bản nào chạy tới:</p><ul class=\"phu-ds\">");
            for (String t : thieu) {
                sb.append("<li>").append(esc(t)).append("</li>");
            }
            sb.append("</ul>");
        }
        return sb.append("</details>").toString();
    }

    /**
     * Bỏ dấu tiếng Việt và hạ chữ thường — dùng cho chỉ mục tìm kiếm.
     * <p>
     * Phía JavaScript có hàm cùng tên làm đúng việc này trên chuỗi người dùng gõ, nên hai bên gặp
     * nhau: gõ {@code "pha san"} khớp {@code "Phá sản"}, mà gõ có dấu vẫn khớp.
     */
    private static String boDau(String raw) {
        if (raw == null) {
            return "";
        }
        return java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Tên neo an toàn cho URL — chỉ chữ, số và gạch ngang. */
    private static String slugNeo(String raw) {
        if (raw == null || raw.isBlank()) {
            return "x";
        }
        String s = java.text.Normalizer.normalize(raw.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return s.isBlank() ? "x" : s;
    }

    private static String mucCase(BaoCaoData.CaseBaoCao c, String nhanSoSanh, String mocLuot) {
        String cls = trangThaiClass(c.trangThai());
        StringBuilder sb = new StringBuilder();
        // Bỏ dấu để gõ "pha san" cũng ra "Phá sản" — không ai gõ dấu khi đang tìm nhanh.
        String timKiem = boDau(nz(c.maCase()) + " " + nz(c.tieuDe()) + " " + lyDoHong(c));
        // Neo để dán liên kết thẳng tới một kịch bản cho đồng nghiệp. Gắn mốc lượt chạy vào id vì
        // cùng một mã case xuất hiện ở mọi lượt — không có mốc thì liên kết luôn nhảy về lượt đầu.
        String neo = "c-" + slugNeo(mocLuot) + "-" + slugNeo(c.maCase());
        sb.append("<details class=\"case\" id=\"").append(neo)
                .append("\" data-tt=\"").append(cls)
                .append("\" data-ma=\"").append(esc(nz(c.maCase()).toLowerCase(Locale.ROOT)))
                .append("\" data-tim=\"").append(esc(timKiem)).append("\"><summary class=\"case-dau\">")
                .append("<span class=\"mui\" aria-hidden=\"true\"></span>")
                // Huy hiệu rỗng = chỉ còn màu, mà màu không bao giờ được đứng một mình.
                .append("<span class=\"huy ").append(cls).append("\">")
                .append(esc(nz(c.trangThai()).isBlank() ? "Không rõ" : c.trangThai())).append("</span>")
                .append(huyNhan(nhanSoSanh))
                .append("<span class=\"ma\">").append(esc(c.maCase())).append("</span>")
                .append("<span class=\"tende\">").append(esc(c.tieuDe())).append("</span>")
                .append("<button class=\"chep\" type=\"button\" data-neo=\"").append(neo)
                .append("\" title=\"Chép liên kết tới kịch bản này\" aria-label=\"Chép liên kết\">🔗</button>")
                .append("<span class=\"case-tg\">").append(esc(TaoDonReportBuilder.formatDuration(c.thoiGianMs())))
                .append("</span></summary><div class=\"case-than\">");

        if (c.moTa() != null && !c.moTa().isBlank()) {
            sb.append("<p class=\"mota\">").append(esc(c.moTa())).append("</p>");
        }
        if (c.nhan() != null && !c.nhan().isEmpty()) {
            sb.append("<p class=\"nhanhang\">");
            for (String n : c.nhan()) {
                sb.append("<span class=\"nhan\">").append(esc(n)).append("</span>");
            }
            sb.append("</p>");
        }
        sb.append(bangKetLuan(c));
        sb.append(daiTienDo(c.tomTatBuocAnToan()));
        // Kịch bản không đi qua 6 bước (suite login) chỉ có thao tác ngoài bước — không in ở đây
        // thì phần "dữ liệu đã nhập" của cả lượt chạy trống trơn.
        sb.append(bangHanhDong(c.hanhDongNgoaiBuocAnToan(), "trường đã nhập (ngoài bước)"));
        String dau = cacSuKien(c.suKienDau());
        if (!dau.isBlank()) {
            sb.append("<div class=\"skhop\">").append(dau).append("</div>");
        }
        if (c.buoc() != null) {
            // Thang đo dùng chung cho cả case: thanh của mỗi bước so với bước lâu nhất, nên nhìn
            // một cái là thấy nút thắt. So từng bước với chính nó thì thanh nào cũng đầy, vô nghĩa.
            long lauNhat = 0;
            for (BaoCaoData.BuocBaoCao b : c.buoc()) {
                if (b != null) {
                    lauNhat = Math.max(lauNhat, b.thoiGianMs());
                }
            }
            for (BaoCaoData.BuocBaoCao b : c.buoc()) {
                if (b != null) {
                    sb.append(mucBuoc(b, lauNhat));
                }
            }
        }
        // Sự kiện sau khi các bước đã xong (vd. kết quả gửi đơn) phải nằm DƯỚI danh sách bước,
        // nếu không dòng lỗi lúc 04:44:54 lại hiện trên bước chạy lúc 04:44:07.
        String cuoi = cacSuKien(c.suKienCuoi());
        if (!cuoi.isBlank()) {
            sb.append("<div class=\"skhop skcuoi\">").append(cuoi).append("</div>");
        }
        if (c.stackTrace() != null && !c.stackTrace().isBlank()) {
            sb.append("<details class=\"vet\"><summary>Chi tiết kỹ thuật cho lập trình viên</summary>")
                    .append("<pre>").append(esc(c.stackTrace().trim())).append("</pre></details>");
        }
        return sb.append("</div></details>").toString();
    }

    /**
     * Kết luận của một kịch bản: mong đợi gì, thực tế ra gì, ghi chú gì.
     * <p>
     * Đây là câu trả lời đầu tiên người đọc cần khi thấy một kịch bản đỏ. Trước đây nó chỉ nằm ở
     * sheet <i>Tổng hợp</i> của file Excel — muốn biết thì phải mở hai file song song. Bỏ trống cả
     * ba thì không hiện gì, để các kịch bản chưa khai báo không đẻ ra một khối rỗng.
     */
    private static String bangKetLuan(BaoCaoData.CaseBaoCao c) {
        // Ba trường này chép nguyên thông báo của hệ thống nên cũng dính "Sao chép mã" và URL.
        String mong = loiChoNguoiDoc(c.ketQuaMongDoi());
        String thuc = loiChoNguoiDoc(c.ketQuaThucTe());
        String ghi = loiChoNguoiDoc(c.ghiChuKetQua());
        // Ghi chú chỉ chép lại Kết quả thực tế thì bỏ hẳn — chiếm một dòng mà không nói thêm gì,
        // và làm người đọc phải đọc hai lần mới nhận ra là cùng một câu.
        if (nhacLai(ghi, thuc)) {
            ghi = "";
        }
        if (mong.isBlank() && thuc.isBlank() && ghi.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("<dl class=\"ketluan\">");
        if (!mong.isBlank()) {
            sb.append("<dt>Kết quả mong đợi</dt><dd>").append(esc(mong)).append("</dd>");
        }
        if (!thuc.isBlank()) {
            sb.append("<dt>Kết quả thực tế</dt><dd>").append(esc(thuc)).append("</dd>");
        }
        if (!ghi.isBlank()) {
            sb.append("<dt>Ghi chú</dt><dd>").append(esc(ghi)).append("</dd>");
        }
        return sb.append("</dl>").toString();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /**
     * Hai câu có cùng nội dung thật hay không, bỏ qua phần dẫn nhập khác nhau.
     * <p>
     * {@code "Message lỗi hệ thống sau Gửi đơn: X"} và {@code "Hệ thống báo lỗi sau Gửi đơn: X"} là
     * cùng một câu với hai cách mở đầu. So phần <b>sau dấu hai chấm đầu tiên</b>, đã chuẩn hoá bỏ
     * mã lỗi, thì nhận ra được.
     */
    private static boolean nhacLai(String a, String b) {
        String x = chuanHoaLoi(sauDauHaiCham(a));
        String y = chuanHoaLoi(sauDauHaiCham(b));
        return !x.isBlank() && x.equalsIgnoreCase(y);
    }

    private static String sauDauHaiCham(String s) {
        String t = nz(s).trim();
        int i = t.indexOf(':');
        // Chỉ coi là phần dẫn nhập nếu nó ngắn; "10:17" hay một URL không phải nhãn.
        return (i > 0 && i <= 40) ? t.substring(i + 1).trim() : t;
    }

    /**
     * Dải 6 bước của một kịch bản — trạng thái từng bước cộng một thanh dài theo thời gian.
     * <p>
     * Danh sách bước bên dưới chỉ chứa những bước <b>đã mở</b>, nên một case hỏng ở bước 3 trông
     * y hệt một case chỉ định chạy tới bước 3. Dải này phân biệt: bước 4–6 hiện "Chưa chạy tới".
     * <p>
     * Hai đại lượng, hai cách mã hoá riêng — trạng thái dùng bảng màu trạng thái <i>kèm ký hiệu và
     * chữ</i> (không bao giờ chỉ bằng màu), thời gian dùng độ dài thanh so với bước lâu nhất của
     * chính case đó. Không trộn hai thứ vào một kênh.
     */
    private static String daiTienDo(List<BaoCaoData.TomTatBuoc> ds) {
        if (ds == null || ds.isEmpty()) {
            return "";
        }
        long lauNhat = 0;
        for (BaoCaoData.TomTatBuoc t : ds) {
            if (t != null) {
                lauNhat = Math.max(lauNhat, t.thoiGianMs());
            }
        }
        StringBuilder sb = new StringBuilder(
                "<ol class=\"dai\" aria-label=\"Tiến độ 6 bước của kịch bản\">");
        for (BaoCaoData.TomTatBuoc t : ds) {
            if (t == null) {
                continue;
            }
            String cls = trangThaiClass(t.ketQua());
            String kyHieu = switch (cls) {
                case "dat" -> "✓";
                case "loi" -> "✕";
                default -> "·";
            };
            double phan = lauNhat > 0 && t.thoiGianMs() > 0 ? t.thoiGianMs() * 100.0 / lauNhat : 0;
            sb.append("<li class=\"o ").append(cls).append("\" title=\"")
                    .append(esc("Bước " + t.soBuoc() + " — " + nz(t.ten()) + ": " + nz(t.ketQua())))
                    .append("\"><span class=\"o-dau\"><b>").append(t.soBuoc())
                    .append("</b><span class=\"o-kh\" aria-hidden=\"true\">").append(kyHieu)
                    .append("</span></span><span class=\"o-tt\">").append(esc(t.ketQua()))
                    .append("</span><span class=\"o-tg\">")
                    .append(t.thoiGianMs() < 0 ? "—" : TaoDonReportBuilder.formatDuration(t.thoiGianMs()))
                    .append("</span><span class=\"o-thanh\"><i style=\"width:")
                    .append(fmt(phan)).append("%\"></i></span></li>");
        }
        return sb.append("</ol>").toString();
    }

    /**
     * @param lauNhat thời gian của bước lâu nhất trong cùng kịch bản — mẫu số của thanh thời gian
     */
    private static String mucBuoc(BaoCaoData.BuocBaoCao b, long lauNhat) {
        String cls = trangThaiClass(b.trangThai());
        String than = cacSuKien(b.suKien());
        double phan = lauNhat > 0 && b.thoiGianMs() > 0 ? b.thoiGianMs() * 100.0 / lauNhat : 0;
        StringBuilder sb = new StringBuilder();
        sb.append("<details class=\"buoc ").append(cls).append("\"><summary class=\"buoc-dau\">")
                .append("<span class=\"mui\" aria-hidden=\"true\"></span>")
                .append("<span class=\"cham ").append(cls).append("\" aria-hidden=\"true\"></span>")
                .append("<span class=\"buoc-ten\">").append(esc(b.ten())).append("</span>")
                .append("<span class=\"buoc-tt ").append(cls).append("\">").append(esc(b.trangThai())).append("</span>")
                // Độ dài là đại lượng độ lớn nên dùng một màu duy nhất, không mượn màu trạng thái.
                .append("<span class=\"buoc-thanh\" aria-hidden=\"true\"><i style=\"width:")
                .append(fmt(phan)).append("%\"></i></span>")
                .append("<span class=\"buoc-tg\">").append(esc(TaoDonReportBuilder.formatDuration(b.thoiGianMs())))
                .append("</span></summary><div class=\"buoc-than\">")
                .append(bangHanhDong(b.hanhDong()));
        boolean trong = than.isBlank() && (b.hanhDong() == null || b.hanhDong().isEmpty());
        if (!trong) {
            sb.append(than);
        } else if ("dat".equals(cls)) {
            sb.append("<p class=\"trong\">Bước chạy trót lọt, không có ghi chú nào.</p>");
        } else {
            // KHÔNG được nói "chạy trót lọt" cho một bước hỏng. Bước bị ngoại lệ ném ra trước khi
            // kịp ghi gì thì rỗng nội dung nhưng trạng thái là Thất bại — câu cũ chỉ xét rỗng hay
            // không, nên in "chạy trót lọt" ngay dưới chữ "Thất bại" đỏ.
            sb.append("<p class=\"trong\">Bước dừng ở đây, không kịp ghi lại chi tiết nào — "
                    + "xem thông báo lỗi ở cuối kịch bản.</p>");
        }
        return sb.append("</div></details>").toString();
    }

    /**
     * Bảng dữ liệu đã nhập trong bước — {@code Họ và tên · Điền · Nguyễn Văn A}.
     * <p>
     * Trước đây chỉ có trong file Excel, nên xem một case hỏng phải mở song song hai file mới biết
     * nó đã nhập gì. Gập sẵn để không lấn át phần kết quả, nhưng ở ngay trong đúng bước.
     */
    private static String bangHanhDong(List<BaoCaoData.HanhDong> ds) {
        return bangHanhDong(ds, "trường đã nhập");
    }

    /** @param nhan phần đuôi của tiêu đề bảng, sau số lượng trường */
    private static String bangHanhDong(List<BaoCaoData.HanhDong> ds, String nhan) {
        if (ds == null || ds.isEmpty()) {
            return "";
        }
        // Bọc bảng trong khung cuộn riêng: giá trị dài (đường dẫn file, URL) làm bảng rộng hơn
        // khung, và trước đây tổ tiên có overflow:hidden nên phần thừa bị CẮT MẤT, không có thanh
        // cuộn nào để kéo ra xem — dữ liệu biến mất im lặng.
        StringBuilder sb = new StringBuilder("<details class=\"dulieu\"><summary>")
                .append(ds.size()).append(' ').append(esc(nhan)).append("</summary><div class=\"cuon-ngang\">")
                .append("<table class=\"bang-dl\">")
                .append("<tr><th>Trường</th><th>Thao tác</th><th>Giá trị</th></tr>");
        for (BaoCaoData.HanhDong h : ds) {
            if (h == null) {
                continue;
            }
            String giaTri = h.giaTri() == null || h.giaTri().isBlank() ? "—" : h.giaTri();
            if (h.ghiChu() != null && !h.ghiChu().isBlank()) {
                giaTri = giaTri + "  ·  " + h.ghiChu();
            }
            sb.append("<tr><td>").append(esc(h.truong()))
                    .append("</td><td class=\"tt-").append(esc(slugThaoTac(h.thaoTac())))
                    .append("\">").append(esc(h.thaoTac()))
                    .append("</td><td>").append(esc(giaTri)).append("</td></tr>");
        }
        return sb.append("</table></div></details>").toString();
    }

    private static String slugThaoTac(String t) {
        String s = t == null ? "" : t.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("điền")) {
            return "dien";
        }
        if (s.startsWith("chọn")) {
            return "chon";
        }
        if (s.startsWith("tải")) {
            return "tai";
        }
        if (s.startsWith("bỏ qua")) {
            return "boqua";
        }
        if (s.startsWith("validation")) {
            return "loi";
        }
        return "khac";
    }

    private static String cacSuKien(List<BaoCaoData.SuKien> ds) {
        if (ds == null || ds.isEmpty()) {
            return "";
        }
        StringBuilder chinh = new StringBuilder();
        StringBuilder kyThuat = new StringBuilder();
        int soKyThuat = 0;
        for (BaoCaoData.SuKien sk : ds) {
            if (sk == null) {
                continue;
            }
            if (laChanDoan(sk)) {
                kyThuat.append(dongSuKien(sk));
                soKyThuat++;
            } else {
                chinh.append(dongSuKien(sk));
            }
        }
        // Dòng chẩn đoán (đo thời gian, số lượt gọi trình duyệt) gập lại: hữu ích khi tối ưu tốc
        // độ, nhưng người đọc báo cáo không cần thấy chúng xen giữa các bước.
        if (soKyThuat > 0) {
            chinh.append("<details class=\"chandoan\"><summary>")
                    .append(soKyThuat).append(" dòng đo lường kỹ thuật</summary>")
                    .append(kyThuat).append("</details>");
        }
        return chinh.toString();
    }

    /** Dòng chỉ phục vụ việc tối ưu tốc độ, không phải kết quả kiểm thử. */
    private static boolean laChanDoan(BaoCaoData.SuKien sk) {
        String s = sk.noiDung() == null ? "" : sk.noiDung();
        return s.startsWith("↳");
    }

    private static String dongSuKien(BaoCaoData.SuKien sk) {
        // Dọn cả dòng, không riêng dòng lỗi: chú thích ảnh cũng dính URL đầy đủ của trang.
        String noiDung = loiChoNguoiDoc(sk.noiDung());
        StringBuilder sb = new StringBuilder("<div class=\"sk sk-").append(esc(sk.muc())).append("\">")
                .append("<span class=\"sk-gio\">").append(esc(sk.thoiDiem())).append("</span>")
                .append("<span class=\"sk-noidung\">").append(esc(noiDung)).append("</span>");
        if (sk.anh() != null && !sk.anh().isBlank()) {
            // aria-label mô tả ĐÚNG sự kiện mà ảnh chụp, không phải "ảnh màn hình" chung chung —
            // lớp phóng to cũng đọc lại nhãn này để đặt alt cho ảnh lớn.
            String nhan = "Ảnh màn hình: " + rutGon(noiDung, 90);
            sb.append("<button class=\"sk-anh\" type=\"button\" data-anh=\"").append(esc(sk.anh()))
                    .append("\" title=\"Bấm để phóng to\" aria-label=\"").append(esc(nhan))
                    .append("\"><img src=\"").append(esc(sk.anh()))
                    .append("\" loading=\"lazy\" alt=\"\"></button>");
        }
        return sb.append("</div>").toString();
    }

    private static String trangThaiClass(String tt) {
        String t = tt == null ? "" : tt.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("đạt")) {
            return "dat";
        }
        if (t.startsWith("thất bại") || t.startsWith("không hoàn thành")) {
            return "loi";
        }
        if (t.startsWith("bỏ qua") || t.startsWith("chưa chạy")) {
            return "bo";
        }
        return "khac";
    }

    // ── Biểu đồ xu hướng ───────────────────────────────────────────────────

    private static String bieuDoXuHuong(List<LuotChay> lichSu) {
        List<LuotChay> ds = lichSu.size() > SO_LUOT_TREN_BIEU_DO
                ? lichSu.subList(lichSu.size() - SO_LUOT_TREN_BIEU_DO, lichSu.size())
                : lichSu;
        int w = 900;
        int h = 210;
        int tren = 14;
        int duoi = 32;
        int trai = 40;
        int phai = 58;
        double vungW = w - trai - phai;
        double vungH = h - tren - duoi;

        StringBuilder sb = new StringBuilder();
        sb.append("<svg class=\"xh\" viewBox=\"0 0 ").append(w).append(' ').append(h)
                .append("\" role=\"img\" aria-label=\"Xu hướng tỉ lệ đạt qua ").append(ds.size())
                .append(" lượt chạy\" preserveAspectRatio=\"xMidYMid meet\">");
        for (int v = 0; v <= 100; v += 25) {
            double y = tren + vungH * (1 - v / 100.0);
            sb.append("<line class=\"luoi\" x1=\"").append(trai).append("\" y1=\"").append(fmt(y))
                    .append("\" x2=\"").append(fmt(trai + vungW)).append("\" y2=\"").append(fmt(y)).append("\"/>")
                    .append("<text class=\"truc\" x=\"").append(trai - 8).append("\" y=\"").append(fmt(y + 4))
                    .append("\" text-anchor=\"end\">").append(v).append("</text>");
        }

        // MỖI BỘ MỘT ĐƯỜNG RIÊNG. Bản cũ nối một đường xuyên qua cả MID, SMOKE lẫn LOGIN — đường
        // nhảy 100% → 51% → 100% chỉ vì đổi bộ chứ không phải vì chất lượng đổi, và tooltip không
        // hề nói điểm đó thuộc bộ nào. Nối hai tập kịch bản khác nhau bằng một đường là vẽ ra một
        // xu hướng không tồn tại.
        List<double[]> diem = new ArrayList<>();
        for (int i = 0; i < ds.size(); i++) {
            double x = trai + (ds.size() == 1 ? vungW / 2 : vungW * i / (ds.size() - 1.0));
            double y = tren + vungH * (1 - ds.get(i).tiLe() / 100.0);
            diem.add(new double[]{x, y});
        }
        // Thứ tự bộ cố định theo lần xuất hiện — màu bám theo BỘ, không bám theo thứ hạng, nên
        // lọc bớt một bộ không làm các bộ còn lại đổi màu.
        List<String> boTheoThuTu = new ArrayList<>();
        for (LuotChay l : ds) {
            String b = nhanBo(l);
            if (!boTheoThuTu.contains(b)) {
                boTheoThuTu.add(b);
            }
        }
        for (int k = 0; k < boTheoThuTu.size(); k++) {
            String bo = boTheoThuTu.get(k);
            StringBuilder duong = new StringBuilder();
            int soDiem = 0;
            for (int i = 0; i < ds.size(); i++) {
                if (!nhanBo(ds.get(i)).equals(bo)) {
                    continue;
                }
                duong.append(soDiem == 0 ? "M" : "L").append(fmt(diem.get(i)[0])).append(' ')
                        .append(fmt(diem.get(i)[1])).append(' ');
                soDiem++;
            }
            if (soDiem >= 2) {
                sb.append("<path class=\"xh-duong bo").append(k % 4).append("\" d=\"")
                        .append(duong.toString().trim()).append("\"/>");
            }
        }
        for (int i = 0; i < ds.size(); i++) {
            LuotChay d = ds.get(i);
            double[] p = diem.get(i);
            int k = boTheoThuTu.indexOf(nhanBo(d));
            sb.append("<circle class=\"xh-diem bo").append(k % 4).append("\" cx=\"").append(fmt(p[0]))
                    .append("\" cy=\"").append(fmt(p[1])).append("\" r=\"4.5\"><title>")
                    .append(esc(nhanBo(d))).append(" · ")
                    .append(esc(d.luc().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))))
                    .append(" — ").append(d.dat()).append('/')
                    .append(d.tong()).append(" đạt · ")
                    .append(esc(TaoDonReportBuilder.formatDuration(d.wallMs())))
                    .append("</title></circle>");
        }
        double[] cuoi = diem.get(diem.size() - 1);
        sb.append("<text class=\"xh-nhan\" x=\"").append(fmt(cuoi[0] + 10)).append("\" y=\"")
                .append(fmt(cuoi[1] + 4)).append("\">").append(ds.get(ds.size() - 1).dat())
                .append('/').append(ds.get(ds.size() - 1).tong()).append("</text>");
        if (ds.size() >= 2) {
            sb.append("<text class=\"truc\" x=\"").append(trai).append("\" y=\"").append(h - 9).append("\">")
                    .append(esc(ds.get(0).luc().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")))).append("</text>")
                    .append("<text class=\"truc\" x=\"").append(fmt(trai + vungW)).append("\" y=\"").append(h - 9)
                    .append("\" text-anchor=\"end\">")
                    .append(esc(ds.get(ds.size() - 1).luc().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))))
                    .append("</text>");
        }
        sb.append("</svg>");
        // Từ hai bộ trở lên thì bắt buộc có chú giải — màu không được đứng một mình.
        if (boTheoThuTu.size() >= 2) {
            sb.append("<p class=\"xh-chu\">");
            for (int k = 0; k < boTheoThuTu.size(); k++) {
                sb.append("<span class=\"xh-muc\"><i class=\"xh-o bo").append(k % 4)
                        .append("\" aria-hidden=\"true\"></i>").append(esc(boTheoThuTu.get(k)))
                        .append("</span>");
            }
            sb.append("</p>");
        }
        return sb.toString();
    }

    /** Nhãn bộ kiểm thử dùng cho biểu đồ — rỗng thì gọi là "Khác" chứ không để trống. */
    private static String nhanBo(LuotChay d) {
        String b = nz(d.bo()).trim();
        return b.isEmpty() ? "Khác" : b.toUpperCase(Locale.ROOT);
    }

    // ── Khung trang ────────────────────────────────────────────────────────

    /**
     * Dựng trang từ danh sách lượt chạy cho sẵn, <b>không đụng tới đĩa</b>.
     * <p>
     * Mở cho test cùng gói: mọi lỗi báo cáo tìm được trong đợt này đều thuộc loại không bắn ngoại
     * lệ — trang vẫn dựng, chỉ là sai. Muốn khoá lại thì phải soi được chuỗi HTML mà không cần
     * chạy test thật và không ghi đè {@code index.html} thật.
     */
    static String dungTrangDeKiemTra(List<LuotChay> lichSu) {
        return trang(lichSu);
    }

    private static String trang(List<LuotChay> lichSu) {
        LuotChay moiNhat = lichSu.isEmpty() ? null : lichSu.get(lichSu.size() - 1);
        String capNhat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        StringBuilder muc = new StringBuilder();
        int nguong = lichSu.size() - SO_LUOT_DUNG_CHI_TIET;
        for (int i = lichSu.size() - 1; i >= 0; i--) {
            muc.append(i >= nguong
                    ? mucLuotChay(lichSu.get(i), luotTruocCungBo(lichSu, i))
                    : mucLuotGon(lichSu.get(i)));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang=\"vi\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>Báo cáo kiểm thử tự động — Tạo đơn dịch vụ tư pháp toà án</title>")
                .append("<style>").append(css()).append("</style></head><body>")
                .append("<a class=\"boqua-toi\" href=\"#ds-luot\">Bỏ qua tới danh sách lượt chạy</a>")
                .append("<header class=\"dau\"><div class=\"dau-trai\"><h1>Báo cáo kiểm thử tự động</h1>")
                .append("<p class=\"phu\">Tạo đơn dịch vụ tư pháp toà án<span class=\"cach\">·</span>cập nhật ")
                .append(esc(capNhat)).append("<span class=\"cach\">·</span>")
                .append(lichSu.size()).append(" lượt chạy đã lưu</p></div>")
                .append("<button id=\"doigiaodien\" class=\"nut\" type=\"button\">Giao diện tối</button>")
                .append("</header><main class=\"than\">");

        if (moiNhat != null) {
            // PHẢI nói rõ bốn ô này là của lượt nào, bộ nào. Đây là bốn con số duy nhất nhiều
            // người đọc thật sự xem, mà `moiNhat` chỉ là lượt mới nhất BẤT KỂ bộ — một lượt SMOKE
            // 3 kịch bản đạt hết sẽ hiện "100.0%" xanh trong khi bộ MID 39 kịch bản đang đỏ nằm
            // ngay bên dưới. Không dán nhãn thì con số đó nói dối mà không sai.
            sb.append("<p class=\"the-dan-nhan\">Lượt chạy mới nhất<span class=\"cach\">·</span>")
                    .append("<b>").append(esc(nz(moiNhat.bo()).isBlank() ? "—" : moiNhat.bo()))
                    .append("</b><span class=\"cach\">·</span>")
                    .append(esc(moiNhat.luc().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))))
                    .append(soBoKhac(lichSu) > 0
                            ? "<span class=\"cach\">·</span><span class=\"mo\">lịch sử bên dưới còn "
                              + soBoKhac(lichSu) + " bộ kiểm thử khác</span>"
                            : "")
                    .append("</p>")
                    .append("<section class=\"the-hang\" aria-label=\"Chỉ số lượt chạy mới nhất\">")
                    .append(theSoDan(moiNhat))
                    .append(the("Số kịch bản", String.valueOf(moiNhat.tong()), "trong lượt mới nhất", ""))
                    // Thẻ này phải nói được cả trường hợp cả lượt không chạy được: hiện "0 lỗi /
                    // không có" trong khi 39 kịch bản bị bỏ qua là câu trả lời đúng nhưng gây hiểu
                    // ngược hẳn — người đọc tưởng mọi thứ ổn.
                    .append(moiNhat.thatBai() == 0 && moiNhat.boQua() > 0
                            ? the("Không chạy được", String.valueOf(moiNhat.boQua()),
                                  "kịch bản chưa kiểm được", "xau")
                            : the("Kịch bản lỗi", String.valueOf(moiNhat.thatBai()),
                                  moiNhat.thatBai() > 0 ? "cần xem lại" : "không có",
                                  moiNhat.thatBai() > 0 ? "xau" : "ok"))
                    .append(the("Thời gian chạy", TaoDonReportBuilder.formatDuration(moiNhat.wallMs()),
                            "thực tế từ đầu đến cuối", ""))
                    .append("</section>");
        }

        sb.append("<section class=\"khoi\"><h2>Xu hướng tỉ lệ đạt</h2>");
        if (lichSu.size() < 2) {
            sb.append("<p class=\"khoi-phu\">Cần ít nhất hai lượt chạy để so sánh — hiện mới có ")
                    .append(lichSu.size()).append(".</p>");
        } else {
            sb.append("<p class=\"khoi-phu\">Mỗi điểm là một lượt chạy; di chuột vào điểm để xem chi tiết.</p>")
                    .append(bieuDoXuHuong(lichSu));
        }
        sb.append("</section>");

        sb.append("<section class=\"khoi\" id=\"ds-luot\"><div class=\"khoi-dau\"><h2>Các lượt chạy</h2>")
                .append("<div class=\"loc\">")
                .append("<input id=\"tim\" type=\"search\" placeholder=\"Tìm theo ngày hoặc bộ kiểm thử…\""
                        + " aria-label=\"Tìm lượt chạy theo ngày hoặc bộ kiểm thử\">")
                // Hai ô ngày được JS điền HÔM NAY lúc tải trang, không nhúng sẵn ngày vào HTML:
                // trang dựng hôm nay mà mở ngày mai thì ngày nhúng sẵn đã sai.
                // Chrome hiện ô ngày theo ngôn ngữ trình duyệt chứ không theo lang="vi", nên có máy
                // ra mm/dd/yyyy. Giữ bộ chọn gốc (tiện hơn hẳn tự viết) nhưng nói rõ thứ tự đang
                // dùng, để không ai gõ 02/08 mà tưởng là mùng 2 tháng 8.
                .append("<label>Từ <input id=\"tu\" type=\"date\"></label>")
                .append("<label>Đến <input id=\"den\" type=\"date\"></label>")
                .append("<span class=\"phimtat\" id=\"thutungay\"></span>")
                .append("<button id=\"xoaloc\" type=\"button\" class=\"nut nho\">Xoá lọc</button>")
                .append("<span class=\"phimtat\">bấm <kbd>/</kbd> để tìm</span></div></div>")
                .append("<p id=\"khonghop\" class=\"trong\" hidden>Không có lượt chạy nào khớp bộ lọc."
                        + " <button id=\"xemtatca\" type=\"button\" class=\"nut nho\">Xem tất cả"
                        + " các lượt</button></p>")
                // Một file dữ liệu hỏng thì lượt đó biến mất khỏi danh sách, khỏi biểu đồ và khỏi
                // phép so với lượt trước. Phải nói ra, không được để nó mất im lặng.
                .append(hong.isEmpty() ? ""
                        : "<p class=\"canhbao\">Không đọc được dữ liệu của " + hong.size()
                          + " lượt chạy (" + esc(String.join(", ", hong))
                          + ") — các lượt đó không có mặt trong trang này.</p>")
                .append(lichSu.isEmpty()
                        ? "<p class=\"trong\">Chưa có lượt chạy nào được lưu. Chạy một bộ kiểm thử "
                          + "(<code>.\\scripts\\chay.cmd</code>) rồi mở lại trang này — mỗi lượt sẽ "
                          + "thành một dòng ở đây, dữ liệu nằm trong <code>runs/&lt;mốc&gt;/</code>.</p>"
                        : "")
                .append("<div class=\"ds\">").append(muc).append("</div>")
                .append("<nav class=\"phantrang\" hidden aria-label=\"Phân trang danh sách lượt chạy\">")
                .append("<button id=\"tr-truoc\" type=\"button\" class=\"nut nho\">‹ Trước</button>")
                .append("<span id=\"tr-nhan\" role=\"status\"></span>")
                .append("<button id=\"tr-sau\" type=\"button\" class=\"nut nho\">Sau ›</button>")
                .append("</nav></section>")
                .append("</main>")
                .append("<div id=\"den-anh\" class=\"den-anh\" hidden role=\"dialog\" aria-modal=\"true\""
                        + " aria-label=\"Ảnh chụp màn hình\"><img id=\"anh-to\" alt=\"Ảnh phóng to\">"
                        + "<button id=\"dong-anh\" type=\"button\" aria-label=\"Đóng\">Đóng</button>"
                        + "<nav><button id=\"anh-truoc\" type=\"button\">← Trước</button>"
                        + "<span id=\"anh-dem\"></span>"
                        + "<button id=\"anh-sau\" type=\"button\">Sau →</button></nav></div>")
                .append("<footer class=\"chan\">Sinh tự động sau mỗi lượt chạy · dữ liệu từng lượt lưu ở "
                        + "<code>runs/&lt;mốc&gt;/bao-cao.json</code></footer>")
                .append("<script>").append(js()).append("</script></body></html>");
        return sb.toString();
    }

    /** Số bộ kiểm thử khác với bộ của lượt mới nhất — để nói rõ thẻ chỉ số không bao trùm tất cả. */
    private static int soBoKhac(List<LuotChay> lichSu) {
        if (lichSu.isEmpty()) {
            return 0;
        }
        String boMoi = nz(lichSu.get(lichSu.size() - 1).bo()).trim();
        java.util.Set<String> khac = new java.util.LinkedHashSet<>();
        for (LuotChay l : lichSu) {
            String b = nz(l.bo()).trim();
            if (!b.isEmpty() && !b.equalsIgnoreCase(boMoi)) {
                khac.add(b.toUpperCase(Locale.ROOT));
            }
        }
        return khac.size();
    }

    private static String theSoDan(LuotChay d) {
        // Xanh chỉ khi THẬT SỰ không có gì phải xem lại. Lượt hỏng từ khâu đăng nhập có
        // thatBai = 0 nhưng boQua = 39 — bản cũ tô "0.0%" màu xanh lá cho đúng cái lượt không
        // kiểm được gì cả.
        String cls = (d.thatBai() > 0 || d.boQua() > 0 || d.dat() == 0) ? "xau" : "ok";
        return "<div class=\"the dan\"><div class=\"the-nhan\">Kịch bản đạt</div>"
                + "<div class=\"the-so " + cls + "\">" + d.dat() + "/" + d.tong() + "</div>"
                + "<div class=\"the-phu\">trong lượt này</div></div>";
    }

    private static String the(String nhan, String giaTri, String phu, String cls) {
        return "<div class=\"the\"><div class=\"the-nhan\">" + esc(nhan) + "</div>"
                + "<div class=\"the-so " + cls + "\">" + esc(giaTri) + "</div>"
                + "<div class=\"the-phu\">" + esc(phu) + "</div></div>";
    }

    private static String css() {
        return """
                /* Mỗi màu dùng làm CHỮ đều phải có bậc riêng cho hai chế độ. Bản cũ dùng chung
                   `--mo:#898781` cho cả hai: trên nền sáng nó chỉ đạt 3,4:1 — dưới ngưỡng 4,5:1 —
                   mà đó là màu của TOÀN BỘ chữ phụ (giờ, thời lượng, mã case, nhãn mục gập). Tương
                   tự `--bo` hổ phách làm màu chữ chỉ được 1,7:1 trên nền sáng. */
                :root{color-scheme:light;
                --nen:#f9f9f7;--mat:#fcfcfb;--muc:#0b0b0b;--muc2:#52514e;--mo:#6b6963;
                --vien:rgba(11,11,11,.10);--luoi:#e1e0d9;--truc:#c3c2b7;
                --xanh:#2a78d6;--xanh-mo:rgba(42,120,214,.12);
                --dat:#0ca30c;--loi:#d03b3b;--bo:#fab219;
                --dat-chu:#006300;--loi-chu:#b02525;--bo-chu:#8a5a00;
                --dien:#1d4ed8;--chon:#0e7490;--tai:#7e22ce;--boqua-chu:#b45309}
                :root[data-theme=dark]{color-scheme:dark;
                --nen:#0d0d0d;--mat:#1a1a19;--muc:#fff;--muc2:#c3c2b7;--mo:#9c9a93;
                --vien:rgba(255,255,255,.10);--luoi:#2c2c2a;--truc:#383835;
                --xanh:#3987e5;--xanh-mo:rgba(57,135,229,.16);
                --dat-chu:#4ec94e;--loi-chu:#ff8a8a;--bo-chu:#f5c04a;
                --dien:#8ab4f8;--chon:#5fd0e0;--tai:#d0a2f7;--boqua-chu:#f0b464}
                @media(prefers-color-scheme:dark){:root:not([data-theme=light]){color-scheme:dark;
                --nen:#0d0d0d;--mat:#1a1a19;--muc:#fff;--muc2:#c3c2b7;--mo:#9c9a93;
                --vien:rgba(255,255,255,.10);--luoi:#2c2c2a;--truc:#383835;
                --xanh:#3987e5;--xanh-mo:rgba(57,135,229,.16);
                --dat-chu:#4ec94e;--loi-chu:#ff8a8a;--bo-chu:#f5c04a;
                --dien:#8ab4f8;--chon:#5fd0e0;--tai:#d0a2f7;--boqua-chu:#f0b464}}
                *{box-sizing:border-box}
                body{margin:0;background:var(--nen);color:var(--muc);
                font-family:system-ui,-apple-system,"Segoe UI",sans-serif;font-size:14px;line-height:1.5}
                .dau{display:flex;align-items:center;gap:16px;padding:18px 28px;
                border-bottom:1px solid var(--vien);background:var(--mat)}
                .dau-trai{flex:1}h1{margin:0;font-size:19px;letter-spacing:-.01em}
                .phu{margin:2px 0 0;color:var(--muc2);font-size:13px}
                .nut{border:1px solid var(--vien);background:var(--mat);color:var(--muc2);
                border-radius:7px;padding:7px 13px;font:inherit;font-size:13px;cursor:pointer}
                .nut:hover{color:var(--muc);border-color:var(--truc)}
                .nut.nho{padding:6px 11px;font-size:12.5px}
                .than{max-width:1120px;margin:0 auto;padding:22px 28px 40px}
                .the-dan-nhan{margin:0 0 8px;font-size:12.5px;color:var(--muc2)}
                .the-dan-nhan b{font-weight:660;color:var(--muc)}
                .the-hang{display:grid;grid-template-columns:repeat(auto-fit,minmax(178px,1fr));gap:14px;margin-bottom:18px}
                .the{background:var(--mat);border:1px solid var(--vien);border-radius:11px;padding:15px 17px}
                .the-nhan{color:var(--muc2);font-size:12.5px;font-weight:500}
                .the-so{font-size:28px;font-weight:650;letter-spacing:-.02em;margin-top:3px}
                .the.dan .the-so{font-size:42px;line-height:1.1}
                .the-phu{color:var(--mo);font-size:12px;margin-top:2px}
                .the-so.ok{color:var(--dat-chu)}.the-so.xau{color:var(--loi-chu)}
                .khoi{background:var(--mat);border:1px solid var(--vien);border-radius:11px;
                padding:18px 20px;margin-bottom:16px}
                .khoi h2{margin:0;font-size:15px;font-weight:620}
                .khoi-phu{margin:3px 0 12px;color:var(--mo);font-size:12.5px}
                .khoi-dau{display:flex;gap:14px;align-items:center;flex-wrap:wrap;margin-bottom:12px}
                .khoi-dau h2{flex:1}
                .loc{display:flex;gap:8px;align-items:center;flex-wrap:wrap}
                .loc input{border:1px solid var(--vien);background:var(--nen);color:var(--muc);
                border-radius:7px;padding:6px 10px;font:inherit;font-size:13px}
                .loc label{color:var(--muc2);font-size:12.5px;display:flex;gap:6px;align-items:center}
                .xh{width:100%;height:auto;display:block}
                .luoi{stroke:var(--luoi);stroke-width:1}
                .truc{fill:var(--mo);font-size:11px;font-variant-numeric:tabular-nums}
                .xh-vung{fill:var(--xanh-mo)}
                .xh-duong{fill:none;stroke:var(--xanh);stroke-width:2;stroke-linejoin:round;stroke-linecap:round}
                .xh-diem{fill:var(--xanh);stroke:var(--mat);stroke-width:2;cursor:pointer}
                .xh-diem:hover{r:6.5px}
                /* Bảng màu phân loại theo BỘ kiểm thử — thứ tự cố định, không bao giờ xoay vòng.
                   Bốn bộ (SMOKE/MID/FULL/LOGIN) là toàn bộ số bộ có thể có. */
                .bo0{--sac:var(--xanh)}
                .bo1{--sac:#b8730f}
                .bo2{--sac:#8046c8}
                .bo3{--sac:#0f7d78}
                :root[data-theme=dark] .bo1{--sac:#e0a44a}
                :root[data-theme=dark] .bo2{--sac:#bfa0f0}
                :root[data-theme=dark] .bo3{--sac:#4fc7c0}
                @media(prefers-color-scheme:dark){:root:not([data-theme=light]) .bo1{--sac:#e0a44a}
                :root:not([data-theme=light]) .bo2{--sac:#bfa0f0}
                :root:not([data-theme=light]) .bo3{--sac:#4fc7c0}}
                .xh-duong.bo0,.xh-duong.bo1,.xh-duong.bo2,.xh-duong.bo3{stroke:var(--sac)}
                .xh-diem.bo0,.xh-diem.bo1,.xh-diem.bo2,.xh-diem.bo3{fill:var(--sac)}
                .xh-chu{display:flex;gap:16px;flex-wrap:wrap;margin:8px 0 0;
                font-size:12px;color:var(--muc2)}
                .xh-muc{display:flex;gap:6px;align-items:center}
                .xh-o{width:16px;height:3px;border-radius:2px;background:var(--sac);flex:0 0 auto}
                .xh-nhan{fill:var(--muc);font-size:12.5px;font-weight:640}
                details{margin:0}summary{cursor:pointer;list-style:none;user-select:none}
                summary::-webkit-details-marker{display:none}
                .mui{width:0;height:0;border-left:5px solid var(--mo);border-top:4px solid transparent;
                border-bottom:4px solid transparent;flex:0 0 auto;transition:transform .12s}
                details[open]>summary .mui{transform:rotate(90deg)}
                /* KHÔNG đặt overflow:hidden ở đây. Nó biến `.luot` thành một scrollport, và
                   `position:sticky` bám theo scrollport gần nhất — nên thanh lọc bên trong trôi đi
                   như phần tử tĩnh, đúng thứ nó sinh ra để chống. Cùng luật đó còn cắt cụt bảng dữ
                   liệu rộng mà không để lại thanh cuộn nào để kéo ra xem. */
                .luot{border:1px solid var(--vien);border-radius:10px;margin-bottom:9px;
                background:var(--mat)}
                .luot>summary{border-radius:9px}
                .luot-dau{display:flex;gap:12px;align-items:center;padding:12px 14px;flex-wrap:wrap}
                .luot-dau:hover{background:var(--nen)}
                .luot-ngay{font-weight:640;font-variant-numeric:tabular-nums;min-width:118px}
                .chip{border:1px solid var(--vien);border-radius:999px;padding:1px 9px;
                font-size:11.5px;color:var(--muc2)}
                .pb{display:inline-flex;height:8px;width:140px;border-radius:4px;overflow:hidden;
                background:var(--luoi);gap:2px;flex:0 0 auto}
                .pb .seg{display:block;height:100%}
                .pb .dat{background:var(--dat)}.pb .loi{background:var(--loi)}.pb .bo{background:var(--bo)}
                .pb-nhan{color:var(--muc2);font-size:12.5px;font-variant-numeric:tabular-nums}
                .pb-nhan b{font-weight:640}
                b.ok{color:var(--dat-chu)}b.xau{color:var(--loi-chu)}b.nhat{color:var(--muc2)}
                .cach{color:var(--truc);margin:0 7px}
                .luot-tile{margin-left:auto;font-weight:650;font-variant-numeric:tabular-nums}
                .luot-tg{color:var(--mo);font-size:12.5px;font-variant-numeric:tabular-nums;
                min-width:92px;text-align:right}
                .luot-than{padding:2px 14px 14px}
                .lienket{margin:0 0 10px;font-size:12.5px}
                .mo{color:var(--mo)}
                .case{border:1px solid var(--vien);border-radius:9px;margin-bottom:7px;background:var(--nen)}
                .case-dau{display:flex;gap:10px;align-items:center;padding:9px 12px;flex-wrap:wrap}
                .case-dau:hover{background:var(--mat)}
                .huy{border-radius:5px;padding:1px 8px;font-size:11.5px;font-weight:620;color:#fff;flex:0 0 auto}
                .huy.dat{background:var(--dat)}.huy.loi{background:var(--loi)}
                .huy.bo{background:var(--bo);color:#3a2a00}.huy.khac{background:var(--mo)}
                .ma{font-family:ui-monospace,Consolas,monospace;font-size:12.5px;color:var(--muc2)}
                .tende{flex:1;min-width:190px}
                .case[data-tt="loi"]>summary .ma,.case[data-tt="loi"]>summary .tende{font-weight:700}
                .case-tg{color:var(--mo);font-size:12.5px;font-variant-numeric:tabular-nums}
                .case-than{padding:2px 12px 12px}
                .mota{margin:4px 0 8px;color:var(--muc2);font-size:13px}
                .nhanhang{margin:0 0 10px;display:flex;gap:6px;flex-wrap:wrap}
                .nhan{border:1px solid var(--vien);border-radius:999px;padding:1px 9px;
                font-size:11.5px;color:var(--muc2);background:var(--mat)}
                .ketluan{display:grid;grid-template-columns:auto 1fr;gap:2px 14px;margin:0 0 11px;
                padding:9px 12px;border:1px solid var(--vien);border-radius:8px;background:var(--nen);
                font-size:12.5px}
                .ketluan dt{color:var(--mo);white-space:nowrap}
                .ketluan dd{margin:0;color:var(--muc2)}
                @media(max-width:620px){.ketluan{grid-template-columns:1fr}
                .ketluan dd{margin-bottom:5px}}
                .dai{list-style:none;display:grid;grid-template-columns:repeat(6,1fr);gap:6px;
                margin:0 0 12px;padding:0}
                .o{border:1px solid var(--vien);border-top:2px solid var(--truc);border-radius:5px;
                padding:5px 7px 6px;background:var(--mat);min-width:0}
                .o.dat{border-top-color:var(--dat)}.o.loi{border-top-color:var(--loi)}
                .o.bo{border-top-color:var(--bo)}
                .o-dau{display:flex;justify-content:space-between;align-items:baseline}
                .o-dau b{font-size:12px;color:var(--muc2);font-weight:640}
                .o-kh{font-size:11px;color:var(--mo)}
                .o.dat .o-kh{color:var(--dat-chu)}.o.loi .o-kh{color:var(--loi-chu)}.o.bo .o-kh{color:var(--bo-chu)}
                .o-tt{display:block;font-size:10.5px;color:var(--muc2);margin:1px 0 3px;
                white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
                .o-tg{display:block;font-size:10.5px;color:var(--mo);
                font-variant-numeric:tabular-nums;margin-bottom:4px}
                .o-thanh{display:block;height:3px;border-radius:2px;background:var(--luoi)}
                .o-thanh>i{display:block;height:100%;border-radius:2px;background:var(--xanh);
                min-width:2px}
                @media(max-width:620px){.dai{grid-template-columns:repeat(3,1fr)}}
                .skhop{margin-bottom:8px}
                .skcuoi{margin-top:10px;padding-top:2px;border-top:1px solid var(--vien)}
                .buoc{border-left:2px solid var(--truc);margin:0 0 5px 4px;padding-left:9px}
                .buoc.dat{border-left-color:var(--dat)}.buoc.loi{border-left-color:var(--loi)}
                .buoc.bo{border-left-color:var(--bo)}
                .buoc-dau{display:flex;gap:9px;align-items:center;padding:5px 2px;flex-wrap:wrap}
                .cham{width:7px;height:7px;border-radius:50%;flex:0 0 auto;background:var(--mo)}
                .cham.dat{background:var(--dat)}.cham.loi{background:var(--loi)}.cham.bo{background:var(--bo)}
                .buoc-ten{flex:1;min-width:165px;font-size:13px}
                .buoc-tt{font-size:12px;color:var(--muc2)}
                .buoc-tt.dat{color:var(--dat-chu)}.buoc-tt.loi{color:var(--loi-chu)}.buoc-tt.bo{color:var(--bo-chu)}
                .buoc-thanh{width:72px;height:3px;border-radius:2px;background:var(--luoi);
                flex:0 0 auto}
                .buoc-thanh>i{display:block;height:100%;border-radius:2px;background:var(--xanh)}
                .buoc-tg{color:var(--mo);font-size:12px;font-variant-numeric:tabular-nums;
                min-width:74px;text-align:right}
                .buoc-than{padding:3px 0 8px 16px}
                .sk{display:flex;gap:10px;align-items:flex-start;padding:5px 0;font-size:12.5px;
                border-top:1px solid var(--vien)}
                .sk:first-child{border-top:none}
                .sk-gio{color:var(--mo);font-variant-numeric:tabular-nums;flex:0 0 auto}
                .sk-noidung{flex:1;white-space:pre-wrap;word-break:break-word}
                .sk-fail .sk-noidung{color:var(--loi-chu)}
                .sk-pass .sk-noidung{color:var(--dat-chu)}
                .sk-warn .sk-noidung,.sk-note .sk-noidung{color:var(--muc2)}
                .sk-anh{border:0;background:none;padding:0;cursor:zoom-in;flex:0 0 auto}
                .sk-anh img{width:100px;border:1px solid var(--vien);border-radius:5px;display:block}
                .sk-anh:hover img{border-color:var(--xanh)}
                .sosanh{margin:0 0 10px;font-size:13px}
                .sosanh b{font-weight:660}
                .gomloi{border:1px solid var(--vien);border-radius:9px;margin:0 0 12px;overflow:hidden}
                .gomloi-dau{background:var(--nen);padding:7px 12px;font-size:12.5px;font-weight:640;
                border-bottom:1px solid var(--vien)}
                .gomloi-dong{display:flex;gap:12px;align-items:baseline;padding:7px 12px;
                border-top:1px solid var(--vien);font-size:12.5px}
                /* `:first-of-type` xét theo TÊN THẺ, không theo class — con div đầu của .gomloi là
                   .gomloi-dau nên dòng lỗi đầu tiên không bao giờ khớp, để lại hai đường kẻ sát
                   nhau ngay dưới tiêu đề. Dùng bộ chọn anh-em liền kề mới đúng. */
                .gomloi-dau+.gomloi-dong{border-top:none}
                /* Khối "bỏ qua" phải trông khác khối lỗi: hai chuyện khác hẳn nhau. */
                .gom-bo .gomloi-so{color:var(--bo-chu)}
                .gom-bo .gomloi-dau{color:var(--muc2)}
                .gomloi-so{min-width:30px;text-align:right;font-weight:700;color:var(--loi-chu);
                font-variant-numeric:tabular-nums}
                .gomloi-msg{flex:1}
                .gomloi-ma{color:var(--mo);font-family:ui-monospace,Consolas,monospace;font-size:11.5px}
                .loc-case{display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin:0 0 9px}
                .loc-case .tim-case{flex:1;min-width:200px;border:1px solid var(--vien);
                background:var(--mat);color:var(--muc);border-radius:7px;padding:6px 10px;font:inherit;font-size:12.5px}
                .chon{display:flex;gap:6px;align-items:center;color:var(--muc2);font-size:12.5px;cursor:pointer}
                .dem-case{font-size:12.5px}
                /* Huy hiệu so-với-lượt-trước. Tên phải KHÁC `.dau` của <header> — trước đây trùng
                   tên, và vì luật này đứng sau nên nó đè padding 18px của header xuống 1px và xoá
                   luôn đường phân cách. Header bẹp dí ở mọi bề rộng trên 760px. */
                .nhandau{border-radius:5px;padding:1px 7px;font-size:11px;font-weight:640;
                flex:0 0 auto;border:1px solid transparent}
                .nhandau-moihong{background:var(--loi);color:#fff}
                .nhandau-dasua{background:var(--dat);color:#fff}
                .nhandau-vanhong{border-color:var(--vien);color:var(--muc2)}
                .nhandau-moi{border-color:var(--xanh);color:var(--xanh)}
                .dulieu{margin:6px 0 2px}
                .dulieu>summary{color:var(--mo);font-size:12px;padding:3px 0}
                /* Nội dung rộng phải cuộn TRONG khung của chính nó, không đẩy cả trang trượt ngang. */
                .cuon-ngang{overflow-x:auto;max-width:100%}
                .bang-dl{width:100%;border-collapse:collapse;font-size:12px;margin-top:4px}
                .bang-dl th,.bang-dl td{border:1px solid var(--vien);padding:4px 8px;text-align:left;
                vertical-align:top;overflow-wrap:anywhere}
                .bang-dl th{background:var(--nen);color:var(--muc2);font-weight:600}
                .bang-dl td:first-child{width:30%;color:var(--muc2)}
                .bang-dl td:nth-child(2){width:12%;font-weight:620}
                .tt-dien{color:var(--dien)}
                .tt-chon{color:var(--chon)}
                .tt-tai{color:var(--tai)}
                .tt-boqua{color:var(--boqua-chu)}
                .tt-loi{color:var(--loi-chu)}
                .tt-khac{color:var(--muc2)}
                .chandoan{margin-top:4px}
                .chandoan>summary{color:var(--mo);font-size:12px;padding:4px 0}
                .vet{margin-top:8px}
                .vet>summary{color:var(--muc2);font-size:12.5px}
                .vet pre{background:var(--nen);border:1px solid var(--vien);border-radius:7px;
                padding:10px;overflow:auto;font-size:11.5px;max-height:280px;margin:6px 0 0}
                .trong{color:var(--mo);font-size:12.5px;margin:6px 0}
                .phantrang{display:flex;gap:12px;align-items:center;justify-content:center;
                margin-top:12px;padding-top:10px;border-top:1px solid var(--vien)}
                .phantrang[hidden]{display:none}
                #tr-nhan{font-size:12.5px;color:var(--muc2);font-variant-numeric:tabular-nums}
                .phantrang .nut[disabled]{opacity:.4;cursor:default}
                .canhbao{margin:0 0 10px;padding:8px 12px;border-radius:8px;font-size:12.5px;
                border:1px solid var(--bo);background:var(--nen);color:var(--muc2)}
                .luot-gon>summary{opacity:.72}
                .chan{max-width:1120px;margin:0 auto;padding:0 28px 28px;color:var(--mo);font-size:12px}
                code{font-family:ui-monospace,Consolas,monospace;font-size:11.5px}
                .den-anh{position:fixed;inset:0;background:rgba(0,0,0,.82);display:flex;
                align-items:center;justify-content:center;z-index:50;padding:28px}
                /* Luật class thắng thuộc tính hidden của trình duyệt, nên phải nói rõ —
                   thiếu dòng này thì lớp phóng ảnh mở sẵn ngay khi tải trang. */
                .den-anh[hidden]{display:none}
                .den-anh img{max-width:100%;max-height:100%;border-radius:6px;box-shadow:0 8px 40px rgba(0,0,0,.5)}
                .den-anh button{position:absolute;top:18px;right:20px;border:1px solid rgba(255,255,255,.3);
                background:rgba(0,0,0,.5);color:#fff;border-radius:7px;padding:7px 14px;font:inherit;cursor:pointer}
                @media(max-width:760px){.luot-tg,.case-tg,.buoc-tg,.buoc-thanh{display:none}.pb{width:92px}
                .than{padding:16px 14px 32px}.dau{padding:14px 16px}}

                /* Dòng nhóm lỗi bấm được để lọc — nói rõ nó là nút, không phải chữ tĩnh. */
                button.gomloi-msg{border:0;background:none;font:inherit;text-align:left;
                cursor:pointer;padding:0;color:inherit;border-bottom:1px dashed transparent}
                button.gomloi-msg:hover{border-bottom-color:var(--truc)}
                button.gomloi-msg.dangloc{color:var(--xanh);border-bottom-color:var(--xanh)}
                @media(hover:hover){
                  .chep-loi{opacity:0}
                  .gomloi-dong:hover .chep-loi,.chep-loi:focus-visible{opacity:1}
                }
                /* Phễu 6 bước: vừa là chú giải "bước N là gì", vừa cho thấy kịch bản rụng ở đâu. */
                .luong{border:1px solid var(--vien);border-radius:9px;margin:0 0 12px;
                background:var(--mat)}
                .luong-dau{background:var(--nen);padding:7px 12px;font-size:12.5px;font-weight:640;
                border-bottom:1px solid var(--vien);border-radius:8px 8px 0 0}
                .luong-ds{list-style:none;margin:0;padding:4px 0}
                .luong-ds li{display:flex;gap:10px;align-items:center;padding:4px 12px;font-size:12.5px}
                .luong-so{width:18px;height:18px;border-radius:50%;background:var(--luoi);
                color:var(--muc2);font-size:11px;font-weight:660;display:flex;
                align-items:center;justify-content:center;flex:0 0 auto}
                .luong-ten{flex:1;min-width:150px;color:var(--muc2)}
                .luong-thanh{width:120px;height:5px;border-radius:3px;background:var(--luoi);
                flex:0 0 auto}
                .luong-thanh>i{display:block;height:100%;border-radius:3px;background:var(--xanh)}
                .luong-qua{min-width:64px;text-align:right;font-variant-numeric:tabular-nums;
                color:var(--muc2)}
                .luong-rot{min-width:92px;text-align:right;color:var(--loi-chu);font-weight:620}
                .luong-rot.mo{color:var(--mo);font-weight:400}
                @media(max-width:760px){.luong-thanh{display:none}.luong-rot{min-width:0}}
                .phu-khoi{border:1px solid var(--vien);border-radius:8px;padding:8px 12px;
                margin:0 0 10px;background:var(--nen)}
                .phu-khoi>summary{font-size:12.5px;color:var(--muc2);display:flex;gap:8px;
                align-items:center;cursor:pointer}
                .phu-ds{margin:4px 0 2px;padding-left:20px;font-size:12px;color:var(--muc2);
                columns:2;column-gap:24px}
                @media(max-width:620px){.phu-ds{columns:1}}

                /* ── Điều hướng khi danh sách dài ─────────────────────────── */
                /* Thanh lọc dính mép trên: với 39 kịch bản, cuộn xuống là mất ô tìm kiếm. */
                .loc-case{position:sticky;top:0;z-index:3;background:var(--mat);
                padding:7px 0;margin-bottom:4px;border-bottom:1px solid var(--vien)}
                /* Nút chép phải NHÌN THẤY ĐƯỢC trên thiết bị cảm ứng. Bản cũ để opacity:0 và chỉ
                   hiện khi hover — trên iPad/laptop cảm ứng không có hover, nên nút vô hình vẫn
                   chiếm chỗ giữa tiêu đề và cột thời gian: chạm trúng thì case không mở, không có
                   phản hồi nào. Chỉ ẩn-mờ ở nơi thật sự có hover. */
                .chep{border:0;background:none;color:var(--mo);cursor:pointer;font:inherit;
                font-size:12px;padding:2px 6px;border-radius:5px;opacity:.55;transition:opacity .12s}
                .chep:hover{background:var(--xanh-mo);color:var(--xanh)}
                .chep.xong{opacity:1;color:var(--dat-chu)}
                .chep.hong{opacity:1;color:var(--loi-chu)}
                @media(hover:hover){
                  .chep{opacity:0}
                  .case-dau:hover .chep,.chep:focus-visible,.chep.xong,.chep.hong{opacity:1}
                }
                /* Kịch bản vừa mở bằng liên kết sâu — nháy lên để mắt bắt được nó. */
                @keyframes nhay{from{background:var(--xanh-mo)}to{background:transparent}}
                .case:target{animation:nhay 1.6s ease-out}
                .case:target>.case-dau{border-radius:6px}

                /* ── Tiếp cận bằng bàn phím ────────────────────────────────── */
                :focus-visible{outline:2px solid var(--xanh);outline-offset:2px}
                .boqua-toi{position:absolute;left:-9999px;top:0;background:var(--mat);
                color:var(--muc);padding:10px 16px;border:1px solid var(--truc);border-radius:0 0 8px 0;z-index:60}
                .boqua-toi:focus{left:0}
                .phimtat{color:var(--mo);font-size:11.5px;white-space:nowrap}
                kbd{border:1px solid var(--truc);border-bottom-width:2px;border-radius:4px;
                padding:0 4px;font-family:inherit;font-size:11px;color:var(--muc2)}

                /* ── Xem ảnh: điều hướng trước/sau ─────────────────────────── */
                .den-anh nav{position:absolute;bottom:20px;left:50%;transform:translateX(-50%);
                display:flex;gap:10px;align-items:center;color:#fff;font-size:12.5px}
                .den-anh nav button{position:static;padding:6px 13px}
                .den-anh nav button[disabled]{opacity:.35;cursor:default}
                #anh-dem{font-variant-numeric:tabular-nums;min-width:56px;text-align:center}

                /* ── Kiểu in / xuất PDF ────────────────────────────────────── */
                /* Sếp hay bấm Ctrl-P. Bung sẵn mọi mục, bỏ nút bấm, cho ảnh vừa khổ giấy. */
                @media print{
                  /* Phải liệt kê CẢ HAI bộ chọn của giao diện tối. `@media` không cộng thêm độ đặc
                     hiệu, nên `:root` trần (0,1,0) thua `:root[data-theme=dark]` (0,2,0) — hậu quả:
                     ai đang xem giao diện tối (kể cả chỉ vì Windows để chế độ tối và chưa bấm nút
                     bao giờ) mà bấm Ctrl-P thì được chữ trắng trên giấy trắng: một tờ trống. */
                  :root,:root[data-theme=dark],:root:not([data-theme=light]){
                  color-scheme:light;
                  --nen:#fff;--mat:#fff;--muc:#000;--muc2:#333;--mo:#555;
                  --vien:#bbb;--luoi:#ddd;--truc:#999;
                  --dat-chu:#0a6b0a;--loi:#b32020;--xanh:#1c5aa8}
                  .nut,.loc,.loc-case,.chep,.den-anh,.boqua-toi,.phimtat{display:none!important}
                  /* Việc bung sẵn mọi mục do JS xử lý ở sự kiện beforeprint — không có thuộc tính
                     CSS nào mở được <details>, trình duyệt ẩn phần thân ở tầng dựng hình. */
                  details>summary{list-style:none}
                  .mui{display:none}
                  .khoi,.the{break-inside:avoid;box-shadow:none}
                  .case,.buoc{break-inside:avoid}
                  .sk-anh img{max-width:12cm}
                  .vet pre{max-height:none;white-space:pre-wrap}
                  a[href^="runs/"]::after{content:" (" attr(href) ")";font-size:10px;color:#666}
                  body{font-size:11px}
                }
                """;
    }

    private static String js() {
        return """
                (function(){
                  // Bỏ dấu tiếng Việt — khớp với hàm cùng tên phía Java dựng chỉ mục tìm kiếm.
                  function boDau(s){
                    return (s||'').normalize('NFD').replace(/[\\u0300-\\u036f]/g,'')
                      .replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase().trim();
                  }

                  var root=document.documentElement,nut=document.getElementById('doigiaodien');
                  // Chế độ ĐANG hiển thị, không phải chế độ đã lưu. Máy để Windows chế độ tối mà
                  // chưa bấm nút bao giờ thì trang đang tối nhưng data-theme rỗng — bản cũ ghi
                  // "Giao diện tối", bấm vào không đổi gì, người dùng kết luận nút hỏng.
                  function dangToi(){
                    var t=root.getAttribute('data-theme');
                    if(t) return t==='dark';
                    try{return window.matchMedia('(prefers-color-scheme:dark)').matches;}catch(e){return false;}
                  }
                  function nhan(){nut.textContent=dangToi()?'Giao diện sáng':'Giao diện tối';}
                  try{var luu=localStorage.getItem('toaan-giaodien');if(luu)root.setAttribute('data-theme',luu);}catch(e){}
                  nhan();
                  nut.addEventListener('click',function(){
                    var m=dangToi()?'light':'dark';
                    root.setAttribute('data-theme',m);nhan();
                    try{localStorage.setItem('toaan-giaodien',m);}catch(e){}
                  });
                  var tim=document.getElementById('tim'),tu=document.getElementById('tu'),
                      den=document.getElementById('den'),bao=document.getElementById('khonghop');

                  // Ô <input type=date> hiện theo ngôn ngữ TRÌNH DUYỆT, không theo lang="vi" của
                  // trang — máy cài tiếng Anh sẽ ra mm/dd/yyyy. Hỏi thẳng trình duyệt thứ tự thật
                  // rồi ghi ra, thay vì đoán hoặc tự viết lại bộ chọn ngày.
                  (function(){
                    var nhan=document.getElementById('thutungay');
                    try{
                      var mau=new Intl.DateTimeFormat(navigator.language,
                        {year:'numeric',month:'2-digit',day:'2-digit'})
                        .formatToParts(new Date(2026,10,25))
                        .filter(function(p){return p.type!=='literal';})
                        .map(function(p){return p.type==='day'?'ngày':p.type==='month'?'tháng':'năm';})
                        .join('/');
                      nhan.textContent='thứ tự '+mau;
                    }catch(e){ nhan.textContent='thứ tự theo ngôn ngữ trình duyệt'; }
                  })();
                  // ── Lọc + phân trang danh sách lượt chạy ────────────────────
                  var MOI_TRANG=10, trangHienTai=1,
                      dsTrang=document.querySelector('.phantrang'),
                      nhanTrang=document.getElementById('tr-nhan'),
                      nutTruoc=document.getElementById('tr-truoc'),
                      nutSau=document.getElementById('tr-sau');

                  /** Lọc trước, phân trang sau — phân trang luôn áp lên KẾT QUẢ đã lọc. */
                  function loc(giuTrang){
                    if(!giuTrang) trangHienTai=1;
                    var q=boDau(tim.value),a=tu.value,b=den.value,khop=[];
                    document.querySelectorAll('details.luot').forEach(function(d){
                      var n=d.dataset.ngay||'',t=d.dataset.tim||'';
                      if((!q||t.indexOf(q)>=0)&&(!a||n>=a)&&(!b||n<=b)) khop.push(d);
                      d.hidden=true;
                    });
                    var soTrang=Math.max(1,Math.ceil(khop.length/MOI_TRANG));
                    if(trangHienTai>soTrang) trangHienTai=soTrang;
                    var dau=(trangHienTai-1)*MOI_TRANG;
                    khop.slice(dau,dau+MOI_TRANG).forEach(function(d){d.hidden=false;});

                    bao.hidden=khop.length>0;
                    dsTrang.hidden=khop.length<=MOI_TRANG;
                    nhanTrang.textContent='Trang '+trangHienTai+'/'+soTrang
                      +' — '+khop.length+' lượt chạy khớp';
                    nutTruoc.disabled=trangHienTai<=1;
                    nutSau.disabled=trangHienTai>=soTrang;
                  }
                  [tim,tu,den].forEach(function(el){el.addEventListener('input',function(){loc();});});
                  nutTruoc.addEventListener('click',function(){
                    if(trangHienTai>1){trangHienTai--;loc(true);
                      document.getElementById('ds-luot').scrollIntoView({block:'start'});}
                  });
                  nutSau.addEventListener('click',function(){
                    trangHienTai++;loc(true);
                    document.getElementById('ds-luot').scrollIntoView({block:'start'});
                  });
                  function boLocNgay(){tim.value='';tu.value='';den.value='';loc();}
                  document.getElementById('xoaloc').addEventListener('click',boLocNgay);
                  document.getElementById('xemtatca').addEventListener('click',boLocNgay);

                  // Mở trang là thấy HÔM NAY. Điền bằng JS chứ không nhúng sẵn vào HTML: trang
                  // dựng hôm nay mà mở ngày mai thì ngày nhúng sẵn đã sai.
                  (function(){
                    var h=new Date();
                    var s=h.getFullYear()+'-'+String(h.getMonth()+1).padStart(2,'0')
                          +'-'+String(h.getDate()).padStart(2,'0');
                    tu.value=s; den.value=s;
                    loc();
                    // Hôm nay chưa chạy gì thì đừng để người đọc nhìn một trang trống mà không
                    // hiểu vì sao — nút "Xem tất cả các lượt" nằm ngay trong dòng thông báo.
                  })();
                  // MỘT hàm lọc duy nhất cho mỗi lượt, đọc cả ba nguồn điều kiện.
                  //
                  // Bản cũ có ba đoạn code cùng gán thẳng `c.hidden`: ô tìm, checkbox, và nút nhóm
                  // lỗi — không đoạn nào biết đoạn nào. Bấm một nhóm lỗi rồi gõ vào ô tìm là bộ lọc
                  // nhóm biến mất trong khi dòng nhóm lỗi VẪN sáng xanh; bấm lại dòng đó thì nó
                  // tắt thay vì bật; và tick "chỉ hiện case lỗi" xong bấm nhóm lỗi hai lần là
                  // checkbox kẹt ở trạng thái tick mà toàn bộ case Đạt hiện ra. Giờ mọi nút chỉ đổi
                  // TRẠNG THÁI, còn quyết định ẩn/hiện chỉ có ở đây.
                  function apDungLoc(luot){
                    var o=luot.querySelector('.tim-case'), chi=luot.querySelector('.chi-loi'),
                        dem=luot.querySelector('.dem-case'),
                        nhom=luot.querySelector('.gomloi-msg.dangloc');
                    if(!o) return;
                    var q=boDau(o.value), chiLoi=chi.checked,
                        dsMa=nhom?(nhom.dataset.locMa||'').split('|').filter(Boolean):null,
                        hien=0, tong=0;
                    luot.querySelectorAll('details.case').forEach(function(c){
                      tong++;
                      var ok=true;
                      if(q && (c.dataset.tim||'').indexOf(q)<0) ok=false;
                      if(chiLoi && c.dataset.tt==='dat') ok=false;
                      if(dsMa && dsMa.indexOf((c.dataset.ma||'').toLowerCase())<0) ok=false;
                      c.hidden=!ok; if(ok) hien++;
                    });
                    var loc=[];
                    if(q) loc.push('từ khoá');
                    if(chiLoi) loc.push('chỉ case lỗi');
                    if(dsMa) loc.push('một nhóm lỗi');
                    dem.textContent=(hien===tong&&!loc.length)?''
                      :('hiện '+hien+'/'+tong+' kịch bản — lọc theo '+loc.join(' + '));
                    var go=luot.querySelector('.bo-loc-case');
                    if(go) go.hidden=!loc.length;
                  }
                  document.querySelectorAll('details.luot').forEach(function(luot){
                    var o=luot.querySelector('.tim-case'), chi=luot.querySelector('.chi-loi');
                    if(!o) return;
                    var chay=function(){apDungLoc(luot);};
                    o.addEventListener('input',chay);
                    chi.addEventListener('change',chay);
                    var go=luot.querySelector('.bo-loc-case');
                    if(go) go.addEventListener('click',function(){
                      o.value=''; chi.checked=false;
                      luot.querySelectorAll('.gomloi-msg').forEach(function(x){x.classList.remove('dangloc');});
                      apDungLoc(luot);
                    });
                  });

                  // Mở tất cả / Thu gọn — chỉ tác động lên các kịch bản ĐANG hiện, để nút này
                  // phối hợp được với bộ lọc thay vì bung cả những case vừa lọc đi.
                  document.querySelectorAll('.bung').forEach(function(nut){
                    nut.addEventListener('click',function(){
                      var mo=nut.dataset.bung==='1', luot=nut.closest('details.luot');
                      luot.querySelectorAll('details.case').forEach(function(c){
                        if(c.hidden) return;
                        c.open=mo;
                        if(!mo) c.querySelectorAll('details').forEach(function(x){x.open=false;});
                      });
                    });
                  });

                  // Chép: liên kết sâu tới kịch bản (data-neo) hoặc thông báo lỗi (data-chep).
                  function chepBangTextarea(txt,xong,hong){
                    var t=document.createElement('textarea');t.value=txt;
                    t.style.position='fixed';t.style.opacity='0';
                    document.body.appendChild(t);t.select();
                    var ok=false;
                    try{ok=document.execCommand('copy');}catch(err){}
                    document.body.removeChild(t);
                    if(ok) xong(); else hong();
                  }
                  function chepVaoClipboard(txt,xong,hong){
                    if(navigator.clipboard&&navigator.clipboard.writeText){
                      // Nhánh reject PHẢI rơi về execCommand. Bản cũ bỏ trống nhánh này, nên khi
                      // trình duyệt từ chối (mất focus, chính sách) thì người dùng bấm, không thấy
                      // gì, dán ra rỗng và không hiểu vì sao.
                      navigator.clipboard.writeText(txt).then(xong,function(){
                        chepBangTextarea(txt,xong,hong);
                      });
                      return;
                    }
                    chepBangTextarea(txt,xong,hong);
                  }
                  document.addEventListener('click',function(e){
                    var b=e.target.closest('.chep');
                    if(!b) return;
                    e.preventDefault(); e.stopPropagation();
                    // Bấm nhanh hai lần: bản cũ chụp textContent ở đầu handler nên lần thứ hai
                    // chụp phải dấu ✓ rồi khôi phục về ✓ — nút kẹt vĩnh viễn. Giữ ký tự gốc trong
                    // dataset, và bỏ qua nếu đang trong nhịp báo hiệu.
                    if(b.dataset.dangBao==='1') return;
                    if(!b.dataset.goc) b.dataset.goc=b.textContent;
                    var goc=b.dataset.goc,
                        txt=b.dataset.chep||(location.href.split('#')[0]+'#'+b.dataset.neo);
                    var baoHieu=function(ky,lop){
                      b.dataset.dangBao='1';
                      b.classList.add(lop); b.textContent=ky;
                      setTimeout(function(){
                        b.classList.remove(lop); b.textContent=goc; b.dataset.dangBao='0';
                      },1400);
                    };
                    chepVaoClipboard(txt,function(){baoHieu('✓','xong');},
                                         function(){baoHieu('✕','hong');});
                  });

                  // Bấm một nhóm lỗi chỉ ĐỔI TRẠNG THÁI; việc ẩn/hiện do apDungLoc quyết định.
                  document.addEventListener('click',function(e){
                    var b=e.target.closest('.gomloi-msg');
                    if(!b) return;
                    var luot=b.closest('details.luot'), bat=!b.classList.contains('dangloc');
                    luot.querySelectorAll('.gomloi-msg').forEach(function(x){x.classList.remove('dangloc');});
                    if(bat) b.classList.add('dangloc');
                    apDungLoc(luot);
                  });

                  // Mở trang kèm neo (#c-<mốc>-<mã case>) thì bung đúng kịch bản đó và cuộn tới.
                  // PHẢI gỡ cả `hidden`: phần tử ẩn không có hộp bố cục nên scrollIntoView không
                  // làm gì cả — người nhận link bấm vào thấy trang đứng yên, tưởng link hỏng.
                  function moTheoNeo(){
                    if(!location.hash) return;
                    var el=document.getElementById(location.hash.slice(1));
                    if(!el) return;
                    for(var n=el;n;n=n.parentElement){
                      if(n.nodeType!==1) break;
                      if(n.hidden) n.hidden=false;
                      if(n.tagName==='DETAILS') n.open=true;
                    }
                    // Ảnh dùng loading=lazy nên nội dung phía trên còn nở ra sau khi cuộn; cuộn
                    // lại một nhịp nữa để case đích không trôi khỏi màn hình.
                    el.scrollIntoView({block:'center'});
                    setTimeout(function(){el.scrollIntoView({block:'center'});},350);
                  }
                  moTheoNeo();
                  window.addEventListener('hashchange',moTheoNeo);

                  // Phím tắt: '/' nhảy vào ô tìm kiếm.
                  document.addEventListener('keydown',function(e){
                    if(e.key!=='/'||e.ctrlKey||e.altKey||e.metaKey) return;
                    var t=e.target.tagName;
                    if(t==='INPUT'||t==='TEXTAREA'||t==='SELECT') return;
                    e.preventDefault(); tim.focus(); tim.select();
                  });

                  // In / xuất PDF: cố ý in ĐÚNG những gì đang thấy trên màn hình.
                  // Bản đầu bung sẵn mọi mục trước khi in — kết quả là 458 trang và 31 MB PDF cho
                  // 7 lượt chạy, vì nó kéo theo cả bảng dữ liệu từng trường, dòng đo lường kỹ thuật
                  // và mọi ảnh chụp. Mở trang rồi bấm in ngay bây giờ cho đúng một tờ tóm tắt:
                  // chỉ số, xu hướng, và một dòng cho mỗi lượt chạy. Muốn in sâu hơn thì bung đúng
                  // phần cần rồi mới in — không có gì phải học, và không ai in nhầm 458 trang.

                  // ── Xem ảnh phóng to, đi được trước/sau bằng phím mũi tên ──
                  var den2=document.getElementById('den-anh'),anhTo=document.getElementById('anh-to'),
                      truoc=document.getElementById('anh-truoc'),sau=document.getElementById('anh-sau'),
                      demAnh=document.getElementById('anh-dem'),dsAnh=[],viTri=-1;
                  function veAnh(){
                    if(viTri<0||viTri>=dsAnh.length) return;
                    var b=dsAnh[viTri];
                    anhTo.src=b.dataset.anh;
                    anhTo.alt=b.getAttribute('aria-label')||'Ảnh chụp màn hình';
                    demAnh.textContent=(viTri+1)+' / '+dsAnh.length;
                    truoc.disabled=viTri===0; sau.disabled=viTri===dsAnh.length-1;
                  }
                  function di(b){
                    // Danh sách ảnh lấy trong phạm vi CÙNG kịch bản — đi tiếp sang case khác thì
                    // người xem mất dấu mình đang ở đâu.
                    var goc=b.closest('details.case')||document;
                    dsAnh=Array.prototype.slice.call(goc.querySelectorAll('.sk-anh'));
                    viTri=dsAnh.indexOf(b);
                    den2.hidden=false; veAnh();
                  }
                  function dong(){den2.hidden=true;anhTo.removeAttribute('src');dsAnh=[];viTri=-1;}
                  document.addEventListener('click',function(e){
                    var b=e.target.closest('.sk-anh');
                    if(b){di(b);return;}
                    if(e.target===truoc){if(viTri>0){viTri--;veAnh();}return;}
                    if(e.target===sau){if(viTri<dsAnh.length-1){viTri++;veAnh();}return;}
                    if(e.target===den2||e.target.id==='dong-anh')dong();
                  });
                  document.addEventListener('keydown',function(e){
                    if(e.key==='Escape'){dong();return;}
                    if(den2.hidden) return;
                    if(e.key==='ArrowLeft'&&viTri>0){e.preventDefault();viTri--;veAnh();}
                    if(e.key==='ArrowRight'&&viTri<dsAnh.length-1){e.preventDefault();viTri++;veAnh();}
                  });
                })();
                """;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}

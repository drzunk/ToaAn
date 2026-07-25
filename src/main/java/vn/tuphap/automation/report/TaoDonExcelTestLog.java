package vn.tuphap.automation.report;

import vn.tuphap.automation.pages.NguyenDonPage;

import vn.tuphap.automation.pages.TaiLieuPage;

import vn.tuphap.automation.pages.NoiDungDonPage;

import vn.tuphap.automation.pages.LoginPage;

import vn.tuphap.automation.pages.TaoDonPage;

import vn.tuphap.automation.pages.BiDonPage;

import vn.tuphap.automation.pages.DashboardPage;

import vn.tuphap.automation.pages.XemLaiGuiDonPage;

import vn.tuphap.automation.data.TaoDonScenario;

import vn.tuphap.automation.data.MasterDataCatalog;

import vn.tuphap.automation.data.DataDictionary;

import vn.tuphap.automation.config.ConfigReader;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ColorScaleFormatting;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Xuất nhật ký kiểm thử Excel theo suite: smoke / mid / full / login.
 * <p>
 * Sheet {@code Thông tin}: bảng tóm tắt KPI, biểu đồ trạng thái, mục lục liên kết.<br>
 * Sheet {@code Độ phủ}: ma trận nhánh luồng đã chạy (đầy đủ cặp đôi mức B).<br>
 * Sheet {@code Tổng hợp}: master/detail thu gọn; Thời gian dạng số + màu nhiệt.
 */
public final class TaoDonExcelTestLog {

    /** Trạng thái ghi vào Excel — toàn bộ tiếng Việt. */
    public static final String ST_DAT = "Đạt";
    public static final String ST_THAT_BAI = "Thất bại";
    public static final String ST_BO_QUA = "Bỏ qua";
    public static final String ST_DAT_CANH_BAO = "Đạt — cảnh báo";

    private static final String SHEET_DO_PHU = "Độ phủ";

    public enum SuiteKind {
        SMOKE("smoke", "SMOKE", "Nhật ký kiểm thử — Mẫu nhanh (tạo đơn)"),
        MID("mid", "MID", "Nhật ký kiểm thử — Trung bình (35+4)"),
        FULL("full", "FULL", "Nhật ký kiểm thử — Đầy đủ cặp đôi (mức B)"),
        LOGIN("login", "LOGIN", "Nhật ký kiểm thử — Đăng nhập");

        private final String folder;
        private final String fileTag;
        private final String titleVi;

        SuiteKind(String folder, String fileTag, String titleVi) {
            this.folder = folder;
            this.fileTag = fileTag;
            this.titleVi = titleVi;
        }

        public String folder() {
            return folder;
        }

        public String fileTag() {
            return fileTag;
        }

        public String titleVi() {
            return titleVi;
        }

        public static SuiteKind fromSuiteName(String suiteName) {
            if (suiteName == null) {
                return FULL;
            }
            String n = suiteName.toLowerCase();
            if (n.contains("smoke")) {
                return SMOKE;
            }
            if (n.contains("login") || n.contains("đăng nhập")) {
                return LOGIN;
            }
            if (n.contains("mid")) {
                return MID;
            }
            if (n.contains("full")) {
                return FULL;
            }
            String prop = System.getProperty("taodon.suite", "");
            if ("smoke".equalsIgnoreCase(prop)) {
                return SMOKE;
            }
            if ("mid".equalsIgnoreCase(prop)) {
                return MID;
            }
            if ("login".equalsIgnoreCase(prop)) {
                return LOGIN;
            }
            return FULL;
        }
    }

    private static final int SHEET_COLS = 9;
    private static final int COL_STATUS = 5;
    private static final int COL_DURATION = 7;
    /** Độ rộng cột chữ dài — tối đa ~40 ký tự (user request). */
    private static final int MAX_TEXT_CHARS = 40;
    private static final int[] SUMMARY_CHARS = {20, 18, 40, 40, 40, 14, 40, 12, 8};
    private static final int[] DETAIL_CHARS = {20, 10, 40, 40, 40};

    private static final Object LOCK = new Object();
    private static TaoDonExcelTestLog INSTANCE;

    private static final ThreadLocal<TaoDonScenario> CURRENT_SCENARIO = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TITLE = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_DESC = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_EXPECTED = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ACTUAL = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_STATUS = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_NOTE = new ThreadLocal<>();

    private final Path outputFile;
    private final List<LogEntry> entries = new ArrayList<>();
    private final AtomicInteger sheetSeq = new AtomicInteger(0);
    private final String suiteLabel;
    private final SuiteKind kind;
    private final String startedAt;

    private TaoDonExcelTestLog(String suiteLabel, SuiteKind kind) {
        this.suiteLabel = suiteLabel == null || suiteLabel.isBlank() ? kind.titleVi() : suiteLabel;
        this.kind = kind;
        this.startedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        this.outputFile = Paths.get(
                "test-output", "testlogs_" + kind.folder(),
                "TestLogs_" + kind.fileTag() + "_" + stamp + ".xlsx");
    }

    public static void initSuite(String suiteName) {
        SuiteKind kind = SuiteKind.fromSuiteName(suiteName);
        System.setProperty("taodon.suite", kind.folder());
        synchronized (LOCK) {
            INSTANCE = new TaoDonExcelTestLog(suiteName, kind);
        }
        TestActionLog.clear();
    }

    public static SuiteKind currentKind() {
        synchronized (LOCK) {
            return INSTANCE == null ? SuiteKind.FULL : INSTANCE.kind;
        }
    }

    public static void bindScenario(TaoDonScenario scenario) {
        clearThreadLocals();
        CURRENT_SCENARIO.set(scenario);
        if (scenario != null) {
            CURRENT_TITLE.set(TaoDonReportBuilder.buildTestTitle(scenario));
            CURRENT_DESC.set(TaoDonReportBuilder.buildTestDescription(scenario));
            CURRENT_EXPECTED.set(defaultExpectedTaoDon(scenario));
        }
    }

    public static void bindLoginCase(String tenDangNhap, String matKhau, String baseUrl) {
        clearThreadLocals();
        CURRENT_TITLE.set("Đăng nhập thành công vào hệ thống");
        CURRENT_DESC.set("Kiểm tra đăng nhập bằng tài khoản và vào được bảng điều khiển (thấy Nộp đơn mới). "
                + "URL: " + nz(baseUrl));
        CURRENT_EXPECTED.set("Đăng nhập thành công và bảng điều khiển hiển thị chức năng Nộp đơn mới.");
        CURRENT_NOTE.set("Tài khoản cấu hình: " + nz(tenDangNhap)
                + (matKhau == null || matKhau.isBlank() ? "" : " (có mật khẩu)"));
    }

    @Deprecated
    public static void appendMucs(List<TaoDonExcelLogBuilder.Muc> extra) {
        if (extra == null) {
            return;
        }
        for (TaoDonExcelLogBuilder.Muc m : extra) {
            TestActionLog.ghiChu(m.buoc() + " | " + m.tenTruong() + " = " + m.giaTri()
                    + (m.ghiChu() == null || m.ghiChu().isBlank() ? "" : " (" + m.ghiChu() + ")"));
        }
    }

    public static void setKetQuaMongDoi(String expected) {
        if (expected != null && !expected.isBlank()) {
            CURRENT_EXPECTED.set(expected.trim());
        }
    }

    public static void setKetQuaThucTe(String actual) {
        if (actual != null && !actual.isBlank()) {
            CURRENT_ACTUAL.set(actual.trim());
        }
    }

    public static void setTrangThai(String status) {
        if (status != null && !status.isBlank()) {
            CURRENT_STATUS.set(normalizeStatusLabel(status.trim()));
        }
    }

    public static void setGhiChuKetQua(String note) {
        if (note != null && !note.isBlank()) {
            CURRENT_NOTE.set(note.trim());
        }
    }

    public static void setKetQua(String ketQua) {
        if (ketQua == null || ketQua.isBlank()) {
            return;
        }
        String k = ketQua.trim();
        CURRENT_ACTUAL.set(k);
        CURRENT_STATUS.set(mapDefaultStatus(k));
    }

    public static void recordFinished(String trangThaiMacDinh, long durationMs) {
        TaoDonScenario s = CURRENT_SCENARIO.get();
        String title = CURRENT_TITLE.get();
        String desc = CURRENT_DESC.get();
        String expected = CURRENT_EXPECTED.get();
        String actual = CURRENT_ACTUAL.get();
        String status = CURRENT_STATUS.get();
        String note = CURRENT_NOTE.get();
        List<TestActionLog.Action> actions = TestActionLog.drain();
        clearThreadLocals();

        if (s == null && actions.isEmpty() && (title == null || title.isBlank())) {
            return;
        }
        if (status == null || status.isBlank()) {
            status = mapDefaultStatus(trangThaiMacDinh);
        }
        if (actual == null || actual.isBlank()) {
            actual = trangThaiMacDinh == null ? "—" : trangThaiMacDinh;
        }
        if (expected == null || expected.isBlank()) {
            expected = s != null ? defaultExpectedTaoDon(s) : "Hoàn thành kịch bản theo mô tả.";
        }
        if (title == null || title.isBlank()) {
            title = s != null ? TaoDonReportBuilder.buildTestTitle(s) : "Kịch bản";
        }
        if (desc == null || desc.isBlank()) {
            desc = s != null ? TaoDonReportBuilder.buildTestDescription(s) : title;
        }

        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = new TaoDonExcelTestLog("Suite kiểm thử", SuiteKind.FULL);
            }
            int seq = INSTANCE.sheetSeq.incrementAndGet();
            String id = "KB_" + INSTANCE.kind.fileTag() + "_" + String.format("%03d", seq);
            INSTANCE.entries.add(new LogEntry(
                    id, title, desc, expected, actual, status,
                    note == null ? "" : note,
                    s,
                    actions, durationMs, seq));
        }
    }

    public static Path saveIfNeeded() {
        synchronized (LOCK) {
            if (INSTANCE == null || INSTANCE.entries.isEmpty()) {
                return null;
            }
            try {
                Path saved = INSTANCE.writeWorkbook();
                System.out.println("📊 Đã xuất nhật ký [" + INSTANCE.kind.fileTag() + "]: "
                        + saved.toAbsolutePath());
                INSTANCE = null;
                return saved;
            } catch (IOException e) {
                System.out.println("⚠ Không ghi được nhật ký Excel: " + e.getMessage());
                return null;
            }
        }
    }

    private static void clearThreadLocals() {
        CURRENT_SCENARIO.remove();
        CURRENT_TITLE.remove();
        CURRENT_DESC.remove();
        CURRENT_EXPECTED.remove();
        CURRENT_ACTUAL.remove();
        CURRENT_STATUS.remove();
        CURRENT_NOTE.remove();
    }

    private static String defaultExpectedTaoDon(TaoDonScenario s) {
        if (s == null) {
            return "Hoàn thành luồng tạo đơn theo đúng dữ liệu đã chuẩn bị.";
        }
        return "Điền đủ 6 bước tạo đơn (" + s.loaiDon() + " / " + blankToDash(s.loaiViec())
                + "), đến màn Xem lại và gửi đơn thành công.";
    }

    private static String mapDefaultStatus(String raw) {
        if (raw == null) {
            return ST_DAT;
        }
        return normalizeStatusLabel(raw);
    }

    /** Chuẩn hóa nhãn trạng thái về tiếng Việt (chấp nhận cả bản tiếng Anh cũ). */
    private static String normalizeStatusLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return ST_DAT;
        }
        String lower = raw.toLowerCase().trim();
        if (lower.contains("thất bại") || lower.contains("fail")) {
            return ST_THAT_BAI;
        }
        if (lower.contains("bỏ qua") || lower.contains("skip") || lower.contains("blocked")) {
            return ST_BO_QUA;
        }
        if (lower.contains("cảnh báo") || lower.contains("warn")) {
            return ST_DAT_CANH_BAO;
        }
        if (lower.contains("đạt") || lower.contains("pass") || lower.equals("ok")) {
            return ST_DAT;
        }
        return raw.trim();
    }

    private Path writeWorkbook() throws IOException {
        Files.createDirectories(outputFile.getParent());
        try (Workbook wb = new XSSFWorkbook()) {
            Styles styles = new Styles(wb);
            // Tổng hợp trước để biết dòng master → hyperlink mục lục
            java.util.LinkedHashMap<String, Integer> masterRows = writeTongHopSheet(wb, styles);
            writeInfoSheet(wb, styles, masterRows);
            if (kind == SuiteKind.FULL || kind == SuiteKind.MID) {
                writeCoverageSheet(wb, styles);
            }
            // Đưa "Thông tin" lên sheet đầu; Độ phủ ngay sau (full / mid)
            wb.setSheetOrder("Thông tin", 0);
            if ((kind == SuiteKind.FULL || kind == SuiteKind.MID) && wb.getSheetIndex(SHEET_DO_PHU) >= 0) {
                wb.setSheetOrder(SHEET_DO_PHU, 1);
            }
            try (OutputStream out = Files.newOutputStream(outputFile)) {
                wb.write(out);
            }
        }
        return outputFile;
    }

    // ─── Sheet: Thông tin (KPI + biểu đồ + mục lục) ──────────────────────────

    private void writeInfoSheet(Workbook wb, Styles st, java.util.Map<String, Integer> masterRows) {
        Sheet sheet = wb.createSheet("Thông tin");
        sheet.setDisplayGridlines(false);
        sheet.setDisplayGuts(true);
        if (sheet instanceof XSSFSheet xssf) {
            xssf.setTabColor(new XSSFColor(new byte[]{(byte) 0x1E, (byte) 0x3A, (byte) 0x8A}, null));
        }

        int totalSteps = countAllSteps();
        long totalMs = totalDurationMs();
        String overall = overallStatus();
        int[] statusCounts = countByStatusBucket(); // Đạt, Thất bại, Cảnh báo, Bỏ qua
        String baseUrl = ConfigReader.getValue("baseUrl", "https://demo-dichvutuphap.gsfpt.com/");

        // ── Header ──
        int r = 0;
        Row title = sheet.createRow(r++);
        Cell t0 = title.createCell(0);
        t0.setCellValue(kind.titleVi().toUpperCase());
        t0.setCellStyle(st.dashTitle);
        for (int i = 1; i < 9; i++) {
            title.createCell(i).setCellStyle(st.dashTitle);
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
        title.setHeightInPoints(34f);

        Row meta = sheet.createRow(r++);
        String metaText = kind.fileTag()
                + "  ·  " + suiteLabel
                + "  ·  Bắt đầu: " + startedAt
                + "  ·  " + entries.size() + " kịch bản";
        Cell m0 = meta.createCell(0);
        m0.setCellValue(metaText);
        m0.setCellStyle(st.infoMeta);
        for (int i = 1; i < 9; i++) {
            meta.createCell(i).setCellStyle(st.infoMeta);
        }
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));
        meta.setHeightInPoints(20f);

        r++; // spacer
        sheet.createRow(r - 1).setHeightInPoints(8f);

        // ── Trái: KPI + suite  |  Phải: biểu đồ ──
        int leftSectionRow = r;
        Row secKetQua = sheet.createRow(r++);
        writeSectionBar(sheet, secKetQua, st, "1. TÓM TẮT KẾT QUẢ", 0, 3);

        // Hàng KPI chính
        Row kpiL1 = sheet.createRow(r++);
        Row kpiV1 = sheet.createRow(r++);
        kpiL1.setHeightInPoints(16f);
        kpiV1.setHeightInPoints(28f);
        writeKpi(kpiL1, kpiV1, 0, "Số kịch bản", String.valueOf(entries.size()), st.kpiLabel, st.kpiValue);
        writeKpi(kpiL1, kpiV1, 1, ST_DAT, String.valueOf(statusCounts[0]), st.kpiLabel, st.kpiPass);
        writeKpi(kpiL1, kpiV1, 2, ST_THAT_BAI, String.valueOf(statusCounts[1]), st.kpiLabel, st.kpiFail);
        writeKpi(kpiL1, kpiV1, 3, "Thời gian", TaoDonReportBuilder.formatDuration(totalMs),
                st.kpiLabel, st.kpiValue);

        Row kpiL2 = sheet.createRow(r++);
        Row kpiV2 = sheet.createRow(r++);
        kpiL2.setHeightInPoints(16f);
        kpiV2.setHeightInPoints(28f);
        writeKpi(kpiL2, kpiV2, 0, "Số bước / dòng dữ liệu", String.valueOf(totalSteps),
                st.kpiLabel, st.kpiValue);
        writeKpi(kpiL2, kpiV2, 1, "Cảnh báo", String.valueOf(statusCounts[2]), st.kpiLabel, st.kpiWarn);
        writeKpi(kpiL2, kpiV2, 2, ST_BO_QUA, String.valueOf(statusCounts[3]), st.kpiLabel, st.kpiWarn);
        writeKpi(kpiL2, kpiV2, 3, "Tổng quan", overall, st.kpiLabel, kpiOverallStyle(st, overall));

        r++;
        sheet.createRow(r - 1).setHeightInPoints(8f);

        Row secSuite = sheet.createRow(r++);
        writeSectionBar(sheet, secSuite, st, "2. THÔNG TIN BỘ KIỂM THỬ & MÔI TRƯỜNG", 0, 3);

        String coverageStrategy = kind == SuiteKind.FULL
                ? "Đầy đủ cặp đôi mức B — đủ cặp loại việc + nhánh luồng (không tổ hợp toàn phần)"
                : kind == SuiteKind.MID
                ? "Trung bình — mọi cặp loại việc thường (1 nhánh) + đủ 4 tư cách Phá sản"
                : kind == SuiteKind.SMOKE ? "Mẫu nhanh — có ép Phá sản" : "Đăng nhập";
        String[][] suiteRows = {
                {"Loại nhật ký", kind.fileTag()},
                {"Bộ kiểm thử", suiteLabel},
                {"Chiến lược độ phủ", coverageStrategy},
                {"Số kịch bản trong file", String.valueOf(entries.size())},
                {"Môi trường", "Giao diện web — tự động hóa kiểm thử"},
                {"URL hệ thống", baseUrl},
                {"Thời điểm xuất báo cáo", startedAt}
        };
        for (int i = 0; i < suiteRows.length; i++) {
            boolean zebra = i % 2 == 1;
            Row row = sheet.createRow(r++);
            Cell k = row.createCell(0);
            k.setCellValue(suiteRows[i][0]);
            k.setCellStyle(zebra ? st.kvKeyZebra : st.kvKey);
            Cell v = row.createCell(1);
            v.setCellValue(suiteRows[i][1]);
            v.setCellStyle(zebra ? st.kvValueZebra : st.kvValue);
            row.createCell(2).setCellStyle(zebra ? st.kvValueZebra : st.kvValue);
            row.createCell(3).setCellStyle(zebra ? st.kvValueZebra : st.kvValue);
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 1, 3));
            row.setHeightInPoints(Math.max(20f, wrappedHeight(suiteRows[i][1], 55)));
        }
        int suiteEnd = r - 1;
        frameRange(sheet, leftSectionRow, suiteEnd, 0, 3);

        // Chart: bảng chú thích cột F–G (hiển thị) + pie bên phải
        int[] chartRange = writeStatusChartData(sheet, st, leftSectionRow + 1);
        if (sheet instanceof XSSFSheet xssf && chartRange[1] >= chartRange[0]) {
            addStatusPieChart(xssf, chartRange[0], chartRange[1], leftSectionRow);
        }

        // Mục lục dưới vùng KPI/suite (chart nằm bên phải, không đẩy TOC quá thấp)
        r = Math.max(r + 1, leftSectionRow + 12);

        Row secToc = sheet.createRow(r++);
        writeSectionBar(sheet, secToc, st,
                "3. MỤC LỤC KỊCH BẢN  —  bấm Mã kịch bản để nhảy sang sheet Tổng hợp", 0, 4);

        Row tocHead = sheet.createRow(r++);
        write(tocHead, 0, "STT", st.header);
        write(tocHead, 1, "Mã kịch bản", st.header);
        write(tocHead, 2, "Mô tả", st.header);
        write(tocHead, 3, "Trạng thái", st.header);
        write(tocHead, 4, "Thời gian", st.header);
        tocHead.setHeightInPoints(22f);

        int tocFirst = r;
        for (int i = 0; i < entries.size(); i++) {
            LogEntry e = entries.get(i);
            boolean zebra = i % 2 == 1;
            Row row = sheet.createRow(r++);
            write(row, 0, String.valueOf(i + 1), zebra ? st.tocCenterZebra : st.tocCenter);
            Cell idCell = row.createCell(1);
            idCell.setCellValue(e.testCaseId());
            Integer masterRow = masterRows.get(e.testCaseId());
            CellStyle idStyle = zebra ? st.tocIdZebra : st.tocId;
            if (masterRow != null) {
                setInternalHyperlink(wb, idCell, "Tổng hợp", masterRow + 1, 2, idStyle);
            } else {
                idCell.setCellStyle(idStyle);
            }
            String moTaNgan = shortDesc(e);
            write(row, 2, moTaNgan, zebra ? st.tocValueZebra : st.kvValue);
            write(row, 3, e.trangThai(), statusStyle(st, e.trangThai()));
            write(row, 4, TaoDonReportBuilder.formatDuration(e.durationMs()),
                    zebra ? st.tocCenterZebra : st.tocCenter);
            row.setHeightInPoints(Math.max(22f, wrappedHeight(moTaNgan, 40)));
        }
        int tocLast = r - 1;
        if (tocLast >= tocFirst) {
            sheet.setAutoFilter(new CellRangeAddress(tocFirst - 1, tocLast, 0, 4));
            frameRange(sheet, tocFirst - 1, tocLast, 0, 4);
        }

        r++;
        Row hint = sheet.createRow(r);
        Cell hc = hint.createCell(0);
        hc.setCellValue("Hướng dẫn: Sheet Tổng hợp — bấm [+] mở kịch bản → [+] dữ liệu bước "
                + "(2 cột: Trường | Thao tác+giá trị; thao tác tô màu). "
                + "Cột Thời gian dùng màu nhiệt (xanh = nhanh, đỏ = chậm).");
        hc.setCellStyle(st.hint);
        for (int i = 1; i < 5; i++) {
            hint.createCell(i).setCellStyle(st.hint);
        }
        sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 4));
        hint.setHeightInPoints(Math.max(36f, wrappedHeight(hc.getStringCellValue(), 90)));

        // Cột cố định — STT width 20
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 24 * 256); // Mã kịch bản
        sheet.setColumnWidth(2, 40 * 256);
        sheet.setColumnWidth(3, 16 * 256);
        sheet.setColumnWidth(4, 14 * 256);
        sheet.setColumnWidth(5, 14 * 256); // chú thích chart
        sheet.setColumnWidth(6, 10 * 256);
        for (int c = 7; c <= 10; c++) {
            sheet.setColumnWidth(c, 11 * 256);
        }
        sheet.createFreezePane(0, 2);
    }

    // ─── Sheet: Độ phủ (ma trận nhánh đầy đủ cặp đôi mức B) ─────────────────

    private void writeCoverageSheet(Workbook wb, Styles st) {
        Sheet sheet = wb.createSheet(SHEET_DO_PHU);
        sheet.setDisplayGridlines(false);
        if (sheet instanceof XSSFSheet xssf) {
            xssf.setTabColor(new XSSFColor(new byte[]{(byte) 0x0F, (byte) 0x76, (byte) 0x6E}, null));
        }

        int r = 0;
        Row title = sheet.createRow(r++);
        Cell t0 = title.createCell(0);
        t0.setCellValue(kind == SuiteKind.MID
                ? "MA TRẬN ĐỘ PHỦ — TRUNG BÌNH (35+4)"
                : "MA TRẬN ĐỘ PHỦ — ĐẦY ĐỦ CẶP ĐÔI (MỨC B)");
        t0.setCellStyle(st.dashTitle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        title.setHeightInPoints(28f);

        Row sub = sheet.createRow(r++);
        Cell s0 = sub.createCell(0);
        s0.setCellValue("Đối chiếu nhánh luồng đã chạy trong bộ kiểm thử (từ kịch bản gắn với từng kịch bản). "
                + "Danh mục phụ (tòa, giới tính…) chỉ xoay — không liệt kê tổ hợp toàn phần.");
        s0.setCellStyle(st.hint);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));
        sub.setHeightInPoints(36f);

        r++;
        Row sec1 = sheet.createRow(r++);
        writeSectionBar(sheet, sec1, st, "1. NHÁNH LUỒNG CẤU TRÚC", 0, 3);

        List<String[]> axisRows = buildCoverageAxisRows();
        Row head1 = sheet.createRow(r++);
        write(head1, 0, "Nhánh", st.header);
        write(head1, 1, "Giá trị", st.header);
        write(head1, 2, "Số kịch bản", st.header);
        write(head1, 3, "Trạng thái", st.header);
        int axisFirst = r;
        for (int i = 0; i < axisRows.size(); i++) {
            String[] row = axisRows.get(i);
            boolean zebra = i % 2 == 1;
            Row excelRow = sheet.createRow(r++);
            write(excelRow, 0, row[0], zebra ? st.kvKeyZebra : st.kvKey);
            write(excelRow, 1, row[1], zebra ? st.kvValueZebra : st.kvValue);
            write(excelRow, 2, row[2], zebra ? st.tocCenterZebra : st.tocCenter);
            boolean ok = !"0".equals(row[2]);
            write(excelRow, 3, ok ? "Đã phủ" : "Thiếu", ok ? st.pass : st.fail);
        }
        frameRange(sheet, axisFirst - 1, r - 1, 0, 3);

        r++;
        Row sec2 = sheet.createRow(r++);
        writeSectionBar(sheet, sec2, st, "2. CẶP LOẠI ĐƠN › LOẠI VIỆC", 0, 3);

        Row head2 = sheet.createRow(r++);
        write(head2, 0, "Loại đơn", st.header);
        write(head2, 1, "Loại việc", st.header);
        write(head2, 2, "Số kịch bản", st.header);
        write(head2, 3, "Trạng thái", st.header);
        int pairFirst = r;
        Map<String, Integer> pairCounts = countPairsFromEntries();
        int pi = 0;
        for (String[] pair : MasterDataCatalog.getAllLoaiDonViecPairs()) {
            String key = pair[0] + ">" + pair[1];
            int count = pairCounts.getOrDefault(key, 0);
            boolean zebra = pi++ % 2 == 1;
            Row excelRow = sheet.createRow(r++);
            write(excelRow, 0, pair[0], zebra ? st.kvKeyZebra : st.kvKey);
            write(excelRow, 1, pair[1], zebra ? st.kvValueZebra : st.kvValue);
            write(excelRow, 2, String.valueOf(count), zebra ? st.tocCenterZebra : st.tocCenter);
            write(excelRow, 3, count > 0 ? "Đã phủ" : "Thiếu", count > 0 ? st.pass : st.fail);
        }
        int pairLast = r - 1;
        if (pairLast >= pairFirst) {
            sheet.setAutoFilter(new CellRangeAddress(pairFirst - 1, pairLast, 0, 3));
            frameRange(sheet, pairFirst - 1, pairLast, 0, 3);
        }

        r++;
        Row sec3 = sheet.createRow(r++);
        writeSectionBar(sheet, sec3, st, "3. TƯ CÁCH NỘP ĐƠN — PHÁ SẢN", 0, 3);

        Row head3 = sheet.createRow(r++);
        write(head3, 0, "Tư cách", st.header);
        write(head3, 1, "Số kịch bản", st.header);
        write(head3, 2, "Trạng thái", st.header);
        write(head3, 3, "", st.header);
        int tcFirst = r;
        Map<String, Integer> tuCachCounts = countTuCachFromEntries();
        int ti = 0;
        for (String tuCach : MasterDataCatalog.getTuCachNopDonPhaSan()) {
            int count = tuCachCounts.getOrDefault(tuCach, 0);
            boolean zebra = ti++ % 2 == 1;
            Row excelRow = sheet.createRow(r++);
            write(excelRow, 0, tuCach, zebra ? st.kvKeyZebra : st.kvKey);
            write(excelRow, 1, String.valueOf(count), zebra ? st.tocCenterZebra : st.tocCenter);
            write(excelRow, 2, count > 0 ? "Đã phủ" : "Thiếu", count > 0 ? st.pass : st.fail);
            write(excelRow, 3, "", zebra ? st.kvValueZebra : st.kvValue);
        }
        frameRange(sheet, tcFirst - 1, r - 1, 0, 2);

        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 40 * 256);
        sheet.setColumnWidth(2, 12 * 256);
        sheet.setColumnWidth(3, 14 * 256);
        sheet.createFreezePane(0, 2);
    }

    private List<String[]> buildCoverageAxisRows() {
        int ndCn = 0, ndTc = 0, daiDienCo = 0, daiDienKhong = 0;
        int bdCn = 0, bdTc = 0, so1 = 0, so2 = 0, nlqCo = 0, nlqKhong = 0, tlCo = 0, tlKhong = 0;
        int uiStandard = 0, uiHonNhan = 0, uiThuanTinh = 0, uiPhaSan = 0, uiHanhChinh = 0;

        for (LogEntry e : entries) {
            TaoDonScenario s = e.scenario();
            if (s == null) {
                continue;
            }
            if (DataDictionary.isToChuc(s.loaiChuThe())) {
                ndTc++;
            } else {
                ndCn++;
            }
            if (!DataDictionary.isToChuc(s.loaiChuThe())) {
                if ("Có".equalsIgnoreCase(safe(s.coNguoiDaiDien()))) {
                    daiDienCo++;
                } else {
                    daiDienKhong++;
                }
            }
            if (!DataDictionary.isPhaSan(s.loaiDon()) && !DataDictionary.isHanhChinh(s.loaiDon())) {
                if (DataDictionary.isToChuc(s.loaiBiDon())) {
                    bdTc++;
                } else {
                    bdCn++;
                }
            }
            if (s.soLuongBiDon() >= 2) {
                so2++;
            } else {
                so1++;
            }
            if ("Có".equalsIgnoreCase(safe(s.coNguoiLienQuan()))) {
                nlqCo++;
            } else {
                nlqKhong++;
            }
            if ("Có".equalsIgnoreCase(safe(s.coTaiLieuBoSung()))) {
                tlCo++;
            } else {
                tlKhong++;
            }
            if (DataDictionary.isPhaSan(s.loaiDon())) {
                uiPhaSan++;
            } else if (DataDictionary.isHanhChinh(s.loaiDon())) {
                uiHanhChinh++;
            } else if (DataDictionary.isThuanTinhLyHon(s.loaiViec())) {
                uiThuanTinh++;
            } else if (DataDictionary.isHonNhanGiaDinh(s.loaiDon())) {
                uiHonNhan++;
            } else if (DataDictionary.isStandardBiDonUi(s.loaiDon())) {
                uiStandard++;
            }
        }

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Nguyên đơn", "Cá nhân", String.valueOf(ndCn)});
        rows.add(new String[]{"Nguyên đơn", "Tổ chức", String.valueOf(ndTc)});
        rows.add(new String[]{"Người đại diện (ND CN)", "Có", String.valueOf(daiDienCo)});
        rows.add(new String[]{"Người đại diện (ND CN)", "Không", String.valueOf(daiDienKhong)});
        rows.add(new String[]{"Bị đơn (giao diện chuẩn/HN)", "Cá nhân", String.valueOf(bdCn)});
        rows.add(new String[]{"Bị đơn (giao diện chuẩn/HN)", "Tổ chức", String.valueOf(bdTc)});
        rows.add(new String[]{"Số bị đơn", "1", String.valueOf(so1)});
        rows.add(new String[]{"Số bị đơn", "2 (Thêm)", String.valueOf(so2)});
        rows.add(new String[]{"Người liên quan", "Có", String.valueOf(nlqCo)});
        rows.add(new String[]{"Người liên quan", "Không", String.valueOf(nlqKhong)});
        rows.add(new String[]{"Tài liệu bổ sung", "Có", String.valueOf(tlCo)});
        rows.add(new String[]{"Tài liệu bổ sung", "Không", String.valueOf(tlKhong)});
        rows.add(new String[]{"Giao diện bước 3", "Chuẩn (Bị đơn N)", String.valueOf(uiStandard)});
        rows.add(new String[]{"Giao diện bước 3", "Hôn nhân — bị yêu cầu", String.valueOf(uiHonNhan)});
        rows.add(new String[]{"Giao diện bước 3", "Hôn nhân — Thuận tình", String.valueOf(uiThuanTinh)});
        rows.add(new String[]{"Giao diện bước 3", "Phá sản", String.valueOf(uiPhaSan)});
        rows.add(new String[]{"Giao diện bước 3", "Hành chính", String.valueOf(uiHanhChinh)});
        return rows;
    }

    private Map<String, Integer> countPairsFromEntries() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (LogEntry e : entries) {
            TaoDonScenario s = e.scenario();
            if (s == null) {
                continue;
            }
            String key = s.loaiDon() + ">" + (s.loaiViec() == null ? "" : s.loaiViec());
            map.merge(key, 1, Integer::sum);
        }
        return map;
    }

    private Map<String, Integer> countTuCachFromEntries() {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (LogEntry e : entries) {
            TaoDonScenario s = e.scenario();
            if (s == null || !DataDictionary.isPhaSan(s.loaiDon())) {
                continue;
            }
            String t = s.tuCachNopDon();
            if (t == null || t.isBlank()) {
                continue;
            }
            map.merge(t, 1, Integer::sum);
        }
        return map;
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static void writeSectionBar(Sheet sheet, Row row, Styles st, String text, int c1, int c2) {
        Cell c = row.createCell(c1);
        c.setCellValue(text);
        c.setCellStyle(st.sectionHeader);
        for (int i = c1 + 1; i <= c2; i++) {
            row.createCell(i).setCellStyle(st.sectionHeader);
        }
        if (c2 > c1) {
            CellRangeAddress range = new CellRangeAddress(row.getRowNum(), row.getRowNum(), c1, c2);
            sheet.addMergedRegion(range);
            RegionUtil.setBorderTop(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, range, sheet);
        }
        row.setHeightInPoints(22f);
    }

    private static void writeKpi(Row labelRow, Row valueRow, int col,
                                 String label, String value, CellStyle labelStyle, CellStyle valueStyle) {
        Cell lc = labelRow.createCell(col);
        lc.setCellValue(label);
        lc.setCellStyle(labelStyle);
        Cell vc = valueRow.createCell(col);
        vc.setCellValue(value == null ? "—" : value);
        vc.setCellStyle(valueStyle);
    }

    private static String shortDesc(LogEntry e) {
        String raw = e.moTa();
        if (raw == null || raw.isBlank()) {
            raw = e.description();
        }
        if (raw == null || raw.isBlank()) {
            return "—";
        }
        String t = raw.trim().replaceAll("\\s+", " ");
        return t.length() > 90 ? t.substring(0, 87) + "…" : t;
    }

    /** [Đạt, Thất bại, Cảnh báo, Bỏ qua] */
    private int[] countByStatusBucket() {
        int[] n = new int[4];
        for (LogEntry e : entries) {
            switch (statusBucket(e.trangThai())) {
                case ST_THAT_BAI -> n[1]++;
                case ST_DAT_CANH_BAO -> n[2]++;
                case ST_BO_QUA -> n[3]++;
                default -> n[0]++;
            }
        }
        return n;
    }

    /** Ghi bảng nguồn biểu đồ ở cột F–G (hiển thị cạnh pie). Trả về [firstDataRow, lastDataRow]. */
    private int[] writeStatusChartData(Sheet sheet, Styles st, int startRow) {
        java.util.LinkedHashMap<String, Integer> counts = new java.util.LinkedHashMap<>();
        counts.put(ST_DAT, 0);
        counts.put(ST_THAT_BAI, 0);
        counts.put(ST_DAT_CANH_BAO, 0);
        counts.put(ST_BO_QUA, 0);
        for (LogEntry e : entries) {
            String bucket = statusBucket(e.trangThai());
            counts.merge(bucket, 1, Integer::sum);
        }
        counts.entrySet().removeIf(en -> en.getValue() == 0);
        if (counts.isEmpty()) {
            return new int[]{0, -1};
        }

        final int colLabel = 5;
        final int colVal = 6;
        int rowIdx = Math.max(0, startRow);

        Row title = sheet.getRow(rowIdx);
        if (title == null) {
            title = sheet.createRow(rowIdx);
        }
        Cell t = title.createCell(colLabel);
        t.setCellValue("Phân bố trạng thái");
        t.setCellStyle(st.sectionHeader);
        title.createCell(colVal).setCellStyle(st.sectionHeader);
        try {
            sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, colLabel, colVal));
        } catch (IllegalStateException ignored) {
            // đã merge
        }
        rowIdx++;

        Row hdr = sheet.getRow(rowIdx);
        if (hdr == null) {
            hdr = sheet.createRow(rowIdx);
        }
        Cell h0 = hdr.createCell(colLabel);
        h0.setCellValue("Trạng thái");
        h0.setCellStyle(st.kpiLabel);
        Cell h1 = hdr.createCell(colVal);
        h1.setCellValue("Số lượng");
        h1.setCellStyle(st.kpiLabel);
        rowIdx++;

        int first = rowIdx;
        for (var en : counts.entrySet()) {
            Row row = sheet.getRow(rowIdx);
            if (row == null) {
                row = sheet.createRow(rowIdx);
            }
            Cell lc = row.createCell(colLabel);
            lc.setCellValue(en.getKey());
            lc.setCellStyle(st.kvKey);
            Cell vc = row.createCell(colVal);
            vc.setCellValue(en.getValue().doubleValue());
            vc.setCellStyle(st.kvValue);
            rowIdx++;
        }
        return new int[]{first, rowIdx - 1};
    }

    private static String statusBucket(String status) {
        if (status == null) {
            return ST_DAT;
        }
        String k = status.toLowerCase();
        if (k.contains("fail") || k.contains("thất bại")) {
            return ST_THAT_BAI;
        }
        if (k.contains("cảnh báo")) {
            return ST_DAT_CANH_BAO;
        }
        if (k.contains("skip") || k.contains("bỏ qua") || k.contains("blocked")) {
            return ST_BO_QUA;
        }
        return ST_DAT;
    }

    private void addStatusPieChart(XSSFSheet sheet, int firstRow, int lastRow, int anchorRow) {
        try {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            int row1 = Math.max(2, anchorRow);
            // Pie bên phải bảng chú thích (cột H→K)
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 7, row1, 11, row1 + 11);
            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("Biểu đồ trạng thái");
            chart.setTitleOverlay(false);

            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.RIGHT);

            XDDFDataSource<String> cats = XDDFDataSourcesFactory.fromStringCellRange(
                    sheet, new CellRangeAddress(firstRow, lastRow, 5, 5));
            XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange(
                    sheet, new CellRangeAddress(firstRow, lastRow, 6, 6));

            XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
            data.setVaryColors(true);
            XDDFChartData.Series series = data.addSeries(cats, vals);
            series.setTitle("Số lượng", null);
            chart.plot(data);

            var pie = chart.getCTChart().getPlotArea().getPieChartArray(0);
            var ser = pie.getSerArray(0);
            if (!ser.isSetDLbls()) {
                ser.addNewDLbls();
            }
            var dLbls = ser.getDLbls();
            if (!dLbls.isSetShowPercent()) {
                dLbls.addNewShowPercent();
            }
            dLbls.getShowPercent().setVal(true);
            if (!dLbls.isSetShowVal()) {
                dLbls.addNewShowVal();
            }
            dLbls.getShowVal().setVal(true);
            if (!dLbls.isSetShowCatName()) {
                dLbls.addNewShowCatName();
            }
            dLbls.getShowCatName().setVal(false);
            if (!dLbls.isSetShowSerName()) {
                dLbls.addNewShowSerName();
            }
            dLbls.getShowSerName().setVal(false);

            applyPieSliceColors(sheet, ser, firstRow, lastRow);
        } catch (Exception ex) {
            System.out.println("⚠ Không tạo được biểu đồ trạng thái: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void applyPieSliceColors(Sheet sheet, org.openxmlformats.schemas.drawingml.x2006.chart.CTPieSer ser,
                                     int firstRow, int lastRow) {
        int idx = 0;
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null || row.getCell(5) == null) {
                continue;
            }
            String label = row.getCell(5).getStringCellValue();
            byte[] rgb = pieColorFor(label);
            while (ser.sizeOfDPtArray() <= idx) {
                ser.addNewDPt();
            }
            var dPt = ser.getDPtArray(idx);
            if (dPt.getIdx() == null) {
                dPt.addNewIdx();
            }
            dPt.getIdx().setVal(idx);
            if (dPt.getSpPr() == null) {
                dPt.addNewSpPr();
            }
            var solid = dPt.getSpPr().isSetSolidFill() ? dPt.getSpPr().getSolidFill() : dPt.getSpPr().addNewSolidFill();
            if (solid.isSetSrgbClr()) {
                solid.unsetSrgbClr();
            }
            solid.addNewSrgbClr().setVal(rgb);
            idx++;
        }
    }

    private static byte[] pieColorFor(String label) {
        String k = label == null ? "" : label.toLowerCase();
        if (k.contains("fail") || k.contains("thất bại")) {
            return new byte[]{(byte) 0xEF, (byte) 0x44, (byte) 0x44}; // đỏ
        }
        if (k.contains("cảnh báo") || k.contains("skip") || k.contains("blocked")) {
            return new byte[]{(byte) 0xF5, (byte) 0x9E, (byte) 0x0B}; // vàng/cam
        }
        return new byte[]{(byte) 0x22, (byte) 0xC5, (byte) 0x5E}; // xanh lá
    }

    // ─── Sheet: Tổng hợp (Outline / Group) ──────────────────────────────────

    /** @return map TestCaseID → row index 0-based của dòng master */
    private java.util.LinkedHashMap<String, Integer> writeTongHopSheet(Workbook wb, Styles st) {
        Sheet sheet = wb.createSheet("Tổng hợp");
        sheet.setDisplayGridlines(true);
        sheet.setDisplayGuts(true);
        sheet.setRowSumsBelow(false);

        java.util.LinkedHashMap<String, Integer> masterRows = new java.util.LinkedHashMap<>();
        java.util.List<Integer> durationMasterRows = new ArrayList<>();
        int r = 0;

        Row title = sheet.createRow(r++);
        Cell t0 = title.createCell(0);
        t0.setCellValue("NHẬT KÝ KIỂM THỬ — " + kind.fileTag() + "  ·  " + startedAt
                + "   |   [+] kịch bản → [+] dữ liệu bước (có cột Thao tác)  ·  Màu nhiệt thời gian");
        t0.setCellStyle(st.dashTitle);
        for (int i = 1; i < SHEET_COLS; i++) {
            title.createCell(i).setCellStyle(st.dashTitle);
        }
        merge(sheet, 0, 0, SHEET_COLS - 1);
        title.setHeightInPoints(30f);

        String[] sumCols = {
                "STT", "Mã kịch bản", "Mô tả", "Kết quả mong đợi", "Kết quả thực tế",
                "Trạng thái", "Ghi chú", "Thời gian", "Số bước"
        };
        int headerRowIdx = r;
        Row sumHeader = sheet.createRow(r++);
        for (int i = 0; i < sumCols.length; i++) {
            Cell c = sumHeader.createCell(i);
            c.setCellValue(sumCols[i]);
            c.setCellStyle(st.header);
        }
        sumHeader.setHeightInPoints(28f);

        List<Integer> groupStarts = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            LogEntry e = entries.get(i);
            List<TestActionLog.Action> actions = compactActions(e.actions());
            List<ReportBlock> blocks = buildReportBlocks(e.scenario(), actions);

            int masterRowIdx = r;
            masterRows.put(e.testCaseId(), masterRowIdx);
            durationMasterRows.add(masterRowIdx);

            Row master = sheet.createRow(r++);
            String moTa = blankToDash(e.description().isBlank() ? e.moTa() : e.description());
            write(master, 0, String.valueOf(i + 1), st.masterCenter);
            write(master, 1, e.testCaseId(), st.masterCenter);
            write(master, 2, moTa, st.masterWrap);
            write(master, 3, e.ketQuaMongDoi(), st.masterWrap);
            write(master, 4, e.ketQuaThucTe(), st.masterWrap);
            write(master, COL_STATUS, e.trangThai(), statusStyle(st, e.trangThai()));
            write(master, 6, blankToDash(e.ghiChu()), st.masterWrap);

            Cell dur = master.createCell(COL_DURATION);
            dur.setCellValue(durationSeconds(e.durationMs()));
            dur.setCellStyle(st.durationNum);
            write(master, 8, String.valueOf(countBlockRows(blocks)), st.masterCenter);

            float h = 24f;
            h = Math.max(h, wrappedHeight(moTa, SUMMARY_CHARS[2]));
            h = Math.max(h, wrappedHeight(e.ketQuaMongDoi(), SUMMARY_CHARS[3]));
            h = Math.max(h, wrappedHeight(e.ketQuaThucTe(), SUMMARY_CHARS[4]));
            h = Math.max(h, wrappedHeight(e.ghiChu(), SUMMARY_CHARS[6]));
            master.setHeightInPoints(h);

            int detailStart = r;

            if (blocks.isEmpty()) {
                Row empty = sheet.createRow(r++);
                write(empty, 0, "—", st.detailCenter);
                write(empty, 1, "—", st.detailCenter);
                write(empty, 2, "(Chưa có dữ liệu / thao tác)", st.detailWrap);
                write(empty, 3, "—", st.detailWrap);
                write(empty, 4, "—", st.detailWrap);
                for (int c = 5; c < SHEET_COLS; c++) {
                    empty.createCell(c).setCellStyle(st.detailWrap);
                }
                empty.setHeightInPoints(20f);
            } else {
                List<Integer> nestedStarts = new ArrayList<>();
                for (int bi = 0; bi < blocks.size(); bi++) {
                    ReportBlock block = blocks.get(bi);
                    boolean overview = isOverviewBlock(block);
                    String stepStatus = resolveBlockStatus(e, block, bi, blocks);
                    int blockStart = r;

                    writeStepBanner(sheet, r++, st, overview, bannerTitle(block, e, overview), stepStatus);
                    if (block.data().isEmpty() && block.actions().isEmpty() && !overview) {
                        frameRange(sheet, blockStart, r - 1, 0, SHEET_COLS - 1);
                        continue;
                    }
                    int nestedStart = r;

                    if (overview && e.scenario() != null) {
                        r = writeOverviewCard(sheet, r, st, e.scenario());
                    } else if (overview) {
                        for (TaoDonExcelLogBuilder.Muc m : block.data()) {
                            Row row = sheet.createRow(r++);
                            writeOverviewKvRow(row, st, blankToDash(m.tenTruong()), blankToDash(m.giaTri()));
                        }
                    } else {
                        // Tiêu đề cột theo từng bước (không đặt chung ở đầu kịch bản)
                        writeDetailColumnHeader(sheet, r++, st);
                        r = writeDataBuocKemCotThaoTac(sheet, r, st, block.data(), block.actions());

                        int nestedEnd = r - 1;
                        if (nestedEnd >= nestedStart) {
                            sheet.groupRow(nestedStart, nestedEnd);
                            nestedStarts.add(nestedStart);
                        }
                        frameRange(sheet, blockStart, r - 1, 0, SHEET_COLS - 1);
                        continue;
                    }

                    int nestedEnd = r - 1;
                    if (nestedEnd >= nestedStart) {
                        sheet.groupRow(nestedStart, nestedEnd);
                        nestedStarts.add(nestedStart);
                    }
                    frameRange(sheet, blockStart, r - 1, 0, SHEET_COLS - 1);
                }
                for (int start : nestedStarts) {
                    try {
                        sheet.setRowGroupCollapsed(start, true);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            int detailEnd = r - 1;
            if (detailEnd >= detailStart) {
                sheet.groupRow(detailStart, detailEnd);
                groupStarts.add(detailStart);
            }
            // Khung ngoài từng kịch bản (dòng master + chi tiết)
            frameRange(sheet, masterRowIdx, detailEnd, 0, SHEET_COLS - 1);
        }

        int lastDataRow = r - 1;
        if (lastDataRow >= headerRowIdx) {
            sheet.setAutoFilter(new CellRangeAddress(headerRowIdx, Math.max(headerRowIdx, lastDataRow),
                    0, SHEET_COLS - 1));
            frameRange(sheet, headerRowIdx, lastDataRow, 0, SHEET_COLS - 1);
        }

        applyDurationHeatmap(sheet, durationMasterRows);

        for (int start : groupStarts) {
            try {
                sheet.setRowGroupCollapsed(start, true);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (sheet instanceof XSSFSheet xssf) {
            if (xssf.getCTWorksheet().isSetSheetPr()) {
                var pr = xssf.getCTWorksheet().getSheetPr();
                if (!pr.isSetOutlinePr()) {
                    pr.addNewOutlinePr();
                }
                pr.getOutlinePr().setSummaryBelow(false);
            } else {
                var pr = xssf.getCTWorksheet().addNewSheetPr();
                pr.addNewOutlinePr().setSummaryBelow(false);
            }
        }

        sheet.createFreezePane(0, 2);
        applyFixedColumnWidths(sheet, SHEET_COLS, SUMMARY_CHARS);
        return masterRows;
    }

    private static double durationSeconds(long durationMs) {
        return Math.round(Math.max(0, durationMs) / 100.0) / 10.0; // 1 decimal
    }

    private static void setInternalHyperlink(Workbook wb, Cell cell, String sheetName,
                                             int excelRow1Based, int excelCol1Based, CellStyle style) {
        try {
            var helper = wb.getCreationHelper();
            var link = helper.createHyperlink(org.apache.poi.common.usermodel.HyperlinkType.DOCUMENT);
            String colLetter = org.apache.poi.ss.util.CellReference.convertNumToColString(excelCol1Based - 1);
            link.setAddress("'" + sheetName + "'!" + colLetter + excelRow1Based);
            cell.setHyperlink(link);
            cell.setCellStyle(style);
        } catch (Exception ex) {
            cell.setCellStyle(style);
        }
    }

    private void applyDurationHeatmap(Sheet sheet, List<Integer> masterRowIndexes) {
        if (masterRowIndexes == null || masterRowIndexes.isEmpty()) {
            return;
        }
        try {
            CellRangeAddress[] regions = new CellRangeAddress[masterRowIndexes.size()];
            for (int i = 0; i < masterRowIndexes.size(); i++) {
                int rr = masterRowIndexes.get(i);
                regions[i] = new CellRangeAddress(rr, rr, COL_DURATION, COL_DURATION);
            }
            SheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();
            ConditionalFormattingRule rule = scf.createConditionalFormattingColorScaleRule();
            ColorScaleFormatting csf = rule.getColorScaleFormatting();
            csf.getThresholds()[0].setRangeType(ConditionalFormattingThreshold.RangeType.MIN);
            csf.getThresholds()[1].setRangeType(ConditionalFormattingThreshold.RangeType.PERCENTILE);
            csf.getThresholds()[1].setValue(50d);
            csf.getThresholds()[2].setRangeType(ConditionalFormattingThreshold.RangeType.MAX);
            // Xanh (nhanh) → Vàng → Đỏ (chậm)
            csf.setColors(new org.apache.poi.ss.usermodel.Color[]{
                    new XSSFColor(new byte[]{(byte) 0x86, (byte) 0xEF, (byte) 0xAC}, null),
                    new XSSFColor(new byte[]{(byte) 0xFE, (byte) 0xF0, (byte) 0x8A}, null),
                    new XSSFColor(new byte[]{(byte) 0xFC, (byte) 0xA5, (byte) 0xA5}, null)
            });
            scf.addConditionalFormatting(regions, rule);
        } catch (Exception ex) {
            System.out.println("⚠ Không áp dụng màu nhiệt thời gian: " + ex.getMessage());
        }
    }

    // ─── Aggregates ─────────────────────────────────────────────────────────

    private int countAllSteps() {
        int n = 0;
        for (LogEntry e : entries) {
            n += countBlockRows(buildReportBlocks(e.scenario(), compactActions(e.actions())));
        }
        return n;
    }

    private static int countBlockRows(List<ReportBlock> blocks) {
        int n = 0;
        for (ReportBlock b : blocks) {
            if (isOverviewBlock(b)) {
                n += b.data().size();
            } else {
                n += countDataBuocRows(b.data(), b.actions());
            }
        }
        return n;
    }

    private static int countDataBuocRows(List<TaoDonExcelLogBuilder.Muc> data,
                                         List<TestActionLog.Action> actions) {
        List<TestActionLog.Action> remaining = new ArrayList<>(
                actions == null ? List.of() : actions);
        int n = 0;
        if (data != null) {
            for (TaoDonExcelLogBuilder.Muc m : data) {
                n++;
                takeMatchingAction(remaining, m.tenTruong());
            }
        }
        return n + remaining.size();
    }

    private static void writeSubBanner(Sheet sheet, int rowIdx, Styles st, String text) {
        Row row = sheet.createRow(rowIdx);
        Cell c0 = row.createCell(0);
        c0.setCellValue(text);
        c0.setCellStyle(st.subBlockHeader);
        for (int c = 1; c < SHEET_COLS; c++) {
            row.createCell(c).setCellStyle(st.subBlockHeader);
        }
        merge(sheet, rowIdx, 0, Math.min(4, SHEET_COLS - 1));
        row.setHeightInPoints(18f);
    }

    /** Tiêu đề cột dữ liệu — gắn ngay dưới banner từng bước. */
    private static void writeDetailColumnHeader(Sheet sheet, int rowIdx, Styles st) {
        Row dh = sheet.createRow(rowIdx);
        String[] dcols = {"Trường", "Thao tác + giá trị", "", "Trường", "Thao tác + giá trị"};
        for (int c = 0; c < dcols.length; c++) {
            Cell cell = dh.createCell(c);
            cell.setCellValue(dcols[c]);
            cell.setCellStyle(st.detailHeader);
        }
        for (int c = dcols.length; c < SHEET_COLS; c++) {
            dh.createCell(c).setCellStyle(st.detailHeader);
        }
        dh.setHeightInPoints(20f);
    }

    private static String bannerTitle(ReportBlock block, LogEntry e, boolean overview) {
        if (overview) {
            String bannerText = "Tổng quan kịch bản";
            if (e.scenario() != null && e.scenario().stt() != null
                    && !e.scenario().stt().isBlank()) {
                bannerText = bannerText + "  #" + e.scenario().stt().trim();
            }
            return bannerText;
        }
        return block.title() + "   (" + block.data().size() + " dữ liệu)";
    }

    /**
     * Banner bước: tiêu đề (cột 0–4) + trạng thái đúng cột {@link #COL_STATUS}
     * (cùng cột với trạng thái tổng của kịch bản).
     */
    private static void writeStepBanner(Sheet sheet, int rowIdx, Styles st,
                                        boolean overview, String title, String trangThai) {
        Row banner = sheet.createRow(rowIdx);
        CellStyle titleStyle = overview ? st.overviewBanner : st.blockHeader;
        Cell b0 = banner.createCell(0);
        b0.setCellValue(title == null ? "" : title);
        b0.setCellStyle(titleStyle);
        for (int c = 1; c < COL_STATUS; c++) {
            banner.createCell(c).setCellStyle(titleStyle);
        }
        merge(sheet, rowIdx, 0, COL_STATUS - 1);

        String status = trangThai == null || trangThai.isBlank() ? "—" : trangThai.trim();
        Cell stCell = banner.createCell(COL_STATUS);
        stCell.setCellValue(status);
        stCell.setCellStyle(statusStyle(st, status));

        for (int c = COL_STATUS + 1; c < SHEET_COLS; c++) {
            banner.createCell(c).setCellStyle(titleStyle);
        }
        banner.setHeightInPoints(overview ? 24f : 22f);
    }

    /** Ưu tiên trạng thái đã ghi trong bước; không có thì suy từ kết quả kịch bản. */
    private static String resolveBlockStatus(LogEntry e, ReportBlock block, int index,
                                             List<ReportBlock> blocks) {
        String recorded = extractRecordedStepStatus(block.actions());
        if (recorded != null) {
            return recorded;
        }
        if (isOverviewBlock(block)) {
            return e.trangThai() == null || e.trangThai().isBlank() ? "—" : e.trangThai();
        }
        String bucket = statusBucket(e.trangThai());
        if (ST_BO_QUA.equals(bucket)) {
            return ST_BO_QUA;
        }
        if (ST_DAT.equals(bucket)) {
            return ST_DAT;
        }
        if (ST_DAT_CANH_BAO.equals(bucket)) {
            int step = extractStepNumber(block.title());
            return step == 6 ? ST_DAT_CANH_BAO : ST_DAT;
        }
        // Thất bại: các bước trước bước cuối = Đạt; bước cuối = Thất bại
        int last = lastNonOverviewIndex(blocks);
        if (index < last) {
            return ST_DAT;
        }
        if (index == last) {
            return ST_THAT_BAI;
        }
        return ST_BO_QUA;
    }

    private static String extractRecordedStepStatus(List<TestActionLog.Action> actions) {
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        for (int i = actions.size() - 1; i >= 0; i--) {
            TestActionLog.Action a = actions.get(i);
            if (TestActionLog.isTrangThaiBuoc(a)) {
                String v = a.giaTri();
                if (v != null && !v.isBlank()) {
                    return normalizeStatusLabel(v);
                }
            }
        }
        return null;
    }

    private static int extractStepNumber(String title) {
        if (title == null) {
            return -1;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)bước\\s+(\\d+)")
                .matcher(title);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private static int lastNonOverviewIndex(List<ReportBlock> blocks) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            if (!isOverviewBlock(blocks.get(i))) {
                return i;
            }
        }
        return blocks.size() - 1;
    }

    /**
     * Dữ liệu bước 2 cột đều: [Trường | Thao tác+giá trị] × 2 mỗi hàng.
     * Chỉ khi còn đúng 1 mục lẻ ở cuối mới merge thành 1 hàng full-width.
     */
    private static int writeDataBuocKemCotThaoTac(
            Sheet sheet, int r, Styles st,
            List<TaoDonExcelLogBuilder.Muc> data,
            List<TestActionLog.Action> actions) {
        List<TestActionLog.Action> remaining = new ArrayList<>(
                actions == null ? List.of() : actions);
        List<BuocItem> items = new ArrayList<>();
        java.util.LinkedHashSet<String> coveredKeys = new java.util.LinkedHashSet<>();

        if (data != null) {
            for (TaoDonExcelLogBuilder.Muc m : data) {
                TestActionLog.Action matched = takeMatchingAction(remaining, m.tenTruong());
                String giaTri = blankToDash(m.giaTri());
                if (m.ghiChu() != null && !m.ghiChu().isBlank()) {
                    giaTri = giaTri + "  ·  " + m.ghiChu().trim();
                }
                if (matched != null && matched.ghiChu() != null && !matched.ghiChu().isBlank()
                        && !giaTri.contains(matched.ghiChu().trim())) {
                    giaTri = giaTri + "  ·  " + matched.ghiChu().trim();
                }
                String tt = matched == null ? "" : nz(matched.thaoTac());
                String truong = blankToDash(m.tenTruong());
                items.add(new BuocItem(truong, giaTri, tt));
                String key = normFieldKey(truong);
                if (!key.isEmpty()) {
                    coveredKeys.add(key);
                }
            }
        }
        for (TestActionLog.Action a : remaining) {
            if (TestActionLog.isTrangThaiBuoc(a) || TestActionLog.isKhoi(a)) {
                continue;
            }
            String truong = blankToDash(a.truong());
            String key = normFieldKey(truong);
            if (!key.isEmpty() && isFieldKeyCovered(coveredKeys, key)) {
                continue;
            }
            String giaTri = a.giaTri() == null || a.giaTri().isBlank() ? "—" : a.giaTri().trim();
            if (a.ghiChu() != null && !a.ghiChu().isBlank()) {
                giaTri = giaTri + "  ·  " + a.ghiChu().trim();
            }
            items.add(new BuocItem(truong, giaTri, nz(a.thaoTac())));
            if (!key.isEmpty()) {
                coveredKeys.add(key);
            }
        }

        Workbook wb = sheet.getWorkbook();
        int n = items.size();
        for (int i = 0; i < n; i += 2) {
            BuocItem left = items.get(i);
            if (i == n - 1) {
                // Ô lẻ cuối cùng → merge 1 hàng
                r = writeBuocFullWidthRow(sheet, r, st, wb, left);
                break;
            }
            r = writeBuocPairRow(sheet, r, st, wb, left, items.get(i + 1));
        }
        return r;
    }

    private static boolean isFieldKeyCovered(java.util.Set<String> covered, String key) {
        if (covered.contains(key)) {
            return true;
        }
        for (String c : covered) {
            if (c.isEmpty()) {
                continue;
            }
            if ((c.contains(key) || key.contains(c)) && Math.min(c.length(), key.length()) >= 4) {
                return true;
            }
        }
        return false;
    }

    private static int writeBuocPairRow(Sheet sheet, int r, Styles st, Workbook wb,
                                        BuocItem left, BuocItem right) {
        Row row = sheet.createRow(r++);
        write(row, 0, left.truong(), st.pairLabel);
        writeThaoTacGiaTriCell(row, 1, left, st, wb);
        row.createCell(2).setCellStyle(st.pairValue);
        merge(sheet, row.getRowNum(), 1, 2);

        write(row, 3, right.truong(), st.pairLabel);
        writeThaoTacGiaTriCell(row, 4, right, st, wb);
        for (int c = 5; c < SHEET_COLS; c++) {
            row.createCell(c).setCellStyle(st.pairValue);
        }
        float h = 20f;
        h = Math.max(h, wrappedHeight(left.displayText(), 22));
        h = Math.max(h, wrappedHeight(right.displayText(), 22));
        row.setHeightInPoints(h);
        return r;
    }

    private static int writeBuocFullWidthRow(Sheet sheet, int r, Styles st, Workbook wb, BuocItem item) {
        Row row = sheet.createRow(r++);
        write(row, 0, item.truong(), st.pairLabel);
        writeThaoTacGiaTriCell(row, 1, item, st, wb);
        for (int c = 2; c <= 4; c++) {
            row.createCell(c).setCellStyle(st.pairValue);
        }
        merge(sheet, row.getRowNum(), 1, 4);
        for (int c = 5; c < SHEET_COLS; c++) {
            row.createCell(c).setCellStyle(st.pairValue);
        }
        row.setHeightInPoints(Math.max(22f, wrappedHeight(item.displayText(), 70)));
        return r;
    }

    private record BuocItem(String truong, String giaTri, String thaoTac) {
        String displayText() {
            if (thaoTac == null || thaoTac.isBlank()) {
                return giaTri == null ? "" : giaTri;
            }
            return thaoTac + "  →  " + (giaTri == null ? "" : giaTri);
        }
    }

    /** Ô gộp: thao tác (tô màu) + giá trị. */
    private static void writeThaoTacGiaTriCell(Row row, int col, BuocItem item, Styles st, Workbook wb) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(st.pairValue);
        String tt = item.thaoTac() == null ? "" : item.thaoTac().trim();
        String gt = item.giaTri() == null ? "" : item.giaTri();
        if (tt.isEmpty()) {
            cell.setCellValue(gt);
            return;
        }
        String full = tt + "  →  " + gt;
        if (wb instanceof XSSFWorkbook) {
            XSSFRichTextString rich = new XSSFRichTextString(full);
            XSSFFont tagFont = (XSSFFont) wb.createFont();
            tagFont.setBold(true);
            tagFont.setFontHeightInPoints((short) 9);
            tagFont.setColor(thaoTacFontColor(tt));
            XSSFFont bodyFont = (XSSFFont) wb.createFont();
            bodyFont.setBold(false);
            bodyFont.setFontHeightInPoints((short) 9);
            rich.applyFont(0, tt.length(), tagFont);
            rich.applyFont(tt.length(), full.length(), bodyFont);
            cell.setCellValue(rich);
        } else {
            cell.setCellValue(full);
        }
    }

    private static XSSFColor thaoTacFontColor(String thaoTac) {
        String t = thaoTac == null ? "" : thaoTac.trim();
        byte[] rgb = switch (t) {
            case "Điền" -> new byte[]{(byte) 0x1D, (byte) 0x4E, (byte) 0xD8};
            case "Chọn" -> new byte[]{(byte) 0x0E, (byte) 0x74, (byte) 0x90};
            case "Tải lên" -> new byte[]{(byte) 0x7E, (byte) 0x22, (byte) 0xCE};
            case "Bỏ qua" -> new byte[]{(byte) 0xB4, (byte) 0x53, (byte) 0x09};
            case "Click", "Bấm" -> new byte[]{(byte) 0x33, (byte) 0x41, (byte) 0x55};
            default -> new byte[]{(byte) 0x0F, (byte) 0x17, (byte) 0x2A};
        };
        return new XSSFColor(rgb, null);
    }

    /** Lấy thao tác UI khớp tên trường (ưu tiên khớp tốt nhất), rồi xóa khỏi danh sách còn lại. */
    private static TestActionLog.Action takeMatchingAction(
            List<TestActionLog.Action> remaining, String tenTruong) {
        if (remaining == null || remaining.isEmpty() || tenTruong == null || tenTruong.isBlank()) {
            return null;
        }
        String key = normFieldKey(tenTruong);
        if (key.isEmpty()) {
            return null;
        }
        int bestIdx = -1;
        int bestScore = 0;
        for (int i = 0; i < remaining.size(); i++) {
            if (TestActionLog.isTrangThaiBuoc(remaining.get(i)) || TestActionLog.isKhoi(remaining.get(i))) {
                continue;
            }
            String ak = normFieldKey(remaining.get(i).truong());
            if (ak.isEmpty()) {
                continue;
            }
            int score = 0;
            if (ak.equals(key)) {
                score = 100;
            } else if (ak.contains(key) || key.contains(ak)) {
                score = Math.min(ak.length(), key.length());
                if (score < 4) {
                    score = 0;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestIdx = i;
            }
        }
        if (bestIdx < 0) {
            return null;
        }
        return remaining.remove(bestIdx);
    }

    private static String normFieldKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.toLowerCase(java.util.Locale.ROOT).trim();
        s = s.replaceAll("\\([^)]*\\)", " ");
        s = s.replaceAll("[\\[\\]#→:\\-–—|/]+", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private static boolean isOverviewBlock(ReportBlock block) {
        if (block == null || block.title() == null) {
            return false;
        }
        String t = block.title().toLowerCase(java.util.Locale.ROOT);
        return t.contains("tổng quan") || t.startsWith("0.");
    }

    /** Thẻ tổng quan gọn: hero + cặp key-value + luồng. */
    private static int writeOverviewCard(Sheet sheet, int r, Styles st, TaoDonScenario s) {
        String loaiViec = (s.loaiViec() == null || s.loaiViec().isBlank())
                ? (DataDictionary.isPhaSan(s.loaiDon()) ? "Không dùng loại việc (Phá sản)" : "—")
                : s.loaiViec().trim();
        String hero = nz(s.loaiDon()) + "  ›  " + loaiViec;
        Row heroRow = sheet.createRow(r++);
        Cell h0 = heroRow.createCell(0);
        h0.setCellValue(hero);
        h0.setCellStyle(st.overviewHero);
        for (int c = 1; c < SHEET_COLS; c++) {
            heroRow.createCell(c).setCellStyle(st.overviewHero);
        }
        merge(sheet, heroRow.getRowNum(), 0, Math.min(4, SHEET_COLS - 1));
        heroRow.setHeightInPoints(Math.max(24f, wrappedHeight(hero, 70)));

        r = writeOverviewFull(sheet, r, st, "Tòa án nhận đơn", blankToDash(s.toaAn()));

        String nguyen = moTaChuTheOverview(s.loaiChuThe());
        String bi = moTaChuTheOverview(s.loaiBiDon());
        if (s.soLuongBiDon() > 1) {
            bi = bi + "  × " + s.soLuongBiDon();
        } else if (DataDictionary.isPhaSan(s.loaiDon())) {
            bi = "Doanh nghiệp / HTX bị yêu cầu";
        } else if (DataDictionary.isHanhChinh(s.loaiDon())) {
            bi = "Cơ quan hành chính";
        }
        r = writeOverviewPair(sheet, r, st, "Nguyên đơn", nguyen, "Bị đơn / bên bị kiện", bi);

        r = writeOverviewPair(sheet, r, st,
                "Người liên quan", yesNoOverview(s.coNguoiLienQuan()),
                "Tài liệu bổ sung", yesNoOverview(s.coTaiLieuBoSung()));

        if (DataDictionary.isPhaSan(s.loaiDon())) {
            r = writeOverviewFull(sheet, r, st, "Tư cách người nộp đơn",
                    (s.tuCachNopDon() == null || s.tuCachNopDon().isBlank()) ? "—" : s.tuCachNopDon());
        }

        r = writeOverviewFull(sheet, r, st, "Luồng kiểm thử",
                "Nộp đơn mới → Loại đơn → Nguyên đơn → Bị đơn → Nội dung → Tài liệu → Xem lại / Gửi");
        return r;
    }

    private static int writeOverviewFull(Sheet sheet, int r, Styles st, String label, String value) {
        Row row = sheet.createRow(r++);
        write(row, 0, label, st.overviewLabel);
        Cell v = row.createCell(1);
        v.setCellValue(value == null ? "—" : value);
        v.setCellStyle(st.overviewValue);
        for (int c = 2; c <= 4; c++) {
            row.createCell(c).setCellStyle(st.overviewValue);
        }
        merge(sheet, row.getRowNum(), 1, 4);
        for (int c = 5; c < SHEET_COLS; c++) {
            row.createCell(c).setCellStyle(st.overviewValue);
        }
        row.setHeightInPoints(Math.max(20f, wrappedHeight(value, 55)));
        return r;
    }

    private static int writeOverviewPair(Sheet sheet, int r, Styles st,
                                         String k1, String v1, String k2, String v2) {
        Row row = sheet.createRow(r++);
        write(row, 0, k1, st.overviewLabel);
        write(row, 1, blankToDash(v1), st.overviewValue);
        write(row, 2, k2, st.overviewLabel);
        Cell v = row.createCell(3);
        v.setCellValue(blankToDash(v2));
        v.setCellStyle(st.overviewValue);
        row.createCell(4).setCellStyle(st.overviewValue);
        merge(sheet, row.getRowNum(), 3, 4);
        for (int c = 5; c < SHEET_COLS; c++) {
            row.createCell(c).setCellStyle(st.overviewValue);
        }
        float h = 20f;
        h = Math.max(h, wrappedHeight(v1, 18));
        h = Math.max(h, wrappedHeight(v2, 22));
        row.setHeightInPoints(h);
        return r;
    }

    private static void writeOverviewKvRow(Row row, Styles st, String label, String value) {
        write(row, 0, label, st.overviewLabel);
        Cell v = row.createCell(1);
        v.setCellValue(value);
        v.setCellStyle(st.overviewValue);
        for (int c = 2; c <= 4; c++) {
            row.createCell(c).setCellStyle(st.overviewValue);
        }
        // merge handled by caller if needed — single row without merge ok for fallback
        for (int c = 5; c < SHEET_COLS; c++) {
            row.createCell(c).setCellStyle(st.overviewValue);
        }
        row.setHeightInPoints(20f);
    }

    private static String moTaChuTheOverview(String loai) {
        if (loai == null || loai.isBlank()) {
            return "—";
        }
        return DataDictionary.isToChuc(loai) ? "Tổ chức / doanh nghiệp" : "Cá nhân";
    }

    private static String yesNoOverview(String v) {
        if (v != null && v.trim().equalsIgnoreCase("có")) {
            return "Có";
        }
        return "Không";
    }

    /**
     * Khối báo cáo: dữ liệu kịch bản (Muc) + thao tác UI cùng một bước/page.
     */
    private static List<ReportBlock> buildReportBlocks(TaoDonScenario scenario,
                                                       List<TestActionLog.Action> actions) {
        List<ActionBlock> actionBlocks = groupIntoBlocks(actions == null ? List.of() : actions);
        java.util.LinkedHashMap<String, List<TestActionLog.Action>> actionsByKey = new java.util.LinkedHashMap<>();
        for (ActionBlock ab : actionBlocks) {
            actionsByKey.put(blockMatchKey(ab.title()), new ArrayList<>(ab.actions()));
        }

        List<ReportBlock> out = new ArrayList<>();
        java.util.Set<String> usedActionKeys = new java.util.HashSet<>();

        if (scenario != null) {
            List<TaoDonExcelLogBuilder.Muc> mucs = TaoDonExcelLogBuilder.build(scenario);
            java.util.LinkedHashMap<String, List<TaoDonExcelLogBuilder.Muc>> byBuoc =
                    new java.util.LinkedHashMap<>();
            for (TaoDonExcelLogBuilder.Muc m : mucs) {
                String buoc = m.buoc() == null || m.buoc().isBlank() ? "Khác" : m.buoc().trim();
                byBuoc.computeIfAbsent(buoc, k -> new ArrayList<>()).add(m);
            }

            List<ReportBlock> stepBlocks = new ArrayList<>();
            ReportBlock overviewBlock = null;

            for (var en : byBuoc.entrySet()) {
                String buoc = en.getKey();
                String key = blockMatchKey(buoc);
                List<TestActionLog.Action> matched = actionsByKey.getOrDefault(key, List.of());
                if (!matched.isEmpty()) {
                    usedActionKeys.add(key);
                } else {
                    String stepKey = stepOnlyKey(buoc);
                    if (stepKey != null) {
                        for (var ae : actionsByKey.entrySet()) {
                            if (usedActionKeys.contains(ae.getKey())) {
                                continue;
                            }
                            if (ae.getKey().startsWith(stepKey)) {
                                matched = ae.getValue();
                                usedActionKeys.add(ae.getKey());
                                break;
                            }
                        }
                    }
                }
                ReportBlock rb = new ReportBlock(buoc, pageForBuocTitle(buoc),
                        List.copyOf(en.getValue()), List.copyOf(matched));
                if (isOverviewBlock(rb)) {
                    overviewBlock = rb;
                } else {
                    stepBlocks.add(rb);
                }
            }

            if (overviewBlock != null) {
                out.add(overviewBlock);
            }
            // Mở biểu mẫu sau tổng quan, trước các bước điền
            attachOrphanActionBlock(out, actionsByKey, usedActionKeys, "mở biểu mẫu",
                    "Mở biểu mẫu Nộp đơn mới", "Bảng điều khiển");
            out.addAll(stepBlocks);
        }

        // Thao tác còn lại (login / bước không có Muc)
        for (ActionBlock ab : actionBlocks) {
            String key = blockMatchKey(ab.title());
            if (usedActionKeys.contains(key)) {
                continue;
            }
            out.add(new ReportBlock(ab.title(), ab.page(), List.of(), ab.actions()));
            usedActionKeys.add(key);
        }
        return out;
    }

    private static void attachOrphanActionBlock(
            List<ReportBlock> out,
            java.util.Map<String, List<TestActionLog.Action>> actionsByKey,
            java.util.Set<String> usedActionKeys,
            String keyContains,
            String defaultTitle,
            String page) {
        for (var ae : new ArrayList<>(actionsByKey.entrySet())) {
            if (usedActionKeys.contains(ae.getKey())) {
                continue;
            }
            if (ae.getKey().contains(keyContains)) {
                out.add(new ReportBlock(
                        ae.getKey().isBlank() ? defaultTitle : titleFromKey(ae.getKey(), defaultTitle),
                        page, List.of(), List.copyOf(ae.getValue())));
                usedActionKeys.add(ae.getKey());
            }
        }
    }

    private static String titleFromKey(String key, String fallback) {
        // keys are lowercased; prefer original from fallback if key is just token
        if (key == null || key.isBlank()) {
            return fallback;
        }
        if (key.contains("mở biểu mẫu") || key.contains("mở form") || key.contains("nộp đơn")) {
            return "Mở biểu mẫu Nộp đơn mới";
        }
        if (key.contains("đăng nhập")) {
            return "Đăng nhập hệ thống";
        }
        return fallback;
    }

    private static String blockMatchKey(String title) {
        if (title == null) {
            return "";
        }
        String t = title.toLowerCase(java.util.Locale.ROOT).trim();
        // Chuẩn hóa: bỏ phần page sau "·", bỏ số lượng
        int dot = t.indexOf('·');
        if (dot > 0) {
            t = t.substring(0, dot).trim();
        }
        return t.replaceAll("\\s+", " ");
    }

    private static String stepOnlyKey(String buocTitle) {
        if (buocTitle == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)bước\\s+(\\d+)")
                .matcher(buocTitle);
        if (m.find()) {
            return "bước " + m.group(1);
        }
        if (buocTitle.trim().startsWith("0.")) {
            return "0.";
        }
        if (buocTitle.toLowerCase(java.util.Locale.ROOT).contains("checklist")) {
            return "checklist";
        }
        return null;
    }

    private static String pageForBuocTitle(String buoc) {
        if (buoc == null) {
            return "";
        }
        String t = buoc.toLowerCase(java.util.Locale.ROOT);
        if (t.startsWith("0.") || t.contains("tổng quan")) {
            return "Kịch bản";
        }
        if (t.contains("checklist") || t.contains("danh sách kiểm")) {
            return "Danh sách kiểm";
        }
        return inferPageName(buoc);
    }

    private long totalDurationMs() {
        long t = 0;
        for (LogEntry e : entries) {
            t += Math.max(0, e.durationMs());
        }
        return t;
    }

    private String overallStatus() {
        boolean anyFail = false;
        boolean anyWarn = false;
        boolean anySkip = false;
        for (LogEntry e : entries) {
            String k = e.trangThai() == null ? "" : e.trangThai().toLowerCase();
            if (k.contains("fail") || k.contains("thất bại")) {
                anyFail = true;
            } else if (k.contains("cảnh báo")) {
                anyWarn = true;
            } else if (k.contains("skip") || k.contains("bỏ qua")) {
                anySkip = true;
            }
        }
        if (anyFail) {
            return ST_THAT_BAI;
        }
        if (anyWarn) {
            return ST_DAT_CANH_BAO;
        }
        if (anySkip && entries.stream().allMatch(e -> {
            String k = e.trangThai() == null ? "" : e.trangThai().toLowerCase();
            return k.contains("skip") || k.contains("bỏ qua");
        })) {
            return ST_BO_QUA;
        }
        return ST_DAT;
    }

    private static CellStyle kpiOverallStyle(Styles st, String status) {
        if (status == null) {
            return st.kpiValue;
        }
        String k = status.toLowerCase();
        if (k.contains("fail") || k.contains("thất bại")) {
            return st.kpiFail;
        }
        if (k.contains("cảnh báo") || k.contains("skip") || k.contains("bỏ qua")) {
            return st.kpiWarn;
        }
        return st.kpiPass;
    }

    // ─── Shared helpers ─────────────────────────────────────────────────────

    private static List<TestActionLog.Action> compactActions(List<TestActionLog.Action> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<TestActionLog.Action> out = new ArrayList<>();
        for (TestActionLog.Action a : raw) {
            if (TestActionLog.isKhoi(a) || TestActionLog.isTrangThaiBuoc(a)) {
                out.add(a);
                continue;
            }
            if (!keepForExcel(a)) {
                continue;
            }
            out.add(new TestActionLog.Action(
                    a.thaoTac(),
                    cleanFieldLabel(a.truong()),
                    a.giaTri() == null ? "" : a.giaTri(),
                    a.ghiChu() == null ? "" : a.ghiChu()));
        }
        return out;
    }

    /**
     * Gom thao tác thành khối theo marker {@code KHỐI} (từng page/bước).
     * Nếu không có marker thì suy luận theo nhãn trường.
     */
    private static List<ActionBlock> groupIntoBlocks(List<TestActionLog.Action> actions) {
        boolean hasKhoi = false;
        for (TestActionLog.Action a : actions) {
            if (TestActionLog.isKhoi(a)) {
                hasKhoi = true;
                break;
            }
        }
        if (hasKhoi) {
            List<ActionBlock> blocks = new ArrayList<>();
            String title = null;
            String page = "";
            List<TestActionLog.Action> buf = new ArrayList<>();
            for (TestActionLog.Action a : actions) {
                if (TestActionLog.isKhoi(a)) {
                    if (title != null) {
                        blocks.add(new ActionBlock(title, page, List.copyOf(buf)));
                    }
                    buf.clear();
                    title = (a.truong() == null || a.truong().isBlank()) ? "Thao tác" : a.truong().trim();
                    page = a.giaTri() == null ? "" : a.giaTri().trim();
                    continue;
                }
                if (title == null) {
                    title = "Thao tác trước các bước";
                    page = "";
                }
                buf.add(a);
            }
            if (title != null) {
                blocks.add(new ActionBlock(title, page, List.copyOf(buf)));
            }
            return blocks;
        }

        List<ActionBlock> blocks = new ArrayList<>();
        String title = null;
        String page = "";
        List<TestActionLog.Action> buf = new ArrayList<>();
        for (TestActionLog.Action a : actions) {
            String inferred = inferBlockTitle(a);
            if (title == null) {
                title = inferred;
                page = inferPageName(inferred);
            } else if (!inferred.equals(title)) {
                blocks.add(new ActionBlock(title, page, List.copyOf(buf)));
                buf.clear();
                title = inferred;
                page = inferPageName(inferred);
            }
            buf.add(a);
        }
        if (title != null) {
            blocks.add(new ActionBlock(title, page, List.copyOf(buf)));
        }
        return blocks;
    }

    private static int countDataActions(List<TestActionLog.Action> actions) {
        int n = 0;
        for (TestActionLog.Action a : actions) {
            if (!TestActionLog.isKhoi(a)) {
                n++;
            }
        }
        return n;
    }

    private static String inferBlockTitle(TestActionLog.Action a) {
        String f = ((a.truong() == null ? "" : a.truong()) + " " + (a.giaTri() == null ? "" : a.giaTri()))
                .toLowerCase();
        String t = a.thaoTac() == null ? "" : a.thaoTac().toLowerCase();
        if (f.contains("đăng nhập") || f.contains("captcha") || f.contains("mật khẩu")
                || f.contains("cccd/tên đăng nhập")) {
            return "Đăng nhập hệ thống";
        }
        if (f.contains("nộp đơn mới") || f.contains("bắt đầu mới")) {
            return "Mở biểu mẫu Nộp đơn mới";
        }
        if (f.contains("tài liệu") || "tải lên".equals(t)) {
            return "Bước 5 — " + TaoDonReportBuilder.tenBuocDayDu(5);
        }
        if (f.contains("gửi đơn") || f.contains("xác nhận thông tin") || f.contains("chỉnh sửa")) {
            return "Bước 6 — " + TaoDonReportBuilder.tenBuocDayDu(6);
        }
        if (f.contains("thời điểm") || f.contains("tranh chấp") || f.contains("quá trình sự việc")
                || f.contains("yêu cầu cụ thể") || f.contains("căn cứ pháp lý")) {
            return "Bước 4 — " + TaoDonReportBuilder.tenBuocDayDu(4);
        }
        if (f.contains("bị đơn") || f.contains("bị kiện") || f.contains("bị yêu cầu")
                || f.contains("cơ quan") || f.contains("thêm bị") || f.contains("người liên quan")
                || f.contains("doanh nghiệp bị yêu cầu")) {
            return "Bước 3 — " + TaoDonReportBuilder.tenBuocDayDu(3);
        }
        if (f.contains("loại đơn") || f.contains("loại việc") || f.contains("tòa án")
                || f.contains("tóm tắt sơ bộ")) {
            return "Bước 1 — " + TaoDonReportBuilder.tenBuocDayDu(1);
        }
        if (f.contains("loại chủ thể") || f.contains("ngày sinh") || f.contains("giới tính")
                || f.contains("thường trú") || f.contains("liên lạc") || f.contains("ngày cấp")
                || f.contains("nơi cấp") || f.contains("đại diện pháp lý") || f.contains("quan hệ đại diện")
                || f.contains("tư cách") || f.contains("họ và tên") || f.contains("cccd")) {
            return "Bước 2 — " + TaoDonReportBuilder.tenBuocDayDu(2);
        }
        return "Thao tác trên hệ thống";
    }

    private static String inferPageName(String blockTitle) {
        if (blockTitle == null) {
            return "";
        }
        String t = blockTitle.toLowerCase();
        if (t.contains("đăng nhập")) {
            return "Màn đăng nhập";
        }
        if (t.contains("mở biểu mẫu") || t.contains("mở form") || t.contains("nộp đơn mới")) {
            return "Bảng điều khiển";
        }
        if (t.startsWith("bước 1")) {
            return "Trang tạo đơn";
        }
        if (t.startsWith("bước 2")) {
            return "Trang nguyên đơn";
        }
        if (t.startsWith("bước 3")) {
            return "Trang bị đơn";
        }
        if (t.startsWith("bước 4") || t.contains("cập nhật lại nội dung")) {
            return "Trang nội dung đơn";
        }
        if (t.startsWith("bước 5")) {
            return "Trang tài liệu";
        }
        if (t.startsWith("bước 6") || t.contains("xem lại")) {
            return "Trang xem lại";
        }
        return "";
    }

    private record ActionBlock(String title, String page, List<TestActionLog.Action> actions) {
    }

    private record ReportBlock(
            String title,
            String page,
            List<TaoDonExcelLogBuilder.Muc> data,
            List<TestActionLog.Action> actions
    ) {
    }

    private static boolean keepForExcel(TestActionLog.Action a) {
        String t = a.thaoTac() == null ? "" : a.thaoTac().trim();
        String field = a.truong() == null ? "" : a.truong().toLowerCase();
        if (t.equalsIgnoreCase("Tìm kiếm dropdown")
                || t.equalsIgnoreCase("Tìm kiếm danh sách thả xuống")
                || t.equalsIgnoreCase("Ghi chú")) {
            return false;
        }
        if (t.equalsIgnoreCase("Bấm") || t.equalsIgnoreCase("Click")) {
            return field.contains("gửi đơn")
                    || field.contains("chỉnh sửa")
                    || field.contains("bắt đầu mới")
                    || field.contains("thêm bị")
                    || field.contains("thêm người")
                    || field.contains("đăng nhập")
                    || field.contains("nộp đơn");
        }
        return t.equalsIgnoreCase("Điền")
                || t.equalsIgnoreCase("Chọn")
                || t.equalsIgnoreCase("Tải lên")
                || t.equalsIgnoreCase("Bỏ qua")
                || t.equals("—");
    }

    private static String cleanFieldLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        int a = s.indexOf('[');
        int b = s.lastIndexOf(']');
        if (a >= 0 && b > a) {
            String inside = s.substring(a + 1, b).trim();
            String prefix = s.substring(0, a).trim().toLowerCase();
            if (prefix.startsWith("thẻ ")) {
                String name = s.substring(0, a).replaceFirst("(?i)^Thẻ\\s+", "").replace(":", "").trim();
                return name.isBlank() ? inside : name;
            }
            if (prefix.contains("nút") || prefix.contains("checkbox") || prefix.contains("hộp kiểm")
                    || prefix.contains("toggle") || prefix.contains("nút chuyển")
                    || prefix.contains("dropdown") || prefix.contains("danh sách thả xuống")
                    || prefix.contains("ô nhập") || prefix.contains("ô ")) {
                return inside;
            }
            return inside.isBlank() ? s : inside;
        }
        return s;
    }

    private static void merge(Sheet sheet, int row, int c1, int c2) {
        if (c2 > c1) {
            CellRangeAddress range = new CellRangeAddress(row, row, c1, c2);
            sheet.addMergedRegion(range);
            // Giữ khung sau merge (POI thường làm mất viền ô gộp)
            RegionUtil.setBorderTop(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, range, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, range, sheet);
        }
    }

    private static void write(Row row, int col, String text, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(text == null ? "" : text);
        cell.setCellStyle(style);
    }

    private static void autoFitColumns(Sheet sheet, int colCount, int[] hintChars) {
        applyFixedColumnWidths(sheet, colCount, hintChars);
    }

    /** Width cố định theo hint; cột STT (0) luôn 20. */
    private static void applyFixedColumnWidths(Sheet sheet, int colCount, int[] hintChars) {
        for (int i = 0; i < colCount; i++) {
            int chars;
            if (i == 0) {
                chars = 20;
            } else {
                int hint = (hintChars != null && i < hintChars.length) ? hintChars[i] : 14;
                chars = Math.min(MAX_TEXT_CHARS, Math.max(5, hint));
            }
            sheet.setColumnWidth(i, chars * 256);
        }
    }

    /** Đóng khung ngoài (MEDIUM) quanh vùng dữ liệu. */
    private static void frameRange(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        if (lastRow < firstRow || lastCol < firstCol) {
            return;
        }
        CellRangeAddress range = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
        RegionUtil.setBorderTop(BorderStyle.MEDIUM, range, sheet);
        RegionUtil.setBorderBottom(BorderStyle.MEDIUM, range, sheet);
        RegionUtil.setBorderLeft(BorderStyle.MEDIUM, range, sheet);
        RegionUtil.setBorderRight(BorderStyle.MEDIUM, range, sheet);
    }

    private static CellStyle statusStyle(Styles st, String status) {
        if (status == null || status.isBlank() || "—".equals(status.trim())) {
            return st.masterCenter;
        }
        String k = status.toLowerCase();
        if (k.contains("fail") || k.contains("thất bại")) {
            return st.fail;
        }
        if (k.contains("bỏ qua") || k.contains("skip") || k.contains("blocked")) {
            return st.warn;
        }
        if (k.contains("cảnh báo") || k.contains("warn")) {
            return st.warn;
        }
        return st.pass;
    }

    private static CellStyle thaoTacStyle(Styles st, String thaoTac) {
        if (thaoTac == null) {
            return st.detailCenter;
        }
        return switch (thaoTac) {
            case "Bỏ qua" -> st.warn;
            case "Tải lên" -> st.tagUpload;
            case "Chọn" -> st.tagChon;
            case "Điền" -> st.tagDien;
            case "Dữ liệu" -> st.tagData;
            case "Bấm", "Click" -> st.detailCenter;
            default -> st.detailCenter;
        };
    }

    private static String blankToDash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    private static float wrappedHeight(String text, int colChars) {
        if (text == null || text.isBlank()) {
            return 20f;
        }
        // Ước lượng rộng hơn (font VN) — không cắt chữ
        int width = Math.max(6, colChars);
        String[] parts = text.split("\r?\n", -1);
        int lines = 0;
        for (String p : parts) {
            int len = p.isEmpty() ? 1 : p.length();
            lines += Math.max(1, (int) Math.ceil(len / (width * 0.78)));
        }
        return Math.min(409f, Math.max(20f, lines * 16.5f + 8f));
    }

    private record LogEntry(
            String testCaseId,
            String moTa,
            String description,
            String ketQuaMongDoi,
            String ketQuaThucTe,
            String trangThai,
            String ghiChu,
            TaoDonScenario scenario,
            List<TestActionLog.Action> actions,
            long durationMs,
            int seq
    ) {
    }

    // ─── Styles: Professional Slate / Corporate Blue ────────────────────────

    private static final class Styles {
        private static final byte[] NAVY = {(byte) 0x1E, (byte) 0x3A, (byte) 0x8A};
        private static final byte[] SLATE = {(byte) 0x0F, (byte) 0x17, (byte) 0x2A};
        private static final byte[] BG_LIGHT = {(byte) 0xF8, (byte) 0xFA, (byte) 0xFC};
        private static final byte[] BORDER = {(byte) 0x64, (byte) 0x74, (byte) 0x8B};
        private static final byte[] ZEBRA = {(byte) 0xF1, (byte) 0xF5, (byte) 0xF9};
        private static final byte[] MASTER = {(byte) 0xDB, (byte) 0xEA, (byte) 0xFE};
        private static final byte[] DETAIL_HDR = {(byte) 0xE0, (byte) 0xE7, (byte) 0xFF};
        private static final byte[] PASS_BG = {(byte) 0xD1, (byte) 0xFA, (byte) 0xE5};
        private static final byte[] FAIL_BG = {(byte) 0xFE, (byte) 0xE2, (byte) 0xE2};
        private static final byte[] WARN_BG = {(byte) 0xFE, (byte) 0xF3, (byte) 0xC7};
        private static final byte[] KPI_CARD = {(byte) 0xEF, (byte) 0xF6, (byte) 0xFF};

        final CellStyle dashTitle;
        final CellStyle header;
        final CellStyle sectionHeader;
        final CellStyle blockHeader;
        final CellStyle subBlockHeader;
        final CellStyle overviewBanner;
        final CellStyle overviewHero;
        final CellStyle overviewLabel;
        final CellStyle overviewValue;
        final CellStyle infoMeta;
        final CellStyle tocCenter;
        final CellStyle tocCenterZebra;
        final CellStyle tocValueZebra;
        final CellStyle tocId;
        final CellStyle tocIdZebra;
        final CellStyle pairLabel;
        final CellStyle pairValue;
        final CellStyle kpiLabel;
        final CellStyle kpiValue;
        final CellStyle kpiPass;
        final CellStyle kpiFail;
        final CellStyle kpiWarn;
        final CellStyle kvKey;
        final CellStyle kvValue;
        final CellStyle kvKeyZebra;
        final CellStyle kvValueZebra;
        final CellStyle hint;
        final CellStyle masterWrap;
        final CellStyle masterCenter;
        final CellStyle detailHeader;
        final CellStyle detailWrap;
        final CellStyle detailValue;
        final CellStyle detailCenter;
        final CellStyle pass;
        final CellStyle fail;
        final CellStyle warn;
        final CellStyle tagDien;
        final CellStyle tagChon;
        final CellStyle tagUpload;
        final CellStyle tagData;
        final CellStyle durationNum;
        final CellStyle hyperlink;

        Styles(Workbook wb) {
            XSSFColor navy = rgb(NAVY);
            XSSFColor slate = rgb(SLATE);
            XSSFColor bgLight = rgb(BG_LIGHT);
            XSSFColor border = rgb(BORDER);
            XSSFColor zebra = rgb(ZEBRA);
            XSSFColor master = rgb(MASTER);
            XSSFColor detailHdr = rgb(DETAIL_HDR);
            XSSFColor passBg = rgb(PASS_BG);
            XSSFColor failBg = rgb(FAIL_BG);
            XSSFColor warnBg = rgb(WARN_BG);
            XSSFColor kpiCard = rgb(KPI_CARD);

            Font whiteTitle = wb.createFont();
            whiteTitle.setBold(true);
            whiteTitle.setFontHeightInPoints((short) 14);
            whiteTitle.setColor(IndexedColors.WHITE.getIndex());

            Font whiteBold = wb.createFont();
            whiteBold.setBold(true);
            whiteBold.setColor(IndexedColors.WHITE.getIndex());
            whiteBold.setFontHeightInPoints((short) 11);

            Font bold = wb.createFont();
            bold.setBold(true);
            bold.setFontHeightInPoints((short) 10);

            Font boldLarge = wb.createFont();
            boldLarge.setBold(true);
            boldLarge.setFontHeightInPoints((short) 14);

            Font smallMuted = wb.createFont();
            smallMuted.setFontHeightInPoints((short) 9);
            if (smallMuted instanceof XSSFFont xf) {
                xf.setColor(slate);
            }

            Font italic = wb.createFont();
            italic.setItalic(true);
            italic.setFontHeightInPoints((short) 9);

            dashTitle = wb.createCellStyle();
            dashTitle.setFont(whiteTitle);
            dashTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dashTitle.setAlignment(HorizontalAlignment.LEFT);
            dashTitle.setVerticalAlignment(VerticalAlignment.CENTER);
            dashTitle.setWrapText(true);
            ((XSSFCellStyle) dashTitle).setFillForegroundColor(navy);
            thinBorder(dashTitle, border);

            header = wb.createCellStyle();
            header.setFont(whiteBold);
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            header.setWrapText(true);
            ((XSSFCellStyle) header).setFillForegroundColor(navy);
            thinBorder(header, border);

            sectionHeader = wb.createCellStyle();
            sectionHeader.setFont(whiteBold);
            sectionHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            ((XSSFCellStyle) sectionHeader).setFillForegroundColor(slate);
            thinBorder(sectionHeader, border);

            XSSFColor blockBg = rgb(new byte[]{(byte) 0x33, (byte) 0x41, (byte) 0x55});
            blockHeader = wb.createCellStyle();
            blockHeader.setFont(whiteBold);
            blockHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            blockHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            blockHeader.setAlignment(HorizontalAlignment.LEFT);
            blockHeader.setWrapText(true);
            ((XSSFCellStyle) blockHeader).setFillForegroundColor(blockBg);
            thinBorder(blockHeader, border);

            XSSFColor subBg = rgb(new byte[]{(byte) 0xE2, (byte) 0xE8, (byte) 0xF0});
            Font subFont = wb.createFont();
            subFont.setBold(true);
            subFont.setFontHeightInPoints((short) 9);
            subBlockHeader = wb.createCellStyle();
            subBlockHeader.setFont(subFont);
            subBlockHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            subBlockHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            ((XSSFCellStyle) subBlockHeader).setFillForegroundColor(subBg);
            thinBorder(subBlockHeader, border);

            XSSFColor overviewBannerBg = rgb(new byte[]{(byte) 0x1E, (byte) 0x40, (byte) 0xAF});
            overviewBanner = wb.createCellStyle();
            overviewBanner.setFont(whiteBold);
            overviewBanner.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            overviewBanner.setVerticalAlignment(VerticalAlignment.CENTER);
            overviewBanner.setAlignment(HorizontalAlignment.LEFT);
            overviewBanner.setWrapText(true);
            ((XSSFCellStyle) overviewBanner).setFillForegroundColor(overviewBannerBg);
            thinBorder(overviewBanner, border);

            XSSFColor heroBg = rgb(new byte[]{(byte) 0xDB, (byte) 0xEA, (byte) 0xFE});
            Font heroFont = wb.createFont();
            heroFont.setBold(true);
            heroFont.setFontHeightInPoints((short) 11);
            overviewHero = wb.createCellStyle();
            overviewHero.setFont(heroFont);
            overviewHero.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            overviewHero.setVerticalAlignment(VerticalAlignment.CENTER);
            overviewHero.setAlignment(HorizontalAlignment.LEFT);
            overviewHero.setWrapText(true);
            ((XSSFCellStyle) overviewHero).setFillForegroundColor(heroBg);
            thinBorder(overviewHero, border);

            XSSFColor ovLabelBg = rgb(new byte[]{(byte) 0xF1, (byte) 0xF5, (byte) 0xF9});
            overviewLabel = wb.createCellStyle();
            overviewLabel.setFont(bold);
            overviewLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            overviewLabel.setVerticalAlignment(VerticalAlignment.CENTER);
            overviewLabel.setWrapText(true);
            ((XSSFCellStyle) overviewLabel).setFillForegroundColor(ovLabelBg);
            thinBorder(overviewLabel, border);

            overviewValue = wb.createCellStyle();
            overviewValue.setWrapText(true);
            overviewValue.setVerticalAlignment(VerticalAlignment.CENTER);
            overviewValue.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            ((XSSFCellStyle) overviewValue).setFillForegroundColor(bgLight);
            thinBorder(overviewValue, border);

            infoMeta = wb.createCellStyle();
            infoMeta.setFont(smallMuted);
            infoMeta.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            infoMeta.setVerticalAlignment(VerticalAlignment.CENTER);
            ((XSSFCellStyle) infoMeta).setFillForegroundColor(rgb(new byte[]{(byte) 0xEE, (byte) 0xF2, (byte) 0xFF}));
            thinBorder(infoMeta, border);

            kpiLabel = wb.createCellStyle();
            kpiLabel.setFont(smallMuted);
            kpiLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kpiLabel.setAlignment(HorizontalAlignment.CENTER);
            kpiLabel.setVerticalAlignment(VerticalAlignment.CENTER);
            ((XSSFCellStyle) kpiLabel).setFillForegroundColor(kpiCard);
            thinBorder(kpiLabel, border);

            kpiValue = wb.createCellStyle();
            kpiValue.setFont(boldLarge);
            kpiValue.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kpiValue.setAlignment(HorizontalAlignment.CENTER);
            kpiValue.setVerticalAlignment(VerticalAlignment.CENTER);
            kpiValue.setWrapText(true);
            ((XSSFCellStyle) kpiValue).setFillForegroundColor(bgLight);
            thinBorder(kpiValue, border);

            kpiPass = cloneFill(wb, kpiValue, boldLarge, passBg, border);
            kpiFail = cloneFill(wb, kpiValue, boldLarge, failBg, border);
            kpiWarn = cloneFill(wb, kpiValue, boldLarge, warnBg, border);

            kvKey = wb.createCellStyle();
            kvKey.setFont(bold);
            kvKey.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            kvKey.setVerticalAlignment(VerticalAlignment.CENTER);
            kvKey.setWrapText(true);
            ((XSSFCellStyle) kvKey).setFillForegroundColor(bgLight);
            thinBorder(kvKey, border);

            kvValue = wb.createCellStyle();
            kvValue.setWrapText(true);
            kvValue.setVerticalAlignment(VerticalAlignment.CENTER);
            ((XSSFCellStyle) kvValue).setFillForegroundColor(bgLight);
            thinBorder(kvValue, border);

            kvKeyZebra = cloneFill(wb, kvKey, bold, zebra, border);
            kvValueZebra = cloneFill(wb, kvValue, null, zebra, border);

            tocCenter = wb.createCellStyle();
            tocCenter.cloneStyleFrom(kvValue);
            tocCenter.setAlignment(HorizontalAlignment.CENTER);
            tocCenter.setVerticalAlignment(VerticalAlignment.CENTER);
            tocCenter.setWrapText(false);

            tocCenterZebra = cloneFill(wb, tocCenter, null, zebra, border);
            tocCenterZebra.setAlignment(HorizontalAlignment.CENTER);
            tocCenterZebra.setWrapText(false);

            tocValueZebra = cloneFill(wb, kvValue, null, zebra, border);
            tocValueZebra.setWrapText(true);

            Font idFont = wb.createFont();
            idFont.setBold(true);
            idFont.setFontHeightInPoints((short) 10);
            idFont.setColor(IndexedColors.BLUE.getIndex());
            idFont.setUnderline(Font.U_SINGLE);

            tocId = wb.createCellStyle();
            tocId.cloneStyleFrom(kvValue);
            tocId.setFont(idFont);
            tocId.setAlignment(HorizontalAlignment.LEFT);
            tocId.setVerticalAlignment(VerticalAlignment.CENTER);
            tocId.setWrapText(false);
            thinBorder(tocId, border);

            tocIdZebra = cloneFill(wb, tocId, idFont, zebra, border);
            tocIdZebra.setAlignment(HorizontalAlignment.LEFT);
            tocIdZebra.setWrapText(false);

            XSSFColor pairLblBg = rgb(new byte[]{(byte) 0xF1, (byte) 0xF5, (byte) 0xF9});
            pairLabel = wb.createCellStyle();
            pairLabel.setFont(bold);
            pairLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            pairLabel.setVerticalAlignment(VerticalAlignment.CENTER);
            pairLabel.setWrapText(true);
            ((XSSFCellStyle) pairLabel).setFillForegroundColor(pairLblBg);
            thinBorder(pairLabel, border);

            pairValue = wb.createCellStyle();
            pairValue.setWrapText(true);
            pairValue.setVerticalAlignment(VerticalAlignment.CENTER);
            pairValue.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            ((XSSFCellStyle) pairValue).setFillForegroundColor(bgLight);
            thinBorder(pairValue, border);

            hint = wb.createCellStyle();
            hint.setFont(italic);
            hint.setWrapText(true);
            hint.setVerticalAlignment(VerticalAlignment.CENTER);
            thinBorder(hint, border);

            masterWrap = wb.createCellStyle();
            masterWrap.setFont(bold);
            masterWrap.setWrapText(true);
            masterWrap.setVerticalAlignment(VerticalAlignment.CENTER);
            masterWrap.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            ((XSSFCellStyle) masterWrap).setFillForegroundColor(master);
            thinBorder(masterWrap, border);

            masterCenter = wb.createCellStyle();
            masterCenter.cloneStyleFrom(masterWrap);
            masterCenter.setAlignment(HorizontalAlignment.CENTER);

            detailHeader = wb.createCellStyle();
            detailHeader.setFont(bold);
            detailHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            detailHeader.setAlignment(HorizontalAlignment.CENTER);
            detailHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            detailHeader.setWrapText(true);
            ((XSSFCellStyle) detailHeader).setFillForegroundColor(detailHdr);
            thinBorder(detailHeader, border);

            detailWrap = wb.createCellStyle();
            detailWrap.setWrapText(true);
            detailWrap.setVerticalAlignment(VerticalAlignment.TOP);
            detailWrap.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            ((XSSFCellStyle) detailWrap).setFillForegroundColor(bgLight);
            thinBorder(detailWrap, border);

            detailValue = wb.createCellStyle();
            detailValue.cloneStyleFrom(detailWrap);
            detailValue.setFont(bold);

            detailCenter = wb.createCellStyle();
            detailCenter.cloneStyleFrom(detailWrap);
            detailCenter.setAlignment(HorizontalAlignment.CENTER);

            pass = statusCell(wb, bold, passBg, border);
            fail = statusCell(wb, bold, failBg, border);
            warn = statusCell(wb, bold, warnBg, border);

            tagDien = statusCell(wb, bold, rgb(new byte[]{(byte) 0xBF, (byte) 0xDB, (byte) 0xFE}), border);
            tagChon = statusCell(wb, bold, rgb(new byte[]{(byte) 0xA5, (byte) 0xF3, (byte) 0xFC}), border);
            tagUpload = statusCell(wb, bold, rgb(new byte[]{(byte) 0xE9, (byte) 0xD5, (byte) 0xFF}), border);
            tagData = statusCell(wb, bold, rgb(new byte[]{(byte) 0xFE, (byte) 0xF3, (byte) 0xC7}), border);

            durationNum = wb.createCellStyle();
            durationNum.cloneStyleFrom(masterCenter);
            durationNum.setDataFormat(wb.createDataFormat().getFormat("0.0\" giây\""));

            Font linkFont = wb.createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            linkFont.setBold(true);
            linkFont.setFontHeightInPoints((short) 10);
            hyperlink = wb.createCellStyle();
            hyperlink.cloneStyleFrom(masterCenter);
            hyperlink.setFont(linkFont);
        }

        private static XSSFColor rgb(byte[] rgb) {
            return new XSSFColor(rgb, null);
        }

        private static CellStyle cloneFill(Workbook wb, CellStyle base, Font font,
                                           XSSFColor fill, XSSFColor border) {
            CellStyle s = wb.createCellStyle();
            s.cloneStyleFrom(base);
            if (font != null) {
                s.setFont(font);
            }
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            ((XSSFCellStyle) s).setFillForegroundColor(fill);
            thinBorder(s, border);
            return s;
        }

        private static CellStyle statusCell(Workbook wb, Font bold, XSSFColor fill, XSSFColor border) {
            CellStyle s = wb.createCellStyle();
            s.setFont(bold);
            s.setAlignment(HorizontalAlignment.CENTER);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setWrapText(true);
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            ((XSSFCellStyle) s).setFillForegroundColor(fill);
            thinBorder(s, border);
            return s;
        }

        private static void thinBorder(CellStyle style, XSSFColor color) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            if (style instanceof XSSFCellStyle xs) {
                xs.setTopBorderColor(color);
                xs.setBottomBorderColor(color);
                xs.setLeftBorderColor(color);
                xs.setRightBorderColor(color);
            }
        }

        private static void dashedBorder(CellStyle style, XSSFColor color) {
            style.setBorderTop(BorderStyle.DASHED);
            style.setBorderBottom(BorderStyle.DASHED);
            style.setBorderLeft(BorderStyle.DASHED);
            style.setBorderRight(BorderStyle.DASHED);
            if (style instanceof XSSFCellStyle xs) {
                xs.setTopBorderColor(color);
                xs.setBottomBorderColor(color);
                xs.setLeftBorderColor(color);
                xs.setRightBorderColor(color);
            }
        }
    }
}

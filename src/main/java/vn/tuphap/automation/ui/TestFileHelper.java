package vn.tuphap.automation.ui;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

public final class TestFileHelper {

    private static final String PDF = "sample.pdf";
    private static final String XLSX = "sample.xlsx";
    private static final String DOCX = "sample.docx";

    private static volatile String cachedXlsx;
    private static volatile String cachedDocx;

    private TestFileHelper() {
    }

    public static String getSamplePdf() {
        return resolveResource("testdata/" + PDF);
    }

    /** Excel mẫu — ưu tiên classpath, không có thì tạo tạm bằng POI. */
    public static String getSampleXlsx() {
        if (cachedXlsx == null) {
            synchronized (TestFileHelper.class) {
                if (cachedXlsx == null) {
                    cachedXlsx = resolveOrCreateSample(XLSX, TestFileHelper::createSampleXlsx);
                }
            }
        }
        return cachedXlsx;
    }

    /** Word mẫu — ưu tiên classpath, không có thì tạo tạm bằng POI. */
    public static String getSampleDocx() {
        if (cachedDocx == null) {
            synchronized (TestFileHelper.class) {
                if (cachedDocx == null) {
                    cachedDocx = resolveOrCreateSample(DOCX, TestFileHelper::createSampleDocx);
                }
            }
        }
        return cachedDocx;
    }

    /** Chọn ngẫu nhiên một trong PDF / Excel / Word cho mỗi lần upload. */
    public static String pickRandomUploadFile() {
        int pick = ThreadLocalRandom.current().nextInt(3);
        return switch (pick) {
            case 0 -> getSamplePdf();
            case 1 -> getSampleXlsx();
            default -> getSampleDocx();
        };
    }

    /** Tên hiển thị trong log / báo cáo. */
    public static String displayName(String absolutePath) {
        if (absolutePath == null || absolutePath.isBlank()) {
            return "";
        }
        String lower = Paths.get(absolutePath).getFileName().toString().toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "tệp mẫu.pdf";
        }
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return "tệp mẫu.xlsx";
        }
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) {
            return "tệp mẫu.docx";
        }
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "tệp mẫu.png";
        }
        return Paths.get(absolutePath).getFileName().toString();
    }

    public static String getSamplePng() {
        return resolveResource("testdata/sample.png");
    }

    private static String resolveOrCreateSample(String fileName, SampleCreator creator) {
        URL url = TestFileHelper.class.getClassLoader().getResource("testdata/" + fileName);
        if (url != null) {
            try {
                return Paths.get(url.toURI()).toAbsolutePath().toString();
            } catch (Exception e) {
                throw new RuntimeException("❌ Không đọc được đường dẫn file test: testdata/" + fileName, e);
            }
        }
        Path target = Paths.get(System.getProperty("java.io.tmpdir"), "toaan-testdata", fileName);
        if (!Files.exists(target)) {
            try {
                Files.createDirectories(target.getParent());
                creator.create(target);
            } catch (IOException e) {
                throw new RuntimeException("❌ Không tạo được file test: " + fileName, e);
            }
        }
        return target.toAbsolutePath().toString();
    }

    private static void createSampleXlsx(Path target) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream out = new FileOutputStream(target.toFile())) {
            var sheet = workbook.createSheet("Mau");
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue("Tai lieu mau automation");
            workbook.write(out);
        }
    }

    private static void createSampleDocx(Path target) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(target.toFile())) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("Tai lieu mau automation");
            document.write(out);
        }
    }

    private static String resolveResource(String resourcePath) {
        URL url = TestFileHelper.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new RuntimeException("❌ Không tìm thấy file test trong classpath: " + resourcePath);
        }
        try {
            return Paths.get(url.toURI()).toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("❌ Không đọc được đường dẫn file test: " + resourcePath, e);
        }
    }

    public static void assertExists(String absolutePath) {
        if (absolutePath == null || !new File(absolutePath).exists()) {
            throw new RuntimeException("❌ File test không tồn tại: " + absolutePath);
        }
    }

    @FunctionalInterface
    private interface SampleCreator {
        void create(Path target) throws IOException;
    }
}

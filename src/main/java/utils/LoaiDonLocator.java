package utils;

import org.openqa.selenium.By;

/**
 * Locator thẻ loại đơn — UI có thể hiển thị tiêu đề ngắn hoặc card mô tả dài (vd: "DS Dân sự Hợp đồng, đất đai...").
 */
public final class LoaiDonLocator {

    private static final int MAX_LOAI_DON_TITLE_LENGTH = 45;

    private LoaiDonLocator() {
    }

    public static By card(String loaiDon) {
        String name = loaiDon == null ? "" : loaiDon.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("loaiDon rỗng");
        }
        if (isKinhDoanhThuongMai(name)) {
            return By.xpath("//div[contains(@class, 'cursor-pointer')]"
                    + "[contains(normalize-space(.), 'Kinh doanh') and contains(normalize-space(.), 'thương mại')]");
        }
        if ("Dân sự".equals(name)) {
            return By.xpath("//div[contains(@class, 'cursor-pointer')]"
                    + "[contains(normalize-space(.), 'Dân sự') or starts-with(normalize-space(.), 'DS ')]");
        }
        return By.xpath("//div[contains(@class, 'cursor-pointer')][contains(normalize-space(.), "
                + xpathLiteral(name) + ")]");
    }

    /**
     * Chuẩn hóa text trên card thành tên loại đơn trong catalog.
     * Bỏ qua card mô tả dài / phụ đề (có "..." hoặc liệt kê nhiều loại việc).
     */
    public static String canonicalName(String cardText) {
        if (cardText == null || cardText.isBlank()) {
            return null;
        }
        String normalized = cardText.trim().replaceAll("\\s+", " ");

        String known = matchKnownLoaiDon(normalized);
        if (known != null) {
            return known;
        }

        if (normalized.length() <= MAX_LOAI_DON_TITLE_LENGTH
                && !normalized.contains("...")
                && !normalized.contains("…")) {
            return normalized;
        }
        return null;
    }

    private static String matchKnownLoaiDon(String text) {
        if (text.contains("Kinh doanh") && text.contains("thương mại")) {
            return "Kinh doanh, thương mại";
        }
        if (text.contains("Hôn nhân") && text.contains("gia đình")) {
            return "Hôn nhân và gia đình";
        }
        if (text.contains("Sở hữu trí tuệ")) {
            return "Sở hữu trí tuệ";
        }
        if (text.contains("Phá sản")) {
            return "Phá sản";
        }
        if (text.contains("Hành chính")) {
            return "Hành chính";
        }
        if (text.contains("Lao động")) {
            return "Lao động";
        }
        if (text.contains("Dân sự") || text.startsWith("DS ")) {
            return "Dân sự";
        }
        return null;
    }

    private static boolean isKinhDoanhThuongMai(String name) {
        String lower = name.toLowerCase();
        return lower.contains("kinh doanh") && lower.contains("thương mại");
    }

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        String[] parts = value.split("'");
        StringBuilder concat = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                concat.append(", \"'\", ");
            }
            concat.append("'").append(parts[i]).append("'");
        }
        concat.append(")");
        return concat.toString();
    }
}

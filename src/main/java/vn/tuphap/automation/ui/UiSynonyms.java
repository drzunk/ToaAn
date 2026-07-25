package vn.tuphap.automation.ui;

import org.openqa.selenium.By;

/**
 * Mẫu chuẩn hóa locator theo <b>khóa nghiệp vụ + synonym</b> (cùng hướng {@link LoaiDonLocator}).
 * <p>
 * Mục tiêu: Page Object không hard-code một chuỗi UI duy nhất.
 * Khi sản phẩm đổi nhãn ("Họ và tên" → "Họ tên"), chỉ cập nhật synonym / catalog.
 * <p>
 * Hướng dài hạn (ưu tiên giảm dần độ mong manh):
 * <ol>
 *   <li>{@code data-testid} / {@code aria-label} ổn định từ phía FE (tốt nhất)</li>
 *   <li>Khóa nghiệp vụ trong catalog + danh sách synonym UI</li>
 *   <li>XPath text tạm thời với nhiều nhánh {@code contains} (fallback)</li>
 * </ol>
 */
public final class UiSynonyms {

    private UiSynonyms() {
    }

    /** Label ô họ tên — các biến thể từng thấy trên form. */
    public static final String[] HO_TEN = {
            "Họ và tên", "Họ tên", "Họ & tên"
    };

    public static final String[] TEN_TO_CHUC = {
            "Tên tổ chức", "Tên tổ chức / doanh nghiệp", "Tên doanh nghiệp"
    };

    public static final String[] TU_CACH_NOP_DON = {
            "Tư cách người nộp đơn", "Tư cách nộp đơn"
    };

    public static final String[] THEM_BI_DON = {
            "Thêm bị đơn", "Thêm người bị yêu cầu", "Thêm người bị kiện", "Thêm người được yêu cầu"
    };

    /**
     * Prefix badge đánh số slot bước 3 (kèm số: "Bị đơn 1", "Người bị kiện 2", …).
     * Không gồm "Người yêu cầu 2" (UI vợ/chồng Hôn nhân — xử lý riêng).
     */
    public static final String[] SLOT_BADGE_PREFIXES = {
            "Bị đơn ",
            "Người bị kiện ",
            "Người được yêu cầu ",
            "Người bị yêu cầu "
    };

    /** XPath {@code contains(., …)} nối bằng {@code or} từ danh sách synonym. */
    public static String containsAnyDot(String[] synonyms) {
        if (synonyms == null || synonyms.length == 0) {
            throw new IllegalArgumentException("synonyms rỗng");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < synonyms.length; i++) {
            if (i > 0) {
                sb.append(" or ");
            }
            sb.append("contains(., ").append(xpathLiteral(synonyms[i])).append(")");
        }
        return sb.toString();
    }

    /** Ví dụ: label họ tên hoặc tên tổ chức (bước nguyên đơn). */
    public static By labelHoTenHoacToChuc() {
        return By.xpath("//label[" + containsAnyDot(HO_TEN)
                + " or " + containsAnyDot(TEN_TO_CHUC) + "]");
    }

    public static By buttonThemBiDonVariants() {
        return By.xpath("//button[" + containsAnyDot(THEM_BI_DON) + "]");
    }

    public static String xpathLiteral(String value) {
        if (value == null) {
            return "''";
        }
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

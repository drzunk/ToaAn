package utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Danh sách tòa án cố định cho automation — chỉ cấp tỉnh/thành phố, không khu vực/huyện.
 */
public final class ToaAnCatalog {

    private static final int MAX_AUTOMATION_COURTS = 10;

    /** Tên khớp UI demo; thứ tự ưu tiên khi lọc từ scrape. */
    private static final String[] PREFERRED = {
            "Tòa án nhân dân tỉnh Sơn La",
            "Tòa án nhân dân thành phố Hà Nội",
            "Tòa án nhân dân Thành phố Hồ Chí Minh",
            "Tòa án nhân dân thành phố Đà Nẵng",
            "Tòa án nhân dân thành phố Cần Thơ",
            "Tòa án nhân dân thành phố Hải Phòng",
            "Tòa án nhân dân thành phố Huế",
            "Tòa án nhân dân tỉnh Bắc Ninh"
    };

    private ToaAnCatalog() {
    }

    public static String[] getAutomationDefaults() {
        return PREFERRED.clone();
    }

    public static List<String> filterForAutomation(List<String> scraped) {
        Set<String> scrapedSet = scraped == null ? Set.of() : new LinkedHashSet<>(scraped);
        List<String> result = new ArrayList<>();

        for (String preferred : PREFERRED) {
            if (scrapedSet.isEmpty() || scrapedSet.contains(preferred)) {
                result.add(preferred);
            }
            if (result.size() >= MAX_AUTOMATION_COURTS) {
                return result;
            }
        }

        if (result.isEmpty()) {
            for (String name : scrapedSet) {
                if (isProvinceOrCityLevel(name) && !result.contains(name)) {
                    result.add(name);
                    if (result.size() >= MAX_AUTOMATION_COURTS) {
                        break;
                    }
                }
            }
        }

        if (result.isEmpty()) {
            return List.of(PREFERRED);
        }
        return result;
    }

    public static boolean isProvinceOrCityLevel(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String lower = name.toLowerCase();
        if (lower.contains("khu vực") || lower.contains("huyện") || lower.startsWith("tòa án nhân dân tp.")) {
            return false;
        }
        return lower.startsWith("tòa án nhân dân tỉnh ")
                || lower.startsWith("tòa án nhân dân thành phố ");
    }
}

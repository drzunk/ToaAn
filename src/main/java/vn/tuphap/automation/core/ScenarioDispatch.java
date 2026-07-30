package vn.tuphap.automation.core;

import vn.tuphap.automation.data.TaoDonScenario;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Đảm bảo mỗi kịch bản chỉ được nhận đúng 1 lần khi chạy parallel nhiều browser.
 * TestNG DataProvider đã chia hàng — lớp này chặn trùng nếu vô tình invoke lại.
 */
public final class ScenarioDispatch {

    private static final Set<String> CLAIMED = ConcurrentHashMap.newKeySet();

    private ScenarioDispatch() {
    }

    public static void reset() {
        CLAIMED.clear();
    }

    public static String keyOf(TaoDonScenario s) {
        if (s == null) {
            return "null";
        }
        return String.join("|",
                nullToEmpty(s.stt()),
                nullToEmpty(s.loaiDon()),
                nullToEmpty(s.loaiViec()),
                nullToEmpty(s.tuCachNopDon()),
                nullToEmpty(s.loaiChuThe()),
                String.valueOf(s.soLuongBiDon()),
                nullToEmpty(s.coDongNguyenDon()),
                nullToEmpty(s.tomTat()));
    }

    /**
     * @return true nếu claim thành công (lần đầu); false nếu case đã chạy trên browser/thread khác.
     */
    public static boolean claim(TaoDonScenario s) {
        String key = keyOf(s);
        boolean first = CLAIMED.add(key);
        if (first) {
            System.out.println("🎯 Case → " + BrowserLayout.browserLabel()
                    + " | " + shortLabel(s));
        } else {
            System.out.println("⚠ Bỏ trùng case trên " + BrowserLayout.browserLabel()
                    + ": " + shortLabel(s));
        }
        return first;
    }

    public static int claimedCount() {
        return CLAIMED.size();
    }

    private static String shortLabel(TaoDonScenario s) {
        if (s == null) {
            return "(null)";
        }
        return "#" + nullToEmpty(s.stt()) + " " + nullToEmpty(s.loaiDon())
                + " / " + nullToEmpty(s.loaiViec());
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v.trim();
    }
}

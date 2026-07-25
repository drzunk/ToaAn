package vn.tuphap.automation.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Ma trận mid: ~35 kịch bản loại đơn thường (1 nhánh / cặp + pad) + đủ 4 tư cách Phá sản.
 * Lấy từ {@link FullCoverageMatrix} đã hợp lệ — không cartesian lại.
 */
public final class MidCoverageMatrix {

    /** Số kịch bản không phải Phá sản (catalog hiện 34 cặp → pad thêm 1 nhánh). */
    public static final int TARGET_REGULAR = 35;

    private MidCoverageMatrix() {}

    public static List<FullCoverageMatrix.BranchSpec> build() {
        List<FullCoverageMatrix.BranchSpec> full = FullCoverageMatrix.build();
        List<FullCoverageMatrix.BranchSpec> mid = new ArrayList<>();
        Set<String> seenPairs = new LinkedHashSet<>();
        Set<String> seenBranchSigs = new LinkedHashSet<>();

        // 1) Mỗi cặp loại việc thường → 1 kịch bản (profile đầu tiên trong full)
        for (FullCoverageMatrix.BranchSpec s : full) {
            if (DataDictionary.isPhaSan(s.loaiDon())) {
                continue;
            }
            String pair = FullCoverageMatrix.pairKey(s);
            if (seenPairs.add(pair)) {
                mid.add(withSeed(s, mid.size()));
                seenBranchSigs.add(branchSig(s));
            }
        }

        // 2) Pad tới TARGET_REGULAR bằng nhánh thứ hai của cặp đã có (đa dạng CN/TC, BD, NLQ…)
        if (mid.size() < TARGET_REGULAR) {
            for (FullCoverageMatrix.BranchSpec s : full) {
                if (DataDictionary.isPhaSan(s.loaiDon())) {
                    continue;
                }
                if (!seenPairs.contains(FullCoverageMatrix.pairKey(s))) {
                    continue;
                }
                String sig = branchSig(s);
                if (!seenBranchSigs.add(sig)) {
                    continue;
                }
                mid.add(withSeed(s, mid.size()));
                if (mid.size() >= TARGET_REGULAR) {
                    break;
                }
            }
        }

        // 3) Đủ 4 tư cách Phá sản
        Set<String> seenTuCach = new LinkedHashSet<>();
        for (FullCoverageMatrix.BranchSpec s : full) {
            if (!DataDictionary.isPhaSan(s.loaiDon())) {
                continue;
            }
            String tu = s.tuCachNopDon() == null ? "" : s.tuCachNopDon().trim();
            if (tu.isEmpty() || !seenTuCach.add(tu)) {
                continue;
            }
            mid.add(withSeed(s, mid.size()));
        }

        return mid;
    }

    public static int expectedRowCount() {
        return build().size();
    }

    public static List<String> validateCoverage(List<FullCoverageMatrix.BranchSpec> rows) {
        List<String> errors = new ArrayList<>();
        Set<String> nonPsPairs = new LinkedHashSet<>();
        Set<String> tuCach = new LinkedHashSet<>();
        int regular = 0;
        int phaSan = 0;

        for (FullCoverageMatrix.BranchSpec r : rows) {
            if (DataDictionary.isPhaSan(r.loaiDon())) {
                phaSan++;
                if (r.tuCachNopDon() != null && !r.tuCachNopDon().isBlank()) {
                    tuCach.add(r.tuCachNopDon());
                }
            } else {
                regular++;
                nonPsPairs.add(FullCoverageMatrix.pairKey(r));
            }
        }

        int catalogNonPs = 0;
        for (String[] p : MasterDataCatalog.getAllLoaiDonViecPairs()) {
            if (!DataDictionary.isPhaSan(p[0])) {
                catalogNonPs++;
            }
        }
        if (nonPsPairs.size() < catalogNonPs) {
            errors.add("Thiếu cặp thường: có " + nonPsPairs.size() + "/" + catalogNonPs);
        }
        if (regular < TARGET_REGULAR) {
            errors.add("Số kịch bản thường < " + TARGET_REGULAR + " (actual=" + regular + ")");
        }
        for (String t : MasterDataCatalog.getTuCachNopDonPhaSan()) {
            if (!tuCach.contains(t)) {
                errors.add("Thiếu tư cách PS: " + t);
            }
        }
        if (phaSan < MasterDataCatalog.getTuCachNopDonPhaSan().length) {
            errors.add("Thiếu hàng Phá sản: " + phaSan);
        }
        return errors;
    }

    public static String summarize(List<FullCoverageMatrix.BranchSpec> rows) {
        int regular = 0;
        int phaSan = 0;
        for (FullCoverageMatrix.BranchSpec r : rows) {
            if (DataDictionary.isPhaSan(r.loaiDon())) {
                phaSan++;
            } else {
                regular++;
            }
        }
        return String.format(Locale.ROOT,
                "Mid: %d kịch bản (%d thường + %d tư cách Phá sản)",
                rows.size(), regular, phaSan);
    }

    private static String branchSig(FullCoverageMatrix.BranchSpec s) {
        return s.nguyenDonToChuc() + "|" + s.coNguoiDaiDien() + "|" + s.biDonToChuc()
                + "|" + s.soLuongBiDon() + "|" + s.coNguoiLienQuan() + "|" + s.coTaiLieuBoSung();
    }

    private static FullCoverageMatrix.BranchSpec withSeed(FullCoverageMatrix.BranchSpec s, int seed) {
        return new FullCoverageMatrix.BranchSpec(
                s.loaiDon(), s.loaiViec(), s.tuCachNopDon(),
                s.nguyenDonToChuc(), s.coNguoiDaiDien(), s.biDonToChuc(),
                s.soLuongBiDon(), s.coNguoiLienQuan(), s.coTaiLieuBoSung(),
                seed);
    }
}

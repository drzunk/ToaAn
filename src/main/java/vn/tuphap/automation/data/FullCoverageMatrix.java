package vn.tuphap.automation.data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Sinh ma trận kịch bản Full mức B (pairwise / đủ nhánh luồng):
 * mọi cặp loại đơn–việc, 4 tư cách Phá sản, xoay đủ CN/TC, đại diện, số BD, NLQ, TLBS.
 * Không cartesian hóa catalog phụ (tòa, giới tính…).
 */
public final class FullCoverageMatrix {

    /** Số profile nhánh xoay trên mỗi cặp loại việc (không phải Phá sản). */
    private static final int PROFILES_PER_PAIR = 3;

    private FullCoverageMatrix() {}

    /**
     * Một hàng cấu trúc trước khi {@link DataGenerator} gắn dữ liệu Faker / catalog phụ.
     */
    public record BranchSpec(
            String loaiDon,
            String loaiViec,
            String tuCachNopDon,
            boolean nguyenDonToChuc,
            boolean coNguoiDaiDien,
            boolean biDonToChuc,
            int soLuongBiDon,
            boolean coNguoiLienQuan,
            boolean coTaiLieuBoSung,
            int seedIndex
    ) {}

    public static List<BranchSpec> build() {
        List<BranchSpec> rows = new ArrayList<>();
        List<String[]> pairs = MasterDataCatalog.getAllLoaiDonViecPairs();
        int seed = 0;

        for (String[] pair : pairs) {
            String loaiDon = pair[0];
            String loaiViec = pair[1];
            if (DataDictionary.isPhaSan(loaiDon)) {
                String[] tuCachs = MasterDataCatalog.getTuCachNopDonPhaSan();
                for (int t = 0; t < tuCachs.length; t++) {
                    rows.add(applyConstraints(loaiDon, loaiViec, tuCachs[t], profile(seed), seed));
                    seed++;
                }
            } else {
                for (int p = 0; p < PROFILES_PER_PAIR; p++) {
                    rows.add(applyConstraints(loaiDon, loaiViec, null, profile(seed), seed));
                    seed++;
                }
            }
        }

        fillGaps(rows, seed);
        return rows;
    }

    public static int expectedRowCount() {
        return build().size();
    }

    /** Profile nhánh theo index — 6 mẫu xoay đủ trục chính. */
    private static boolean[] profile(int index) {
        // [ndOrg, daiDien, bdOrg, soLuong2, nlq, tlbs]
        return switch (Math.floorMod(index, 6)) {
            case 0 -> new boolean[]{false, true, false, false, true, false};
            case 1 -> new boolean[]{true, false, true, true, false, true};
            case 2 -> new boolean[]{false, false, true, true, true, true};
            case 3 -> new boolean[]{true, false, false, false, false, false};
            case 4 -> new boolean[]{false, true, true, false, false, true};
            default -> new boolean[]{true, false, false, true, true, false};
        };
    }

    private static BranchSpec applyConstraints(
            String loaiDon, String loaiViec, String tuCach, boolean[] p, int seed) {
        boolean ndOrg = p[0];
        boolean daiDien = !ndOrg && p[1];
        boolean bdOrg = p[2];
        boolean so2 = p[3];
        boolean nlq = p[4];
        boolean tlbs = p[5];

        if (DataDictionary.isPhaSan(loaiDon)) {
            bdOrg = true;
            so2 = false;
        }
        if (DataDictionary.isThuanTinhLyHon(loaiViec)) {
            so2 = false;
        }
        if (!DataDictionary.allowsThemBiDon(loaiDon, loaiViec)) {
            so2 = false;
        }
        // Hành chính: form cơ quan — flag BD CN/TC không dùng; giữ giá trị để xoay seed
        if (ndOrg) {
            daiDien = false;
        }

        return new BranchSpec(
                loaiDon,
                loaiViec,
                tuCach == null ? "" : tuCach,
                ndOrg,
                daiDien,
                bdOrg,
                so2 ? 2 : 1,
                nlq,
                tlbs,
                seed);
    }

    private static void fillGaps(List<BranchSpec> rows, int nextSeed) {
        int seed = nextSeed;

        if (!hasNd(rows, false)) {
            rows.add(flipOnEligible(rows, seed++, spec -> withNd(spec, false)));
        }
        if (!hasNd(rows, true)) {
            rows.add(flipOnEligible(rows, seed++, spec -> withNd(spec, true)));
        }
        if (!hasDaiDienCo(rows)) {
            rows.add(flipOnEligible(rows, seed++, FullCoverageMatrix::withDaiDienCo));
        }
        if (!hasBd(rows, false)) {
            rows.add(flipOnEligible(rows, seed++, spec -> withBd(spec, false)));
        }
        if (!hasBd(rows, true)) {
            rows.add(flipOnEligible(rows, seed++, spec -> withBd(spec, true)));
        }
        if (!hasSoLuong2(rows, Family.STANDARD)) {
            rows.add(flipOnEligible(rows, seed++, Family.STANDARD, FullCoverageMatrix::withSoLuong2));
        }
        if (!hasSoLuong2(rows, Family.HON_NHAN)) {
            rows.add(flipOnEligible(rows, seed++, Family.HON_NHAN, FullCoverageMatrix::withSoLuong2));
        }
        if (!hasSoLuong2(rows, Family.HANH_CHINH)) {
            rows.add(flipOnEligible(rows, seed++, Family.HANH_CHINH, FullCoverageMatrix::withSoLuong2));
        }
        if (!hasNlq(rows, true)) {
            rows.add(flipOnEligible(rows, seed++, spec -> withNlq(spec, true)));
        }
        if (!hasNlq(rows, false)) {
            rows.add(flipOnEligible(rows, seed++, spec -> withNlq(spec, false)));
        }
        if (!hasTlbs(rows, true)) {
            rows.add(flipOnEligible(rows, seed++, spec -> withTlbs(spec, true)));
        }
        if (!hasTlbs(rows, false)) {
            rows.add(flipOnEligible(rows, seed++, spec -> withTlbs(spec, false)));
        }

        // Đảm bảo đủ 4 tư cách Phá sản (phòng catalog lệch)
        for (String tuCach : MasterDataCatalog.getTuCachNopDonPhaSan()) {
            if (!hasTuCach(rows, tuCach)) {
                String[] pair = firstPhaSanPair();
                boolean[] p = profile(seed);
                rows.add(applyConstraints(pair[0], pair[1], tuCach, p, seed++));
            }
        }
    }

    private enum Family { STANDARD, HON_NHAN, HANH_CHINH, ANY }

    @FunctionalInterface
    private interface SpecTransform {
        BranchSpec apply(BranchSpec base);
    }

    private static BranchSpec flipOnEligible(List<BranchSpec> rows, int seed, SpecTransform tx) {
        return flipOnEligible(rows, seed, Family.ANY, tx);
    }

    private static BranchSpec flipOnEligible(
            List<BranchSpec> rows, int seed, Family family, SpecTransform tx) {
        for (int i = rows.size() - 1; i >= 0; i--) {
            BranchSpec base = rows.get(i);
            if (!matchesFamily(base, family)) {
                continue;
            }
            BranchSpec next = tx.apply(base);
            if (next != null) {
                return new BranchSpec(
                        next.loaiDon(), next.loaiViec(), next.tuCachNopDon(),
                        next.nguyenDonToChuc(), next.coNguoiDaiDien(), next.biDonToChuc(),
                        next.soLuongBiDon(), next.coNguoiLienQuan(), next.coTaiLieuBoSung(),
                        seed);
            }
        }
        // Fallback: lấy cặp phù hợp từ catalog
        String[] pair = firstPairForFamily(family);
        boolean[] p = profile(seed);
        BranchSpec created = applyConstraints(pair[0], pair[1],
                DataDictionary.isPhaSan(pair[0]) ? MasterDataCatalog.getTuCachNopDonPhaSan()[0] : null,
                p, seed);
        BranchSpec transformed = tx.apply(created);
        return transformed != null ? transformed : created;
    }

    private static boolean matchesFamily(BranchSpec s, Family family) {
        return switch (family) {
            case ANY -> true;
            case STANDARD -> DataDictionary.isStandardBiDonUi(s.loaiDon());
            case HON_NHAN -> DataDictionary.isHonNhanGiaDinh(s.loaiDon())
                    && !DataDictionary.isThuanTinhLyHon(s.loaiViec());
            case HANH_CHINH -> DataDictionary.isHanhChinh(s.loaiDon());
        };
    }

    private static String[] firstPairForFamily(Family family) {
        for (String[] pair : MasterDataCatalog.getAllLoaiDonViecPairs()) {
            BranchSpec probe = new BranchSpec(pair[0], pair[1], "", false, false, false, 1, false, false, 0);
            if (matchesFamily(probe, family)) {
                return pair;
            }
        }
        List<String[]> all = MasterDataCatalog.getAllLoaiDonViecPairs();
        return all.get(0);
    }

    private static String[] firstPhaSanPair() {
        for (String[] pair : MasterDataCatalog.getAllLoaiDonViecPairs()) {
            if (DataDictionary.isPhaSan(pair[0])) {
                return pair;
            }
        }
        return new String[]{"Phá sản", DataDictionary.PHA_SAN_LOAI_VIEC_MAC_DINH};
    }

    private static BranchSpec withNd(BranchSpec s, boolean org) {
        boolean daiDien = !org && s.coNguoiDaiDien();
        return new BranchSpec(s.loaiDon(), s.loaiViec(), s.tuCachNopDon(),
                org, daiDien, s.biDonToChuc(), s.soLuongBiDon(),
                s.coNguoiLienQuan(), s.coTaiLieuBoSung(), s.seedIndex());
    }

    private static BranchSpec withDaiDienCo(BranchSpec s) {
        // Luôn ép nguyên đơn Cá nhân + đại diện Có
        return new BranchSpec(s.loaiDon(), s.loaiViec(), s.tuCachNopDon(),
                false, true, s.biDonToChuc(), s.soLuongBiDon(),
                s.coNguoiLienQuan(), s.coTaiLieuBoSung(), s.seedIndex());
    }

    private static BranchSpec withBd(BranchSpec s, boolean org) {
        if (DataDictionary.isPhaSan(s.loaiDon())) {
            return org ? s : null; // PS chỉ Tổ chức
        }
        if (DataDictionary.isHanhChinh(s.loaiDon())) {
            return null; // không có CN/TC BD
        }
        return new BranchSpec(s.loaiDon(), s.loaiViec(), s.tuCachNopDon(),
                s.nguyenDonToChuc(), s.coNguoiDaiDien(), org, s.soLuongBiDon(),
                s.coNguoiLienQuan(), s.coTaiLieuBoSung(), s.seedIndex());
    }

    private static BranchSpec withSoLuong2(BranchSpec s) {
        if (!DataDictionary.allowsThemBiDon(s.loaiDon(), s.loaiViec())) {
            return null;
        }
        return new BranchSpec(s.loaiDon(), s.loaiViec(), s.tuCachNopDon(),
                s.nguyenDonToChuc(), s.coNguoiDaiDien(), s.biDonToChuc(), 2,
                s.coNguoiLienQuan(), s.coTaiLieuBoSung(), s.seedIndex());
    }

    private static BranchSpec withNlq(BranchSpec s, boolean co) {
        return new BranchSpec(s.loaiDon(), s.loaiViec(), s.tuCachNopDon(),
                s.nguyenDonToChuc(), s.coNguoiDaiDien(), s.biDonToChuc(), s.soLuongBiDon(),
                co, s.coTaiLieuBoSung(), s.seedIndex());
    }

    private static BranchSpec withTlbs(BranchSpec s, boolean co) {
        return new BranchSpec(s.loaiDon(), s.loaiViec(), s.tuCachNopDon(),
                s.nguyenDonToChuc(), s.coNguoiDaiDien(), s.biDonToChuc(), s.soLuongBiDon(),
                s.coNguoiLienQuan(), co, s.seedIndex());
    }

    private static boolean hasNd(List<BranchSpec> rows, boolean org) {
        for (BranchSpec r : rows) {
            if (r.nguyenDonToChuc() == org) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDaiDienCo(List<BranchSpec> rows) {
        for (BranchSpec r : rows) {
            if (!r.nguyenDonToChuc() && r.coNguoiDaiDien()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBd(List<BranchSpec> rows, boolean org) {
        for (BranchSpec r : rows) {
            if (DataDictionary.isPhaSan(r.loaiDon()) || DataDictionary.isHanhChinh(r.loaiDon())) {
                continue;
            }
            if (r.biDonToChuc() == org) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSoLuong2(List<BranchSpec> rows, Family family) {
        for (BranchSpec r : rows) {
            if (r.soLuongBiDon() >= 2 && matchesFamily(r, family)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNlq(List<BranchSpec> rows, boolean co) {
        for (BranchSpec r : rows) {
            if (r.coNguoiLienQuan() == co) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTlbs(List<BranchSpec> rows, boolean co) {
        for (BranchSpec r : rows) {
            if (r.coTaiLieuBoSung() == co) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTuCach(List<BranchSpec> rows, String tuCach) {
        for (BranchSpec r : rows) {
            if (DataDictionary.isPhaSan(r.loaiDon()) && tuCach.equals(r.tuCachNopDon())) {
                return true;
            }
        }
        return false;
    }

    /** Kiểm tra nhanh sau khi sinh — dùng cho test / log. */
    public static List<String> validateCoverage(List<BranchSpec> rows) {
        List<String> errors = new ArrayList<>();
        Set<String> pairs = new LinkedHashSet<>();
        for (String[] p : MasterDataCatalog.getAllLoaiDonViecPairs()) {
            pairs.add(p[0] + ">" + p[1]);
        }
        Set<String> seenPairs = new LinkedHashSet<>();
        Set<String> seenTuCach = new LinkedHashSet<>();
        for (BranchSpec r : rows) {
            seenPairs.add(r.loaiDon() + ">" + r.loaiViec());
            if (DataDictionary.isPhaSan(r.loaiDon()) && r.tuCachNopDon() != null && !r.tuCachNopDon().isBlank()) {
                seenTuCach.add(r.tuCachNopDon());
            }
        }
        for (String p : pairs) {
            if (!seenPairs.contains(p)) {
                errors.add("Thiếu cặp: " + p);
            }
        }
        for (String t : MasterDataCatalog.getTuCachNopDonPhaSan()) {
            if (!seenTuCach.contains(t)) {
                errors.add("Thiếu tư cách PS: " + t);
            }
        }
        if (!hasNd(rows, false)) {
            errors.add("Thiếu nguyên đơn Cá nhân");
        }
        if (!hasNd(rows, true)) {
            errors.add("Thiếu nguyên đơn Tổ chức");
        }
        if (!hasDaiDienCo(rows)) {
            errors.add("Thiếu đại diện Có");
        }
        if (!hasBd(rows, false)) {
            errors.add("Thiếu bị đơn Cá nhân");
        }
        if (!hasBd(rows, true)) {
            errors.add("Thiếu bị đơn Tổ chức");
        }
        if (!hasSoLuong2(rows, Family.STANDARD)) {
            errors.add("Thiếu 2 bị đơn (UI chuẩn)");
        }
        if (!hasSoLuong2(rows, Family.HON_NHAN)) {
            errors.add("Thiếu 2 người bị yêu cầu (Hôn nhân)");
        }
        if (!hasSoLuong2(rows, Family.HANH_CHINH)) {
            errors.add("Thiếu 2 cơ quan (Hành chính)");
        }
        if (!hasNlq(rows, true) || !hasNlq(rows, false)) {
            errors.add("Thiếu nhánh NLQ Có/Không");
        }
        if (!hasTlbs(rows, true) || !hasTlbs(rows, false)) {
            errors.add("Thiếu nhánh TLBS Có/Không");
        }
        return errors;
    }

    public static String pairKey(BranchSpec s) {
        return s.loaiDon() + ">" + s.loaiViec();
    }

    public static String summarize(List<BranchSpec> rows) {
        return String.format(Locale.ROOT,
                "Full pairwise B: %d kịch bản (%d cặp loại việc, %d tư cách PS)",
                rows.size(),
                MasterDataCatalog.getAllLoaiDonViecPairs().size(),
                MasterDataCatalog.getTuCachNopDonPhaSan().length);
    }
}

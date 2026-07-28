package vn.tuphap.automation.data;

import net.datafaker.Faker;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DataGenerator {

    private static final long FAKER_SEED = 20240724L;
    private static final int SMOKE_ROW_COUNT = 3;

    /** Địa chỉ VN đầy đủ: số nhà/đường + phường (chỉ để đọc) + tỉnh (hint dropdown).
     * Textarea "Chi tiết" chỉ nhận phần số nhà/đường ({@code WebUI.toAddressStreetDetail}). */
    private static String vietnameseAddress(Faker faker) {
        int soNha = faker.number().numberBetween(1, 999);
        String[] streets = {"Nguyễn Huệ", "Lê Lợi", "Trần Phú", "Hoàng Diệu", "Phan Đình Phùng", "Bà Triệu"};
        String[] wards = {"Phường Bồ Đề", "Phường Minh Khai", "Phường Cầu Giấy", "Phường 1", "Phường 5"};
        // Tên tỉnh/TP gần option dropdown UAT (hint chọn tỉnh từ đoạn cuối địa chỉ).
        String[] cities = {"Hà Nội", "Thành phố Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
                "Bắc Ninh", "Ninh Bình", "Sơn La", "Lâm Đồng", "Khánh Hòa"};
        String city = cities[faker.number().numberBetween(0, cities.length)];
        return soNha + " " + streets[faker.number().numberBetween(0, streets.length)]
                + ", " + wards[faker.number().numberBetween(0, wards.length)]
                + ", " + city;
    }

    /** CCCD 12 số — format gần thực tế, tránh chuỗi ngẫu nhiên bị UAT chặn. */
    private static String generateCccd(Faker faker) {
        return "0" + String.format("%02d", faker.number().numberBetween(1, 96))
                + faker.number().digits(9);
    }

    /**
     * Full mức B (pairwise): mọi cặp loại đơn–việc, 4 tư cách Phá sản,
     * xoay đủ nhánh CN/TC / đại diện / số BD / NLQ / TLBS.
     */
    public static Object[][] generateFullCoverageData() {
        List<FullCoverageMatrix.BranchSpec> specs = FullCoverageMatrix.build();
        List<String> gaps = FullCoverageMatrix.validateCoverage(specs);
        if (!gaps.isEmpty()) {
            throw new IllegalStateException("Full pairwise B thiếu nhánh: " + String.join("; ", gaps));
        }
        System.out.println(" 📋 " + FullCoverageMatrix.summarize(specs));

        Object[][] data = new Object[specs.size()][1];
        for (int i = 0; i < specs.size(); i++) {
            FullCoverageMatrix.BranchSpec spec = specs.get(i);
            Faker faker = createSeededFaker(spec.seedIndex());
            RowSelections selections = fromBranchSpec(spec);
            normalizePhaSanSelections(selections, spec.seedIndex());
            normalizeThuanTinhSelections(selections);
            validateSelections(selections);
            TaoDonScenario scenario = buildScenario(i, selections, faker);
            data[i][0] = scenario;
            if (i < 5 || DataDictionary.isPhaSan(scenario.loaiDon()) || i == specs.size() - 1) {
                System.out.println(" 🎲 Full B #" + (i + 1) + "/" + specs.size() + ": "
                        + scenario.loaiDon() + " / " + scenario.loaiViec()
                        + " — ND=" + scenario.loaiChuThe()
                        + ", BD×" + scenario.soLuongBiDon()
                        + (DataDictionary.allowsDongNguyenDon(scenario.loaiDon())
                        ? ", đồngND=" + scenario.coDongNguyenDon() : "")
                        + (DataDictionary.isPhaSan(scenario.loaiDon())
                        ? ", tư cách=" + scenario.tuCachNopDon() : ""));
            }
        }
        return data;
    }

    /**
     * Mid: ~35 kịch bản loại đơn thường (1 nhánh/cặp + pad) + đủ 4 tư cách Phá sản.
     */
    public static Object[][] generateMidCoverageData() {
        List<FullCoverageMatrix.BranchSpec> specs = MidCoverageMatrix.build();
        List<String> gaps = MidCoverageMatrix.validateCoverage(specs);
        if (!gaps.isEmpty()) {
            throw new IllegalStateException("Mid thiếu nhánh: " + String.join("; ", gaps));
        }
        System.out.println(" 📋 " + MidCoverageMatrix.summarize(specs));

        Object[][] data = new Object[specs.size()][1];
        for (int i = 0; i < specs.size(); i++) {
            FullCoverageMatrix.BranchSpec spec = specs.get(i);
            Faker faker = createSeededFaker(spec.seedIndex());
            RowSelections selections = fromBranchSpec(spec);
            normalizePhaSanSelections(selections, spec.seedIndex());
            normalizeThuanTinhSelections(selections);
            validateSelections(selections);
            TaoDonScenario scenario = buildScenario(i, selections, faker);
            data[i][0] = scenario;
            if (i < 5 || DataDictionary.isPhaSan(scenario.loaiDon()) || i == specs.size() - 1) {
                System.out.println(" 🎲 Mid #" + (i + 1) + "/" + specs.size() + ": "
                        + scenario.loaiDon() + " / " + scenario.loaiViec()
                        + " — ND=" + scenario.loaiChuThe()
                        + ", BD×" + scenario.soLuongBiDon()
                        + (DataDictionary.isPhaSan(scenario.loaiDon())
                        ? ", tư cách=" + scenario.tuCachNopDon() : ""));
            }
        }
        return data;
    }

    /**
     * Smoke (3 kịch bản):
     * 1) Phá sản (luồng đặc thù)
     * 2) Dân sự / Bồi thường — eform iframe bước 4 (hiện UAT chỉ có case này)
     * 3) 1 kịch bản ngẫu nhiên khác (không Phá sản, không trùng eform trên)
     */
    public static Object[][] generateSmokeData() {
        Object[][] data = new Object[SMOKE_ROW_COUNT][1];
        Faker faker = new Faker(new Locale("vi"));

        RowSelections phaSan = buildPhaSanSelections(faker, 0);
        validateSelections(phaSan);
        data[0][0] = buildScenario(0, phaSan, faker);
        System.out.println(" 🎲 Kịch bản smoke số 1 (bắt buộc Phá sản): "
                + phaSan.loaiDon + " / " + phaSan.loaiViec
                + " — tư cách: " + phaSan.tuCachNopDon);

        RowSelections eform = buildDanSuBoiThuongEformSelections();
        validateSelections(eform);
        data[1][0] = buildScenario(1, eform, faker);
        System.out.println(" 🎲 Kịch bản smoke số 2 (eform bước 4): "
                + eform.loaiDon + " / " + eform.loaiViec);

        RowSelections random = buildRandomSelections(faker);
        int guard = 0;
        while (guard++ < 30 && (DataDictionary.isPhaSan(random.loaiDon)
                || isDanSuBoiThuongEform(random.loaiDon, random.loaiViec))) {
            random = buildRandomSelections(faker);
        }
        normalizePhaSanSelections(random, 2);
        validateSelections(random);
        data[2][0] = buildScenario(2, random, faker);
        System.out.println(" 🎲 Kịch bản smoke số 3 (ngẫu nhiên): "
                + random.loaiDon + " / " + random.loaiViec
                + " — số bị đơn: " + random.soLuongBiDon);

        return data;
    }

    /** Một kịch bản ngẫu nhiên (vd. test chỉnh sửa). */
    public static TaoDonScenario generateOneRandomScenario() {
        return (TaoDonScenario) generateDynamicData(1)[0][0];
    }

    /**
     * Kịch bản có tài liệu bắt buộc ở bước 5 — phù hợp test Chỉnh sửa từ Xem lại (UI mới).
     * Tránh Hành chính/Hôn nhân không có hồ sơ bắt buộc → xem trước đơn lỗi.
     */
    public static TaoDonScenario generateScenarioForReviewEdit() {
        Faker faker = new Faker(new Locale("vi"));
        TaoDonScenario preferred = pickReviewEditScenario(faker, true);
        if (preferred != null) {
            return preferred;
        }
        TaoDonScenario fallback = pickReviewEditScenario(faker, false);
        if (fallback != null) {
            return fallback;
        }
        return generateOneRandomScenario();
    }

    /**
     * Đúng 1 kịch bản / mỗi loại đơn trong catalog (7 loại) — gọn để kiểm tra bước 2–3:
     * Cá nhân, 1 bị đơn, không đồng ND / NLQ / đại diện.
     */
    public static Object[][] generateBuoc23AllLoaiDonData() {
        Faker faker = new Faker(new Locale("vi"));
        String[] loaiDons = MasterDataCatalog.getLoaiDon();
        Object[][] data = new Object[loaiDons.length][1];
        for (int i = 0; i < loaiDons.length; i++) {
            String loaiDon = loaiDons[i];
            String[] viecs = MasterDataCatalog.getLoaiViecByLoaiDon(loaiDon);
            String loaiViec = viecs[Math.floorMod(i, viecs.length)];
            // Hành chính ưu tiên việc có form cơ quan ổn định (tránh "Khác" UI lệch).
            if ("Hành chính".equals(loaiDon)) {
                for (String v : viecs) {
                    if (v != null && v.toLowerCase(Locale.ROOT).contains("quyết định")) {
                        loaiViec = v;
                        break;
                    }
                }
            }
            RowSelections s = new RowSelections();
            s.loaiDon = loaiDon;
            s.loaiViec = loaiViec;
            s.toaAn = DataDictionary.pick(MasterDataCatalog.getToaAn(), i);
            s.loaiChuTheNguyenDon = "Cá nhân";
            s.gioiTinh = DataDictionary.pick(MasterDataCatalog.getGioiTinh(), i);
            s.noiCap = DataDictionary.pick(MasterDataCatalog.getNoiCapCccd(), i);
            s.loaiHinhToChuc = DataDictionary.pick(MasterDataCatalog.getLoaiHinhToChuc(), i);
            s.coNguoiDaiDien = "Không";
            s.quanHeDaiDien = DataDictionary.pick(MasterDataCatalog.getQuanHeDaiDien(), i);
            s.loaiChuTheBiDon = DataDictionary.pick(MasterDataCatalog.getLoaiChuTheBiDon(), i);
            s.loaiHinhBiDon = DataDictionary.pick(MasterDataCatalog.getLoaiHinhToChuc(), i + 1);
            s.coNguoiLienQuan = "Không";
            s.coTaiLieuBoSung = "Không";
            s.soLuongBiDon = 1;
            s.coDongNguyenDon = "Không";
            if (DataDictionary.isPhaSan(loaiDon)) {
                s.tuCachNopDon = DataDictionary.pick(MasterDataCatalog.getTuCachNopDonPhaSan(), i);
            }
            normalizePhaSanSelections(s, i);
            validateSelections(s);
            TaoDonScenario scenario = buildScenario(i, s, faker);
            System.out.println(" 🎲 Kịch bản bước 2–3 [" + (i + 1) + "/" + loaiDons.length + "]: "
                    + scenario.loaiDon() + " / " + scenario.loaiViec());
            data[i][0] = scenario;
        }
        return data;
    }

    private static TaoDonScenario pickReviewEditScenario(Faker faker, boolean preferDanSuOrShtt) {
        for (int i = 0; i < 25; i++) {
            RowSelections selections = buildRandomSelections(faker);
            normalizePhaSanSelections(selections, i);
            validateSelections(selections);
            if (DataDictionary.isHanhChinh(selections.loaiDon)
                    || DataDictionary.isHonNhanGiaDinh(selections.loaiDon)
                    || DataDictionary.isPhaSan(selections.loaiDon)) {
                continue;
            }
            if (preferDanSuOrShtt
                    && !"Dân sự".equals(selections.loaiDon)
                    && !"Sở hữu trí tuệ".equals(selections.loaiDon)) {
                continue;
            }
            if (DataDictionary.isToChuc(selections.loaiChuTheNguyenDon)) {
                continue;
            }
            // Giữ kịch bản gọn: 1 bị đơn, không đồng ND / NLQ — tránh treo lâu khi đi lại 1→5 sau Chỉnh sửa.
            selections.soLuongBiDon = 1;
            selections.coDongNguyenDon = "Không";
            selections.coNguoiLienQuan = "Không";
            selections.coTaiLieuBoSung = "Không";
            selections.coNguoiDaiDien = "Không";
            TaoDonScenario scenario = buildScenario(i, selections, faker);
            System.out.println(" 🎲 Kịch bản chỉnh sửa Xem lại: "
                    + scenario.loaiDon() + " / " + scenario.loaiViec());
            return scenario;
        }
        return null;
    }

    public static Object[][] generateDynamicData(int soLuongKichBan) {
        Object[][] data = new Object[soLuongKichBan][1];
        Faker faker = new Faker(new Locale("vi"));
        for (int i = 0; i < soLuongKichBan; i++) {
            RowSelections selections = buildRandomSelections(faker);
            normalizePhaSanSelections(selections, i);
            validateSelections(selections);
            TaoDonScenario scenario = buildScenario(i, selections, faker);
            System.out.println(" 🎲 Kịch bản ngẫu nhiên số " + (i + 1) + ": "
                    + scenario.loaiDon() + " / " + scenario.loaiViec()
                    + " — số bị đơn: " + scenario.soLuongBiDon());
            data[i][0] = scenario;
        }
        return data;
    }

    private static void validateSelections(RowSelections s) {
        MasterDataCatalog.assertInCatalog(s.loaiDon, "loaiDon", MasterDataCatalog.getLoaiDon());
        MasterDataCatalog.assertInCatalog(s.loaiViec, "loaiViec", MasterDataCatalog.getLoaiViecByLoaiDon(s.loaiDon));
        MasterDataCatalog.assertInCatalog(s.toaAn, "toaAn", MasterDataCatalog.getToaAn());

        MasterDataCatalog.assertInCatalog(s.loaiChuTheNguyenDon, "loaiChuTheNguyenDon", MasterDataCatalog.getLoaiChuTheNguyenDon());
        if (!DataDictionary.isToChuc(s.loaiChuTheNguyenDon)) {
            MasterDataCatalog.assertInCatalog(s.gioiTinh, "gioiTinh", MasterDataCatalog.getGioiTinh());
            MasterDataCatalog.assertInCatalog(s.noiCap, "noiCapCccd", MasterDataCatalog.getNoiCapCccd());
        } else {
            MasterDataCatalog.assertInCatalog(s.loaiHinhToChuc, "loaiHinhToChuc", MasterDataCatalog.getLoaiHinhToChuc());
        }

        MasterDataCatalog.assertInCatalog(s.coNguoiDaiDien, "coNguoiDaiDien", MasterDataCatalog.getCoKhong());
        if ("Có".equals(s.coNguoiDaiDien) && !DataDictionary.isToChuc(s.loaiChuTheNguyenDon)) {
            MasterDataCatalog.assertInCatalog(s.quanHeDaiDien, "quanHeDaiDien", MasterDataCatalog.getQuanHeDaiDien());
        }

        if (DataDictionary.isPhaSan(s.loaiDon)) {
            MasterDataCatalog.assertInCatalog(s.tuCachNopDon, "tuCachNopDonPhaSan",
                    MasterDataCatalog.getTuCachNopDonPhaSan());
        } else {
            MasterDataCatalog.assertInCatalog(s.loaiChuTheBiDon, "loaiChuTheBiDon", MasterDataCatalog.getLoaiChuTheBiDon());
        }
        MasterDataCatalog.assertInCatalog(s.coNguoiLienQuan, "coNguoiLienQuan", MasterDataCatalog.getCoKhong());
    }

    /**
     * 1 kịch bản gọn tới bước 4 eform: Dân sự / Bồi thường thiệt hại ngoài hợp đồng.
     * Cá nhân, 1 bị đơn, không đồng ND / NLQ / đại diện.
     */
    public static Object[][] generateDanSuBoiThuongEformProbeData() {
        Faker faker = new Faker(new Locale("vi"));
        RowSelections s = buildDanSuBoiThuongEformSelections();
        validateSelections(s);
        TaoDonScenario scenario = buildScenario(0, s, faker);
        System.out.println(" 🎲 Probe eform bước 4: " + scenario.loaiDon() + " / " + scenario.loaiViec());
        return new Object[][]{{scenario}};
    }

    /** Dân sự / Bồi thường — eform iframe bước 4 (UAT hiện chỉ có case này). */
    private static RowSelections buildDanSuBoiThuongEformSelections() {
        RowSelections s = new RowSelections();
        s.loaiDon = "Dân sự";
        s.loaiViec = "Bồi thường thiệt hại ngoài hợp đồng";
        s.toaAn = DataDictionary.pick(MasterDataCatalog.getToaAn(), 0);
        s.loaiChuTheNguyenDon = "Cá nhân";
        s.gioiTinh = DataDictionary.pick(MasterDataCatalog.getGioiTinh(), 0);
        s.noiCap = DataDictionary.pick(MasterDataCatalog.getNoiCapCccd(), 0);
        s.loaiHinhToChuc = DataDictionary.pick(MasterDataCatalog.getLoaiHinhToChuc(), 0);
        s.coNguoiDaiDien = "Không";
        s.quanHeDaiDien = DataDictionary.pick(MasterDataCatalog.getQuanHeDaiDien(), 0);
        s.loaiChuTheBiDon = DataDictionary.pick(MasterDataCatalog.getLoaiChuTheBiDon(), 0);
        s.loaiHinhBiDon = DataDictionary.pick(MasterDataCatalog.getLoaiHinhToChuc(), 1);
        s.coNguoiLienQuan = "Không";
        s.coTaiLieuBoSung = "Không";
        s.soLuongBiDon = 1;
        s.coDongNguyenDon = "Không";
        return s;
    }

    private static boolean isDanSuBoiThuongEform(String loaiDon, String loaiViec) {
        return "Dân sự".equals(loaiDon)
                && loaiViec != null
                && loaiViec.contains("Bồi thường thiệt hại ngoài hợp đồng");
    }

    private static TaoDonScenario buildScenario(int index, RowSelections selections, Faker faker) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        TaoDonScenario.Builder b = TaoDonScenario.builder()
                .stt(String.valueOf(index + 1))
                .loaiDon(selections.loaiDon)
                .loaiViec(selections.loaiViec)
                .toaAn(selections.toaAn)
                .tomTat(faker.lorem().sentence(8))
                .loaiChuThe(selections.loaiChuTheNguyenDon)
                .sdt("09" + faker.number().digits(8))
                .email(faker.internet().emailAddress())
                .coNguoiDaiDien(DataDictionary.isToChuc(selections.loaiChuTheNguyenDon)
                        ? "Không"
                        : selections.coNguoiDaiDien)
                .loaiBiDon(selections.loaiChuTheBiDon)
                .sdtBD("09" + faker.number().digits(8))
                .emailBD(faker.internet().emailAddress())
                .coNguoiLienQuan(selections.coNguoiLienQuan)
                .thoiDiemPhatSinh(sdf.format(faker.date().past(800, java.util.concurrent.TimeUnit.DAYS)))
                .tomTatQuaTrinh(longText(faker, 100))
                .yeuCauCuThe(faker.lorem().sentence(12))
                .canCuPhapLy("Điều " + faker.number().numberBetween(1, 500) + " Bộ luật Dân sự năm 2015")
                .coTaiLieuBoSung(selections.coTaiLieuBoSung)
                .tuCachNopDon(selections.tuCachNopDon)
                .soLuongBiDon(selections.soLuongBiDon);

        if (!DataDictionary.isToChuc(selections.loaiChuTheNguyenDon)) {
            b.hoTen(faker.name().fullName())
                    .ngaySinh(sdf.format(faker.date().birthday(18, 60)))
                    .gioiTinh(selections.gioiTinh)
                    .cccd(generateCccd(faker))
                    .ngayCap(sdf.format(faker.date().past(1500, java.util.concurrent.TimeUnit.DAYS)))
                    .noiCap(selections.noiCap)
                    .thuongTru(vietnameseAddress(faker))
                    // 50/50: giống thường trú / địa chỉ liên lạc riêng
                    .lienLac(Math.floorMod(index, 2) == 0 ? "Giống thường trú" : vietnameseAddress(faker));
        } else {
            b.tenToChuc(cleanCompanyName(faker.company().name()))
                    .loaiHinhToChuc(selections.loaiHinhToChuc)
                    .mst(faker.number().digits(10))
                    .diaChiToChuc(vietnameseAddress(faker))
                    .nguoiDaiDienToChuc(faker.name().fullName())
                    .chucVuToChuc("Giám đốc")
                    .ngaySinh(sdf.format(faker.date().birthday(18, 60)))
                    .gioiTinh(selections.gioiTinh)
                    .cccd(generateCccd(faker))
                    .ngayCap(sdf.format(faker.date().past(1500, java.util.concurrent.TimeUnit.DAYS)))
                    .noiCap(selections.noiCap);
        }

        if ("Có".equals(selections.coNguoiDaiDien)
                && !DataDictionary.isToChuc(selections.loaiChuTheNguyenDon)) {
            b.tenNguoiDaiDien(faker.name().fullName()).quanHeDaiDien(selections.quanHeDaiDien);
        }

        // Phá sản bước 3 = DN/HTX bị yêu cầu (luôn tổ chức)
        if (DataDictionary.isPhaSan(selections.loaiDon) || DataDictionary.isToChuc(selections.loaiChuTheBiDon)) {
            b.loaiBiDon("Tổ chức")
                    .tenToChucBD(cleanCompanyName(faker.company().name()))
                    .loaiHinhBD(selections.loaiHinhBiDon)
                    .mstBD(faker.number().digits(10))
                    .diaChiTruSoBD(vietnameseAddress(faker))
                    .nguoiDaiDienBD(faker.name().fullName())
                    .chucVuBD("Giám đốc");
        } else {
            b.hoTenBD(faker.name().fullName())
                    .cccdBD(generateCccd(faker))
                    .namSinhBD(String.valueOf(faker.number().numberBetween(1960, 2000)))
                    .gioiTinhBD(selections.gioiTinh == null || selections.gioiTinh.isBlank()
                            ? "Nam" : selections.gioiTinh)
                    .diaChiCaNhanBD(vietnameseAddress(faker))
                    .noiOHienTaiBD(vietnameseAddress(faker))
                    .ngheNghiepBD(faker.options().option(
                            "Nhân viên văn phòng", "Kỹ sư", "Kinh doanh", "Lao động tự do", "Giáo viên"));
        }

        if ("Có".equals(selections.coNguoiLienQuan)) {
            b.hoTenNLQ(faker.name().fullName())
                    .lyDoNLQ(faker.lorem().sentence(6))
                    .thongTinLienLacNLQ("09" + faker.number().digits(8));
        }

        if (DataDictionary.isHanhChinh(selections.loaiDon)) {
            b.tenCoQuanHC("UBND " + faker.address().cityName())
                    .chucDanhHC("Chủ tịch UBND")
                    .nguoiThamQuyenHC(faker.name().fullName())
                    .diaChiTruSoBD(vietnameseAddress(faker));
        }

        if (DataDictionary.hasGiaTriTranhChap(selections.loaiDon)) {
            b.giaTriTranhChap(String.valueOf(faker.number().numberBetween(5_000_000L, 2_000_000_000L)));
        }

        if (selections.soLuongBiDon >= 2 && !DataDictionary.isPhaSan(selections.loaiDon)) {
            b.biDonThem(buildExtraBiDon(selections, faker));
        }

        String coDongNguyenDon = normalizeCoDongNguyenDon(selections);
        b.coDongNguyenDon(coDongNguyenDon);
        if ("Có".equals(coDongNguyenDon)) {
            b.dongNguyenDon(buildDongNguyenDon(faker, index));
        }

        return b.build();
    }

    private static String normalizeCoDongNguyenDon(RowSelections selections) {
        if (!DataDictionary.allowsDongNguyenDon(selections.loaiDon)) {
            return "Không";
        }
        String raw = selections.coDongNguyenDon;
        return (raw == null || raw.isBlank()) ? "Không" : raw.trim();
    }

    private static BiDonData buildExtraBiDon(RowSelections selections, Faker faker) {
        BiDonData.Builder extra = BiDonData.builder()
                .sdt("09" + faker.number().digits(8))
                .email(faker.internet().emailAddress());

        // Bị đơn #2: 50/50 Cá nhân / Tổ chức (độc lập với bị đơn #1)
        String loai2 = pickChuThe50_50(DataDictionary.getLoaiChuTheBiDon(), faker);
        if (DataDictionary.isHanhChinh(selections.loaiDon)) {
            // Hành chính không đổi loại cá nhân/tổ chức theo cùng rule — giữ cơ quan
            return extra.tenCoQuanHC("Sở " + faker.address().cityName())
                    .chucDanhHC("Giám đốc Sở")
                    .nguoiThamQuyenHC(faker.name().fullName())
                    .diaChiTruSo(vietnameseAddress(faker))
                    .build();
        }
        extra.loai(loai2);
        if (DataDictionary.isToChuc(loai2)) {
            return extra.tenToChuc(cleanCompanyName(faker.company().name()))
                    .loaiHinh(faker.options().option(DataDictionary.getLoaiHinhToChuc()))
                    .mst(faker.number().digits(10))
                    .diaChiTruSo(vietnameseAddress(faker))
                    .nguoiDaiDien(faker.name().fullName())
                    .chucVu("Giám đốc")
                    .build();
        }
        return extra.hoTen(faker.name().fullName())
                .cccd(generateCccd(faker))
                .namSinh(String.valueOf(faker.number().numberBetween(1965, 1998)))
                .gioiTinh(faker.options().option("Nam", "Nữ", "Khác"))
                .diaChiCaNhan(vietnameseAddress(faker))
                .noiOHienTai(vietnameseAddress(faker))
                .ngheNghiep(faker.options().option(
                        "Nhân viên văn phòng", "Kỹ sư", "Kinh doanh", "Lao động tự do"))
                .build();
    }

    private static String cleanCompanyName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Doanh nghiệp ABC";
        }
        String name = raw.trim().replaceAll("\\s+", " ");
        // Datafaker đôi khi trả "Cty ..." / đã có "Công ty" — chuẩn hóa 1 lần.
        name = name.replaceFirst("(?i)^công\\s*ty\\s+", "");
        name = name.replaceFirst("(?i)^cty\\s+", "");
        if (name.isBlank()) {
            return "Doanh nghiệp ABC";
        }
        return "Công ty " + name;
    }

    private static String longText(Faker faker, int minChars) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < minChars) {
            sb.append(faker.lorem().sentence(10)).append(' ');
        }
        return sb.toString().trim();
    }

    private static Faker createSeededFaker(int index) {
        return new Faker(new Locale("vi"), new Random(FAKER_SEED + index));
    }

    private static class RowSelections {
        String loaiDon;
        String loaiViec;
        String toaAn;
        String loaiChuTheNguyenDon;
        String loaiHinhToChuc;
        String gioiTinh;
        String noiCap;
        String coNguoiDaiDien;
        String quanHeDaiDien;
        String loaiChuTheBiDon;
        String loaiHinhBiDon;
        String coNguoiLienQuan;
        String coTaiLieuBoSung;
        String tuCachNopDon;
        int soLuongBiDon;
        String coDongNguyenDon;
    }

    private static void normalizePhaSanSelections(RowSelections s, int index) {
        if (!DataDictionary.isPhaSan(s.loaiDon)) {
            return;
        }
        s.loaiViec = DataDictionary.PHA_SAN_LOAI_VIEC_MAC_DINH;
        s.soLuongBiDon = 1;
        s.loaiChuTheBiDon = "Tổ chức";
        if (s.tuCachNopDon == null || s.tuCachNopDon.isBlank()) {
            s.tuCachNopDon = DataDictionary.pick(DataDictionary.getTuCachNopDonPhaSan(), index);
        }
        if (s.loaiHinhBiDon == null || s.loaiHinhBiDon.isBlank()) {
            s.loaiHinhBiDon = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), index + 1);
        }
    }

    private static void normalizeThuanTinhSelections(RowSelections s) {
        if (DataDictionary.isThuanTinhLyHon(s.loaiViec)) {
            s.soLuongBiDon = 1;
        }
    }

    private static RowSelections fromBranchSpec(FullCoverageMatrix.BranchSpec spec) {
        int i = spec.seedIndex();
        RowSelections s = new RowSelections();
        s.loaiDon = spec.loaiDon();
        s.loaiViec = spec.loaiViec();
        s.toaAn = DataDictionary.pick(DataDictionary.getToaAn(), i);
        s.loaiChuTheNguyenDon = pickChuTheByWantOrg(
                DataDictionary.getLoaiChuTheNguyenDon(), spec.nguyenDonToChuc());
        s.loaiHinhToChuc = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), i);
        s.gioiTinh = DataDictionary.pick(DataDictionary.getGioiTinh(), i);
        s.noiCap = DataDictionary.pick(DataDictionary.getNoiCapCccd(), i);
        s.coNguoiDaiDien = spec.coNguoiDaiDien() ? "Có" : "Không";
        s.quanHeDaiDien = DataDictionary.pick(DataDictionary.getQuanHeDaiDien(), i);
        s.loaiChuTheBiDon = pickChuTheByWantOrg(
                DataDictionary.getLoaiChuTheBiDon(), spec.biDonToChuc());
        s.loaiHinhBiDon = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), i + 1);
        s.coNguoiLienQuan = spec.coNguoiLienQuan() ? "Có" : "Không";
        s.coTaiLieuBoSung = spec.coTaiLieuBoSung() ? "Có" : "Không";
        s.soLuongBiDon = Math.max(1, spec.soLuongBiDon());
        s.tuCachNopDon = spec.tuCachNopDon();
        s.coDongNguyenDon = pickCoDongNguyenDon(spec.loaiDon(), spec.seedIndex());
        return s;
    }

    /** 50/50 Có/Không đồng nguyên đơn — áp dụng cho cả 7 loại đơn (theo seed/index). */
    private static String pickCoDongNguyenDon(String loaiDon, int index) {
        if (!DataDictionary.allowsDongNguyenDon(loaiDon)) {
            return "Không";
        }
        return Math.floorMod(index, 2) == 0 ? "Có" : "Không";
    }

    /** 50/50 ngẫu nhiên — dùng cho smoke / kịch bản random. */
    private static String pickCoDongNguyenDonRandom(Faker faker) {
        return faker.options().option(DataDictionary.getCoKhong());
    }

    private static DongNguyenDonData buildDongNguyenDon(Faker faker, int index) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        // 50/50 Cá nhân / Tổ chức — form đồng ND cũng có tab giống nguyên đơn chính.
        boolean toChuc = Math.floorMod(index, 2) == 1;
        if (toChuc) {
            return DongNguyenDonData.builder()
                    .loai("Tổ chức")
                    .tenToChuc(cleanCompanyName(faker.company().name()))
                    .loaiHinh(DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), index))
                    .mst(faker.number().digits(10))
                    .diaChiTruSo(vietnameseAddress(faker))
                    .nguoiDaiDien(faker.name().fullName())
                    .chucVu("Giám đốc")
                    .sdt("09" + faker.number().digits(8))
                    .email("dongnd.tc." + index + "@test.example.com")
                    .build();
        }
        return DongNguyenDonData.builder()
                .loai("Cá nhân")
                .hoTen(faker.name().fullName())
                .ngaySinh(sdf.format(faker.date().birthday(18, 60)))
                .gioiTinh(DataDictionary.pick(DataDictionary.getGioiTinh(), index))
                .cccd(generateCccd(faker))
                .diaChiCuTru(vietnameseAddress(faker))
                .noiOHienTai(vietnameseAddress(faker))
                .ngheNghiep("Kỹ sư phần mềm")
                .sdt("09" + faker.number().digits(8))
                .email("dongnd.cn." + index + "@test.example.com")
                .build();
    }

    private static RowSelections buildPhaSanSelections(Faker faker, int index) {
        RowSelections s = new RowSelections();
        s.loaiDon = "Phá sản";
        s.loaiViec = DataDictionary.PHA_SAN_LOAI_VIEC_MAC_DINH;
        // Tòa án: giữ ngẫu nhiên (không ép phân bố đều theo danh mục)
        s.toaAn = faker.options().option(DataDictionary.getToaAn());
        s.loaiChuTheNguyenDon = pickChuThe50_50(DataDictionary.getLoaiChuTheNguyenDon(), index);
        s.loaiHinhToChuc = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), index);
        s.gioiTinh = DataDictionary.pick(DataDictionary.getGioiTinh(), index);
        s.noiCap = DataDictionary.pick(DataDictionary.getNoiCapCccd(), index);
        // Có/Không người đại diện: 50/50 khi nguyên đơn Cá nhân
        s.coNguoiDaiDien = DataDictionary.pick(DataDictionary.getCoKhong(), index);
        s.quanHeDaiDien = DataDictionary.pick(DataDictionary.getQuanHeDaiDien(), index);
        s.loaiChuTheBiDon = "Tổ chức";
        s.loaiHinhBiDon = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), index + 1);
        s.coNguoiLienQuan = DataDictionary.pick(DataDictionary.getCoKhong(), index + 1);
        s.coTaiLieuBoSung = DataDictionary.pick(DataDictionary.getCoKhong(), index + 2);
        s.tuCachNopDon = DataDictionary.pick(DataDictionary.getTuCachNopDonPhaSan(), index);
        s.soLuongBiDon = 1;
        s.coDongNguyenDon = pickCoDongNguyenDon(s.loaiDon, index);
        return s;
    }

    private static RowSelections buildCoverageSelections(int i) {
        RowSelections s = new RowSelections();
        String[] pair = DataDictionary.pick(MasterDataCatalog.getLoaiDonViecPairs(), i).split(">");
        s.loaiDon = pair[0];
        s.loaiViec = pair[1];
        // Tòa án: random theo seed hàng — không ép xoay đều danh mục tòa
        s.toaAn = createSeededFaker(i).options().option(DataDictionary.getToaAn());
        s.loaiChuTheNguyenDon = pickChuThe50_50(DataDictionary.getLoaiChuTheNguyenDon(), i);
        s.loaiHinhToChuc = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), i);
        s.gioiTinh = DataDictionary.pick(DataDictionary.getGioiTinh(), i);
        s.noiCap = DataDictionary.pick(DataDictionary.getNoiCapCccd(), i);
        s.coNguoiDaiDien = DataDictionary.pick(DataDictionary.getCoKhong(), i);
        s.quanHeDaiDien = DataDictionary.pick(DataDictionary.getQuanHeDaiDien(), i);
        s.loaiChuTheBiDon = pickChuThe50_50(DataDictionary.getLoaiChuTheBiDon(), i + 1);
        s.loaiHinhBiDon = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), i + 1);
        s.coNguoiLienQuan = DataDictionary.pick(DataDictionary.getCoKhong(), i + 1);
        s.coTaiLieuBoSung = DataDictionary.pick(DataDictionary.getCoKhong(), i + 2);
        // Thêm bị đơn: 50/50 (1 hoặc 2) — trừ Phá sản (luôn 1)
        s.soLuongBiDon = (!DataDictionary.isPhaSan(s.loaiDon) && Math.floorMod(i, 2) == 0) ? 2 : 1;
        if (DataDictionary.isPhaSan(s.loaiDon)) {
            s.tuCachNopDon = DataDictionary.pick(DataDictionary.getTuCachNopDonPhaSan(), i);
        }
        s.coDongNguyenDon = pickCoDongNguyenDon(s.loaiDon, i);
        return s;
    }

    private static RowSelections buildRandomSelections(Faker faker) {
        RowSelections s = new RowSelections();
        List<String[]> pairs = MasterDataCatalog.getAllLoaiDonViecPairs();
        // Mỗi cặp loại đơn/loại việc có xác suất ngang nhau
        String[] pair = pairs.get(faker.random().nextInt(pairs.size()));
        s.loaiDon = pair[0];
        s.loaiViec = pair[1];
        // Tòa án: random tự do
        s.toaAn = faker.options().option(DataDictionary.getToaAn());
        s.loaiChuTheNguyenDon = pickChuThe50_50(DataDictionary.getLoaiChuTheNguyenDon(), faker);
        s.loaiHinhToChuc = faker.options().option(DataDictionary.getLoaiHinhToChuc());
        s.gioiTinh = faker.options().option(DataDictionary.getGioiTinh());
        s.noiCap = faker.options().option(DataDictionary.getNoiCapCccd());
        s.coNguoiDaiDien = faker.options().option(DataDictionary.getCoKhong());
        s.quanHeDaiDien = faker.options().option(DataDictionary.getQuanHeDaiDien());
        s.loaiChuTheBiDon = pickChuThe50_50(DataDictionary.getLoaiChuTheBiDon(), faker);
        s.loaiHinhBiDon = faker.options().option(DataDictionary.getLoaiHinhToChuc());
        s.coNguoiLienQuan = faker.options().option(DataDictionary.getCoKhong());
        s.coTaiLieuBoSung = faker.options().option(DataDictionary.getCoKhong());
        s.soLuongBiDon = (!DataDictionary.isPhaSan(s.loaiDon) && faker.bool().bool()) ? 2 : 1;
        if (DataDictionary.isPhaSan(s.loaiDon)) {
            s.tuCachNopDon = faker.options().option(DataDictionary.getTuCachNopDonPhaSan());
        }
        s.coDongNguyenDon = pickCoDongNguyenDonRandom(faker);
        return s;
    }

    /** Chẵn → Cá nhân, lẻ → Tổ chức (coverage / smoke Phá sản có index). */
    private static String pickChuThe50_50(String[] options, int index) {
        return pickChuTheByWantOrg(options, Math.floorMod(index, 2) == 1);
    }

    /** Random thật ~50/50 Cá nhân / Tổ chức. */
    private static String pickChuThe50_50(String[] options, Faker faker) {
        return pickChuTheByWantOrg(options, faker.bool().bool());
    }

    private static String pickChuTheByWantOrg(String[] options, boolean wantToChuc) {
        if (options == null || options.length == 0) {
            return wantToChuc ? "Tổ chức" : "Cá nhân";
        }
        String caNhan = null;
        String toChuc = null;
        for (String option : options) {
            if (option == null || option.isBlank()) {
                continue;
            }
            if (DataDictionary.isToChuc(option)) {
                if (toChuc == null) {
                    toChuc = option;
                }
            } else if (caNhan == null) {
                caNhan = option;
            }
        }
        if (wantToChuc) {
            return toChuc != null ? toChuc : options[Math.min(1, options.length - 1)];
        }
        return caNhan != null ? caNhan : options[0];
    }
}

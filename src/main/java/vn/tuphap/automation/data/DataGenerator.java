package vn.tuphap.automation.data;

import net.datafaker.Faker;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DataGenerator {

    private static final long FAKER_SEED = 20240724L;
    private static final int SMOKE_ROW_COUNT = 3;

    /**
     * Địa chỉ VN: số nhà/đường + tỉnh (hint chọn dropdown tỉnh).
     * Textarea "Chi tiết" chỉ nhận phần số nhà/đường ({@code WebUI.toAddressStreetDetail}).
     * <p>
     * <b>Cố ý KHÔNG sinh tên phường.</b> Bản cũ bốc phường từ một danh sách 5 phường Hà Nội và
     * tỉnh từ một danh sách 10 tỉnh <i>độc lập nhau</i>, nên ra những địa chỉ không tồn tại kiểu
     * "Phường Bồ Đề, Cần Thơ". {@code WebUI.extractWardHint} lấy tên phường đó đi tìm trong danh
     * sách phường của tỉnh đã chọn — đo trên 39 case: <b>98/122 lần trượt (80%)</b>, mỗi lần tốn
     * thêm một lượt mở dropdown + gõ + quét rồi mới chịu chọn ngẫu nhiên.
     * <p>
     * Không có hint thì luồng chọn thẳng một phường <i>có thật</i> của tỉnh đó, và
     * {@code TestActionLog.chon} ghi lại đúng phường đã chọn — báo cáo trung thực hơn hẳn so với
     * việc mang theo một tên phường không thuộc tỉnh nào trong đơn.
     */
    private static String vietnameseAddress(Faker faker) {
        int soNha = faker.number().numberBetween(1, 999);
        String[] streets = {"Nguyễn Huệ", "Lê Lợi", "Trần Phú", "Hoàng Diệu", "Phan Đình Phùng", "Bà Triệu"};
        // Tên tỉnh/TP gần option dropdown UAT (hint chọn tỉnh từ đoạn cuối địa chỉ).
        String[] cities = {"Hà Nội", "Thành phố Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
                "Bắc Ninh", "Ninh Bình", "Sơn La", "Lâm Đồng", "Khánh Hòa"};
        String city = cities[faker.number().numberBetween(0, cities.length)];
        return soNha + " " + streets[faker.number().numberBetween(0, streets.length)]
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
     * Sinh đúng các case đã chọn trên menu ({@code run.cases}) / Google Sheet.
     * Số case có thể nhiều hơn số Chrome — TestNG xếp hàng, reuse session theo thread.
     * <p>
     * Mỗi dòng trả về gồm 2 cột: {@code [0]} = {@link TaoDonScenario},
     * {@code [1]} = {@code CaseProfile} đã sinh ra scenario đó. Dòng có giá trị không khớp danh mục
     * bị bỏ kèm cảnh báo (không làm hỏng cả lượt chạy), nên số dòng trả về có thể ít hơn
     * {@code profiles.size()} — vì vậy cặp scenario↔profile phải ghép tại đây, không ghép lại
     * theo chỉ số ở nơi gọi.
     */
    public static Object[][] generateConfiguredCases(
            java.util.List<vn.tuphap.automation.config.RunFlowConfig.CaseProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalArgumentException("run.cases rỗng — không sinh được kịch bản.");
        }
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        int skippedBad = 0;
        Faker faker = new Faker(new Locale("vi"));
        for (int i = 0; i < profiles.size(); i++) {
            var p = profiles.get(i);
            TaoDonScenario scenario;
            try {
                RowSelections s = buildConfiguredSelections(p, i);
                normalizePhaSanSelections(s, i);
                normalizeThuanTinhSelections(s);
                validateSelections(s);
                scenario = buildScenario(i, s, faker);
                if (p.hasNegativeExpectation()) {
                    scenario = applyNegativeFieldOverride(scenario, p);
                }
            } catch (RuntimeException ex) {
                // Giá trị lạ ở 1 dòng (gõ sai Loại đơn / Loại việc / Tòa án…) chỉ được bỏ dòng đó,
                // không được làm hỏng cả lượt chạy của những dòng còn lại.
                skippedBad++;
                System.out.println(" ⚠ Bỏ qua case dòng " + (i + 1) + " (" + p.loaiDon()
                        + (p.loaiViec() == null || p.loaiViec().isBlank() ? "" : " / " + p.loaiViec())
                        + "): " + ex.getMessage());
                continue;
            }
            System.out.println(" Case [" + (i + 1) + "/" + profiles.size() + "]: "
                    + scenario.loaiDon() + " / " + scenario.loaiViec()
                    + " — ND=" + scenario.loaiChuThe()
                    + ", tòa=" + scenario.toaAn()
                    + ", BD×" + scenario.soLuongBiDon()
                    + ", đồngND=" + scenario.coDongNguyenDon()
                    + ", đại diện=" + scenario.coNguoiDaiDien()
                    + ", NLQ=" + scenario.coNguoiLienQuan()
                    + ", TLBS=" + scenario.coTaiLieuBoSung()
                    + (DataDictionary.isPhaSan(scenario.loaiDon())
                    ? ", tư cách=" + scenario.tuCachNopDon() : "")
                    + " | until=" + p.untilStep()
                    + (p.submit() ? "+submit" : "")
                    + (p.hasNegativeExpectation() ? "  [CA ÂM: " + p.truongLoi() + "=\"" + p.giaTriLoi() + "\"]" : "")
                    + (p.ghiChu() == null || p.ghiChu().isBlank() ? "" : "  [" + p.ghiChu() + "]"));
            // Cột 1 = CaseProfile sinh ra chính scenario này — ghép tại nguồn để dòng bị bỏ ở trên
            // không làm lệch cặp scenario↔profile (trước đây ghép lại theo chỉ số ở tầng test).
            rows.add(new Object[]{scenario, p});
        }
        if (skippedBad > 0) {
            System.out.println(" ⚠ Tổng cộng " + skippedBad + "/" + profiles.size()
                    + " case bị bỏ vì dữ liệu không khớp danh mục — sửa lại các dòng nêu trên.");
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("Không dựng được case nào từ " + profiles.size()
                    + " dòng cấu hình — mọi dòng đều có giá trị không khớp danh mục (xem cảnh báo bên trên).");
        }
        return rows.toArray(new Object[0][]);
    }

    /**
     * Ca âm: tiêm {@code p.giaTriLoi()} vào đúng 1 field của scenario hợp lệ, để kiểm tra hệ thống
     * có chặn đúng validation hay không (thay vì automation luôn điền dữ liệu hợp lệ như trước).
     * Field không khớp ngữ cảnh (vd. "Mã số thuế" cho nguyên đơn Cá nhân) thì bỏ qua kèm cảnh báo —
     * không throw, vì 1 dòng cấu hình sai không nên làm sập cả run.
     */
    private static TaoDonScenario applyNegativeFieldOverride(
            TaoDonScenario scenario, vn.tuphap.automation.config.RunFlowConfig.CaseProfile p) {
        FieldOverrideAttempt attempt = tryFieldOverride(scenario, p.truongLoi(), p.giaTriLoi());
        if (!attempt.applicable()) {
            System.out.println("⚠ Trường lỗi '" + p.truongLoi() + "' không áp dụng cho case này ("
                    + attempt.skipReason() + ") — bỏ qua override, nhưng case vẫn được coi là ca âm"
                    + " nên sẽ FAIL nếu không bị chặn. Hãy chọn Trường lỗi khác hoặc sửa Chủ thể.");
            return scenario;
        }
        String value = p.giaTriLoi() == null ? "" : p.giaTriLoi();
        System.out.println("   ↳ Ca âm: ép " + p.truongLoi() + " = \"" + value + "\""
                + (value.isBlank() ? " (để trống)" : "")
                + " — kỳ vọng bị chặn"
                + (p.thongBaoMongDoi() == null || p.thongBaoMongDoi().isBlank()
                ? "" : ": \"" + p.thongBaoMongDoi() + "\""));
        return attempt.result();
    }

    /**
     * Kết quả thử ép 1 field sang giá trị sai — {@code applicable=false} nghĩa là field này không
     * tồn tại/không áp dụng cho ngữ cảnh của {@code scenario} (vd. MST cho nguyên đơn Cá nhân);
     * khi đó {@code result()} chính là scenario gốc, chưa đổi gì. Dùng chung cho ca âm khai báo
     * trên sheet ({@link #applyNegativeFieldOverride}) và bộ quét dò field
     * ({@code FieldDiscoverySweepTest}).
     */
    public record FieldOverrideAttempt(boolean applicable, String skipReason, TaoDonScenario result) {
    }

    /**
     * Thử ép field {@code truongLoiRaw} (nhãn tiếng Việt, xem {@link #TRUONG_LOI_HOP_LE}) sang
     * {@code value} trên một bản sao của {@code scenario}.
     * Không throw — field không nhận diện được hoặc không áp dụng cho ngữ cảnh hiện tại
     * (loại chủ thể nguyên đơn/bị đơn, loại đơn) đều trả {@code applicable=false} kèm lý do.
     */
    public static FieldOverrideAttempt tryFieldOverride(TaoDonScenario scenario, String truongLoiRaw, String value) {
        String key = normalizeNegativeFieldKey(truongLoiRaw);
        String v = value == null ? "" : value;
        boolean nguyenDonToChuc = DataDictionary.isToChuc(scenario.loaiChuThe());
        // BiDonPage.dienMotBiDon có 3 nhánh: Hành chính (cơ quan) / Phá sản (luôn Tổ chức) /
        // còn lại theo loaiBiDon — không phải chỉ CN/TC như nguyên đơn.
        boolean hanhChinh = DataDictionary.isHanhChinh(scenario.loaiDon());
        boolean phaSan = DataDictionary.isPhaSan(scenario.loaiDon());
        boolean biDonNhanhToChuc = phaSan || (!hanhChinh && DataDictionary.isToChuc(scenario.loaiBiDon()));
        boolean biDonNhanhCaNhan = !hanhChinh && !phaSan && !DataDictionary.isToChuc(scenario.loaiBiDon());

        TaoDonScenario.Builder b = scenario.toBuilder();
        switch (key) {
            case "sdt" -> b.sdt(v);
            case "email" -> b.email(v);
            case "cccd" -> {
                if (nguyenDonToChuc) {
                    return notApplicable(scenario, "nguyên đơn đang là Tổ chức");
                }
                b.cccd(v);
            }
            case "hoten" -> {
                if (nguyenDonToChuc) {
                    return notApplicable(scenario, "nguyên đơn đang là Tổ chức");
                }
                b.hoTen(v);
            }
            case "ngaysinh" -> {
                // Người đại diện Tổ chức cũng có ô ngày sinh, nhưng chỉ hiện tuỳ trạng thái UI
                // (NguyenDonPage.dienNguoiDaiDienToChuc kiểm tra isElementVisible lúc chạy) — không
                // suy được từ dữ liệu tĩnh nên tạm giới hạn ở nhánh Cá nhân cho chắc chắn áp dụng được.
                if (nguyenDonToChuc) {
                    return notApplicable(scenario,
                            "nguyên đơn đang là Tổ chức (ô ngày sinh người đại diện chỉ hiện có điều kiện)");
                }
                b.ngaySinh(v);
            }
            case "ngaycap" -> {
                if (nguyenDonToChuc) {
                    return notApplicable(scenario,
                            "nguyên đơn đang là Tổ chức (ô ngày cấp người đại diện chỉ hiện có điều kiện)");
                }
                b.ngayCap(v);
            }
            case "mst" -> {
                if (!nguyenDonToChuc) {
                    return notApplicable(scenario, "nguyên đơn đang là Cá nhân");
                }
                b.mst(v);
            }
            // sdtBD được đọc ở cả 3 nhánh bị đơn (kể cả Hành chính) — luôn áp dụng được.
            case "sdtbd" -> b.sdtBD(v);
            case "emailbd" -> {
                if (hanhChinh) {
                    return notApplicable(scenario, "bị đơn Hành chính (cơ quan) không có ô Email");
                }
                b.emailBD(v);
            }
            case "cccdbd" -> {
                if (!biDonNhanhCaNhan) {
                    return notApplicable(scenario,
                            "bị đơn không thuộc nhánh Cá nhân chuẩn (Hành chính/Phá sản/Tổ chức)");
                }
                b.cccdBD(v);
            }
            case "hotenbd" -> {
                if (!biDonNhanhCaNhan) {
                    return notApplicable(scenario,
                            "bị đơn không thuộc nhánh Cá nhân chuẩn (Hành chính/Phá sản/Tổ chức)");
                }
                b.hoTenBD(v);
            }
            case "mstbd" -> {
                if (!biDonNhanhToChuc) {
                    return notApplicable(scenario, "bị đơn không thuộc nhánh Tổ chức");
                }
                b.mstBD(v);
            }
            case "giatritranhchap" -> {
                if (!DataDictionary.hasGiaTriTranhChap(scenario.loaiDon())) {
                    return notApplicable(scenario,
                            "loại đơn '" + scenario.loaiDon() + "' không có ô Giá trị tranh chấp");
                }
                b.giaTriTranhChap(v);
            }
            case "gioitinh" -> {
                if (nguyenDonToChuc) {
                    return notApplicable(scenario, "nguyên đơn đang là Tổ chức (không có ô Giới tính)");
                }
                b.gioiTinh(v);
            }
            case "thuongtru" -> {
                if (nguyenDonToChuc) {
                    return notApplicable(scenario, "nguyên đơn đang là Tổ chức (không có ô Địa chỉ thường trú)");
                }
                b.thuongTru(v);
            }
            case "nghenghiepbd" -> {
                if (!biDonNhanhCaNhan) {
                    return notApplicable(scenario,
                            "bị đơn không thuộc nhánh Cá nhân chuẩn (Hành chính/Phá sản/Tổ chức)");
                }
                b.ngheNghiepBD(v);
            }
            case "noiohientai" -> {
                if (!biDonNhanhCaNhan) {
                    return notApplicable(scenario,
                            "bị đơn không thuộc nhánh Cá nhân chuẩn (Hành chính/Phá sản/Tổ chức)");
                }
                b.noiOHienTaiBD(v);
            }
            // 4 field dưới đây thuộc bước 4 (nhánh textarea cố định) — không có predicate riêng theo
            // loại đơn trong DataDictionary như giatritranhchap, nên coi là luôn áp dụng được (giống
            // sdt/email). Với Dân sự/Bồi thường, bước 4 có thể đổi sang eform trong iframe — override
            // vẫn set được field Java, chỉ là không chắc phản ánh lên UI cho 2 loại đơn đó.
            case "tomtatquatrinh" -> b.tomTatQuaTrinh(v);
            case "yeucaucuthe" -> b.yeuCauCuThe(v);
            case "cancuphaply" -> b.canCuPhapLy(v);
            case "thoidiemphatsinh" -> b.thoiDiemPhatSinh(v);
            default -> {
                return notApplicable(scenario, "Trường lỗi '" + truongLoiRaw + "' không nhận diện được"
                        + " — xem danh sách hợp lệ ở DataGenerator.TRUONG_LOI_HOP_LE / README mục 6.4");
            }
        }
        return new FieldOverrideAttempt(true, "", b.build());
    }

    private static FieldOverrideAttempt notApplicable(TaoDonScenario original, String reason) {
        return new FieldOverrideAttempt(false, reason, original);
    }

    /** Tên trường hợp lệ cho "Trường lỗi" (xem {@link #TRUONG_LOI_HOP_LE}) — không phân biệt dấu/hoa thường. */
    private static String normalizeNegativeFieldKey(String raw) {
        String norm = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        norm = java.text.Normalizer.normalize(norm, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z]+", "");
        boolean biDon = norm.contains("bidon");
        if (norm.contains("dienthoai")) {
            return biDon ? "sdtbd" : "sdt";
        }
        if (norm.contains("email") || norm.contains("thudientu")) {
            return biDon ? "emailbd" : "email";
        }
        if (norm.contains("cccd") || norm.contains("hochieu") || norm.contains("cmnd")) {
            return biDon ? "cccdbd" : "cccd";
        }
        if (norm.contains("ngaysinh")) {
            return "ngaysinh";
        }
        if (norm.contains("ngaycap")) {
            return "ngaycap";
        }
        if (norm.contains("hoten")) {
            return biDon ? "hotenbd" : "hoten";
        }
        if (norm.contains("masothue") || norm.contains("mst")) {
            return biDon ? "mstbd" : "mst";
        }
        if (norm.contains("giatritranhchap")) {
            return "giatritranhchap";
        }
        if (norm.contains("gioitinh")) {
            return "gioitinh";
        }
        if (norm.contains("thuongtru")) {
            return "thuongtru";
        }
        if (norm.contains("nghenghiep")) {
            return "nghenghiepbd";
        }
        if (norm.contains("noiohientai")) {
            return "noiohientai";
        }
        if (norm.contains("tomtatquatrinh")) {
            return "tomtatquatrinh";
        }
        if (norm.contains("yeucaucuthe")) {
            return "yeucaucuthe";
        }
        if (norm.contains("cancuphaply")) {
            return "cancuphaply";
        }
        if (norm.contains("thoidiemphatsinh")) {
            return "thoidiemphatsinh";
        }
        return "";
    }

    /**
     * 21 nhãn "Trường lỗi" hợp lệ cho ca âm — nguồn DUY NHẤT cho validate khai báo case
     * ({@code CaseFileSource}) và dropdown trên dashboard ({@code CaseEditorServer}). Mỗi nhãn phải
     * khớp đúng {@link #normalizeNegativeFieldKey} — đổi 1 bên mà quên đổi bên kia thì nhãn liệt kê
     * ở đây sẽ "hợp lệ" trên form nhưng lại rơi vào {@code default} lúc chạy thật.
     */
    public static final List<String> TRUONG_LOI_HOP_LE = List.of(
            "Số điện thoại", "Số điện thoại (Bị đơn)",
            "Email", "Email (Bị đơn)",
            "CCCD", "CCCD (Bị đơn)",
            "Ngày sinh",
            "Ngày cấp",
            "Họ tên", "Họ tên (Bị đơn)",
            "Mã số thuế", "Mã số thuế (Bị đơn)",
            "Giá trị tranh chấp",
            "Giới tính",
            "Địa chỉ thường trú",
            "Nghề nghiệp (Bị đơn)",
            "Nơi ở hiện tại (Bị đơn)",
            "Tóm tắt quá trình",
            "Yêu cầu cụ thể",
            "Căn cứ pháp lý",
            "Thời điểm phát sinh"
    );

    /** true nếu {@code truongLoiRaw} khớp 1 trong các trường ca âm hệ thống nhận diện được. */
    public static boolean isKnownNegativeField(String truongLoiRaw) {
        return !normalizeNegativeFieldKey(truongLoiRaw).isEmpty();
    }

    private static RowSelections buildConfiguredSelections(
            vn.tuphap.automation.config.RunFlowConfig.CaseProfile p, int index) {
        RowSelections s = new RowSelections();
        s.loaiDon = resolveLoaiDon(p.loaiDon());
        s.loaiViec = resolveLoaiViec(s.loaiDon, p.loaiViec());
        s.toaAn = resolveToaAn(p.toaAn(), index);
        s.loaiChuTheNguyenDon = resolveChuTheNguyenDon(p.chuThe());
        s.gioiTinh = DataDictionary.pick(MasterDataCatalog.getGioiTinh(), index);
        s.noiCap = DataDictionary.pick(MasterDataCatalog.getNoiCapCccd(), index);
        s.loaiHinhToChuc = DataDictionary.pick(MasterDataCatalog.getLoaiHinhToChuc(), index);
        s.coNguoiDaiDien = coKhong(p.coNguoiDaiDien(), false);
        s.quanHeDaiDien = DataDictionary.pick(MasterDataCatalog.getQuanHeDaiDien(), index);
        s.loaiChuTheBiDon = DataDictionary.pick(MasterDataCatalog.getLoaiChuTheBiDon(), index);
        s.loaiHinhBiDon = DataDictionary.pick(MasterDataCatalog.getLoaiHinhToChuc(), index + 1);
        s.coNguoiLienQuan = coKhong(p.coNguoiLienQuan(), false);
        s.coTaiLieuBoSung = coKhong(p.coTaiLieuBoSung(), false);
        s.soLuongBiDon = resolveSoLuongBiDon(p.soLuongBiDon(), s.loaiDon, s.loaiViec);
        s.coDongNguyenDon = resolveCoDongNguyenDon(p.coDongNguyenDon(), s.loaiDon);
        if (DataDictionary.isPhaSan(s.loaiDon)) {
            s.tuCachNopDon = resolveTuCach(p.tuCachNopDon(), index);
        }
        return s;
    }

    /** Tòa án theo cấu hình (khớp mờ với catalog); rỗng → xoay vòng theo index như trước. */
    private static String resolveToaAn(String raw, int index) {
        String[] opts = MasterDataCatalog.getToaAn();
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) {
            return DataDictionary.pick(opts, index);
        }
        String want = raw.trim();
        for (String opt : opts) {
            if (opt.equalsIgnoreCase(want)) {
                return opt;
            }
        }
        String wantLower = want.toLowerCase(Locale.ROOT);
        for (String opt : opts) {
            String optLower = opt.toLowerCase(Locale.ROOT);
            if (optLower.contains(wantLower) || wantLower.contains(optLower)) {
                return opt;
            }
        }
        MasterDataCatalog.assertInCatalog(want, "toaAn", opts);
        return want;
    }

    /** Số bị đơn theo cấu hình (1–2); 0/không hợp lệ → 1. Ép 1 khi UI không cho thêm bị đơn. */
    private static int resolveSoLuongBiDon(int configured, String loaiDon, String loaiViec) {
        int n = configured <= 0 ? 1 : Math.min(2, configured);
        if (n > 1 && !DataDictionary.allowsThemBiDon(loaiDon, loaiViec)) {
            System.out.println("   ↳ " + loaiDon + " / " + loaiViec
                    + " không cho thêm bị đơn — ép về 1 bị đơn.");
            return 1;
        }
        return n;
    }

    /** Đồng nguyên đơn theo cấu hình; loại đơn không hỗ trợ thì luôn "Không". */
    private static String resolveCoDongNguyenDon(Boolean configured, String loaiDon) {
        if (!DataDictionary.allowsDongNguyenDon(loaiDon)) {
            if (Boolean.TRUE.equals(configured)) {
                System.out.println("   ↳ " + loaiDon + " không có đồng nguyên đơn — bỏ qua cấu hình \"Có\".");
            }
            return "Không";
        }
        return coKhong(configured, false);
    }

    /** {@code null} = automation tự chọn (dùng {@code fallback}). */
    private static String coKhong(Boolean value, boolean fallback) {
        boolean on = value == null ? fallback : value;
        return on ? "Có" : "Không";
    }

    private static String resolveLoaiDon(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Thiếu loại đơn trong run.cases");
        }
        String want = raw.trim();
        for (String opt : MasterDataCatalog.getLoaiDon()) {
            if (opt.equalsIgnoreCase(want) || opt.toLowerCase(Locale.ROOT).contains(want.toLowerCase(Locale.ROOT))) {
                return opt;
            }
        }
        MasterDataCatalog.assertInCatalog(want, "loaiDon", MasterDataCatalog.getLoaiDon());
        return want;
    }

    private static String resolveLoaiViec(String loaiDon, String raw) {
        if (DataDictionary.isPhaSan(loaiDon)) {
            return DataDictionary.PHA_SAN_LOAI_VIEC_MAC_DINH;
        }
        String[] viecs = MasterDataCatalog.getLoaiViecByLoaiDon(loaiDon);
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) {
            return viecs[0];
        }
        String want = raw.trim();
        for (String v : viecs) {
            if (v.equalsIgnoreCase(want) || v.toLowerCase(Locale.ROOT).contains(want.toLowerCase(Locale.ROOT))) {
                return v;
            }
        }
        MasterDataCatalog.assertInCatalog(want, "loaiViec", viecs);
        return want;
    }

    private static String resolveChuTheNguyenDon(String raw) {
        String[] opts = MasterDataCatalog.getLoaiChuTheNguyenDon();
        boolean wantOrg = raw != null && (raw.toLowerCase(Locale.ROOT).contains("tổ chức")
                || raw.toLowerCase(Locale.ROOT).contains("doanh nghiệp")
                || "tc".equalsIgnoreCase(raw.trim()));
        for (String opt : opts) {
            boolean isOrg = DataDictionary.isToChuc(opt);
            if (wantOrg == isOrg) {
                return opt;
            }
        }
        return opts[0];
    }

    private static String resolveTuCach(String raw, int index) {
        String[] opts = MasterDataCatalog.getTuCachNopDonPhaSan();
        if (raw == null || raw.isBlank() || "-".equals(raw.trim())) {
            return DataDictionary.pick(opts, index);
        }
        String want = raw.trim();
        for (String opt : opts) {
            if (opt.equalsIgnoreCase(want) || opt.toLowerCase(Locale.ROOT).contains(want.toLowerCase(Locale.ROOT))) {
                return opt;
            }
        }
        MasterDataCatalog.assertInCatalog(want, "tuCachNopDonPhaSan", opts);
        return want;
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

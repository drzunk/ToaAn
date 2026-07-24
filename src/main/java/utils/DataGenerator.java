package utils;

import net.datafaker.Faker;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

public class DataGenerator {

    private static final long FAKER_SEED = 20240724L;

    public static Object[][] generateFullCoverageData() {
        int rowCount = MasterDataCatalog.getMinimumCoverageRowCount();
        Object[][] data = new Object[rowCount][50];

        for (int i = 0; i < rowCount; i++) {
            Faker faker = createSeededFaker(i);
            RowSelections selections = buildCoverageSelections(i);
            validateSelections(selections); // Fix F: Validate sớm
            data[i] = buildRow(i, selections, faker);
        }
        return data;
    }

    public static Object[][] generateDynamicData(int soLuongKichBan) {
        Object[][] data = new Object[soLuongKichBan][50];
        Faker faker = new Faker(new Locale("vi"));

        for (int i = 0; i < soLuongKichBan; i++) {
            RowSelections selections = buildRandomSelections(faker);
            validateSelections(selections); // Fix F: Validate sớm
            data[i] = buildRow(i, selections, faker);
        }
        return data;
    }

    // Fix F: Chốt chặn kiểm tra lỗi lệch Master Data trước khi đẩy vào Test
    // 🚀 CHỐT CHẶN FAIL-FAST: Quét toàn bộ dữ liệu ngẫu nhiên so với Catalog
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
        if ("Có".equals(s.coNguoiDaiDien)) {
            MasterDataCatalog.assertInCatalog(s.quanHeDaiDien, "quanHeDaiDien", MasterDataCatalog.getQuanHeDaiDien());
        }

        MasterDataCatalog.assertInCatalog(s.loaiChuTheBiDon, "loaiChuTheBiDon", MasterDataCatalog.getLoaiChuTheBiDon());
        MasterDataCatalog.assertInCatalog(s.coNguoiLienQuan, "coNguoiLienQuan", MasterDataCatalog.getCoKhong());
    }

    private static Object[] buildRow(int index, RowSelections selections, Faker faker) {
        Object[] row = new Object[50];
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        row[0] = String.valueOf(index + 1);
        row[1] = selections.loaiDon;
        row[2] = selections.loaiViec;
        row[3] = selections.toaAn;
        row[4] = faker.lorem().sentence(8);
        row[5] = selections.loaiChuTheNguyenDon;

        if (!DataDictionary.isToChuc(selections.loaiChuTheNguyenDon)) {
            row[6] = faker.name().fullName();
            row[7] = sdf.format(faker.date().birthday(18, 60));
            row[8] = selections.gioiTinh;
            row[9] = faker.number().digits(12);
            row[10] = sdf.format(faker.date().past(1500, java.util.concurrent.TimeUnit.DAYS));
            row[11] = selections.noiCap;
            row[12] = faker.address().fullAddress();
            row[13] = faker.bool().bool() ? "Giống thường trú" : faker.address().fullAddress();
            clearFields(row, 14, 19);
        } else {
            clearFields(row, 6, 13);
            row[14] = "Công ty " + faker.company().name();
            row[15] = selections.loaiHinhToChuc;
            row[16] = faker.number().digits(10);
            row[17] = faker.address().fullAddress();
            row[18] = faker.name().fullName();
            row[19] = "Giám đốc";
        }

        row[20] = "09" + faker.number().digits(8);
        row[21] = faker.internet().emailAddress();
        row[22] = selections.coNguoiDaiDien;

        if ("Có".equals(selections.coNguoiDaiDien)) {
            row[23] = faker.name().fullName();
            row[24] = selections.quanHeDaiDien;
        } else {
            row[23] = "";
            row[24] = "";
        }

        row[25] = selections.loaiChuTheBiDon;

        if (!DataDictionary.isToChuc(selections.loaiChuTheBiDon)) {
            row[26] = faker.name().fullName();
            row[27] = faker.number().digits(12);
            row[28] = String.valueOf(faker.number().numberBetween(1960, 2000));
            row[29] = faker.address().fullAddress();
            clearFields(row, 30, 34);
        } else {
            clearFields(row, 26, 29);
            row[30] = "Công ty " + faker.company().name();
            row[31] = selections.loaiHinhBiDon;
            row[32] = faker.number().digits(10);
            row[33] = faker.address().fullAddress();
            row[34] = faker.name().fullName();
        }

        row[35] = "09" + faker.number().digits(8);
        row[36] = faker.internet().emailAddress();

        row[37] = selections.coNguoiLienQuan;
        if ("Có".equals(selections.coNguoiLienQuan)) {
            row[38] = faker.name().fullName();
            row[39] = faker.lorem().sentence(6);
            row[40] = "09" + faker.number().digits(8);
        } else {
            clearFields(row, 38, 40);
        }

        if (DataDictionary.isHanhChinh(selections.loaiDon)) {
            row[41] = "UBND " + faker.address().cityName();
            row[42] = "Chủ tịch UBND";
            row[43] = faker.name().fullName();
            // Fix B: Gán cứng địa chỉ (cột 33) cho cơ quan Hành chính để tránh rỗng
            row[33] = faker.address().fullAddress();
        } else {
            clearFields(row, 41, 43);
        }

        // =================== BƯỚC 4: NỘI DUNG ĐƠN ===================
        row[44] = sdf.format(faker.date().past(800, java.util.concurrent.TimeUnit.DAYS));
        if (DataDictionary.hasGiaTriTranhChap(selections.loaiDon)) {
            row[45] = String.valueOf(faker.number().numberBetween(5_000_000L, 2_000_000_000L));
        } else {
            row[45] = "";
        }
        row[46] = longText(faker, 100);
        row[47] = faker.lorem().sentence(12);
        row[48] = faker.bool().bool() ? "Điều " + faker.number().numberBetween(1, 500)
                + " Bộ luật Dân sự năm 2015" : "";
        row[49] = selections.coTaiLieuBoSung;

        return row;
    }

    private static String longText(Faker faker, int minChars) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < minChars) {
            sb.append(faker.lorem().sentence(10)).append(' ');
        }
        return sb.toString().trim();
    }

    private static void clearFields(Object[] row, int fromInclusive, int toInclusive) {
        for (int i = fromInclusive; i <= toInclusive; i++) {
            row[i] = "";
        }
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
    }

    private static RowSelections buildCoverageSelections(int i) {
        RowSelections s = new RowSelections();
        String[] pair = DataDictionary.pick(MasterDataCatalog.getLoaiDonViecPairs(), i).split(">");
        s.loaiDon = pair[0];
        s.loaiViec = pair[1];
        s.toaAn = DataDictionary.pick(DataDictionary.getToaAn(), i);
        s.loaiChuTheNguyenDon = DataDictionary.pick(DataDictionary.getLoaiChuTheNguyenDon(), i);
        s.loaiHinhToChuc = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), i);
        s.gioiTinh = DataDictionary.pick(DataDictionary.getGioiTinh(), i);
        s.noiCap = DataDictionary.pick(DataDictionary.getNoiCapCccd(), i);
        s.coNguoiDaiDien = DataDictionary.pick(DataDictionary.getCoKhong(), i);
        s.quanHeDaiDien = DataDictionary.pick(DataDictionary.getQuanHeDaiDien(), i);

        s.loaiChuTheBiDon = DataDictionary.pick(DataDictionary.getLoaiChuTheBiDon(), i / 2);
        s.loaiHinhBiDon = DataDictionary.pick(DataDictionary.getLoaiHinhToChuc(), i + 1);
        s.coNguoiLienQuan = DataDictionary.pick(DataDictionary.getCoKhong(), i + 1);
        s.coTaiLieuBoSung = DataDictionary.pick(DataDictionary.getCoKhong(), i + 2);
        return s;
    }

    private static RowSelections buildRandomSelections(Faker faker) {
        RowSelections s = new RowSelections();
        s.loaiDon = faker.options().option(DataDictionary.getLoaiDon());
        s.loaiViec = faker.options().option(DataDictionary.getLoaiViecByLoaiDon(s.loaiDon));
        s.toaAn = faker.options().option(DataDictionary.getToaAn());
        s.loaiChuTheNguyenDon = faker.options().option(DataDictionary.getLoaiChuTheNguyenDon());
        s.loaiHinhToChuc = faker.options().option(DataDictionary.getLoaiHinhToChuc());
        s.gioiTinh = faker.options().option(DataDictionary.getGioiTinh());
        s.noiCap = faker.options().option(DataDictionary.getNoiCapCccd());
        s.coNguoiDaiDien = faker.options().option(DataDictionary.getCoKhong());
        s.quanHeDaiDien = faker.options().option(DataDictionary.getQuanHeDaiDien());
        s.loaiChuTheBiDon = faker.options().option(DataDictionary.getLoaiChuTheBiDon());
        s.loaiHinhBiDon = faker.options().option(DataDictionary.getLoaiHinhToChuc());
        s.coNguoiLienQuan = faker.options().option(DataDictionary.getCoKhong());
        s.coTaiLieuBoSung = faker.options().option(DataDictionary.getCoKhong());
        return s;
    }
}
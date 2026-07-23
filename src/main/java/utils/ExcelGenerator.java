package utils;

import net.datafaker.Faker;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public class ExcelGenerator {

    public static void generateTestData() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Faker faker = new Faker(new Locale("vi"));

        // 1. TẠO DÒNG TIÊU ĐỀ (HEADER - 41 CỘT)
        // (Để đơn giản hóa, ta có thể ghi các cột bắt buộc hoặc cấu trúc đầy đủ)
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "STT", "Loại đơn", "Loại việc", "Tòa án", "Tóm tắt",
                "Loại chủ thể", "Họ tên", "Ngày sinh", "Giới tính", "CCCD", "Ngày cấp", "Nơi cấp", "Thường trú", "Liên lạc",
                "Tên tổ chức", "Loại hình", "Mã số thuế", "Địa chỉ trụ sở", "Đại diện", "Chức vụ",
                "SĐT", "Email", "Có ĐD", "Tên ĐD", "Quan hệ",
                "Loại BĐ", "Họ tên BĐ", "CCCD BĐ", "Năm sinh BĐ", "Địa chỉ BĐ",
                "Tên tổ chức BĐ", "Loại hình BĐ", "MST BĐ", "Địa chỉ trụ sở BĐ", "Đại diện BĐ",
                "SĐT BĐ", "Email BĐ", "Có NLQ", "Họ tên NLQ", "Lý do NLQ", "Liên lạc NLQ"
        };
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // 2. TẠO DÒNG DỮ LIỆU SỐ 1: LUỒNG CÁ NHÂN
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("1");
        row1.createCell(1).setCellValue("Dân sự");
        row1.createCell(2).setCellValue("Hợp đồng dân sự");
        row1.createCell(3).setCellValue("Tòa án nhân dân tỉnh Sơn La");
        row1.createCell(4).setCellValue("Tranh chấp hợp đồng vay tài sản tự động sinh");
        row1.createCell(5).setCellValue("Cá nhân"); // Loại chủ thể

        // Data Cá nhân giả lập bằng Faker
        row1.createCell(6).setCellValue(faker.name().fullName()); // Họ tên
        row1.createCell(7).setCellValue("15/08/1990"); // Ngày sinh
        row1.createCell(8).setCellValue("Nam"); // Giới tính
        row1.createCell(9).setCellValue(faker.number().digits(12)); // CCCD
        row1.createCell(10).setCellValue("10/05/2018");
        row1.createCell(11).setCellValue("Cục Cảnh sát QLHC");
        row1.createCell(12).setCellValue(faker.address().fullAddress());
        row1.createCell(13).setCellValue("Giống thường trú");

        // Phần tổ chức bỏ trống (Cột 14 đến 19)
        // ... (Các cột dùng chung ở cuối)
        row1.createCell(20).setCellValue("09" + faker.number().digits(8)); // SĐT
        row1.createCell(21).setCellValue(faker.internet().emailAddress()); // Email
        row1.createCell(22).setCellValue("Không"); // Có ĐD ủy quyền

        // Bị đơn (Cá nhân)
        row1.createCell(25).setCellValue("Cá nhân");
        row1.createCell(26).setCellValue(faker.name().fullName());
        row1.createCell(27).setCellValue(faker.number().digits(12));
        row1.createCell(28).setCellValue("1995");
        row1.createCell(29).setCellValue(faker.address().fullAddress());
        row1.createCell(35).setCellValue("09" + faker.number().digits(8));
        row1.createCell(36).setCellValue(faker.internet().emailAddress());
        row1.createCell(37).setCellValue("Không"); // Người liên quan

        // 3. XUẤT FILE RA THƯ MỤC RESOURCES
        try {
            String filePath = "src/test/resources/TestData/DuLieuTaoDon.xlsx";
            FileOutputStream fileOut = new FileOutputStream(filePath);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            System.out.println("✅ Máy đẻ Excel đã tạo xong file test data mới tinh!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

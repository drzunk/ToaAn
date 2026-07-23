package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelHelper {

    // Trả về mảng 2 chiều Object[][] để tương thích với @DataProvider của TestNG
    public static Object[][] getExcelData(String filePath, String sheetName) {
        Object[][] data = null;
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("Không tìm thấy sheet: " + sheetName);
            }

            int rowCount = sheet.getLastRowNum(); // Số dòng (không tính dòng 0 header)
            int colCount = sheet.getRow(0).getLastCellNum(); // Số cột

            data = new Object[rowCount][colCount];

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue; // Bỏ qua dòng trống

                for (int j = 0; j < colCount; j++) {
                    Cell cell = row.getCell(j, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                    // Ép kiểu mọi định dạng ô Excel về kiểu String
                    cell.setCellType(CellType.STRING);
                    data[i - 1][j] = cell.getStringCellValue();
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi đọc file Excel: " + e.getMessage());
        }
        return data;
    }
}
package utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static Properties properties;

    static {
        try {
            // TUYỆT CHIÊU: Đọc thẳng từ Classpath, không cần quan tâm đường dẫn src/...
            InputStream stream = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties");

            if (stream == null) {
                throw new RuntimeException("❌ Không tìm thấy file config.properties trong thư mục resources!");
            }

            properties = new Properties();
            properties.load(stream);
            stream.close();
        } catch (Exception e) {
            System.out.println("❌ LỖI NGHIÊM TRỌNG: KHÔNG THỂ NẠP FILE CONFIG!");
            e.printStackTrace();
            throw new RuntimeException("Cấu hình thất bại: " + e.getMessage());
        }
    }

    public static String getValue(String key) {
        if (properties == null) {
            throw new RuntimeException("Lỗi: Không thể lấy giá trị [" + key + "] vì file config chưa được nạp!");
        }

        String value = properties.getProperty(key);
        if (value == null) {
            System.out.println("⚠️ Cảnh báo: Không tìm thấy key [" + key + "] trong file config.properties");
        }
        return value;
    }
}

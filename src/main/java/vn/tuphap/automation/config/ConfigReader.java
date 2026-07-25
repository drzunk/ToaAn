package vn.tuphap.automation.config;

import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Đọc cấu hình theo thứ tự ưu tiên:
 * 1) Biến môi trường (TOAAN_USERNAME, TOAAN_PASSWORD, TOAAN_BASE_URL, …)
 * 2) System property (-Dusername=…)
 * 3) config.properties trên classpath
 */
public class ConfigReader {
    private static final Properties PROPERTIES = new Properties();
    private static final boolean CONFIG_LOADED;

    static {
        boolean loaded = false;
        try (InputStream stream = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (stream != null) {
                PROPERTIES.load(stream);
                loaded = true;
            } else {
                System.out.println("⚠ Không tìm thấy config.properties — dùng env / system property.");
            }
        } catch (Exception e) {
            System.out.println("⚠ Không nạp được config.properties: " + e.getMessage());
        }
        CONFIG_LOADED = loaded;
    }

    private ConfigReader() {
    }

    public static String getValue(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cấu hình không được rỗng.");
        }

        String envKey = toEnvKey(key);
        String fromEnv = System.getenv(envKey);
        if (hasText(fromEnv)) {
            return fromEnv.trim();
        }

        String fromSys = System.getProperty(key);
        if (hasText(fromSys)) {
            return fromSys.trim();
        }

        String fromFile = PROPERTIES.getProperty(key);
        if (hasText(fromFile)) {
            return fromFile.trim();
        }

        String hint = CONFIG_LOADED
                ? "thiếu key [" + key + "] trong config.properties / env " + envKey
                : "tạo config.properties từ config.example.properties hoặc set env " + envKey;
        throw new RuntimeException("❌ Không tìm thấy cấu hình [" + key + "] — " + hint);
    }

    public static String getValue(String key, String defaultValue) {
        try {
            return getValue(key);
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static String toEnvKey(String key) {
        return "TOAAN_" + key.trim().replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

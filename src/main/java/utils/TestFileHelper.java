package utils;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

public final class TestFileHelper {

    private TestFileHelper() {
    }

    public static String getSamplePdf() {
        return resolveResource("TestData/sample.pdf");
    }

    public static String getSamplePng() {
        return resolveResource("TestData/sample.png");
    }

    private static String resolveResource(String resourcePath) {
        URL url = TestFileHelper.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new RuntimeException("❌ Không tìm thấy file test trong classpath: " + resourcePath);
        }
        try {
            return Paths.get(url.toURI()).toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException("❌ Không đọc được đường dẫn file test: " + resourcePath, e);
        }
    }

    public static void assertExists(String absolutePath) {
        if (absolutePath == null || !new File(absolutePath).exists()) {
            throw new RuntimeException("❌ File test không tồn tại: " + absolutePath);
        }
    }
}

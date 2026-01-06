package common.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Base64;

public class FileUtil {

    // 1. Biến File thành chuỗi Base64 (Để gửi đi)
    public static String encodeFileToBase64(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 2. Biến chuỗi Base64 thành File (Để lưu về máy)
    public static boolean decodeBase64ToFile(String base64String, String targetFilePath) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            try (FileOutputStream fos = new FileOutputStream(targetFilePath)) {
                fos.write(decodedBytes);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
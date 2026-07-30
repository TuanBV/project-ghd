package guru.springframework.ghd.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import java.util.*;
import java.text.Normalizer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class CommonUtil {
    public static boolean isNumber(String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        int len = value.length();
        boolean check = true;
        char target = 0;
        for (int i = 0; i < len; i++) {
            target = value.charAt(i);
            if (Objects.equals(i, 0) && Objects.equals(target, '-')) {
                // 先頭がマイナスだった場合はチェックしない
            } else {
                check = Character.isDigit(target);
            }
            if (!check) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNullNumber(Integer i) {
        return i == null || i.intValue() <= 0;
    }

    public static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return (collection == null || collection.isEmpty());
    }

    public static boolean isEmpty(Object[] array) {
        return (array == null || array.length == 0);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return (map == null || map.isEmpty());
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static String generateSlug(String input) {
        if (input == null || input.trim().isEmpty()) return "";

        String normalized = input.toLowerCase()
                .replaceAll("[áàảãạâấầẩẫậăắằẳẵặ]", "a")
                .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                .replaceAll("[íìỉĩị]", "i")
                .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                .replaceAll("[úùủũụưứừửữự]", "u")
                .replaceAll("[ýỳỷỹỵ]", "y")
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "") // Xóa ký tự đặc biệt
                .replaceAll("\\s+", "-")         // Thay khoảng trắng bằng -
                .replaceAll("-+", "-")           // Gộp nhiều dấu - thành 1
                .replaceAll("^-+|-+$", "")       // Xóa dấu - ở đầu và cuối
                .trim();

        return normalized;
    }

    public static String getCleanDescription(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "";
        }

        // 1. Loại bỏ tất cả các thẻ HTML, chỉ giữ lại văn bản thuần túy
        String cleanText = Jsoup.clean(htmlContent, Safelist.none());

        // 2. Giải mã các thực thể HTML (vd: &nbsp; thành khoảng trắng, &gt; thành >)
        // Jsoup.clean thường đã làm một phần, nhưng đảm bảo hơn bằng cách:
        cleanText = Jsoup.parse(cleanText).text();

        // 3. Xử lý các ký tự đặc biệt để an toàn cho JSON Schema
        // Xóa dấu ngoặc kép " để tránh vỡ chuỗi JSON và các ký tự xuống dòng
        cleanText = cleanText.replace("\"", "'")  // Chuyển ngoặc kép thành ngoặc đơn
                .replace("\n", " ")  // Thay xuống dòng bằng khoảng trắng
                .replace("\r", "")
                .trim();

        return cleanText;
    }

    public static String safeSubstringByWord(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        int end = text.lastIndexOf(" ", maxLength);
        if (end == -1) {
            return text.substring(0, maxLength); // fallback nếu không có space
        }

        return text.substring(0, end);
    }

    public static String toSlug(String input) {
        if (input == null || input.isEmpty()) return "";
        String slug = input.toLowerCase();
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = slug.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        slug = slug.replace('đ', 'd').replace('Đ', 'd');
        slug = slug.replaceAll("[^a-z0-9\\s]", "");
        slug = slug.replaceAll("\\s+", "-");
        slug = slug.replaceAll("-+", "-");

        return slug.trim();
    }
}

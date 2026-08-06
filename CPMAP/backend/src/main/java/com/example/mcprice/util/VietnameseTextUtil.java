package com.example.mcprice.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class VietnameseTextUtil {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private VietnameseTextUtil() {
    }

    public static String stripDiacritics(String input) {
        if (input == null) {
            return null;
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutMarks = DIACRITICS.matcher(normalized).replaceAll("");
        return withoutMarks.replace('đ', 'd').replace('Đ', 'D');
    }
}

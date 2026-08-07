package com.example.mcprice.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chuan hoa SKU va sinh alias candidate. Quy tac theo yeu cau nghiep vu:
 * - Chi bo khoang trang/gach ngang/underscore (separator khong mang y nghia).
 * - Giu nguyen dau "." vi day co the la hau to phien ban (VD "HPF AD6783.1" != "HPF AD6783").
 * - "SL12C (LC1000C)" sinh hai alias nhung phai o trang thai chua xac nhan (confirmed=false).
 * - Hau to dang ".1"/".2" sinh alias "base" nhung KHONG duoc tu dong coi la trung, confirmed=false.
 */
public final class SkuNormalizer {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-_]+");
    private static final Pattern PARENTHESES = Pattern.compile("\\(([^)]+)\\)");
    private static final Pattern VERSION_SUFFIX = Pattern.compile("^(?<base>[A-Z0-9]{3,}[A-Z0-9.\\-]*[A-Z0-9])\\.(?<suffix>\\d{1,2})$");
    // Chi doi hoi mot day lien tuc chu/so/gach ngang; VIEC co phai SKU hay khong (phai co CA
    // chu lan so, du dai toi thieu) duoc kiem tra rieng o looksLikeSkuToken — KHONG bat buoc
    // ky tu dau tien phai la chu, vi rat nhieu SKU do dien tu bat dau bang so (vd kich thuoc
    // man hinh: "50W660G", "43X8500F") va se bi bo sot neu chi nhan dien token bat dau bang chu.
    private static final Pattern WORD_TOKEN = Pattern.compile("[A-Z0-9][A-Z0-9\\-]*");
    private static final int MIN_SKU_TOKEN_LENGTH = 4;

    private SkuNormalizer() {
    }

    public record AliasCandidate(String rawValue, String normalizedValue, double confidence, boolean confirmed, String reason) {
    }

    /** Chuan hoa SKU: trim, uppercase, bo khoang trang/gach noi khong mang y nghia. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toUpperCase();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        String withoutParens = PARENTHESES.matcher(trimmed).replaceAll("").trim();
        return SEPARATORS.matcher(withoutParens).replaceAll("");
    }

    /** Sinh cac alias tiem nang (ngoac don, hau to phien ban) tu SKU raw. */
    public static List<AliasCandidate> generateAliasCandidates(String raw) {
        List<AliasCandidate> candidates = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return candidates;
        }
        String trimmed = raw.trim().toUpperCase();

        Matcher parenMatcher = PARENTHESES.matcher(trimmed);
        while (parenMatcher.find()) {
            String inner = parenMatcher.group(1).trim();
            String normalizedInner = normalize(inner);
            if (!normalizedInner.isEmpty()) {
                candidates.add(new AliasCandidate(inner, normalizedInner, 0.6, false,
                        "Model thay the trong ngoac don cua SKU '" + raw + "', can duyet thu cong"));
            }
        }

        Matcher suffixMatcher = VERSION_SUFFIX.matcher(normalize(raw));
        if (suffixMatcher.matches()) {
            String base = suffixMatcher.group("base");
            candidates.add(new AliasCandidate(base, base, 0.5, false,
                    "SKU '" + raw + "' co hau to phien ban '." + suffixMatcher.group("suffix")
                            + "', khong duoc tu dong coi la trung voi SKU goc"));
        }

        return candidates;
    }

    /** Tim cac token co dang giong SKU/model ben trong mot chuoi title de phuc vu matching. */
    public static List<String> extractSkuLikeTokens(String title) {
        List<String> tokens = new ArrayList<>();
        if (title == null) {
            return tokens;
        }
        String upper = VietnameseTextUtil.stripDiacritics(title).toUpperCase();
        Matcher matcher = WORD_TOKEN.matcher(upper);
        while (matcher.find()) {
            String word = matcher.group();
            if (looksLikeSkuToken(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    /** Mot token duoc coi la "giong SKU" khi co CA chu lan so (khong phai thuan so nhu "50",
     * cung khong phai thuan chu nhu "TIVI") va du dai toi thieu de tranh trung ngau nhien. */
    private static boolean looksLikeSkuToken(String word) {
        if (word.length() < MIN_SKU_TOKEN_LENGTH) {
            return false;
        }
        boolean hasDigit = false;
        boolean hasLetter = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLetter(c)) {
                hasLetter = true;
            }
        }
        return hasDigit && hasLetter;
    }
}

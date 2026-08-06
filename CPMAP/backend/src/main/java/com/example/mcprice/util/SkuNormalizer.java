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
    private static final Pattern SKU_TOKEN = Pattern.compile("[A-Z][A-Z0-9\\-]{2,}[0-9][A-Z0-9\\-]*");

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
        Matcher matcher = SKU_TOKEN.matcher(upper);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }
}

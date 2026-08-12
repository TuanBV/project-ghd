package com.example.mcprice.util;

import com.example.mcprice.service.CompetitorService;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sinh URL trang chi tiet san pham tren website cua chinh minh (tongkhodienmaymienbac.com) tu ten
 * san pham. Website nay dung WordPress, URL permalink duoc tao dung theo thuat toan
 * sanitize_title_with_dashes() cua WordPress — da doi chieu truc tiep voi cac URL that tren site
 * de dam bao khop tuyet doi, vi du:
 *   "...QA65Q7F5A [65Q7F]"        -> ".../qa65q7f5a-65q7f/"        (dau ngoac vuong bi XOA, khong sinh gach ngang)
 *   "Dieu hoa ... 12.000 BTU"     -> ".../dieu-hoa-...-12-000-btu/" (dau cham THAY BANG gach ngang)
 *   "GR-RF665WIA-PGV(22)-XK"      -> ".../gr-rf665wia-pgv22-xk/"    (dau ngoac don bi XOA)
 */
public final class ProductUrlBuilder {

    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-z0-9 _-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-+");
    private static final Pattern LEADING_TRAILING_DASHES = Pattern.compile("^-+|-+$");

    private ProductUrlBuilder() {
    }

    /** Tra ve null neu title rong/blank — khong sinh URL vo nghia tu chuoi trong. */
    public static String buildOwnWebsiteUrl(String title) {
        String slug = slugify(title);
        return slug == null ? null : "https://" + CompetitorService.OWN_WEBSITE_DOMAIN + "/" + slug + "/";
    }

    static String slugify(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String withoutDiacritics = VietnameseTextUtil.stripDiacritics(title);
        String lower = withoutDiacritics.toLowerCase(Locale.ROOT);
        String dotsToDashes = lower.replace('.', '-');
        String withoutInvalidChars = INVALID_CHARS.matcher(dotsToDashes).replaceAll("");
        String dashed = WHITESPACE.matcher(withoutInvalidChars).replaceAll("-");
        String collapsed = MULTIPLE_DASHES.matcher(dashed).replaceAll("-");
        String trimmed = LEADING_TRAILING_DASHES.matcher(collapsed).replaceAll("");
        return trimmed.isBlank() ? null : trimmed;
    }
}

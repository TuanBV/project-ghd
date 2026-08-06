package com.example.mcprice.util;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse cac dang chuoi gia trong feed MC ("3550000 VND") va file so sanh gia (so thuan hoac
 * "Lien he"). Khong bao gio tra ve double/float; ket qua luon la BigDecimal hoac empty.
 */
public final class PriceParser {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?[0-9][0-9.,]*");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Za-z]{3}");

    private PriceParser() {
    }

    public record ParsedPrice(BigDecimal amount, String currency) {
    }

    /** Parse "3550000 VND" -> ParsedPrice(3550000, "VND"). Tra empty neu khong parse duoc so. */
    public static Optional<ParsedPrice> parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        String trimmed = rawText.trim();
        Matcher numberMatcher = NUMBER_PATTERN.matcher(trimmed);
        if (!numberMatcher.find()) {
            return Optional.empty();
        }
        String numberPart = normalizeNumber(numberMatcher.group());
        BigDecimal amount;
        try {
            amount = new BigDecimal(numberPart);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        Matcher currencyMatcher = CURRENCY_PATTERN.matcher(trimmed);
        String currency = currencyMatcher.find() ? currencyMatcher.group().toUpperCase() : "VND";
        return Optional.of(new ParsedPrice(amount, currency));
    }

    /** true neu chuoi la dang khong co gia so ("Lien he", rong, chi chu). */
    public static boolean isContactOnly(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return false;
        }
        String normalized = VietnameseTextUtil.stripDiacritics(rawText.trim()).toLowerCase();
        return normalized.contains("lien he") || normalized.equals("call") || normalized.equals("contact");
    }

    private static String normalizeNumber(String number) {
        // Excel/feed co the dung dau "," hoac "." lam phan ngan chuc nghin -> bo het separator, giu nguyen phan so.
        String cleaned = number.replace(",", "").replace(".", "");
        return cleaned.isEmpty() ? "0" : cleaned;
    }
}

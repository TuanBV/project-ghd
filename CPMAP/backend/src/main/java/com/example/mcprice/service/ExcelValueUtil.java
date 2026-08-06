package com.example.mcprice.service;

final class ExcelValueUtil {

    private ExcelValueUtil() {
    }

    /** Chuyen gia tri cell (co the la Double do POI doc so) thanh String on dinh, khong co ".0" du thua. */
    static String asText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !d.isInfinite()) {
                return String.valueOf(d.longValue());
            }
            return String.valueOf(d);
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    static java.math.BigDecimal asNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double d) {
            return java.math.BigDecimal.valueOf(d);
        }
        try {
            return new java.math.BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

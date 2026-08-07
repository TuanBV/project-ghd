package com.example.mcprice.util;

/** Chuan hoa cac bien the "con hang" khong phan biet hoa/thuong/khoang trang: "In Stock", "in  Stock", "out of Stock", "in"... */
public final class AvailabilityNormalizer {

    public enum Availability {
        IN_STOCK, OUT_OF_STOCK, PREORDER, UNKNOWN
    }

    private AvailabilityNormalizer() {
    }

    public static Availability normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Availability.UNKNOWN;
        }
        // schema.org JSON-LD/microdata tra ve dang "http://schema.org/InStock" (camelCase, khong
        // dau cach) — chuan hoa ve chung mot dang voi text tieng Anh/Viet truoc khi so khop tu khoa.
        String withoutSchemaPrefix = raw.replaceAll("(?i)^https?://schema\\.org/", "");
        // Bo dau tieng Viet TRUOC khi so khop, vi hau het website doi thu ghi thang tieng Viet co
        // dau ("Hết hàng", "Còn hàng", "Đặt trước") thay vi dung schema.org tieng Anh.
        String collapsed = VietnameseTextUtil.stripDiacritics(withoutSchemaPrefix).trim().toLowerCase().replaceAll("\\s+", " ");
        if (collapsed.contains("out of stock") || collapsed.equals("out") || collapsed.contains("outofstock")
                || collapsed.contains("het hang") || collapsed.contains("ngung kinh doanh") || collapsed.contains("tam het hang")) {
            return Availability.OUT_OF_STOCK;
        }
        if (collapsed.contains("preorder") || collapsed.contains("pre order") || collapsed.contains("dat truoc")
                || collapsed.contains("dat hang truoc")) {
            return Availability.PREORDER;
        }
        if (collapsed.contains("in stock") || collapsed.equals("in") || collapsed.contains("con hang")
                || collapsed.contains("instock") || collapsed.contains("san co")) {
            return Availability.IN_STOCK;
        }
        return Availability.UNKNOWN;
    }
}

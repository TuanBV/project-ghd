package com.example.mcprice.adapter;

import com.example.mcprice.domain.ObservationStatus;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.util.AvailabilityNormalizer;
import com.example.mcprice.util.PriceParser;

/**
 * Quy tac chung chuyen (priceText, stockText) thanh CrawlResult — dung chung cho StaticHtmlCrawler
 * va BrowserCrawler de tranh trung logic (va trung ca bug sua) o 2 noi rieng biet.
 */
public final class CrawlResultClassifier {

    private CrawlResultClassifier() {
    }

    public static CrawlResult classify(String priceText, String stockText, Integer httpStatus, String finalUrl) {
        if (priceText == null) {
            return new CrawlResult(true, ObservationStatus.NO_PRICE, null, null, stockText, httpStatus, finalUrl,
                    "Khong tim thay gia theo selector da cau hinh hoac chuan pho bien (JSON-LD/meta/microdata)");
        }
        if (PriceParser.isContactOnly(priceText)) {
            return new CrawlResult(true, ObservationStatus.CONTACT_ONLY, null, priceText, stockText, httpStatus, finalUrl, null);
        }
        var parsed = PriceParser.parse(priceText);
        if (parsed.isEmpty()) {
            return new CrawlResult(true, ObservationStatus.PARSE_ERROR, null, priceText, stockText, httpStatus, finalUrl,
                    "Khong parse duoc gia tu chuoi: " + priceText);
        }
        // Gia doc duoc <= 0 (loi CMS, o hang danh dau gia 0, hoac chua cap nhat) — coi nhu KHONG
        // co gia that, khong tinh vao gia trung binh (giong het "Lien he"), thay vi bao VALID gia 0.
        if (parsed.get().amount().signum() <= 0) {
            return new CrawlResult(true, ObservationStatus.NO_PRICE, null, priceText, stockText, httpStatus, finalUrl,
                    "Gia doc duoc la 0 hoac am, coi nhu chua co gia that");
        }
        var availability = AvailabilityNormalizer.normalize(stockText);
        if (availability == AvailabilityNormalizer.Availability.OUT_OF_STOCK) {
            return new CrawlResult(true, ObservationStatus.OUT_OF_STOCK, parsed.get().amount(), priceText, stockText,
                    httpStatus, finalUrl, null);
        }
        return new CrawlResult(true, ObservationStatus.VALID, parsed.get().amount(), priceText, stockText, httpStatus, finalUrl, null);
    }
}

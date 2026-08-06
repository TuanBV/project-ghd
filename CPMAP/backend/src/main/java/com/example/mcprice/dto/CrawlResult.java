package com.example.mcprice.dto;

import com.example.mcprice.domain.ObservationStatus;
import java.math.BigDecimal;

/** Ket qua mot lan crawl mot listing. Khong bao gio "gia lap" du lieu khi crawl loi. */
public record CrawlResult(
        boolean success,
        ObservationStatus observationStatus,
        BigDecimal price,
        String rawPriceText,
        String availability,
        Integer httpStatus,
        String finalUrl,
        String errorMessage
) {
    public static CrawlResult failure(String errorMessage) {
        return new CrawlResult(false, ObservationStatus.PARSE_ERROR, null, null, null, null, null, errorMessage);
    }

    public static CrawlResult manualRequired(String reason) {
        return new CrawlResult(false, ObservationStatus.NO_PRICE, null, null, null, null, null, reason);
    }
}

package com.example.mcprice.dto;

import com.example.mcprice.domain.MatchMethod;
import com.example.mcprice.domain.MatchStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Ket qua ghep san pham. Luon co phuong phap, diem confidence, ly do va trang thai duyet
 * theo yeu cau nghiep vu — khong bao gio tra ve "match mu quang" khong co giai trinh.
 */
public record MatchResult(
        Long matchedProductId,
        MatchMethod method,
        BigDecimal score,
        String reason,
        MatchStatus status,
        List<Long> conflictingProductIds
) {
    public static MatchResult noMatch(String reason) {
        return new MatchResult(null, MatchMethod.MANUAL, BigDecimal.ZERO, reason, MatchStatus.REVIEW_REQUIRED, List.of());
    }

    public boolean isAutoConfirmable() {
        return status == MatchStatus.AUTO_CONFIRMED;
    }
}

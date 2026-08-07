package com.example.mcprice.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AvailabilityNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "In Stock, IN_STOCK",
            "in Stock, IN_STOCK",
            "'In  Stock', IN_STOCK",
            "in, IN_STOCK",
            "Out of Stock, OUT_OF_STOCK",
            "out of Stock, OUT_OF_STOCK",
            "'', UNKNOWN",
            "http://schema.org/InStock, IN_STOCK",
            "https://schema.org/OutOfStock, OUT_OF_STOCK",
            "InStock, IN_STOCK",
            "Hết hàng, OUT_OF_STOCK",
            "Còn hàng, IN_STOCK",
            "Đặt trước, PREORDER",
    })
    void normalize_handlesRealFeedVariantsCaseAndWhitespaceInsensitively(String raw, String expected) {
        assertThat(AvailabilityNormalizer.normalize(raw).name()).isEqualTo(expected);
    }

    @Test
    void normalize_null_returnsUnknown() {
        assertThat(AvailabilityNormalizer.normalize(null)).isEqualTo(AvailabilityNormalizer.Availability.UNKNOWN);
    }
}

package com.example.mcprice.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.mcprice.domain.ObservationStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CrawlResultClassifierTest {

    @Test
    void classify_validPrice_returnsValid() {
        var result = CrawlResultClassifier.classify("10.690.000", "Con hang", 200, "https://x.com/p");

        assertThat(result.observationStatus()).isEqualTo(ObservationStatus.VALID);
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("10690000"));
    }

    @Test
    void classify_contactOnlyText_returnsContactOnlyWithNullPrice() {
        var result = CrawlResultClassifier.classify("Liên hệ", null, 200, "https://x.com/p");

        assertThat(result.observationStatus()).isEqualTo(ObservationStatus.CONTACT_ONLY);
        assertThat(result.price()).isNull();
    }

    @Test
    void classify_zeroPrice_treatedAsNoPrice_notCountedAsValid() {
        var result = CrawlResultClassifier.classify("0", null, 200, "https://x.com/p");

        assertThat(result.observationStatus()).isEqualTo(ObservationStatus.NO_PRICE);
        assertThat(result.price()).isNull();
    }

    @Test
    void classify_negativePrice_treatedAsNoPrice() {
        var result = CrawlResultClassifier.classify("-5000", null, 200, "https://x.com/p");

        assertThat(result.observationStatus()).isEqualTo(ObservationStatus.NO_PRICE);
    }

    @Test
    void classify_nullPriceText_returnsNoPrice() {
        var result = CrawlResultClassifier.classify(null, null, 200, "https://x.com/p");

        assertThat(result.observationStatus()).isEqualTo(ObservationStatus.NO_PRICE);
    }

    @Test
    void classify_unparsablePriceText_returnsParseError() {
        var result = CrawlResultClassifier.classify("???", null, 200, "https://x.com/p");

        assertThat(result.observationStatus()).isEqualTo(ObservationStatus.PARSE_ERROR);
    }

    @Test
    void classify_outOfStockText_stillCapturesPriceButFlagsOutOfStock() {
        var result = CrawlResultClassifier.classify("10.000.000", "Hết hàng", 200, "https://x.com/p");

        assertThat(result.observationStatus()).isEqualTo(ObservationStatus.OUT_OF_STOCK);
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("10000000"));
    }
}

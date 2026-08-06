package com.example.mcprice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PriceParserTest {

    @Test
    void parse_mcFeedFormat_returnsAmountAndCurrency() {
        var result = PriceParser.parse("3550000 VND");

        assertThat(result).isPresent();
        assertThat(result.get().amount()).isEqualByComparingTo(new BigDecimal("3550000"));
        assertThat(result.get().currency()).isEqualTo("VND");
    }

    @Test
    void parse_withThousandSeparators_stripsThemAsGrouping() {
        var result = PriceParser.parse("5.100.000 VND");

        assertThat(result).isPresent();
        assertThat(result.get().amount()).isEqualByComparingTo(new BigDecimal("5100000"));
    }

    @Test
    void parse_blankOrNull_returnsEmpty() {
        assertThat(PriceParser.parse(null)).isEmpty();
        assertThat(PriceParser.parse("")).isEmpty();
        assertThat(PriceParser.parse("   ")).isEmpty();
    }

    @Test
    void parse_nonNumericText_returnsEmpty() {
        assertThat(PriceParser.parse("Lien he")).isEmpty();
    }

    @Test
    void isContactOnly_detectsLienHeRegardlessOfDiacriticsOrCase() {
        assertThat(PriceParser.isContactOnly("Liên hệ")).isTrue();
        assertThat(PriceParser.isContactOnly("lien he")).isTrue();
        assertThat(PriceParser.isContactOnly("LIEN HE")).isTrue();
        assertThat(PriceParser.isContactOnly("3550000 VND")).isFalse();
    }

    @Test
    void parse_zeroPrice_isStillParsedAsZero_callerDecidesMeaning() {
        // Gia Min = 0 nghia la "khong co gia", nhung viec dien giai do thuoc ve importer,
        // khong phai PriceParser — PriceParser chi parse dung con so xuat hien trong chuoi.
        var result = PriceParser.parse("0");

        assertThat(result).isPresent();
        assertThat(result.get().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}

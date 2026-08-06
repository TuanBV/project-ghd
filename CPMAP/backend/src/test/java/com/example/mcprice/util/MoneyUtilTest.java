package com.example.mcprice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoneyUtilTest {

    @Test
    void average_computesArithmeticMeanUsingBigDecimal() {
        BigDecimal avg = MoneyUtil.average(List.of(
                new BigDecimal("5100000"), new BigDecimal("4950000")));

        assertThat(avg).isEqualByComparingTo(new BigDecimal("5025000"));
    }

    @Test
    void average_emptyList_returnsNull() {
        assertThat(MoneyUtil.average(List.of())).isNull();
        assertThat(MoneyUtil.average(null)).isNull();
    }

    @Test
    void roundToStep_roundsHalfUpToNearestStep() {
        assertThat(MoneyUtil.roundToStep(new BigDecimal("6575000"), new BigDecimal("10000")))
                .isEqualByComparingTo(new BigDecimal("6580000"));
        assertThat(MoneyUtil.roundToStep(new BigDecimal("6574000"), new BigDecimal("10000")))
                .isEqualByComparingTo(new BigDecimal("6570000"));
    }

    @Test
    void roundToStep_zeroOrNullStep_fallsBackToIntegerRounding() {
        assertThat(MoneyUtil.roundToStep(new BigDecimal("100.5"), BigDecimal.ZERO))
                .isEqualByComparingTo(new BigDecimal("101"));
    }

    @Test
    void percentChange_computesRelativeDifference() {
        BigDecimal change = MoneyUtil.percentChange(new BigDecimal("10000000"), new BigDecimal("12000000"));

        assertThat(change).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void percentChange_zeroOrNegativeOldValue_returnsNull() {
        assertThat(MoneyUtil.percentChange(BigDecimal.ZERO, new BigDecimal("100"))).isNull();
        assertThat(MoneyUtil.percentChange(null, new BigDecimal("100"))).isNull();
    }

    @Test
    void isPositive_checksSignumCorrectly() {
        assertThat(MoneyUtil.isPositive(new BigDecimal("1"))).isTrue();
        assertThat(MoneyUtil.isPositive(BigDecimal.ZERO)).isFalse();
        assertThat(MoneyUtil.isPositive(null)).isFalse();
    }
}

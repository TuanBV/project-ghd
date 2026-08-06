package com.example.mcprice.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConditionNormalizerTest {

    @Test
    void normalize_isCaseInsensitive() {
        assertThat(ConditionNormalizer.normalize("New")).isEqualTo(ConditionNormalizer.Condition.NEW);
        assertThat(ConditionNormalizer.normalize("new")).isEqualTo(ConditionNormalizer.Condition.NEW);
    }

    @Test
    void normalize_unknownOrBlank_returnsUnknown() {
        assertThat(ConditionNormalizer.normalize(null)).isEqualTo(ConditionNormalizer.Condition.UNKNOWN);
        assertThat(ConditionNormalizer.normalize("")).isEqualTo(ConditionNormalizer.Condition.UNKNOWN);
        assertThat(ConditionNormalizer.normalize("weird-value")).isEqualTo(ConditionNormalizer.Condition.UNKNOWN);
    }
}

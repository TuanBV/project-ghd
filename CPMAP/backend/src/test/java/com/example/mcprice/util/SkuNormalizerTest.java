package com.example.mcprice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SkuNormalizerTest {

    @Test
    void normalize_trimsUppercasesAndRemovesInsignificantSeparators() {
        assertThat(SkuNormalizer.normalize(" fr-132ci ")).isEqualTo("FR132CI");
        assertThat(SkuNormalizer.normalize("FR 132CI")).isEqualTo("FR132CI");
        assertThat(SkuNormalizer.normalize("FR132CI")).isEqualTo("FR132CI");
    }

    @Test
    void normalize_keepsDotBecauseItCanBeAMeaningfulVersionSuffix() {
        assertThat(SkuNormalizer.normalize("HPF AD6783.1")).isEqualTo("HPFAD6783.1");
        assertThat(SkuNormalizer.normalize("HPF AD6783")).isEqualTo("HPFAD6783");
    }

    @Test
    void normalize_stripsParenthesesContent() {
        assertThat(SkuNormalizer.normalize("SL12C (LC1000C)")).isEqualTo("SL12C");
    }

    @Test
    void generateAliasCandidates_forSkuWithParentheses_createsUnconfirmedAlias() {
        List<SkuNormalizer.AliasCandidate> candidates = SkuNormalizer.generateAliasCandidates("SL12C (LC1000C)");

        assertThat(candidates).hasSize(1);
        SkuNormalizer.AliasCandidate candidate = candidates.get(0);
        assertThat(candidate.normalizedValue()).isEqualTo("LC1000C");
        assertThat(candidate.confirmed()).isFalse();
    }

    @Test
    void generateAliasCandidates_forVersionSuffix_doesNotAutoConfirmBaseSku() {
        List<SkuNormalizer.AliasCandidate> candidates = SkuNormalizer.generateAliasCandidates("HPF AD6783.1");

        assertThat(candidates).hasSize(1);
        SkuNormalizer.AliasCandidate candidate = candidates.get(0);
        assertThat(candidate.normalizedValue()).isEqualTo("HPFAD6783");
        assertThat(candidate.confirmed()).isFalse();
    }

    @Test
    void generateAliasCandidates_forPlainSku_producesNoCandidates() {
        assertThat(SkuNormalizer.generateAliasCandidates("AQT32K85FX")).isEmpty();
    }

    @Test
    void extractSkuLikeTokens_findsCandidateModelCodesInsideTitle() {
        List<String> tokens = SkuNormalizer.extractSkuLikeTokens("Tu lanh Funiki FR-132CI FR132CI FR 132CI tu mini");

        assertThat(tokens).isNotEmpty();
        assertThat(tokens.stream().anyMatch(t -> SkuNormalizer.normalize(t).equals("FR132CI"))).isTrue();
    }

    @Test
    void extractSkuLikeTokens_findsModelCodesThatStartWithADigit() {
        // Rat pho bien o do dien tu: SKU bat dau bang kich thuoc man hinh (vd TV Sony 50W660G,
        // 43X8500F) — tung la bug that: regex cu bat buoc ky tu dau la chu nen bo sot toan bo
        // nhom SKU nay khi tach tu URL slug (vd ".../kdl-50w660g" sau khi doi "-" thanh khoang
        // trang thi "50w660g" dung mot minh, khong con dinh lien voi "kdl" nua).
        List<String> tokens = SkuNormalizer.extractSkuLikeTokens("smart tivi sony 50 inch kdl 50w660g");

        assertThat(tokens.stream().anyMatch(t -> SkuNormalizer.normalize(t).equals("50W660G"))).isTrue();
    }

    @Test
    void extractSkuLikeTokens_ignoresPureNumbersAndPureWords() {
        List<String> tokens = SkuNormalizer.extractSkuLikeTokens("smart tivi sony 50 inch");

        assertThat(tokens).isEmpty();
    }

    @Test
    void extractSkuLikeTokens_ignoresTokensShorterThanMinimumLength() {
        // "A8F" (do dai 3) qua ngan de coi la SKU dang tin cay — tranh trung ngau nhien.
        List<String> tokens = SkuNormalizer.extractSkuLikeTokens("tivi a8f");

        assertThat(tokens).isEmpty();
    }
}

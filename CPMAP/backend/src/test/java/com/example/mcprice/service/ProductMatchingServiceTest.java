package com.example.mcprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.mcprice.domain.MatchMethod;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.dto.MatchCandidate;
import com.example.mcprice.dto.MatchResult;
import com.example.mcprice.domain.Product;
import com.example.mcprice.domain.ProductAlias;
import com.example.mcprice.repository.ProductAliasRepository;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductMatchingServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductAliasRepository productAliasRepository;

    private ProductMatchingService service;

    @BeforeEach
    void setUp() {
        service = new ProductMatchingService(productRepository, productAliasRepository);
    }

    private Product productWithId(Long id) {
        Product p = new Product();
        p.setId(id);
        return p;
    }

    @Test
    void match_exactNormalizedSku_isAutoConfirmedWithHighestPriority() {
        when(productRepository.findAllBySkuNormalized("AQT32K85FX")).thenReturn(List.of(productWithId(1L)));

        MatchResult result = service.match(new MatchCandidate("AQT32K85FX", "Google Tivi Aqua 32 inch", null));

        assertThat(result.method()).isEqualTo(MatchMethod.EXACT_NORMALIZED_SKU);
        assertThat(result.status()).isEqualTo(MatchStatus.AUTO_CONFIRMED);
        assertThat(result.matchedProductId()).isEqualTo(1L);
        assertThat(result.score()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void match_confirmedAlias_isAutoConfirmedWhenNoExactSkuMatch() {
        when(productRepository.findAllBySkuNormalized("LC1000C")).thenReturn(List.of());
        Product aliasOwner = productWithId(2L);
        ProductAlias alias = ProductAlias.builder().product(aliasOwner).build();
        when(productAliasRepository.findFirstByAliasNormalizedAndConfirmedTrue("LC1000C")).thenReturn(Optional.of(alias));

        MatchResult result = service.match(new MatchCandidate("LC1000C", "Model thay the", null));

        assertThat(result.method()).isEqualTo(MatchMethod.CONFIRMED_ALIAS);
        assertThat(result.status()).isEqualTo(MatchStatus.AUTO_CONFIRMED);
        assertThat(result.matchedProductId()).isEqualTo(2L);
    }

    @Test
    void match_skuFoundInTitle_isAutoConfirmedWhenTokenIsLongEnoughToBeConfident() {
        when(productRepository.findAllBySkuNormalized("NOSKU")).thenReturn(List.of());
        when(productAliasRepository.findFirstByAliasNormalizedAndConfirmedTrue("NOSKU")).thenReturn(Optional.empty());
        when(productRepository.findAllBySkuNormalized("AQT32K85FX")).thenReturn(List.of(productWithId(3L)));

        MatchResult result = service.match(new MatchCandidate("NOSKU", "Google Tivi Aqua 32 inch AQT32K85FX gia tot", null));

        assertThat(result.method()).isEqualTo(MatchMethod.SKU_IN_TITLE);
        assertThat(result.status()).isEqualTo(MatchStatus.AUTO_CONFIRMED);
        assertThat(result.matchedProductId()).isEqualTo(3L);
    }

    @Test
    void match_multipleDifferentSkusFoundInTitle_isReviewRequiredNotAutoMerged() {
        when(productRepository.findAllBySkuNormalized("NONE")).thenReturn(List.of());
        when(productAliasRepository.findFirstByAliasNormalizedAndConfirmedTrue("NONE")).thenReturn(Optional.empty());
        when(productRepository.findAllBySkuNormalized("MODEL111")).thenReturn(List.of(productWithId(4L)));
        when(productRepository.findAllBySkuNormalized("MODEL222")).thenReturn(List.of(productWithId(5L)));

        MatchResult result = service.match(new MatchCandidate("NONE", "Tu lanh MODEL111 thay the cho MODEL222", null));

        assertThat(result.status()).isEqualTo(MatchStatus.REVIEW_REQUIRED);
        assertThat(result.matchedProductId()).isNull();
        assertThat(result.conflictingProductIds()).hasSize(2);
    }

    @Test
    void match_duplicateSkuNormalizedInOwnDatabase_isReviewRequiredNotArbitrarilyPicked() {
        when(productRepository.findAllBySkuNormalized("SHAREDGROUP"))
                .thenReturn(List.of(productWithId(6L), productWithId(7L)));

        MatchResult result = service.match(new MatchCandidate("SHARED-GROUP", "Bien the mau khac nhau", null));

        assertThat(result.status()).isEqualTo(MatchStatus.REVIEW_REQUIRED);
        assertThat(result.matchedProductId()).isNull();
        assertThat(result.conflictingProductIds()).containsExactlyInAnyOrder(6L, 7L);
    }

    @Test
    void match_noSkuAliasOrTitleMatch_returnsReviewRequiredNoMatch() {
        when(productRepository.findAllBySkuNormalized("XYZ")).thenReturn(List.of());
        when(productAliasRepository.findFirstByAliasNormalizedAndConfirmedTrue("XYZ")).thenReturn(Optional.empty());

        MatchResult result = service.match(new MatchCandidate("XYZ", "San pham khong lien quan gi ca", null));

        assertThat(result.status()).isEqualTo(MatchStatus.REVIEW_REQUIRED);
        assertThat(result.matchedProductId()).isNull();
    }

    @Test
    void suggestFuzzyMatches_scoresByTitleTokenOverlapAndExcludesSelf() {
        Product target = productWithId(10L);
        target.setTitle("Quat dung Asia F16008 mau xanh");
        Product candidateSame = productWithId(10L);
        Product candidateSimilar = productWithId(11L);
        candidateSimilar.setTitle("Quat dung Asia F16008 mau do");
        Product candidateUnrelated = productWithId(12L);
        candidateUnrelated.setTitle("Tu lanh Funiki 150 lit");

        List<ProductMatchingService.FuzzySuggestion> suggestions =
                service.suggestFuzzyMatches(target, List.of(candidateSame, candidateSimilar, candidateUnrelated), 0.3);

        assertThat(suggestions).extracting(ProductMatchingService.FuzzySuggestion::productId).containsExactly(11L);
    }
}

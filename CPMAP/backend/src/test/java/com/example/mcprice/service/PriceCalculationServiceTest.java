package com.example.mcprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.example.mcprice.domain.Competitor;
import com.example.mcprice.domain.CrawlMode;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchMethod;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.ObservationStatus;
import com.example.mcprice.domain.OutlierStrategy;
import com.example.mcprice.domain.PolicyScope;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.domain.PricePolicy;
import com.example.mcprice.domain.RecommendationStatus;
import com.example.mcprice.domain.SourceType;
import com.example.mcprice.dto.PriceRecommendationDto;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.repository.PricePolicyRepository;
import com.example.mcprice.repository.PriceRecommendationRepository;
import com.example.mcprice.repository.PriceRecommendationSourceRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceCalculationServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CompetitorListingRepository competitorListingRepository;
    @Mock
    private PriceObservationRepository priceObservationRepository;
    @Mock
    private PricePolicyRepository pricePolicyRepository;
    @Mock
    private PriceRecommendationRepository priceRecommendationRepository;
    @Mock
    private PriceRecommendationSourceRepository priceRecommendationSourceRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PriceCalculationService service;

    private Product product;
    private PricePolicy defaultPolicy;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setTitle("San pham test");
        product.setCurrentWebsitePrice(new BigDecimal("10000000"));

        defaultPolicy = PricePolicy.builder()
                .scope(PolicyScope.GLOBAL)
                .minimumCompetitorCount(2)
                .maxObservationAgeHours(48)
                .roundingStep(new BigDecimal("10000"))
                .maxIncreasePercent(new BigDecimal("15"))
                .maxDecreasePercent(new BigDecimal("15"))
                .outlierThresholdPercent(new BigDecimal("30"))
                .outlierStrategy(OutlierStrategy.FLAG_ONLY)
                .requireManualApproval(false)
                .build();

        lenient().when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        lenient().when(pricePolicyRepository.findByScopeAndProductId(PolicyScope.PRODUCT, 1L)).thenReturn(Optional.empty());
        lenient().when(pricePolicyRepository.findByScope(PolicyScope.GLOBAL)).thenReturn(Optional.of(defaultPolicy));
        lenient().when(priceRecommendationRepository.save(any())).thenAnswer(invocation -> {
            var rec = invocation.getArgument(0, com.example.mcprice.domain.PriceRecommendation.class);
            rec.setId(100L);
            rec.setCreatedAt(OffsetDateTime.now());
            return rec;
        });
    }

    private CompetitorListing confirmedListing(Long id, String competitorName) {
        Competitor competitor = Competitor.builder().id(id).name(competitorName).enabled(true).crawlMode(CrawlMode.STATIC_HTML).build();
        return CompetitorListing.builder()
                .id(id)
                .product(product)
                .competitor(competitor)
                .url("https://" + competitorName + "/p/" + id)
                .matchMethod(MatchMethod.EXACT_NORMALIZED_SKU)
                .matchStatus(MatchStatus.AUTO_CONFIRMED)
                .active(true)
                .build();
    }

    private PriceObservation validObservation(BigDecimal price, OffsetDateTime capturedAt) {
        return PriceObservation.builder()
                .id(1L)
                .price(price)
                .currency("VND")
                .sourceType(SourceType.MANUAL)
                .observationStatus(ObservationStatus.VALID)
                .capturedAt(capturedAt)
                .build();
    }

    @Test
    void calculateForProduct_belowMinimumCompetitorCount_isInsufficientDataAndKeepsCurrentPrice() {
        CompetitorListing listing = confirmedListing(1L, "sgt.com.vn");
        when(competitorListingRepository.findConfirmedListingsForProduct(1L,
                List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED))).thenReturn(List.of(listing));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9500000"), OffsetDateTime.now())));

        PriceRecommendationDto dto = service.calculateForProduct(1L);

        assertThat(dto.status()).isEqualTo(RecommendationStatus.INSUFFICIENT_DATA.name());
        assertThat(dto.finalSuggestedPrice()).isEqualByComparingTo(product.getCurrentWebsitePrice());
        assertThat(dto.includedSourceCount()).isEqualTo(1);
    }

    @Test
    void calculateForProduct_twoValidSources_computesArithmeticMeanAndRoundsToStep() {
        CompetitorListing listingA = confirmedListing(1L, "sgt.com.vn");
        CompetitorListing listingB = confirmedListing(2L, "dienmay88.vn");
        when(competitorListingRepository.findConfirmedListingsForProduct(1L,
                List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED))).thenReturn(List.of(listingA, listingB));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9700000"), OffsetDateTime.now())));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(2L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9750000"), OffsetDateTime.now())));

        PriceRecommendationDto dto = service.calculateForProduct(1L);

        // trung binh = (9700000+9750000)/2 = 9725000 -> lam tron buoc 10000 = 9730000 (giam ~2.75%, trong guardrail)
        assertThat(dto.rawAveragePrice()).isEqualByComparingTo(new BigDecimal("9725000"));
        assertThat(dto.roundedPrice()).isEqualByComparingTo(new BigDecimal("9730000"));
        assertThat(dto.includedSourceCount()).isEqualTo(2);
        assertThat(dto.status()).isEqualTo(RecommendationStatus.READY.name());
    }

    @Test
    void calculateForProduct_excludesContactOnlyAndParseErrorObservations() {
        CompetitorListing listingA = confirmedListing(1L, "sgt.com.vn");
        CompetitorListing listingB = confirmedListing(2L, "dienmay88.vn");
        when(competitorListingRepository.findConfirmedListingsForProduct(1L,
                List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED))).thenReturn(List.of(listingA, listingB));
        PriceObservation contactOnly = PriceObservation.builder().id(2L).price(null).currency("VND")
                .sourceType(SourceType.MANUAL).observationStatus(ObservationStatus.CONTACT_ONLY).capturedAt(OffsetDateTime.now()).build();
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.of(contactOnly));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(2L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9700000"), OffsetDateTime.now())));

        PriceRecommendationDto dto = service.calculateForProduct(1L);

        assertThat(dto.status()).isEqualTo(RecommendationStatus.INSUFFICIENT_DATA.name());
        assertThat(dto.includedSourceCount()).isEqualTo(1);
        assertThat(dto.excludedSourceCount()).isEqualTo(1);
    }

    @Test
    void calculateForProduct_staleObservation_isExcludedFromAverage() {
        CompetitorListing listingA = confirmedListing(1L, "sgt.com.vn");
        CompetitorListing listingB = confirmedListing(2L, "dienmay88.vn");
        when(competitorListingRepository.findConfirmedListingsForProduct(1L,
                List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED))).thenReturn(List.of(listingA, listingB));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9700000"), OffsetDateTime.now().minusHours(100))));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(2L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9750000"), OffsetDateTime.now())));

        PriceRecommendationDto dto = service.calculateForProduct(1L);

        assertThat(dto.includedSourceCount()).isEqualTo(1);
        assertThat(dto.status()).isEqualTo(RecommendationStatus.INSUFFICIENT_DATA.name());
    }

    @Test
    void calculateForProduct_outlierFlagged_requiresReviewButDoesNotChangeAverageByDefault() {
        CompetitorListing listingA = confirmedListing(1L, "sgt.com.vn");
        CompetitorListing listingB = confirmedListing(2L, "dienmay88.vn");
        CompetitorListing listingC = confirmedListing(3L, "dienmaythienphu.vn");
        when(competitorListingRepository.findConfirmedListingsForProduct(1L,
                List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED))).thenReturn(List.of(listingA, listingB, listingC));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("10100000"), OffsetDateTime.now())));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(2L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9900000"), OffsetDateTime.now())));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(3L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("16000000"), OffsetDateTime.now())));

        PriceRecommendationDto dto = service.calculateForProduct(1L);

        // trung binh CA 3 nguon (khong loai outlier vi FLAG_ONLY) = (10100000+9900000+16000000)/3 = 12000000
        assertThat(dto.rawAveragePrice()).isEqualByComparingTo(new BigDecimal("12000000"));
        assertThat(dto.includedSourceCount()).isEqualTo(3);
        assertThat(dto.status()).isEqualTo(RecommendationStatus.REVIEW_REQUIRED.name());
    }

    @Test
    void calculateForProduct_guardrailBreached_requiresManualApproval() {
        CompetitorListing listingA = confirmedListing(1L, "sgt.com.vn");
        CompetitorListing listingB = confirmedListing(2L, "dienmay88.vn");
        when(competitorListingRepository.findConfirmedListingsForProduct(1L,
                List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED))).thenReturn(List.of(listingA, listingB));
        // gia trung binh cao hon 15% so voi gia hien tai 10.000.000 -> vuot guardrail tang toi da
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("13000000"), OffsetDateTime.now())));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(2L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("13000000"), OffsetDateTime.now())));

        PriceRecommendationDto dto = service.calculateForProduct(1L);

        assertThat(dto.status()).isEqualTo(RecommendationStatus.REVIEW_REQUIRED.name());
    }

    @Test
    void calculateForProduct_requireManualApprovalPolicy_alwaysReviewEvenWithoutGuardrailBreach() {
        defaultPolicy.setRequireManualApproval(true);
        CompetitorListing listingA = confirmedListing(1L, "sgt.com.vn");
        CompetitorListing listingB = confirmedListing(2L, "dienmay88.vn");
        when(competitorListingRepository.findConfirmedListingsForProduct(1L,
                List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED))).thenReturn(List.of(listingA, listingB));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(1L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9700000"), OffsetDateTime.now())));
        when(priceObservationRepository.findFirstByCompetitorListingIdOrderByCapturedAtDesc(2L))
                .thenReturn(Optional.of(validObservation(new BigDecimal("9750000"), OffsetDateTime.now())));

        PriceRecommendationDto dto = service.calculateForProduct(1L);

        assertThat(dto.status()).isEqualTo(RecommendationStatus.REVIEW_REQUIRED.name());
    }
}

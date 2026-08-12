package com.example.mcprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.CrawlItem;
import com.example.mcprice.domain.CrawlRun;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.domain.Product;
import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.repository.CrawlRunRepository;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Kiem tra logic "tu dong xac nhan 1 listing moi kham pha neu gia crawl duoc lech <= 10% so voi
 * gia hien tai cua san pham" — day la quy tac nghiep vu de sai (vd quen kiem tra crawl that bai,
 * gia null, hoac tinh sai % lech) va anh huong truc tiep den do chinh xac cua gia trung binh.
 */
@ExtendWith(MockitoExtension.class)
class MatchConfirmPriceServiceTest {

    @Mock
    private CrawlRunRepository crawlRunRepository;
    @Mock
    private CrawlItemExecutor crawlItemExecutor;
    @Mock
    private PriceCalculationService priceCalculationService;
    @Mock
    private MatchingService matchingService;
    @Mock
    private CompetitorListingRepository competitorListingRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AuditService auditService;

    private MatchConfirmPriceService service;

    private static final Long LISTING_ID = 42L;
    private static final Long PRODUCT_ID = 7L;
    private static final Long COMPETITOR_ID = 3L;

    @BeforeEach
    void setUp() {
        service = new MatchConfirmPriceService(crawlRunRepository, crawlItemExecutor, priceCalculationService,
                matchingService, competitorListingRepository, productRepository, auditService);
        // lenient: khong phai test nao cung di den buoc crawl (vd test "khong phai REVIEW_REQUIRED" thoat som).
        lenient().when(crawlRunRepository.save(any(CrawlRun.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CompetitorListingDto listingDto(String matchStatus, BigDecimal lastPrice) {
        return new CompetitorListingDto(LISTING_ID, PRODUCT_ID, "San pham", "SKU1", COMPETITOR_ID, "Doi thu",
                "https://doithu.vn/x", null, "SKU_IN_URL_SLUG", BigDecimal.valueOf(0.7), "ly do",
                matchStatus, true, lastPrice, lastPrice == null ? null : "VALID", null);
    }

    @Test
    void tryAutoConfirm_notReviewRequired_doesNothing() {
        when(matchingService.getDto(LISTING_ID)).thenReturn(listingDto("AUTO_CONFIRMED", BigDecimal.valueOf(100_000)));

        boolean result = service.tryAutoConfirmByPricePlausibility(LISTING_ID);

        assertThat(result).isFalse();
        verify(crawlItemExecutor, never()).crawlOne(any(), anyLong(), anyLong());
    }

    @Test
    void tryAutoConfirm_crawlFails_staysReviewRequired() {
        when(matchingService.getDto(LISTING_ID)).thenReturn(listingDto("REVIEW_REQUIRED", null));
        when(crawlItemExecutor.crawlOne(any(), eq(COMPETITOR_ID), eq(LISTING_ID))).thenReturn(CrawlItem.Status.FAILED);

        boolean result = service.tryAutoConfirmByPricePlausibility(LISTING_ID);

        assertThat(result).isFalse();
        verify(competitorListingRepository, never()).save(any());
        verify(priceCalculationService, never()).calculateForProduct(any());
    }

    @Test
    void tryAutoConfirm_crawlSucceedsButNoPrice_staysReviewRequired() {
        when(matchingService.getDto(LISTING_ID))
                .thenReturn(listingDto("REVIEW_REQUIRED", null))
                .thenReturn(listingDto("REVIEW_REQUIRED", null));
        when(crawlItemExecutor.crawlOne(any(), eq(COMPETITOR_ID), eq(LISTING_ID))).thenReturn(CrawlItem.Status.SUCCESS);

        boolean result = service.tryAutoConfirmByPricePlausibility(LISTING_ID);

        assertThat(result).isFalse();
        verify(competitorListingRepository, never()).save(any());
    }

    @Test
    void tryAutoConfirm_productHasNoCurrentPrice_staysReviewRequired() {
        when(matchingService.getDto(LISTING_ID))
                .thenReturn(listingDto("REVIEW_REQUIRED", null))
                .thenReturn(listingDto("REVIEW_REQUIRED", BigDecimal.valueOf(105_000)));
        when(crawlItemExecutor.crawlOne(any(), eq(COMPETITOR_ID), eq(LISTING_ID))).thenReturn(CrawlItem.Status.SUCCESS);
        Product product = Product.builder().id(PRODUCT_ID).currentWebsitePrice(null).build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        boolean result = service.tryAutoConfirmByPricePlausibility(LISTING_ID);

        assertThat(result).isFalse();
        verify(competitorListingRepository, never()).save(any());
    }

    @Test
    void tryAutoConfirm_priceWithinTenPercent_autoConfirmsAndRecalculates() {
        when(matchingService.getDto(LISTING_ID))
                .thenReturn(listingDto("REVIEW_REQUIRED", null))
                .thenReturn(listingDto("REVIEW_REQUIRED", BigDecimal.valueOf(108_000)));
        when(crawlItemExecutor.crawlOne(any(), eq(COMPETITOR_ID), eq(LISTING_ID))).thenReturn(CrawlItem.Status.SUCCESS);
        Product product = Product.builder().id(PRODUCT_ID).currentWebsitePrice(BigDecimal.valueOf(100_000)).build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        CompetitorListing listing = CompetitorListing.builder().id(LISTING_ID).matchStatus(MatchStatus.REVIEW_REQUIRED).build();
        when(competitorListingRepository.findById(LISTING_ID)).thenReturn(Optional.of(listing));

        boolean result = service.tryAutoConfirmByPricePlausibility(LISTING_ID);

        assertThat(result).isTrue();
        assertThat(listing.getMatchStatus()).isEqualTo(MatchStatus.AUTO_CONFIRMED);
        verify(competitorListingRepository, times(1)).save(listing);
        verify(priceCalculationService, times(1)).calculateForProduct(PRODUCT_ID);
    }

    @Test
    void tryAutoConfirm_priceExceedsTenPercent_staysReviewRequired() {
        when(matchingService.getDto(LISTING_ID))
                .thenReturn(listingDto("REVIEW_REQUIRED", null))
                .thenReturn(listingDto("REVIEW_REQUIRED", BigDecimal.valueOf(150_000)));
        when(crawlItemExecutor.crawlOne(any(), eq(COMPETITOR_ID), eq(LISTING_ID))).thenReturn(CrawlItem.Status.SUCCESS);
        Product product = Product.builder().id(PRODUCT_ID).currentWebsitePrice(BigDecimal.valueOf(100_000)).build();
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

        boolean result = service.tryAutoConfirmByPricePlausibility(LISTING_ID);

        assertThat(result).isFalse();
        verify(competitorListingRepository, never()).save(any());
        verify(priceCalculationService, never()).calculateForProduct(any());
    }
}

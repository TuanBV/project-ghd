package com.example.mcprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mcprice.adapter.OwnWebsitePriceFetcher;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.domain.Product;
import com.example.mcprice.dto.ProductDetailDto;
import com.example.mcprice.dto.ProductSummaryDto;
import com.example.mcprice.dto.ProductUpdateRequest;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.repository.PriceRecommendationRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * Kiem tra logic "sua productUrl thi tu dong crawl gia mac dinh, URL khong doi thi khong crawl"
 * trong ProductService.update() — day la nhanh logic de sai (crawl thua tren moi lan luu neu
 * khong so sanh dung URL cu/moi, gay cham va tai khong can thiet cho website chinh).
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductAliasRepository productAliasRepository;
    @Mock
    private CompetitorListingRepository competitorListingRepository;
    @Mock
    private PriceRecommendationRepository priceRecommendationRepository;
    @Mock
    private PriceObservationRepository priceObservationRepository;
    @Mock
    private MatchingService matchingService;
    @Mock
    private AuditService auditService;
    @Mock
    private OwnWebsitePriceFetcher ownWebsitePriceFetcher;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, productAliasRepository, competitorListingRepository,
                priceRecommendationRepository, priceObservationRepository, matchingService, auditService, ownWebsitePriceFetcher);

        Product product = Product.builder()
                .id(1L)
                .title("San pham test")
                .productUrl("https://tongkhodienmaymienbac.com/cu/")
                .currentWebsitePrice(BigDecimal.valueOf(100_000))
                .availability("IN_STOCK")
                .active(true)
                .build();
        // lenient: cac test cua search() khong dung den cac stub nay (danh cho update()).
        lenient().when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(productAliasRepository.findByProductId(1L)).thenReturn(List.of());
        lenient().when(competitorListingRepository.findByProductId(1L)).thenReturn(List.of());
        lenient().when(priceRecommendationRepository.findFirstByProductIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());
    }

    @Test
    void update_urlUnchanged_doesNotFetchPrice_keepsSubmittedPrice() {
        ProductUpdateRequest request = new ProductUpdateRequest(null, null, null, null,
                BigDecimal.valueOf(200_000), true, null, "https://tongkhodienmaymienbac.com/cu/");

        ProductDetailDto result = service.update(1L, request);

        verify(ownWebsitePriceFetcher, never()).fetchPrice(any());
        assertThat(result.currentWebsitePrice()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
    }

    @Test
    void update_urlChanged_fetchesPriceAndOverridesSubmittedPrice() {
        when(ownWebsitePriceFetcher.fetchPrice("https://tongkhodienmaymienbac.com/moi/"))
                .thenReturn(BigDecimal.valueOf(555_000));
        ProductUpdateRequest request = new ProductUpdateRequest(null, null, null, null,
                BigDecimal.valueOf(200_000), true, null, "https://tongkhodienmaymienbac.com/moi/");

        ProductDetailDto result = service.update(1L, request);

        verify(ownWebsitePriceFetcher).fetchPrice("https://tongkhodienmaymienbac.com/moi/");
        assertThat(result.currentWebsitePrice()).isEqualByComparingTo(BigDecimal.valueOf(555_000));
        assertThat(result.productUrl()).isEqualTo("https://tongkhodienmaymienbac.com/moi/");
    }

    @Test
    void update_urlChangedButFetchFails_fallsBackToSubmittedPrice() {
        when(ownWebsitePriceFetcher.fetchPrice("https://tongkhodienmaymienbac.com/moi/")).thenReturn(null);
        ProductUpdateRequest request = new ProductUpdateRequest(null, null, null, null,
                BigDecimal.valueOf(200_000), true, null, "https://tongkhodienmaymienbac.com/moi/");

        ProductDetailDto result = service.update(1L, request);

        assertThat(result.currentWebsitePrice()).isEqualByComparingTo(BigDecimal.valueOf(200_000));
    }

    @Test
    void update_noUrlInRequest_doesNotTouchExistingUrlOrFetchPrice() {
        ProductUpdateRequest request = new ProductUpdateRequest(null, null, null, null,
                null, true, null, null);

        ProductDetailDto result = service.update(1L, request);

        verify(ownWebsitePriceFetcher, never()).fetchPrice(any());
        assertThat(result.productUrl()).isEqualTo("https://tongkhodienmaymienbac.com/cu/");
        assertThat(result.currentWebsitePrice()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
    }

    @Test
    void search_averageOfConfirmedListings_isRoundedToNearestTenThousand() {
        Product product = Product.builder().id(1L).skuOriginal("SKU1").title("San pham 1")
                .currentWebsitePrice(BigDecimal.valueOf(2_000_000)).build();
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(productPage);
        when(priceRecommendationRepository.findLatestForProducts(List.of(1L))).thenReturn(List.of());

        CompetitorListing listing1 = CompetitorListing.builder().id(10L).product(product)
                .matchStatus(MatchStatus.AUTO_CONFIRMED).active(true).build();
        CompetitorListing listing2 = CompetitorListing.builder().id(11L).product(product)
                .matchStatus(MatchStatus.MANUALLY_CONFIRMED).active(true).build();
        when(competitorListingRepository.findByProductIdIn(List.of(1L))).thenReturn(List.of(listing1, listing2));

        // Trung binh raw = (2.000.000 + 2.286.666) / 2 = 2.143.333 -> lam tron ve boi so 10.000 gan nhat = 2.140.000.
        PriceObservation obs1 = PriceObservation.builder().competitorListing(listing1).price(BigDecimal.valueOf(2_000_000)).build();
        PriceObservation obs2 = PriceObservation.builder().competitorListing(listing2).price(BigDecimal.valueOf(2_286_666)).build();
        when(priceObservationRepository.findLatestForListings(anyList())).thenReturn(List.of(obs1, obs2));

        Page<ProductSummaryDto> result = service.search(null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent().get(0).averageCompetitorPrice()).isEqualByComparingTo(BigDecimal.valueOf(2_140_000));
    }

    @Test
    void search_noConfirmedListings_averageIsNull() {
        Product product = Product.builder().id(1L).skuOriginal("SKU1").title("San pham 1")
                .currentWebsitePrice(BigDecimal.valueOf(2_000_000)).build();
        Page<Product> productPage = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(productPage);
        when(priceRecommendationRepository.findLatestForProducts(List.of(1L))).thenReturn(List.of());

        CompetitorListing listing = CompetitorListing.builder().id(10L).product(product)
                .matchStatus(MatchStatus.REVIEW_REQUIRED).active(true).build();
        when(competitorListingRepository.findByProductIdIn(List.of(1L))).thenReturn(List.of(listing));

        Page<ProductSummaryDto> result = service.search(null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent().get(0).averageCompetitorPrice()).isNull();
    }
}

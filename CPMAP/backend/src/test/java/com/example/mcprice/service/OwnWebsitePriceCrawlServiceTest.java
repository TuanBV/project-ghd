package com.example.mcprice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.mcprice.adapter.DomainRateLimiter;
import com.example.mcprice.adapter.OwnWebsitePriceFetcher;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnWebsitePriceCrawlServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OwnWebsitePriceFetcher ownWebsitePriceFetcher;
    @Mock
    private DomainRateLimiter domainRateLimiter;
    @Mock
    private JobRunService jobRunService;

    private OwnWebsitePriceCrawlService service;

    @BeforeEach
    void setUp() {
        service = new OwnWebsitePriceCrawlService(productRepository, ownWebsitePriceFetcher, domainRateLimiter, jobRunService);
    }

    @Test
    void crawlMissingPrices_setsPriceOnSuccessAndCountsFailures() {
        Product found = Product.builder().id(1L).productUrl("https://tongkhodienmaymienbac.com/a/").build();
        Product notFound = Product.builder().id(2L).productUrl("https://tongkhodienmaymienbac.com/b/").build();
        when(productRepository.findByProductUrlIsNotNullAndCurrentWebsitePriceIsNull()).thenReturn(List.of(found, notFound));
        when(ownWebsitePriceFetcher.fetchPrice("https://tongkhodienmaymienbac.com/a/")).thenReturn(BigDecimal.valueOf(123_000));
        when(ownWebsitePriceFetcher.fetchPrice("https://tongkhodienmaymienbac.com/b/")).thenReturn(null);

        int[] result = service.crawlMissingPrices(99L);

        assertThat(result).containsExactly(2, 1, 1);
        assertThat(found.getCurrentWebsitePrice()).isEqualByComparingTo(BigDecimal.valueOf(123_000));
        assertThat(notFound.getCurrentWebsitePrice()).isNull();
        verify(productRepository, times(1)).save(found);
        verify(domainRateLimiter, times(2)).acquire(anyLong(), anyInt());
        verify(jobRunService, times(2)).updateProgress(anyLong(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void crawlMissingPrices_noCandidates_returnsZeroCounts() {
        when(productRepository.findByProductUrlIsNotNullAndCurrentWebsitePriceIsNull()).thenReturn(List.of());

        int[] result = service.crawlMissingPrices(99L);

        assertThat(result).containsExactly(0, 0, 0);
        verify(productRepository, times(0)).save(any());
    }
}

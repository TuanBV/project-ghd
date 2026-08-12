package com.example.mcprice.service;

import com.example.mcprice.adapter.DomainRateLimiter;
import com.example.mcprice.adapter.OwnWebsitePriceFetcher;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Job chay tay (khong nam trong DailyPipelineJob): lay gia tren website cua chinh minh cho cac
 * san pham DANG THIEU gia (co productUrl nhung currentWebsitePrice con null) — dung mot lan cho
 * du lieu cu (vd san pham import tu CSV_PRODUCT truoc khi tinh nang nay ton tai), hoac chay lai
 * de "don not" nhung san pham lan truoc crawl loi (idempotent: chi lay lai ban ghi con thieu).
 *
 * KHONG dung @Transactional o muc method: day la vong lap CO THE chay hang chuc phut (hang nghin
 * URL, gioi han toc do de khong lam qua tai website chinh), moi productRepository.save(...) tu
 * commit rieng — tranh giu 1 transaction/connection DB mo suot qua trinh chay.
 */
@Service
@RequiredArgsConstructor
public class OwnWebsitePriceCrawlService {

    private static final Logger log = LoggerFactory.getLogger(OwnWebsitePriceCrawlService.class);

    /** Key gia (khong trung voi id Competitor that nao) danh rieng cho DomainRateLimiter khi crawl website cua chinh minh. */
    private static final Long RATE_LIMIT_KEY = -1L;
    private static final int REQUESTS_PER_MINUTE = 60;

    private final ProductRepository productRepository;
    private final OwnWebsitePriceFetcher ownWebsitePriceFetcher;
    private final DomainRateLimiter domainRateLimiter;
    private final JobRunService jobRunService;

    public int[] crawlMissingPrices(Long jobRunId) {
        var products = productRepository.findByProductUrlIsNotNullAndCurrentWebsitePriceIsNull();
        int total = products.size();
        int success = 0;
        int failed = 0;
        for (Product product : products) {
            domainRateLimiter.acquire(RATE_LIMIT_KEY, REQUESTS_PER_MINUTE);
            try {
                BigDecimal price = ownWebsitePriceFetcher.fetchPrice(product.getProductUrl());
                if (price != null) {
                    product.setCurrentWebsitePrice(price);
                    productRepository.save(product);
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.warn("Loi khong mong doi khi lay gia cho product #{}: {}", product.getId(), e.getMessage());
                failed++;
            }
            jobRunService.updateProgress(jobRunId, total, success, failed);
        }
        return new int[] { total, success, failed };
    }
}

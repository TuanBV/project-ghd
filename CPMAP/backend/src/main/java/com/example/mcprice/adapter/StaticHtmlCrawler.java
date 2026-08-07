package com.example.mcprice.adapter;

import com.example.mcprice.util.SafeUrlValidator;
import com.example.mcprice.domain.Competitor;
import com.example.mcprice.domain.CrawlMode;
import com.example.mcprice.config.AppProperties;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.domain.CompetitorListing;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Crawl trang HTML tinh bang Jsoup. Selector gia/khuyen mai/ton kho/ma san pham lay tu
 * extractor_config (JSONB) cua tung competitor, khong hardcode CSS selector trong service.
 */
@Component
@RequiredArgsConstructor
public class StaticHtmlCrawler implements CompetitorPriceCrawler {

    private static final Logger log = LoggerFactory.getLogger(StaticHtmlCrawler.class);

    private final AppProperties appProperties;
    private final RobotsTxtChecker robotsTxtChecker;
    private final GenericPriceExtractor genericPriceExtractor;

    @Override
    public boolean supports(Competitor competitor) {
        return competitor.getCrawlMode() == CrawlMode.STATIC_HTML;
    }

    @Override
    @Retry(name = "competitorCrawl")
    @CircuitBreaker(name = "competitorCrawl")
    public CrawlResult crawl(CompetitorListing listing) {
        String url = listing.getUrl();
        if (!SafeUrlValidator.isSafeToCrawl(url, appProperties.getSecurity().getAllowedCrawlDomains())) {
            return CrawlResult.failure("URL khong nam trong danh sach domain duoc phep crawl hoac khong an toan (SSRF guard)");
        }
        String userAgent = appProperties.getSecurity().getCrawlUserAgent();
        if (!robotsTxtChecker.isAllowed(url, userAgent)) {
            return CrawlResult.manualRequired("robots.txt cua domain nay khong cho phep crawl trang nay");
        }
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(appProperties.getSecurity().getCrawlTimeoutSeconds() * 1000)
                    .maxBodySize(5 * 1024 * 1024)
                    .followRedirects(true)
                    .execute();
            Document document = response.parse();
            Map<String, Object> config = listing.getCompetitor().getExtractorConfig();
            return extract(document, config, response.statusCode(), response.url().toString());
        } catch (Exception e) {
            log.warn("Crawl loi cho listing #{} ({}): {}", listing.getId(), url, e.getMessage());
            return CrawlResult.failure("Loi khi crawl: " + e.getMessage());
        }
    }

    private CrawlResult extract(Document document, Map<String, Object> config, int httpStatus, String finalUrl) {
        String priceSelector = stringConfig(config, "priceSelector");
        String saleSelector = stringConfig(config, "saleSelector");
        String stockSelector = stringConfig(config, "stockSelector");

        String priceText = firstNonBlankText(document, saleSelector, priceSelector);
        String stockText = textOf(document, stockSelector);

        if (priceText == null) {
            // Doi thu chua duoc cau hinh selector rieng (hoac selector khong khop trang nay) —
            // thu them cac chuan pho bien (JSON-LD Product, meta tag, microdata) truoc khi bo cuoc,
            // de van co gia ma khong bat admin phai tu tay dien CSS selector cho tung website.
            var generic = genericPriceExtractor.extract(document);
            if (generic.isPresent()) {
                priceText = generic.get().priceText();
                if (stockText == null) {
                    stockText = generic.get().availabilityText();
                }
            }
        }

        return CrawlResultClassifier.classify(priceText, stockText, httpStatus, finalUrl);
    }

    private String firstNonBlankText(Document document, String... selectors) {
        for (String selector : selectors) {
            String text = textOf(document, selector);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String textOf(Document document, String selector) {
        if (selector == null || selector.isBlank()) {
            return null;
        }
        Element element = document.selectFirst(selector);
        return element == null ? null : element.text().trim();
    }

    private String stringConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? null : value.toString();
    }
}

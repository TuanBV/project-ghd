package com.example.mcprice.adapter;

import com.example.mcprice.util.AvailabilityNormalizer;
import com.example.mcprice.util.PriceParser;
import com.example.mcprice.util.SafeUrlValidator;
import com.example.mcprice.domain.Competitor;
import com.example.mcprice.domain.CrawlMode;
import com.example.mcprice.config.AppProperties;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.ObservationStatus;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback crawl cho trang render bang JavaScript, dung Playwright. Playwright browser
 * binary co the CHUA duoc cai trong moi trung local/CI (khong bat buoc voi cac doi thu
 * seed san la STATIC_HTML/MANUAL_ONLY); neu khong khoi tao duoc browser, tra ve ket qua
 * "can nhap tay" thay vi gia lap du lieu hoac lam crash job.
 */
@Component
@RequiredArgsConstructor
public class BrowserCrawler implements CompetitorPriceCrawler {

    private static final Logger log = LoggerFactory.getLogger(BrowserCrawler.class);

    private final AppProperties appProperties;
    private final GenericPriceExtractor genericPriceExtractor;

    @Override
    public boolean supports(Competitor competitor) {
        return competitor.getCrawlMode() == CrawlMode.BROWSER;
    }

    @Override
    public CrawlResult crawl(CompetitorListing listing) {
        String url = listing.getUrl();
        if (!SafeUrlValidator.isSafeToCrawl(url, appProperties.getSecurity().getAllowedCrawlDomains())) {
            return CrawlResult.failure("URL khong nam trong danh sach domain duoc phep crawl hoac khong an toan (SSRF guard)");
        }
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
            try (Browser browser = playwright.chromium().launch(options)) {
                Page page = browser.newPage();
                page.setDefaultTimeout(appProperties.getSecurity().getCrawlTimeoutSeconds() * 1000.0);
                page.navigate(url);
                Map<String, Object> config = listing.getCompetitor().getExtractorConfig();
                return extract(page, config, url);
            }
        } catch (Exception e) {
            log.info("Khong the chay Playwright browser cho listing #{} ({}), can nhap tay: {}",
                    listing.getId(), url, e.getMessage());
            return CrawlResult.manualRequired("Browser crawler khong san sang (co the chua cai driver): " + e.getMessage());
        }
    }

    private CrawlResult extract(Page page, Map<String, Object> config, String url) {
        String priceSelector = stringConfig(config, "priceSelector");
        String saleSelector = stringConfig(config, "saleSelector");
        String stockSelector = stringConfig(config, "stockSelector");

        String priceText = firstNonBlank(page, saleSelector, priceSelector);
        String stockText = textOf(page, stockSelector);

        if (priceText == null) {
            // Nhu StaticHtmlCrawler: khong co/khong khop selector rieng thi thu chuan pho bien
            // (JSON-LD/meta/microdata) tren HTML da render, truoc khi bo cuoc.
            var generic = genericPriceExtractor.extract(Jsoup.parse(page.content()));
            if (generic.isPresent()) {
                priceText = generic.get().priceText();
                if (stockText == null) {
                    stockText = generic.get().availabilityText();
                }
            }
        }

        if (priceText == null) {
            return new CrawlResult(true, ObservationStatus.NO_PRICE, null, null, stockText, 200, url,
                    "Khong tim thay gia theo selector da cau hinh hoac chuan pho bien (JSON-LD/meta/microdata)");
        }
        if (PriceParser.isContactOnly(priceText)) {
            return new CrawlResult(true, ObservationStatus.CONTACT_ONLY, null, priceText, stockText, 200, url, null);
        }
        var parsed = PriceParser.parse(priceText);
        if (parsed.isEmpty()) {
            return new CrawlResult(true, ObservationStatus.PARSE_ERROR, null, priceText, stockText, 200, url,
                    "Khong parse duoc gia tu chuoi: " + priceText);
        }
        var availability = AvailabilityNormalizer.normalize(stockText);
        if (availability == AvailabilityNormalizer.Availability.OUT_OF_STOCK) {
            return new CrawlResult(true, ObservationStatus.OUT_OF_STOCK, parsed.get().amount(), priceText, stockText, 200, url, null);
        }
        return new CrawlResult(true, ObservationStatus.VALID, parsed.get().amount(), priceText, stockText, 200, url, null);
    }

    private String firstNonBlank(Page page, String... selectors) {
        for (String selector : selectors) {
            String text = textOf(page, selector);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String textOf(Page page, String selector) {
        if (selector == null || selector.isBlank()) {
            return null;
        }
        try {
            return page.textContent(selector);
        } catch (Exception e) {
            return null;
        }
    }

    private String stringConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? null : value.toString();
    }
}

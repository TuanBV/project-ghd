package com.example.mcprice.adapter;

import com.example.mcprice.config.AppProperties;
import com.example.mcprice.util.PriceParser;
import com.example.mcprice.util.SafeUrlValidator;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lay gia hien tai tren website CUA CHINH MINH (tongkhodienmaymienbac.com) tu productUrl, dung
 * lai GenericPriceExtractor (khong co CSS selector rieng vi day khong phai competitor duoc cau
 * hinh extractor_config — chi dua vao JSON-LD/meta tag chuan ma trang WooCommerce nao cung co).
 *
 * Khac voi CompetitorPriceCrawler: khong tao PriceObservation/CrawlItem (nhung bang do gan voi
 * CompetitorListing, khong lien quan toi gia tren website cua minh) — chi tra ve BigDecimal de
 * goi noi truc tiep gan vao Product.currentWebsitePrice.
 */
@Component
@RequiredArgsConstructor
public class OwnWebsitePriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(OwnWebsitePriceFetcher.class);

    private final AppProperties appProperties;
    private final RobotsTxtChecker robotsTxtChecker;
    private final GenericPriceExtractor genericPriceExtractor;

    /** Tra ve null neu khong lay duoc gia (URL khong an toan, robots.txt chan, loi mang, khong tim thay gia...). */
    public BigDecimal fetchPrice(String url) {
        if (!SafeUrlValidator.isSafeToCrawl(url, appProperties.getSecurity().getAllowedCrawlDomains())) {
            log.warn("Khong the lay gia: URL khong an toan hoac khong nam trong allowed-crawl-domains: {}", url);
            return null;
        }
        String userAgent = appProperties.getSecurity().getCrawlUserAgent();
        if (!robotsTxtChecker.isAllowed(url, userAgent)) {
            log.warn("Khong the lay gia: robots.txt chan URL {}", url);
            return null;
        }
        try {
            Document document = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(appProperties.getSecurity().getCrawlTimeoutSeconds() * 1000)
                    .maxBodySize(5 * 1024 * 1024)
                    .followRedirects(true)
                    .get();
            return genericPriceExtractor.extract(document)
                    .flatMap(extracted -> PriceParser.parse(extracted.priceText()))
                    .map(PriceParser.ParsedPrice::amount)
                    // Gia <= 0 la placeholder/loi du lieu, khong phai gia that (giong CrawlResultClassifier).
                    .filter(amount -> amount.signum() > 0)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Loi khi lay gia tu {}: {}", url, e.getMessage());
            return null;
        }
    }
}

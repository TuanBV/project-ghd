package com.example.mcprice.service;

import com.example.mcprice.util.SafeUrlValidator;
import com.example.mcprice.config.AppProperties;
import com.example.mcprice.adapter.DomainRateLimiter;
import com.example.mcprice.adapter.RobotsTxtChecker;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Doc sitemap.xml (co the la sitemapindex long nhau nhieu cap) de lay danh sach URL san pham,
 * thay vi dung chuc nang tim kiem noi bo — nhieu website Disallow duong dan tim kiem trong
 * robots.txt nhung LUON cho phep doc sitemap (chinh site cong bo de phuc vu crawler).
 */
@Component
@RequiredArgsConstructor
public class SitemapFetcher {

    private static final Logger log = LoggerFactory.getLogger(SitemapFetcher.class);
    private static final int MAX_SITEMAP_FILES = 500;
    private static final int MAX_FETCH_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 4000;

    private final AppProperties appProperties;
    private final RobotsTxtChecker robotsTxtChecker;
    private final DomainRateLimiter domainRateLimiter;

    /**
     * Cac tu khoa trong TEN FILE sitemap con cho biet day KHONG phai sitemap san pham (blog,
     * trang tinh, danh muc, thuoc tinh loc, thuong hieu...). Nhieu site gop chung sitemap
     * blog/tin-tuc/brand va sitemap san pham trong cung mot sitemap index; neu khong loc, SKU
     * co the trung ngau nhien voi slug bai viet/trang thuong hieu, tao ra ghep sai hang loat.
     */
    private static final List<String> NON_PRODUCT_SITEMAP_HINTS = List.of(
            "post", "blog", "tin-tuc", "news", "page-sitemap", "sitemap_page", "category", "collection",
            "product_cat", "product-cat", "pa_", "attribute", "author", "article", "brand");

    /** Lay toan bo URL <loc> tim thay trong CAC sitemap con duoc nhan dien la sitemap san pham. */
    public List<String> fetchAllUrls(String rootSitemapUrl, Long competitorId, int requestsPerMinute, String userAgent) {
        Set<String> collected = new LinkedHashSet<>();
        Set<String> visitedSitemapFiles = new LinkedHashSet<>();
        fetchRecursive(rootSitemapUrl, competitorId, requestsPerMinute, userAgent, collected, visitedSitemapFiles, true);
        return new ArrayList<>(collected);
    }

    private void fetchRecursive(String sitemapUrl, Long competitorId, int requestsPerMinute, String userAgent,
                                 Set<String> collectedProductUrls, Set<String> visitedSitemapFiles, boolean isRoot) {
        if (visitedSitemapFiles.size() >= MAX_SITEMAP_FILES || visitedSitemapFiles.contains(sitemapUrl)) {
            return;
        }
        if (!isRoot && looksLikeNonProductSitemap(sitemapUrl)) {
            log.debug("Bo qua sitemap khong phai san pham: {}", sitemapUrl);
            return;
        }
        visitedSitemapFiles.add(sitemapUrl);

        if (!SafeUrlValidator.isSafeToCrawl(sitemapUrl, appProperties.getSecurity().getAllowedCrawlDomains())) {
            log.warn("Bo qua sitemap khong thuoc domain duoc phep: {}", sitemapUrl);
            return;
        }
        if (!robotsTxtChecker.isAllowed(sitemapUrl, userAgent)) {
            log.warn("robots.txt khong cho phep doc: {}", sitemapUrl);
            return;
        }

        Document document = fetchWithRetry(sitemapUrl, competitorId, requestsPerMinute, userAgent);
        if (document == null) {
            return;
        }

        var nestedSitemaps = document.select("sitemapindex > sitemap > loc");
        if (!nestedSitemaps.isEmpty()) {
            for (var loc : nestedSitemaps) {
                fetchRecursive(loc.text().trim(), competitorId, requestsPerMinute, userAgent, collectedProductUrls,
                        visitedSitemapFiles, false);
            }
            return;
        }

        // Mot so site (vd dienmayabc.com: sitemap_pc.xml) dung <urlset> (khong phai <sitemapindex>
        // dung chuan) nhung cac <loc> ben trong lai la LIEN KET DEN CAC FILE SITEMAP CON KHAC (vd
        // sitemap_pc195.xml), khong phai trang san pham thuc — neu khong de quy tiep, se mat toan
        // bo san pham nam trong cac file sitemap con nay. Nhan dien qua duoi ".xml".
        for (var loc : document.select("urlset > url > loc")) {
            String url = loc.text().trim();
            if (url.toLowerCase(java.util.Locale.ROOT).endsWith(".xml")) {
                fetchRecursive(url, competitorId, requestsPerMinute, userAgent, collectedProductUrls,
                        visitedSitemapFiles, false);
            } else {
                collectedProductUrls.add(url);
            }
        }
    }

    /**
     * Nhieu site sinh sitemap lon (WooCommerce/Yoast) "nong" khi it dung, gay timeout/zero-bytes
     * o lan doc dau tien du server van khoe manh — retry vai lan voi backoff truoc khi bo cuoc,
     * thay vi coi la loi vinh vien chi sau 1 lan thu (tranh bao cao thieu URL sai lech).
     */
    private Document fetchWithRetry(String sitemapUrl, Long competitorId, int requestsPerMinute, String userAgent) {
        for (int attempt = 1; attempt <= MAX_FETCH_ATTEMPTS; attempt++) {
            domainRateLimiter.acquire(competitorId, requestsPerMinute);
            try {
                return Jsoup.connect(sitemapUrl)
                        .userAgent(userAgent)
                        .timeout(30_000)
                        .maxBodySize(20 * 1024 * 1024)
                        .parser(Parser.xmlParser())
                        .get();
            } catch (Exception e) {
                if (attempt == MAX_FETCH_ATTEMPTS) {
                    log.warn("Khong doc duoc sitemap {} sau {} lan thu: {}", sitemapUrl, attempt, e.getMessage());
                } else {
                    log.debug("Loi tam thoi doc sitemap {} (lan {}/{}): {}, se thu lai",
                            sitemapUrl, attempt, MAX_FETCH_ATTEMPTS, e.getMessage());
                    sleepQuietly(RETRY_BACKOFF_MS);
                }
            }
        }
        return null;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean looksLikeNonProductSitemap(String sitemapUrl) {
        String lower = sitemapUrl.toLowerCase(java.util.Locale.ROOT);
        return NON_PRODUCT_SITEMAP_HINTS.stream().anyMatch(lower::contains);
    }
}

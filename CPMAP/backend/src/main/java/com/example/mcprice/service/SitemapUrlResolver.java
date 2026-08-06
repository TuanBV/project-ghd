package com.example.mcprice.service;

import com.example.mcprice.adapter.RobotsTxtChecker;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tu dong tim sitemap san pham cua mot website tu baseUrl, de nguoi dung khong phai tu tay
 * dien sitemapUrl khi them doi thu moi. Uu tien "Sitemap:" khai bao trong robots.txt (chinh
 * website tu cong bo, dang tin cay nhat); neu khong co thi thu cac duong dan pho bien va xac
 * minh noi dung thuc su la sitemap XML truoc khi chap nhan.
 */
@Component
@RequiredArgsConstructor
public class SitemapUrlResolver {

    private static final Logger log = LoggerFactory.getLogger(SitemapUrlResolver.class);

    private static final List<String> COMMON_SITEMAP_PATHS = List.of(
            "/sitemap.xml", "/sitemap_index.xml", "/sitemap-index.xml", "/sitemap1.xml");

    private final RobotsTxtChecker robotsTxtChecker;

    public Optional<String> resolve(String baseUrl, String userAgent) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        for (String declared : robotsTxtChecker.getSitemapUrls(normalizedBase, userAgent)) {
            if (looksLikeXmlSitemap(declared, userAgent)) {
                log.info("Tim thay sitemap qua robots.txt cho {}: {}", normalizedBase, declared);
                return Optional.of(declared);
            }
        }

        for (String path : COMMON_SITEMAP_PATHS) {
            String candidate = normalizedBase + path;
            if (!robotsTxtChecker.isAllowed(candidate, userAgent)) {
                continue;
            }
            if (looksLikeXmlSitemap(candidate, userAgent)) {
                log.info("Tim thay sitemap qua duong dan pho bien cho {}: {}", normalizedBase, candidate);
                return Optional.of(candidate);
            }
        }

        log.warn("Khong tu dong tim duoc sitemap san pham cho {}", normalizedBase);
        return Optional.empty();
    }

    private boolean looksLikeXmlSitemap(String url, String userAgent) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(10_000)
                    .maxBodySize(5 * 1024 * 1024)
                    .parser(Parser.xmlParser())
                    .get();
            return !document.select("urlset, sitemapindex").isEmpty();
        } catch (Exception e) {
            log.debug("Khong doc duoc {} nhu mot sitemap XML: {}", url, e.getMessage());
            return false;
        }
    }
}

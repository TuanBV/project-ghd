package com.example.mcprice.adapter;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Kiem tra robots.txt o muc co ban (Disallow cho User-agent: * hoac dung ten bot) truoc khi
 * crawl, tuan thu dieu khoan website. Cache theo host de tranh goi robots.txt qua nhieu lan.
 */
@Component
public class RobotsTxtChecker {

    private static final Logger log = LoggerFactory.getLogger(RobotsTxtChecker.class);

    private final Map<String, String> rawRobotsTxtByHost = new ConcurrentHashMap<>();

    public boolean isAllowed(String url, String userAgent) {
        try {
            URI uri = URI.create(url);
            String host = uri.getScheme() + "://" + uri.getHost();
            List<String> disallowed = parseDisallowForAllAgents(fetchRawRobotsTxt(host, userAgent));
            String path = uri.getPath() == null ? "/" : uri.getPath();
            return disallowed.stream().noneMatch(rule -> !rule.isBlank() && path.startsWith(rule));
        } catch (Exception e) {
            log.warn("Khong kiem tra duoc robots.txt cho {}: {}", url, e.getMessage());
            return true;
        }
    }

    /**
     * Doc cac dong "Sitemap:" ma chinh website tu cong bo trong robots.txt — day la cach
     * dang tin cay nhat de tim sitemap san pham, khong can do doan duong dan pho bien.
     */
    public List<String> getSitemapUrls(String baseUrl, String userAgent) {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getScheme() + "://" + uri.getHost();
            return parseSitemapDirectives(fetchRawRobotsTxt(host, userAgent));
        } catch (Exception e) {
            log.warn("Khong doc duoc Sitemap: trong robots.txt cua {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    private String fetchRawRobotsTxt(String host, String userAgent) {
        return rawRobotsTxtByHost.computeIfAbsent(host, h -> {
            try {
                Connection.Response response = Jsoup.connect(h + "/robots.txt")
                        .userAgent(userAgent)
                        .timeout((int) Duration.ofSeconds(5).toMillis())
                        .ignoreContentType(true)
                        .execute();
                return response.body();
            } catch (Exception e) {
                log.info("Khong tai duoc robots.txt cho {}, coi nhu cho phep crawl (best-effort)", h);
                return "";
            }
        });
    }

    private List<String> parseDisallowForAllAgents(String robotsTxt) {
        List<String> disallow = new java.util.ArrayList<>();
        boolean relevantSection = false;
        for (String line : robotsTxt.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String lower = trimmed.toLowerCase();
            if (lower.startsWith("user-agent:")) {
                String agent = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                relevantSection = agent.equals("*");
            } else if (relevantSection && lower.startsWith("disallow:")) {
                String path = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                if (!path.isEmpty()) {
                    disallow.add(path);
                }
            }
        }
        return disallow;
    }

    private List<String> parseSitemapDirectives(String robotsTxt) {
        List<String> sitemaps = new java.util.ArrayList<>();
        for (String line : robotsTxt.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith("sitemap:")) {
                String value = trimmed.substring(trimmed.indexOf(':') + 1).trim();
                if (!value.isEmpty()) {
                    sitemaps.add(value);
                }
            }
        }
        return sitemaps;
    }
}

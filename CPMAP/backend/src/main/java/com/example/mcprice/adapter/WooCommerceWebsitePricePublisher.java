package com.example.mcprice.adapter;

import com.example.mcprice.config.AppProperties;
import com.example.mcprice.domain.Product;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter cho WooCommerce REST API, chi hoat dong khi app.woocommerce.enabled=true.
 * Khong hardcode credentials — consumer key/secret luon lay tu bien moi truong qua AppProperties.
 * San pham can co externalId la WooCommerce product ID de PUT /wp-json/wc/v3/products/{id}.
 */
@Component
@RequiredArgsConstructor
public class WooCommerceWebsitePricePublisher implements WebsitePricePublisher {

    private static final Logger log = LoggerFactory.getLogger(WooCommerceWebsitePricePublisher.class);

    private final AppProperties appProperties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public PublishResult updateWebsitePrice(Product product, BigDecimal newPrice) {
        var config = appProperties.getWoocommerce();
        if (!config.isEnabled()) {
            return new PublishResult(false, "WooCommerce publisher chua duoc bat (WOOCOMMERCE_ENABLED=false)");
        }
        if (product.getExternalId() == null) {
            return new PublishResult(false, "San pham #" + product.getId() + " chua co externalId (WooCommerce product ID)");
        }
        try {
            String body = "{\"regular_price\":\"" + newPrice.toPlainString() + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/wp-json/wc/v3/products/" + product.getExternalId()))
                    .header("Authorization", basicAuth(config.getConsumerKey(), config.getConsumerSecret()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() / 100 == 2;
            return new PublishResult(success, "WooCommerce PUT status=" + response.statusCode());
        } catch (Exception e) {
            log.error("Loi cap nhat gia WooCommerce cho product #{}: {}", product.getId(), e.getMessage());
            return new PublishResult(false, "Loi goi WooCommerce API: " + e.getMessage());
        }
    }

    @Override
    public VerificationResult verifyLandingPagePrice(Product product, BigDecimal expectedPrice) {
        var config = appProperties.getWoocommerce();
        if (!config.isEnabled() || product.getExternalId() == null) {
            return new VerificationResult(false, null, "WooCommerce chua san sang de verify");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/wp-json/wc/v3/products/" + product.getExternalId()))
                    .header("Authorization", basicAuth(config.getConsumerKey(), config.getConsumerSecret()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                return new VerificationResult(false, null, "WooCommerce GET status=" + response.statusCode());
            }
            String body = response.body();
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\"regular_price\"\\s*:\\s*\"([0-9.]+)\"").matcher(body);
            if (!matcher.find()) {
                return new VerificationResult(false, null, "Khong doc duoc regular_price tu response");
            }
            BigDecimal actual = new BigDecimal(matcher.group(1));
            return new VerificationResult(actual.compareTo(expectedPrice) == 0, actual, "Da doc gia tu WooCommerce");
        } catch (Exception e) {
            return new VerificationResult(false, null, "Loi verify WooCommerce: " + e.getMessage());
        }
    }

    private String basicAuth(String key, String secret) {
        String raw = key + ":" + secret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}

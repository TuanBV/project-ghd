package com.example.mcprice.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Trich xuat gia/tinh trang con hang KHONG can admin tu khai bao CSS selector cho tung doi
 * thu — chi dung khi selector rieng (priceSelector/saleSelector) khong cau hinh hoac khong
 * tim thay gia tren trang. Uu tien theo do tin cay: JSON-LD Product (chuan schema.org, Google
 * cung dung de hien gia tren ket qua tim kiem nen hau het site thuong mai dien tu co san) ->
 * meta tag Open Graph/Product -> microdata itemprop="price" -> mot vai class CSS pho bien
 * nhat (".price" ...) nhu phuong an cuoi cung.
 */
@Component
@RequiredArgsConstructor
public class GenericPriceExtractor {

    private static final List<String> COMMON_PRICE_SELECTORS = List.of(
            ".product-price", ".price-box .price", ".current-price", ".sale-price", ".price");

    private final ObjectMapper objectMapper;

    public record Extracted(String priceText, String availabilityText) {
    }

    public Optional<Extracted> extract(Document document) {
        Optional<Extracted> fromJsonLd = extractFromJsonLd(document);
        if (fromJsonLd.isPresent()) {
            return fromJsonLd;
        }

        String metaPrice = firstNonBlankAttr(document, "content",
                "meta[property=product:price:amount]", "meta[property=og:price:amount]", "meta[itemprop=price]");
        if (metaPrice != null) {
            String metaAvailability = firstNonBlankAttr(document, "content",
                    "meta[property=product:availability]", "meta[property=og:availability]");
            return Optional.of(new Extracted(metaPrice, metaAvailability));
        }

        Element microdataPrice = document.selectFirst("[itemprop=price]");
        if (microdataPrice != null) {
            String value = microdataPrice.hasAttr("content") ? microdataPrice.attr("content") : microdataPrice.text();
            if (!value.isBlank()) {
                return Optional.of(new Extracted(value, null));
            }
        }

        for (String selector : COMMON_PRICE_SELECTORS) {
            Element element = document.selectFirst(selector);
            if (element != null && !element.text().isBlank()) {
                return Optional.of(new Extracted(element.text(), null));
            }
        }
        return Optional.empty();
    }

    private Optional<Extracted> extractFromJsonLd(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                JsonNode root = objectMapper.readTree(script.data());
                JsonNode product = findProductNode(root);
                if (product == null) {
                    continue;
                }
                JsonNode offers = product.get("offers");
                if (offers != null && offers.isArray() && !offers.isEmpty()) {
                    offers = offers.get(0);
                }
                if (offers == null) {
                    continue;
                }
                JsonNode priceNode = offers.get("price");
                if (priceNode == null || priceNode.asText("").isBlank()) {
                    continue;
                }
                JsonNode availabilityNode = offers.get("availability");
                return Optional.of(new Extracted(priceNode.asText(),
                        availabilityNode == null ? null : availabilityNode.asText(null)));
            } catch (Exception ignored) {
                // JSON-LD khong hop le hoac khong lien quan den Product, bo qua block nay
            }
        }
        return Optional.empty();
    }

    private JsonNode findProductNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            JsonNode type = node.get("@type");
            if (type != null && isProductType(type)) {
                return node;
            }
            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                JsonNode found = findProductNode(children.next());
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findProductNode(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean isProductType(JsonNode typeNode) {
        if (typeNode.isTextual()) {
            return "Product".equalsIgnoreCase(typeNode.asText());
        }
        if (typeNode.isArray()) {
            for (JsonNode t : typeNode) {
                if ("Product".equalsIgnoreCase(t.asText(""))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String firstNonBlankAttr(Document document, String attr, String... selectors) {
        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                String value = element.attr(attr);
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }
}

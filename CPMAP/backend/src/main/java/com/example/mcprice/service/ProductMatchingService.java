package com.example.mcprice.service;

import com.example.mcprice.util.SkuNormalizer;
import com.example.mcprice.util.VietnameseTextUtil;
import com.example.mcprice.domain.MatchMethod;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.dto.MatchCandidate;
import com.example.mcprice.dto.MatchResult;
import com.example.mcprice.domain.Product;
import com.example.mcprice.domain.ProductAlias;
import com.example.mcprice.repository.ProductAliasRepository;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline ghep san pham theo dung thu tu nghiep vu (muc 6 cua dac ta):
 * 1. Exact normalized SKU. 2. Exact alias da xac nhan. 3. SKU/model trong title.
 * 4. SKU/model trong URL slug. 5. Fuzzy (xem {@link #suggestFuzzyMatches}). 6. Manual.
 * Khong bao gio tu dong gop cac model gan giong nhau; ket qua REVIEW_REQUIRED khong duoc auto-publish.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductMatchingService {

    /** SKU/model qua ngan trong title de coi la chac chan, phai review du tim thay. */
    private static final int MIN_CONFIDENT_TOKEN_LENGTH = 5;

    private final ProductRepository productRepository;
    private final ProductAliasRepository productAliasRepository;

    public MatchResult match(MatchCandidate candidate) {
        String skuNormalized = SkuNormalizer.normalize(candidate.skuRaw());

        MatchResult exactSku = matchByExactSku(skuNormalized);
        if (exactSku != null) {
            return exactSku;
        }

        MatchResult confirmedAlias = matchByConfirmedAlias(skuNormalized);
        if (confirmedAlias != null) {
            return confirmedAlias;
        }

        MatchResult titleMatch = matchBySkuInTitle(candidate.titleRaw());
        if (titleMatch != null) {
            return titleMatch;
        }

        MatchResult urlMatch = matchBySkuInUrlSlug(candidate.url());
        if (urlMatch != null) {
            return urlMatch;
        }

        return MatchResult.noMatch("Khong tim thay SKU/alias/title/url slug khop; can fuzzy match hoac gan tay");
    }

    /**
     * sku_normalized khong unique trong DB (nhieu bien the mau/dung luong co the chia se cung
     * item_group_id) — luon tra ve List thay vi Optional de khong bao gio nem
     * IncorrectResultSizeDataAccessException khi co nhieu hon 1 ket qua.
     */
    private List<Product> findBySkuNormalizedSafe(String skuNormalized) {
        if (skuNormalized == null || skuNormalized.isBlank()) {
            return List.of();
        }
        return productRepository.findAllBySkuNormalized(skuNormalized);
    }

    private MatchResult matchByExactSku(String skuNormalized) {
        List<Product> found = findBySkuNormalizedSafe(skuNormalized);
        if (found.isEmpty()) {
            return null;
        }
        if (found.size() > 1) {
            return new MatchResult(null, MatchMethod.EXACT_NORMALIZED_SKU, BigDecimal.valueOf(0.5),
                    "SKU chuan hoa '" + skuNormalized + "' trung voi " + found.size()
                            + " san pham (co the la cac bien the cung dong), can duyet thu cong de chon dung",
                    MatchStatus.REVIEW_REQUIRED, found.stream().map(Product::getId).toList());
        }
        Product p = found.get(0);
        return new MatchResult(p.getId(), MatchMethod.EXACT_NORMALIZED_SKU, BigDecimal.ONE,
                "SKU chuan hoa '" + skuNormalized + "' khop chinh xac voi san pham #" + p.getId(),
                MatchStatus.AUTO_CONFIRMED, List.of());
    }

    private MatchResult matchByConfirmedAlias(String skuNormalized) {
        if (skuNormalized == null || skuNormalized.isBlank()) {
            return null;
        }
        return productAliasRepository.findFirstByAliasNormalizedAndConfirmedTrue(skuNormalized)
                .map(alias -> new MatchResult(alias.getProduct().getId(), MatchMethod.CONFIRMED_ALIAS,
                        BigDecimal.valueOf(0.95),
                        "Khop voi alias da xac nhan '" + alias.getAliasNormalized() + "' (" + alias.getAliasType() + ")",
                        MatchStatus.AUTO_CONFIRMED, List.of()))
                .orElse(null);
    }

    private MatchResult matchBySkuInTitle(String titleRaw) {
        if (titleRaw == null || titleRaw.isBlank()) {
            return null;
        }
        List<String> tokens = SkuNormalizer.extractSkuLikeTokens(titleRaw);
        Set<Long> matchedProductIds = new LinkedHashSet<>();
        Long lastMatchedId = null;
        boolean anyShortToken = false;
        String lastToken = null;
        for (String token : tokens) {
            String normalizedToken = SkuNormalizer.normalize(token);
            List<Product> found = findBySkuNormalizedSafe(normalizedToken);
            for (Product product : found) {
                matchedProductIds.add(product.getId());
                lastMatchedId = product.getId();
                lastToken = normalizedToken;
            }
            if (!found.isEmpty() && normalizedToken.length() < MIN_CONFIDENT_TOKEN_LENGTH) {
                anyShortToken = true;
            }
        }
        if (matchedProductIds.isEmpty()) {
            return null;
        }
        if (matchedProductIds.size() > 1) {
            return new MatchResult(null, MatchMethod.SKU_IN_TITLE, BigDecimal.valueOf(0.4),
                    "Tim thay nhieu SKU khac nhau trong title, can duyet thu cong de tranh gop nham",
                    MatchStatus.REVIEW_REQUIRED, matchedProductIds.stream().toList());
        }
        boolean confident = !anyShortToken;
        return new MatchResult(lastMatchedId, MatchMethod.SKU_IN_TITLE,
                confident ? BigDecimal.valueOf(0.85) : BigDecimal.valueOf(0.55),
                "Tim thay SKU '" + lastToken + "' trong title" + (confident ? "" : " (SKU ngan, can duyet)"),
                confident ? MatchStatus.AUTO_CONFIRMED : MatchStatus.REVIEW_REQUIRED,
                List.of());
    }

    private MatchResult matchBySkuInUrlSlug(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String slug = extractSlug(url);
        if (slug == null || slug.isBlank()) {
            return null;
        }
        List<String> tokens = SkuNormalizer.extractSkuLikeTokens(VietnameseTextUtil.stripDiacritics(slug).replace('-', ' '));
        Set<Long> matchedProductIds = new LinkedHashSet<>();
        Long lastMatchedId = null;
        String lastToken = null;
        for (String token : tokens) {
            String normalizedToken = SkuNormalizer.normalize(token);
            List<Product> found = findBySkuNormalizedSafe(normalizedToken);
            for (Product product : found) {
                matchedProductIds.add(product.getId());
                lastMatchedId = product.getId();
                lastToken = normalizedToken;
            }
        }
        if (matchedProductIds.isEmpty()) {
            return null;
        }
        if (matchedProductIds.size() > 1) {
            return new MatchResult(null, MatchMethod.SKU_IN_URL_SLUG, BigDecimal.valueOf(0.35),
                    "URL slug chua nhieu SKU khac nhau: co the la conflict giua nhieu san pham",
                    MatchStatus.REVIEW_REQUIRED, matchedProductIds.stream().toList());
        }
        return new MatchResult(lastMatchedId, MatchMethod.SKU_IN_URL_SLUG, BigDecimal.valueOf(0.7),
                "Tim thay SKU '" + lastToken + "' trong URL slug", MatchStatus.REVIEW_REQUIRED, List.of());
    }

    private String extractSlug(String url) {
        try {
            URI uri = URI.create(url.trim());
            String path = uri.getPath();
            if (path == null) {
                return null;
            }
            String[] segments = path.split("/");
            return segments.length > 0 ? segments[segments.length - 1] : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Goi ban tay khi can goi y fuzzy match cho MOT san pham cu the (khong chay hang loat
     * tren toan bo du lieu de tranh gop mu quang / qua tai). So sanh theo Jaccard token cua title.
     */
    public List<FuzzySuggestion> suggestFuzzyMatches(Product target, List<Product> candidates, double minScore) {
        Set<String> targetTokens = titleTokens(target.getTitle());
        return candidates.stream()
                .filter(c -> !c.getId().equals(target.getId()))
                .map(c -> {
                    Set<String> candidateTokens = titleTokens(c.getTitle());
                    double score = jaccard(targetTokens, candidateTokens);
                    return new FuzzySuggestion(c.getId(), score);
                })
                .filter(s -> s.score() >= minScore)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    private Set<String> titleTokens(String title) {
        if (title == null) {
            return Set.of();
        }
        String normalized = VietnameseTextUtil.stripDiacritics(title).toLowerCase();
        return new LinkedHashSet<>(List.of(normalized.split("[^a-z0-9]+")));
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new LinkedHashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new LinkedHashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    public record FuzzySuggestion(Long productId, double score) {
    }
}

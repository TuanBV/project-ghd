package com.example.mcprice.adapter;

import com.example.mcprice.domain.Product;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Publisher gia lap chay o local/test: luu gia "da cap nhat" trong memory de verify lai duoc,
 * khong goi ra ngoai. Dung khi chua co API/CMS thuc cua website.
 */
@Component
public class MockWebsitePricePublisher implements WebsitePricePublisher {

    private final ConcurrentHashMap<Long, AtomicReference<BigDecimal>> priceStore = new ConcurrentHashMap<>();

    @Override
    public PublishResult updateWebsitePrice(Product product, BigDecimal newPrice) {
        priceStore.computeIfAbsent(product.getId(), id -> new AtomicReference<>()).set(newPrice);
        return new PublishResult(true, "MOCK: da cap nhat gia website thanh " + newPrice.toPlainString());
    }

    @Override
    public VerificationResult verifyLandingPagePrice(Product product, BigDecimal expectedPrice) {
        AtomicReference<BigDecimal> stored = priceStore.get(product.getId());
        BigDecimal actual = stored == null ? null : stored.get();
        boolean matches = actual != null && actual.compareTo(expectedPrice) == 0;
        return new VerificationResult(matches, actual, matches ? "MOCK: gia khop" : "MOCK: gia landing page khong khop");
    }
}

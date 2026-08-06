package com.example.mcprice.adapter;

import com.example.mcprice.domain.Product;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Publisher gia lap cho Google Merchant Center, dung cho local/test — khong goi API thuc. */
@Component
public class MockMerchantCenterPublisher implements MerchantCenterPublisher {

    private final ConcurrentHashMap<Long, BigDecimal> priceStore = new ConcurrentHashMap<>();

    @Override
    public PublishResult updateMerchantPrice(Product product, BigDecimal newPrice) {
        priceStore.put(product.getId(), newPrice);
        return new PublishResult(true, "MOCK: da dong bo gia Merchant Center thanh " + newPrice.toPlainString());
    }

    @Override
    public MerchantStatusResult checkMerchantStatus(Product product) {
        BigDecimal current = priceStore.get(product.getId());
        return new MerchantStatusResult(current == null ? "NOT_SYNCED" : "ACTIVE",
                current == null ? "Chua duoc dong bo" : "Gia hien tai tren MC (mock): " + current.toPlainString());
    }
}

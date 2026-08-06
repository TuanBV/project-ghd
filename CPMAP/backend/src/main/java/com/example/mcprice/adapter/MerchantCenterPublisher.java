package com.example.mcprice.adapter;

import com.example.mcprice.domain.Product;
import java.math.BigDecimal;

public interface MerchantCenterPublisher {

    PublishResult updateMerchantPrice(Product product, BigDecimal newPrice);

    MerchantStatusResult checkMerchantStatus(Product product);

    record PublishResult(boolean success, String message) {
    }

    record MerchantStatusResult(String status, String message) {
    }
}

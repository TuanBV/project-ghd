package com.example.mcprice.adapter;

import com.example.mcprice.domain.Product;
import java.math.BigDecimal;

public interface WebsitePricePublisher {

    PublishResult updateWebsitePrice(Product product, BigDecimal newPrice);

    VerificationResult verifyLandingPagePrice(Product product, BigDecimal expectedPrice);

    record PublishResult(boolean success, String message) {
    }

    record VerificationResult(boolean matches, BigDecimal actualPrice, String message) {
    }
}

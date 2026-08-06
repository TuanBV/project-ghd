package com.example.mcprice.adapter;

import com.example.mcprice.config.AppProperties;
import com.example.mcprice.domain.Product;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter thuc cho Google Merchant API (Accounts/Products/DataSources - API stable hien tai,
 * KHONG dung Content API v2.1 cu). Component nay CHI duoc goi khi
 * MERCHANT_PUBLISHER_PROVIDER=GOOGLE_API va MERCHANT_DRY_RUN=false — hai dieu kien nay khong
 * bao gio dung mac dinh (xem MerchantSyncService).
 *
 * De hoan thien ket noi thuc can:
 * 1. Them dependency chinh thuc cho Google Merchant API (thay the placeholder ben duoi).
 * 2. Cau hinh OAuth2 service account (MERCHANT_CREDENTIALS_PATH) hoac OAuth flow phu hop tai khoan.
 * 3. Khong hardcode accountId/dataSourceId — da lay tu AppProperties (bien moi truong).
 */
@Component
@RequiredArgsConstructor
public class GoogleMerchantApiPublisher implements MerchantCenterPublisher {

    private static final Logger log = LoggerFactory.getLogger(GoogleMerchantApiPublisher.class);

    private final AppProperties appProperties;

    @Override
    public PublishResult updateMerchantPrice(Product product, BigDecimal newPrice) {
        var config = appProperties.getPublish().getMerchant();
        if (isBlank(config.getAccountId()) || isBlank(config.getDataSourceId()) || isBlank(config.getCredentialsPath())) {
            return new PublishResult(false, "Chua cau hinh day du MERCHANT_ACCOUNT_ID/MERCHANT_DATA_SOURCE_ID/MERCHANT_CREDENTIALS_PATH");
        }
        log.error("GoogleMerchantApiPublisher chua duoc ket noi API thuc (thieu Google Merchant API client + OAuth). "
                + "San pham #{} khong duoc dong bo.", product.getId());
        return new PublishResult(false, "Chua tich hop Google Merchant API client thuc — can bo sung truoc khi tat DRY_RUN");
    }

    @Override
    public MerchantStatusResult checkMerchantStatus(Product product) {
        return new MerchantStatusResult("UNKNOWN", "Chua tich hop Google Merchant API client thuc");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

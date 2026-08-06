-- Seed competitor website va policy gia toan cuc.
-- tongkhodienmaymienbac.com KHONG duoc seed vao day vi day la website cua minh, khong phai doi thu.

INSERT INTO competitors (name, base_url, enabled, crawl_mode, requests_per_minute, timeout_seconds, extractor_config)
VALUES
    ('sgt.com.vn', 'https://sgt.com.vn', TRUE, 'STATIC_HTML', 10, 10,
     '{"priceSelector": ".product-price, .price", "saleSelector": ".price-sale", "stockSelector": ".stock-status", "skuSelector": ".sku"}'::jsonb),
    ('dienmay88.vn', 'https://dienmay88.vn', TRUE, 'STATIC_HTML', 10, 10,
     '{"priceSelector": ".woocommerce-Price-amount, .price", "saleSelector": ".price ins .amount", "stockSelector": ".stock", "skuSelector": ".sku"}'::jsonb),
    ('dienmaythienphu.vn', 'https://dienmaythienphu.vn', TRUE, 'BROWSER', 6, 15,
     '{"priceSelector": ".price", "saleSelector": ".price-discount", "stockSelector": ".availability", "skuSelector": ".product-code"}'::jsonb),
    ('dienmayabc.com', 'https://dienmayabc.com', TRUE, 'MANUAL_ONLY', 10, 10,
     '{"priceSelector": ".price", "saleSelector": null, "stockSelector": ".stock", "skuSelector": ".sku"}'::jsonb);

INSERT INTO price_policies (
    scope, minimum_competitor_count, max_observation_age_hours, rounding_step,
    max_increase_percent, max_decrease_percent, outlier_threshold_percent, outlier_strategy,
    require_manual_approval, auto_publish_enabled
) VALUES (
    'GLOBAL', 2, 48, 10000, 15, 15, 30, 'FLAG_ONLY', TRUE, FALSE
);

-- Nguoi dung demo. Password: Admin@123 / Analyst@123 / Operator@123 (BCrypt).
INSERT INTO users (username, password_hash, full_name, role, enabled) VALUES
    ('admin', '$2b$10$u4H87DQhWJGxM2Jn/yw./OXe/d1k.OFghY4Xq90mWk1ytq259G4VW', 'System Admin', 'ADMIN', TRUE),
    ('analyst', '$2b$10$UYhzIG9rfr1eOjTrl0cyM.YQ8R1527f09O9HvOB/fiUFr9uDDPIfK', 'Pricing Analyst', 'ANALYST', TRUE),
    ('operator', '$2b$10$uP9R5NQRzPwUx5ZPhTewRehVNvYUVHR7U3GYvi9PSfS3dipl6WAoK', 'Operator', 'OPERATOR', TRUE);

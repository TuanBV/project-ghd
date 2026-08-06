-- Du lieu demo minh hoa day du cac tinh huong nghiep vu (muc 15 cua dac ta):
-- du 2 gia, chi 1 gia, "Lien he", outlier, stale, fuzzy match, duplicate URL, manual override,
-- cong voi 1 job run thanh cong va 1 job run partial failure.

INSERT INTO products (mc_offer_id, item_group_id, item_group_id_normalized, sku_original, sku_normalized,
                       title, description, product_url, image_url, brand, google_category, product_type,
                       condition, raw_condition, availability, raw_availability, current_website_price,
                       current_mc_price, currency, active, import_source, source_updated_at)
VALUES
    ('DEMO-A', 'DEMOA', 'DEMOA', 'DEMO-A', 'DEMOA',
     'Tivi Demo A - du 2 gia doi thu hop le', 'San pham demo co 2 nguon gia doi thu hop le de tinh trung binh',
     'https://tongkhodienmaymienbac.com/demo/tivi-demo-a', 'https://tongkhodienmaymienbac.com/img/demo-a.jpg',
     'DemoBrand', 'Electronics > TV', 'TV', 'NEW', 'New', 'IN_STOCK', 'In Stock', 5000000, 5000000, 'VND',
     TRUE, 'DEMO', now()),
    ('DEMO-B', 'DEMOB', 'DEMOB', 'DEMO-B', 'DEMOB',
     'Tu lanh Demo B - chi co 1 gia doi thu', 'San pham demo chi co 1 nguon gia doi thu hop le (INSUFFICIENT_DATA)',
     'https://tongkhodienmaymienbac.com/demo/tu-lanh-demo-b', 'https://tongkhodienmaymienbac.com/img/demo-b.jpg',
     'DemoBrand', 'Electronics > Fridge', 'Fridge', 'NEW', 'New', 'IN_STOCK', 'In Stock', 8000000, 8000000, 'VND',
     TRUE, 'DEMO', now()),
    ('DEMO-C', 'DEMOC', 'DEMOC', 'DEMO-C', 'DEMOC',
     'May giat Demo C - doi thu bao Lien he', 'San pham demo co doi thu chi bao "Lien he", khong co gia so',
     'https://tongkhodienmaymienbac.com/demo/may-giat-demo-c', 'https://tongkhodienmaymienbac.com/img/demo-c.jpg',
     'DemoBrand', 'Electronics > Washer', 'Washer', 'NEW', 'New', 'IN_STOCK', 'In Stock', 6500000, 6500000, 'VND',
     TRUE, 'DEMO', now()),
    ('DEMO-D', 'DEMOD', 'DEMOD', 'DEMO-D', 'DEMOD',
     'Dieu hoa Demo D - co outlier', 'San pham demo co 3 gia doi thu, 1 gia lech bat thuong (outlier FLAG_ONLY)',
     'https://tongkhodienmaymienbac.com/demo/dieu-hoa-demo-d', 'https://tongkhodienmaymienbac.com/img/demo-d.jpg',
     'DemoBrand', 'Electronics > AC', 'AC', 'NEW', 'New', 'IN_STOCK', 'In Stock', 10000000, 10000000, 'VND',
     TRUE, 'DEMO', now()),
    ('DEMO-E', 'DEMOE', 'DEMOE', 'DEMO-E', 'DEMOE',
     'Bep tu Demo E - du lieu stale', 'San pham demo co gia doi thu da qua cu (STALE), khong dung de tinh trung binh',
     'https://tongkhodienmaymienbac.com/demo/bep-tu-demo-e', 'https://tongkhodienmaymienbac.com/img/demo-e.jpg',
     'DemoBrand', 'Electronics > Cooktop', 'Cooktop', 'NEW', 'New', 'IN_STOCK', 'In Stock', 4200000, 4200000, 'VND',
     TRUE, 'DEMO', now()),
    ('DEMO-F', 'DEMOF', 'DEMOF', 'DEMO-F', 'DEMOF',
     'Quat Demo F - fuzzy match can duyet', 'San pham demo co competitor listing tao boi fuzzy match, REVIEW_REQUIRED',
     'https://tongkhodienmaymienbac.com/demo/quat-demo-f', 'https://tongkhodienmaymienbac.com/img/demo-f.jpg',
     'DemoBrand', 'Electronics > Fan', 'Fan', 'NEW', 'New', 'IN_STOCK', 'In Stock', 1200000, 1200000, 'VND',
     TRUE, 'DEMO', now()),
    ('DEMO-G', 'DEMOG', 'DEMOG', 'DEMO-G', 'DEMOG',
     'Loa Demo G - URL duoc gan dung', 'San pham demo giu URL doi thu hop le (dung de minh hoa duplicate URL conflict)',
     'https://tongkhodienmaymienbac.com/demo/loa-demo-g', 'https://tongkhodienmaymienbac.com/img/demo-g.jpg',
     'DemoBrand', 'Electronics > Speaker', 'Speaker', 'NEW', 'New', 'IN_STOCK', 'In Stock', 900000, 900000, 'VND',
     TRUE, 'DEMO', now()),
    ('DEMO-H', 'DEMOH', 'DEMOH', 'DEMO-H', 'DEMOH',
     'Loa Demo H - tranh URL voi Demo G', 'San pham demo minh hoa xung dot: cung mot URL doi thu bi tranh gan',
     'https://tongkhodienmaymienbac.com/demo/loa-demo-h', 'https://tongkhodienmaymienbac.com/img/demo-h.jpg',
     'DemoBrand', 'Electronics > Speaker', 'Speaker', 'NEW', 'New', 'IN_STOCK', 'In Stock', 950000, 950000, 'VND',
     TRUE, 'DEMO', now()),
    ('DEMO-I', 'DEMOI', 'DEMOI', 'DEMO-I', 'DEMOI',
     'May loc nuoc Demo I - co manual override', 'San pham demo co recommendation bi ghi de gia thu cong, co ly do va ngay het han',
     'https://tongkhodienmaymienbac.com/demo/may-loc-nuoc-demo-i', 'https://tongkhodienmaymienbac.com/img/demo-i.jpg',
     'DemoBrand', 'Electronics > WaterPurifier', 'WaterPurifier', 'NEW', 'New', 'IN_STOCK', 'In Stock', 7000000, 7000000, 'VND',
     TRUE, 'DEMO', now());

-- Competitor listings (dung ten competitor da seed o V2)
INSERT INTO competitor_listings (product_id, competitor_id, url, external_sku, match_method, match_score, match_reason, match_status, active)
VALUES
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-A'), (SELECT id FROM competitors WHERE name = 'sgt.com.vn'),
     'https://sgt.com.vn/demo/tivi-demo-a', 'DEMO-A', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-A'), (SELECT id FROM competitors WHERE name = 'dienmay88.vn'),
     'https://dienmay88.vn/demo/tivi-demo-a', 'DEMO-A', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),

    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-B'), (SELECT id FROM competitors WHERE name = 'sgt.com.vn'),
     'https://sgt.com.vn/demo/tu-lanh-demo-b', 'DEMO-B', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),

    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-C'), (SELECT id FROM competitors WHERE name = 'dienmay88.vn'),
     'https://dienmay88.vn/demo/may-giat-demo-c', 'DEMO-C', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-C'), (SELECT id FROM competitors WHERE name = 'dienmaythienphu.vn'),
     'https://dienmaythienphu.vn/demo/may-giat-demo-c', 'DEMO-C', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),

    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-D'), (SELECT id FROM competitors WHERE name = 'sgt.com.vn'),
     'https://sgt.com.vn/demo/dieu-hoa-demo-d', 'DEMO-D', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-D'), (SELECT id FROM competitors WHERE name = 'dienmay88.vn'),
     'https://dienmay88.vn/demo/dieu-hoa-demo-d', 'DEMO-D', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-D'), (SELECT id FROM competitors WHERE name = 'dienmaythienphu.vn'),
     'https://dienmaythienphu.vn/demo/dieu-hoa-demo-d', 'DEMO-D', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),

    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-E'), (SELECT id FROM competitors WHERE name = 'sgt.com.vn'),
     'https://sgt.com.vn/demo/bep-tu-demo-e', 'DEMO-E', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-E'), (SELECT id FROM competitors WHERE name = 'dienmay88.vn'),
     'https://dienmay88.vn/demo/bep-tu-demo-e', 'DEMO-E', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),

    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-F'), (SELECT id FROM competitors WHERE name = 'dienmayabc.com'),
     'https://dienmayabc.com/demo/quat-tuong-tu-model-tuong-tu', 'QUATSIM01', 'FUZZY', 0.55,
     'Demo seed: fuzzy match theo token title, can nguoi duyet xac nhan', 'REVIEW_REQUIRED', TRUE),

    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-G'), (SELECT id FROM competitors WHERE name = 'dienmayabc.com'),
     'https://dienmayabc.com/demo/loa-shared-url', 'DEMO-G', 'EXACT_NORMALIZED_SKU', 1.0,
     'Demo seed: URL nay duoc gan dung cho Demo G; Demo H tranh URL nay se bi conflict (xem import_issues)',
     'AUTO_CONFIRMED', TRUE),

    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-I'), (SELECT id FROM competitors WHERE name = 'sgt.com.vn'),
     'https://sgt.com.vn/demo/may-loc-nuoc-demo-i', 'DEMO-I', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-I'), (SELECT id FROM competitors WHERE name = 'dienmay88.vn'),
     'https://dienmay88.vn/demo/may-loc-nuoc-demo-i', 'DEMO-I', 'EXACT_NORMALIZED_SKU', 1.0, 'Demo seed: SKU khop chinh xac', 'AUTO_CONFIRMED', TRUE);

-- Price observations
INSERT INTO price_observations (competitor_listing_id, price, currency, availability, raw_price_text, source_type, observation_status, captured_at, note)
VALUES
    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-A')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'sgt.com.vn')),
     5100000, 'VND', 'In Stock', '5.100.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed'),
    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-A')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'dienmay88.vn')),
     4950000, 'VND', 'In Stock', '4.950.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed'),

    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-B')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'sgt.com.vn')),
     8200000, 'VND', 'In Stock', '8.200.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed - chi 1 nguon => INSUFFICIENT_DATA'),

    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-C')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'dienmay88.vn')),
     NULL, 'VND', NULL, 'Lien he', 'MANUAL', 'CONTACT_ONLY', now(), 'Demo seed - Lien he, khong co gia so'),
    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-C')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'dienmaythienphu.vn')),
     6600000, 'VND', 'In Stock', '6.600.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed'),

    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-D')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'sgt.com.vn')),
     10100000, 'VND', 'In Stock', '10.100.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed'),
    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-D')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'dienmay88.vn')),
     9900000, 'VND', 'In Stock', '9.900.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed'),
    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-D')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'dienmaythienphu.vn')),
     16000000, 'VND', 'In Stock', '16.000.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed - gia outlier, lech ~60% so voi 2 gia con lai'),

    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-E')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'sgt.com.vn')),
     4300000, 'VND', 'In Stock', '4.300.000 VND', 'MANUAL', 'STALE', now() - interval '10 days', 'Demo seed - du lieu qua cu'),
    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-E')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'dienmay88.vn')),
     4250000, 'VND', 'In Stock', '4.250.000 VND', 'MANUAL', 'STALE', now() - interval '9 days', 'Demo seed - du lieu qua cu'),

    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-G')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'dienmayabc.com')),
     920000, 'VND', 'In Stock', '920.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed'),

    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-I')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'sgt.com.vn')),
     7100000, 'VND', 'In Stock', '7.100.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed'),
    ((SELECT id FROM competitor_listings WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-I')
        AND competitor_id = (SELECT id FROM competitors WHERE name = 'dienmay88.vn')),
     7050000, 'VND', 'In Stock', '7.050.000 VND', 'MANUAL', 'VALID', now(), 'Demo seed');

-- Import run + issue de minh hoa DUPLICATE_URL_CONFLICT giua Demo G va Demo H
INSERT INTO import_runs (import_type, file_name, file_hash, status, total_rows, success_rows, issue_rows, started_at, finished_at, triggered_by)
VALUES ('COMPARISON', 'demo-seed-duplicate-url.xlsx', 'demo-seed-hash-0001', 'PARTIAL_SUCCESS', 2, 1, 1, now(), now(), 'DEMO_SEED');

INSERT INTO import_rows (import_run_id, row_number, row_identity, raw_row_json, status, product_id)
VALUES
    ((SELECT id FROM import_runs WHERE file_hash = 'demo-seed-hash-0001'), 1, 'demo-seed-row-1',
     '{"sku": "DEMO-G", "url": "https://dienmayabc.com/demo/loa-shared-url"}'::jsonb, 'IMPORTED',
     (SELECT id FROM products WHERE mc_offer_id = 'DEMO-G')),
    ((SELECT id FROM import_runs WHERE file_hash = 'demo-seed-hash-0001'), 2, 'demo-seed-row-2',
     '{"sku": "DEMO-H", "url": "https://dienmayabc.com/demo/loa-shared-url"}'::jsonb, 'ISSUE',
     (SELECT id FROM products WHERE mc_offer_id = 'DEMO-H'));

INSERT INTO import_issues (import_run_id, import_row_id, issue_type, severity, message, details)
VALUES
    ((SELECT id FROM import_runs WHERE file_hash = 'demo-seed-hash-0001'),
     (SELECT id FROM import_rows WHERE row_identity = 'demo-seed-row-2'),
     'DUPLICATE_URL_CONFLICT', 'ERROR',
     'URL https://dienmayabc.com/demo/loa-shared-url da duoc gan cho san pham DEMO-G, khong the gan tiep cho DEMO-H',
     '{"conflictingProduct": "DEMO-G", "attemptedProduct": "DEMO-H"}'::jsonb);

-- Price recommendations demo (trang thai da tinh san de minh hoa dashboard/Products screen ngay sau khi seed;
-- PriceCalculationJob chay lai se tao ban ghi moi voi cung logic).
INSERT INTO price_recommendations (product_id, current_price, raw_average_price, rounded_price, final_suggested_price,
                                    included_source_count, excluded_source_count, calculation_snapshot, status,
                                    approved_by, approved_at)
VALUES
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-A'), 5000000, 5025000, 5030000, 5030000, 2, 0,
     '{"note": "Demo seed: 2 nguon hop le (5.100.000 + 4.950.000)/2 = 5.025.000, lam tron buoc 10000 -> 5.030.000"}'::jsonb,
     'APPROVED', 'admin', now() - interval '2 hours'),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-B'), 8000000, NULL, NULL, 8000000, 1, 0,
     '{"note": "Demo seed: chi 1 nguon hop le, nho hon minimumCompetitorCount=2 -> INSUFFICIENT_DATA"}'::jsonb,
     'INSUFFICIENT_DATA', NULL, NULL),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-C'), 6500000, NULL, NULL, 6500000, 1, 1,
     '{"note": "Demo seed: 1 nguon CONTACT_ONLY bi loai, chi con 1 nguon hop le -> INSUFFICIENT_DATA"}'::jsonb,
     'INSUFFICIENT_DATA', NULL, NULL),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-D'), 10000000, 12000000, 12000000, 12000000, 3, 0,
     '{"note": "Demo seed: trung binh 3 nguon bi keo cao boi outlier 16.000.000, outlierFlagged=true -> REVIEW_REQUIRED (FLAG_ONLY khong loai outlier)"}'::jsonb,
     'REVIEW_REQUIRED', NULL, NULL),
    ((SELECT id FROM products WHERE mc_offer_id = 'DEMO-I'), 7000000, 7075000, 7080000, 7200000, 2, 0,
     '{"note": "Demo seed: trung binh 2 nguon = 7.075.000, lam tron = 7.080.000, nhung duoc override thanh 7.200.000"}'::jsonb,
     'REVIEW_REQUIRED', NULL, NULL);

UPDATE price_recommendations
SET override_price = 7200000,
    override_by = 'admin',
    override_reason = 'Giu bien loi nhuan toi thieu theo yeu cau kinh doanh quy nay',
    override_expires_at = now() + interval '30 days'
WHERE product_id = (SELECT id FROM products WHERE mc_offer_id = 'DEMO-I');

-- Job runs demo: mot thanh cong, mot partial failure
INSERT INTO job_runs (job_key, trigger_type, status, total_items, success_items, failed_items, progress_percent,
                       started_at, finished_at, correlation_id, triggered_by)
VALUES
    ('CompetitorCrawlJob', 'MANUAL', 'SUCCESS', 8, 8, 0, 100, now() - interval '1 hour', now() - interval '55 minutes',
     'demo-correlation-success', 'DEMO_SEED'),
    ('CompetitorCrawlJob', 'SCHEDULED', 'PARTIAL_SUCCESS', 8, 5, 3, 100, now() - interval '1 day', now() - interval '1 day' + interval '5 minutes',
     'demo-correlation-partial', 'QUARTZ_SCHEDULER');

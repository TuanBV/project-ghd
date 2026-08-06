-- Doi ten cot cho dung ban chat: khong con la "so URL da xac minh qua HTML tung trang" (cham,
-- can tai tung trang) ma la "tong so URL san pham lay duoc tu sitemap sau khi loc file khong
-- phai san pham theo ten file" — nhanh, khong can tai tung trang rieng.
ALTER TABLE competitors RENAME COLUMN verified_product_page_count TO last_sitemap_url_count;

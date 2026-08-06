-- Luu tong so trang "co ve la san pham thuc" tren site doi thu ma sitemap discovery tim thay,
-- KE CA nhung URL khong khop duoc SKU nao trong DB cua minh (vd doi thu ban hang minh khong ban).
-- Khac voi so luong trong competitor_listings (chi tinh URL da khop duoc voi 1 Product cua minh,
-- vi competitor_listings.product_id la NOT NULL) — cot nay cho biet TONG catalog thuc te cua doi
-- thu, xac minh bang cach doc HTML tung URL chua khop va kiem tra co price/sku selector hay khong.
ALTER TABLE competitors ADD COLUMN verified_product_page_count INT NOT NULL DEFAULT 0;

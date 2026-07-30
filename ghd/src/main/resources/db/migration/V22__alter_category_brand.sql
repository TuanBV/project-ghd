ALTER TABLE category
    ADD COLUMN slug varchar(100);
ALTER TABLE brand
    ADD COLUMN slug varchar(100);

CREATE INDEX idx_category_slug ON category(slug);
CREATE INDEX idx_brand_slug ON brand(slug);

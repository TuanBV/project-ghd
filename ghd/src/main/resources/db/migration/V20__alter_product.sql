ALTER TABLE product
    ADD COLUMN sku          varchar(100)   NOT NULL,
    ADD COLUMN price        decimal(15, 2) NOT NULL DEFAULT 0,
    ADD COLUMN sale_price   decimal(15, 2) NOT NULL DEFAULT 0,
    ADD COLUMN stock_qty    integer        NOT NULL DEFAULT 0,
    ADD COLUMN color        varchar(50),
    ADD COLUMN size         varchar(50),
    ADD COLUMN image        varchar(500),
    ADD COLUMN slug         varchar(500),
    ADD COLUMN sold_count INT DEFAULT 0 NOT NULL,
    ADD COLUMN group_id TEXT NULL;